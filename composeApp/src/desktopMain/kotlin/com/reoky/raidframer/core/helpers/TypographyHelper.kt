package com.reoky.raidframer.core.helpers

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Shared text styles for the app's dense overlay surfaces, so sizes and weights stay consistent
 * across sections and components. Colors stay at the call site so contexts can tint freely.
 */
object RFTypography {
  /** Uppercase section label, e.g. "SNAPSHOT HISTORY". */
  val Kicker = TextStyle(fontSize = 9.sp, lineHeight = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium)

  /** Uppercase heading that groups a section, e.g. above the snapshot list. */
  val SectionLabel = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium)

  /** Uppercase panel or page title, e.g. a chart panel heading. */
  val SectionTitle = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium)

  /** Card or panel heading value, e.g. a snapshot timestamp. */
  val Headline = TextStyle(fontSize = 15.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)

  /** Numeric emphasis for key statistics. */
  val Metric = TextStyle(fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)

  /** Standard readable body text, e.g. player names. */
  val Body = TextStyle(fontSize = 11.sp, lineHeight = 13.sp)

  /** Secondary metadata: stat labels, legend entries, axis notes. */
  val Meta = TextStyle(fontSize = 10.sp, lineHeight = 12.sp)

  /** Micro annotations: chips, unit suffixes, badge labels. */
  val Micro = TextStyle(fontSize = 8.sp, lineHeight = 10.sp)
}
