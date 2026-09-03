package com.reoky.raidframer.core.definitions

object SorceryDefinition : SkillTreeDefinition {
  override val gameId = 7
  override val tree = SkillTreeType.SORCERY
  override val skills = listOf(
    Skill(0, "Flamebolt", 0.8, 0.0, listOf("Flamebolt"), possibleCastIDs = listOf(15926, 36474, 42452, 42805, 47718)),
    Skill(1, "Freezing Arrow", 1.2, 6.0, listOf("Freezing Arrow"), possibleCastIDs = listOf(10667, 17354, 38849, 38976, 39563, 40583, 9001047)),
    Skill(2, "Insulating Lens", 1.0, 0.0, listOf("Insulating Lens"), possibleCastIDs = listOf(10153)),
    Skill(3, "Arc Lightning", 3.8, 12.0, listOf("Arc Lightning"), possibleCastIDs = listOf()), // no casting
    Skill(4, "Magic Circle", 0.0, 21.0, listOf("Magic Circle", "Magic Circle Teleport"), possibleCastIDs = listOf()),
    Skill(5, "Freezing Earth", 0.0, 28.0, listOf("Freezing Earth"), possibleCastIDs = listOf(16340)),
    Skill(6, "Flame Barrier", 0.0, 26.0, listOf("Flame Barrier"), possibleCastIDs = listOf(41223)),
    Skill(7, "Chain Lightning", 0.0, 30.0, listOf("Chain Lightning"), possibleCastIDs = listOf()), // no casting
    Skill(8, "Searing Rain", 2.0, 13.0, listOf("Searing Rain"), possibleCastIDs = listOf()), // no casting
    Skill(9, "Frigid Tracks", 1.6, 40.0, listOf("Frigid Tracks"), possibleCastIDs = listOf(11314)),
    Skill(10, "Meteor Strike", 4.0, 28.0, listOf("Meteor Strike"), possibleCastIDs = listOf(10664, 45373, 46785, 50191, 9002799)),
    Skill(11, "Gods' Whip", 0.0, 21.0, listOf("Gods' Whip"), possibleCastIDs = listOf(23593, 23646, 23647, 23648, 23649, 39669, 39670, 39671, 39672, 39673, 46984))
  )
}
