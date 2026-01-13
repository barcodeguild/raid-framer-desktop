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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

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
          val painter: Painter? =
            when (thumbnails[index]) {
              "Abyssal Exhaustion" -> painterResource("abyssal_exhaustion.png")
              "Aftereffect" -> painterResource("aftereffect.png")
              "All Spooked Out" -> painterResource("all_spooked_out.png")
              "Anchored: Move Speed 0" -> painterResource("anchored.png")
              "Arcadian Sea Keeper's Curse" -> painterResource("arcadian_sea_keepers_curse.png")
              "Assault Shock" -> painterResource("assault_shock.png")
              "Bane" -> painterResource("bane.png")
              "Banishment" -> painterResource("banishment.png")
              "Banshee Wail" -> painterResource("banshee_wail.png")
              "Blade Flurry" -> painterResource("blade_flurry.png")
              "Blast Echo" -> painterResource("blast_echo.png")
              "Bleeding (Rank 1)" -> painterResource("bleeding.png")
              "Bleeding (Rank 2)" -> painterResource("bleeding.png")
              "Bleeding (Rank 3)" -> painterResource("bleeding.png")
              "Bleeding (Rank 4)" -> painterResource("bleeding.png")
              "Bleeding (Rank 5)" -> painterResource("bleeding.png")
              "Bleeding" -> painterResource("bleeding.png")
              "Blight" -> painterResource("blight.png")
              "Blighted" -> painterResource("blight.png")
              "Blind" -> painterResource("blind.png")
              "Blinded By Crows" -> painterResource("blinded_by_crows.png")
              "Blinding Flash" -> painterResource("blinding_flash.png")
              "Bloodthirst Shock" -> painterResource("bloodthirst_shock.png")
              "Bloodwind Rank 1" -> painterResource("bloodwind.png")
              "Bloodwind Rank 2" -> painterResource("bloodwind.png")
              "Bloodwind Rank 3" -> painterResource("bloodwind.png")
              "Bloodwind Rank 4" -> painterResource("bloodwind.png")
              "Bloodwind Rank 5" -> painterResource("bloodwind.png")
              "Bloodwind Rank 6" -> painterResource("bloodwind.png")
              "Bloodwind Rank 7" -> painterResource("bloodwind.png")
              "Bloodwind Rank 8" -> painterResource("bloodwind.png")
              "Bubble Trap" -> painterResource("bubble_trap.png")
              "Burning Flesh" -> painterResource("burning_flesh.png")
              "Burning" -> painterResource("burning.png")
              "Cat Nap" -> painterResource("cat_nap.png")
              "Charging" -> painterResource("charging.png")
              "Charmed" -> painterResource("charmed.png")
              "Cleaver Target" -> painterResource("cleaver_target.png")
              "Cold Wave" -> painterResource("cold_wave.png")
              "Conflagration" -> painterResource("conflagration.png")
              "Corrosion" -> painterResource("corrosion.png")
              "Crippling Mire" -> painterResource("crippling_mire.png")
              "Crow Attack" -> painterResource("crow_attack.png")
              "Crows Search" -> painterResource("crows_search.png")
              "Cumulative Damage" -> painterResource("cumulative_damage.png")
              "Curse of Kraken" -> painterResource("curse_of_kraken.png")
              "Curse" -> painterResource("curse.png")
              "Cursed Flame" -> painterResource("cursed_flame.png")
              "Cursed Seeds" -> painterResource("cursed_seeds.png")
              "Cursed Thorns" -> painterResource("cursed_seeds.png")
              "Dagger" -> painterResource("dagger.png")
              "Dahuta's Curse" -> painterResource("dahutas_curse.png")
              "Dazed" -> painterResource("dazed.png")
              "Deadly Poison" -> painterResource("deadly_poison.png")
              "Deathmark Aura" -> painterResource("deathmark_aura.png")
              "Destroyer Cursed Spear" -> painterResource("destroyer_cursed_spear.png")
              "Disable Instrument" -> painterResource("disable_instrument.png")
              "Disable Left-Hand Weapon" -> painterResource("disable_left_weapon.png")
              "Disables Right-Hand weapon" -> painterResource("disable_right_weapon.png")
              "Discord" -> painterResource("discord.png")
              "Dissonance" -> painterResource("dissonance.png")
              "Distressed" -> painterResource("distressed.png")
              "Dive Trap" -> painterResource("dive_trap.png")
              "Dominator's Curse" -> painterResource("dominators.png")
              "Dragon Roar" -> painterResource("dragon_roar.png")
              "Dragonfire" -> painterResource("dragonfire.png")
              "Dried Up" -> painterResource("dried_up.png")
              "Ear-Splitter" -> painterResource("ear_splitter.png")
              "Earthen Grip" -> painterResource("earthen_grip.png")
              "Electric Shock" -> painterResource("electric_shock.png")
              "Enervated" -> painterResource("enervated.png")
              "Fall Stun" -> painterResource("fall_stun.png")
              "Falling" -> painterResource("falling.png")
              "Fatal Wound Immunity" -> painterResource("general_mechanic.png")
              "Fatal Wound" -> painterResource("fatal_wound.png")
              "Fear" -> painterResource("fear.png")
              "Felon" -> painterResource("felon.png")
              "Filthy Mucus" -> painterResource("filthy_mucus.png")
              "Flame Barrier" -> painterResource("flame_barrier.png")
              "Flame Hell Spear" -> painterResource("hellspear.png")
              "Flight Speed Boost Cooldown" -> painterResource("general_mechanic.png")
              "Focal Concussion" -> painterResource("focal_concussion.png")
              "Freeze" -> painterResource("freeze.png")
              "Freezing Arrow" -> painterResource("freezing_arrow.png")
              "Freezing" -> painterResource("freeze.png")
              "Frostbite" -> painterResource("frostbite.png")
              "Frozen" -> painterResource("frozen.png")
              "Frustration" -> painterResource("frustration.png")
              "Ghastly Aura" -> painterResource("ghastly_aura.png")
              "Gleeful Destruction" -> painterResource("gleeful_destruction.png")
              "Glider Disabled" -> painterResource("general_mechanic.png")
              "Greater Shock" -> painterResource("greater_shock.png")
              "Ground Shackle" -> painterResource("ground_shackle.png")
              "Hell Spear" -> painterResource("hellspear.png")
              "Heroic Grandeur" -> painterResource("heroic_grandeur.png")
              "Ice Shard" -> painterResource("ice_shard.png")
              "Impaled" -> painterResource("impaled.png")
              "Incinerate" -> painterResource("incinerate.png")
              "Instinct" -> painterResource("instinct.png")
              "Invincibility Limit" -> painterResource("invincibility_limit.png")
              "Invincible Flight Disabled" -> painterResource("invincibility_limit.png")
              "Jola's Grudge" -> painterResource("jolas_grudge.png")
              "Kitsu's Charm" -> painterResource("kitsus_charm.png")
              "Kitsu's Fear" -> painterResource("kitsus_fear.png")
              "Lassitude" -> painterResource("lassitude.png")
              "Lassoed" -> painterResource("lassoed.png")
              "Leech" -> painterResource("leech.png")
              "Lethargy (Bloody Chantey)" -> painterResource("lethargy.png")
              "Lightning Fervent Healing" -> painterResource("lightning_fervent_healing.png")
              "Lucius: Gods and Heroes" -> painterResource("lucius_gods_and_heroes.png")
              "Machine in 5mins" -> painterResource("machine_in_5mins.png")
              "Magical Bleeding" -> painterResource("magical_bleeding.png")
              "Malicious Binding" -> painterResource("malicious_binding.png")
              "Mana Chain" -> painterResource("mana_chain.png")
              "Mana Poison" -> painterResource("mana_poison.png")
              "Mark (Rank 1)" -> painterResource("stalkers_mark.png")
              "Mark (Rank 2)" -> painterResource("stalkers_mark.png")
              "Mark (Rank 3)" -> painterResource("stalkers_mark.png")
              "Mark (Rank 4)" -> painterResource("stalkers_mark.png")
              "Mark" -> painterResource("mark.png")
              "Meteor Impact" -> painterResource("meteor_impact.png")
              "Mirror's View" -> painterResource("mirrors_view.png") // admins maybe?
              "Mist Wraith's Curse" -> painterResource("mist_wraiths_curse.png")
              "Morpheus's Mark" -> painterResource("morpheus_mark.png")
              "Mucus" -> painterResource("mucus.png")
              "Napping" -> painterResource("napping.png")
              "Nightmare Grinder" -> painterResource("nightmare_grinder.png")
              "Obscure Vision" -> painterResource("obscure_vision.png")
              "Oh no, oh no, oh no..." -> painterResource("oh_no_oh_no_oh_no.png")
              "Over Healing" -> painterResource("over_healing.png")
              "Overpowered" -> painterResource("overpowered.png")
              "Overpowering Aura" -> painterResource("overpowering_aura.png")
              "Paralyzing Poison" -> painterResource("paralyzing_poison.png")
              "Petrification" -> painterResource("petrification.png")
              "Phantasm's Wail" -> painterResource("phantasms_wail.png")
              "Pirate's Toxic Shot" -> painterResource("pirates_toxic_shot.png")
              "Poisoned" -> painterResource("poisoned.png")
              "Poisoning" -> painterResource("poisoned.png")
              "Powerful Shock" -> painterResource("powerful_shock.png")
              "Preparing Glider" -> painterResource("general_mechanic.png")
              "Preparing next Trick" -> painterResource("preparing_next_trick.png")
              "Provoked" -> painterResource("provoked.png")
              "Puncture" -> painterResource("puncture.png")
              "Reduces Received Healing" -> painterResource("reduces_received_healing.png")
              "Root" -> painterResource("rooted.png")
              "Rotten Venom" -> painterResource("rotten_venom.png")
              "Rough Sea Winds" -> painterResource("rough_sea_winds.png")
              "Scratch" -> painterResource("scratch.png")
              "Shackle" -> painterResource("shackled.png")
              "Shackled" -> painterResource("shackled.png")
              "Shaken (Rank 1)" -> painterResource("shaken.png")
              "Shaken (Rank 2)" -> painterResource("shaken.png")
              "Shaken (Rank 3)" -> painterResource("shaken.png")
              "Shaken (Rank 4)" -> painterResource("shaken.png")
              "Shaken (Rank 5)" -> painterResource("shaken.png")
              "Shaken (Rank 6)" -> painterResource("shaken.png")
              "Shaken (Rank 7)" -> painterResource("shaken.png")
              "Shaken (Rank 8)" -> painterResource("shaken.png")
              "Shaken" -> painterResource("shaken.png")
              "Shattering Curse (Rank 6)" -> painterResource("shattering_curse.png")
              "Shattering Curse (Rank 7)" -> painterResource("shattering_curse.png")
              "Shock" -> painterResource("shock.png")
              "Shockwave" -> painterResource("shockwave.png")
              "Silence" -> painterResource("silenced.png")
              "Skewer" -> painterResource("skewer.png")
              "Sleep" -> painterResource("sleep.png")
              "Slow" -> painterResource("slow.png")
              "Snare" -> painterResource("snare.png")
              "Snipe Target" -> painterResource("snipe_target.png")
              "Sonic Wave" -> painterResource("sonic_wave.png")
              "Soulbound Edge" -> painterResource("soulbound_edge.png")
              "Stagger" -> painterResource("stagger.png")
              "Stepping on a Landmine" -> painterResource("landmine.png")
              "Stone Corrosion" -> painterResource("stone_corrosion.png")
              "Stun" -> painterResource("stunned.png")
              "Stunned" -> painterResource("stunned.png")
              "Taunt" -> painterResource("taunt.png")
              "Taunted" -> painterResource("taunted.png")
              "Tired Snowflake" -> painterResource("tired_snowflake.png")
              "Totem Gaze" -> painterResource("totem_gaze.png")
              "Tremor" -> painterResource("tremor.png")
              "Trip Impact" -> painterResource("trip_impact.png")
              "Tripped" -> painterResource("tripped.png")
              "Turbulence" -> painterResource("turbulence.png")
              "Twilight Stealth Cooldown" -> painterResource("general_mechanic.png")
              "Twin Shadow Slash" -> painterResource("twin_shadow_slash.png")
              "Twisted Contract Activated" -> painterResource("twisted_contract_activated.png")
              "Twisted Flesh" -> painterResource("twisted_flesh.png")
              "Unable to use Potions" -> painterResource("unable_to_use_potions.png")
              "Unguarded (Bulwark Ballad)" -> painterResource("unguarded.png")
              "Unguarded" -> painterResource("unguarded.png")
              "Unpleasant Sensation (Quickstep)" -> painterResource("unpleasant_sensation.png")
              "Untargetable Cooldown" -> painterResource("general_mechanic.png")
              "Weak (Rank 1)" -> painterResource("weak.png")
              "Weak (Rank 2)" -> painterResource("weak.png")
              "Weak (Rank 3)" -> painterResource("weak.png")
              "Weak (Rank 4)" -> painterResource("weak.png")
              "Weak (Rank 5)" -> painterResource("weak.png")
              "Weak (Rank 6)" -> painterResource("weak.png")
              "Weakened Energy" -> painterResource("weakened_energy.png")
              "Withdraw!" -> painterResource("withdraw.png")
              "Wraith's Curse" -> painterResource("wraiths_curse.png")
              else -> {
                println("No debuff icon for ${thumbnails[index]}.")
                painterResource("not_found.png")
                null
              }
            }
          if (painter != null) {
            Image(
              painter = painter,
              contentDescription = thumbnails[index],
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
 * We use this for the skill-tree icons in the player ranking rows. Will crash if the resource is not found. Try..catch not supported around composable function invocations.
 */
@Composable
fun renderTinySkillTreeIconFor(treeName: String): Painter {
  // Converts "Malediction" -> "malediction_tiny.png"
  val filename = "svg\\${treeName.lowercase().replace(" ", "_")}.svg"
  return painterResource(filename)
}

/*
 * Function to get pet thumbnails based on pet type.
 */
@Composable
fun getPetIcon(petType: String): Painter {
  val filename = when (petType) {
    "red_dragon" -> "pets/red_dragon.png"
    "green_dragon" -> "pets/green_dragon.png"
    "black_dragon" -> "pets/black_dragon.png"
    "riso" -> "pets/riso.png"
    "Ellam" -> "pets/ellam.png"
    "Violet Bloomfang" -> "pets/violet_bloomfang.png"
    "Black Bloomfang" -> "pets/black_bloomfang.png"
    "Igneer" -> "pets/igneer.png"
    "Iceneer" -> "pets/iceneer.png"
    "Mooneer" -> "pets/mooneer.png"
    "Ser Meatball" -> "pets/ser_meatball.png"
    "Stormwraith Kirin" -> "pets/stormwraith_kirin.png"
    "Gloomwraith Kirin" -> "pets/gloomwraith_kirin.png"
    "Hellwraith Kirin" -> "pets/hellwraith_kirin.png"
    "Moonlight Kitsu" -> "pets/moonlight_kitsu.png"
    "Celestial Kitsu" -> "pets/celestial_kitsu.png"
    "Siegeram Taurus" -> "pets/siegeram_taurus.png"
    "Typhoon Drake" -> "pets/typhoon_drake.png"
    "Deathmaw" -> "pets/deathmaw.png"
    "Maahes" -> "pets/maahes.png"
    else -> "unknown_tinyy.png"
  }
  return painterResource(filename)
}
