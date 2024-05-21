package core.definitions

/*
 * The base definitions class used for identifying what classes players are playing.
 */
interface SkillTreeDefinition {

  val id: Int
  val name: String
  val skills: List<Skill>

}

data class Skill(
  val id: Int,
  val name: String,
  val castTime: Double,
  val cooldown: Double
)