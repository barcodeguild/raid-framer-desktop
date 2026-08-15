package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.model.BuffGainedEvent
import com.reoky.raidframer.core.model.CastingEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Attributes Protective Wings, whose area object erases the player source from aura events.
 * Lua timestamps are second-granular, so each cast credits matching ally applications in that second.
 */
object ProtectiveWingsAttributorInteractor : Interactor() {
  private const val TAG = "ProtectiveWingsAttributor"
  private const val RETENTION_MS = 5_000L

  const val MIST_CAST_ID = 36466
  const val FLAME_CAST_ID = 36467
  const val MIST_BUFF_ID = 258
  const val FLAME_BUFF_ID = 21630

  private val mutex = Mutex()

  private data class Cast(
    val caster: String,
    val timestamp: Long,
    val spellId: Int,
  )

  private data class PendingAura(
    val event: BuffGainedEvent,
    val receivedAt: Long,
  )

  private val casts = mutableListOf<Cast>()
  private val pendingAuras = mutableListOf<PendingAura>()
  private val creditedTargets = mutableSetOf<String>()

  fun onCast(event: CastingEvent) {
    if (event.spellId != MIST_CAST_ID && event.spellId != FLAME_CAST_ID) return
    scope.launch {
      mutex.withLock {
        casts.add(Cast(event.source, event.timestamp, event.spellId))
      }
    }
  }

  fun isProtectiveWingsBuff(event: BuffGainedEvent): Boolean {
    return event.buffId == MIST_BUFF_ID || event.buffId == FLAME_BUFF_ID
  }

  fun onAura(event: BuffGainedEvent) {
    if (!isProtectiveWingsBuff(event)) return
    scope.launch {
      mutex.withLock {
        pendingAuras.add(PendingAura(event, System.currentTimeMillis()))
      }
    }
  }

  override suspend fun interact() {
    mutex.withLock {
      val now = System.currentTimeMillis()
      val pending = pendingAuras.toList()
      pendingAuras.clear()

      pending.forEach { pendingAura ->
        val aura = pendingAura.event
        val matchingCasts = casts.filter { cast ->
          cast.timestamp == aura.timestamp &&
            cast.spellId == castIdForBuff(aura.buffId)
        }

        val caster = matchingCasts.map { it.caster }.distinct().singleOrNull()
        if (caster == null || !PlayerCacheInteractor.isRealPlayer(aura.target)) {
          if (now - pendingAura.receivedAt < RETENTION_MS) {
            pendingAuras.add(pendingAura)
          } else {
            Log.debug(TAG, "Skipping ambiguous Protective Wings aura ${aura.buffId} on ${aura.target}")
          }
          return@forEach
        }

        val key = "$caster|${aura.timestamp}|${aura.buffId}|${aura.target.lowercase()}"
        if (creditedTargets.add(key)) {
          PlayerCacheInteractor.postEvent(
            BuffGainedEvent(
              timestamp = aura.timestamp,
              cid = aura.cid,
              source = caster,
              target = aura.target,
              buff = aura.buff,
              buffId = aura.buffId,
            )
          )
          Log.info(TAG, "Attributed Protective Wings ${aura.buffId} on ${aura.target} to $caster")
        }
      }

      casts.removeAll { now - it.timestamp > RETENTION_MS }
      creditedTargets.removeAll { key ->
        val timestamp = key.split('|').getOrNull(1)?.toLongOrNull()
        timestamp != null && now - timestamp > RETENTION_MS
      }
    }
  }

  private fun castIdForBuff(buffId: Int): Int = when (buffId) {
    MIST_BUFF_ID -> MIST_CAST_ID
    FLAME_BUFF_ID -> FLAME_CAST_ID
    else -> -1
  }
}
