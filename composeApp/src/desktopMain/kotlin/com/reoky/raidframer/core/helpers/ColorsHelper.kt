package com.reoky.raidframer.core.helpers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.PlayerRole

/*
 * General colors to be used throughout the app.
 */
object RFColors {
  // Dark theme palette
  val CardBackground = Color(0xFF1A1A1A)
  val CardBorder = Color(0xFF2A2A2A) // Subtle lighter border
  val CardBorderAccent = Color(0xFF4A1A1A) // Very subtle red tint for hover/focus

  // Icon/Badge backgrounds
  val IconBackground = Color(0xFF2A1A1A)
  val IconBorder = Color(0xFF5A2020) // Muted red border
  val BadgeBackground = Color(0xFF2A2A2A)
  val DebuffBadgeBackground = Color(0xFF3A1A1A)

  // Text colors
  val TextPrimary = Color(0xFFE0E0E0)
  val TextSecondary = Color(0xFFB0B0B0)
  val TextTertiary = Color(0xFF8B8B8B)
  val TextDisabled = Color(0xFF5A5A5A)

  // Accent colors
  val AccentRed = Color(0xFFDC143C)
  val AccentRedMuted = Color(0xFFD08080)

  // Update / dialog colors
  val UpdateGreen = Color(0xFF66BB6A)
  val UpdateGold = Color(0xFFFFCA28)
  val LinkBlue = Color(0xFF64B5F6)

  val dpsOrange = Color(249, 191, 59, 255)
  val healsGreen = Color(105, 235, 113, 255)
  val ccCyan = Color.Cyan

  // Faction colors
  val factionHaranya = Color(0xFFAB47BC)
  val factionNuia = Color(0xFFEC407A)
  val factionPirate = Color(0xFF7E57C2)

  // Per-faction kill ranking colors
  val killsHaranyaGreen = Color(0xFF66BB6A)
  val killsNuiaOrange = Color(0xFFFFA726)
  val killsPirateRed = Color(0xFFEF5350)

  // Utility stat colors
  val potionTeal = Color(0xFF26A69A)
  val gliderBlue = Color(0xFF42A5F5)
  val itemSkillYellow = Color(0xFFFFCA28)

  // Custom category ranking colors
  val charmPink = Color(0xFFEC407A)
  val silencePurple = Color(0xFFAB47BC)
  val distressPurple = Color(0xFF7E57C2)

  // Gear score histogram gradient colors (low to high gear)
  val gearRed = Color(0xFFFF1744)
  val gearOrange = Color(0xFFFF9800)
  val gearYellow = Color(0xFFFFEB3B)
  val gearGreen = Color(0xFF8BC34A)
  val gearBlue = Color(0xFF03A9F4)
  val gearCyan = Color(0xFF00E5FF)
  val gearUnknown = Color(0xFF666666)
}

enum class RFGraphColor(val color: Color) {
  RED(Color.Red),
  GREEN(Color.Green),
  BLUE(Color.Blue),
  YELLOW(Color.Yellow),
  CYAN(Color.Cyan),
  MAGENTA(Color.Magenta),
  ORANGE(Color(249, 191, 59, 255))
}


/*
 * For portraying raid frames designed to match the look and feel of the game.
 */
object RaidColors {
  val Green = Color(0xFF91A660)
  val Pink = Color(0xFFD493D3)
  val Red = Color(0xFFA54851)
  val Purple = Color(0xFF8369CD)
  val Blue = Color(0xFF4F93C0)
  val FrameBorder = Color(0xFF3E3E3E)
  val ManaBarBlue = Color(0xFF204ABF)
}

/*
 * Get the highlight color associated with a player faction based on a given faction's perspective
 * of another player's faction.
 */
fun Faction.getFactionHighlightColor(faction: Faction): Color {
  return when (this) {
    Faction.HARANYA -> when (faction) {
      Faction.HARANYA -> Color(0xFF36F1CC)
      Faction.NUIA -> Color.Red.copy(alpha = 0.75f)
      Faction.PIRATE -> Color(0xFFE56CAB)
      Faction.UNKNOWN -> Color.Transparent
    }
    Faction.NUIA -> when (faction) {
      Faction.HARANYA -> Color.Red.copy(alpha = 0.75f)
      Faction.NUIA -> Color(0xFF36F1CC)
      Faction.PIRATE -> Color(0xFFE56CAB)
      Faction.UNKNOWN -> Color.Transparent
    }
    Faction.PIRATE -> when (faction) {
      Faction.HARANYA -> Color(0xFFE56CAB)
      Faction.NUIA -> Color.Red.copy(alpha = 0.75f)
      Faction.PIRATE -> Color.Yellow.copy(alpha = 0.75f)
      Faction.UNKNOWN -> Color.Transparent
    }
    Faction.UNKNOWN -> Color.Transparent
  }
}

/*
 * Get the raid frame color associated with a player role.
 */
fun PlayerRole.getRaidColor(): Color {
  return when (this) {
    PlayerRole.BLUE -> RaidColors.Blue
    PlayerRole.GREEN -> RaidColors.Green
    PlayerRole.PINK -> RaidColors.Pink
    PlayerRole.RED -> RaidColors.Red
    PlayerRole.PURPLE -> RaidColors.Purple
    PlayerRole.UNKNOWN -> RaidColors.FrameBorder
  }
}


/*
 * Pick the next color that is not already used from a predefined list randomly.
 */
fun pickNextColor(usedColors: Set<RFGraphColor>): RFGraphColor {
  val availableColors = RFGraphColor.entries
  val unusedColors = availableColors.filter { it !in usedColors }
  if (unusedColors.isNotEmpty()) {
    return unusedColors.random()
  }
  return RFGraphColor.ORANGE // default if all colors are used
}

/**
 * Maps a 0.0-1.0 value to a color spectrum: Black -> Hue Cycle -> White. This takes a slider float from 0f to 1f
 * and gibs a color from that.
 */
fun sliderValueToColor(value: Float): Int {
  return when {
    value <= 0f -> Color.Black.toArgb()
    value >= 1f -> Color.White.toArgb()
    value < 0.1f -> {
      // Smooth transition from Black to full saturation
      val v = value * 10f
      Color.hsv(0f, 1f, v).toArgb()
    }
    value > 0.9f -> {
      // Smooth transition from full saturation to White
      val v = (value - 0.9f) * 10f
      Color.hsv(0f, 1f - v, 1f).toArgb()
    }
    else -> {
      // Hue cycle
      val hue = ((value - 0.1f) / 0.8f) * 360f
      Color.hsv(hue, 1f, 1f).toArgb()
    }
  }
}

/**
 * Maps an ARGB color back to a 0.0-1.0 slider value.
 */
fun colorToSliderValue(colorInt: Int): Float {
  val c = Color(colorInt)
  val r = c.red
  val g = c.green
  val b = c.blue
  val max = maxOf(r, maxOf(g, b))
  val min = minOf(r, minOf(g, b))
  val delta = max - min
  val saturation = if (max == 0f) 0f else delta / max
  val value = max

  return when {
    max == 0f -> 0f
    max == 1f && saturation == 0f -> 1f
    saturation > 0.1f -> {
      val hue = if (delta == 0f) 0f else {
        val h = when {
          max == r -> (g - b) / delta
          max == g -> 2f + (b - r) / delta
          else -> 4f + (r - g) / delta
        }
        (h * 60f).let { if (it < 0) it + 360f else it }
      }
      (hue / 360f * 0.8f) + 0.1f
    }
    else -> value
  }
}
