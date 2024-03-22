package ui.overlay

import AppState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
fun AboutOverlay() {
  Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.60f))) {

    // close button
    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
      val interactionSource = remember { MutableInteractionSource() }
      val isCloseHovered by interactionSource.collectIsHoveredAsState()
      IconButton(
        onClick = {
          AppState.isAboutOverlayVisible.value = false
        },
        modifier = Modifier
          .size(32.dp)
          .background(if (isCloseHovered) Color.Red.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.20f), MaterialTheme.shapes.small)
          .shadow(
            elevation = 0.dp,
            clip = true,
            ambientColor = Color.Transparent,
            spotColor = Color.Transparent
          )
          .hoverable(interactionSource = interactionSource)
          .clip(RoundedCornerShape(8.dp))
      ) {
        Text("✕", fontSize = 18.sp, color = if (isCloseHovered) Color.White else Color.White, textAlign = TextAlign.Center)
      }
    }

    // content area
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)
    ) {

      // Header Logo and Version
      Row {
        Column(modifier = Modifier.weight(0.33f)) {
          val image = painterResource("raidframer.ico")
          Image(
            painter = image,
            contentDescription = "Raid Framer Icon",
            modifier = Modifier.align(Alignment.CenterHorizontally)
          )
        }
        Column(modifier = Modifier.weight(0.67f)) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Raid Framer Desktop",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row {
            Text(
              text = "Version:",
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.White
            )
            Text(
              text = "1.4.8",
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
              text = "Package:",
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.White
            )
            Text(
              text = "lol.rfcloud",
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
              text = "Source:",
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.White
            )
            Text(
              text = "https://github.com/barcodeguild/raid-framer-desktop",
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = Color.White
            )
          }
          Button(
            onClick = {
              AppState.isAboutOverlayVisible.value = false
              AppState.isSettingsOverlayVisible.value = true
            },
            colors = ButtonDefaults.buttonColors(Color.White),
            modifier = Modifier.padding(bottom = 8.dp, end = 8.dp).align(Alignment.End)
          ) {
            Text(
              text = "Go to Settings",
              color = Color.Black
            )
          }
        }
      }

      Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))

      Row {
        Column(modifier = Modifier.weight(1f).padding(8.dp)) {
          Image(
            painter = painterResource("catreo.png"),
            contentDescription = "Cute Reoky Icon"
          )
          Spacer(modifier = Modifier.height(16.dp))
          Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
              text = "Author:",
              fontWeight = FontWeight.W200,
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.LightGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "~ Reoky ~",
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              color = Color.White
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Dedicated to all of our friends on the East and out in the virtual world. May this code be used for good, and not evil.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.White
          )
        }
        Column(modifier = Modifier.weight(1f).padding(8.dp)) {
          Image(
            painter = painterResource("haranyanseal.png"),
            contentDescription = "Raid Framer Icon",
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(start = 24.dp, end = 24.dp)
          )
          Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
              text = "Ty ty from",
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              fontWeight = FontWeight.W200,
              color = Color.LightGray,
            )
            Text(
              text = "United East",
              modifier = Modifier.padding(2.dp),
              textAlign = TextAlign.Start,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "To the ArcheRage community for their support and feedback. ~",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "To the ArcheRage staff for their dedication and hard work. ~",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "To the ArcheRage developers for their continued support and updates. ~",
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