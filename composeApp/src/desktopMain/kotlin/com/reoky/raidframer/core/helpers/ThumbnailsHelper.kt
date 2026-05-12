package com.reoky.raidframer.core.helpers

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.definitions.SkillTreeType
// New Resource Imports
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import raid_framer_desktop.composeapp.generated.resources.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun renderDebuffThumbnailGrid(thumbnails: List<String>) {
  var showDebuffTooltip by remember { mutableStateOf(false) }
  var currentHoveredIndex by remember { mutableStateOf(-1) }

  Box {
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 38.dp),
      contentPadding = PaddingValues(1.dp),
      content = {
        items(thumbnails.size) { index ->
          val debuffName = thumbnails[index]
          // Safe lookup: returns a compile-safe DrawableResource or null
          val resource = getDebuffResource(debuffName)

          if (resource != null) {
            Image(
              painter = painterResource(resource),
              contentDescription = debuffName,
              modifier = Modifier
                .padding(2.dp)
                .size(36.dp)
                .border(1.dp, Color.Red)
                .onPointerEvent(PointerEventType.Enter) {
                  showDebuffTooltip = true
                  currentHoveredIndex = index
                }
                .onPointerEvent(PointerEventType.Exit) {
                  showDebuffTooltip = false
                }
            )
          } else {
            // Fallback for missing mapping
            // If you have a 'not_found' png, use it here, otherwise we just don't render anything
            // avoiding the crash.
            // println("No resource mapping found for: $debuffName")
            Image(
              painter = painterResource(Res.drawable.not_found),
              contentDescription = "Unknown: $debuffName",
              modifier = Modifier.padding(2.dp).size(36.dp).border(1.dp, Color.Gray)
            )
          }
        }
      }
    )

    if (showDebuffTooltip) {
      Surface(
        shape = RoundedCornerShape(4.dp),
        elevation = 8.dp,
        color = Color.White
      ) {
        Text(
          text = thumbnails.getOrNull(currentHoveredIndex) ?: "Lost to Ravines of Time",
          modifier = Modifier.padding(8.dp),
          color = Color.Black
        )
      }
    }
  }
}

/*
 * We use this for the skill-tree icons.
 * Migrated to compile-time safe lookup via SkillTreeType
 */
@Composable
fun skillTreeIconPainterFor(tree: SkillTreeType?): Painter {
  val resource = when (tree) {
    SkillTreeType.ARCHERY -> Res.drawable.archery
    SkillTreeType.AURAMANCY -> Res.drawable.auramancy
    SkillTreeType.BATTLERAGE -> Res.drawable.battlerage
    SkillTreeType.DEFENSE -> Res.drawable.defense
    SkillTreeType.OCCULTISM -> Res.drawable.occultism
    SkillTreeType.SHADOWPLAY -> Res.drawable.shadowplay
    SkillTreeType.SONGCRAFT -> Res.drawable.songcraft
    SkillTreeType.SORCERY -> Res.drawable.sorcery
    SkillTreeType.VITALISM -> Res.drawable.vitalism
    SkillTreeType.WITCHCRAFT -> Res.drawable.witchcraft
    SkillTreeType.MALEDICTION -> Res.drawable.malediction
    SkillTreeType.SWIFTBLADE -> Res.drawable.swiftblade
    SkillTreeType.GUNSLINGER -> Res.drawable.gunslinger
    SkillTreeType.SPELLDANCE -> Res.drawable.spelldance
    else -> Res.drawable.unknown
  }
  return painterResource(resource)
}

/*
 * Function to get pet thumbnails based on pet type.
 */
@Composable
fun getPetIcon(petType: String): Painter {
  val resource = when (petType) {
    "red_dragon" -> Res.drawable.red_dragon
    "green_dragon" -> Res.drawable.green_dragon
    "black_dragon" -> Res.drawable.black_dragon
    "riso" -> Res.drawable.riso
    "Ellam" -> Res.drawable.ellam
    "Violet Bloomfang" -> Res.drawable.violet_bloomfang
    "Black Bloomfang" -> Res.drawable.black_bloomfang
    "Igneer" -> Res.drawable.igneer
    "Iceneer" -> Res.drawable.iceneer
    "Mooneer" -> Res.drawable.mooneer
    "Ser Meatball" -> Res.drawable.ser_meatball
    "Stormwraith Kirin" -> Res.drawable.stormwraith_kirin
    "Gloomwraith Kirin" -> Res.drawable.gloomwraith_kirin
    "Hellwraith Kirin" -> Res.drawable.hellwraith_kirin
    "Moonlight Kitsu" -> Res.drawable.moonlight_kitsu
    "Celestial Kitsu" -> Res.drawable.celestial_kitsu
    "Siegeram Taurus" -> Res.drawable.siegeram_taurus
    "Typhoon Drake" -> Res.drawable.typhoon_drake
    "Deathmaw" -> Res.drawable.deathmaw
    "Maahes" -> Res.drawable.maahes
    else -> Res.drawable.unknown
  }
  return painterResource(resource)
}

/**
 * Maps game string names to Generated Compose Resources.
 * This replaces the previous runtime string path construction.
 */
private fun getDebuffResource(name: String): DrawableResource? {
  return when (name) {
    "Abyssal Exhaustion" -> Res.drawable.abyssal_exhaustion
    "Aftereffect" -> Res.drawable.aftereffect
    "All Spooked Out" -> Res.drawable.all_spooked_out
    "Anchored: Move Speed 0" -> Res.drawable.anchored
    "Arcadian Sea Keeper's Curse" -> Res.drawable.arcadian_sea_keepers_curse
    "Assault Shock" -> Res.drawable.assault_shock
    "Bane" -> Res.drawable.bane
    "Banishment" -> Res.drawable.banishment
    "Banshee Wail" -> Res.drawable.banshee_wail
    "Blade Flurry" -> Res.drawable.blade_flurry
    "Blast Echo" -> Res.drawable.blast_echo
    "Bleeding" -> Res.drawable.bleeding
    "Bleeding (Rank 1)" -> Res.drawable.bleeding
    "Bleeding (Rank 2)" -> Res.drawable.bleeding
    "Bleeding (Rank 3)" -> Res.drawable.bleeding
    "Bleeding (Rank 4)" -> Res.drawable.bleeding
    "Bleeding (Rank 5)" -> Res.drawable.bleeding
    "Blight" -> Res.drawable.blight
    "Blighted" -> Res.drawable.blight
    "Blind" -> Res.drawable.blind
    "Blinded By Crows" -> Res.drawable.blinded_by_crows
    "Blinding Flash" -> Res.drawable.blinding_flash
    "Bloodthirst Shock" -> Res.drawable.bloodthirst_shock
    "Bloodwind Rank 1" -> Res.drawable.bloodwind
    "Bloodwind Rank 2" -> Res.drawable.bloodwind
    "Bloodwind Rank 3" -> Res.drawable.bloodwind
    "Bloodwind Rank 4" -> Res.drawable.bloodwind
    "Bloodwind Rank 5" -> Res.drawable.bloodwind
    "Bloodwind Rank 6" -> Res.drawable.bloodwind
    "Bloodwind Rank 7" -> Res.drawable.bloodwind
    "Bloodwind Rank 8" -> Res.drawable.bloodwind
    "Bubble Trap" -> Res.drawable.bubble_trap
    "Burning Flesh" -> Res.drawable.burning_flesh
    "Burning" -> Res.drawable.burning
    "Cat Nap" -> Res.drawable.cat_nap
    "Charging" -> Res.drawable.charging
    "Charmed" -> Res.drawable.charmed
    "Cleaver Target" -> Res.drawable.cleaver_target
    "Cold Wave" -> Res.drawable.cold_wave
    "Conflagration" -> Res.drawable.conflagration
    "Corrosion" -> Res.drawable.corrosion
    "Crippling Mire" -> Res.drawable.crippling_mire
    "Crow Attack" -> Res.drawable.crow_attack
    "Crows Search" -> Res.drawable.crows_search
    "Cumulative Damage" -> Res.drawable.cumulative_damage
    "Curse of Kraken" -> Res.drawable.curse_of_kraken
    "Curse" -> Res.drawable.curse
    "Cursed Flame" -> Res.drawable.cursed_flame
    "Cursed Seeds" -> Res.drawable.cursed_seeds
    "Cursed Thorns" -> Res.drawable.cursed_seeds
    "Dagger" -> Res.drawable.dagger
    "Dahuta's Curse" -> Res.drawable.dahutas_curse
    "Dazed" -> Res.drawable.dazed
    "Deadly Poison" -> Res.drawable.deadly_poison
    "Deathmark Aura" -> Res.drawable.deathmark_aura
    "Destroyer Cursed Spear" -> Res.drawable.destroyer_cursed_spear
    "Disable Instrument" -> Res.drawable.disable_instrument
    "Disable Left-Hand Weapon" -> Res.drawable.disable_left_weapon
    "Disables Right-Hand weapon" -> Res.drawable.disable_right_weapon
    "Discord" -> Res.drawable.discord
    "Dissonance" -> Res.drawable.dissonance
    "Distressed" -> Res.drawable.distressed
    "Dive Trap" -> Res.drawable.dive_trap
    "Dominator's Curse" -> Res.drawable.dominators
    "Dragon Roar" -> Res.drawable.dragon_roar
    "Dragonfire" -> Res.drawable.dragonfire
    "Dried Up" -> Res.drawable.dried_up
    "Ear-Splitter" -> Res.drawable.ear_splitter
    "Earthen Grip" -> Res.drawable.earthen_grip
    "Electric Shock" -> Res.drawable.electric_shock
    "Enervated" -> Res.drawable.enervated
    "Fall Stun" -> Res.drawable.fall_stun
    "Falling" -> Res.drawable.falling
    "Fatal Wound Immunity" -> Res.drawable.general_mechanic
    "Fatal Wound" -> Res.drawable.fatal_wound
    "Fear" -> Res.drawable.fear
    "Felon" -> Res.drawable.felon
    "Filthy Mucus" -> Res.drawable.filthy_mucus
    "Flame Barrier" -> Res.drawable.flame_barrier
    "Flame Hell Spear" -> Res.drawable.hellspear
    "Flight Speed Boost Cooldown" -> Res.drawable.general_mechanic
    "Focal Concussion" -> Res.drawable.focal_concussion
    "Freeze" -> Res.drawable.freeze
    "Freezing Arrow" -> Res.drawable.freezing_arrow
    "Freezing" -> Res.drawable.freeze
    "Frostbite" -> Res.drawable.frostbite
    "Frozen" -> Res.drawable.frozen
    "Frustration" -> Res.drawable.frustration
    "Ghastly Aura" -> Res.drawable.ghastly_aura
    "Gleeful Destruction" -> Res.drawable.gleeful_destruction
    "Glider Disabled" -> Res.drawable.general_mechanic
    "Greater Shock" -> Res.drawable.greater_shock
    "Ground Shackle" -> Res.drawable.ground_shackle
    "Hell Spear" -> Res.drawable.hellspear
    "Heroic Grandeur" -> Res.drawable.heroic_grandeur
    "Ice Shard" -> Res.drawable.ice_shard
    "Impaled" -> Res.drawable.impaled
    "Incinerate" -> Res.drawable.incinerate
    "Instinct" -> Res.drawable.instinct
    "Invincibility Limit" -> Res.drawable.invincibility_limit
    "Invincible Flight Disabled" -> Res.drawable.invincibility_limit
    "Jola's Grudge" -> Res.drawable.jolas_grudge
    "Kitsu's Charm" -> Res.drawable.kitsus_charm
    "Kitsu's Fear" -> Res.drawable.kitsus_fear
    "Lassitude" -> Res.drawable.lassitude
    "Lassoed" -> Res.drawable.lassoed
    "Leech" -> Res.drawable.leech
    "Lethargy (Bloody Chantey)" -> Res.drawable.lethargy
    "Lightning Fervent Healing" -> Res.drawable.lightning_fervent_healing
    "Lucius: Gods and Heroes" -> Res.drawable.lucius_gods_and_heroes
    "Machine in 5mins" -> Res.drawable.machine_in_5mins
    "Magical Bleeding" -> Res.drawable.magical_bleeding
    "Malicious Binding" -> Res.drawable.malicious_binding
    "Mana Chain" -> Res.drawable.mana_chain
    "Mana Poison" -> Res.drawable.mana_poison
    "Mark" -> Res.drawable.mark
    "Mark (Rank 1)" -> Res.drawable.stalkers_mark
    "Mark (Rank 2)" -> Res.drawable.stalkers_mark
    "Mark (Rank 3)" -> Res.drawable.stalkers_mark
    "Mark (Rank 4)" -> Res.drawable.stalkers_mark
    "Meteor Impact" -> Res.drawable.meteor_impact
    "Mirror's View" -> Res.drawable.mirrors_view
    "Mist Wraith's Curse" -> Res.drawable.mist_wraiths_curse
    "Morpheus's Mark" -> Res.drawable.morpheus_mark
    "Mucus" -> Res.drawable.mucus
    "Napping" -> Res.drawable.napping
    "Nightmare Grinder" -> Res.drawable.nightmare_grinder
    "Obscure Vision" -> Res.drawable.obscure_vision
    "Oh no, oh no, oh no..." -> Res.drawable.oh_no_oh_no_oh_no
    "Over Healing" -> Res.drawable.over_healing
    "Overpowered" -> Res.drawable.overpowered
    "Overpowering Aura" -> Res.drawable.overpowering_aura
    "Paralyzing Poison" -> Res.drawable.paralyzing_poison
    "Petrification" -> Res.drawable.petrification
    "Phantasm's Wail" -> Res.drawable.phantasms_wail
    "Pirate's Toxic Shot" -> Res.drawable.pirates_toxic_shot
    "Poisoned" -> Res.drawable.poisoned
    "Poisoning" -> Res.drawable.poisoned
    "Powerful Shock" -> Res.drawable.powerful_shock
    "Preparing Glider" -> Res.drawable.general_mechanic
    "Preparing next Trick" -> Res.drawable.preparing_next_trick
    "Provoked" -> Res.drawable.provoked
    "Puncture" -> Res.drawable.puncture
    "Reduces Received Healing" -> Res.drawable.reduces_received_healing
    "Root" -> Res.drawable.rooted
    "Rotten Venom" -> Res.drawable.rotten_venom
    "Rough Sea Winds" -> Res.drawable.rough_sea_winds
    "Scratch" -> Res.drawable.scratch
    "Shackle" -> Res.drawable.shackled
    "Shackled" -> Res.drawable.shackled
    "Shaken" -> Res.drawable.shaken
    "Shaken (Rank 1)" -> Res.drawable.shaken
    "Shaken (Rank 2)" -> Res.drawable.shaken
    "Shaken (Rank 3)" -> Res.drawable.shaken
    "Shaken (Rank 4)" -> Res.drawable.shaken
    "Shaken (Rank 5)" -> Res.drawable.shaken
    "Shaken (Rank 6)" -> Res.drawable.shaken
    "Shaken (Rank 7)" -> Res.drawable.shaken
    "Shaken (Rank 8)" -> Res.drawable.shaken
    "Shattering Curse (Rank 6)" -> Res.drawable.shattering_curse
    "Shattering Curse (Rank 7)" -> Res.drawable.shattering_curse
    "Shock" -> Res.drawable.shock
    "Shockwave" -> Res.drawable.shockwave
    "Silence" -> Res.drawable.silenced
    "Skewer" -> Res.drawable.skewer
    "Sleep" -> Res.drawable.sleep
    "Slow" -> Res.drawable.slow
    "Snare" -> Res.drawable.snare
    "Snipe Target" -> Res.drawable.snipe_target
    "Sonic Wave" -> Res.drawable.sonic_wave
    "Soulbound Edge" -> Res.drawable.soulbound_edge
    "Stagger" -> Res.drawable.stagger
    "Stepping on a Landmine" -> Res.drawable.landmine
    "Stone Corrosion" -> Res.drawable.stone_corrosion
    "Stun" -> Res.drawable.stunned
    "Stunned" -> Res.drawable.stunned
    "Taunt" -> Res.drawable.taunt
    "Taunted" -> Res.drawable.taunted
    "Tired Snowflake" -> Res.drawable.tired_snowflake
    "Totem Gaze" -> Res.drawable.totem_gaze
    "Tremor" -> Res.drawable.tremor
    "Trip Impact" -> Res.drawable.trip_impact
    "Tripped" -> Res.drawable.tripped
    "Turbulence" -> Res.drawable.turbulence
    "Twilight Stealth Cooldown" -> Res.drawable.general_mechanic
    "Twin Shadow Slash" -> Res.drawable.twin_shadow_slash
    "Twisted Contract Activated" -> Res.drawable.twisted_contract_activated
    "Twisted Flesh" -> Res.drawable.twisted_flesh
    "Unable to use Potions" -> Res.drawable.unable_to_use_potions
    "Unguarded (Bulwark Ballad)" -> Res.drawable.unguarded
    "Unguarded" -> Res.drawable.unguarded
    "Unpleasant Sensation (Quickstep)" -> Res.drawable.unpleasant_sensation
    "Untargetable Cooldown" -> Res.drawable.general_mechanic
    "Weak (Rank 1)" -> Res.drawable.weak
    "Weak (Rank 2)" -> Res.drawable.weak
    "Weak (Rank 3)" -> Res.drawable.weak
    "Weak (Rank 4)" -> Res.drawable.weak
    "Weak (Rank 5)" -> Res.drawable.weak
    "Weak (Rank 6)" -> Res.drawable.weak
    "Weakened Energy" -> Res.drawable.weakened_energy
    "Withdraw!" -> Res.drawable.withdraw
    "Wraith's Curse" -> Res.drawable.wraiths_curse
    else -> null
  }
}
