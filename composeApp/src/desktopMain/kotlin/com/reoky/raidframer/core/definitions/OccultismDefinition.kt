package com.reoky.raidframer.core.definitions

object OccultismDefinition : SkillTreeDefinition {
  override val gameId = 5
  override val tree = SkillTreeType.OCCULTISM
  override val skills = listOf(
    Skill(0, "Crippling Mire", 0.0, 36.0, listOf("Crippling Mire")), // no casting
    Skill(1, "Absorb Lifeforce", 0.0, 28.0, listOf("Absorb Lifeforce")), // no casting
    Skill(2, "Play Dead", 0.0, 40.0, listOf("Play Dead")),
    Skill(3, "Cursed Thorns", 0.0, 36.0, listOf("Cursed Thorns")),
    Skill(4, "Shadow Step", 0.0, 30.0, listOf("Shadow Step")),
    Skill(5, "Boneyard", 0.0, 45.0, listOf("Boneyard")), // no casting
    Skill(6, "Summon Crows", 0.0, 30.0, listOf("Summon Crows")), // no casting, no dmg (Blinded By Crows debuff multi-way)
    Skill(7, "Hell Spear", 0.0, 0.0, listOf("Hell Spear")), // no casting (Flame Hell Spear debuff)
    Skill(8, "Pain Harvest", 2.5, 0.0, listOf("Pain Harvest")), // no casting, no dmg
    Skill(9, "Shadow Vortex", 0.8, 60.0, listOf("Shadow Vortex")), // no casting, no dmg
    Skill(10, "Summon Wraith", 0.0, 60.0, listOf("Summon Wraith")),
    Skill(11, "Death's Vengeance", 2.5, 0.0, listOf("Death's Vengeance"))
  )
}
