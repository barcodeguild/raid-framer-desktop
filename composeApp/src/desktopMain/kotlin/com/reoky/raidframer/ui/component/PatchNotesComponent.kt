package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.helpers.RFColors
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.settings_update_whats_new

@Composable
fun PatchNotesComponent(releaseNotes: String) {
  if (releaseNotes.isBlank()) return

  Spacer(modifier = Modifier.height(8.dp))
  Text(
    text = stringResource(Res.string.settings_update_whats_new),
    color = RFColors.TextSecondary,
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold
  )
  Spacer(modifier = Modifier.height(4.dp))
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(max = 150.dp)
      .background(RFColors.BadgeBackground, RoundedCornerShape(6.dp))
      .padding(8.dp)
  ) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.verticalScroll(scrollState)) {
      releaseNotes.lines().forEach { line ->
        val trimmed = line.trim()
        when {
          trimmed.isEmpty() -> {
            Spacer(modifier = Modifier.height(4.dp))
          }
          trimmed.startsWith("- ") -> {
            val content = trimmed.removePrefix("- ")
            Text(
              text = buildAnnotatedString {
                append("  \u2022  ")
                appendMarkdownBold(content)
              },
              color = RFColors.TextPrimary,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
          else -> {
            Text(
              text = buildAnnotatedString { appendMarkdownBold(trimmed) },
              color = RFColors.TextPrimary,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
        }
      }
    }
  }
}

private fun AnnotatedString.Builder.appendMarkdownBold(text: String) {
  var remaining = text
  while (remaining.contains("**")) {
    val boldStart = remaining.indexOf("**")
    val boldEnd = remaining.indexOf("**", boldStart + 2)
    if (boldEnd == -1) {
      append(remaining)
      remaining = ""
      break
    }
    append(remaining.substring(0, boldStart))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
      append(remaining.substring(boldStart + 2, boldEnd))
    }
    remaining = remaining.substring(boldEnd + 2)
  }
  append(remaining)
}
