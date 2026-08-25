package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.definitions.META_SPEC_CATEGORIES
import com.reoky.raidframer.core.definitions.MetaSpecCategory
import com.reoky.raidframer.core.definitions.MetaSpecs
import com.reoky.raidframer.core.definitions.MetaSpecsRepo
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.localizedDisplayNameRes
import com.reoky.raidframer.core.definitions.rememberMetaSpecs
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.SpecTypePickerComponent
import com.reoky.raidframer.ui.component.TitleBarComponent
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.meta_specs_add
import raid_framer_desktop.composeapp.generated.resources.meta_specs_accept
import raid_framer_desktop.composeapp.generated.resources.meta_specs_cancel
import raid_framer_desktop.composeapp.generated.resources.meta_specs_custom
import raid_framer_desktop.composeapp.generated.resources.meta_specs_description
import raid_framer_desktop.composeapp.generated.resources.meta_specs_none
import raid_framer_desktop.composeapp.generated.resources.meta_specs_reset
import raid_framer_desktop.composeapp.generated.resources.meta_specs_stock
import raid_framer_desktop.composeapp.generated.resources.meta_specs_title

private fun MetaSpecs.get(category: MetaSpecCategory): Set<SpecType> = when (category) {
  MetaSpecCategory.CC -> cc
  MetaSpecCategory.MELEE -> melee
  MetaSpecCategory.HEALER -> healer
  MetaSpecCategory.MAGE -> mage
  MetaSpecCategory.DANCER -> dancer
  MetaSpecCategory.RANGED -> ranged
}

private fun MetaSpecs.with(category: MetaSpecCategory, value: Set<SpecType>): MetaSpecs = when (category) {
  MetaSpecCategory.CC -> copy(cc = value)
  MetaSpecCategory.MELEE -> copy(melee = value)
  MetaSpecCategory.HEALER -> copy(healer = value)
  MetaSpecCategory.MAGE -> copy(mage = value)
  MetaSpecCategory.DANCER -> copy(dancer = value)
  MetaSpecCategory.RANGED -> copy(ranged = value)
}

/**
 * Opaque tool-tip window (like Settings) for editing the meta specs. Uses a tag-system editor:
 * each category renders its specs as chips (with an x to remove) and a + button to add via the
 * standardized [SpecTypePickerComponent]. Accept persists, Cancel discards.
 */
@Composable
fun MetaSpecsOverlay(wm: WindowManager? = null) {
  val current = rememberMetaSpecs()
  var draft by remember { mutableStateOf(current) }
  var addCategory by remember { mutableStateOf<MetaSpecCategory?>(null) }

  fun commit() {
    MetaSpecsRepo.update(draft)
    wm?.closeWindow(OverlayType.META_SPECS)
  }

  fun cancel() {
    wm?.closeWindow(OverlayType.META_SPECS)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF121212))
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
      TitleBarComponent(
        title = stringResource(Res.string.meta_specs_title),
        onClose = { cancel() }
      )

      // Stock / Custom status line
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(if (draft.isStock) Res.string.meta_specs_stock else Res.string.meta_specs_custom),
          color = if (draft.isStock) RFColors.TextSecondary else RFColors.metaSpecCustom,
          fontWeight = if (draft.isStock) FontWeight.Normal else FontWeight.Bold,
          fontSize = 12.sp
        )
        Text(
          text = stringResource(Res.string.meta_specs_reset),
          color = RFColors.AccentRed,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          modifier = Modifier
            .clickable { draft = MetaSpecs.STOCK }
            .padding(4.dp)
        )
      }

      Text(
        text = stringResource(Res.string.meta_specs_description),
        color = RFColors.TextSecondary,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
      )

      META_SPEC_CATEGORIES.forEach { category ->
        MetaSpecCategoryRow(
          category = category,
          specs = draft.get(category),
          stock = MetaSpecs.STOCK.get(category),
          onRemove = { spec ->
            draft = draft.with(category, draft.get(category) - spec)
          },
          onAdd = { addCategory = category }
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Button(
          onClick = { cancel() },
          colors = ButtonDefaults.buttonColors(RFColors.CardBorder),
          modifier = Modifier.weight(1f)
        ) {
          Text(stringResource(Res.string.meta_specs_cancel), color = RFColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        }
        Button(
          onClick = { commit() },
          colors = ButtonDefaults.buttonColors(RFColors.AccentRed),
          modifier = Modifier.weight(1f)
        ) {
          Text(stringResource(Res.string.meta_specs_accept), color = Color.White, fontWeight = FontWeight.SemiBold)
        }
      }
    }

    // Add-spec overlay (covers the window so the picker's dropdowns have vertical room).
    val category = addCategory
    if (category != null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.6f))
          .clickable(enabled = false) {}
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(RFColors.WellColor, RoundedCornerShape(10.dp))
            .border(1.dp, RFColors.CardBorder, RoundedCornerShape(10.dp))
            .padding(16.dp)
        ) {
          Text(
            text = "${stringResource(category.labelRes)} — ${stringResource(Res.string.meta_specs_add)}",
            color = RFColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Spacer(modifier = Modifier.height(12.dp))
          SpecTypePickerComponent(
            onAccept = { spec ->
              draft = draft.with(category, draft.get(category) + spec)
              addCategory = null
            },
            onCancel = { addCategory = null }
          )
        }
      }
    }
  }
}

@Composable
private fun MetaSpecCategoryRow(
  category: MetaSpecCategory,
  specs: Set<SpecType>,
  stock: Set<SpecType>,
  onRemove: (SpecType) -> Unit,
  onAdd: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = stringResource(category.labelRes),
        color = RFColors.TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
      )
      IconButton(
        onClick = onAdd,
        modifier = Modifier.width(30.dp).height(30.dp)
      ) {
        Text("+", color = RFColors.metaSpecPlus, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
      }
    }
    if (specs.isEmpty()) {
      Text(
        text = stringResource(Res.string.meta_specs_none),
        color = RFColors.TextTertiary,
        fontSize = 11.sp
      )
    } else {
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        specs.sortedBy { it.name }.forEach { spec ->
          MetaSpecTag(
            spec = spec,
            custom = spec !in stock,
            onRemove = { onRemove(spec) }
          )
        }
      }
    }
  }
}

@Composable
private fun MetaSpecTag(spec: SpecType, custom: Boolean, onRemove: () -> Unit) {
  Row(
    modifier = Modifier
      .background(if (custom) RFColors.metaSpecTagBgCustom else RFColors.metaSpecTagBg, RoundedCornerShape(6.dp))
      .border(1.dp, RFColors.metaSpecTagBorder, RoundedCornerShape(6.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = stringResource(spec.localizedDisplayNameRes),
      color = if (custom) RFColors.metaSpecCustom else RFColors.TextPrimary,
      fontWeight = if (custom) FontWeight.Bold else FontWeight.Normal,
      fontSize = 12.sp,
      maxLines = 1
    )
    Spacer(modifier = Modifier.width(4.dp))
    IconButton(
      onClick = onRemove,
      modifier = Modifier.width(18.dp).height(18.dp)
    ) {
      Text("x", color = RFColors.metaSpecRemove, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
  }
}