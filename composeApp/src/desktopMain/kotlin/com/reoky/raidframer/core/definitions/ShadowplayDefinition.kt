package com.reoky.raidframer.core.definitions

object ShadowplayDefinition : SkillTreeDefinition {
  override val gameId = 8
  override val tree = SkillTreeType.SHADOWPLAY
  override val skills = listOf(
    Skill(0, "Rapid Strike", 0.0, 0.0, listOf("Rapid Strike"), possibleCastIDs = listOf(18125, 18126, 18127)),
    Skill(1, "Poisoned Weapons", 0.0, 7.2, listOf("Poisoned Weapons"), possibleCastIDs = listOf(10481, 40787, 40788)),
    Skill(2, "Pin Down", 0.0, 16.8, listOf("Pin Down"), possibleCastIDs = listOf(13344)),
    Skill(3, "Drop Back", 0.0, 9.6, listOf("Drop Back"), possibleCastIDs = listOf(12049, 17391, 32355, 36590, 36591)),
    Skill(4, "Overwhelm", 0.0, 14.4, listOf("Overwhelm"), possibleCastIDs = listOf(15958, 16028, 23759, 32799, 35780, 36396, 37641, 40362, 42320, 44873, 45354, 47927, 49042, 8000442, 8001450)),
    Skill(5, "Stalker's Mark", 0.0, 21.6, listOf("Stalker's Mark"), possibleCastIDs = listOf(12139, 44288, 44289)),
    Skill(6, "Wallop", 0.0, 9.6, listOf("Wallop"), possibleCastIDs = listOf()), // no casting
    Skill(7, "Stealth", 0.0, 25.0, listOf("Stealth"), possibleCastIDs = listOf(10082, 11413, 13900, 40077, 44999, 49265, 9000068, 9000069, 9000072, 9000073)),
    Skill(8, "Freerunner", 0.0, 71.0, listOf("Freerunner"), possibleCastIDs = listOf(10189, 39298)),
    Skill(9, "Shadowsmite", 0.0, 19.2, listOf("Shadowsmite"), possibleCastIDs = listOf(23760)),
    Skill(10, "Leech", 0.0, 36.0, listOf("Leech"), possibleCastIDs = listOf(10104, 23707)),
    Skill(11, "Throw Dagger", 0.0, 28.0, listOf("Throw Dagger"), possibleCastIDs = listOf()) // no casting
  )
}
