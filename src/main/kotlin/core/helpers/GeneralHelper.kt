package core.helpers
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import java.awt.Toolkit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.realm.kotlin.Realm
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Extension function to abbreviate a numeric values by thousands, millions, billions, etc.
 */
fun Long.humanReadableAbbreviation(): String {
  val value = this.toDouble()
  val suffixes = arrayOf("", "k", "M", "B", "T")
  val suffixNum = when {
    value < 1_000_000 -> 1
    value < 1_000_000_000 -> 2
    else -> 3
  }
  val divisor = Math.pow(10.0, suffixNum * 3.toDouble())
  var shortValue = value / divisor
  return String.format("%.1f", shortValue) + suffixes[suffixNum]
}

/*
 * Finds the width and height of the screen in dp. So we can calculate where to put the overlay windows and not
 * on completely over or undershoot on friend's tiny displays.
 */
fun getScreenSizeInDp(): Pair<Dp, Dp> {
  val screenSize = Toolkit.getDefaultToolkit().screenSize
  val density = Toolkit.getDefaultToolkit().screenResolution / 160.0f
  val widthInDp = (screenSize.width / density).dp
  val heightInDp = (screenSize.height / density).dp
  return Pair(widthInDp, heightInDp)
}

fun Long.toLocalTimeString(): String {
  val instant = Instant.ofEpochMilli(this)
  val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
  return localDateTime.format(DateTimeFormatter.ofPattern("hh:mm:ss"))
}

/*
 * Makes a pretty-looking AnnotatedString for the attack event. I brought this code into a different file because
 * it's too and will get used multiple times potentially.
 */
@Composable
fun annotatedStringForAttack(event: CombatInteractor.AttackEvent): AnnotatedString {
  return buildAnnotatedString {
    withStyle(style = SpanStyle(color = Color(249, 191, 59))) {
      append(event.caster)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" attacked ")
    }
    withStyle(style = SpanStyle(color = Color(249, 191, 59))) {
      append(event.target)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" with ")
    }
    withStyle(style = SpanStyle(color = Color.Red)) {
      append(event.spell)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" to deal ")
    }
    withStyle(style = SpanStyle(color = Color(249, 191, 59))) {
      append("${event.damage}")
    }
    withStyle(style = if (event.critical) SpanStyle(color = Color.Red) else SpanStyle(color = Color.White)) {
      append(if (event.critical) " critical damage!" else " damage.")
    }
    withStyle(style = SpanStyle(color = Color.Magenta)) {
      append("(${event.timestamp.toLocalTimeString()})")
    }
  }
}

/*
 * Makes a pretty-looking AnnotatedString for the heal events.
 */
@Composable
fun annotatedStringForHeal(event: CombatInteractor.HealEvent): AnnotatedString {
  return buildAnnotatedString {
    withStyle(style = SpanStyle(color = Color(249, 191, 59))) {
      append(event.target)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" was healed by ")
    }
    withStyle(style = SpanStyle(color = Color(249, 191, 59))) {
      append(event.caster)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" using ")
    }
    withStyle(style = SpanStyle(color = Color.Green)) {
      append(event.spell)
    }
    withStyle(style = SpanStyle(color = Color.White)) {
      append(" to restore ")
    }
    withStyle(style = SpanStyle(color = Color.Green)) {
      append("${event.amount}")
    }
    withStyle(style = if (event.critical) SpanStyle(color = Color.Green) else SpanStyle(color = Color.White)) {
      append(if (event.critical) " (critical!) " else " ")
    }
    withStyle(style = SpanStyle(color = Color.Magenta)) {
      append("(${event.timestamp.toLocalTimeString()})")
    }
  }
}

@Composable
fun renderDebuffThumbnailGrid(thumbnails: List<String>) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 38.dp),
    contentPadding = PaddingValues(1.dp),
    content = {
      items(thumbnails.size) { index ->
        val painter: Painter? = when(thumbnails[index]) {
          "Tripped" -> painterResource("tripped.png")
          "Frozen" -> painterResource("frozen.png")
          "Freeze" -> painterResource("freeze.png")
          "Burning" -> painterResource("burning.png")
          "Charmed" -> painterResource("charmed.png")
          "Bleeding" -> painterResource("bleeding.png")
          "Bleeding (Rank 1)" -> painterResource("bleeding.png")
          "Bleeding (Rank 2)" -> painterResource("bleeding.png")
          "Bleeding (Rank 3)" -> painterResource("bleeding.png")
          "Bleeding (Rank 4)" -> painterResource("bleeding.png")
          "Bleeding (Rank 5)" -> painterResource("bleeding.png")
          "Dissonance" -> painterResource("dissonance.png")
          "Enervated" -> painterResource("enervated.png")
          "Root" -> painterResource("rooted.png")
          "Stun" -> painterResource("stunned.png")
          "Stunned" -> painterResource("stunned.png")
          "Shackle" -> painterResource("shackled.png")
          "Silence" -> painterResource("silenced.png")
          "Lethargy (Bloody Chantey)" -> painterResource("lethargy.png")
          "Taunt" -> painterResource("taunt.png")
          "Distressed" -> painterResource("distressed.png")
          "Snare" -> painterResource("snare.png")
          "Dominator's Curse" -> painterResource("dominators.png")
          "Shaken" -> painterResource("shaken.png")
          "Shaken (Rank 1)" -> painterResource("shaken.png")
          "Shaken (Rank 2)" -> painterResource("shaken.png")
          "Shaken (Rank 3)" -> painterResource("shaken.png")
          "Shaken (Rank 4)" -> painterResource("shaken.png")
          "Shaken (Rank 5)" -> painterResource("shaken.png")
          "Shaken (Rank 6)" -> painterResource("shaken.png")
          "Shaken (Rank 7)" -> painterResource("shaken.png")
          "Shaken (Rank 8)" -> painterResource("shaken.png")
          "Provoked" -> painterResource("provoked.png")
          "Bubble Trap" -> painterResource("bubble_trap.png")
          "Falling" -> painterResource("falling.png")
          "Discord" -> painterResource("discord.png")
          "Unguarded (Bulwark Ballad)" -> painterResource("unguarded.png")
          "Unguarded" -> painterResource("unguarded.png")
          "Banshee Wail" -> painterResource("banshee_wail.png")
          "Overpowered" -> painterResource("overpowered.png")
          "Puncture" -> painterResource("puncture.png")
          "Slow" -> painterResource("slow.png")
          "Weak (Rank 1)" -> painterResource("weak.png")
          "Weak (Rank 2)" -> painterResource("weak.png")
          "Weak (Rank 3)" -> painterResource("weak.png")
          "Weak (Rank 4)" -> painterResource("weak.png")
          "Weak (Rank 5)" -> painterResource("weak.png")
          "Weak (Rank 6)" -> painterResource("weak.png")
          "Blinded By Crows" -> painterResource("blinded_by_crows.png")
          "Crow Attack" -> painterResource("crow_attack.png")
          "Cursed Seeds" -> painterResource("cursed_seeds.png")
          "Cursed Thorns" -> painterResource("cursed_seeds.png")
          "Reduces Received Healing" -> painterResource("reduces_received_healing.png")
          "Flame Hell Spear" -> painterResource("hellspear.png")
          "Crippling Mire" -> painterResource("crippling_mire.png")
          "Gleeful Destruction" -> painterResource("gleeful_destruction.png")
          "Stagger" -> painterResource("stagger.png")
          "Skewer" -> painterResource("skewer.png")
          "Deathmark Aura" -> painterResource("deathmark_aura.png")
          "Poisoned" -> painterResource("poisoned.png")
          "Poisoning" -> painterResource("poisoned.png")
          "Flight Speed Boost Cooldown" -> painterResource("general_mechanic.png")
          "Preparing Glider" -> painterResource("general_mechanic.png")
          "Blighted" -> painterResource("blight.png")
          "Stone Corrosion" -> painterResource("stone_corrosion.png")
          "Cat Nap" -> painterResource("cat_nap.png")
          "Invincibility Limit" -> painterResource("invincibility_limit.png")
          "Invincible Flight Disabled" -> painterResource("invincibility_limit.png")
          "Ground Shackle" -> painterResource("ground_shackle.png")
          "Wraith's Curse" -> painterResource("wraiths_curse.png")
          "Earthen Grip" -> painterResource("earthen_grip.png")
          "Shockwave" -> painterResource("shockwave.png")
          "Dazed" -> painterResource("dazed.png")
          "Electric Shock" -> painterResource("electric_shock.png")
          "Mist Wraith's Curse" -> painterResource("mist_wraiths_curse.png")
          "Ice Shard" -> painterResource("ice_shard.png")
          "Instinct" -> painterResource("instinct.png")
          "Twin Shadow Slash" -> painterResource("twin_shadow_slash.png")
          "Blade Flurry" -> painterResource("blade_flurry.png")
          "Bloodwind Rank 1" -> painterResource("bloodwind.png")
          "Bloodwind Rank 2" -> painterResource("bloodwind.png")
          "Bloodwind Rank 3" -> painterResource("bloodwind.png")
          "Bloodwind Rank 4" -> painterResource("bloodwind.png")
          "Bloodwind Rank 5" -> painterResource("bloodwind.png")
          "Bloodwind Rank 6" -> painterResource("bloodwind.png")
          "Bloodwind Rank 7" -> painterResource("bloodwind.png")
          "Bloodwind Rank 8" -> painterResource("bloodwind.png")
          "Mark" -> painterResource("mark.png")
          "Lightning Fervent Healing" -> painterResource("lightning_fervent_healing.png")
          "Obscure Vision" -> painterResource("obscure_vision.png")
          "Disable Left-Hand Weapon" -> painterResource("disable_left_weapon.png")
          "Unpleasant Sensation (Quickstep)" -> painterResource("unpleasant_sensation.png")

          else -> {
            println("No debuff icon for ${thumbnails[index]}.")
            null
          }
        }
        if (painter != null) {
          Image(
            painter = painter,
            contentDescription = thumbnails[index],
            modifier = Modifier
              .padding(1.dp)
              .size(32.dp)
              .border(1.dp, Color.Red) // Add this line
          )
        }
      }
    }
  )
}

/*
 * Why this isn't built into the kotlin version of realm the world may never know.
 */
inline fun <T> Realm.use(block: (Realm) -> T): T {
  return try {
    block(this)
  } finally {
    this.close()
  }
}
