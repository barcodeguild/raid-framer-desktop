package com.reoky.raidframer.ui.overlay

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.CloseButton
import org.jetbrains.compose.resources.painterResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.about_app_title
import raid_framer_desktop.composeapp.generated.resources.about_author_label
import raid_framer_desktop.composeapp.generated.resources.about_author_name
import raid_framer_desktop.composeapp.generated.resources.about_community_dedication
import raid_framer_desktop.composeapp.generated.resources.about_dev_dedication
import raid_framer_desktop.composeapp.generated.resources.about_dedication_text
import raid_framer_desktop.composeapp.generated.resources.about_go_to_settings_button
import raid_framer_desktop.composeapp.generated.resources.about_package_label
import raid_framer_desktop.composeapp.generated.resources.about_read_settings_instruction
import raid_framer_desktop.composeapp.generated.resources.about_source_label
import raid_framer_desktop.composeapp.generated.resources.about_thanks_label
import raid_framer_desktop.composeapp.generated.resources.about_thanks_location
import raid_framer_desktop.composeapp.generated.resources.catreo
import raid_framer_desktop.composeapp.generated.resources.haranyanseal
import raid_framer_desktop.composeapp.generated.resources.raidframer

@Preview
@Composable
fun PreviewAboutOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    AboutOverlay()
  }
}

@Composable
fun AboutOverlay(wm: WindowManager? = null) {
  Box(modifier = Modifier.fillMaxSize()) {

     CloseButton(
       onClose = { wm?.closeWindow(OverlayType.ABOUT) },
       modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
     )

    // content area
    Column(
      modifier = Modifier.fillMaxSize().padding(12.dp)
    ) {

      // Header Logo and Version
      Row {
        Column(modifier = Modifier.weight(0.33f)) {
          val image = painterResource(Res.drawable.raidframer)
          Image(
            painter = image,
            contentDescription = "Raid Framer Icon",
            modifier = Modifier.align(Alignment.CenterHorizontally)
          )
        }
        Column(modifier = Modifier.weight(0.67f)) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(Res.string.about_app_title),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row {
            Text(
              text = stringResource(Res.string.about_version_label),
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.White
            )
            Text(
              text = AppGlobals.APP_VERSION,
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = Color.White
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row {
            Text(
              text = stringResource(Res.string.about_package_label),
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.White
            )
            Text(
              text = AppGlobals.PACKAGE_ID,
              modifier = Modifier.padding(2.dp),
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.White
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row {
            Text(
              text = stringResource(Res.string.about_source_label),
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.White
            )
            Text(
              text = AppGlobals.GITHUB_URL,
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = Color.White
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
          ) {
            Text(
              text = stringResource(Res.string.about_read_settings_instruction),
              modifier = Modifier
                .weight(1f)
                .padding(2.dp),
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Visible
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { wm?.openWindow(OverlayType.SETTINGS) },
              colors = ButtonDefaults.buttonColors(Color.White),
              modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
            ) {
              Text(text = stringResource(Res.string.about_go_to_settings_button), color = Color.Black)
            }
          }
        }
      }

      Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))

      Row {
        Column(modifier = Modifier.weight(1f).padding(8.dp)) {
          Image(
            painter = painterResource(Res.drawable.catreo),
            contentDescription = "Cute Reoky Icon"
          )
          Spacer(modifier = Modifier.height(16.dp))
          Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
              text = stringResource(Res.string.about_author_label),
              fontWeight = FontWeight.W200,
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.LightGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(Res.string.about_author_name),
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.White
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = stringResource(Res.string.about_dedication_text),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.White
          )
        }
        Column(modifier = Modifier.weight(1f).padding(8.dp)) {
          Image(
            painter = painterResource(Res.drawable.haranyanseal

            ),
            contentDescription = "Raid Framer Icon",
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(start = 24.dp, end = 24.dp)
          )
          Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
              text = stringResource(Res.string.about_thanks_label),
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              fontWeight = FontWeight.W200,
              color = Color.LightGray,
            )
            Text(
              text = stringResource(Res.string.about_thanks_location),
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = stringResource(Res.string.about_community_dedication),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = stringResource(Res.string.about_staff_dedication),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = stringResource(Res.string.about_dev_dedication),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.White
          )
        }
      }
    }
  }
}
