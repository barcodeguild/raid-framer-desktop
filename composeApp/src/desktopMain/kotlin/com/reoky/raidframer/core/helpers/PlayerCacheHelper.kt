package com.reoky.raidframer.core.helpers

import com.reoky.raidframer.core.database.LeadershipRole
import com.reoky.raidframer.core.database.PlayerCacheEntity
import com.reoky.raidframer.core.model.PlayerCard

/**
 * Build a PlayerCacheEntity from this PlayerCard while preserving any previously persisted
 * cache fields. Optionally override the spec to write (`specOverride`).
 */
fun PlayerCard.createCacheObject(specOverride: String? = null): PlayerCacheEntity {
  val c = this.cache
  return PlayerCacheEntity(
    playerName = this.name,
    lastSeen = this.lastEvent,
    lastKnownSpec = specOverride ?: (c?.lastKnownSpec ?: this.currentBuild),
    lastKnownLevel = c?.lastKnownLevel ?: 0,
    lastKnownGearScore = if (this.lastKnownGearScore > 0) this.lastKnownGearScore else (c?.lastKnownGearScore ?: 0),
    leaderships = this.leaderships,
    lastKnownFaction = c?.lastKnownFaction ?: this.lastKnownFaction,
    lastKnownFactionStatus = c?.lastKnownFactionStatus ?: this.lastKnownFactionStatus,
    lastKnownGuild = c?.lastKnownGuild ?: this.lastKnownGuild,
    lastKnownRegion = c?.lastKnownRegion ?: "",

    lastBDGlider = c?.lastBDGlider ?: 0L,
    lastSkyEmpGlider = c?.lastSkyEmpGlider ?: 0L,
    lastKrakenGlider = c?.lastKrakenGlider ?: 0L,
    lastCrystalWings = c?.lastCrystalWings ?: 0L,
    lastRocketGlider = c?.lastRocketGlider ?: 0L,

    lastKrakenScepter = c?.lastKrakenScepter ?: 0L,
    lastKrakenSpear = c?.lastKrakenSpear ?: 0L,
    lastLibShieldPull = c?.lastLibShieldPull ?: 0L,
    lastGreatclub = c?.lastGreatclub ?: 0L,
    lastHalcyNecklace = c?.lastHalcyNecklace ?: 0L,
    lastSoulNecklace = c?.lastSoulNecklace ?: 0L,
    lastHonorNodachi = c?.lastHonorNodachi ?: 0L,
    lastJolaShield = c?.lastJolaShield ?: 0L,

    lastMinorHealingPotion = c?.lastMinorHealingPotion ?: 0L,
    lastMajorHealingPotion = c?.lastMajorHealingPotion ?: 0L,
    lastMinorManaPotion = c?.lastMinorManaPotion ?: 0L,
    lastMajorManaPotion = c?.lastMajorManaPotion ?: 0L,
    lastWildGinseng = c?.lastWildGinseng ?: 0L,
    lastJinhuiWish = c?.lastJinhuiWish ?: 0L,
    lastBlueGoblet = c?.lastBlueGoblet ?: 0L,
    lastYellowGoblet = c?.lastYellowGoblet ?: 0L,
    lastPurpleGoblet = c?.lastPurpleGoblet ?: 0L,
    lastPinkGoblet = c?.lastPinkGoblet ?: 0L,
    lastGrayGoblet = c?.lastGrayGoblet ?: 0L,
    lastOrangeGoblet = c?.lastOrangeGoblet ?: 0L,
    lastAncientsPotion = c?.lastAncientsPotion ?: 0L,
    lastDahutasBubble = c?.lastDahutasBubble ?: 0L,
    lastWhisperPotion = c?.lastWhisperPotion ?: 0L,
    lastRedBerryFruit = c?.lastRedBerryFruit ?: 0L,
    lastBlueBerryFruit = c?.lastBlueBerryFruit ?: 0L,
    lastSecretGift = c?.lastSecretGift ?: 0L,
    lastHonorElixir = c?.lastHonorElixir ?: 0L,
    lastWonderlandPVEBook = c?.lastWonderlandPVEBook ?: 0L,

    lifetimeTotalDamage = c?.lifetimeTotalDamage ?: 0L,
    lifetimeTotalHealing = c?.lifetimeTotalHealing ?: 0L,
    lifetimeTotalCCDelivered = c?.lifetimeTotalCCDelivered ?: 0L,
    lifetimeTotalDebuffsApplied = c?.lifetimeTotalDebuffsApplied ?: 0L,
    lifetimeTotalCharms = c?.lifetimeTotalCharms ?: 0L,
    lifetimeTotalDistresses = c?.lifetimeTotalDistresses ?: 0L,
    lifetimeTotalSilences = c?.lifetimeTotalSilences ?: 0L,
    lifetimeTotalGliderUses = c?.lifetimeTotalGliderUses ?: 0L,
    lifetimeTotalItemSkillsUsed = c?.lifetimeTotalItemSkillsUsed ?: 0L,
    lifetimeTotalKills = c?.lifetimeTotalKills ?: 0L,
    lifetimeTotalKillsKB = c?.lifetimeTotalKillsKB ?: 0L,
    lifetimeTotalDeaths = c?.lifetimeTotalDeaths ?: 0L,
    lifetimeTotalDamageTaken = c?.lifetimeTotalDamageTaken ?: 0L
  )
}
