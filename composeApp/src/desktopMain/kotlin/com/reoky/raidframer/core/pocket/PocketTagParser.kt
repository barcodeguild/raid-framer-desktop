package com.reoky.raidframer.core.pocket

private val hashtagPattern = Regex("(?<![\\w-])#([\\p{L}\\p{N}_-]+)")
private val playerReferencePattern = Regex("(?<![\\w-])@([\\p{L}\\p{N}_-]+)")
private val wordPattern = Regex("[\\p{L}\\p{N}_-]+")

data class PocketTagMatch(
  val displayValue: String,
  val normalizedValue: String,
)

fun extractPocketTags(markdown: String, knownPlayerNames: Collection<String> = emptyList()): List<PocketTagMatch> {
  val matches = linkedMapOf<String, PocketTagMatch>()

  fun add(value: String) {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return
    val normalized = trimmed.lowercase()
    matches.putIfAbsent(normalized, PocketTagMatch(trimmed, normalized))
  }

  hashtagPattern.findAll(markdown).forEach { add(it.groupValues[1]) }

  val canonicalPlayers = knownPlayerNames
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .distinctBy { it.lowercase() }
    .sortedByDescending { it.length }
  val playersByNormalizedName = canonicalPlayers.associateBy { it.lowercase() }

  playerReferencePattern.findAll(markdown).forEach { match ->
    val value = match.groupValues[1]
    add(playersByNormalizedName[value.lowercase()] ?: value)
  }

  val words = wordPattern.findAll(markdown).map { it.value.lowercase() }.toSet()
  canonicalPlayers.forEach { player ->
    if (player.lowercase() in words) add(player)
  }

  return matches.values.toList()
}
