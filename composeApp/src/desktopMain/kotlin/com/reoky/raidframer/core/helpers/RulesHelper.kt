package com.reoky.raidframer.core.helpers

// used in the detection of entities to classify whether damage is pvp or pve
// and if pokemon were used in the fight
enum class EntityType(type: String) {
  DECIDING("deciding"),
  PLAYER("player"),
  NPC("npc"),
  DRAGON("dragon"),
  RISO("riso"),
  PET("pet"),
  MOUNT("mount"),
}

// players can't have spaces in their name
fun String.hasSpaces(): Boolean {
  return contains(" ")
}

// real players are playing one of the builds in the index
// EntityAnalyzer.playerBuildFor(playerName: String): Build?
//fun String.isRealPlayer(playerName: String): Boolean {
//  return EntityAnalyzer.playerBuildFor(playerName) != null && !playerName.hasSpaces()
//}