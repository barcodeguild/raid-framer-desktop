package com.reoky.raidframer.core.helpers

import com.reoky.raidframer.core.database.*
import com.reoky.raidframer.core.model.LifeMendQuality
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.PetCard

/**
 * Build a PlayerCacheEntity from this PlayerCard while preserving any previously persisted
 * cache fields. Optionally override the spec to write (`specOverride`).
 */
fun PlayerCard.createCacheObject(specOverride: String? = null): PlayerCacheEntity {
  val c = this.cache
  return PlayerCacheEntity(
    playerName = this.name,
    identity = PlayerCacheIdentity(
      lastSeen = this.lastEvent,
      lastKnownSpec = specOverride ?: (c?.lastKnownSpec ?: this.currentBuild),
      lastKnownLevel = c?.lastKnownLevel ?: 0,
      lastKnownGearScore = if (this.lastKnownGearScore > 0) this.lastKnownGearScore else (c?.lastKnownGearScore ?: 0),
      leaderships = this.leaderships,
      lastKnownFaction = c?.lastKnownFaction ?: this.lastKnownFaction,
      lastKnownFactionStatus = c?.lastKnownFactionStatus ?: this.lastKnownFactionStatus,
      lastKnownGuild = c?.lastKnownGuild ?: this.lastKnownGuild,
      lastKnownRegion = c?.lastKnownRegion ?: ""
    ),
    gliders = c?.gliders ?: PlayerCacheGliders(),
    utilityItems = c?.utilityItems ?: PlayerCacheUtilityItems(),
    snakeItems = c?.snakeItems ?: PlayerCacheSnakeItems(),
    blackDragonItems = c?.blackDragonItems ?: PlayerCacheBlackDragonItems(),
    dungeonItems = c?.dungeonItems ?: PlayerCacheDungeonItems(),
    potions = c?.potions ?: PlayerCachePotions(),
    lifetimeCombat = c?.lifetimeCombat ?: PlayerCacheLifetimeCombat(),
    lifetimeDebuffs = c?.lifetimeDebuffs ?: PlayerCacheLifetimeDebuffs(),
    lifetimeAvoidance = c?.lifetimeAvoidance ?: PlayerCacheLifetimeAvoidance()
  )
}

/**
 * Returns a copy of this PlayerCard with all session totals and recent event lists reset to zero/empty.
 * Preserves all persistent/cache fields and player identity (name, faction, gear score, etc.). This is to consolidate
 * the logical to a singular location and to reduce code bulk in the player cache interactor. tyty
 */
fun PlayerCard.resetSession(): PlayerCard {
  return this.copy(
    // Recent event buffers
    recentCastSuccessfulCastEvents = listOf(),
    recentCastEvents = listOf(),
    recentDamageEvents = listOf(),
    recentHealEvents = listOf(),
    recentBuffGainedEvents = listOf(),
    recentBuffEndedEvents = listOf(),
    recentBuffAppliedEvents = listOf(),
    recentDebuffGainedEvents = listOf(),
    recentDebuffEndedEvents = listOf(),
    recentDebuffAppliedEvents = listOf(),
    recentAvoidanceEvents = listOf(),
    recentSkillItemUsages = listOf(),

    // Session totals
    sessionSpellDamageMap = mapOf(),
    sessionDamageTotal = 0L,
    sessionHealTotal = 0L,
    sessionCCTotal = 0,
    sessionDebuffTotal = 0,
    sessionBuffTotal = 0,
    sessionCharmTotal = 0,
    sessionKillTotal = 0,
    sessionKillTotalKB = 0,
    sessionDeathTotal = 0,
    sessionGliderTotal = 0,
    sessionSilenceTotal = 0,
    sessionDistressTotal = 0,
    sessionItemSkillTotal = 0,
    sessionPotionTotal = 0,
    sessionSongsTotal = 0,
    sessionDamageTakenTotal = 0,
    sessionHealsReceivedTotal = 0,
    sessionOdeHealsTotal = 0,
    sessionTigerStrikeTotal = 0,
    sessionFreezeTotal = 0,
    sessionTripsTotal = 0,
    sessionBubblesTotal = 0,
    sessionBracingsTotal = 0,
    sessionShieldStripTotal = 0,
    sessionWeaponDisablesTotal = 0,
    sessionPotionDisablesTotal = 0,
    sessionBdGliderTotal = 0,
    sessionCrystalWingsTotal = 0,
    sessionGliderDisablesTotal = 0,
    sessionProvokedTotal = 0,
    sessionDefianceTotal = 0,
    sessionGardenDefianceTotal = 0,
    sessionPurgeTotal = 0,
    sessionSacDanceTotal = 0,
    sessionDeepTranquilityTotal = 0,
    sessionThrowDaggerTotal = 0,
    sessionStunsTotal = 0,
    sessionStaggersTotal = 0,
    sessionPetrificationTotal = 0,
    sessionAbsorbLifeforceTotal = 0,
    sessionCorrosiveBarrageTotal = 0,
    sessionBlindedByCrowsTotal = 0,
    sessionMistSunderTotal = 0,
    sessionRegularSunderTotal = 0,
    sessionImpaleImmunityTotal = 0,
    sessionProtectiveWingsTotal = 0,
    sessionCourageousActionTotal = 0,
    sessionManaBarrierTotal = 0,
    sessionReviveTotal = 0,
    sessionDeependDebuffTotal = 0,
    sessionDissonanceQuakeTotal = 0,
    sessionCriticalDiscordAoETotal = 0,
    sessionShackleTotal = 0,
    sessionDissonanceNonAoETotal = 0,
    sessionCriticalDiscordNonAoETotal = 0,
    sessionBattleHymnTotal = 0,
    sessionBlockTotal = 0,
    sessionParryTotal = 0,
    sessionBlockMitigated = 0L,
    sessionParryMitigated = 0L,
    sessionEvadeTotal = 0,
    sessionImmuneTotal = 0,
    sessionMissTotal = 0,
    sessionAbsorbTotal = 0L,

    // Life Mend tracking
    lifeMendTotal = 0,
    lifeMendHealAmounts = listOf(),
    lifeMendAverage = 0,
    lifeMendQuality = LifeMendQuality.NONE,

    // Session edge weights for battle graph
    sessionDamageToPlayer = mapOf(),
    sessionDamageFromPlayer = mapOf(),
    sessionHealToPlayer = mapOf(),
    sessionHealFromPlayer = mapOf(),
    sessionCCToPlayer = mapOf(),
    sessionCCFromPlayer = mapOf(),
    sessionDamageToPlayerBySpell = mapOf(),
    sessionHealToPlayerBySpell = mapOf(),
    sessionCCToPlayerBySpell = mapOf(),
    sessionKillsToPlayer = mapOf(),
    sessionKillsToPlayerBySpell = mapOf(),
    sessionDebuffToPlayer = mapOf(),
    sessionDebuffToPlayerBySpell = mapOf(),
    sessionBuffToPlayer = mapOf(),
    sessionBuffToPlayerBySpell = mapOf(),
    sessionCharmToPlayer = mapOf(),
    sessionCharmToPlayerBySpell = mapOf(),
    sessionDistressToPlayer = mapOf(),
    sessionDistressToPlayerBySpell = mapOf(),
    sessionSilenceToPlayer = mapOf(),
    sessionSilenceToPlayerBySpell = mapOf(),
    sessionSpellHealMap = mapOf(),
    sessionSpellCCMap = mapOf(),
    sessionSpellBuffMap = mapOf(),
    sessionSpellDebuffMap = mapOf(),
    recentKills = mapOf(),
    recentKilledBys = mapOf(),
    recentKillsKB = mapOf(),
    recentKilledByKB = mapOf()
  )
}

/**
 * Returns a copy of this PetCard with session totals and recent event lists reset to zero/empty.
 * Preserves identity metadata tying it to its owner.
 */
fun PetCard.resetSession(): PetCard {
  return this.copy(
    recentDamageEvents = listOf(),
    recentDebuffAppliedEvents = listOf(),
    sessionDamageTotal = 0L,
    sessionDebuffTotal = 0
  )
}
