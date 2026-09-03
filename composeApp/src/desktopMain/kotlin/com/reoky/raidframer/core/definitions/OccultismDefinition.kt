package com.reoky.raidframer.core.definitions

object OccultismDefinition : SkillTreeDefinition {
  override val gameId = 5
  override val tree = SkillTreeType.OCCULTISM
  override val skills = listOf(
    Skill(0, "Crippling Mire", 0.0, 36.0, listOf("Crippling Mire"), possibleCastIDs = listOf(15054, 15422, 36620)),
    Skill(1, "Absorb Lifeforce", 0.0, 28.0, listOf("Absorb Lifeforce"), possibleCastIDs = listOf()), // no casting
    Skill(2, "Play Dead", 0.0, 40.0, listOf("Play Dead"), possibleCastIDs = listOf(10488)),
    Skill(3, "Cursed Thorns", 0.0, 36.0, listOf("Cursed Thorns"), possibleCastIDs = listOf(34171, 46140, 46180, 46182)),
    Skill(4, "Shadow Step", 0.0, 30.0, listOf("Shadow Step"), possibleCastIDs = listOf(12075, 23761, 23811, 39116, 39223, 39295, 39296, 39339, 39340)),
    Skill(5, "Boneyard", 0.0, 45.0, listOf("Boneyard"), possibleCastIDs = listOf(47333)),
    Skill(6, "Summon Crows", 0.0, 30.0, listOf("Summon Crows"), possibleCastIDs = listOf(12662, 46184, 46185, 46186)),
    Skill(7, "Hell Spear", 0.0, 0.0, listOf("Hell Spear"), possibleCastIDs = listOf(10135, 36586, 44346, 44347, 50190)),
    Skill(8, "Pain Harvest", 2.5, 0.0, listOf("Pain Harvest"), possibleCastIDs = listOf()), // no casting, no dmg
    Skill(9, "Shadow Vortex", 0.8, 60.0, listOf("Shadow Vortex"), possibleCastIDs = listOf(46945, 47204)),
    Skill(10, "Summon Wraith", 0.0, 60.0, listOf("Summon Wraith"), possibleCastIDs = listOf(10434, 36626, 36627)),
    Skill(11, "Death's Vengeance", 2.5, 0.0, listOf("Death's Vengeance"), possibleCastIDs = listOf(23591, 37935, 40719))
  )
}
