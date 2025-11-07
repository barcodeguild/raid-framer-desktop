package com.reoky.raidframer.core.definitions

object SongcraftDefinition : SkillTreeDefinition {

  override val tree = SkillTreeType.SONGCRAFT
  override val skills = listOf(
    Skill(0, "Critical Discord", 0.0, 16.0, listOf("Critical Discord")), // no casting
    Skill(1, "Startling Strain", 0.0, 18.0, listOf("Startling Strain")),
    Skill(2, "[Perform] Quickstep", 0.0, 2.0, listOf("[Perform] Quickstep")),
    Skill(3, "Dissonance", 0.0, 30.0, listOf("Dissonance")), // no casting
    Skill(4, "Double-Time", 0.0, 28.0, listOf("Double-Time")),
    Skill(5, "[Perform] Ode to Recovery", 0.0, 2.0, listOf("[Perform] Ode to Recovery")),
    Skill(6, "Healing Hymn", 0.0, 23.0, listOf("Healing Hymn")), // no casting
    Skill(7, "Deadly Refrain", 0.0, 8.0, listOf("Deadly Refrain")), // no casting, no dmg, no visible buff (stacks of Rythem)
    Skill(8, "[Perform] Bulwark Ballad", 0.0, 0.0, listOf("[Perform] Bulwark Ballad")),
    Skill(9, "Sonic Wave", 0.0, 45.0, listOf("Sonic Wave")), // no casting
    Skill(10, "[Perform] Bloody Chantey", 0.0, 2.0, listOf("[Perform] Bloody Chantey")),
    Skill(11, "Battle Hymn", 3.6, 118.0, listOf("Battle Hymn")) // no casting
  )
}
