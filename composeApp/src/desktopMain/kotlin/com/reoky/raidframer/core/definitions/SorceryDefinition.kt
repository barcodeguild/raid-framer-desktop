package com.reoky.raidframer.core.definitions

object SorceryDefinition : SkillTreeDefinition {
  override val gameId = 7
  override val tree = SkillTreeType.SORCERY
  override val skills = listOf(
    Skill(0, "Flamebolt", 0.8, 0.0, listOf("Flamebolt")),
    Skill(1, "Freezing Arrow", 1.2, 6.0, listOf("Freezing Arrow")),
    Skill(2, "Insulating Lens", 1.0, 0.0, listOf("Insulating Lens")),
    Skill(3, "Arc Lightning", 3.8, 12.0, listOf("Arc Lightning")), // no casting (aww)
    Skill(4, "Magic Circle", 0.0, 21.0, listOf("Magic Circle", "Magic Circle Teleport")), // Teleport (casting) is when the person tps back to their circle
    Skill(5, "Freezing Earth", 0.0, 28.0, listOf("Freezing Earth")),
    Skill(6, "Flame Barrier", 0.0, 26.0, listOf("Flame Barrier")), // no casting
    Skill(7, "Chain Lightning", 0.0, 30.0, listOf("Chain Lightning")), // no casting
    Skill(8, "Searing Rain", 2.0, 13.0, listOf("Searing Rain")), // no casting
    Skill(9, "Frigid Tracks", 1.6, 40.0, listOf("Frigid Tracks")),
    Skill(10, "Meteor Strike", 4.0, 28.0, listOf("Meteor Strike")), // no casting on wave/lightning versions, casting on basic version
    Skill(11, "Gods' Whip", 0.0, 21.0, listOf("Gods' Whip"))
  )
}
