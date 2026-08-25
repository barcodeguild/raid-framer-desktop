package com.reoky.raidframer.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.localizedDisplayNameRes
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.helpers.FontsHelper
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.accentColor
import com.reoky.raidframer.core.helpers.skillTreeIconPainterFor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.meta_specs_accept
import raid_framer_desktop.composeapp.generated.resources.meta_specs_cancel
import raid_framer_desktop.composeapp.generated.resources.meta_specs_unknown_spec
import raid_framer_desktop.composeapp.generated.resources.skill_tree_defense

/**
 * Standardized Spec-Type picker. A searchable spec-type dropdown drives three searchable
 * skill-tree dropdowns; choosing a spec type auto-fills the trees, and editing any tree
 * reverse-resolves the spec type via [SpecType.fromTrees]. Invalid/incomplete/impossible
 * combinations are rejected (Accept is disabled) and surface as "Unknown / Incomplete".
 *
 * Pressing Accept calls [onAccept] with a valid [SpecType]; Cancel calls [onCancel] and
 * closes. This component is intentionally callback-based so it can be reused by other features.
 */
@Composable
fun SpecTypePickerComponent(
  initialSpec: SpecType? = null,
  onAccept: (SpecType) -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Three tree slots, kept in display order. null = unset slot.
  var trees by remember(initialSpec) {
    mutableStateOf<List<SkillTreeType?>>(initialSpec?.trees?.sortedByDisplayOrder() ?: emptyList())
  }

  // The spec the current tree combination resolves to (or null when invalid).
  val resolvedSpec = remember(trees) {
    val set = trees.filterNotNull().toSet()
    if (set.size < 3) null else SpecType.fromTrees(set).takeUnless { it == SpecType.UNKNOWN }
  }
  val selectedSpec = resolvedSpec

  fun selectSpec(spec: SpecType) {
    trees = spec.trees.sortedByDisplayOrder()
  }

  fun changeTree(index: Int, tree: SkillTreeType?) {
    val list = trees.toMutableList()
    while (list.size <= index) list.add(null)
    list[index] = tree
    while (list.size > 3) list.removeAt(3)
    trees = list
  }

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    SearchableDropdown(
      currentValue = selectedSpec,
      displayValue = { spec -> stringResource(spec.localizedDisplayNameRes) },
      placeholder = stringResource(Res.string.meta_specs_unknown_spec),
      items = SpecType.entries.filterNot { it == SpecType.UNKNOWN },
      labelFor = { spec -> stringResource(spec.localizedDisplayNameRes) },
      onSelected = { selectSpec(it) },
      itemColor = { RFColors.metaSpecPickerAccent },
      leadingFaCode = "\uf71b", // Font Awesome "swords" (crossed swords) — represents a spec/build
      modifier = Modifier.fillMaxWidth()
    )

    for (index in 0..2) {
      SearchableDropdown(
        currentValue = trees.getOrNull(index),
        displayValue = { tree -> stringResource(tree.localizedDisplayNameRes) },
        placeholder = stringResource(Res.string.skill_tree_defense),
        items = SkillTreeType.entries,
        labelFor = { tree -> stringResource(tree.localizedDisplayNameRes) },
        onSelected = { changeTree(index, it) },
        iconFor = { tree -> skillTreeIconPainterFor(tree) },
        itemColor = { tree -> tree.accentColor() },
        modifier = Modifier.fillMaxWidth()
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
      verticalAlignment = Alignment.CenterVertically
    ) {
      PickerButton(
        labelRes = Res.string.meta_specs_cancel,
        enabled = true,
        onClick = onCancel
      )
      PickerButton(
        labelRes = Res.string.meta_specs_accept,
        enabled = selectedSpec != null,
        onClick = { selectedSpec?.let(onAccept) },
        accent = true
      )
    }
  }
}

@Composable
private fun RowScope.PickerButton(
  labelRes: StringResource,
  enabled: Boolean,
  onClick: () -> Unit,
  accent: Boolean = false,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, if (enabled) RFColors.metaSpecTagBorder else RFColors.CardBorder),
    colors = ButtonDefaults.buttonColors(
      backgroundColor = if (accent) RFColors.AccentRed else RFColors.CardBackground,
      contentColor = if (accent) Color.White else RFColors.TextPrimary,
      disabledBackgroundColor = RFColors.CardBackground.copy(alpha = 0.5f),
      disabledContentColor = RFColors.TextDisabled
    ),
    modifier = Modifier.weight(1f)
  ) {
    Text(
      text = stringResource(labelRes),
      color = if (enabled) (if (accent) Color.White else RFColors.TextPrimary) else RFColors.TextDisabled,
      fontWeight = FontWeight.Bold,
      fontSize = 12.sp
    )
  }
}

/**
 * A searchable dropdown with autocomplete + keyboard navigation, modeled on the session-type
 * dropdown. Generic over [T] so it can power the spec-type picker and other features.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun <T> SearchableDropdown(
  currentValue: T?,
  displayValue: @Composable (T) -> String,
  placeholder: String,
  items: List<T>,
  labelFor: @Composable (T) -> String,
  onSelected: (T) -> Unit,
  itemColor: (T) -> Color = { RFColors.metaSpecPickerAccent },
  iconFor: @Composable ((T) -> Painter)? = null,
  leadingFaCode: String? = null,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var highlightedIndex by remember { mutableStateOf(0) }
  val focusRequester = remember { FocusRequester() }
  val normalizedQuery = searchQuery.trim()
  // Compute labels in the composable context (labelFor is @Composable), then filter/sort on the String.
  val labeledItems = buildList {
    for (item in items) add(item to labelFor(item))
  }
  val filteredItems = labeledItems
    .filter { (_, label) -> normalizedQuery.isBlank() || label.contains(normalizedQuery, ignoreCase = true) }
    .sortedWith(compareBy { (_, label) -> !label.startsWith(normalizedQuery, ignoreCase = true) })

  LaunchedEffect(expanded) {
    if (expanded) {
      searchQuery = ""
      highlightedIndex = 0
      focusRequester.requestFocus()
    }
  }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = modifier
  ) {
    val shown = if (expanded) {
      searchQuery
    } else {
      val cv = currentValue
      if (cv == null) "" else displayValue(cv)
    }
    OutlinedTextField(
      value = shown,
      onValueChange = {
        searchQuery = it
        highlightedIndex = 0
      },
      readOnly = !expanded,
      placeholder = { Text(placeholder, color = RFColors.TextTertiary, fontSize = 13.sp, maxLines = 1) },
      leadingIcon = {
        if (!expanded) {
          if (leadingFaCode != null) {
            Text(
              text = leadingFaCode,
              fontFamily = FontsHelper.faSolid(),
              color = if (currentValue != null) itemColor(currentValue) else RFColors.TextSecondary,
              fontSize = 14.sp
            )
          } else if (currentValue != null) {
            iconFor?.let { icon ->
              Icon(
                icon(currentValue),
                null,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
        .onPreviewKeyEvent { event ->
          if (event.type != KeyEventType.KeyDown) {
            false
          } else {
            when (event.key) {
              Key.DirectionDown -> {
                if (filteredItems.isNotEmpty()) highlightedIndex = (highlightedIndex + 1) % filteredItems.size
                true
              }
              Key.DirectionUp -> {
                if (filteredItems.isNotEmpty()) {
                  highlightedIndex = (highlightedIndex - 1 + filteredItems.size) % filteredItems.size
                }
                true
              }
              Key.Enter -> {
                filteredItems.getOrNull(highlightedIndex)?.let { (item, _) -> onSelected(item) }
                expanded = false
                true
              }
              else -> false
            }
          }
        },
      colors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = if (currentValue != null) itemColor(currentValue) else RFColors.TextSecondary,
        backgroundColor = Color(0xFF1E1E1E),
        focusedBorderColor = RFColors.metaSpecPickerAccent,
        unfocusedBorderColor = RFColors.CardBorder,
        cursorColor = RFColors.metaSpecPickerAccent,
        disabledTextColor = RFColors.TextDisabled,
        disabledBorderColor = RFColors.CardBorder,
        placeholderColor = RFColors.TextTertiary
      ),
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
      },
      textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
      singleLine = true,
      maxLines = 1,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      shape = RoundedCornerShape(8.dp)
    )

    MaterialTheme(colors = MaterialTheme.colors.copy(surface = RFColors.CardBackground)) {
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, RFColors.CardBorder, RoundedCornerShape(8.dp))
      ) {
        filteredItems.forEachIndexed { index, (item, label) ->
          DropdownMenuItem(
            onClick = {
              onSelected(item)
              expanded = false
            },
            modifier = Modifier.background(
              if (index == highlightedIndex) RFColors.metaSpecPickerAccent.copy(alpha = 0.15f)
              else Color.Transparent
            )
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              if (leadingFaCode != null) {
                Text(
                  text = leadingFaCode,
                  fontFamily = FontsHelper.faSolid(),
                  color = if (item == currentValue) itemColor(item) else RFColors.TextSecondary,
                  fontSize = 13.sp
                )
              } else {
                iconFor?.let { icon ->
                  Icon(
                    icon(item),
                    null,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
              Text(
                text = label,
                color = if (item == currentValue) itemColor(item) else RFColors.TextPrimary,
                fontWeight = if (item == currentValue) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1
              )
            }
          }
        }
      }
    }
  }
}