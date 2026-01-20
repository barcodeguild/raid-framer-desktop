package com.reoky.raidframer.core.helpers

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.arkorean_regular
import raid_framer_desktop.composeapp.generated.resources.fa_regular_400
import raid_framer_desktop.composeapp.generated.resources.fa_solid_900

/**
 * Helper object to load custom fonts for use in Compose UI. This also has the same Korean font that the game uses, or
 * at least a very close match to it and the free font awesome icons.
 */
object FontsHelper {

  @Composable
  fun faSolid(): FontFamily = FontFamily(Font(Res.font.fa_solid_900))

  @Composable
  fun faRegular(): FontFamily = FontFamily(Font(Res.font.fa_regular_400))

  @Composable
  fun arKorean(): FontFamily = FontFamily(Font(Res.font.arkorean_regular))
}

@Composable
fun FaIcon(codepoint: String, useSolid: Boolean = true, sizeSp: Int = 20) {
  // We call the Composable font loaders here
  val family = if (useSolid) FontsHelper.faSolid() else FontsHelper.faRegular()
  Text(text = codepoint, style = TextStyle(fontFamily = family, fontSize = sizeSp.sp))
}