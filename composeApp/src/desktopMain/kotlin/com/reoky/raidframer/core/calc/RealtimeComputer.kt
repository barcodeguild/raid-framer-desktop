package com.reoky.raidframer.core.calc

import com.reoky.raidframer.ui.component.graphs.CandlestickDataFrame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

data class MetricRawSample(val timestampMs: Long, val value: Double)

/**
 * Realtime aggregator:
 * - windowBuckets: how many buckets to keep (not minutes; unit = buckets)
 * - bucketMillis: bucket granularity in ms (set 60_000L for 1-minute buckets)
 *
 * This version adds smoothing of the _current value by averaging samples within a short
 * sliding window and emitting that averaged current every `smoothingEmitIntervalMs`.
 */
class RealtimeComputer(
  private val windowBuckets: Int = 60,
  private val bucketMillis: Long = 10_000L, // 10-second chart
  private val smoothingWindowMs: Long = 1_000L,        // how far back to average for "current"
  private val smoothingEmitIntervalMs: Long = 1_000L   // how often to update _current
) {
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val buckets = LinkedHashMap<Long, MutableList<Double>>() // bucketEpoch -> samples
  private val sampleCh = Channel<MetricRawSample>(Channel.UNLIMITED)

  private val _candles = MutableStateFlow(CandlestickDataFrame(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
  val candles: StateFlow<CandlestickDataFrame> = _candles.asStateFlow()

  // exposed "current" will be the smoothed/averaged value
  private val _current = MutableStateFlow<Double?>(null)
  val current: StateFlow<Double?> = _current.asStateFlow()

  // buffer for recent raw samples used for smoothing
  private val recentMutex = Mutex()
  private val recentSamples = ArrayDeque<MetricRawSample>()

  init {
    // single consumer for incoming samples (keeps bucket updates serialized)
    scope.launch {
      for (s in sampleCh) processSample(s)
    }

    // ticker coroutine updates the smoothed current value every smoothingEmitIntervalMs
    scope.launch {
      while (isActive) {
        delay(smoothingEmitIntervalMs)
        emitSmoothedCurrent()
      }
    }
  }

  fun push(sample: MetricRawSample) { sampleCh.trySend(sample) }

  private suspend fun processSample(sample: MetricRawSample) {
    val bucket = sample.timestampMs / bucketMillis
    val list = buckets.getOrPut(bucket) { mutableListOf() }
    list.add(sample.value)

    // keep window size within configured bucket count
    while (buckets.size > windowBuckets) {
      val firstKey = buckets.keys.firstOrNull() ?: break
      buckets.remove(firstKey)
    }

    // append to recent-samples buffer and prune old entries
    recentMutex.withLock {
      recentSamples.addLast(sample)
      val cutoff = sample.timestampMs - smoothingWindowMs
      while (recentSamples.isNotEmpty() && recentSamples.first.timestampMs < cutoff) {
        recentSamples.removeFirst()
      }
    }

    emitAggregated()
    // do not set _current here to avoid noisy per-sample updates — smoothed updates are done by ticker
  }

  private fun emitAggregated() {
    val keys = buckets.keys.sorted()
    if (keys.isEmpty()) {
      _candles.value = CandlestickDataFrame(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
      return
    }

    // create sequential labels (one per bucket) starting at 0 to keep chart indexing aligned to indices
    val labels = keys.mapIndexed { idx, _ -> idx.toString() }

    val opens = keys.map { k -> buckets[k]!!.first() }
    val closes = keys.map { k -> buckets[k]!!.last() }
    val highs = keys.map { k -> buckets[k]!!.maxOrNull() ?: 0.0 }
    val lows = keys.map { k -> buckets[k]!!.minOrNull() ?: 0.0 }

    _candles.value = CandlestickDataFrame(labels, opens, highs, lows, closes)
  }

  private suspend fun emitSmoothedCurrent() {
    val now = System.currentTimeMillis()
    val cutoff = now - smoothingWindowMs
    val samples: List<MetricRawSample> = recentMutex.withLock {
      // prune any entries older than cutoff (in case processSample hasn't done it)
      while (recentSamples.isNotEmpty() && recentSamples.first.timestampMs < cutoff) {
        recentSamples.removeFirst()
      }
      recentSamples.toList()
    }

    val avg = if (samples.isNotEmpty()) {
      samples.map { it.value }.average()
    } else {
      // fallback: if we have candle data, use last close; else null
      val lastClose = _candles.value.closes.lastOrNull()
      lastClose
    }

    _current.value = avg
  }

  fun stop() {
    sampleCh.close()
    scope.cancel()
  }
}
