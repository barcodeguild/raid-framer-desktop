package com.reoky.raidframer.core.interactor

import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.use
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interactor responsible for locating and tailing the combat.log file.
 * Searches user directories for combat.log files and maintains a handle
 * to the selected file for tailing new lines.
 */
object GameMonitorInteractor : Interactor() {
  private val _isSearching = MutableStateFlow(false)
  val isSearching: StateFlow<Boolean> get() = _isSearching

  private val _possiblePaths = MutableStateFlow<List<Path>>(emptyList())
  val possiblePaths: StateFlow<List<Path>> get() = _possiblePaths

  // time range markers used for replays / restarts and exports long marker position timestamp
  // so the player can view damage charts for a specific fights
  private val _userMarkerStart = MutableStateFlow<Long>(0L)
  val userMarkerStart: StateFlow<Long> get() = _userMarkerStart

  private val _userMarkerEnd = MutableStateFlow<Long>(0L)
  val userMarkerEnd: StateFlow<Long> get() = _userMarkerEnd

  @Volatile
  private var currentPath: Path? = null

  @Volatile
  private var raf: RandomAccessFile? = null

  @Volatile
  private var currentPosition: Long = 0L


  /**
   * Search for combat.log files according to the provided logic.
   * Updates _possiblePaths and _isSearching. Will search the user's entire home directory
   * if searchEverywhere is true, otherwise looks for ArcheRage's default locations.
   */
  fun locateCombatLog(searchEverywhere: Boolean = false) {
    _isSearching.value = true

    val oneDriveDocumentsPath = Paths.get(System.getProperty("user.home"), "OneDrive", "Documents", "ArcheRage")
    val documentsPath = Paths.get(System.getProperty("user.home"), "Documents", "ArcheRage")
    val everywherePath = Paths.get(System.getProperty("user.home"))

    val searchPaths = mutableListOf<Path>()

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


  /**
   * Called periodically (every 3s from an IO thread). Maintains a handle to the selected combat.log,
   * tails new lines and forwards them to CombatEventInteractor.parseLine().
   */
  override suspend fun interact() {
    try {
      val candidates = _possiblePaths.value
      if (candidates.isEmpty()) {
        // nothing to tail
        closeFile()
        return
      }

      val desired = currentPath ?: candidates.firstOrNull() ?: return

      // if we don't have a current, or it doesn't exist, or it's not the desired path, then open the desired one
      if (currentPath == null || !Files.exists(currentPath) || currentPath != desired) {
        openFile(desired)
      } else {
        // desired is current, check to see if file size has changed since three seconds ago
        // check for truncation/rotation
        val fileSize = Files.size(desired)
        if (fileSize < currentPosition) {
          openFile(desired) // reopen and start tailing log
        }
      }

      val localRaf = raf ?: return

      synchronized(this) {
        // Ensure pointer at last known position, then read available lines
        localRaf.seek(currentPosition)
        var rawLine = localRaf.readLine()
        while (rawLine != null) {
          // RandomAccessFile.readLine uses ISO-8859-1 — convert to UTF-8
          val line = String(rawLine.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
          try {
            EventParserInteractor.parseLines(listOf(line))
          } catch (_: Exception) {
            // swallow parse exceptions to avoid stopping tailing
          }
          currentPosition = localRaf.filePointer
          rawLine = localRaf.readLine()
        }
      }
    } catch (t: Throwable) {
      // on any IO error, close to allow recovery next tick
      try {
        closeFile()
      } catch (_: Exception) {}
    }
  }

  private fun openFile(path: Path) {
    synchronized(this) {
      closeFile()
      try {
        val rafLocal = RandomAccessFile(path.toFile(), "r")
        // start tailing at end of file to avoid reprocessing existing content
        val startPos = rafLocal.length()
        rafLocal.seek(startPos)
        raf = rafLocal
        currentPath = path
        currentPosition = startPos
      } catch (t: Throwable) {
        // failed to open — ensure closed state
        raf = null
        currentPath = null
        currentPosition = 0L
      }
    }
  }

  private fun closeFile() {
    synchronized(this) {
      try {
        raf?.close()
      } catch (_: Exception) {}
      raf = null
      currentPath = null
      currentPosition = 0L
    }
  }
}


enum class MonitorModes {
  DISABLED, // no monitoring
  REPLAY, // start from beginning of file (or from marker position) and stop at end
  RESTART, // start from marker position and continue tailing
  TAIL // start tailing from end of file
}