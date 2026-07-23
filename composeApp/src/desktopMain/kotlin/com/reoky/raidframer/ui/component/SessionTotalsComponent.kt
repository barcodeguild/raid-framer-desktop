package com.reoky.raidframer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.database.PlayerSessionTotalsEntity
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.PlayerCard
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_buffs
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_cc
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_charms
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_damage
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_debuffs
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_distress
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_glider
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_healing
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_items
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_kills_killing_blow
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_kills_most_damage
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_potions
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_silence
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_total_damage_taken
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_total_heals_received
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_tiger_strikes

// View-model-agnostic shape so the totals card can render either the in-memory
// session or an aggregated historical one without branching on the source.
data class SessionTotals(
  val damage: Long,
  val healing: Long,
  val cc: Int,
  val buffs: Int,
  val debuffs: Int,
  val charms: Int,
  val distress: Int,
  val silence: Int,
  val glider: Int,
  val items: Int,
  val potions: Int,
  val kills: Int,
  val killsKB: Int,
  val damageTaken: Int,
  val healsReceived: Int,
  val tigerStrikes: Int
) {
  companion object {
    fun fromPlayerCard(card: PlayerCard) = SessionTotals(
      damage = card.sessionDamageTotal,
      healing = card.sessionHealTotal,
      cc = card.sessionCCTotal,
      buffs = card.sessionBuffTotal,
      debuffs = card.sessionDebuffTotal,
      charms = card.sessionCharmTotal,
      distress = card.sessionDistressTotal,
      silence = card.sessionSilenceTotal,
      glider = card.sessionGliderTotal,
      items = card.sessionItemSkillTotal,
      potions = card.sessionPotionTotal,
      kills = card.sessionKillTotal,
      killsKB = card.sessionKillTotalKB,
      damageTaken = card.sessionDamageTakenTotal,
      healsReceived = card.sessionHealsReceivedTotal,
      tigerStrikes = card.sessionTigerStrikeTotal
    )

    fun fromEntity(entity: PlayerSessionTotalsEntity) = SessionTotals(
      damage = entity.totalDamage,
      healing = entity.totalHealing,
      cc = entity.totalCC,
      buffs = entity.totalBuffs,
      debuffs = entity.totalDebuffs,
      charms = entity.totalCharms,
      distress = entity.totalDistresses,
      silence = entity.totalSilences,
      glider = entity.totalGliderUses,
      items = entity.totalItemSkills,
      potions = entity.totalPotions,
      kills = entity.totalKills,
      killsKB = entity.totalKillsKB,
      damageTaken = entity.totalDamageTaken,
      healsReceived = entity.totalHealsReceived,
      tigerStrikes = entity.totalTigerStrikes
    )
  }
}

@Composable
fun SessionStatRows(totals: SessionTotals) {
  StatRow(stringResource(Res.string.player_card_stat_damage), totals.damage, RFColors.dpsOrange)
  StatRow(stringResource(Res.string.player_card_stat_healing), totals.healing, RFColors.healsGreen)
  StatRow(stringResource(Res.string.player_card_stat_cc), totals.cc.toLong(), RFColors.ccCyan)
  StatRow(stringResource(Res.string.player_card_stat_buffs), totals.buffs.toLong(), RFColors.buffsBlue)
  StatRow(stringResource(Res.string.player_card_stat_debuffs), totals.debuffs.toLong(), RFColors.debuffsPurple)
  StatRow(stringResource(Res.string.player_card_stat_charms), totals.charms.toLong(), RFColors.charmPink)
  StatRow(stringResource(Res.string.player_card_stat_distress), totals.distress.toLong(), RFColors.distressPurple)
  StatRow(stringResource(Res.string.player_card_stat_silence), totals.silence.toLong(), RFColors.silencePurple)
  StatRow(stringResource(Res.string.player_card_stat_tiger_strikes), totals.tigerStrikes.toLong(), RFColors.techNoTigerStrikes)
  StatRow(stringResource(Res.string.player_card_stat_glider), totals.glider.toLong(), RFColors.gliderBlue)
  StatRow(stringResource(Res.string.player_card_stat_items), totals.items.toLong(), RFColors.itemSkillYellow)
  StatRow(stringResource(Res.string.player_card_stat_potions), totals.potions.toLong(), RFColors.potionTeal)
  StatRow(stringResource(Res.string.player_card_stat_kills_most_damage), totals.kills.toLong(), RFColors.killsRed)
  StatRow(stringResource(Res.string.player_card_stat_kills_killing_blow), totals.killsKB.toLong(), RFColors.killsHaranyaGreen)
  StatRow(stringResource(Res.string.player_card_stat_total_damage_taken), totals.damageTaken.toLong(), RFColors.killsRed)
  StatRow(stringResource(Res.string.player_card_stat_total_heals_received), totals.healsReceived.toLong(), RFColors.healsGreen)
}

@Composable
fun StatRow(label: String, value: Long, valueColor: Color = RFColors.TextPrimary) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, color = RFColors.TextSecondary, fontSize = 13.sp)
    Text(text = value.humanReadableAbbreviation(), color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
fun CompactSessionTotals(
  playerName: String,
  modifier: Modifier = Modifier
) {
  val card by PlayerCacheInteractor.observeCard(playerName).collectAsState()

  val sessionTotals = card?.let { SessionTotals.fromPlayerCard(it) } ?: return

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(RFColors.CardBackground.copy(alpha = 0.85f))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(1.dp)
  ) {
    Text(
      text = playerName,
      color = RFColors.TextPrimary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      modifier = Modifier.padding(bottom = 2.dp)
    )

    CompactStatRow(stringResource(Res.string.player_card_stat_damage), sessionTotals.damage, RFColors.dpsOrange)
    CompactStatRow(stringResource(Res.string.player_card_stat_healing), sessionTotals.healing, RFColors.healsGreen)
    CompactStatRow(stringResource(Res.string.player_card_stat_cc), sessionTotals.cc.toLong(), RFColors.ccCyan)
    CompactStatRow(stringResource(Res.string.player_card_stat_kills_most_damage), sessionTotals.kills.toLong(), RFColors.killsRed)
    CompactStatRow(stringResource(Res.string.player_card_stat_kills_killing_blow), sessionTotals.killsKB.toLong(), RFColors.killsHaranyaGreen)
    CompactStatRow(stringResource(Res.string.player_card_stat_total_damage_taken), sessionTotals.damageTaken.toLong(), RFColors.killsRed)
    CompactStatRow(stringResource(Res.string.player_card_stat_total_heals_received), sessionTotals.healsReceived.toLong(), RFColors.healsGreen)
    CompactStatRow(stringResource(Res.string.player_card_stat_buffs), sessionTotals.buffs.toLong(), RFColors.buffsBlue)
    CompactStatRow(stringResource(Res.string.player_card_stat_debuffs), sessionTotals.debuffs.toLong(), RFColors.debuffsPurple)
    CompactStatRow(stringResource(Res.string.player_card_stat_charms), sessionTotals.charms.toLong(), RFColors.charmPink)
    CompactStatRow(stringResource(Res.string.player_card_stat_distress), sessionTotals.distress.toLong(), RFColors.distressPurple)
    CompactStatRow(stringResource(Res.string.player_card_stat_silence), sessionTotals.silence.toLong(), RFColors.silencePurple)
    CompactStatRow(stringResource(Res.string.player_card_stat_tiger_strikes), sessionTotals.tigerStrikes.toLong(), RFColors.techNoTigerStrikes)
    CompactStatRow(stringResource(Res.string.player_card_stat_glider), sessionTotals.glider.toLong(), RFColors.gliderBlue)
    CompactStatRow(stringResource(Res.string.player_card_stat_items), sessionTotals.items.toLong(), RFColors.itemSkillYellow)
    CompactStatRow(stringResource(Res.string.player_card_stat_potions), sessionTotals.potions.toLong(), RFColors.potionTeal)
  }
}

@Composable
private fun CompactStatRow(label: String, value: Long, valueColor: Color) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, color = RFColors.TextSecondary, fontSize = 9.sp)
    Text(text = value.humanReadableAbbreviation(), color = valueColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
  }
}
