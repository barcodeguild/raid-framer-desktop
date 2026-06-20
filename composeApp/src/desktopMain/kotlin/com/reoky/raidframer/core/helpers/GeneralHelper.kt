package com.reoky.raidframer.core.helpers

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.HealEvent
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.exists
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.ranges.contains

/**
 * Extension function to abbreviate a numeric values by thousands, millions, billions, etc.
 */
fun Long.humanReadableAbbreviation(): String {
  if (this in -999..999) return this.toString() // No abbreviation needed cause we're under 1,000
  val absVal = abs(this.toDouble())
  val exp = (ln(absVal) / ln(1000.0)).toInt().coerceAtLeast(1)
  val suffixes = "kMGTPE"
  val idx = (exp - 1).coerceAtMost(suffixes.lastIndex)
  val scaled = this / 1000.0.pow(exp.toDouble())
  return String.format("%.1f%c", scaled, suffixes[idx])
}

/**
 * Find the Documents path. Blame Microsoft for having OneDrive mount the user's Documents inside of OneDrive
 */
fun getDocumentsDirectory(): String? {
  if (System.getProperty("os.name").lowercase().contains("win")) {
    val userProfile = System.getenv("USERPROFILE")
    if (!userProfile.isNullOrBlank()) {
      val oneDriveDocs = Paths.get(userProfile, "OneDrive", "Documents")
      if (oneDriveDocs.exists()) return oneDriveDocs.toString()
      val regularDocs = Paths.get(userProfile, "Documents")
      if (regularDocs.exists()) return regularDocs.toString()
    }
  }
  val home = System.getProperty("user.home") ?: return null
  return Paths.get(home, "Documents").toString()
}

fun getExportDirectory(): String? {
  val documentsDir = getDocumentsDirectory() ?: return null
  return Paths.get(documentsDir, "RFExports").toString()
}

fun getDirectorySizeBytes(directoryPath: String): Long {
  return try {
    val directory = File(directoryPath)
    if (!directory.exists()) return 0L
    directory.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
  } catch (_: Exception) {
    0L
  }
}

fun formatFileSize(bytes: Long): String {
  if (bytes < 1024L) return "$bytes B"
  if (bytes < 1024L * 1024L) return "%.1f KB".format(bytes / 1024.0)
  if (bytes < 1024L * 1024L * 1024L) return "%.1f MB".format(bytes / (1024.0 * 1024.0))
  return "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}


/**
 * Opens a browser window to the specified URL.
 */
fun openWebLink(url: String) {
  if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
    Desktop.getDesktop().browse(URI(url))
  }
}

/**
 * Simple extension function to convert epoch milliseconds to a local time string.
 */
fun Long.toLocalTimeString(): String {
  val instant = Instant.ofEpochMilli(this)
  val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
  return localDateTime.format(DateTimeFormatter.ofPattern("hh:mm:ss"))
}

/**
 * Makes a pretty-looking AnnotatedString for the attack event. I brought this code into a different file because
 * it's too and will get used multiple times potentially.
 */
@Composable
fun annotatedStringForAttack(event: DamageEvent): AnnotatedString {
  return buildAnnotatedString {
    withStyle(style = SpanStyle(color = Color(249, 191, 59))) {
      append(event.source)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" attacked ")
    }
    withStyle(style = SpanStyle(color = Color(249, 191, 59))) {
      append(event.target)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" with ")
    }
    withStyle(style = SpanStyle(color = Color.Red)) {
      append(event.spell)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" to deal ")
    }
    withStyle(style = SpanStyle(color = Color(249, 191, 59))) {
      append("${event.damage}")
    }
    withStyle(style = if (event.critical) SpanStyle(color = Color.Red) else SpanStyle(color = Color.White)) {
      append(if (event.critical) " critical damage!" else " damage.")
    }
    withStyle(style = SpanStyle(color = Color.Magenta)) {
      append("(${event.timestamp.toLocalTimeString()})")
    }
  }
}

/*
 * Makes a pretty-looking AnnotatedString for the heal events.
 */
@Composable
fun annotatedStringForHeal(event: HealEvent): AnnotatedString {
  return buildAnnotatedString {
    withStyle(style = SpanStyle(color = Color(249, 191, 59))) {
      append(event.target)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" was healed by ")
    }
    withStyle(style = SpanStyle(color = Color(249, 191, 59))) {
      append(event.source)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" using ")
    }
    withStyle(style = SpanStyle(color = Color.Green)) {
      append(event.spell)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" to restore ")
    }
    withStyle(style = SpanStyle(color = Color.Green)) {
      append("${event.amount}")
    }
    withStyle(style = if (event.critical) SpanStyle(color = Color.Green) else SpanStyle(color = Color.White)) {
      append(if (event.critical) " (critical!) " else " ")
    }
    withStyle(style = SpanStyle(color = Color.Magenta)) {
      append("(${event.timestamp.toLocalTimeString()})")
    }
  }
}
