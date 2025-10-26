package com.reoky.raidframer.core.interactor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

class LoggingInteractor : Interactor() {

  private val queue = ConcurrentLinkedQueue<LogEntry>()
  private val seq = AtomicLong(0)
  private val writeMutex = Mutex()

  private val userHomeDirectory = System.getProperty("user.home")
  private val appDirectory = "$userHomeDirectory/.RaidFramer"
  private val loggingFilePath: Path = Paths.get("$appDirectory/debug.log")

  private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    .withZone(ZoneId.systemDefault())

  // Public convenience methods
  fun debug(tag: String, message: String) = log(Level.DEBUG, tag, message)
  fun info(tag: String, message: String) = log(Level.INFO, tag, message)
  fun warn(tag: String, message: String) = log(Level.WARN, tag, message)
  fun error(tag: String, message: String) = log(Level.ERROR, tag, message)

  fun log(level: Level, tag: String, message: String) {
    val entry = LogEntry(Instant.now(), level, tag, message, seq.incrementAndGet())
    queue.add(entry)
    // Print immediately for real-time debugging
    println(formatLine(entry))
  }

  // mimic android's open-source logcat format for easy parsing
  private fun formatLine(entry: LogEntry): String {
    val ts = timestampFormatter.format(entry.timestamp)
    return "$ts ${entry.level.letter}/${entry.tag}: ${entry.message}"
  }

  override suspend fun interact() {
    // Quick exit if nothing to do
    if (queue.isEmpty()) return

    // Ensure only one writer at a time and run IO on IO dispatcher
    withContext(Dispatchers.IO) {
      writeMutex.withLock {
        // Drain queue into list
        val drained = ArrayList<LogEntry>()
        while (true) {
          val e = queue.poll() ?: break
          drained.add(e)
        }
        if (drained.isEmpty()) return@withLock

        // Sort by timestamp then sequence to ensure stable order
        drained.sortWith(compareBy({ it.timestamp }, { it.seq }))

        // Ensure directory exists
        Files.createDirectories(loggingFilePath.parent)

        // Append lines to file
        Files.newBufferedWriter(
          loggingFilePath,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND
        ).use { writer ->
          for (entry in drained) {
            writer.write(formatLine(entry))
            writer.newLine()
          }
          writer.flush()
        }
      }
    }
  }

  enum class Level(val letter: Char) {
    VERBOSE('V'),
    DEBUG('D'),
    INFO('I'),
    WARN('W'),
    ERROR('E')
  }

  data class LogEntry(
    val timestamp: Instant,
    val level: Level,
    val tag: String,
    val message: String,
    val seq: Long
  )
}

