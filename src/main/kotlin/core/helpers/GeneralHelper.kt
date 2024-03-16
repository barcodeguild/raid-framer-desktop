package core.helpers
import java.awt.Toolkit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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