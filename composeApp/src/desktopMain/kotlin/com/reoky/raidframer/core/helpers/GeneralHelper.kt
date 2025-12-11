package lol.rfcloud.core.helpers

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.HealEvent
import java.awt.Desktop
import java.awt.Toolkit
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Extension function to abbreviate a numeric values by thousands, millions, billions, etc.
 */
fun Long.humanReadableAbbreviation(): String {
  val value = this.toDouble()
  val suffixes = arrayOf("", "k", "M", "B", "T")
  val suffixNum = when {
    value < 1_000_000 -> 1
    value < 1_000_000_000 -> 2
    else -> 3
  }
  val divisor = Math.pow(10.0, suffixNum * 3.toDouble())
  var shortValue = value / divisor
  return String.format("%.1f", shortValue) + suffixes[suffixNum]
}


/*
 * Opens a browser window to the specified URL.
 */
fun openWebLink(url: String) {
  if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
    Desktop.getDesktop().browse(URI(url))
  }
}

/*
 * Finds the width and height of the screen in dp. So we can calculate where to put the overlay windows and not
 * on completely over or undershoot on friend's tiny displays.
 */
fun getScreenSizeInDp(): Pair<Dp, Dp> {
  val screenSize = Toolkit.getDefaultToolkit().screenSize
  val density = Toolkit.getDefaultToolkit().screenResolution / 160.0f
  val widthInDp = (screenSize.width / density).dp
  val heightInDp = (screenSize.height / density).dp
  return Pair(widthInDp, heightInDp)
}

fun Long.toLocalTimeString(): String {
  val instant = Instant.ofEpochMilli(this)
  val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
  return localDateTime.format(DateTimeFormatter.ofPattern("hh:mm:ss"))
}

/*
 * Makes a pretty-looking AnnotatedString for the attack event. I brought this code into a different file because
 * it's too and will get used multiple times potentially.
 */
@Composable
fun annotatedStringForAttack(event: DamageEvent): AnnotatedString {
  return buildAnnotatedString {
    withStyle(style = SpanStyle(color = Color(249, 191, 59))) {
      append(event.caster)
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
      append(event.caster)
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

/*
 * Simple extension function to shorten people's names to not overflow the page.
 */
fun String.ellipsis(chars: Int): String {
  return if (length > chars) {
    substring(0, chars) + ".."
  } else {
    this
  }
}
