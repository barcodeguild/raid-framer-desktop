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

}

enum class RFGraphColor(val color: Color) {
  RED(Color.Red),
  GREEN(Color.Green),
  BLUE(Color.Blue),
  YELLOW(Color.Yellow),
  CYAN(Color.Cyan),
  MAGENTA(Color.Magenta),
  ORANGE(Color(0xFFFFA500))
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