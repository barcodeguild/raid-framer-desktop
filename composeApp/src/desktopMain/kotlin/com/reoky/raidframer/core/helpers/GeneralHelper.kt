package com.reoky.raidframer.core.helpers

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.reoky.raidframer.AppState
import com.reoky.raidframer.OverlayNav
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.HealEvent
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.graphs.GraphMetricType
import com.reoky.raidframer.ui.overlay.RaidTab
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.time_ago_just_now
import raid_framer_desktop.composeapp.generated.resources.time_ago_minutes_one
import raid_framer_desktop.composeapp.generated.resources.time_ago_minutes_other
import raid_framer_desktop.composeapp.generated.resources.time_ago_hours_one
import raid_framer_desktop.composeapp.generated.resources.time_ago_hours_other
import raid_framer_desktop.composeapp.generated.resources.time_ago_days_one
import raid_framer_desktop.composeapp.generated.resources.time_ago_days_other
import raid_framer_desktop.composeapp.generated.resources.time_ago_weeks_one
import raid_framer_desktop.composeapp.generated.resources.time_ago_weeks_other
import raid_framer_desktop.composeapp.generated.resources.time_ago_months_one
import raid_framer_desktop.composeapp.generated.resources.time_ago_months_other
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
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
 * Formats a duration in milliseconds as a compact human-readable string using the most sensible
 * unit, e.g. "59s", "1min 2s", "10min 2s", "1h 2min". Seconds-only when under a minute.
 */
fun Long.toHumanDuration(): String {
  val totalSeconds = this / 1000
  val hours   = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60
  return when {
    hours > 0 -> "${hours}h ${minutes}min"
    minutes > 0 -> "${minutes}min ${seconds}s"
    else -> "${seconds}s"
  }
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

/** Root directory for application-owned files that should not live beside the installed app. */
fun getRaidFramerDirectory(): Path? {
  val home = System.getProperty("user.home") ?: return null
  return Paths.get(home, ".RaidFramer")
}

/** Root directory for locally persisted Pocket journal entries. */
fun getPocketJournalDirectory(createdAt: Long, entryId: String): Path? {
  val root = getRaidFramerDirectory() ?: return null
  val date = Instant.ofEpochMilli(createdAt)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()
  return root.resolve("journals")
    .resolve(date.year.toString())
    .resolve("%02d".format(date.monthValue))
    .resolve(entryId)
}

/** Creates an application directory and returns it, or null when the user home is unavailable. */
fun ensureRaidFramerDirectory(): Path? {
  return getRaidFramerDirectory()?.also { Files.createDirectories(it) }
}

/** Writes UTF-8 text through a sibling temporary file before replacing the destination. */
fun writeTextAtomically(path: Path, text: String) {
  Files.createDirectories(path.parent)
  val temporary = path.resolveSibling(".${path.fileName}.tmp")
  Files.write(
    temporary,
    text.toByteArray(Charsets.UTF_8),
    StandardOpenOption.CREATE,
    StandardOpenOption.TRUNCATE_EXISTING,
    StandardOpenOption.WRITE
  )
  Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
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
 * Represents a unit of time for the "time ago" display.
 */
enum class TimeAgoUnit {
  JUST_NOW,
  MINUTE,
  HOUR,
  DAY,
  WEEK,
  MONTH
}

/**
 * Structured result from [timeAgo] containing the numeric value and unit.
 * The caller resolves the actual display string via string resources.
 */
data class TimeAgoResult(val value: Long, val unit: TimeAgoUnit)

/**
 * Returns a [TimeAgoResult] representing the relative time since the given epoch millisecond timestamp.
 * The caller should resolve the display string using the appropriate string resources.
 */
fun Long.timeAgo(): TimeAgoResult {
  val now = System.currentTimeMillis()
  val diff = now - this
  if (diff < 0) return TimeAgoResult(0, TimeAgoUnit.JUST_NOW)

  val seconds = diff / 1000
  val minutes = seconds / 60
  val hours = minutes / 60
  val days = hours / 24
  val weeks = days / 7
  val months = days / 30

  return when {
    seconds < 60 -> TimeAgoResult(0, TimeAgoUnit.JUST_NOW)
    minutes < 60 -> TimeAgoResult(minutes, TimeAgoUnit.MINUTE)
    hours < 24 -> TimeAgoResult(hours, TimeAgoUnit.HOUR)
    days < 7 -> TimeAgoResult(days, TimeAgoUnit.DAY)
    weeks < 5 -> TimeAgoResult(weeks, TimeAgoUnit.WEEK)
    else -> TimeAgoResult(months, TimeAgoUnit.MONTH)
  }
}

/**
 * Resolves a [TimeAgoResult] to a localized string using string resources.
 */
@Composable
fun TimeAgoResult.resolveLocalizedString(): String {
  return when (unit) {
    TimeAgoUnit.JUST_NOW -> stringResource(Res.string.time_ago_just_now)
    TimeAgoUnit.MINUTE -> {
      val res = if (value == 1L) Res.string.time_ago_minutes_one else Res.string.time_ago_minutes_other
      stringResource(res, value)
    }
    TimeAgoUnit.HOUR -> {
      val res = if (value == 1L) Res.string.time_ago_hours_one else Res.string.time_ago_hours_other
      stringResource(res, value)
    }
    TimeAgoUnit.DAY -> {
      val res = if (value == 1L) Res.string.time_ago_days_one else Res.string.time_ago_days_other
      stringResource(res, value)
    }
    TimeAgoUnit.WEEK -> {
      val res = if (value == 1L) Res.string.time_ago_weeks_one else Res.string.time_ago_weeks_other
      stringResource(res, value)
    }
    TimeAgoUnit.MONTH -> {
      val res = if (value == 1L) Res.string.time_ago_months_one else Res.string.time_ago_months_other
      stringResource(res, value)
    }
  }
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

/**
 * Toggles the player card overlay: closes it if the same player is already selected and the card is open,
 * otherwise opens it with the given player (and optional metric type).
 */
fun togglePlayerCard(wm: WindowManager?, playerName: String, metricType: GraphMetricType? = null) {
  val isAlreadyOpen = wm?.isVisible(OverlayType.PLAYER_CARD)?.value == true
  val isSamePlayer = AppState.selectedPlayer.value == playerName
  if (isAlreadyOpen && isSamePlayer) {
    wm?.closeWindow(OverlayType.PLAYER_CARD)
  } else {
    AppState.selectPlayer(playerName)
    if (metricType != null) AppState.selectMetricType(metricType)
    wm?.openWindow(OverlayType.PLAYER_CARD)
  }
}

/**
 * Opens the Raid overlay with the given [tab] selected. When [highlightBuffSelect] is true, the
 * Buffs tab's buff-selection pane flashes to draw the user's attention to it.
 */
fun openRaidTab(wm: WindowManager?, tab: RaidTab, highlightBuffSelect: Boolean = false) {
  OverlayNav.pendingRaidTab.value = tab
  OverlayNav.highlightRaidBuffSelect.value = highlightBuffSelect
  wm?.openWindow(OverlayType.RAID)
}

/**
 * Opens the Summary overlay with the dropdown index (category) [index] selected.
 */
fun openSummaryTab(wm: WindowManager?, index: Int) {
  OverlayNav.pendingSummaryTabIndex.value = index
  wm?.openWindow(OverlayType.SUMMARY)
}

/**
 * Opens the Settings overlay, scrolling to and flashing the General Settings section.
 */
fun openSettingsGeneral(wm: WindowManager?) {
  OverlayNav.highlightSettingsGeneral.value = true
  wm?.openWindow(OverlayType.SETTINGS)
}

/**
 * Opens the editable Meta Specs overlay.
 */
fun openMetaSpecs(wm: WindowManager?) {
  wm?.openWindow(OverlayType.META_SPECS)
}

/**
 * Toggles the pet (Pokemon) overlay open/closed.
 */
fun togglePokemon(wm: WindowManager?) {
  if (wm?.isVisible(OverlayType.POKEMON)?.value == true) {
    wm.closeWindow(OverlayType.POKEMON)
  } else {
    wm?.openWindow(OverlayType.POKEMON)
  }
}
