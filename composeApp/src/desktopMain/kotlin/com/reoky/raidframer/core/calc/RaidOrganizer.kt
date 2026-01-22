package com.reoky.raidframer.core.calc

import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.PlayerRole

enum class ArrangementMode {
  CLASSIC_ROLES,
  BALANCED_PARTIES
}

object RaidOrganizer {

  fun organize(players: List<PlayerCard>, mode: ArrangementMode): List<PlayerCard> {
    val raids = players.chunked(50)

    return raids.flatMap { raidRoster ->
      when (mode) {
        ArrangementMode.CLASSIC_ROLES -> organizeClassic(raidRoster)
        ArrangementMode.BALANCED_PARTIES -> organizeBalanced(raidRoster)
      }
    }
  }

  private fun organizeClassic(roster: List<PlayerCard>): List<PlayerCard> {
    val leaders = roster.filter { it.leaderships > 0 }
      .sortedByDescending { it.leaderships }
      .toMutableList()
    val tanks = roster.filter {
      PlayerRole.fromInt(it.currentRole) == PlayerRole.GREEN && it.leaderships == 0
    }.toMutableList()
    val healers = roster.filter {
      PlayerRole.fromInt(it.currentRole) == PlayerRole.PINK
    }.toMutableList()
    val dancers = roster.filter {
      PlayerRole.fromInt(it.currentRole) == PlayerRole.PURPLE
    }.toMutableList()
    val redDps = roster.filter {
      PlayerRole.fromInt(it.currentRole) == PlayerRole.RED
    }.toMutableList()
    val blueDps = roster.filter {
      PlayerRole.fromInt(it.currentRole) == PlayerRole.BLUE
    }.toMutableList()
    val others = roster.filter {
      val role = PlayerRole.fromInt(it.currentRole)
      it.leaderships == 0 && role != PlayerRole.GREEN && role != PlayerRole.PINK &&
          role != PlayerRole.PURPLE && role != PlayerRole.RED && role != PlayerRole.BLUE
    }.toMutableList()

    val slots = arrayOfNulls<PlayerCard>(50)

    fun fillSlots(indices: List<Int>, source: MutableList<PlayerCard>) {
      for (i in indices) {
        if (source.isEmpty()) break
        if (i < slots.size && slots[i] == null) {
          slots[i] = source.removeFirst()
        }
      }
    }

    if (leaders.isNotEmpty()) {
      slots[0] = leaders.removeFirst()
    }

    fillSlots(listOf(20, 21, 22, 23, 24), dancers)
    fillSlots(listOf(20, 21, 22, 23, 24), healers)
    fillSlots(listOf(45, 46, 47, 48, 49), dancers)
    fillSlots(listOf(45, 46, 47, 48, 49), healers)
    fillSlots(listOf(15, 16, 17, 18, 19), healers)
    fillSlots(listOf(40, 41, 42, 43, 44), healers)

    fillSlots((1..14).toList(), tanks)
    fillSlots((25..34).toList(), tanks)

    fillSlots(listOf(10, 11, 12, 13, 14), redDps)
    fillSlots(listOf(35, 36, 37, 38, 39), redDps)
    fillSlots(listOf(5, 6, 7, 8, 9), blueDps)
    fillSlots(listOf(30, 31, 32, 33, 34), blueDps)

    for (i in slots.indices) {
      if (slots[i] == null) {
        if (i in 20..24 || i in 40..49) {
          slots[i] = dancers.removeFirstOrNull()
            ?: healers.removeFirstOrNull()
        } else {
          slots[i] = redDps.removeFirstOrNull()
            ?: blueDps.removeFirstOrNull()
                ?: tanks.removeFirstOrNull()
                ?: others.removeFirstOrNull()
                ?: leaders.removeFirstOrNull()
        }
      }
    }

    return slots.filterNotNull()
  }

  private fun organizeBalanced(roster: List<PlayerCard>): List<PlayerCard> {
    val leaders = roster.filter { it.leaderships > 0 }
      .sortedByDescending { it.leaderships }
      .toMutableList()
    val tanks = roster.filter {
      PlayerRole.fromInt(it.currentRole) == PlayerRole.GREEN && it.leaderships == 0
    }.toMutableList()
    val healers = roster.filter {
      PlayerRole.fromInt(it.currentRole) == PlayerRole.PINK
    }.toMutableList()
    val dancers = roster.filter {
      PlayerRole.fromInt(it.currentRole) == PlayerRole.PURPLE
    }.toMutableList()
    val dps = roster.filter {
      val role = PlayerRole.fromInt(it.currentRole)
      it.leaderships == 0 && role != PlayerRole.GREEN && role != PlayerRole.PINK && role != PlayerRole.PURPLE
    }.toMutableList()

    val slots = arrayOfNulls<PlayerCard>(50)

    // Position 1 of each party: Leaders/Tanks (slots 0, 5, 10, 15, 20, 25, 30, 35, 40, 45)
    val tankSlots = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45)
    for (slot in tankSlots) {
      slots[slot] = leaders.removeFirstOrNull() ?: tanks.removeFirstOrNull()
    }

    // Position 5 of each party: Healers (slots 4, 9, 14, 19, 24, 29, 34, 39, 44, 49)
    val healerSlots = listOf(4, 9, 14, 19, 24, 29, 34, 39, 44, 49)
    for (slot in healerSlots) {
      if (healers.isNotEmpty()) {
        slots[slot] = healers.removeFirst()
      }
    }

    // Fill remaining slots with Dancers, then DPS
    for (i in slots.indices) {
      if (slots[i] == null) {
        slots[i] = dancers.removeFirstOrNull()
          ?: dps.removeFirstOrNull()
              ?: tanks.removeFirstOrNull()
              ?: healers.removeFirstOrNull()
              ?: leaders.removeFirstOrNull()
      }
    }

    return slots.filterNotNull()
  }
}
