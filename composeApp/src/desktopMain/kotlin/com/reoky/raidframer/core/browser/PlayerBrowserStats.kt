package com.reoky.raidframer.core.browser

import androidx.compose.ui.graphics.Color
import com.reoky.raidframer.core.database.PlayerCacheEntity
import com.reoky.raidframer.core.helpers.RFColors

data class BrowserStatDef(
  val key: String,
  val label: String,
  val color: Color,
  val extract: (PlayerCacheEntity) -> Long
)

val BROWSER_STATS: List<BrowserStatDef> = listOf(
  BrowserStatDef("damage", "DMG", RFColors.dpsOrange, { it.lifetimeTotalDamage }),
  BrowserStatDef("healing", "Heals", RFColors.healsGreen, { it.lifetimeTotalHealing }),
  BrowserStatDef("cc", "CC", RFColors.ccCyan, { it.lifetimeTotalCCDelivered }),
  BrowserStatDef("buffs", "Buffs", RFColors.buffsBlue, { it.lifetimeTotalBuffsApplied }),
  BrowserStatDef("debuffs", "Debuffs", RFColors.debuffsPurple, { it.lifetimeTotalDebuffsApplied }),
  BrowserStatDef("charms", "Charms", RFColors.charmPink, { it.lifetimeTotalCharms }),
  BrowserStatDef("songs", "Songs", RFColors.charmPink, { it.lifetimeTotalSongs }),
  BrowserStatDef("distress", "Distress", RFColors.distressPurple, { it.lifetimeTotalDistresses }),
  BrowserStatDef("silence", "Silence", RFColors.silencePurple, { it.lifetimeTotalSilences }),
  BrowserStatDef("glider", "Gliders", RFColors.gliderBlue, { it.lifetimeTotalGliderUses }),
  BrowserStatDef("items", "Items", RFColors.itemSkillYellow, { it.lifetimeTotalItemSkillsUsed }),
  BrowserStatDef("potions", "Potions", RFColors.potionTeal, { it.lifetimeTotalPotionUsages }),
  BrowserStatDef("kills", "Kills", RFColors.killsRed, { it.lifetimeTotalKills }),
  BrowserStatDef("killsKB", "KB", RFColors.killsHaranyaGreen, { it.lifetimeTotalKillsKB }),
  BrowserStatDef("deaths", "Deaths", RFColors.killsRed, { it.lifetimeTotalDeaths }),
  BrowserStatDef("dmgTaken", "DMG Taken", RFColors.killsRed, { it.lifetimeTotalDamageTaken }),
  BrowserStatDef("healsRecv", "Heals Recv", RFColors.healsGreen, { it.lifetimeTotalHealsReceived }),
  BrowserStatDef("tiger", "Tiger", RFColors.techNoTigerStrikes, { it.lifetimeTotalTigerStrikes }),
  BrowserStatDef("freeze", "Freeze", RFColors.freezeIceBlue, { it.lifetimeTotalFreezes }),
  BrowserStatDef("trip", "Trip", RFColors.tripsAmber, { it.lifetimeTotalTrips }),
  BrowserStatDef("bubble", "Bubble", RFColors.bubblesCyan, { it.lifetimeTotalBubbles }),
  BrowserStatDef("bracing", "Bracing", RFColors.bracingsGreen, { it.lifetimeTotalBracings }),
  BrowserStatDef("shieldStrip", "Strip", RFColors.shieldStripOrange, { it.lifetimeTotalShieldStrip }),
  BrowserStatDef("weaponDis", "WpnDis", RFColors.weaponDisablesRed, { it.lifetimeTotalWeaponDisables }),
  BrowserStatDef("potionDis", "PotDis", RFColors.potionDisablesPurple, { it.lifetimeTotalPotionDisables }),
  BrowserStatDef("bdGlider", "BD Glider", RFColors.bdGliderTeal, { it.lifetimeTotalBdGlider }),
  BrowserStatDef("crystalWings", "C.Wings", RFColors.crystalWingsBlue, { it.lifetimeTotalCrystalWings }),
  BrowserStatDef("gliderDis", "GlidDis", RFColors.gliderDisablesPink, { it.lifetimeTotalGliderDisables }),
  BrowserStatDef("provoke", "Provoke", RFColors.provokesDeepPurple, { it.lifetimeTotalProvoked }),
  BrowserStatDef("defiance", "Defiance", RFColors.defianceGold, { it.lifetimeTotalDefiance }),
  BrowserStatDef("gardenDefiance", "G.Defiance", RFColors.gardenDefianceBlue, { it.lifetimeTotalGardenDefiance }),
  BrowserStatDef("purge", "Purge", RFColors.purgeGreen, { it.lifetimeTotalPurges }),
  BrowserStatDef("sacDance", "SacDance", RFColors.sacDancePurple, { it.lifetimeTotalSacDances }),
  BrowserStatDef("deepTranq", "DeepTranq", RFColors.deepTranquilityTeal, { it.lifetimeTotalDeepTranquility }),
  BrowserStatDef("deepend", "Deepend", RFColors.deedendDebuffRed, { it.lifetimeTotalDeependDebuff }),
  BrowserStatDef("dagger", "Dagger", RFColors.throwDaggerAmber, { it.lifetimeTotalThrowDagger }),
  BrowserStatDef("stun", "Stun", RFColors.stunDeepRed, { it.lifetimeTotalStuns }),
  BrowserStatDef("stagger", "Stagger", RFColors.staggerBrown, { it.lifetimeTotalStaggers }),
  BrowserStatDef("petrify", "Petrify", RFColors.petrificationGray, { it.lifetimeTotalPetrification }),
  BrowserStatDef("absorb", "Absorb", RFColors.absorbLifeforceMagenta, { it.lifetimeTotalAbsorbLifeforce }),
  BrowserStatDef("corrosive", "Corrosive", RFColors.corrosiveBarrageLime, { it.lifetimeTotalCorrosiveBarrage }),
  BrowserStatDef("crows", "Crows", RFColors.blindedByCrowsDark, { it.lifetimeTotalBlindedByCrows }),
  BrowserStatDef("mistSunder", "MistSunder", RFColors.mistSunderCyan, { it.lifetimeTotalMistSunder }),
  BrowserStatDef("regSunder", "Sunder", RFColors.regularSunderOrange, { it.lifetimeTotalRegularSunder }),
  BrowserStatDef("impale", "Impale", RFColors.impaleImmunitySteel, { it.lifetimeTotalImpaleImmunity }),
  BrowserStatDef("protWings", "ProtWings", RFColors.protectiveWingsGold, { it.lifetimeTotalProtectiveWings }),
  BrowserStatDef("courageous", "Courage", RFColors.courageousActionBright, { it.lifetimeTotalCourageousAction }),
  BrowserStatDef("manaBarrier", "ManaBar", RFColors.manaBarrierBlue, { it.lifetimeTotalManaBarrier }),
  BrowserStatDef("revive", "Revive", RFColors.reviveGhostWhite, { it.lifetimeTotalRevive })
)

val HERO_STATS: List<BrowserStatDef> = listOf(
  BROWSER_STATS[0], BROWSER_STATS[1], BROWSER_STATS[2],
  BrowserStatDef("performance", "Performance", RFColors.courageousActionBright, {
    it.lifetimeTotalDamage + it.lifetimeTotalHealing + it.lifetimeTotalCCDelivered * 1000L
  })
)

data class BrowserCondition(val statKey: String, val op: String, val value: Long)

fun parseShorthand(raw: String): Long {
  val t = raw.trim().uppercase()
  if (t.isEmpty()) return 0L
  val mult = when {
    t.endsWith("M") -> 1_000_000L
    t.endsWith("K") -> 1_000L
    else -> 1L
  }
  val num = t.trimEnd('M', 'K').toDoubleOrNull() ?: return 0L
  return (num * mult).toLong()
}

fun applyBrowserFilters(
  all: List<PlayerCacheEntity>,
  search: String,
  faction: String,
  guild: String,
  spec: String,
  minGear: Int,
  lastSeenAfter: Long,
  conditions: List<BrowserCondition>
): List<PlayerCacheEntity> {
  val byKey = BROWSER_STATS.associateBy { it.key } + HERO_STATS.associateBy { it.key }
  return all.filter { p ->
    (search.isBlank() || p.playerName.contains(search, true) || p.lastKnownGuild.contains(search, true)) &&
      (faction == "All" || p.lastKnownFaction.equals(faction, true)) &&
      (guild.isBlank() || p.lastKnownGuild.contains(guild, true)) &&
      (spec.isBlank() || p.lastKnownSpec.contains(spec, true)) &&
      (minGear <= 0 || p.lastKnownGearScore >= minGear) &&
      (lastSeenAfter <= 0L || p.lastSeen >= lastSeenAfter) &&
      conditions.all { c ->
        val v = byKey[c.statKey]?.extract?.invoke(p) ?: 0L
        when (c.op) {
          ">" -> v > c.value
          "<" -> v < c.value
          ">=" -> v >= c.value
          "=" -> v == c.value
          else -> true
        }
      }
  }
}

fun sortBrowser(
  list: List<PlayerCacheEntity>,
  sortKey: String,
  descending: Boolean
): List<PlayerCacheEntity> {
  val byKey = BROWSER_STATS.associateBy { it.key } + HERO_STATS.associateBy { it.key }
  val def = byKey[sortKey] ?: BROWSER_STATS[0]
  val sorted = list.sortedByDescending { def.extract(it) }
  return if (descending) sorted else sorted.reversed()
}
