package com.reoky.raidframer.core.definitions

object SpelldanceDefinition : SkillTreeDefinition {
  override val gameId = 14
  override val tree = SkillTreeType.AURAMANCY
  override val skills = listOf(
    Skill(0, "Dancer's Touch", 0.0, 0.0, false, listOf("Dancer's Touch")),
    Skill(1, "Divine Blessing", 0.0, 8.0, false,listOf("Divine Blessing")), // no casting
    Skill(2, "Divine Presence", 0.0, 30.0, false, listOf("Divine Presence")),
    Skill(3, "Conversation", 0.0, 0.0, false, listOf()), // didn't produce any log output, only Communication buff
    Skill(4, "Psychic Shock", 0.0, 4.0, false,  listOf("Psychic Shock")),
    Skill(5, "[Dance] Illusion Dance", 0.0, 2.0, false, listOf("[Dance] Illusion Dance")), // no log output, only Elated Dancer buff on caster
    Skill(6, "Communication Blink", 0.0, 35.0, false, listOf("")), // no output
    Skill(7, "[Dance] Dance of Calm", 0.0, 2.0, false,  listOf("[Dance] Dance of Calm")), // no casting
    Skill(8, "[Dance] Dance of Hope", 0.0, 2.0, false, listOf("[Dance] Dance of Hope")), // no casting
    Skill(9, "[Dance] Dance of Debuff", 0.0, 2.0,  false, listOf("[Dance] Dance of Debuff")), // no casting
    Skill(10, "Communication Maximization", 0.0, 20.0, false, listOf("Communication Maximization")),
    Skill(11, "[Dance] Dance of Sacrifice", 0.0, 2.0, true, listOf("[Dance] Dance of Sacrifice")) // no casting
  )
} // dances produced "Elated Dancer on Stage" buff and Sac dance produced "Sacrifice" buff
// for communication the buffs were "Communication (Self-Ally)" and