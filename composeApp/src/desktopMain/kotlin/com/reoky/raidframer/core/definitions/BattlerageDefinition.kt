package com.reoky.raidframer.core.definitions

object BattlerageDefinition : SkillTreeDefinition {
  override val gameId = 1
  override val tree = SkillTreeType.BATTLERAGE
  override val skills = listOf(
    Skill(0, "Triple Slash", 0.0, 0.0, listOf("Triple Slash"), possibleCastIDs = listOf(18132, 18134, 23768, 36401, 36402, 36403)),
    Skill(1, "Charge", 0.0, 12.0, listOf("Charge"), possibleCastIDs = listOf(14056, 15050, 15188, 23765, 39026, 39125, 39173, 39231, 39552, 42331, 42799, 42835, 42865, 46679, 46691, 46981, 47730)),
    Skill(2, "Battle Focus", 0.0, 90.0, listOf("Battle Focus"), possibleCastIDs = listOf(10377)),
    Skill(3, "Whirlwind Slash", 0.0, 12.0, listOf("Whirlwind Slash"), possibleCastIDs = listOf()), // no casting
    Skill(4, "Sunder Earth", 0.0, 16.0, listOf("Sunder Earth"), possibleCastIDs = listOf(39622, 44254, 9001751)),
    Skill(5, "Frenzy", 0.0, 90.0, listOf("Frenzy"), possibleCastIDs = listOf(10455, 23763, 43189)),
    Skill(6, "Precision Strike", 0.0, 21.0, listOf("Precision Strike"), possibleCastIDs = listOf(12026, 23766)),
    Skill(7, "Tiger Strike", 0.0, 18.0, listOf("Tiger Strike"), possibleCastIDs = listOf(23769, 45396)),
    Skill(8, "Bondbreaker", 0.0, 18.0, listOf("Bondbreaker"), possibleCastIDs = listOf(12034)),
    Skill(9, "Terrifying Roar", 0.0, 18.0, listOf("Terrifying Roar"), possibleCastIDs = listOf(15304, 18308, 22389, 39229, 43632, 44417, 47720, 47723)),
    Skill(10, "Hammer Toss", 0.0, 29.0, listOf("Hammer Toss"), possibleCastIDs = listOf()), // no casting
    Skill(11, "Behind Enemy Lines", 0.0, 21.0, listOf("Behind Enemy Lines"), possibleCastIDs = listOf(23587, 39661, 39662))
  )
}
