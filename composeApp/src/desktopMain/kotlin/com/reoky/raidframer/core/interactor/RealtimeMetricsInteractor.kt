package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.calc.RealtimeComputer
import com.reoky.raidframer.core.calc.MetricRawSample
import com.reoky.raidframer.core.mock.mockCandlestickDataFrame
import kotlinx.coroutines.delay

class RealtimeMetricsInteractor : Interactor() {

  val realtimeComputer = RealtimeComputer(windowBuckets = 120, bucketMillis = 60_000L)

  private val mockData = mockCandlestickDataFrame(points = 60, intervalMinutes = 1)
  private var index = 0

  // Simulated playback clock. Advance this per-candle so buckets represent minutes.
  private var simulatedClockMs: Long = java.time.Instant.now().toEpochMilli()
  private val simulatedIntervalMs = 60_000L // 1 minute per mock candle (matches intervalMinutes)

  override suspend fun interact() {

    if (index > mockData.closes.size) return;

    // produce four samples per mock candle so bucket aggregate has an open/high/low/close
    val idx = index % mockData.closes.size

    val open = mockData.opens[idx]
    val high = mockData.highs[idx]
    val low = mockData.lows[idx]
    val close = mockData.closes[idx]

    // Use the simulated clock as the base timestamp for the whole candle so all four samples land in the same bucket.
    val baseMs = simulatedClockMs

    realtimeComputer.push(MetricRawSample(timestampMs = baseMs + 0L, value = open))
    delay(500L)
    realtimeComputer.push(MetricRawSample(timestampMs = baseMs + 10L, value = high))
    delay(500L)
    realtimeComputer.push(MetricRawSample(timestampMs = baseMs + 20L, value = low))
    delay(1000L)
    realtimeComputer.push(MetricRawSample(timestampMs = baseMs + 30L, value = close))

    // advance simulated clock by one candle interval so next interact() advances to next bucket
    simulatedClockMs += simulatedIntervalMs
    index++
  }

  override fun stop() {
    realtimeComputer.stop()
    super.stop()
  }
}