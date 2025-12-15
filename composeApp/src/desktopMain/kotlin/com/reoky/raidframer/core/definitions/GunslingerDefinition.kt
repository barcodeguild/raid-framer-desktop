package com.reoky.raidframer.core.definitions

object GunslingerDefinition : SkillTreeDefinition {

  override val tree = SkillTreeType.GUNSLINGER
  override val skills = listOf(
    Skill(0, "Ceaseless Fire", 0.0, 0.0, false, listOf("Ceaseless Fire")), // no casting
    Skill(1, "Corrosive Barrage", 0.0, 8.0, true, listOf("Corrosive Barrage")), //  no casting
    Skill(2, "Backdraft", 0.4, 16.0, false,listOf("Backdraft")), // no casting Backdraft
    Skill(3, "Splinter Shell", 0.0, 40.0, false, listOf("Splinter Shell")), // no casting
    Skill(4, "Blight Bolt", 0.0, 23.0, false, listOf("Blight Bolt")), // no casting
    Skill(5, "Room Sweeper", 0.0, 5.0, false, listOf("Room Sweeper")), // Room Sweeper buff only, no casting
    Skill(6, "Vicious Rebuke", 0.0, 16.0, false, listOf("Vicious Rebuke")),
    Skill(7, "Tactical Roll", 0.0, 8.0, false, listOf("Tactical Roll")), // no casting / Tactical Roll buff only
    Skill(8, "Trigger Happy", 0.0, 24.0, false, listOf("Trigger Happy")),
    Skill(9, "Reversal", 0.0, 45.0, true, listOf("Reversal")), // no casting, no dmg, only debuffs applied
    Skill(10, "Sniper's Bane", 0.0, 40.0, true, listOf("Sniper's Bane")),  // no casting, no dmg, Bane debuff applied to target
    Skill(11, "Collateral Damage", 1.8, 40.0, false, listOf("Collateral Damage")) // no casting
  )
}
