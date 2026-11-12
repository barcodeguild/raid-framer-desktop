package com.reoky.raidframer.core.helpers

import com.reoky.raidframer.core.definitions.MetaSpecsRepo
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.database.PlayerAttendanceRank
import com.reoky.raidframer.core.model.PlayerRole

/*
 * Guess the player's role based on their recent actions / spec / past roles. Really whatever we can glean.
 * This is going to be kind of rough and ready at first.
 */
fun SpecType.guessPlayerRole(): PlayerRole {
  val meta = MetaSpecsRepo.current
  if (this in meta.dancer) return PlayerRole.PURPLE
  if (this in meta.cc) return PlayerRole.GREEN // always green for cc specs / tanks
  if (this in meta.melee) return PlayerRole.GREEN // melee dps are green if their gear is above 17k
  if (this in meta.healer) return PlayerRole.PINK
  if (this in meta.mage) return PlayerRole.RED
  if (this in meta.ranged) return PlayerRole.BLUE
  return PlayerRole.BLUE
}

/** The role a player is ranked under for attendance purposes. Players without a recognized meta spec rank nowhere. */
enum class AttendanceRole { HEALER, CC_TANK, DPS }

/**
 * Classifies a spec name (as stored in player_cache.lastKnownSpec) into an attendance role,
 * using the user's meta spec sets. Healer and cc/tank take precedence; DPS covers the melee,
 * mage, and ranged meta categories. Dancers and unknown specs rank nowhere.
 */
fun String.attendanceRole(): AttendanceRole? {
  val spec = SpecType.fromName(this) ?: return null
  val meta = MetaSpecsRepo.current
  return when {
    spec in meta.healer -> AttendanceRole.HEALER
    spec in meta.cc -> AttendanceRole.CC_TANK
    spec in meta.melee || spec in meta.mage || spec in meta.ranged -> AttendanceRole.DPS
    else -> null
  }
}

/** Filters a lifetime ranking to one attendance role, best-attended first with [tiebreak] deciding ties. */
fun List<PlayerAttendanceRank>.rankedForRole(role: AttendanceRole, tiebreak: (PlayerAttendanceRank) -> Long): List<PlayerAttendanceRank> =
  filter { it.lastKnownSpec.attendanceRole() == role }
    .sortedWith(compareByDescending<PlayerAttendanceRank> { it.sessionCount }.thenByDescending(tiebreak))
