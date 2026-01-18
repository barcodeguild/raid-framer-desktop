package com.reoky.raidframer.core.helpers

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

object FontsHelper {
  // In Desktop, simply pass the resource path relative to the resources directory.
  // Ensure "fonts" is a folder inside "src/desktopMain/resources" or "src/commonMain/resources".
  val FaSolid: FontFamily by lazy {
    FontFamily(Font(resource = "fonts/Font Awesome 7 Free-Solid-900.otf"))
  }
  // God, I love font awesome
  val FaRegular: FontFamily by lazy {
    FontFamily(Font(resource = "fonts/Font Awesome 7 Free-Regular-400.otf"))
  }

  val ARKorean: FontFamily by lazy {
    FontFamily(Font(resource = "fonts/ARKorean-Regular.ttf"))
  }
}

/** Render a Font Awesome glyph by Unicode codepoint (e.g. "\uF0C0"). */
@Composable
fun FaIcon(codepoint: String, useSolid: Boolean = true, sizeSp: Int = 20) {
  val family = if (useSolid) FontsHelper.FaSolid else FontsHelper.FaRegular
  Text(text = codepoint, style = TextStyle(fontFamily = family, fontSize = sizeSp.sp))
}

