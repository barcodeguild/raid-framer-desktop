package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.help_before_installing_body
import raid_framer_desktop.composeapp.generated.resources.help_before_installing_title
import raid_framer_desktop.composeapp.generated.resources.help_install_body
import raid_framer_desktop.composeapp.generated.resources.help_install_title
import raid_framer_desktop.composeapp.generated.resources.help_performance_body
import raid_framer_desktop.composeapp.generated.resources.help_performance_title
import raid_framer_desktop.composeapp.generated.resources.help_recording_body
import raid_framer_desktop.composeapp.generated.resources.help_recording_title
import raid_framer_desktop.composeapp.generated.resources.help_resetting_body
import raid_framer_desktop.composeapp.generated.resources.help_resetting_title
import raid_framer_desktop.composeapp.generated.resources.help_title
import raid_framer_desktop.composeapp.generated.resources.help_troubleshooting_body
import raid_framer_desktop.composeapp.generated.resources.help_troubleshooting_title
import raid_framer_desktop.composeapp.generated.resources.help_troubleshooting_addon_line
import raid_framer_desktop.composeapp.generated.resources.help_updating_body
import raid_framer_desktop.composeapp.generated.resources.help_updating_title

@Composable
fun HelpOverlay(wm: WindowManager? = null) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF121212))
      .verticalScroll(rememberScrollState())
  ) {
    TitleBarComponent(
      title = stringResource(Res.string.help_title),
      onClose = { wm?.closeWindow(OverlayType.HELP) }
    )
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      HelpSection(Res.string.help_before_installing_title, Res.string.help_before_installing_body)
      HelpSection(Res.string.help_install_title, Res.string.help_install_body)
      HelpSection(Res.string.help_recording_title, Res.string.help_recording_body)
      HelpSection(Res.string.help_troubleshooting_title, Res.string.help_troubleshooting_body)
      HelpSection(Res.string.help_updating_title, Res.string.help_updating_body)
      HelpSection(Res.string.help_resetting_title, Res.string.help_resetting_body)
      HelpSection(Res.string.help_performance_title, Res.string.help_performance_body)
    }
  }
}

@Composable
private fun HelpSection(title: org.jetbrains.compose.resources.StringResource, body: org.jetbrains.compose.resources.StringResource) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = RFColors.CardBackground,
    elevation = 2.dp
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(stringResource(title), color = RFColors.AccentRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(8.dp))
      if (body == Res.string.help_troubleshooting_body) {
        Text(stringResource(body), color = RFColors.TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
              append(stringResource(Res.string.help_troubleshooting_addon_line))
            }
          },
          color = RFColors.TextPrimary,
          fontSize = 13.sp,
          lineHeight = 19.sp
        )
      } else {
        Text(stringResource(body), color = RFColors.TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
      }
    }
  }
}
