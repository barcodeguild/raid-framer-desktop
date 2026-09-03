package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.helpers.getRaidFramerDirectory
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant

/** Installs process-wide handlers and writes fatal failures synchronously before exit. */
object CrashLogger {
  private const val TAG = "CrashLogger"
  private val fallbackPath = Path.of("debug.log")

  fun install() {
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      record("Uncaught exception on thread '${thread.name}'", throwable)
      defaultHandler?.uncaughtException(thread, throwable)
    }
  }

  fun record(message: String, throwable: Throwable) {
    val stackTrace = StringWriter().also { writer ->
      throwable.printStackTrace(PrintWriter(writer))
    }.toString()
    val entry = "${Instant.now()} E/$TAG: $message\n$stackTrace"

    try {
      val path = getRaidFramerDirectory()?.resolve("debug.log") ?: fallbackPath
      path.parent?.let(Files::createDirectories)
      Files.newBufferedWriter(
        path,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND
      ).use { writer ->
        writer.write(entry)
        writer.newLine()
        writer.flush()
      }
    } catch (loggingFailure: Throwable) {
      System.err.println(entry)
      System.err.println("Unable to write crash log: ${loggingFailure.message}")
    }
  }
}
