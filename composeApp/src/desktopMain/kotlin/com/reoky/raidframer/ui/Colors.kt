package com.reoky.raidframer.ui

import androidx.compose.ui.graphics.Color

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

  val dpsOrange = Color(249, 191, 59, 255)
  val healsGreen = Color(105, 235, 113, 255)
  val ccCyan = Color.Cyan
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
