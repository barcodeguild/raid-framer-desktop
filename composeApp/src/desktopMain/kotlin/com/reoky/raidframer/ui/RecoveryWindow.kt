package com.reoky.raidframer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.showFolderInExplorer
import com.reoky.raidframer.core.interactor.Log
import com.reoky.raidframer.core.interactor.LoggingInteractor
import com.reoky.raidframer.core.recovery.RecoveryBackup
import com.reoky.raidframer.core.recovery.RecoveryMode
import com.reoky.raidframer.core.recovery.RecoveryRelauncher
import com.reoky.raidframer.ui.dialog.RFDialog
import com.reoky.raidframer.quit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.general_cancel
import raid_framer_desktop.composeapp.generated.resources.recovery_backup_hint
import raid_framer_desktop.composeapp.generated.resources.recovery_backup_label
import raid_framer_desktop.composeapp.generated.resources.recovery_confirm_button
import raid_framer_desktop.composeapp.generated.resources.recovery_confirm_text
import raid_framer_desktop.composeapp.generated.resources.recovery_confirm_title
import raid_framer_desktop.composeapp.generated.resources.recovery_error_detail_format
import raid_framer_desktop.composeapp.generated.resources.recovery_error_headline
import raid_framer_desktop.composeapp.generated.resources.recovery_error_reassurance
import raid_framer_desktop.composeapp.generated.resources.recovery_launch_button
import raid_framer_desktop.composeapp.generated.resources.recovery_manual_headline
import raid_framer_desktop.composeapp.generated.resources.recovery_manual_reassurance
import raid_framer_desktop.composeapp.generated.resources.recovery_reset_button
import raid_framer_desktop.composeapp.generated.resources.recovery_reset_complete
import raid_framer_desktop.composeapp.generated.resources.recovery_reset_failed_format
import raid_framer_desktop.composeapp.generated.resources.recovery_reset_failed_lock_hint
import raid_framer_desktop.composeapp.generated.resources.recovery_resetting
import raid_framer_desktop.composeapp.generated.resources.recovery_title
import raid_framer_desktop.composeapp.generated.resources.recovery_view_log
import java.awt.Desktop
import java.nio.file.Path

private const val TAG = "RecoveryWindow"

private enum class RecoveryPhase { IDLE, RESETTING, DONE, FAILED }

/*
 * The singular recovery window. While recovery mode is active this is the entire app:
 * a barebones UI with just the logging service running, never the database. Closing
 * the window exits the app; "Launch App" hands control back to the real application.
 * The window is always on top because this app is a game overlay — in a migration
 * failure the game is typically fullscreen, and a regular window would open behind it
 * where the user would never find it.
 */
@Composable
fun RecoveryWindow(reason: RecoveryMode.Reason, error: Throwable?) {

  // Recovery mode keeps the logging service alive (without truncating the log, unlike
  // a normal launch) so recovery actions are recorded. Nothing else is started.
  LaunchedEffect(Unit) {
    if (!LoggingInteractor.isRunning()) LoggingInteractor.start(delay = 1000L)
    Log.info(TAG, "Recovery mode active (${reason.name})")
  }

  var phase by remember { mutableStateOf(RecoveryPhase.IDLE) }
  var backupDirectory by remember { mutableStateOf<Path?>(null) }
  var failureMessage by remember { mutableStateOf<String?>(null) }
  var showResetConfirm by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  // What happened and what the user can do about it, per entry reason.
  val migrationFailure = reason == RecoveryMode.Reason.MIGRATION_FAILURE
  val headline = stringResource(
    if (migrationFailure) Res.string.recovery_error_headline else Res.string.recovery_manual_headline
  )
  val reassurance = stringResource(
    if (migrationFailure) Res.string.recovery_error_reassurance else Res.string.recovery_manual_reassurance
  )

  Window(
    onCloseRequest = { exitRecovery() },
    title = stringResource(Res.string.recovery_title),
    state = rememberWindowState(
      width = 560.dp,
      height = 520.dp,
      position = WindowPosition(alignment = Alignment.Center)
    ),
    alwaysOnTop = true,
    resizable = false
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(RFColors.OverlayBackground)
        .padding(24.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "⚠", color = RFColors.AccentRed, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(Res.string.recovery_title),
          color = RFColors.TextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = headline,
        color = RFColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 19.sp
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = reassurance,
        color = RFColors.TextSecondary,
        fontSize = 13.sp,
        lineHeight = 18.sp
      )

      // What actually went wrong, when the app entered recovery on its own.
      if (error != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = stringResource(
            Res.string.recovery_error_detail_format,
            error.message ?: error::class.java.simpleName
          ),
          color = RFColors.TextTertiary,
          fontSize = 11.sp,
          lineHeight = 15.sp,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis
        )
      }

      // Progress and results, grouped in a card like the app's settings panels.
      if (phase != RecoveryPhase.IDLE) {
        Spacer(modifier = Modifier.height(20.dp))
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          color = RFColors.CardBackground,
          elevation = 2.dp
        ) {
          Column(
            modifier = Modifier
              .border(1.dp, RFColors.CardBorder, RoundedCornerShape(10.dp))
              .padding(16.dp)
          ) {
            when (phase) {
              RecoveryPhase.IDLE -> {}
              RecoveryPhase.RESETTING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = RFColors.AccentRed,
                    strokeWidth = 2.dp
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = stringResource(Res.string.recovery_resetting),
                    color = RFColors.TextSecondary,
                    fontSize = 13.sp
                  )
                }
              }
              RecoveryPhase.DONE -> {
                Text(
                  text = stringResource(Res.string.recovery_reset_complete),
                  color = RFColors.UpdateGreen,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = stringResource(Res.string.recovery_backup_label),
                  color = RFColors.TextSecondary,
                  fontSize = 13.sp
                )
                // The backup folder only exists after the reset, so the link is shown here.
                backupDirectory?.let { path ->
                  Text(
                    text = path.toAbsolutePath().toString(),
                    color = RFColors.LinkBlue,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                      .clickable {
                        Log.info(TAG, "Opening backup folder in explorer: $path")
                        showFolderInExplorer(path)
                      }
                      .padding(vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = stringResource(Res.string.recovery_backup_hint),
                  color = RFColors.TextTertiary,
                  fontSize = 11.sp,
                  lineHeight = 15.sp
                )
              }
              RecoveryPhase.FAILED -> {
                Text(
                  text = stringResource(
                    Res.string.recovery_reset_failed_format,
                    failureMessage.orEmpty()
                  ),
                  color = RFColors.AccentRed,
                  fontSize = 13.sp,
                  lineHeight = 18.sp
                )
                // A sharing violation means something else still holds the database
                // file; tell the user what to check instead of leaving a raw error.
                if (failureMessage?.contains("another process", ignoreCase = true) == true) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = stringResource(Res.string.recovery_reset_failed_lock_hint),
                    color = RFColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Actions follow the app's dialog convention: neutral dismissal on the left, the
      // red confirming action on the right, both anchored to the window's trailing edge.
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = { launchApp() },
          enabled = phase != RecoveryPhase.RESETTING,
          colors = ButtonDefaults.buttonColors(RFColors.CardBorder)
        ) {
          Text(
            text = stringResource(Res.string.recovery_launch_button),
            color = RFColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
          onClick = { showResetConfirm = true },
          enabled = phase == RecoveryPhase.IDLE || phase == RecoveryPhase.FAILED,
          colors = ButtonDefaults.buttonColors(
            backgroundColor = RFColors.AccentRed,
            contentColor = Color.White,
            disabledBackgroundColor = RFColors.CardBackground,
            disabledContentColor = RFColors.TextDisabled
          )
        ) {
          Text(
            text = stringResource(Res.string.recovery_reset_button),
            color = Color.White,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      // Minimal bottom menu: view the log in Notepad.
      Text(
        text = stringResource(Res.string.recovery_view_log),
        color = RFColors.TextTertiary,
        fontSize = 12.sp,
        modifier = Modifier
          .clickable { openLogInNotepad() }
          .padding(vertical = 4.dp)
      )
    }

    if (showResetConfirm) {
      RFDialog(
        title = stringResource(Res.string.recovery_confirm_title),
        text = stringResource(Res.string.recovery_confirm_text),
        onDismissRequest = { showResetConfirm = false },
        confirmLabel = stringResource(Res.string.recovery_confirm_button),
        dismissLabel = stringResource(Res.string.general_cancel),
        onConfirm = {
          showResetConfirm = false
          phase = RecoveryPhase.RESETTING
          performReset(scope) { result, failure ->
            if (failure != null) {
              failureMessage = failure
              phase = RecoveryPhase.FAILED
            } else {
              backupDirectory = result
              phase = RecoveryPhase.DONE
            }
          }
        }
      )
    }
    }
  }
}

/** Runs the backup on IO; the window shows progress and reports the backup folder when done. */
private fun performReset(
  scope: CoroutineScope,
  onResult: (Path?, String?) -> Unit
) {
  scope.launch(Dispatchers.IO) {
    try {
      val result = RecoveryBackup.reset()
      onResult(result.backupDirectory, null)
    } catch (e: Throwable) {
      Log.error(TAG, e, "Database reset failed")
      onResult(null, e.message ?: e::class.java.simpleName)
    }
  }
}

/** Exits the app cleanly; the user is expected to relaunch Raid Framer themselves. */
private fun exitRecovery() {
  LoggingInteractor.flush()
  quit()
}

/** Hands control back to the real application via a fresh process. */
private fun launchApp() {
  LoggingInteractor.flush()
  RecoveryRelauncher.relaunchNormally()
}

/** Opens the debug log in Notepad, falling back to the system default editor. */
private fun openLogInNotepad() {
  val path = RecoveryMode.debugLogPath() ?: return
  try {
    ProcessBuilder("notepad.exe", path.toAbsolutePath().toString()).start()
  } catch (_: Exception) {
    runCatching { Desktop.getDesktop().open(path.toFile()) }
  }
}
