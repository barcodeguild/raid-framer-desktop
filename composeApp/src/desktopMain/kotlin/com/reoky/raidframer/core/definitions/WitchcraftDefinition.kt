package com.reoky.raidframer.core.definitions

object WitchcraftDefinition : SkillTreeDefinition {

  override val tree = SkillTreeType.WITCHCRAFT
  override val skills = listOf(
    Skill(0, "Earthen Grip", 0.4, 18.0, true, listOf("Earthen Grip")),
    Skill(1, "Enervate", 0.0, 24.0, true,  listOf("Enervated")),
    Skill(2, "Bubble Trap", 1.3, 39.0, true,  listOf("Bubble Trap")),
    Skill(3, "Insidious Whisper", 1.7, 30.0, true,  listOf("Insidious Whisper")),
    Skill(4, "Mirror Warp", 0.0, 40.0, false, listOf("Mirror Warp")),
    Skill(5, "Purge", 0.0, 30.0, false, listOf("Purge")),
    Skill(6, "Lassitude", 0.0, 45.0, false,  listOf("Lassitude")),
    Skill(7, "Stillness", 0.9, 36.0, true, listOf("Stillness")),
    Skill(8, "Dahuta's Breath", 0.0, 21.0, true, listOf("Dahuta's Breath")), // combat only no casting
    Skill(9, "Focal Concussion", 0.0, 27.0, false, listOf("Focal Concussion")),
    Skill(10, "Banshee Wail", 0.0, 45.0, true, listOf("Banshee Wail")),
    Skill(11, "Fiend's Knell", 3.4, 60.0, true, listOf("Fiend's Knell"))
  )
}
