package com.reoky.raidframer.core.database

import java.util.Date

private const val ITEM_USAGE_COUNTER_MASK = 0xFFFFFFFFL
private const val ITEM_USAGE_COUNTER_MAX = 0xFFFFFFFFL
private const val ITEM_USAGE_TIMESTAMP_MAX_SECONDS = 0xFFFFFFFFL // 2^32 - 1 = 4,294,967,295 seconds ≈ year 2106-02-07 UTC

/**
 * Combine a usage counter and last-used timestamp into a single Long.
 * Higher 32 bits = epoch seconds, lower 32 bits = usage counter.
 *
 * @param uses The usage counter (0 to 4,294,967,295). Values above the max are clamped.
 * @param lastUsed The last used timestamp as a Date.
 */
fun packItemUsageLong(uses: Long, lastUsed: Date): Long {
  val seconds = lastUsed.time / 1000L
  require(seconds in 0..ITEM_USAGE_TIMESTAMP_MAX_SECONDS) {
    "It's not past the year 2106 is it? Out-of-bounds timestamp (0..$ITEM_USAGE_TIMESTAMP_MAX_SECONDS)"
  }
  val usesConfined = uses.coerceIn(0, ITEM_USAGE_COUNTER_MAX)
  return (seconds shl 32) or (usesConfined and ITEM_USAGE_COUNTER_MASK)
}

/**
 * Unpack the usage counter from a packed item usage Long.
 */
fun Long.unpackItemUsageCounter(): Long = this and ITEM_USAGE_COUNTER_MASK

/**
 * Unpack the last-used timestamp (as Date) from a packed item usage Long.
 */
fun Long.unpackItemUsageDate(): Date {
  val seconds = (this ushr 32) and ITEM_USAGE_TIMESTAMP_MAX_SECONDS
  return Date(seconds * 1000L)
}
