package com.reoky.raidframer.core.browser

import com.reoky.raidframer.core.database.PlayerCacheEntity
import java.io.File

fun exportBrowserCsv(rows: List<PlayerCacheEntity>, file: File): File {
  file.parentFile?.mkdirs()
  var out = if (file.name.endsWith(".csv", ignoreCase = true)) file
  else File(file.parentFile, "${file.nameWithoutExtension}.csv")
  val header = listOf("name", "guild", "faction", "spec", "gearScore", "lastSeen") + BROWSER_STATS.map { it.key }
  val lines = mutableListOf(header.joinToString(","))
  for (p in rows) {
    val vals = listOf(p.playerName, p.lastKnownGuild, p.lastKnownFaction, p.lastKnownSpec, p.lastKnownGearScore.toString(), p.lastSeen.toString()) +
      BROWSER_STATS.map { it.extract(p).toString() }
    lines.add(vals.map { "\"${it.replace("\"", "\"\"")}\"" }.joinToString(","))
  }
  out.writeText(lines.joinToString("\n"))
  return out
}
