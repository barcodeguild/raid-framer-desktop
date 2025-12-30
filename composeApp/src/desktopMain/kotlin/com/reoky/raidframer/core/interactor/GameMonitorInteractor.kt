// kotlin
package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.helpers.EventParserHelper
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.use
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/*
 * Interactor that monitors the ArcheRage combat log file for new events.  Can operate
 * in REPLAY mode (read events between UTC timestamps) or MONITOR mode (tail the file for new events).
 */
object GameMonitorInteractor : Interactor() {

  private const val TAG = "GameMonitorInteractor"

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> get() = _isSearching

  private val _possibleCombatLogs = MutableStateFlow<List<Path>>(emptyList())
  val possiblePaths: StateFlow<List<Path>> get() = _possibleCombatLogs

  private val _userMarkerStart = MutableStateFlow<Long>(0L) // UTC timestamp (ms)
  val userMarkerStart: StateFlow<Long> get() = _userMarkerStart

  private val _userMarkerEnd = MutableStateFlow<Long>(0L) // UTC timestamp (ms); 0 => no end
  val userMarkerEnd: StateFlow<Long> get() = _userMarkerEnd

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> get() = _isPlaying

  @Volatile
  var currentMode: MonitorModes = MonitorModes.DISABLED

  @Volatile
  private var currentPath: Path? = null

  @Volatile
  private var raf: RandomAccessFile? = null

  @Volatile
  private var currentPosition: Long = 0L

  @Volatile
  private var replayCompleted: Boolean = false

  // Tunables
  private const val READ_CHUNK_SIZE = 16 * 1024 * 1024 // 16 MB
  private const val PARSE_BATCH_SIZE = 1000


  /*
   * Recursively search for ArcheRage directory in common locations to enumerate possible combat log files.
   */
  fun locateArcheRageDirectory(searchEverywhere: Boolean = false) {
    _isSearching.value = true

    val oneDriveDocumentsPath = Paths.get(System.getProperty("user.home"), "OneDrive", "Documents", "ArcheRage")
    val documentsPath = Paths.get(System.getProperty("user.home"), "Documents", "ArcheRage")
    val everywherePath = Paths.get(System.getProperty("user.home"))

    val searchPaths = mutableListOf<Path>(oneDriveDocumentsPath, documentsPath)

    if (searchEverywhere) {
      if (Files.exists(everywherePath)) searchPaths.add(everywherePath)
    } else {
      if (Files.exists(oneDriveDocumentsPath)) searchPaths.add(oneDriveDocumentsPath)
      if (Files.exists(documentsPath)) searchPaths.add(documentsPath)
    }

    val possibleArcheRageDirectories = mutableListOf<Path>()

    fun seek(baseDir: Path) {
      val stream = Files.list(baseDir)
      stream.use { paths ->
        paths.forEach { file ->
          try {
            if (Files.isDirectory(file) && Files.isReadable(file)) {
              seek(file)
            } else if (file.fileName.toString().lowercase().contains("system.cfg")) {
              possibleArcheRageDirectories.add(file.parent)
            }
          } catch (_: Exception) {
          }
        }
      }
    }

    searchPaths.forEach {
      try {
        seek(it)
      } catch (_: Exception) {
      }
    }

    println(possibleArcheRageDirectories.distinct())


    val combatLogPaths = possibleArcheRageDirectories.mapNotNull { dir ->
      val combatLogPath = dir.resolve("combat.log")
      if (Files.exists(combatLogPath) && Files.isReadable(combatLogPath)) {
        // assign valid install path as well as return the combat log path
        RFConfig.update { old ->
          old.copy(defaultArcheRageDirectory = dir.toString())
        }
        combatLogPath
      } else {
        null
      }
    }

    _possibleCombatLogs.value = combatLogPaths.distinct()
    _isSearching.value = false
  }

  /*
   * Manually choose a combat log file to monitor or replay.
   */
  fun chooseCombatLog(path: Path) {
    synchronized(this) {
      currentPath = path
    }
  }

  /*
   * Set monitoring mode and user-defined UTC start/end timestamps (milliseconds).
   */
  fun setOptions(
    mode: MonitorModes,
    startMarker: Long,
    endMarker: Long
  ) {
    _userMarkerStart.value = startMarker
    _userMarkerEnd.value = endMarker
    currentMode = mode

    if (mode != MonitorModes.REPLAY) {
      replayCompleted = false
    }

    if (mode == MonitorModes.DISABLED) {
      closeFile()
      replayCompleted = false
      _isPlaying.value = false
    } else {
      try {
        if (mode == MonitorModes.REPLAY) replayCompleted = false
        restart()
      } catch (_: Exception) {
        _isPlaying.value = false
      }
    }
  }

  /*
   * Restart monitoring or replaying from the chosen combat log file and user-defined markers.
   */
  fun restart() {
    synchronized(this) {
      replayCompleted = false
      val path = currentPath ?: _possibleCombatLogs.value.firstOrNull()
      if (path == null) {
        _isPlaying.value = false
        return
      }
      openFileForMode(path, restart = true)
    }
  }

  /*
   * Main event loop tick: read new lines from the combat log file as per the current mode and user markers.
   */
  override suspend fun interact() {
    println("GameMonitorInteractor.interact tick: mode=$currentMode, path=$currentPath, isPlaying=${_isPlaying.value}, replayCompleted=$replayCompleted")
    try {
      if (currentMode == MonitorModes.DISABLED) {
        closeFile()
        _isPlaying.value = false
        return
      }

      if (currentMode == MonitorModes.REPLAY && replayCompleted) {
        _isPlaying.value = false
        return
      }

      val candidates = _possibleCombatLogs.value
      if (candidates.isEmpty() && currentPath == null) {
        closeFile()
        _isPlaying.value = false
        return
      }

      val desired = currentPath ?: candidates.firstOrNull() ?: return

      if (currentPath == null || !Files.exists(currentPath) || currentPath != desired) {
        openFileForMode(desired, restart = false)
      } else {
        val fileSize = Files.size(desired)
        if (fileSize < currentPosition) {
          openFileForMode(desired, restart = false)
        }
      }

      val localRaf = raf ?: run {
        _isPlaying.value = false
        return
      }

      synchronized(this) {
        val channel = localRaf.channel
        val fileLength = channel.size()

        // In timestamp-based REPLAY, stopPosition is file end (we stop based on event timestamps)
        val stopPosition = fileLength

        if (currentPosition < stopPosition && (currentMode != MonitorModes.REPLAY || !replayCompleted)) {
          var pos = currentPosition
          val iso = Charsets.ISO_8859_1
          val batch = ArrayList<String>(PARSE_BATCH_SIZE)
          var leftover = ByteArray(0)
          var reachedEndByTimestamp = false
          var anyEventFoundAfterStart = false

          while (pos < stopPosition && !reachedEndByTimestamp) {
            val remaining = (stopPosition - pos).coerceAtMost(READ_CHUNK_SIZE.toLong()).toInt()
            val buffer = ByteBuffer.allocate(remaining)
            var read = 0
            while (read < remaining) {
              val r = channel.read(buffer, pos + read)
              if (r <= 0) break
              read += r.toInt()
            }
            if (read <= 0) break
            buffer.flip()
            val chunkBytes = ByteArray(buffer.remaining())
            buffer.get(chunkBytes)

            // combine leftover + chunk
            val combined = if (leftover.isNotEmpty()) {
              val merged = ByteArray(leftover.size + chunkBytes.size)
              System.arraycopy(leftover, 0, merged, 0, leftover.size)
              System.arraycopy(chunkBytes, 0, merged, leftover.size, chunkBytes.size)
              merged
            } else {
              chunkBytes
            }

            var start = 0
            val newline = '\n'.code.toByte()
            for (i in combined.indices) {
              if (combined[i] == newline) {
                val endIdx = if (i > 0 && combined[i - 1] == '\r'.code.toByte()) i - 1 else i
                val lineBytes = combined.copyOfRange(start, endIdx)
                val line = String(lineBytes, iso)
                batch.add(line)
                if (batch.size >= PARSE_BATCH_SIZE) {
                  // parse and handle batch
                  try {
                    val events = EventParserHelper.parseCombatEvents(batch.toList())
                    if (events.isNotEmpty()) {
                      val startMarkerTs = _userMarkerStart.value
                      val endMarkerTs = _userMarkerEnd.value
                      // mark if any event is after the start marker
                      if (events.any { it.timestamp >= startMarkerTs }) anyEventFoundAfterStart = true

                      // post only events inside inclusive [start, end]
                      val toPost = events.filter { ev ->
                        val afterStart = if (startMarkerTs <= 0L) true else ev.timestamp >= startMarkerTs
                        val beforeEnd = if (endMarkerTs <= 0L) true else ev.timestamp <= endMarkerTs
                        afterStart && beforeEnd
                      }
                      for (event in toPost) {
                        PlayerCacheInteractor.postEvent(event)
                      }

                      // if any event in this batch exceeds endMarker (strictly >), end replay
                      if (endMarkerTs > 0L && events.any { it.timestamp > endMarkerTs }) {
                        reachedEndByTimestamp = true
                      }
                    }
                  } catch (_: Exception) {
                    // swallow parse exceptions
                  }
                  batch.clear()
                }
                start = i + 1
              }
            }

            leftover = if (start < combined.size) combined.copyOfRange(start, combined.size) else ByteArray(0)
            pos += read.toLong()
          }

          // trailing leftover becomes final line fragment
          if (leftover.isNotEmpty() && !reachedEndByTimestamp) {
            val line = String(leftover, iso)
            batch.add(line)
          }

          if (batch.isNotEmpty() && !reachedEndByTimestamp) {
            try {
              val events = EventParserHelper.parseCombatEvents(batch.toList())
              if (events.isNotEmpty()) {
                val startMarkerTs = _userMarkerStart.value
                val endMarkerTs = _userMarkerEnd.value
                if (events.any { it.timestamp >= startMarkerTs }) anyEventFoundAfterStart = true

                val toPost = events.filter { ev ->
                  val afterStart = if (startMarkerTs <= 0L) true else ev.timestamp >= startMarkerTs
                  val beforeEnd = if (endMarkerTs <= 0L) true else ev.timestamp <= endMarkerTs
                  afterStart && beforeEnd
                }
                for (event in toPost) {
                  PlayerCacheInteractor.postEvent(event)
                }

                if (endMarkerTs > 0L && events.any { it.timestamp > endMarkerTs }) {
                  reachedEndByTimestamp = true
                }
              }
            } catch (_: Exception) {
            }
            batch.clear()
          }

          currentPosition = pos.coerceAtMost(stopPosition)

          // If replay mode and we've either seen an event past the end timestamp or we've reached EOF
          if (currentMode == MonitorModes.REPLAY) {
            val endMarkerTs = _userMarkerEnd.value
            // If we reached end-by-timestamp, finish.
            if (reachedEndByTimestamp) {
              // finished replay: close reader but keep the chosen path and mark completed
              try {
                closeFile(clearPath = false)
              } catch (_: Exception) {}
              replayCompleted = true
              _isPlaying.value = false
              Log.info(TAG, "Completed REPLAY mode tailing of combat log at $currentPath (by end timestamp)...")
            } else {
              // If EOF and we never found any event after start marker, still mark completed (nothing to play)
              val fileLenAfter = channel.size()
              if (currentPosition >= fileLenAfter) {
                // If start marker was set but no events matched, treat as completed
                val startMarkerTs = _userMarkerStart.value
                if (startMarkerTs > 0L && !anyEventFoundAfterStart) {
                  try {
                    closeFile(clearPath = false)
                  } catch (_: Exception) {}
                  replayCompleted = true
                  _isPlaying.value = false
                  Log.info(TAG, "Completed REPLAY mode tailing of combat log at $currentPath (EOF with no events after start)...")
                } else {
                  _isPlaying.value = !replayCompleted
                }
              } else {
                _isPlaying.value = true
              }
            }
          } else {
            _isPlaying.value = true
          }
        } else {
          // nothing to read
          _isPlaying.value = false
        }
      }
    } catch (t: Throwable) {
      try {
        closeFile()
      } catch (_: Exception) {}
      _isPlaying.value = false
    }
  }

  private fun openFileForMode(path: Path, restart: Boolean) {
    Log.info(TAG, "Opening combat log at $path for mode $currentMode (restart=$restart)")
    synchronized(this) {
      closeFile()
      try {
        val rafLocal = RandomAccessFile(path.toFile(), "r")
        val fileLen = rafLocal.length()

        // For timestamp-based REPLAY, we start reading from file start (0L) and filter events by timestamp.
        // MONITOR retains previous behaviour (start at end unless user asked otherwise).
        val startMarker = _userMarkerStart.value
        val startPos = when (currentMode) {
          MonitorModes.REPLAY -> {
            0L
          }
          MonitorModes.MONITOR -> {
            if (startMarker > 0L) startMarker.coerceIn(0L, fileLen) else fileLen
          }
          else -> {
            fileLen
          }
        }

        rafLocal.seek(startPos)
        raf = rafLocal
        currentPath = path
        currentPosition = startPos

        _isPlaying.value = when (currentMode) {
          MonitorModes.REPLAY -> {
            // initially true; actual completion will be determined during reading/parsing
            fileLen > 0L
          }
          MonitorModes.MONITOR -> true
          else -> false
        }
      } catch (t: Throwable) {
        raf = null
        currentPath = null
        currentPosition = 0L
        _isPlaying.value = false
      }
    }
  }

  private fun closeFile(clearPath: Boolean = true) {
    synchronized(this) {
      try {
        raf?.close()
      } catch (_: Exception) {}
      raf = null
      if (clearPath) {
        currentPath = null
        currentPosition = 0L
      } else {
        // preserve currentPosition when we only close the file but keep the chosen path
      }
    }
  }

  enum class MonitorModes {
    DISABLED,
    REPLAY,
    MONITOR
  }
}
