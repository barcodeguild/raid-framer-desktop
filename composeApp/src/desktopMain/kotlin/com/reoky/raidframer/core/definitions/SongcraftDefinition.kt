package com.reoky.raidframer.core.definitions

object SongcraftDefinition : SkillTreeDefinition {
  override val gameId = 9
  override val tree = SkillTreeType.SONGCRAFT
  override val skills = listOf(
    Skill(0, "Critical Discord",  0.0, 16.0, listOf("Critical Discord"), possibleCastIDs = listOf()), // no casting
    Skill(1, "Startling Strain", 0.0, 18.0, listOf("Startling Strain"), possibleCastIDs = listOf(11049, 36597, 36598)),
    Skill(2, "[Perform] Quickstep", 0.0, 2.0, listOf("[Perform] Quickstep"), possibleCastIDs = listOf()),
    Skill(3, "Dissonance", 0.0, 30.0, listOf("Dissonance"), possibleCastIDs = listOf()), // no casting
    Skill(4, "Double-Time", 0.0, 28.0, listOf("Double-Time"), possibleCastIDs = listOf(37839, 39299)),
    Skill(5, "[Perform] Ode to Recovery", 0.0, 2.0, listOf("[Perform] Ode to Recovery"), possibleCastIDs = listOf()),
    Skill(6, "Healing Hymn", 0.0, 23.0, listOf("Healing Hymn"), possibleCastIDs = listOf()), // no casting
    Skill(7, "Deadly Refrain", 0.0, 8.0, listOf("Deadly Refrain"), possibleCastIDs = listOf()), // no casting
    Skill(8, "[Perform] Bulwark Ballad", 0.0, 0.0, listOf("[Perform] Bulwark Ballad"), possibleCastIDs = listOf()),
    Skill(9, "Sonic Wave", 0.0, 45.0, listOf("Sonic Wave"), possibleCastIDs = listOf()), // no casting
    Skill(10, "[Perform] Bloody Chantey", 0.0, 2.0, listOf("[Perform] Bloody Chantey"), possibleCastIDs = listOf()),
    Skill(11, "Battle Hymn", 3.6, 118.0, listOf("Battle Hymn"), possibleCastIDs = listOf()) // no casting
  )
}
