package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.CheckBoxComponent
import com.reoky.raidframer.ui.component.TitleBarComponent
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.companion_description
import raid_framer_desktop.composeapp.generated.resources.companion_toggl_display_mark_hvt_healers
import raid_framer_desktop.composeapp.generated.resources.companion_toggle
import raid_framer_desktop.composeapp.generated.resources.companion_toggle_display_charmed
import raid_framer_desktop.composeapp.generated.resources.companion_toggle_display_mark_hvt_cc
import raid_framer_desktop.composeapp.generated.resources.companion_toggle_display_mark_hvt_dps
import raid_framer_desktop.composeapp.generated.resources.companion_toggle_display_silenced
import raid_framer_desktop.composeapp.generated.resources.companion_toggle_raid_status

@Preview
@Composable
fun CompanionOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    CompanionOverlay()
  }
}

@Composable
fun CompanionOverlay(wm: WindowManager? = null) {
  Box(modifier = Modifier.fillMaxSize()) {
    Column {
        TitleBarComponent("Lua Companion Management", onClose = { wm?.closeWindow(OverlayType.COMPANION) })
        Spacer(modifier = Modifier.height(8.dp))

      // lua + kotlin row
      Row {

        val luaPainter = painterResource("svg/lua.svg")
        val kotlinPainter = painterResource("svg/kotlin.svg")

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Image(
            painter = luaPainter,
            contentDescription = "Lua logo",
            modifier = Modifier.size(92.dp)
          )

          Text(
            text = "+",
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 64.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
          )

          Image(
            painter = kotlinPainter,
            contentDescription = "Kotlin logo",
            modifier = Modifier.size(128.dp)
          )
        }
      } // end lua + kotlin row

      Row {
        Text(
          text = stringResource(Res.string.companion_description, AppGlobals.APP_NAME),
          color = Color.White,
          modifier = Modifier.padding(horizontal = 12.dp)
        )
      }

      Divider(color = Color.White, thickness = 1.dp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

      Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        // main toggle
        Row {
          CheckBoxComponent(
            label = stringResource(Res.string.companion_toggle),
            initialChecked = false,
            onCheckedChange = { enabled ->


            }
          )
        }

        // chat display raid stats toggle
        Row {
          CheckBoxComponent(
            label = stringResource(Res.string.companion_toggle_raid_status),
            initialChecked = false,
            onCheckedChange = { enabled ->

            }
          )
        }

        // chat charmed toggle
        Row {
          CheckBoxComponent(
            label = stringResource(Res.string.companion_toggle_display_charmed),
            initialChecked = false,
            onCheckedChange = { enabled ->

            }
          )
        }

        // chat silenced toggle
        Row {
          CheckBoxComponent(
            label = stringResource(Res.string.companion_toggle_display_silenced),
            initialChecked = false,
            onCheckedChange = { enabled ->

            }
          )
        }

        // mark enemy healers
        Row {
          CheckBoxComponent(
            label = stringResource(Res.string.companion_toggl_display_mark_hvt_healers),
            initialChecked = false,
            onCheckedChange = { enabled ->

            }
          )
        }

        // mark enemy dps
        Row {
          CheckBoxComponent(
            label = stringResource(Res.string.companion_toggle_display_mark_hvt_dps),
            initialChecked = false,
            onCheckedChange = { enabled ->

            }
          )
        }

        // mark enemy cc
        Row {
          CheckBoxComponent(
            label = stringResource(Res.string.companion_toggle_display_mark_hvt_cc),
            initialChecked = false,
            onCheckedChange = { enabled ->

            }
          )
        }

        // mark sac dancers

        // enemy cc (purple = charmed, black = silenced)
        Row {
          CheckBoxComponent(
            label = "Apply CC dots over players heads to indicate charmed / silenced status. (Slightly more visible than default game effect.)",
            initialChecked = false,
            onCheckedChange = { enabled ->

            }
          )
        }
      }

    }
  }
}
