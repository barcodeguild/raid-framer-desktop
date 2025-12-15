package com.reoky.raidframer.core.definitions

import com.reoky.raidframer.core.interactor.Log

/*
 * Whitelist of preferred builds. This is mostly just opinionated. I'm going to put a badge next to players who are playing PvP specs.
 */
val META_CC_SPECS = setOf<SpecType>(
  SpecType.DREAMBREAKER, SpecType.DEFILER, SpecType.REVENANT, SpecType.NIGHTCLOAK, SpecType.REVENANT
)
val META_MELEE_SPECS = setOf<SpecType>(
  SpecType.DARKRUNNER, SpecType.DUSKRAIDER, SpecType.DREADHUNTER, SpecType.DREADRUNNER, SpecType.DUSKDIRGE,
  SpecType.DUSK_DANCER, SpecType.FEARSTALKER, SpecType.FEAR_ARTIST, SpecType.FEAR_AVENGER, SpecType.FEAR_CONTROLLER,
  SpecType.FEAR_RESOLVER, SpecType.FEAR_REVOLUTIONIST, SpecType.FEAR_SAVIOR
)
val META_HEALER_SPECS = setOf<SpecType>(
  SpecType.SOOTHSAYER, SpecType.CONFESSOR, SpecType.ASSASSIN
)
val META_MAGE_SPECS = setOf<SpecType>(
  SpecType.FANATIC, SpecType.DREADRUNNER, SpecType.SPIRITUALIST, SpecType.EQUALIZER
)

/*
 * The base definitions class used for identifying what classes players are playing.
 */
interface SkillTreeDefinition {
  val tree: SkillTreeType
  val skills: List<Skill>
}

data class Skill(
  val id: Int,
  val name: String,
  val castTime: Double,
  val cooldown: Double,
  val consideredCC: Boolean,
  val possibleNames: List<String> = emptyList()
)

// Build a map of all skill-trees to a last-used timestamp initialized to 0.
fun buildSkillTreeLastUsedMap(): MutableMap<SkillTreeType, Long> {
  val map = mutableMapOf<SkillTreeType, Long>()
  SkillTreeType.entries.forEach { treeType -> map[treeType] = 0L }
  return map
}

/**
 * Find a Skill by a string by checking the skill's name and its possibleNames (case-insensitive).
 */
fun findSkillByName(query: String): Skill? {
  val q = query.trim()
  return SkillTreeType.entries.asSequence()
    .flatMap { it.tree.skills.asSequence() }
    .find { skill ->
      skill.name.equals(q, ignoreCase = true) ||
          skill.possibleNames.any { it.equals(q, ignoreCase = true) }
    }
}

/**
 * Return the SkillTreeDefinition that contains the provided Skill, or null if none found.
 * find any where one of the possible names for a spell matches the skill name
 */
fun findSkillTreeForSkill(skill: Skill): SkillTreeType? {
  return SkillTreeType.entries.find { treeType ->
    treeType.tree.skills.any { it.possibleNames.contains(skill.name) } // uses possible names for matching the log file
  }
}

/**
 * Convenience: find the SkillTreeDefinition for a skill name string.
 */
fun findSkillTreeForSpell(spell: String): SkillTreeType? {
  val skill = findSkillByName(spell) ?: return null
  return findSkillTreeForSkill(skill)
}

/*
 * Skill trees sorted by name alphabetically and then indexed by their id starting from 0.
 */
enum class SkillTreeType(val tree: SkillTreeDefinition) {
  ARCHERY(ArcheryDefinition),
  AURAMANCY(AuramancyDefinition),
  BATTLERAGE(BattlerageDefinition),
  DEFENSE(DefenseDefinition),
  GUNSLINGER(GunslingerDefinition),
  MALEDICTION(MaledictionDefinition),
  OCCULTISM(OccultismDefinition),
  SHADOWPLAY(ShadowplayDefinition),
  SONGCRAFT(SongcraftDefinition),
  SORCERY(SorceryDefinition),
  SPELLDANCE(SpelldanceDefinition),
  SWIFTBLADE(SwiftbladeDefinition),
  VITALISM(VitalismDefinition),
  WITCHCRAFT(WitchcraftDefinition);

  companion object {
    fun fromName(name: String): SkillTreeType? {
      return entries.find { it.name.equals(name, ignoreCase = true) }
    }
  }
}

enum class SpecType(val trees: Set<SkillTreeType>) {
  ABOLISHER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY)),
  ABSOLVER(setOf(SkillTreeType.ARCHERY, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  ACTIVIST(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  ADEPT(setOf(SkillTreeType.AURAMANCY, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  ALCHEMIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  ANCHORITE(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE)),
  ANIMIST(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  ANNIHILATOR(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.GUNSLINGER)),
  ANTHROPOMANCER(setOf(SkillTreeType.SORCERY, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  APOSTATE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  ARACHNOMANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE)),
  ARCANE_HUNTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY)),
  ARCANIST(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.SORCERY)),
  ARCHON(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.ARCHERY)),
  ARGENT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.VITALISM)),
  ASSASSIN(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  ASTRAL_RANGER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY)),
  ATHAME(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  AUGMENTOR(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  AUGURER(setOf(SkillTreeType.SORCERY, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  AVATAR(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  BANEBOLT(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER)),
  BANESONG(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  BANSHEE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.SWIFTBLADE)),
  BARRELACE(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER)),
  BARRICADE(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.SWIFTBLADE)),
  BASTION(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY)),
  BATTLEMAGE(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.SORCERY)),
  BATTLEPRIEST(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  BEGUILER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  BEGUILING_DANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  BLACKGUARD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.VITALISM)),
  BLADECHANTER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  BLADEMAGE(setOf(SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  BLADESPELL(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  BLADE_DANCER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  BLASTFUSE(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  BLIGHTCASTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  BLIGHTER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY)),
  BLINKHUNTER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE)),
  BLINKSHOT(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  BLOODMAGE(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.GUNSLINGER)),
  BLOODREAVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM)),
  BLOODSKALD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT)),
  BLOODTHRALL(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  BLOODY_DANCER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  BLOOD_ARROW(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.VITALISM)),
  BONECHILLER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  BONESTALKER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY)),
  BONEWEAVER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  BOUNTY_HUNTER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  BREATHSTEALER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  BUCCANEER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.GUNSLINGER)),
  BULWARK(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.GUNSLINGER)),
  CABALIST(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.SORCERY)),
  CANNONEER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  CARETAKER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  CHARMER(setOf(SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  CLAIRVOYER(setOf(SkillTreeType.DEFENSE, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  CLERIC(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  COMEDIAN(setOf(SkillTreeType.DEFENSE, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  CONFESSOR(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  CONFUTATOR(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  CONJUROR(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.MALEDICTION)),
  CORRUPTOR(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION)),
  CORSAIR(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  COVERT_DANCER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  CRUSADER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.SORCERY)),
  CRYPTWALKER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  CULTIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  CURSEBOLT(setOf(SkillTreeType.DEFENSE, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  CURSEBRAND(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE)),
  CYCLONE(setOf(SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  DAGGERSPELL(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  DARKNESS_ARTIST(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  DARKNESS_AVENGER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  DARKNESS_CONTROLLER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  DARKNESS_FANATIC(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  DARKNESS_RESOLVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  DARKNESS_REVOLUTIONIST(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  DARKNESS_SAVIOR(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  DARKRUNNER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY)),
  DARKSEER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  DARK_AEGIS(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT)),
  DAWNCALLER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT)),
  DEATHCHORD(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  DEATHTRIGGER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  DEATHVICAR(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.GUNSLINGER)),
  DEATHWISH(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  DEATH_PROPHET(setOf(SkillTreeType.VITALISM, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  DEATH_WARDEN(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  DECEIVER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  DEFIANT(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER)),
  DEFILER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM)),
  DEMOLISHER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  DEMONOLOGIST(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.SORCERY)),
  DEMONSHOT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  DERVISH(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.VITALISM)),
  DESTROYER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.SORCERY)),
  DIRGEWEAVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT)),
  DISCIPLE(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  DIVINER(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  DOOMBRINGER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  DOOMCALLER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  DOOMLORD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM)),
  DOOMSHOT(setOf(SkillTreeType.ARCHERY, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  DOOMSKALD(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  DOOMSPEAKER(setOf(SkillTreeType.SORCERY, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  DOURGUARD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.MALEDICTION)),
  DREADBOW(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY)),
  DREADDART(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  DREADHUNTER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY)),
  DREADNAUGHT(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY)),
  DREADRUNNER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  DREADSTONE(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY)),
  DREAMBREAKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY)),
  DREAMSPINNER(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  DRUID(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.VITALISM)),
  DUSKDIRGE(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  DUSKRAIDER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  DUSK_DANCER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  EARTHSINGER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  EBONSHIELD(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  EBONSONG(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  EDGERIPPER(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  EDGEWALKER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.VITALISM)),
  EIDOLON(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY)),
  EMISSARY(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION)),
  ENCHANTRIX(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT)),
  ENFORCER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.SORCERY)),
  ENIGMATIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  EQUALIZER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION)),
  EVANGELIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  EVISCERATOR(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  EVOKER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  EXECUTIONER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY)),
  EXORCIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  FANATIC(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  FANGBORN(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  FARSLAYER(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.SORCERY)),
  FATEMARK(setOf(SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  FATESPINNER(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  FEARFUL_FANATIC(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  FEARSTALKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  FEAR_ARTIST(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  FEAR_AVENGER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  FEAR_CONTROLLER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.SPELLDANCE)),
  FEAR_RESOLVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.SPELLDANCE)),
  FEAR_REVOLUTIONIST(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  FEAR_SAVIOR(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  FEVERSONG(setOf(SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  FLASHBLADE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.SWIFTBLADE)),
  FLESHSHAPER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  FORSAKER(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE)),
  FREEBOOTER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  FURY_CANON(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  FURY_MAGE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  GHOSTBLADE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION)),
  GLAMOROUS_AVENGER(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  GLAMOROUS_REVOLUTIONIST(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  GLAMOROUS_SAVIOR(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  GLOOMKNIGHT(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  GLOOMSPELL(setOf(SkillTreeType.SORCERY, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  GLOOMSTALKER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  GRAVEBOW(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE)),
  GRAVESHOT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.GUNSLINGER)),
  GRAVESINGER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT)),
  GRAVE_ADMIRAL(setOf(SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  GRIMSHOT(setOf(SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  GYPSY(setOf(SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  HARBINGER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY)),
  HARUSPEX(setOf(SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  HEARTSBANE(setOf(SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  HELLSHIELD(setOf(SkillTreeType.DEFENSE, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  HELLWEAVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  HERALD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT)),
  HEREAFTER_ARTIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  HEREAFTER_AVENGER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  HEREAFTER_CONTROLLER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.SPELLDANCE)),
  HEREAFTER_FANATIC(setOf(SkillTreeType.AURAMANCY, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  HEREAFTER_RESOLVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.SPELLDANCE)),
  HEREAFTER_REVOLUTIONIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  HEREAFTER_SAVIOR(setOf(SkillTreeType.AURAMANCY, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  HEXBLADE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE)),
  HEXBOLT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.GUNSLINGER)),
  HEXDANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.SPELLDANCE)),
  HEXSNIPER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.GUNSLINGER)),
  HEX_RANGER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT)),
  HEX_WARDEN(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY)),
  HIEROPHANT(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.VITALISM)),
  HONORGUARD(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT)),
  HORDEBREAKER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM)),
  HOWLER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT)),
  ILLUSION_APOSTLE(setOf(SkillTreeType.VITALISM, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  ILLUSION_ARGENT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  ILLUSION_BLADE(setOf(SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  ILLUSION_BLADESPELL(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  ILLUSION_CHASER(setOf(SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  ILLUSION_DANCER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.SPELLDANCE)),
  ILLUSION_DISCIPLE(setOf(SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  ILLUSION_EVIL_SPIRIT(setOf(SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  ILLUSION_FIGHTER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.SPELLDANCE)),
  ILLUSION_GUNSLINGER(setOf(SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  ILLUSION_HUNTER(setOf(SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  ILLUSION_MAGICIAN(setOf(SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  ILLUSION_MONK(setOf(SkillTreeType.SORCERY, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  ILLUSION_MONK_ARCHER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  ILLUSION_PILGRIM(setOf(SkillTreeType.ARCHERY, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  ILLUSION_REAPER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  ILLUSION_SAGE(setOf(SkillTreeType.SORCERY, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  ILLUSION_STALKER(setOf(SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  ILLUSION_SWORDSPELL(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  ILLUSION_TERMINATOR(setOf(SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  ILLUSION_TRICKSTER(setOf(SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  ILLUSION_WARMACHINE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  IMMORTALIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  INCANTATOR(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  INFILTRATOR(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  INQUISITOR(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  INSTIGATOR(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  INVADER(setOf(SkillTreeType.DEFENSE, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  INVOKER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  IRONSOUL(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.MALEDICTION)),
  JINXMENDER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  JUSTICAR(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.VITALISM)),
  LAMENTOR(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  LEADBITER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  LEADDANCER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  LEGIONNAIRE(setOf(SkillTreeType.DEFENSE, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  LIBERATOR(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.ARCHERY)),
  LIFESTEALER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  LIGHTSOUL(setOf(SkillTreeType.ARCHERY, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  LOREBREAKER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT)),
  MAGISTER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  MARAUDER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  MARROWBLADE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  MENTALIST(setOf(SkillTreeType.SORCERY, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  MERCENARY(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.SWIFTBLADE)),
  MINDSLAVER(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION)),
  MINDSLAYER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  MINSTREL(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  MUSICIAN(setOf(SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  NATURALIST(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  NECROMANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.VITALISM)),
  NIGHTBEARER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  NIGHTBLADE(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY)),
  NIGHTCLOAK(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM)),
  NIGHTHAUNT(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  NIGHTMARE_ARTIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_AVENGER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_CONTROLLER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_FANATIC(setOf(SkillTreeType.OCCULTISM, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_RESOLVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_REVOLUTIONIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_SAVIOR(setOf(SkillTreeType.OCCULTISM, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  NIGHTWITCH(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  NOCTURNE(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  NULLWEAVER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  OATHSWORN(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  ORACLE(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.VITALISM)),
  OUTRIDER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY)),
  PALADIN(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.VITALISM)),
  PHANTASM(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT)),
  PHANTOM(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.SWIFTBLADE)),
  PITFIEND(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  PIT_FIGHTER(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION)),
  PLANESHIFTER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY)),
  PONTIFEX(setOf(SkillTreeType.AURAMANCY, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  POXBANE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT)),
  PRIMEVAL(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY)),
  PRIVATEER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  PURGATOR(setOf(SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  PURIFIER(setOf(SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  QUICKDRAW(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  RAGEBINDER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  RAGECHANTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER)),
  RANGER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  RAVAGER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.SORCERY)),
  REALMSHIFTER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  REAPER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  REINCARNATOR(setOf(SkillTreeType.VITALISM, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  REQUIEM(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  REVELATOR(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  REVENANT(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.SORCERY)),
  REVOLUTIONIST(setOf(SkillTreeType.DEFENSE, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  RUNEBREAKER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  SABOTEUR(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  SCION(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  SEAL_ARTIST(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  SEAL_CONTROLLER(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.SPELLDANCE)),
  SEAL_FANATIC(setOf(SkillTreeType.DEFENSE, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  SEAL_RESOLVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.SPELLDANCE)),
  SEAL_REVOLUTIONIST(setOf(SkillTreeType.DEFENSE, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  SECRET_CHOREOGRAPHER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  SHADEHUNTER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY)),
  SHADESTRIKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY)),
  SHADOWBANE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.VITALISM)),
  SHADOWBLADE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY)),
  SHADOWDANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.SPELLDANCE)),
  SHADOWGUARD(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  SHADOWKNIGHT(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY)),
  SHADOWSONG(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  SHADOW_PROPHET(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  SHADOW_REAPER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  SHAMAN(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  SHROUDMASTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY)),
  SHROUDSONG(setOf(SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  SHROUDSPELL(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  SKULLKNIGHT(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM)),
  SKULL_WARDEN(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.GUNSLINGER)),
  SONG_CONTROLLER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  SONG_FANATIC(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  SOOTHSAYER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  SORROWSONG(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  SOULBOW(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.VITALISM)),
  SOULLEECH(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  SOULSONG(setOf(SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  SOULTHIEF(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION)),
  SPECTRE(setOf(SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  SPELLBINDER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  SPELLBOW(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.SORCERY)),
  SPELLSINGER(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  SPELLSONG(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  SPELLSWORD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  SPELLWEAVER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  SPIRITUALIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  STONE_ARROW(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY)),
  STORMCASTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.SORCERY)),
  STORMCHASER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.SORCERY)),
  STRIKING_DANCER(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.SPELLDANCE)),
  SUNSET_DANCER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  SWIFTSHOT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE)),
  SWIFTSTONE(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  SYNERGIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  TEMPEST(setOf(SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  TEMPLAR(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.VITALISM)),
  THAUMATURGE(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.SORCERY)),
  TOMBCALLER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT)),
  TOMBSONG(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  TOMB_WARDEN(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT)),
  TOUGH_DANCER(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.SPELLDANCE)),
  TRAGEDY_DANCER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  TRICKSTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY)),
  TWISTEDSHOT(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE)),
  VANGUARD(setOf(SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  VENOMBITE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  VICIOUS_CHOREOGRAPHER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  VISIONARY(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION)),
  VOIDSTALKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION)),
  VOIDWALKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  WARCASTER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  WARPRIEST(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.VITALISM)),
  WAR_DANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.SPELLDANCE)),
  WIDOWHAUNT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER)),
  WILDCLAW(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION)),
  WILDMAGE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  WINDSEEKER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION)),
  WINDSHAPER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  WINDSOUL(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE)),
  WISPWALKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.SWIFTBLADE)),
  WITCH_DOCTOR(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION)),
  WRATHSLAYER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.GUNSLINGER)),
  ZEALOT(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  ZEPHYR(setOf(SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),

  UNKNOWN(setOf());

  fun toDisplayName(): String = name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }

  companion object {
    private val byTrees = entries.associateBy { it.trees }
    fun fromTrees(trees: Set<SkillTreeType>): SpecType = byTrees[trees] ?: SpecType.UNKNOWN
    fun fromName(name: String): SpecType? = entries.find { it.name.equals(name, ignoreCase = true) }
  }
}

fun SpecType.isPvPFriendly(): Boolean {
  return when (this) {
    // tanks
    SpecType.DREAMBREAKER, SpecType.DEFILER, SpecType.REVENANT -> true
    // healers
    SpecType.CONFESSOR, SpecType.ASSASSIN -> true
    // melee dps
    SpecType.BLOODREAVER, SpecType.BLADE_DANCER, SpecType.HEX_WARDEN, SpecType.HEXBLADE -> true
    else -> false
  }
}

fun SpecType.isCCTank(): Boolean {
  if (trees.contains(SkillTreeType.WITCHCRAFT) && trees.contains(SkillTreeType.OCCULTISM)) return true
  if (META_CC_SPECS.contains(this)) return true
  return false
}

enum class SpecIndication() {
  DPS, TANK, HEALS;
}

/*
 * Initiating Spells : Spells that when cast
 */
val initiatingSpells = listOf(
  "Mana Bolts",
  "Divebomb",
  "Charge",
  "Tiger Strike",
  "Shoot Arrow",
  "Concussive Arrow",
  "Endless Arrows",
  "Absorb Lifeforce",
  "Enervated",
  "Ceaseless Fire",
  "Flamebolt",
  "Freezing Arrow",
  "Arc Lightning",
  "Electrical Arrow",
  "Rapid Strike",
  "Pin Down",
  "Blade Flurry",
  "Entangle",
  "Dancer's Touch",
  "Holy Bolt",
  "Revive",
  "Mana Barrier",
  "Fervent Healing",
  "Bull Rush",
  "Critical Discord"
)
