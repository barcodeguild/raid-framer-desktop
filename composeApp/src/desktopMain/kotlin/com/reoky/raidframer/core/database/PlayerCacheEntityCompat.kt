package com.reoky.raidframer.core.database

/*
 * Keep read-only callers independent of the Room grouping used to keep the entity
 * constructor below the JVM method descriptor limit. The embedded groups still map
 * to the original, unprefixed SQLite columns.
 */
val PlayerCacheEntity.lastSeen get() = identity.lastSeen
val PlayerCacheEntity.lastKnownSpec get() = identity.lastKnownSpec
val PlayerCacheEntity.lastKnownLevel get() = identity.lastKnownLevel
val PlayerCacheEntity.leaderships get() = identity.leaderships
val PlayerCacheEntity.lastKnownGearScore get() = identity.lastKnownGearScore
val PlayerCacheEntity.lastKnownFaction get() = identity.lastKnownFaction
val PlayerCacheEntity.lastKnownFactionStatus get() = identity.lastKnownFactionStatus
val PlayerCacheEntity.lastKnownGuild get() = identity.lastKnownGuild
val PlayerCacheEntity.lastKnownRegion get() = identity.lastKnownRegion

val PlayerCacheEntity.lastBDGlider get() = gliders.lastBDGlider
val PlayerCacheEntity.lastSkyEmpGlider get() = gliders.lastSkyEmpGlider
val PlayerCacheEntity.lastKrakenGlider get() = gliders.lastKrakenGlider
val PlayerCacheEntity.lastCrystalWings get() = gliders.lastCrystalWings
val PlayerCacheEntity.lastRocketGlider get() = gliders.lastRocketGlider
val PlayerCacheEntity.lastRavenspineWings get() = gliders.lastRavenspineWings
val PlayerCacheEntity.lastTWTGlider get() = gliders.lastTWTGlider
val PlayerCacheEntity.lastMoonshadowGlider get() = gliders.lastMoonshadowGlider

val PlayerCacheEntity.lastKrakenScepter get() = utilityItems.lastKrakenScepter
val PlayerCacheEntity.lastKrakenSpear get() = utilityItems.lastKrakenSpear
val PlayerCacheEntity.lastKrakenShield get() = utilityItems.lastKrakenShield
val PlayerCacheEntity.lastLibShieldPull get() = utilityItems.lastLibShieldPull
val PlayerCacheEntity.lastGreatclub get() = utilityItems.lastGreatclub
val PlayerCacheEntity.lastHalcyNecklace get() = utilityItems.lastHalcyNecklace
val PlayerCacheEntity.lastSoulNecklace get() = utilityItems.lastSoulNecklace
val PlayerCacheEntity.lastHonorNodachi get() = utilityItems.lastHonorNodachi
val PlayerCacheEntity.lastJolaShield get() = utilityItems.lastJolaShield

val PlayerCacheEntity.lastSnakeGreatsword get() = snakeItems.lastSnakeGreatsword
val PlayerCacheEntity.lastSnakeShield get() = snakeItems.lastSnakeShield
val PlayerCacheEntity.lastSnakeSword get() = snakeItems.lastSnakeSword
val PlayerCacheEntity.lastSnakeAxe get() = snakeItems.lastSnakeAxe
val PlayerCacheEntity.lastSnakeScepter get() = snakeItems.lastSnakeScepter
val PlayerCacheEntity.lastSnakeGun get() = snakeItems.lastSnakeGun

val PlayerCacheEntity.lastBdShield get() = blackDragonItems.lastBdShield
val PlayerCacheEntity.lastBdClub get() = blackDragonItems.lastBdClub
val PlayerCacheEntity.lastBdBow get() = blackDragonItems.lastBdBow
val PlayerCacheEntity.lastBdRifle get() = blackDragonItems.lastBdRifle
val PlayerCacheEntity.lastBdStaff get() = blackDragonItems.lastBdStaff
val PlayerCacheEntity.lastBdSword get() = blackDragonItems.lastBdSword
val PlayerCacheEntity.lastBd2hSword get() = blackDragonItems.lastBd2hSword

val PlayerCacheEntity.lastAnthSetPull get() = dungeonItems.lastAnthSetPull
val PlayerCacheEntity.lastGardenAnthSetPull get() = dungeonItems.lastGardenAnthSetPull
val PlayerCacheEntity.lastLibBow get() = dungeonItems.lastLibBow
val PlayerCacheEntity.lastLibDagger get() = dungeonItems.lastLibDagger
val PlayerCacheEntity.lastLibShortspear get() = dungeonItems.lastLibShortspear
val PlayerCacheEntity.lastLibStaff get() = dungeonItems.lastLibStaff
val PlayerCacheEntity.lastSerpStaff get() = dungeonItems.lastSerpStaff
val PlayerCacheEntity.lastSerpShield get() = dungeonItems.lastSerpShield
val PlayerCacheEntity.lastMistNodachi get() = dungeonItems.lastMistNodachi
val PlayerCacheEntity.lastMistDagger get() = dungeonItems.lastMistDagger
val PlayerCacheEntity.lastMistShield get() = dungeonItems.lastMistShield

val PlayerCacheEntity.lastMinorHealingPotion get() = potions.lastMinorHealingPotion
val PlayerCacheEntity.lastMajorHealingPotion get() = potions.lastMajorHealingPotion
val PlayerCacheEntity.lastMinorManaPotion get() = potions.lastMinorManaPotion
val PlayerCacheEntity.lastMajorManaPotion get() = potions.lastMajorManaPotion
val PlayerCacheEntity.lastWildGinseng get() = potions.lastWildGinseng
val PlayerCacheEntity.lastJinhuiWish get() = potions.lastJinhuiWish
val PlayerCacheEntity.lastBlueGoblet get() = potions.lastBlueGoblet
val PlayerCacheEntity.lastYellowGoblet get() = potions.lastYellowGoblet
val PlayerCacheEntity.lastPurpleGoblet get() = potions.lastPurpleGoblet
val PlayerCacheEntity.lastPinkGoblet get() = potions.lastPinkGoblet
val PlayerCacheEntity.lastGrayGoblet get() = potions.lastGrayGoblet
val PlayerCacheEntity.lastOrangeGoblet get() = potions.lastOrangeGoblet
val PlayerCacheEntity.lastAncientsPotion get() = potions.lastAncientsPotion
val PlayerCacheEntity.lastDahutasBubble get() = potions.lastDahutasBubble
val PlayerCacheEntity.lastWhisperPotion get() = potions.lastWhisperPotion
val PlayerCacheEntity.lastRedBerryFruit get() = potions.lastRedBerryFruit
val PlayerCacheEntity.lastBlueBerryFruit get() = potions.lastBlueBerryFruit
val PlayerCacheEntity.lastSecretGift get() = potions.lastSecretGift
val PlayerCacheEntity.lastHonorElixir get() = potions.lastHonorElixir
val PlayerCacheEntity.lastWonderlandPVEBook get() = potions.lastWonderlandPVEBook

val PlayerCacheEntity.lifetimeTotalDamage get() = lifetimeCombat.lifetimeTotalDamage
val PlayerCacheEntity.lifetimeTotalHealing get() = lifetimeCombat.lifetimeTotalHealing
val PlayerCacheEntity.lifetimeTotalCCDelivered get() = lifetimeCombat.lifetimeTotalCCDelivered
val PlayerCacheEntity.lifetimeTotalBuffsApplied get() = lifetimeCombat.lifetimeTotalBuffsApplied
val PlayerCacheEntity.lifetimeTotalDebuffsApplied get() = lifetimeCombat.lifetimeTotalDebuffsApplied
val PlayerCacheEntity.lifetimeTotalCharms get() = lifetimeCombat.lifetimeTotalCharms
val PlayerCacheEntity.lifetimeTotalSongs get() = lifetimeCombat.lifetimeTotalSongs
val PlayerCacheEntity.lifetimeTotalDistresses get() = lifetimeCombat.lifetimeTotalDistresses
val PlayerCacheEntity.lifetimeTotalSilences get() = lifetimeCombat.lifetimeTotalSilences
val PlayerCacheEntity.lifetimeTotalGliderUses get() = lifetimeCombat.lifetimeTotalGliderUses
val PlayerCacheEntity.lifetimeTotalItemSkillsUsed get() = lifetimeCombat.lifetimeTotalItemSkillsUsed
val PlayerCacheEntity.lifetimeTotalPotionUsages get() = lifetimeCombat.lifetimeTotalPotionUsages
val PlayerCacheEntity.lifetimeTotalKills get() = lifetimeCombat.lifetimeTotalKills
val PlayerCacheEntity.lifetimeTotalKillsKB get() = lifetimeCombat.lifetimeTotalKillsKB
val PlayerCacheEntity.lifetimeTotalDeaths get() = lifetimeCombat.lifetimeTotalDeaths
val PlayerCacheEntity.lifetimeTotalDamageTaken get() = lifetimeCombat.lifetimeTotalDamageTaken
val PlayerCacheEntity.lifetimeTotalHealsReceived get() = lifetimeCombat.lifetimeTotalHealsReceived
val PlayerCacheEntity.lifetimeTotalTigerStrikes get() = lifetimeCombat.lifetimeTotalTigerStrikes
val PlayerCacheEntity.lifetimeTotalFreezes get() = lifetimeCombat.lifetimeTotalFreezes
val PlayerCacheEntity.lifetimeTotalTrips get() = lifetimeCombat.lifetimeTotalTrips
val PlayerCacheEntity.lifetimeTotalBubbles get() = lifetimeCombat.lifetimeTotalBubbles
val PlayerCacheEntity.lifetimeTotalBracings get() = lifetimeCombat.lifetimeTotalBracings
val PlayerCacheEntity.lifetimeTotalShieldStrip get() = lifetimeCombat.lifetimeTotalShieldStrip
val PlayerCacheEntity.lifetimeTotalWeaponDisables get() = lifetimeCombat.lifetimeTotalWeaponDisables
val PlayerCacheEntity.lifetimeTotalPotionDisables get() = lifetimeCombat.lifetimeTotalPotionDisables
val PlayerCacheEntity.lifetimeTotalBdGlider get() = lifetimeCombat.lifetimeTotalBdGlider
val PlayerCacheEntity.lifetimeTotalCrystalWings get() = lifetimeCombat.lifetimeTotalCrystalWings
val PlayerCacheEntity.lifetimeTotalGliderDisables get() = lifetimeCombat.lifetimeTotalGliderDisables
val PlayerCacheEntity.lifetimeTotalProvoked get() = lifetimeCombat.lifetimeTotalProvoked
val PlayerCacheEntity.lifetimeTotalDissonanceQuake get() = lifetimeCombat.lifetimeTotalDissonanceQuake
val PlayerCacheEntity.lifetimeTotalCriticalDiscordAoE get() = lifetimeCombat.lifetimeTotalCriticalDiscordAoE
val PlayerCacheEntity.lifetimeTotalShackle get() = lifetimeCombat.lifetimeTotalShackle
val PlayerCacheEntity.lifetimeTotalDissonanceNonAoE get() = lifetimeCombat.lifetimeTotalDissonanceNonAoE
val PlayerCacheEntity.lifetimeTotalCriticalDiscordNonAoE get() = lifetimeCombat.lifetimeTotalCriticalDiscordNonAoE
val PlayerCacheEntity.lifetimeTotalBattleHymn get() = lifetimeCombat.lifetimeTotalBattleHymn

val PlayerCacheEntity.lifetimeTotalDefiance get() = lifetimeDebuffs.lifetimeTotalDefiance
val PlayerCacheEntity.lifetimeTotalGardenDefiance get() = lifetimeDebuffs.lifetimeTotalGardenDefiance
val PlayerCacheEntity.lifetimeTotalPurges get() = lifetimeDebuffs.lifetimeTotalPurges
val PlayerCacheEntity.lifetimeTotalSacDances get() = lifetimeDebuffs.lifetimeTotalSacDances
val PlayerCacheEntity.lifetimeTotalDeepTranquility get() = lifetimeDebuffs.lifetimeTotalDeepTranquility
val PlayerCacheEntity.lifetimeTotalDeependDebuff get() = lifetimeDebuffs.lifetimeTotalDeependDebuff
val PlayerCacheEntity.lifetimeTotalThrowDagger get() = lifetimeDebuffs.lifetimeTotalThrowDagger
val PlayerCacheEntity.lifetimeTotalStuns get() = lifetimeDebuffs.lifetimeTotalStuns
val PlayerCacheEntity.lifetimeTotalStaggers get() = lifetimeDebuffs.lifetimeTotalStaggers
val PlayerCacheEntity.lifetimeTotalPetrification get() = lifetimeDebuffs.lifetimeTotalPetrification
val PlayerCacheEntity.lifetimeTotalAbsorbLifeforce get() = lifetimeDebuffs.lifetimeTotalAbsorbLifeforce
val PlayerCacheEntity.lifetimeTotalCorrosiveBarrage get() = lifetimeDebuffs.lifetimeTotalCorrosiveBarrage
val PlayerCacheEntity.lifetimeTotalBlindedByCrows get() = lifetimeDebuffs.lifetimeTotalBlindedByCrows
val PlayerCacheEntity.lifetimeTotalMistSunder get() = lifetimeDebuffs.lifetimeTotalMistSunder
val PlayerCacheEntity.lifetimeTotalRegularSunder get() = lifetimeDebuffs.lifetimeTotalRegularSunder
val PlayerCacheEntity.lifetimeTotalImpaleImmunity get() = lifetimeDebuffs.lifetimeTotalImpaleImmunity
val PlayerCacheEntity.lifetimeTotalProtectiveWings get() = lifetimeDebuffs.lifetimeTotalProtectiveWings
val PlayerCacheEntity.lifetimeTotalCourageousAction get() = lifetimeDebuffs.lifetimeTotalCourageousAction
val PlayerCacheEntity.lifetimeTotalManaBarrier get() = lifetimeDebuffs.lifetimeTotalManaBarrier
val PlayerCacheEntity.lifetimeTotalRevive get() = lifetimeDebuffs.lifetimeTotalRevive

val PlayerCacheEntity.lifetimeTotalBlocks get() = lifetimeAvoidance.lifetimeTotalBlocks
val PlayerCacheEntity.lifetimeTotalParries get() = lifetimeAvoidance.lifetimeTotalParries
val PlayerCacheEntity.lifetimeTotalEvades get() = lifetimeAvoidance.lifetimeTotalEvades
val PlayerCacheEntity.lifetimeTotalImmunes get() = lifetimeAvoidance.lifetimeTotalImmunes
val PlayerCacheEntity.lifetimeTotalMisses get() = lifetimeAvoidance.lifetimeTotalMisses
val PlayerCacheEntity.lifetimeTotalAbsorbs get() = lifetimeAvoidance.lifetimeTotalAbsorbs
val PlayerCacheEntity.lifetimeTotalBlockMitigated get() = lifetimeAvoidance.lifetimeTotalBlockMitigated
val PlayerCacheEntity.lifetimeTotalParryMitigated get() = lifetimeAvoidance.lifetimeTotalParryMitigated
val PlayerCacheEntity.lifetimeTotalOdeHeals get() = lifetimeAvoidance.lifetimeTotalOdeHeals

inline fun PlayerCacheEntity.withIdentity(update: PlayerCacheIdentity.() -> PlayerCacheIdentity) =
    copy(identity = identity.update())

inline fun PlayerCacheEntity.withGliders(update: PlayerCacheGliders.() -> PlayerCacheGliders) =
    copy(gliders = gliders.update())

inline fun PlayerCacheEntity.withUtilityItems(update: PlayerCacheUtilityItems.() -> PlayerCacheUtilityItems) =
    copy(utilityItems = utilityItems.update())

inline fun PlayerCacheEntity.withSnakeItems(update: PlayerCacheSnakeItems.() -> PlayerCacheSnakeItems) =
    copy(snakeItems = snakeItems.update())

inline fun PlayerCacheEntity.withBlackDragonItems(update: PlayerCacheBlackDragonItems.() -> PlayerCacheBlackDragonItems) =
    copy(blackDragonItems = blackDragonItems.update())

inline fun PlayerCacheEntity.withDungeonItems(update: PlayerCacheDungeonItems.() -> PlayerCacheDungeonItems) =
    copy(dungeonItems = dungeonItems.update())

inline fun PlayerCacheEntity.withPotions(update: PlayerCachePotions.() -> PlayerCachePotions) =
    copy(potions = potions.update())

inline fun PlayerCacheEntity.withLifetimeCombat(update: PlayerCacheLifetimeCombat.() -> PlayerCacheLifetimeCombat) =
    copy(lifetimeCombat = lifetimeCombat.update())

inline fun PlayerCacheEntity.withLifetimeDebuffs(update: PlayerCacheLifetimeDebuffs.() -> PlayerCacheLifetimeDebuffs) =
    copy(lifetimeDebuffs = lifetimeDebuffs.update())

inline fun PlayerCacheEntity.withLifetimeAvoidance(update: PlayerCacheLifetimeAvoidance.() -> PlayerCacheLifetimeAvoidance) =
    copy(lifetimeAvoidance = lifetimeAvoidance.update())
