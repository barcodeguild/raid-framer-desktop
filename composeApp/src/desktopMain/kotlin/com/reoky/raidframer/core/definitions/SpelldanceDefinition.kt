package com.reoky.raidframer.core.definitions

object SpelldanceDefinition : SkillTreeDefinition {
  override val gameId = 14
  override val tree = SkillTreeType.AURAMANCY
  override val skills = listOf(
    Skill(0, "Dancer's Touch", 0.0, 0.0, listOf("Dancer's Touch"), possibleCastIDs = listOf()),
    Skill(1, "Divine Blessing", 0.0, 8.0, listOf("Divine Blessing"), possibleCastIDs = listOf(44385)),
    Skill(2, "Divine Presence", 0.0, 30.0, listOf("Divine Presence"), possibleCastIDs = listOf(47964, 48590)),
    Skill(3, "Conversation", 0.0, 0.0, listOf(), possibleCastIDs = listOf()), // only Communication buff
    Skill(4, "Psychic Shock", 0.0, 4.0, listOf("Psychic Shock"), possibleCastIDs = listOf(47966)),
    Skill(5, "[Dance] Illusion Dance", 0.0, 2.0, listOf("[Dance] Illusion Dance"), possibleCastIDs = listOf()),
    Skill(6, "Communication Blink", 0.0, 35.0, listOf(""), possibleCastIDs = listOf()),
    Skill(7, "[Dance] Dance of Calm", 0.0, 2.0, listOf("[Dance] Dance of Calm"), possibleCastIDs = listOf()),
    Skill(8, "[Dance] Dance of Hope", 0.0, 2.0, listOf("[Dance] Dance of Hope"), possibleCastIDs = listOf()),
    Skill(9, "[Dance] Dance of Debuff", 0.0, 2.0, listOf("[Dance] Dance of Debuff"), possibleCastIDs = listOf()),
    Skill(10, "Communication Maximization", 0.0, 20.0, listOf("Communication Maximization"), possibleCastIDs = listOf()),
    Skill(11, "[Dance] Dance of Sacrifice", 0.0, 2.0, listOf("[Dance] Dance of Sacrifice"), possibleCastIDs = listOf())
  )
} // dances produced "Elated Dancer on Stage" buff and Sac dance produced "Sacrifice" buff
// for communication the buffs were "Communication (Self-Ally)" and