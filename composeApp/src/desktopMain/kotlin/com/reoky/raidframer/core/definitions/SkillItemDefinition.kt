package com.reoky.raidframer.core.definitions

/*
 * Skill trees sorted by name alphabetically and then indexed by their id starting from 0.
 */
enum class SkillItemType(val definition: SkillItemDefinition) {
  UTILITY(UtilityDefinition),
  GLIDER(GliderDefinition),
  POTION(PotionDefinition),
  SUMMON(SummonDefinition);

  companion object {
    fun fromName(name: String): SkillItemType? {
      return entries.find { it.name.equals(name, ignoreCase = true) }
    }
  }
}

interface SkillItemDefinition {
  val skills: List<Skill>
}
