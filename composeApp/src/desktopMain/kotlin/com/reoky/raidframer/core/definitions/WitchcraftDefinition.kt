package com.reoky.raidframer.core.definitions

object WitchcraftDefinition : SkillTreeDefinition {
  override val gameId = 2
  override val tree = SkillTreeType.WITCHCRAFT
  override val skills = listOf(
    Skill(0, "Earthen Grip", 0.4, 18.0, listOf("Earthen Grip"), possibleCastIDs = listOf(14376, 16052, 35681, 36451)),
    Skill(1, "Enervate", 0.0, 24.0, listOf("Enervated"), possibleCastIDs = listOf(10159, 23777, 44252, 50189)),
    Skill(2, "Bubble Trap", 1.3, 39.0, listOf("Bubble Trap"), possibleCastIDs = listOf(10154, 23775, 36453, 46195, 46196, 46197)),
    Skill(3, "Insidious Whisper", 1.7, 30.0, listOf("Insidious Whisper"), possibleCastIDs = listOf(10409, 21410, 23776, 46191, 46192, 46193)),
    Skill(4, "Mirror Warp", 0.0, 40.0, listOf("Mirror Warp"), possibleCastIDs = listOf(23934, 39291, 39292)),
    Skill(5, "Purge", 0.0, 30.0, listOf("Purge"), possibleCastIDs = listOf(10712, 23778)),
    Skill(6, "Lassitude", 0.0, 45.0, listOf("Lassitude"), possibleCastIDs = listOf(10134, 23774)),
    Skill(7, "Stillness", 0.9, 36.0, listOf("Stillness", "Silence"), possibleCastIDs = listOf(10665, 16423)),
    Skill(8, "Dahuta's Breath", 0.0, 21.0, listOf("Dahuta's Breath"), possibleCastIDs = listOf()),
    Skill(9, "Focal Concussion", 0.0, 27.0, listOf("Focal Concussion"), possibleCastIDs = listOf(11353)),
    Skill(10, "Banshee Wail", 0.0, 45.0, listOf("Banshee Wail"), possibleCastIDs = listOf(36455, 36656)),
    Skill(11, "Fiend's Knell", 3.4, 60.0, listOf("Fiend's Knell"), possibleCastIDs = listOf(23588, 32925))
  )
}
