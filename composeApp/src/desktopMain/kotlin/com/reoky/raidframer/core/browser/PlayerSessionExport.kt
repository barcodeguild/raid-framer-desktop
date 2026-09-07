package com.reoky.raidframer.core.browser

import com.reoky.raidframer.core.database.PlayerSessionTotalsEntity
import java.io.File

private fun esc(v: Any?): String = "\"${v.toString().replace("\"", "\"\"")}\""

fun exportSessionCsv(sessions: List<PlayerSessionTotalsEntity>, file: File): File {
  file.parentFile?.mkdirs()
  var out = if (file.name.endsWith(".csv", ignoreCase = true)) file
  else File(file.parentFile, "${file.nameWithoutExtension}.csv")
  val header = listOf(
    "playerName", "sessionStart", "sessionEnd", "sessionType", "sessionTitle",
    "totalDamage", "totalHealing", "totalCC", "totalBuffs", "totalDebuffs",
    "totalCharms", "totalSongs", "totalDistresses", "totalSilences",
    "totalGliderUses", "totalItemSkills", "totalPotions",
    "totalKills", "totalKillsKB", "totalDeaths",
    "totalDamageTaken", "totalHealsReceived", "totalOdeHeals",
    "totalTigerStrikes", "totalFreezes", "totalTrips", "totalBubbles", "totalBracings",
    "totalShieldStrip", "totalWeaponDisables", "totalPotionDisables",
    "totalBdGlider", "totalCrystalWings", "totalGliderDisables", "totalProvoked",
    "totalDefiance", "totalGardenDefiance", "totalPurges", "totalSacDances",
    "totalDeepTranquility", "totalDeependDebuff", "totalThrowDagger",
    "totalStuns", "totalStaggers", "totalPetrification", "totalAbsorbLifeforce",
    "totalCorrosiveBarrage", "totalBlindedByCrows", "totalMistSunder", "totalRegularSunder",
    "totalImpaleImmunity", "totalProtectiveWings", "totalCourageousAction",
    "totalManaBarrier", "totalRevive"
  )
  val lines = mutableListOf(header.joinToString(","))
  for (s in sessions) {
    val vals = listOf(
      s.playerName, s.sessionStart, s.sessionEnd, s.sessionType, s.sessionTitle,
      s.totalDamage, s.totalHealing, s.totalCC, s.totalBuffs, s.totalDebuffs,
      s.totalCharms, s.totalSongs, s.totalDistresses, s.totalSilences,
      s.totalGliderUses, s.totalItemSkills, s.totalPotions,
      s.totalKills, s.totalKillsKB, s.totalDeaths,
      s.totalDamageTaken, s.totalHealsReceived, s.totalOdeHeals,
      s.totalTigerStrikes, s.totalFreezes, s.totalTrips, s.totalBubbles, s.totalBracings,
      s.totalShieldStrip, s.totalWeaponDisables, s.totalPotionDisables,
      s.totalBdGlider, s.totalCrystalWings, s.totalGliderDisables, s.totalProvoked,
      s.totalDefiance, s.totalGardenDefiance, s.totalPurges, s.totalSacDances,
      s.totalDeepTranquility, s.totalDeependDebuff, s.totalThrowDagger,
      s.totalStuns, s.totalStaggers, s.totalPetrification, s.totalAbsorbLifeforce,
      s.totalCorrosiveBarrage, s.totalBlindedByCrows, s.totalMistSunder, s.totalRegularSunder,
      s.totalImpaleImmunity, s.totalProtectiveWings, s.totalCourageousAction,
      s.totalManaBarrier, s.totalRevive
    )
    lines.add(vals.map { esc(it) }.joinToString(","))
  }
  out.writeText(lines.joinToString("\n"))
  return out
}
