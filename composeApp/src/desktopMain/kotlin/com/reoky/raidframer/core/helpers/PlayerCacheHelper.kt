package com.reoky.raidframer.core.helpers

import com.reoky.raidframer.core.database.PlayerCacheEntity
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
    lastRavenspineWings = c?.lastRavenspineWings ?: 0L,
    lastTWTGlider = c?.lastTWTGlider ?: 0L,
    lastMoonshadowGlider = c?.lastMoonshadowGlider ?: 0L,

    lastKrakenScepter = c?.lastKrakenScepter ?: 0L,
    lastKrakenSpear = c?.lastKrakenSpear ?: 0L,
    lastKrakenShield = c?.lastKrakenShield ?: 0L,
    lastLibShieldPull = c?.lastLibShieldPull ?: 0L,
    lastGreatclub = c?.lastGreatclub ?: 0L,
    lastHalcyNecklace = c?.lastHalcyNecklace ?: 0L,
    lastSoulNecklace = c?.lastSoulNecklace ?: 0L,
    lastHonorNodachi = c?.lastHonorNodachi ?: 0L,
    lastJolaShield = c?.lastJolaShield ?: 0L,

    // snake weapons and shield
    lastSnakeGreatsword = c?.lastSnakeGreatsword ?: 0L,
    lastSnakeShield = c?.lastSnakeShield ?: 0L,
    lastSnakeSword = c?.lastSnakeSword ?: 0L,
    lastSnakeAxe = c?.lastSnakeAxe ?: 0L,
    lastSnakeScepter = c?.lastSnakeScepter ?: 0L,
    lastSnakeGun = c?.lastSnakeGun ?: 0L,

    // black dragon items
    lastBdShield = c?.lastBdShield ?: 0L,
    lastBdClub = c?.lastBdClub ?: 0L,
    lastBdBow = c?.lastBdBow ?: 0L,
    lastBdRifle = c?.lastBdRifle ?: 0L,
    lastBdStaff = c?.lastBdStaff ?: 0L,
    lastBdSword = c?.lastBdSword ?: 0L,
    lastBd2hSword = c?.lastBd2hSword ?: 0L,

    // anthalon items
    lastAnthSetPull = c?.lastAnthSetPull ?: 0L,
    lastGardenAnthSetPull = c?.lastGardenAnthSetPull ?: 0L,

    // library dungeon items
    lastLibBow = c?.lastLibBow ?: 0L,
    lastLibDagger = c?.lastLibDagger ?: 0L,
    lastLibShortspear = c?.lastLibShortspear ?: 0L,
    lastLibStaff = c?.lastLibStaff ?: 0L,

    // serpentis dungeon items
    lastSerpStaff = c?.lastSerpStaff ?: 0L,
    lastSerpShield = c?.lastSerpShield ?: 0L,

    // mistsong dungeon items
    lastMistNodachi = c?.lastMistNodachi ?: 0L,
    lastMistDagger = c?.lastMistDagger ?: 0L,
    lastMistShield = c?.lastMistShield ?: 0L,

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
    lifetimeTotalBuffsApplied = c?.lifetimeTotalBuffsApplied ?: 0L,
    lifetimeTotalDebuffsApplied = c?.lifetimeTotalDebuffsApplied ?: 0L,
    lifetimeTotalCharms = c?.lifetimeTotalCharms ?: 0L,
    lifetimeTotalSongs = c?.lifetimeTotalSongs ?: 0L,
    lifetimeTotalDistresses = c?.lifetimeTotalDistresses ?: 0L,
    lifetimeTotalSilences = c?.lifetimeTotalSilences ?: 0L,
    lifetimeTotalGliderUses = c?.lifetimeTotalGliderUses ?: 0L,
    lifetimeTotalItemSkillsUsed = c?.lifetimeTotalItemSkillsUsed ?: 0L,
    lifetimeTotalPotionUsages = c?.lifetimeTotalPotionUsages ?: 0L,
    lifetimeTotalKills = c?.lifetimeTotalKills ?: 0L,
    lifetimeTotalKillsKB = c?.lifetimeTotalKillsKB ?: 0L,
    lifetimeTotalDeaths = c?.lifetimeTotalDeaths ?: 0L,
    lifetimeTotalDamageTaken = c?.lifetimeTotalDamageTaken ?: 0L,
    lifetimeTotalHealsReceived = c?.lifetimeTotalHealsReceived ?: 0L,
    lifetimeTotalTigerStrikes = c?.lifetimeTotalTigerStrikes ?: 0L,
    lifetimeTotalFreezes = c?.lifetimeTotalFreezes ?: 0L,
    lifetimeTotalTrips = c?.lifetimeTotalTrips ?: 0L,
    lifetimeTotalBubbles = c?.lifetimeTotalBubbles ?: 0L,
    lifetimeTotalBracings = c?.lifetimeTotalBracings ?: 0L,
    lifetimeTotalShieldStrip = c?.lifetimeTotalShieldStrip ?: 0L,
    lifetimeTotalWeaponDisables = c?.lifetimeTotalWeaponDisables ?: 0L,
    lifetimeTotalPotionDisables = c?.lifetimeTotalPotionDisables ?: 0L,
    lifetimeTotalBdGlider = c?.lifetimeTotalBdGlider ?: 0L,
    lifetimeTotalCrystalWings = c?.lifetimeTotalCrystalWings ?: 0L,
    lifetimeTotalGliderDisables = c?.lifetimeTotalGliderDisables ?: 0L,
    lifetimeTotalProvoked = c?.lifetimeTotalProvoked ?: 0L,
    lifetimeTotalDefiance = c?.lifetimeTotalDefiance ?: 0L,
    lifetimeTotalGardenDefiance = c?.lifetimeTotalGardenDefiance ?: 0L,
    lifetimeTotalPurges = c?.lifetimeTotalPurges ?: 0L,
    lifetimeTotalSacDances = c?.lifetimeTotalSacDances ?: 0L
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
    sessionCCFromPlayer = mapOf()
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
