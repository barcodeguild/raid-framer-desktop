package com.reoky.raidframer.core.definitions

object GunslingerDefinition : SkillTreeDefinition {
  override val gameId = 13
  override val tree = SkillTreeType.GUNSLINGER
  override val skills = listOf(
    Skill(0, "Ceaseless Fire", 0.0, 0.0, listOf("Ceaseless Fire"), possibleCastIDs = listOf()), // no casting
    Skill(1, "Corrosive Barrage", 0.0, 8.0, listOf("Corrosive Barrage"), possibleCastIDs = listOf()), // no casting
    Skill(2, "Backdraft", 0.4, 16.0, listOf("Backdraft"), possibleCastIDs = listOf()), // no casting
    Skill(3, "Splinter Shell", 0.0, 40.0, listOf("Splinter Shell"), possibleCastIDs = listOf()), // no casting
    Skill(4, "Blight Bolt", 0.0, 23.0, listOf("Blight Bolt"), possibleCastIDs = listOf()), // no casting
    Skill(5, "Room Sweeper", 0.0, 5.0, listOf("Room Sweeper"), possibleCastIDs = listOf()), // buff only
    Skill(6, "Vicious Rebuke", 0.0, 16.0, listOf("Vicious Rebuke"), possibleCastIDs = listOf()),
    Skill(7, "Tactical Roll", 0.0, 8.0, listOf("Tactical Roll"), possibleCastIDs = listOf()), // buff only
    Skill(8, "Trigger Happy", 0.0, 24.0, listOf("Trigger Happy"), possibleCastIDs = listOf()),
    Skill(9, "Reversal", 0.0, 45.0, listOf("Reversal"), possibleCastIDs = listOf()), // debuffs only
    Skill(10, "Sniper's Bane", 0.0, 40.0, listOf("Sniper's Bane"), possibleCastIDs = listOf()), // debuff only
    Skill(11, "Collateral Damage", 1.8, 40.0, listOf("Collateral Damage"), possibleCastIDs = listOf()) // no casting
  )
}
