package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.helpers.ParserHelper
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.use
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object GameMonitorInteractor : Interactor() {

  private const val TAG = "GameMonitorInteractor"

  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> get() = _isSearching

  private val _possiblePaths = MutableStateFlow<List<Path>>(emptyList())
  val possiblePaths: StateFlow<List<Path>> get() = _possiblePaths

  private val _userMarkerStart = MutableStateFlow<Long>(0L)
  val userMarkerStart: StateFlow<Long> get() = _userMarkerStart

  private val _userMarkerEnd = MutableStateFlow<Long>(0L)
  val userMarkerEnd: StateFlow<Long> get() = _userMarkerEnd

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> get() = _isPlaying

  @Volatile
  private var currentPath: Path? = null

  @Volatile
  private var raf: RandomAccessFile? = null

  @Volatile
  private var currentPosition: Long = 0L

  @Volatile
  private var currentMode: MonitorModes = MonitorModes.DISABLED

  @Volatile
  private var replayCompleted: Boolean = false

  // Tunables
  private const val READ_CHUNK_SIZE = 16 * 1024 * 1024 // 16 MB
  private const val PARSE_BATCH_SIZE = 1000

  fun locateCombatLog(searchEverywhere: Boolean = false) {
    _isSearching.value = true

    val oneDriveDocumentsPath = Paths.get(System.getProperty("user.home"), "OneDrive", "Documents", "ArcheRage")
    val documentsPath = Paths.get(System.getProperty("user.home"), "Documents", "ArcheRage")
    val everywherePath = Paths.get(System.getProperty("user.home"))
    val desktopPath = Paths.get(System.getProperty("user.home"), "Desktop")

    val searchPaths = mutableListOf<Path>(desktopPath)

    if (searchEverywhere) {
      if (Files.exists(everywherePath)) searchPaths.add(everywherePath)
    } else {
      if (Files.exists(oneDriveDocumentsPath)) searchPaths.add(oneDriveDocumentsPath)
      if (Files.exists(documentsPath)) searchPaths.add(documentsPath)
    }

    val possibleLogFiles = mutableListOf<Path>()

    fun seek(baseDir: Path) {
      val stream = Files.list(baseDir)
      stream.use { paths ->
        paths.forEach { path ->
          try {
            if (Files.isDirectory(path) && Files.isReadable(path)) {
              seek(path)
            } else if (path.fileName.toString().contains("combat.log") && !path.pathString.contains("LogBackups")) {
              possibleLogFiles.add(path)
            }
          } catch (_: Exception) {
            // ignore directories we can't read
          }
        }
      }
    }

    searchPaths.forEach {
      try {
        seek(it)
      } catch (_: Exception) {
        // ignore unreachable top-level search paths
      }
    }

    _possiblePaths.value = possibleLogFiles
    _isSearching.value = false
  }

  fun chooseCombatLog(path: Path) {
    synchronized(this) {
      currentPath = path
    }
  }

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

  fun restart() {
    synchronized(this) {
      replayCompleted = false
      val path = currentPath ?: _possiblePaths.value.firstOrNull()
      if (path == null) {
        _isPlaying.value = false
        return
      }
      openFileForMode(path, restart = true)
    }
  }

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

      val candidates = _possiblePaths.value
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

        // determine read target (respect REPLAY end marker)
        val endMarker = _userMarkerEnd.value
        val stopPosition = if (currentMode == MonitorModes.REPLAY && endMarker > 0L) {
          endMarker.coerceAtMost(fileLength)
        } else {
          fileLength
        }

        if (currentPosition < stopPosition) {
          var pos = currentPosition
          val iso = Charsets.ISO_8859_1
          val batch = ArrayList<String>(PARSE_BATCH_SIZE)
          var leftover = ByteArray(0)

          while (pos < stopPosition) {
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
                  try {
                    ParserHelper.parseCombatEvents(batch.toList())
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
          if (leftover.isNotEmpty()) {
            val line = String(leftover, iso)
            batch.add(line)
          }

          if (batch.isNotEmpty()) {
            try {
              ParserHelper.parseCombatEvents(batch.toList())
            } catch (_: Exception) {
              // swallow parse exceptions
            }
            batch.clear()
          }

          currentPosition = pos.coerceAtMost(stopPosition)
        }

        // After reading available lines, mode-specific actions
        val fileLenAfter = channel.size()
        when (currentMode) {
          MonitorModes.REPLAY -> {
            val endMarkerVal = _userMarkerEnd.value
            val stopPos = if (endMarkerVal <= 0L) Long.MAX_VALUE else endMarkerVal
            if (currentPosition >= stopPos || currentPosition >= fileLenAfter) {
              // finished replay: close reader but keep the chosen path and mark completed
              closeFile(clearPath = false)
              replayCompleted = true
              _isPlaying.value = false
              Log.info(TAG, "Completed REPLAY mode tailing of combat log at $desired...")
            } else {
              _isPlaying.value = true
            }
          }
          MonitorModes.MONITOR -> {
            _isPlaying.value = true
          }
          else -> {
            _isPlaying.value = false
          }
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

        val startMarker = _userMarkerStart.value
        val startPos = when (currentMode) {
          MonitorModes.REPLAY -> {
            val desired = if (startMarker > 0L) startMarker else 0L
            desired.coerceIn(0L, fileLen)
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
            val endMarker = _userMarkerEnd.value
            val stopPosition = if (endMarker <= 0L) Long.MAX_VALUE else endMarker
            (currentPosition < stopPosition) && (currentPosition < rafLocal.length())
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
        currentPosition = 0L
      }
    }
  }

  enum class MonitorModes {
    DISABLED,
    REPLAY,
    MONITOR
  }
}
