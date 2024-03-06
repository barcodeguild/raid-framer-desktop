package core.helpers

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