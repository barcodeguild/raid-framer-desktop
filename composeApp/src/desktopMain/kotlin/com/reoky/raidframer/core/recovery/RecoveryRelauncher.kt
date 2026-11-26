package com.reoky.raidframer.core.recovery

import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.interactor.LoggingInteractor
import com.reoky.raidframer.messageBox
import com.reoky.raidframer.quit
import java.nio.file.Paths

/*
 * Spawns a fresh copy of the app so recovery mode can hand control back to the real
 * application, and so Settings can send the user into recovery mode on purpose.
 */
object RecoveryRelauncher {

  private const val TAG = "RecoveryRelauncher"

  /**
   * Spawns a new instance of the app with [extraArgs] and returns true when the
   * process appears to have started. Best effort: when the launcher command can't be
   * resolved (for example when running from Gradle), the caller should exit and let
   * the user reopen the app manually.
   */
  fun spawn(extraArgs: List<String> = emptyList()): Boolean {
    val command = ProcessHandle.current().info().command().orElse(null) ?: return false
    // A plain JVM (running from Gradle) can't be relaunched by command line alone.
    val fileName = Paths.get(command).fileName?.toString()?.lowercase().orEmpty()
    if (fileName.startsWith("java")) return false
    return try {
      ProcessBuilder(listOf(command) + extraArgs).start()
      LoggingInteractor.info(TAG, "Relaunched app: $command ${extraArgs.joinToString(" ")}")
      true
    } catch (e: Exception) {
      LoggingInteractor.error(TAG, "Relaunch failed ($command): ${e.message}")
      false
    }
  }

  /** Closes the app and reopens it normally. Used by the recovery window's launch button. */
  fun relaunchNormally() {
    if (!spawn()) {
      messageBox(AppGlobals.APP_NAME, "Raid Framer will start normally the next time it is opened.")
    }
    quit()
  }

  /** Closes the app and reopens it directly in recovery mode. Used by the Settings button. */
  fun relaunchIntoRecovery() {
    // The marker file guarantees recovery mode even if the relaunch never comes up.
    RecoveryMode.writeMarkerFile()
    if (!spawn(listOf(RecoveryMode.LAUNCH_ARG))) {
      messageBox(AppGlobals.APP_NAME, "Raid Framer will open in recovery mode the next time it is started.")
    }
    quit()
  }
}
