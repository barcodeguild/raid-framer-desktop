package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.definitions.petSkillWhitelist
import com.reoky.raidframer.core.model.CombatEvent
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.SuccessfulCastEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Accumulates pet damage/cast events and attributes them to the correct pet cards using a sliding
 * window approach. When multiple same-named pets cast similar spells, damage is divided
 * proportionally among active casters based on their cast timing windows.
 */
object PetAccumulatorInteractor : Interactor() {

  private const val TAG = "PetAccumulator"
  private val mutex = Mutex()

  // Configurable window for correlating damage to casts (in milliseconds)
  private const val DEFAULT_ATTRIBUTION_WINDOW_MS = 15000L
  private const val UNATTRIBUTED_BUFFER_MS = 20_000L

  // Track rider spell casts with their attribution windows
  private data class CastWindow(
    val ownerName: String,
    val petName: String,
    val spellId: Int,
    val castTime: Long,
    val windowEnd: Long,
    val skillCooldown: Long
  )

  private data class PendingRiderCast(
    val ownerName: String,
    val cast: SuccessfulCastEvent,
    val firstSeen: Long
  )

  private data class BufferedEvent<T>(val event: T, val firstSeen: Long)

  private fun SuccessfulCastEvent.sameLogicalCast(other: SuccessfulCastEvent): Boolean =
    timestamp == other.timestamp &&
      source.equals(other.source, ignoreCase = true) &&
      target.equals(other.target, ignoreCase = true) &&
      spellId == other.spellId &&
      spell.equals(other.spell, ignoreCase = true)

  // Event markers for accumulation based on the initiation of the pet skill
  private val riderCastWindow = mutableListOf<CastWindow>()
  private val pendingRiderCasts = mutableListOf<PendingRiderCast>()
  private val accumulatedDamageEvents = mutableListOf<DamageEvent>()
  private val accumulatedCastEvents = mutableListOf<SuccessfulCastEvent>()
  private val unattributedDamageBuffer = mutableListOf<BufferedEvent<DamageEvent>>()
  private val unattributedCastBuffer = mutableListOf<BufferedEvent<SuccessfulCastEvent>>()

  /** Called after Mate metadata is committed, so buffered events are retried immediately. */
  fun onPetRegistered() {
    scope.launch { interact() }
  }

  fun postEvent(event: CombatEvent) {
    when (event) {
      is DamageEvent -> handleDamage(event)
      is SuccessfulCastEvent -> handleSuccessfulCast(event)
      else -> PlayerCacheInteractor.postEvent(event)
    }
  }

  private fun handleDamage(event: DamageEvent) {
    val cleanSource = cleanName(event.source)
    val isKnownPet = PlayerCacheInteractor.getPetEntriesByName(cleanSource).isNotEmpty()
    val isPetSkill = isPetRelatedSkill(event.spellId, event.spell)
    
    if (!isKnownPet && !isPetSkill) {
      // It's neither known as a pet nor uses a pet skill, send it to PlayerCache
      PlayerCacheInteractor.postEventInternal(event)
      return
    }

    Log.info(TAG, "Accumulating pet damage: ${event.source} dealt ${event.damage} with ${event.spell} (id:${event.spellId})")
    scope.launch {
      mutex.withLock {
        accumulatedDamageEvents.add(event)
      }
    }
  }

  private fun handleSuccessfulCast(event: SuccessfulCastEvent) {
    val petSkill = petSkillWhitelist.find { it.id == event.spellId }
    val cleanSource = cleanName(event.source)
    val isKnownPet = PlayerCacheInteractor.getPetEntriesByName(cleanSource).isNotEmpty()

    if (petSkill == null && !isKnownPet) {
        PlayerCacheInteractor.postEventInternal(event)
        return
    }
    
    // We may not have a petSkill here if it's a known pet doing a non-whitelisted spell, but we still log it.
    Log.info(TAG, "Recording pet cast: ${event.source} cast ${event.spell} (id:${event.spellId})")

    scope.launch {
      mutex.withLock {
        if (petSkill?.isPetInitiator == true && (
            accumulatedCastEvents.any { it.sameLogicalCast(event) } ||
              pendingRiderCasts.any { it.cast.sameLogicalCast(event) }
          )) {
          Log.debug(TAG, "Ignoring duplicate rider cast '${event.spell}' from ${event.source} at ${event.timestamp}")
          return@withLock
        }
        accumulatedCastEvents.add(event)

        // If this is a rider spell, create an attribution window
        if (petSkill?.isPetInitiator == true) {
          if (pendingRiderCasts.none { it.cast.sameLogicalCast(event) }) {
            pendingRiderCasts.add(PendingRiderCast(cleanName(event.source), event, System.currentTimeMillis()))
          }
          val windowDuration = calculateWindowDuration(petSkill.cooldown)
          riderCastWindow.add(
            CastWindow(
              ownerName = event.source,
              petName = inferPetNameFromRiderCast(event),
              spellId = event.spellId,
              castTime = event.timestamp,
              windowEnd = event.timestamp + windowDuration,
              skillCooldown = (petSkill.cooldown * 1000).toLong()
            )
          )
        }
      }
    }
  }

  override suspend fun interact() {
    val drainedDamages: List<DamageEvent>
    val drainedCasts: List<SuccessfulCastEvent>
    val recheckBufferedDamage: List<DamageEvent>
    val recheckBufferedCasts: List<SuccessfulCastEvent>

    mutex.withLock {
      drainedDamages = accumulatedDamageEvents.toList()
      drainedCasts = accumulatedCastEvents.toList()
      accumulatedDamageEvents.clear()
      accumulatedCastEvents.clear()

      // Re-evaluate buffered events: try to attribute now that pet cards may exist
      recheckBufferedDamage = unattributedDamageBuffer.map { it.event }
      recheckBufferedCasts = unattributedCastBuffer.map { it.event }
      unattributedDamageBuffer.clear()
      unattributedCastBuffer.clear()

      // Clean up expired windows
      val now = System.currentTimeMillis()
      riderCastWindow.removeAll { it.windowEnd < now }
    }

    if (drainedCasts.isNotEmpty()) {
      processCasts(drainedCasts)
    }

    if (drainedDamages.isNotEmpty()) {
      processDamages(drainedDamages)
    }

    // Re-evaluate previously buffered events against newly created pet cards
    if (recheckBufferedCasts.isNotEmpty()) {
      processCasts(recheckBufferedCasts)
    }
    if (recheckBufferedDamage.isNotEmpty()) {
      processDamages(recheckBufferedDamage)
    }

    // Expire by event time, not by scheduler cadence. The latter dropped events during
    // a busy frame and made a one-second Lua timestamp especially problematic.
    mutex.withLock {
      val now = System.currentTimeMillis()
      val expiredDamage = unattributedDamageBuffer.filter { now - it.firstSeen >= UNATTRIBUTED_BUFFER_MS }
      expiredDamage.forEach { PlayerCacheInteractor.postEventInternal(it.event) }
      unattributedDamageBuffer.removeAll(expiredDamage.toSet())
      unattributedCastBuffer.removeAll { now - it.firstSeen >= UNATTRIBUTED_BUFFER_MS }
      pendingRiderCasts.removeAll { now - it.firstSeen >= UNATTRIBUTED_BUFFER_MS }
    }
  }

  private fun processCasts(casts: List<SuccessfulCastEvent>) {
    casts.forEach { cast ->
      val cleanSource = cleanName(cast.source)
      val initiator = isPetInitiator(cast)
      var candidates = if (initiator) {
        // Rider skills are emitted by the owner, not the pet. Never resolve the
        // owner's name as a pet name or broadcast the cast to every owned pet.
        PlayerCacheInteractor.getPetEntriesByName("")
          .filter { it.value.owner.equals(cleanSource, ignoreCase = true) }
      } else {
        PlayerCacheInteractor.getPetEntriesByName(cleanSource)
      }

      // If no candidates by source name, try inferring pet name from rider spell
      if (candidates.isEmpty() && initiator) {
        val inferredPetName = inferPetNameFromRiderCast(cast)
        if (inferredPetName.isNotBlank()) {
          candidates = PlayerCacheInteractor.getPetEntriesByName(inferredPetName)
            .filter { it.value.owner.equals(cleanSource, ignoreCase = true) }
        }
      }

      when {
        candidates.isEmpty() -> {
          // Pet card doesn't exist yet. Buffer the cast for re-attribution when the card is created.
          val petSkill = petSkillWhitelist.find { it.id == cast.spellId }
          if (petSkill != null) {
            Log.info(TAG, "Buffering pet cast for '${cast.source}' (spell='${cast.spell}' id:${cast.spellId}) - waiting for pet card creation")
            unattributedCastBuffer.add(BufferedEvent(cast, System.currentTimeMillis()))
          }
        }
        candidates.size == 1 -> {
          PlayerCacheInteractor.postPetSuccessfulCast(candidates.first().key, cast)
        }
        else -> {
          if (!initiator) {
            // Apply to all candidates (lightweight)
            candidates.forEach {
              PlayerCacheInteractor.postPetSuccessfulCast(it.key, cast)
            }
          } else {
            // A rider cast without an exact pet CID is ambiguous. Do not create
            // false breath/rocket icons on every pet owned by the rider.
            Log.info(TAG, "Skipping ambiguous rider cast '${cast.spell}' for owner '$cleanSource'; candidate pets=${candidates.map { it.value.name }}")
          }
        }
      }
    }
  }

  private fun processDamages(damages: List<DamageEvent>) {
    damages.forEach { damage ->
      val cleanSource = cleanName(damage.source)
      var candidates = PlayerCacheInteractor.getPetEntriesByName(cleanSource)
      val pendingMatches = pendingRiderCasts.filter { pending ->
        isRelatedSpell(damage.spellId, damage.spell, pending.cast.spellId) &&
          damage.timestamp >= pending.cast.timestamp &&
          damage.timestamp - pending.cast.timestamp <= DEFAULT_ATTRIBUTION_WINDOW_MS
      }

      // IPC damage has spellId=0 and can arrive before Mate metadata. A rider cast
      // identifies the owner even when the pet name is arbitrary, so use that owner
      // to narrow same-named pets before applying the normal window logic.
      if (candidates.isEmpty()) {
        val ownerCandidates = riderCastWindow
          .filter { damage.timestamp >= it.castTime && damage.timestamp <= it.windowEnd }
          .filter { isRelatedSpell(damage.spellId, damage.spell, it.spellId) }
          .map { it.ownerName }
          .distinct()
        if (ownerCandidates.size == 1) {
          candidates = PlayerCacheInteractor.getPetEntriesByName("")
            .filter { it.value.owner.equals(ownerCandidates.single(), ignoreCase = true) }
        }
      }

      if (candidates.size > 1) {
        val matchingWindows = riderCastWindow.filter { window ->
          window.ownerName.equals(candidates.first().value.owner, ignoreCase = true) &&
            damage.timestamp >= window.castTime &&
            damage.timestamp <= window.windowEnd &&
            isRelatedSpell(damage.spellId, damage.spell, window.spellId)
        }
        if (matchingWindows.size == 1) {
          val owner = matchingWindows.single().ownerName
          candidates = candidates.filter { it.value.owner.equals(owner, ignoreCase = true) }
        }
      }

      if (pendingMatches.size == 1 && candidates.isNotEmpty()) {
        val pending = pendingMatches.single()
        val ownerPets = candidates.filter {
          it.value.owner.equals(pending.ownerName, ignoreCase = true)
        }
        if (ownerPets.isNotEmpty()) {
          // A rider cast identifies its owner, while the damage source identifies
          // the pet. If several pets owned by that rider are possible, use the
          // matching cast window to avoid losing the cast/damage association.
          val matchingWindow = riderCastWindow.firstOrNull { window ->
            window.ownerName.equals(pending.ownerName, ignoreCase = true) &&
              window.castTime == pending.cast.timestamp &&
              isRelatedSpell(damage.spellId, damage.spell, window.spellId)
          }
          val selectedPet = if (ownerPets.size == 1) {
            ownerPets.single()
          } else {
            matchingWindow?.let { window ->
              ownerPets.find { it.value.name.equals(window.petName, ignoreCase = true) }
            }
          }
          if (selectedPet != null) {
            PlayerCacheInteractor.postPetSuccessfulCast(selectedPet.key, pending.cast)
            pendingRiderCasts.remove(pending)
            candidates = listOf(selectedPet)
          }
        }
      }

      if (candidates.isEmpty()) {
        // No pet card found for this source. The damage was accumulated because
        // it matched a whitelisted pet skill, but no pet card exists yet.
        // Buffer it for re-attribution when the pet card is created (Mate metadata arrives late).
        if (!PlayerCacheInteractor.isRealPlayer(cleanSource)) {
          Log.info(TAG, "Buffering pet damage for '$cleanSource' (spell='${damage.spell}' id:${damage.spellId}) - waiting for pet card creation")
          unattributedDamageBuffer.add(BufferedEvent(damage, System.currentTimeMillis()))
        } else {
          Log.info(TAG, "No pet card found for damage source: $cleanSource (spell='${damage.spell}' id:${damage.spellId}) - routing to player cache. Known pets: ${PlayerCacheInteractor.getPetEntriesByName("").map { it.value.name }}")
          PlayerCacheInteractor.postEventInternal(damage)
        }
        return@forEach
      }

      // Single pet with this name - direct attribution
      if (candidates.size == 1) {
        PlayerCacheInteractor.postPetDamage(candidates.first().key, damage)
        return@forEach
      }

      // Multiple same-named pets - use sliding window attribution
      val relevantWindows = riderCastWindow.filter { window ->
        window.petName.equals(cleanSource, ignoreCase = true) &&
            damage.timestamp >= window.castTime &&
            damage.timestamp <= window.windowEnd &&
            isRelatedSpell(damage.spellId, damage.spell, window.spellId)
      }

      when {
        relevantWindows.isEmpty() -> {
          // No stable identity exists for a respawned pet. Divide only among
          // the remaining same-name candidates instead of using ephemeral CIDs.
          attributeDamageProportionally(damage, candidates.map { it.key })
        }
        relevantWindows.size == 1 -> {
          // Single active caster window
          val window = relevantWindows.first()
          val petKey = candidates.find { it.value.owner == window.ownerName }?.key
          if (petKey != null) {
            PlayerCacheInteractor.postPetDamage(petKey, damage)
          } else {
            // Fallback
            attributeDamageProportionally(damage, candidates.map { it.key })
          }
        }
        else -> {
          // Multiple active casters - divide proportionally
          val eligiblePetKeys = relevantWindows.mapNotNull { window ->
            candidates.find { it.value.owner == window.ownerName }?.key
          }
          attributeDamageProportionally(damage, eligiblePetKeys)
        }
      }
    }
  }

  private fun attributeDamageProportionally(damage: DamageEvent, petKeys: List<String>) {
    if (petKeys.isEmpty()) return

    val share = (damage.damage.toDouble() / petKeys.size).toLong()
      .coerceIn(-250_000L, 250_000L)
      .toInt()

    Log.debug(TAG, "Dividing ${damage.damage} damage among ${petKeys.size} pets: $share each")

    petKeys.forEach { petKey ->
      val sharedDamage = damage.copy(damage = share)
      PlayerCacheInteractor.postPetDamage(petKey, sharedDamage)
    }
  }

  // Helper functions

  private fun isPetRelatedSkill(spellId: Int, spellName: String): Boolean {
    // Check whitelist by spell ID (cast/success events from log file have spellId)
    if (petSkillWhitelist.any { it.id == spellId }) return true

    // Also check by spell name - IPC addon events always have spellId=0 for damage/heal,
    // so we must match by name for those events.
    if (petSkillWhitelist.any { skill ->
      skill.possibleNames.any { it.equals(spellName, ignoreCase = true) }
    }) return true

    return false
  }

  private fun isPetInitiator(cast: SuccessfulCastEvent): Boolean {
    return petSkillWhitelist.any { skill ->
      skill.isPetInitiator && (
        skill.id == cast.spellId ||
          skill.possibleNames.any { it.equals(cast.spell, ignoreCase = true) }
      )
    }
  }

  private fun inferPetNameFromRiderCast(cast: SuccessfulCastEvent): String {
    // Remove "(Rider)" suffix to get base spell name
    val baseSpell = cast.spell.replace("\\s*\\(Rider\\)".toRegex(), "").trim()

    // Look up the pet skill without rider suffix
    // Try to find the pet card by recent successful casts
    val recentPets = PlayerCacheInteractor.getPetEntriesByName("")
      .filter { it.value.owner.equals(cast.source, ignoreCase = true) }
    if (recentPets.size == 1) return recentPets.first().value.name

    // Fallback: return a generic name based on spell
    return when {
      baseSpell.contains("Scratch") -> "Mara"
      baseSpell.contains("Guided Missiles") -> "Siege Risopoda"
      baseSpell.contains("Dragon's Breath", ignoreCase = true) -> ""
      else -> ""
    }
  }

  private fun isRelatedSpell(damageSpellId: Int, damageSpellName: String, castSpellId: Int): Boolean {
    // Direct match
    if (damageSpellId == castSpellId) return true

    // Dragon breath rider spells -> Clinging Flame / Clinging Flame Explosion
    val dragonBreathRiderIds = setOf(38418, 38699, 38701) // Red, Green, Black
    val dragonDamageIds = setOf(22608, 22609, 22618) // Clinging Flame x2, Clinging Flame Explosion
    if (castSpellId in dragonBreathRiderIds && damageSpellId in dragonDamageIds) {
      return true
    }

    // Drake breath rider spells -> Thunderbreath / Thunderbreath Aftershock
    val drakeBreathRiderIds = setOf(35787) // Thunderbreath (Rider)
    val drakeDamageIds = setOf(35786, 21015) // Thunderbreath, Thunderbreath Aftershock
    if (castSpellId in drakeBreathRiderIds && damageSpellId in drakeDamageIds) {
      return true
    }

    // Also check by name for IPC events where spellId=0
    val castSkill = petSkillWhitelist.find { it.id == castSpellId }
    if (castSkill != null) {
      val isDragonBreath = castSkill.name.contains("Dragon's Breath", ignoreCase = true)
      val isClingingFlame = damageSpellName.contains("Clinging Flame", ignoreCase = true) ||
          damageSpellName.contains("폭발하는 씨앗", ignoreCase = true) ||
          damageSpellName.contains("Раскаленная лава", ignoreCase = true)
      if (isDragonBreath && isClingingFlame) return true

      val isDrakeBreath = castSkill.name.contains("Thunderbreath", ignoreCase = true) ||
          castSkill.name.contains("천둥의 숨결", ignoreCase = true)
      val isThunderDmg = damageSpellName.contains("Thunderbreath", ignoreCase = true) ||
          damageSpellName.contains("천둥의 숨결", ignoreCase = true)
      if (isDrakeBreath && isThunderDmg) return true

      // Scratch -> Bleeding
      if (castSkill.name.contains("Scratch", ignoreCase = true) &&
        damageSpellName.contains("Bleeding", ignoreCase = true)) {
        return true
      }
    }

    // Fallback: check cast spell name against whitelist for IPC events (spellId=0)
    val castNameMatch = petSkillWhitelist.find { skill ->
      skill.possibleNames.any { it.equals(damageSpellName, ignoreCase = true) }
    }
    if (castNameMatch != null) return true

    return false
  }

  private fun calculateWindowDuration(cooldownSeconds: Double): Long {
    // Window should be at least as long as the cooldown, but capped at a max
    val cooldownMs = (cooldownSeconds * 1000).toLong()
    return cooldownMs.coerceIn(5000L, DEFAULT_ATTRIBUTION_WINDOW_MS)
  }

  private fun cleanName(source: String): String {
    return source.replace("\\s*\\([^)]*\\)$".toRegex(), "").trim()
  }
}
