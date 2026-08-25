package com.reoky.raidframer.core.definitions

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.serialization.AppJson
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.meta_specs_cc
import raid_framer_desktop.composeapp.generated.resources.meta_specs_dancer
import raid_framer_desktop.composeapp.generated.resources.meta_specs_healer
import raid_framer_desktop.composeapp.generated.resources.meta_specs_mage
import raid_framer_desktop.composeapp.generated.resources.meta_specs_melee
import raid_framer_desktop.composeapp.generated.resources.meta_specs_ranged

/**
 * The six user-editable meta spec categories. When [isStock] is true the built-in
 * opinionated sets in SkillTreeDefinition.kt (META_*_SPECS) are in use; otherwise the
 * user has customized at least one category.
 */
data class MetaSpecs(
  val cc: Set<SpecType>,
  val melee: Set<SpecType>,
  val healer: Set<SpecType>,
  val mage: Set<SpecType>,
  val dancer: Set<SpecType>,
  val ranged: Set<SpecType>,
) {
  /** True when every category exactly matches the built-in stock sets. */
  val isStock: Boolean
    get() = this == STOCK

  /** Number of customized (non-stock) categories. */
  val customCategoryCount: Int
    get() {
      val s = STOCK
      return listOf(
        cc != s.cc, melee != s.melee, healer != s.healer,
        mage != s.mage, dancer != s.dancer, ranged != s.ranged
      ).count { it }
    }

  companion object {
    /** The built-in opinionated sets. These remain the "stock" baseline. */
    val STOCK = MetaSpecs(
      cc = META_CC_SPECS,
      melee = META_MELEE_SPECS,
      healer = META_HEALER_SPECS,
      mage = META_MAGE_SPECS,
      dancer = META_DANCER_SPECS,
      ranged = META_RANGED_SPEC,
    )
  }
}

/** A single category of the editor, used to keep UI state and stock-checking uniform. */
enum class MetaSpecCategory(val labelRes: StringResource) {
  CC(Res.string.meta_specs_cc),
  MELEE(Res.string.meta_specs_melee),
  HEALER(Res.string.meta_specs_healer),
  MAGE(Res.string.meta_specs_mage),
  DANCER(Res.string.meta_specs_dancer),
  RANGED(Res.string.meta_specs_ranged),
}

/** Categories ordered for the editor UI. */
val META_SPEC_CATEGORIES = MetaSpecCategory.entries

/** JSON DTO for the persisted overrides. Stores enum names so unknown specs are dropped on load. */
@Serializable
private data class MetaSpecsJson(
  val cc: List<String> = emptyList(),
  val melee: List<String> = emptyList(),
  val healer: List<String> = emptyList(),
  val mage: List<String> = emptyList(),
  val dancer: List<String> = emptyList(),
  val ranged: List<String> = emptyList(),
)

/** Returns [MetaSpecs] parsed from the persisted JSON, or null when it's blank (stock). */
fun parseMetaSpecsJson(json: String): MetaSpecs? {
  if (json.isBlank()) return null
  return runCatching {
    val dto = AppJson.decodeFromString<MetaSpecsJson>(json)
    MetaSpecs(
      cc = dto.cc.toSpecSet(),
      melee = dto.melee.toSpecSet(),
      healer = dto.healer.toSpecSet(),
      mage = dto.mage.toSpecSet(),
      dancer = dto.dancer.toSpecSet(),
      ranged = dto.ranged.toSpecSet(),
    )
  }.getOrNull()
}

/** Encodes [MetaSpecs] to the persisted JSON. Names are sorted for deterministic storage. */
fun encodeMetaSpecsJson(specs: MetaSpecs): String {
  val dto = MetaSpecsJson(
    cc = specs.cc.map { it.name }.sorted(),
    melee = specs.melee.map { it.name }.sorted(),
    healer = specs.healer.map { it.name }.sorted(),
    mage = specs.mage.map { it.name }.sorted(),
    dancer = specs.dancer.map { it.name }.sorted(),
    ranged = specs.ranged.map { it.name }.sorted(),
  )
  return AppJson.encodeToString(MetaSpecsJson.serializer(), dto)
}

private fun List<String>.toSpecSet(): Set<SpecType> =
  mapNotNull { SpecType.fromName(it) }.toSet()

/**
 * Single reactive source of truth for the effective meta specs, derived from the persisted
 * config. Falls back to the stock sets whenever the config blob is blank (or unparseable).
 */
object MetaSpecsRepo {
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  val state: StateFlow<MetaSpecs> = RFConfig.state
    .map { parseMetaSpecsJson(it.customMetaSpecsJson) ?: MetaSpecs.STOCK }
    .stateIn(scope, SharingStarted.Eagerly, MetaSpecs.STOCK)

  /** Synchronous, non-suspending access for analysis/helper code. */
  val current: MetaSpecs
    get() = state.value

  /** Persist the user's overrides and mark the sets as custom. */
  fun update(specs: MetaSpecs) {
    RFConfig.update { it.copy(customMetaSpecsJson = encodeMetaSpecsJson(specs)) }
  }

  /** Clear the overrides, restoring the built-in stock sets. */
  fun reset() {
    RFConfig.update { it.copy(customMetaSpecsJson = "") }
  }
}

/** Reactive read for composables. Re-composes whenever the persisted meta specs change. */
@Composable
fun rememberMetaSpecs(): MetaSpecs {
  val specs by MetaSpecsRepo.state.collectAsState()
  return specs
}