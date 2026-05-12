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

  // Track rider spell casts with their attribution windows
  private data class CastWindow(
    val ownerName: String,
    val petName: String,
    val spellId: Int,
    val castTime: Long,
    val windowEnd: Long,
    val skillCooldown: Long
  )

  // Event markers for accumulation based on the initiation of the pet skill
  private val riderCastWindow = mutableListOf<CastWindow>()
  private val accumulatedDamageEvents = mutableListOf<DamageEvent>()
  private val accumulatedCastEvents = mutableListOf<SuccessfulCastEvent>()

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
        accumulatedCastEvents.add(event)

        // If this is a rider spell, create an attribution window
        if (petSkill != null && isRiderSpell(event.spell)) {
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

    mutex.withLock {
      drainedDamages = accumulatedDamageEvents.toList()
      drainedCasts = accumulatedCastEvents.toList()
      accumulatedDamageEvents.clear()
      accumulatedCastEvents.clear()

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
  }

  private fun processCasts(casts: List<SuccessfulCastEvent>) {
    casts.forEach { cast ->
      val cleanSource = cleanName(cast.source)
      val candidates = PlayerCacheInteractor.getPetEntriesByName(cleanSource)

      when {
        candidates.isEmpty() -> {
          // Pet card doesn't exist yet; will be created by CompanionInteractor
        }
        candidates.size == 1 -> {
          PlayerCacheInteractor.postPetSuccessfulCast(candidates.first().key, cast)
        }
        else -> {
          // Multiple same-named pets; try CID match first
          val byCid = candidates.find { cast.cid in it.value.recentCids }
          if (byCid != null) {
            PlayerCacheInteractor.postPetSuccessfulCast(byCid.key, cast)
          } else {
            // Apply to all candidates (lightweight)
            candidates.forEach {
              PlayerCacheInteractor.postPetSuccessfulCast(it.key, cast)
            }
          }
        }
      }
    }
  }

  private fun processDamages(damages: List<DamageEvent>) {
    damages.forEach { damage ->
      val cleanSource = cleanName(damage.source)
      val candidates = PlayerCacheInteractor.getPetEntriesByName(cleanSource)

      if (candidates.isEmpty()) {
        Log.debug(TAG, "No pet card found for damage source: $cleanSource")
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
          // No active windows; try CID match
          val byCid = candidates.find { damage.cid in it.value.recentCids }
          if (byCid != null) {
            PlayerCacheInteractor.postPetDamage(byCid.key, damage)
          } else {
            // Fallback: divide equally among all candidates
            attributeDamageProportionally(damage, candidates.map { it.key })
          }
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
    // Check whitelist
    if (petSkillWhitelist.any { it.id == spellId }) return true

    // Check for DoT effects by name patterns
    val dotPatterns = listOf("Bleeding", "Poison", "Burn")
    return dotPatterns.any { spellName.contains(it, ignoreCase = true) }
  }

  private fun isRiderSpell(spellName: String): Boolean {
    return spellName.contains("(Rider)", ignoreCase = true)
  }

  private fun inferPetNameFromRiderCast(cast: SuccessfulCastEvent): String {
    // Remove "(Rider)" suffix to get base spell name
    val baseSpell = cast.spell.replace("\\s*\\(Rider\\)".toRegex(), "").trim()

    // Look up the pet skill without rider suffix
    val petSkill = petSkillWhitelist.find { skill ->
      skill.possibleNames.any { it.equals(baseSpell, ignoreCase = true) }
    }

    // Try to find the pet card by recent successful casts
    if (cast.cid != null) {
      val recentPets = PlayerCacheInteractor.getPetEntriesByName("")
        .filter { it.value.owner == cast.source && cast.cid in it.value.recentCids }
      if (recentPets.size == 1) {
        return recentPets.first().value.name
      }
    }

    // Fallback: return a generic name based on spell
    return when {
      baseSpell.contains("Scratch") -> "Mara"
      baseSpell.contains("Guided Missiles") -> "Siege Risopoda"
      else -> "Unknown Pet"
    }
  }

  private fun isRelatedSpell(damageSpellId: Int, damageSpellName: String, castSpellId: Int): Boolean {
    // Direct match
    if (damageSpellId == castSpellId) return true

    // Check if damage spell is a DoT related to the cast
    val castSkill = petSkillWhitelist.find { it.id == castSpellId }
    if (castSkill != null) {
      // For Scratch -> Bleeding
      if (castSkill.name.contains("Scratch", ignoreCase = true) &&
        damageSpellName.contains("Bleeding", ignoreCase = true)) {
        return true
      }
      // Add more correlations as needed
    }

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
