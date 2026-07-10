package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.model.CombatEvent
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.DebuffAppliedEvent
import com.reoky.raidframer.core.model.HealEvent
import com.reoky.raidframer.ui.component.graphs.GraphMetricType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

data class GraphDataPoint(val timestamp: Long, val damage: Long = 0L, val healing: Long = 0L, val cc: Int = 0)

private data class PlayerGraphData(
  val buckets: MutableMap<Long, GraphDataPoint> = mutableMapOf(),
  val earliestTimestamp: Long = Long.MAX_VALUE,
  val latestTimestamp: Long = 0L
)

object GraphDataInteractor : Interactor() {

  private const val BUCKET_SIZE_MS = 1000L
  private const val MAX_AGE_MS = 24 * 60 * 60 * 1000L

  private val playerData = ConcurrentHashMap<String, PlayerGraphData>()
  private val mutex = Mutex()

  private val _lastUpdate = MutableStateFlow(0L)
  val lastUpdate = _lastUpdate.asStateFlow()

  override suspend fun interact() {
    pruneOldData()
  }

  fun postEvent(event: CombatEvent) {
    if (!RFConfig.state.value.allowPVEDamage) {
      if (event is DamageEvent && !PlayerCacheInteractor.isRealPlayer(event.target)) return
    }
    scope.launch {
      mutex.withLock {
        val bucketStart = (event.timestamp / BUCKET_SIZE_MS) * BUCKET_SIZE_MS

        when (event) {
          is DamageEvent -> {
            val data = playerData.getOrPut(event.source) { PlayerGraphData() }
            val bucket = data.buckets.getOrPut(bucketStart) {
              GraphDataPoint(timestamp = bucketStart)
            }
            data.buckets[bucketStart] = bucket.copy(damage = bucket.damage + event.damage)
            _lastUpdate.value = event.timestamp
          }
          is HealEvent -> {
            val data = playerData.getOrPut(event.source) { PlayerGraphData() }
            val bucket = data.buckets.getOrPut(bucketStart) {
              GraphDataPoint(timestamp = bucketStart)
            }
            data.buckets[bucketStart] = bucket.copy(healing = bucket.healing + event.amount)
            _lastUpdate.value = event.timestamp
          }
          is DebuffAppliedEvent -> {
            val data = playerData.getOrPut(event.source) { PlayerGraphData() }
            val bucket = data.buckets.getOrPut(bucketStart) {
              GraphDataPoint(timestamp = bucketStart)
            }
            data.buckets[bucketStart] = bucket.copy(cc = bucket.cc + 1)
            _lastUpdate.value = event.timestamp
          }
        }
      }
    }
  }

  fun getAggregatedData(
    playerName: String,
    metricType: GraphMetricType,
    startTimeMs: Long,
    endTimeMs: Long,
    bucketSizeMs: Long = BUCKET_SIZE_MS
  ): List<Pair<Long, Long>> {
    val data = playerData[playerName] ?: return emptyList()

    val result = mutableListOf<Pair<Long, Long>>()
    var currentBucketStart = (startTimeMs / bucketSizeMs) * bucketSizeMs

    while (currentBucketStart < endTimeMs) {
      val point = data.buckets[currentBucketStart]
      val sum = when (metricType) {
        GraphMetricType.DAMAGE -> point?.damage ?: 0L
        GraphMetricType.HEALING -> point?.healing ?: 0L
        GraphMetricType.CC -> point?.cc?.toLong() ?: 0L
      }

      result.add(currentBucketStart to sum)
      currentBucketStart += bucketSizeMs
    }

    return result
  }

  fun getDataForPlayer(playerName: String): List<GraphDataPoint> {
    val data = playerData[playerName] ?: return emptyList()
    return data.buckets.values.sortedBy { it.timestamp }
  }

  fun getPlayerNames(): Set<String> = playerData.keys.toSet()

  private suspend fun pruneOldData() {
    val cutoff = System.currentTimeMillis() - MAX_AGE_MS
    mutex.withLock {
      playerData.values.forEach { data ->
        data.buckets.entries.removeAll { it.key < cutoff }
      }
      playerData.entries.removeAll { it.value.buckets.isEmpty() }
    }
  }

  fun clear() {
    scope.launch {
      mutex.withLock {
        playerData.clear()
      }
    }
  }
}
