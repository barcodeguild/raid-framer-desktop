package com.reoky.raidframer.core.definitions

import com.reoky.raidframer.core.database.incrementPackedItemUsage
import com.reoky.raidframer.core.database.unpackItemUsageDate
import com.reoky.raidframer.core.model.BuffGainedEvent
import com.reoky.raidframer.core.model.CombatEvent
import com.reoky.raidframer.core.model.PlayerCard
import org.jetbrains.compose.resources.StringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.glider_name_bd_glider
import raid_framer_desktop.composeapp.generated.resources.glider_name_crystal_wings
import raid_framer_desktop.composeapp.generated.resources.glider_name_feathered_dragon
import raid_framer_desktop.composeapp.generated.resources.glider_name_moonshadow
import raid_framer_desktop.composeapp.generated.resources.glider_name_ravenspine
import raid_framer_desktop.composeapp.generated.resources.glider_name_rocket_wings
import raid_framer_desktop.composeapp.generated.resources.glider_name_sky_emperor
import raid_framer_desktop.composeapp.generated.resources.glider_name_twt

/**
 * Defines glider types that are detected via self-applied buffs (SPELL_AURA_APPLIED with buffType = BUFF).
 * Each glider has a set of possible buff IDs (different ranks/variations) and updates the corresponding
 * cache field with a packed usage counter + timestamp. Debouncing is handled by the packed timestamp
 * (the cooldown field represents the minimum seconds between counted usages).
 */
enum class GliderSpell(
  val buffIds: List<Int>,
  val cooldown: Double,
  val friendlyNameRes: StringResource,
  val updateCard: (PlayerCard) -> PlayerCard
) {
  FURIOUS_TITANS_WINGS(
    buffIds = listOf(22596, 8000662, 8000663, 8000664, 8000665, 8000666),
    cooldown = 5.0,
    friendlyNameRes = Res.string.glider_name_twt,
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastTWTGlider = incrementPackedItemUsage(card.cache.lastTWTGlider))
    )}
  ),
  MOONSHADOW_GLIDER(
    buffIds = listOf(3583, 7136, 8000076, 8000093, 8000110, 8000127, 8000144),
    cooldown = 5.0,
    friendlyNameRes = Res.string.glider_name_moonshadow,
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastMoonshadowGlider = incrementPackedItemUsage(card.cache.lastMoonshadowGlider))
    )}
  ),
  CRYSTAL_WINGS(
    buffIds = listOf(8000608, 8000603, 8000602, 8000601, 8000600, 8000599, 22123, 8000604),
    cooldown = 5.0,
    friendlyNameRes = Res.string.glider_name_crystal_wings,
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastCrystalWings = incrementPackedItemUsage(card.cache.lastCrystalWings))
    )}
  ),
  LEGENDARY_DRAGON_WINGS(
    buffIds = listOf(6575, 8000157, 8000158, 8000159, 8000160, 8000161),
    cooldown = 5.0,
    friendlyNameRes = Res.string.glider_name_bd_glider,
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastBDGlider = incrementPackedItemUsage(card.cache.lastBDGlider))
    )}
  ),
  ROCKET_WINGS(
    buffIds = listOf(21604, 8000506, 8000507, 8000508, 8000509, 8000510, 8000511),
    cooldown = 5.0,
    friendlyNameRes = Res.string.glider_name_rocket_wings,
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastRocketGlider = incrementPackedItemUsage(card.cache.lastRocketGlider))
    )}
  ),
  RAVENSPINE_WINGS(
    buffIds = listOf(21573, 8000486, 8000487, 8000488, 8000489, 8000490, 8000491),
    cooldown = 5.0,
    friendlyNameRes = Res.string.glider_name_ravenspine,
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastRavenspineWings = incrementPackedItemUsage(card.cache.lastRavenspineWings))
    )}
  ),
  FEATHERED_DRAGON_GLIDER(
    buffIds = listOf(2097, 15239, 8000073, 8000090, 8000107, 8000124, 8000141),
    cooldown = 5.0,
    friendlyNameRes = Res.string.glider_name_feathered_dragon,
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastKrakenGlider = incrementPackedItemUsage(card.cache.lastKrakenGlider))
    )}
  ),
  SKY_EMPEROR(
    buffIds = listOf(23082),
    cooldown = 5.0,
    friendlyNameRes = Res.string.glider_name_sky_emperor,
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastSkyEmpGlider = incrementPackedItemUsage(card.cache.lastSkyEmpGlider))
    )}
  );

  companion object {
    private val allBuffIds: Map<Int, GliderSpell> by lazy {
      entries.flatMap { glider -> glider.buffIds.map { it to glider } }.toMap()
    }

    /**
     * Find the GliderSpell that matches a given buff ID, if any.
     */
    fun findByBuffId(buffId: Int): GliderSpell? = allBuffIds[buffId]
  }
}

/**
 * Check if a CombatEvent pertains to a glider (buff gained event with a matching glider buff ID).
 */
fun CombatEvent.pertainsToGlider(glider: GliderSpell): Boolean = when (this) {
  is BuffGainedEvent -> this.buffId in glider.buffIds
  else -> false
}

/**
 * Middleware to detect glider usages from BuffGainedEvent and update the PlayerCard.
 * - Only processes BuffGainedEvent (self-applied buffs when opening gliders)
 * - Debounces per-glider by checking the packed timestamp in the cache field
 * - Records usages and increments sessionGliderTotal
 */
fun PlayerCard.copiedWithGliderDetectionMiddleWare(event: CombatEvent): PlayerCard {
  if (event !is BuffGainedEvent) return this

  val glider = GliderSpell.findByBuffId(event.buffId) ?: return this
  val now = System.currentTimeMillis()

  // Debounce: check if this glider was used recently (packed timestamp in upper 32 bits)
  val packedValue = when {
    this.cache != null -> {
      val fieldName = glider.name
      when (fieldName) {
        "FURIOUS_TITANS_WINGS" -> this.cache.lastTWTGlider
        "MOONSHADOW_GLIDER" -> this.cache.lastMoonshadowGlider
        "CRYSTAL_WINGS" -> this.cache.lastCrystalWings
        "LEGENDARY_DRAGON_WINGS" -> this.cache.lastBDGlider
        "ROCKET_WINGS" -> this.cache.lastRocketGlider
        "RAVENSPINE_WINGS" -> this.cache.lastRavenspineWings
        "FEATHERED_DRAGON_GLIDER" -> this.cache.lastKrakenGlider
        "SKY_EMPEROR" -> this.cache.lastSkyEmpGlider
        else -> 0L
      }
    }
    else -> 0L
  }

  val lastUsedDate = packedValue.unpackItemUsageDate()
  val cooldownMillis = (glider.cooldown * 1000).toLong()
  if (now - lastUsedDate.time < cooldownMillis) return this

  val card = glider.updateCard(this)
  return card.copy(
    lastEvent = event.timestamp,
    cache = card.cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalGliderUses = (card.cache?.lifetimeTotalGliderUses ?: 0L) + 1
    ),
    sessionGliderTotal = this.sessionGliderTotal + 1,
    lastGliderUse = now
  )
}
