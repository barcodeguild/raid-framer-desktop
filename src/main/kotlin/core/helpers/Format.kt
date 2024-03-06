package core.helpers

/*
 * Extension function to abbreviate a numeric values by thousands, millions, billions, etc.
 */
fun Long.humanReadableAbbreviation(): String {
  val value = this.toDouble()
  val suffixes = arrayOf("", "k", "M", "B", "T")
  val suffixNum = (value.toString().length - 1) / 3
  var shortValue = value / Math.pow(10.0, suffixNum * 3.toDouble())
  if (shortValue % 1 != 0.0) {
    shortValue = (shortValue * 10.0).toInt() / 10.0
  }
  return shortValue.toString() + suffixes[suffixNum]
}