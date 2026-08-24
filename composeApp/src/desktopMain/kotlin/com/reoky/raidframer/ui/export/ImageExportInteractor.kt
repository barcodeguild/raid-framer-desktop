package com.reoky.raidframer.ui.export

import androidx.compose.ui.graphics.Color as ComposeColor
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.MAX_EXPORT_BACKGROUND_DIMNESS
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.localizedDisplayNameRes
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.getDocumentsDirectory
import com.reoky.raidframer.core.helpers.getFactionHighlightColor
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.interactor.Log
import com.reoky.raidframer.core.locale.AppLocale
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.LifeMendQuality
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.pvpPerformancePoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.export_no_data
import raid_framer_desktop.composeapp.generated.resources.export_title_battle_summary
import raid_framer_desktop.composeapp.generated.resources.export_header_on
import raid_framer_desktop.composeapp.generated.resources.export_header_off
import raid_framer_desktop.composeapp.generated.resources.export_header_ode_label
import raid_framer_desktop.composeapp.generated.resources.export_header_pve_label
import raid_framer_desktop.composeapp.generated.resources.export_header_kills_label
import raid_framer_desktop.composeapp.generated.resources.export_header_most_damage
import raid_framer_desktop.composeapp.generated.resources.export_header_killing_blow
import raid_framer_desktop.composeapp.generated.resources.summary_haranya_builds
import raid_framer_desktop.composeapp.generated.resources.summary_most_item_usages
import raid_framer_desktop.composeapp.generated.resources.summary_nuia_builds
import raid_framer_desktop.composeapp.generated.resources.summary_pirate_builds
import raid_framer_desktop.composeapp.generated.resources.summary_top_buffs
import raid_framer_desktop.composeapp.generated.resources.summary_top_loot_peak
import raid_framer_desktop.composeapp.generated.resources.summary_worst_loot_peak
import raid_framer_desktop.composeapp.generated.resources.summary_top_buff_count
import raid_framer_desktop.composeapp.generated.resources.summary_top_charms
import raid_framer_desktop.composeapp.generated.resources.summary_top_debuffs
import raid_framer_desktop.composeapp.generated.resources.summary_top_damage_taken
import raid_framer_desktop.composeapp.generated.resources.export_combat_pve_cc
import raid_framer_desktop.composeapp.generated.resources.export_combat_pve_damage
import raid_framer_desktop.composeapp.generated.resources.export_combat_pve_heals
import raid_framer_desktop.composeapp.generated.resources.export_combat_pvp_cc
import raid_framer_desktop.composeapp.generated.resources.export_combat_pvp_damage
import raid_framer_desktop.composeapp.generated.resources.export_combat_pvp_heals
import raid_framer_desktop.composeapp.generated.resources.summary_top_distresses
import raid_framer_desktop.composeapp.generated.resources.summary_top_glider_gamers
import raid_framer_desktop.composeapp.generated.resources.summary_top_heals_received
import raid_framer_desktop.composeapp.generated.resources.summary_top_heal_ratio
import raid_framer_desktop.composeapp.generated.resources.summary_top_haranya_item_uses
import raid_framer_desktop.composeapp.generated.resources.summary_top_haranya_performance
import raid_framer_desktop.composeapp.generated.resources.summary_top_haranya_spells_damage
import raid_framer_desktop.composeapp.generated.resources.summary_top_kills_haranya
import raid_framer_desktop.composeapp.generated.resources.summary_top_kills_nuia
import raid_framer_desktop.composeapp.generated.resources.summary_top_kills_pirate
import raid_framer_desktop.composeapp.generated.resources.summary_top_nuia_item_uses
import raid_framer_desktop.composeapp.generated.resources.summary_top_nuia_performance
import raid_framer_desktop.composeapp.generated.resources.summary_top_nuia_spells_damage
import raid_framer_desktop.composeapp.generated.resources.summary_top_ode_haranya
import raid_framer_desktop.composeapp.generated.resources.summary_top_ode_nuia
import raid_framer_desktop.composeapp.generated.resources.summary_top_ode_pirate
import raid_framer_desktop.composeapp.generated.resources.summary_top_pirate_item_uses
import raid_framer_desktop.composeapp.generated.resources.summary_top_pirate_performance
import raid_framer_desktop.composeapp.generated.resources.summary_top_pirate_spells_damage
import raid_framer_desktop.composeapp.generated.resources.summary_top_potion_drinkers
import raid_framer_desktop.composeapp.generated.resources.summary_top_silences
import raid_framer_desktop.composeapp.generated.resources.summary_top_songs
import raid_framer_desktop.composeapp.generated.resources.summary_top_trips
import raid_framer_desktop.composeapp.generated.resources.summary_top_bubbles
import raid_framer_desktop.composeapp.generated.resources.summary_top_bracings
import raid_framer_desktop.composeapp.generated.resources.summary_top_shield_strip
import raid_framer_desktop.composeapp.generated.resources.summary_top_weapon_disables
import raid_framer_desktop.composeapp.generated.resources.summary_top_potion_disables
import raid_framer_desktop.composeapp.generated.resources.summary_top_bd_glider
import raid_framer_desktop.composeapp.generated.resources.summary_top_crystal_wings
import raid_framer_desktop.composeapp.generated.resources.summary_top_glider_disables
import raid_framer_desktop.composeapp.generated.resources.summary_top_provokes
import raid_framer_desktop.composeapp.generated.resources.summary_top_tiger_strikes
import raid_framer_desktop.composeapp.generated.resources.summary_top_freezes
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.compose.resources.getString
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.svg.SVGDOM
import raid_framer_desktop.composeapp.generated.resources.summary_top_deep_tranquility
import raid_framer_desktop.composeapp.generated.resources.summary_top_deepend_debuff
import raid_framer_desktop.composeapp.generated.resources.summary_top_throw_dagger
import raid_framer_desktop.composeapp.generated.resources.summary_top_stuns
import raid_framer_desktop.composeapp.generated.resources.summary_top_staggers
import raid_framer_desktop.composeapp.generated.resources.summary_top_absorb_lifeforce
import raid_framer_desktop.composeapp.generated.resources.summary_top_corrosive_barrage
import raid_framer_desktop.composeapp.generated.resources.summary_top_blinded_by_crows
import raid_framer_desktop.composeapp.generated.resources.summary_top_mist_sunder
import raid_framer_desktop.composeapp.generated.resources.summary_top_regular_sunder
import raid_framer_desktop.composeapp.generated.resources.summary_top_impales
import raid_framer_desktop.composeapp.generated.resources.summary_top_protective_wings
import raid_framer_desktop.composeapp.generated.resources.summary_top_courageous_action
import raid_framer_desktop.composeapp.generated.resources.summary_top_mana_barrier
import raid_framer_desktop.composeapp.generated.resources.summary_top_revive
import raid_framer_desktop.composeapp.generated.resources.summary_top_petrification
import raid_framer_desktop.composeapp.generated.resources.summary_top_defiance
import raid_framer_desktop.composeapp.generated.resources.summary_top_garden_defiance
import raid_framer_desktop.composeapp.generated.resources.summary_top_purges
import raid_framer_desktop.composeapp.generated.resources.summary_top_sac_dances
import raid_framer_desktop.composeapp.generated.resources.summary_top_life_mends
import javax.imageio.ImageIO

object ImageExportInteractor {

  data class ExportProgress(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val current: Int = 0,
    val total: Int = 1,
    val languageCode: String = "",
    val item: String = "",
  )

  private val _progress = MutableStateFlow(ExportProgress())
  val progress = _progress.asStateFlow()

  private const val IMAGE_WIDTH = 4500
  // Keep the final PNG at the wallpaper's max width, but render content larger first.
  private const val EXPORT_RENDER_SCALE = 2
  private const val SVG_ICON_RENDER_SCALE = 4
  private const val ROW_HEIGHT = 24
  private const val SECTION_HEADER_HEIGHT = 32
  private const val CHART_HEIGHT = 170
  private const val CATEGORY_MIN_HEIGHT = 100
  private const val COLUMN_GAP = 10
  private const val CARD_PADDING = 8
  private const val SUPER_COL_GAP = 10
  private const val SUPER_COLUMN_COUNT = 5

  // Title card is now full-width; store its fixed height, so both layout functions agree.
  private const val TITLE_CARD_HEIGHT = 90

  private val CARD_BACKGROUND = Color(0, 0, 0)
  private val CARD_BACKGROUND_TRANSPARENT = Color(0, 0, 0, 140)
  private val BORDER_COLOR = Color(55, 55, 70)
  private val BORDER_COLOR_TRANSPARENT = Color(55, 55, 70, 160)

  private fun toAwtColor(composeColor: ComposeColor): Color {
    return Color(composeColor.red, composeColor.green, composeColor.blue, composeColor.alpha)
  }

  private val TEXT_PRIMARY        = toAwtColor(RFColors.TextPrimary)
  private val HARANYA_COLOR       = toAwtColor(RFColors.factionHaranya)
  private val NUIA_COLOR          = toAwtColor(RFColors.factionNuia)
  private val PIRATE_COLOR        = toAwtColor(RFColors.factionPirate)
  private val KILLS_HARANYA_COLOR = toAwtColor(RFColors.killsHaranyaGreen)
  private val KILLS_NUIA_COLOR    = toAwtColor(RFColors.killsNuiaOrange)
  private val KILLS_PIRATE_COLOR  = toAwtColor(RFColors.killsPirateRed)
  private val POTION_COLOR        = toAwtColor(RFColors.potionTeal)
  private val GLIDER_COLOR        = toAwtColor(RFColors.gliderBlue)
  private val ITEM_SKILL_COLOR    = toAwtColor(RFColors.itemSkillYellow)
  private val SILENCE_COLOR       = Color(0xAB47BC)
  private val CHARM_COLOR         = Color(0xEC407A)
  private val DISTRESS_COLOR      = Color(0x7E57C2)
  private val TRIPS_COLOR = toAwtColor(RFColors.tripsAmber)
  private val BUBBLES_COLOR = toAwtColor(RFColors.bubblesCyan)
  private val BRACINGS_COLOR = toAwtColor(RFColors.bracingsGreen)
  private val SHIELD_STRIP_COLOR = toAwtColor(RFColors.shieldStripOrange)
  private val WEAPON_DISABLES_COLOR = toAwtColor(RFColors.weaponDisablesRed)
  private val POTION_DISABLES_COLOR = toAwtColor(RFColors.potionDisablesPurple)
  private val BD_GLIDER_COLOR = toAwtColor(RFColors.bdGliderTeal)
  private val CRYSTAL_WINGS_COLOR = toAwtColor(RFColors.crystalWingsBlue)
  private val GLIDER_DISABLES_COLOR = toAwtColor(RFColors.gliderDisablesPink)
  private val PROVOKED_COLOR = toAwtColor(RFColors.provokesDeepPurple)
  private val TIGER_STRIKE_COLOR = toAwtColor(RFColors.techNoTigerStrikes)
  private val FREEZE_COLOR = toAwtColor(RFColors.freezeIceBlue)
  private val DEFIANCE_COLOR = toAwtColor(RFColors.defianceGold)
  private val GARDEN_DEFIANCE_COLOR = toAwtColor(RFColors.gardenDefianceBlue)
  private val PURGE_COLOR = toAwtColor(RFColors.purgeGreen)
  private val SAC_DANCE_COLOR = toAwtColor(RFColors.sacDancePurple)
  private val DEEP_TRANQUILITY_COLOR = toAwtColor(RFColors.deepTranquilityTeal)
  private val DEEPEND_DEBUFF_COLOR = toAwtColor(RFColors.deedendDebuffRed)
  private val THROW_DAGGER_COLOR = toAwtColor(RFColors.throwDaggerAmber)
  private val STUNS_COLOR = toAwtColor(RFColors.stunDeepRed)
  private val STAGGERS_COLOR = toAwtColor(RFColors.staggerBrown)
  private val PETRIFICATION_COLOR = toAwtColor(RFColors.petrificationGray)
  private val ABSORB_LIFEFORCE_COLOR = toAwtColor(RFColors.absorbLifeforceMagenta)
  private val CORROSIVE_BARRAGE_COLOR = toAwtColor(RFColors.corrosiveBarrageLime)
  private val BLINDED_BY_CROWS_COLOR = toAwtColor(RFColors.blindedByCrowsDark)
  private val MIST_SUNDER_COLOR = toAwtColor(RFColors.mistSunderCyan)
  private val REGULAR_SUNDER_COLOR = toAwtColor(RFColors.regularSunderOrange)
  private val IMPALE_IMMUNITY_COLOR = toAwtColor(RFColors.impaleImmunitySteel)
  private val PROTECTIVE_WINGS_COLOR = toAwtColor(RFColors.protectiveWingsGold)
  private val COURAGEOUS_ACTION_COLOR = toAwtColor(RFColors.courageousActionBright)
  private val MANA_BARRIER_COLOR = toAwtColor(RFColors.manaBarrierBlue)
  private val REVIVE_COLOR = toAwtColor(RFColors.reviveGhostWhite)
  private val LOOT_BUFF_COLOR = toAwtColor(RFColors.buffsBlue)

  /**
   * Maps a heal ratio (0.0 = 0% healed, 1.0 = 100% healed) to a color gradient:
   * Red (0%) -> Orange (25%) -> Yellow (50%) -> Green (75%) -> Cyan (100%+)
   */
  private fun healRatioColor(ratio: Float): Color {
    val clamped = ratio.coerceIn(0f, 1.5f)
    return when {
      clamped < 0.25f -> {
        val t = clamped / 0.25f
        Color(
          (255 * (1 - t * 0.38)).toInt(), // R: 255 -> 153
          (50 * t).toInt(),                 // G: 0 -> 50
          0,                                // B
          255
        )
      }
      clamped < 0.5f -> {
        val t = (clamped - 0.25f) / 0.25f
        Color(
          (153 + 102 * t).toInt(),  // R: 153 -> 255
          (50 + 171 * t).toInt(),  // G: 50 -> 221
          0,                        // B
          255
        )
      }
      clamped < 1.0f -> {
        val t = (clamped - 0.5f) / 0.5f
        Color(
          255,                      // R
          (221 + 34 * t).toInt(),  // G: 221 -> 255
          (intArrayOf(0, 50, 100, 150, 200, 230).getOrElse((t * 5).toInt()) { 230 }), // B increases
          255
        )
      }
      else -> Color(0, 230, 255, 255) // Cyan for over-healed
    }
  }

  // ── Skill-tree icon cache (SVG → BufferedImage via Skiko) ─────────────────
  private val skillTreeImageCache = ConcurrentHashMap<Pair<SkillTreeType?, Int>, BufferedImage?>()
  private val wallpaperCache = ConcurrentHashMap<String, BufferedImage?>()
  private var logoCache: BufferedImage? = null

  fun clearImageCaches() {
    skillTreeImageCache.values.forEach { it?.flush() }
    wallpaperCache.values.forEach { it?.flush() }
    logoCache?.flush()
    skillTreeImageCache.clear()
    wallpaperCache.clear()
    logoCache = null
    Log.info("ImageExport", "Cleared image caches after export")
  }

  private fun applyHighQualityRenderingHints(g2d: Graphics2D) {
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
    g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
  }

  /**
   * Renders an SVG resource to a [BufferedImage] at the requested [targetSize].
   *
   * All skill-tree icons have an intrinsic 64×64 viewBox.  Skiko's SVGDOM
   * clips rather than scales when the render surface is smaller than the SVG's
   * native size, so we always render at native resolution (64×64) and then
   * scale down with bilinear interpolation using Java2D.
   */
  private fun renderSvgToAwtImage(svgBytes: ByteArray, targetSize: Int): BufferedImage? {
    return try {
      val data   = Data.makeFromBytes(svgBytes)
      val svgDom = SVGDOM(data)

      val nativeSize = 64   // matches every skill-tree SVG's viewBox width/height
      val surface    = org.jetbrains.skia.Surface.makeRasterN32Premul(nativeSize, nativeSize)
      surface.canvas.clear(0x00000000)          // start with a transparent background
      svgDom.setContainerSize(nativeSize.toFloat(), nativeSize.toFloat())
      svgDom.render(surface.canvas)

      // skia is the ui framework compose (at least on Windows desktops but not Android nor iOS) uses for rendering the ui
      // to place SVGs inside this bitmap programmatically we are calling skia's own drawing functions.
      val bitmap = Bitmap()
      bitmap.allocPixels(ImageInfo.makeN32Premul(nativeSize, nativeSize))
      if (!surface.readPixels(bitmap, 0, 0)) return null

      val native = BufferedImage(nativeSize, nativeSize, BufferedImage.TYPE_INT_ARGB)
      for (py in 0 until nativeSize) {
        for (px in 0 until nativeSize) {
          native.setRGB(px, py, bitmap.getColor(px, py))
        }
      }

      // Scale to the requested display size using high-quality filtering.
      if (targetSize == nativeSize) return native
      val scaled = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB)
      val sg = scaled.createGraphics()
      applyHighQualityRenderingHints(sg)
      sg.drawImage(native, 0, 0, targetSize, targetSize, null)
      sg.dispose()
      scaled
    } catch (_: Exception) { null }
  }

  private fun loadSkillTreeImage(tree: SkillTreeType?, size: Int = 14): BufferedImage? {
    return skillTreeImageCache.getOrPut(tree to size) {
      try {
        val name = tree?.name?.lowercase() ?: "unknown"
        val uri  = Res.getUri("drawable/$name.svg")
        val bytes = URI(uri).toURL().openStream().use { it.readBytes() }
        renderSvgToAwtImage(bytes, size)
      } catch (_: Exception) { null }
    }
  }

  data class ExportData(
    val sessionTitle: String,
    val sessionDate: String,
    val sessionDurationMs: Long,
    val allowPvE: Boolean,
    val allowOdeToRecoveryCountAsHeals: Boolean,
    // val killCounterMode: String, // TODO: uncomment when kill counter mode selection is implemented
    // Pre-loaded localised strings
    val battleSummaryTitle: String,
    val noDataText: String,
    val exportHeaderOn: String,
    val exportHeaderOff: String,
    val exportHeaderOdeLabel: String,
    val exportHeaderPveLabel: String,
    val exportHeaderKillsLabel: String,
    val exportHeaderMostDamage: String,
    val exportHeaderKillingBlow: String,
    val combatDamageTitle: String,
    val combatHealsTitle: String,
    val combatCCTitle: String,
    val topDamage: List<PlayerCard>,
    val topHeals: List<PlayerCard>,
    val topCC: List<PlayerCard>,
    val topSilences: List<PlayerCard>,
    val topCharms: List<PlayerCard>,
    val topDistresses: List<PlayerCard>,
    val topDamageSpellsHaranya: List<SpellDamage>,
    val topDamageSpellsNuia: List<SpellDamage>,
    val topDamageSpellsPirate: List<SpellDamage>,
    val topDebuffs: List<PlayerCard>,
    val topSongs: List<PlayerCard>,
    val topBuffs: List<PlayerCard>,
    val topOdeHaranya: List<PlayerCard>,
    val topOdeNuia: List<PlayerCard>,
    val topOdePirate: List<PlayerCard>,
    val topKillsHaranya: List<PlayerCard>,
    val topKillsNuia: List<PlayerCard>,
    val topKillsPirate: List<PlayerCard>,
    val topDamageTaken: List<PlayerCard>,
    val topHealsReceived: List<PlayerCard>,
    val topHealRatio: List<Pair<PlayerCard, Float>>, // (card, ratio 0.0-1.0+)
    val topItemUsesHaranya: List<ItemUsage>,
    val topItemUsesNuia: List<ItemUsage>,
    val topItemUsesPirate: List<ItemUsage>,
    val topPotters: List<PlayerCard>,
    val topGliderGamers: List<PlayerCard>,
    val topItemSkillCasters: List<PlayerCard>,
    val buildCountsHaranya: Map<String, Int>,
    val buildCountsNuia: Map<String, Int>,
    val buildCountsPirate: Map<String, Int>,
    val buildDisplayNames: Map<String, String>,
    val topPerformanceHaranya: List<PlayerCard>,
    val topPerformanceNuia: List<PlayerCard>,
    val topPerformancePirate: List<PlayerCard>,
    val factionSilenceData: Map<String, Float>,
    val factionCharmData: Map<String, Float>,
    val factionDistressData: Map<String, Float>,
    // New debuff category rankings
    val topTigerStrikes: List<PlayerCard>,
    val topFreezes: List<PlayerCard>,
    val topTrips: List<PlayerCard>,
    val topBubbles: List<PlayerCard>,
    val topBracings: List<PlayerCard>,
    val topShieldStrip: List<PlayerCard>,
    val topWeaponDisables: List<PlayerCard>,
    val topPotionDisables: List<PlayerCard>,
    val topBdGlider: List<PlayerCard>,
    val topCrystalWings: List<PlayerCard>,
    val topGliderDisables: List<PlayerCard>,
     val topProvoked: List<PlayerCard>,
     val topDefiance: List<PlayerCard>,
     val topGardenDefiance: List<PlayerCard>,
     val topPurges: List<PlayerCard>,
      val topSacDances: List<PlayerCard>,
      val topLifeMenders: List<PlayerCard>,
      val topDeepTranquility: List<PlayerCard>,
      val topDeependDebuff: List<PlayerCard>,
      val topThrowDagger: List<PlayerCard>,
      val topStuns: List<PlayerCard>,
      val topStaggers: List<PlayerCard>,
      val topPetrification: List<PlayerCard>,
      val topAbsorbLifeforce: List<PlayerCard>,
      val topCorrosiveBarrage: List<PlayerCard>,
      val topBlindedByCrows: List<PlayerCard>,
      val topMistSunder: List<PlayerCard>,
      val topRegularSunder: List<PlayerCard>,
      val topImpaleImmunity: List<PlayerCard>,
      val topProtectiveWings: List<PlayerCard>,
      val topCourageousAction: List<PlayerCard>,
val topManaBarrier: List<PlayerCard>,
    val topRevive: List<PlayerCard>,
    // Loot buff rankings (raid-wide)
    val topLootPeak: List<PlayerCard>,
    val worstLootPeak: List<PlayerCard>,
    val topBuffCount: List<PlayerCard>,
    // New faction comparison data
    val factionTigerStrikeData: Map<String, Float>,
    val factionFreezeData: Map<String, Float>,
    val factionTripsData: Map<String, Float>,
    val factionBubblesData: Map<String, Float>,
    val factionBracingsData: Map<String, Float>,
    val factionShieldStripData: Map<String, Float>,
    val factionWeaponDisablesData: Map<String, Float>,
    val factionPotionDisablesData: Map<String, Float>,
    val factionBdGliderData: Map<String, Float>,
    val factionCrystalWingsData: Map<String, Float>,
    val factionGliderDisablesData: Map<String, Float>,
    val factionProvokedData: Map<String, Float>,
  )

  data class SpellDamage(val spell: String, val total: Double)
  data class ItemUsage(val itemName: String, val count: Int)

  data class ExportLanguage(val code: String, val locale: Locale, val nativeLabel: String)

  private fun selectedExportLanguages(): List<ExportLanguage> {
    val config = RFConfig.state.value
    val current = AppLocale.entryFor(config.preferredLanguage)
    val entries = buildList {
      if (current.code.isNotBlank()) {
        add(current)
      } else {
        // System Default — resolve to a supported language, falling back to English
        add(AppLocale.resolveSystemDefault())
      }
      addAll(AppLocale.validEntriesForCodes(config.exportPngLanguages))
    }.distinctBy { it.code }
    return entries.map { ExportLanguage(it.code, it.locale, it.nativeLabel) }
  }

  suspend fun captureSnapshot(explicitDurationMs: Long? = null): ExportData {
    val config = RFConfig.state.value
    val sessionStart = config.lastSessionStart
    val durationMs = explicitDurationMs ?: if (config.lastSessionDurationMs > 0) {
      config.lastSessionDurationMs
    } else if (sessionStart > 0) {
      System.currentTimeMillis() - sessionStart
    } else {
      0L
    }

    // take 50 of the main dmg/heals/cc chart and 15 top players of everything else. We can change this to whatever. I just
    // picked what I'd personally been doing in the screenshots.
    return ExportData(
      sessionTitle        = config.lastSessionTitle.ifBlank { "session" },
      sessionDate         = DateFormat.getDateInstance(DateFormat.SHORT).format(Date()),
      sessionDurationMs   = durationMs,
      allowPvE            = config.allowPVEDamage,
      allowOdeToRecoveryCountAsHeals = config.allowOdeToRecoveryCountAsHeals,
      // killCounterMode     = config.killCounterMode, // TODO: uncomment when kill counter mode selection is implemented
      battleSummaryTitle  = getString(Res.string.export_title_battle_summary),
      noDataText          = getString(Res.string.export_no_data),
      exportHeaderOn      = getString(Res.string.export_header_on),
      exportHeaderOff     = getString(Res.string.export_header_off),
      exportHeaderOdeLabel = getString(Res.string.export_header_ode_label),
      exportHeaderPveLabel = getString(Res.string.export_header_pve_label),
      exportHeaderKillsLabel = getString(Res.string.export_header_kills_label),
      exportHeaderMostDamage = getString(Res.string.export_header_most_damage),
      exportHeaderKillingBlow = getString(Res.string.export_header_killing_blow),
      combatDamageTitle = getString(
        if (config.allowPVEDamage) Res.string.export_combat_pve_damage else Res.string.export_combat_pvp_damage
      ),
      combatHealsTitle = getString(
        if (config.allowPVEDamage) Res.string.export_combat_pve_heals else Res.string.export_combat_pvp_heals
      ),
      combatCCTitle = getString(
        if (config.allowPVEDamage) Res.string.export_combat_pve_cc else Res.string.export_combat_pvp_cc
      ),
      topDamage           = PlayerCacheInteractor.topDamage.value.take(50),
      topHeals            = PlayerCacheInteractor.topHeals.value.take(50),
      topCC               = PlayerCacheInteractor.topCC.value.take(50),
      topSilences         = PlayerCacheInteractor.topSilences.value.take(25),
      topCharms           = PlayerCacheInteractor.topCharms.value.take(25),
      topDistresses       = PlayerCacheInteractor.topDistresses.value.take(25),
      topDamageSpellsHaranya = PlayerCacheInteractor.topDamageSpellsHaranya.value.take(25).map { SpellDamage(it.spell, it.total) },
      topDamageSpellsNuia    = PlayerCacheInteractor.topDamageSpellsNuia.value.take(25).map { SpellDamage(it.spell, it.total) },
      topDamageSpellsPirate  = PlayerCacheInteractor.topDamageSpellsPirate.value.take(25).map { SpellDamage(it.spell, it.total) },
      topDebuffs          = PlayerCacheInteractor.topDebuff.value.take(25),
      topSongs            = PlayerCacheInteractor.topSongs.value.take(25),
      topBuffs            = PlayerCacheInteractor.topBuffs.value.take(25),
      topOdeHaranya       = PlayerCacheInteractor.topOdeHaranya.value.take(25),
      topOdeNuia          = PlayerCacheInteractor.topOdeNuia.value.take(25),
      topOdePirate        = PlayerCacheInteractor.topOdePirate.value.take(25),
      topKillsHaranya     = PlayerCacheInteractor.topKillsHaranya.value.take(25),
      topKillsNuia        = PlayerCacheInteractor.topKillsNuia.value.take(25),
      topKillsPirate      = PlayerCacheInteractor.topKillsPirate.value.take(25),
      topDamageTaken      = PlayerCacheInteractor.topDamageTaken.value.take(25),
      topHealsReceived    = PlayerCacheInteractor.topHealsReceived.value.take(25),
      topHealRatio        = PlayerCacheInteractor.topDamageTaken.value
        .filter { it.sessionDamageTakenTotal > 0 }
        .sortedByDescending { it.sessionDamageTakenTotal }
        .map { card ->
          val ratio = if (card.sessionDamageTakenTotal > 0) {
            card.sessionHealsReceivedTotal.toFloat() / card.sessionDamageTakenTotal.toFloat()
          } else 0f
          card to ratio
        },
      topItemUsesHaranya  = PlayerCacheInteractor.topItemUsesHaranya.value.take(25).map { ItemUsage(getString(it.itemName), it.count) },
      topItemUsesNuia     = PlayerCacheInteractor.topItemUsesNuia.value.take(25).map { ItemUsage(getString(it.itemName), it.count) },
      topItemUsesPirate   = PlayerCacheInteractor.topItemUsesPirate.value.take(25).map { ItemUsage(getString(it.itemName), it.count) },
      topPotters          = PlayerCacheInteractor.topPotters.value.take(25),
      topGliderGamers     = PlayerCacheInteractor.topGliderGamers.value.take(25),
      topItemSkillCasters = PlayerCacheInteractor.topItemSkillCasters.value.take(25),
      buildCountsHaranya  = PlayerCacheInteractor.buildCountsHaranya.value,
      buildCountsNuia     = PlayerCacheInteractor.buildCountsNuia.value,
      buildCountsPirate   = PlayerCacheInteractor.buildCountsPirate.value,
      buildDisplayNames   = (PlayerCacheInteractor.buildCountsHaranya.value.keys +
                              PlayerCacheInteractor.buildCountsNuia.value.keys +
                              PlayerCacheInteractor.buildCountsPirate.value.keys)
        .distinct()
        .associateWith { name -> SpecType.fromName(name)?.let { getString(it.localizedDisplayNameRes) } ?: name },
      topPerformanceHaranya = PlayerCacheInteractor.topPerformanceHaranya.value.take(25),
      topPerformanceNuia    = PlayerCacheInteractor.topPerformanceNuia.value.take(25),
      topPerformancePirate  = PlayerCacheInteractor.topPerformancePirate.value.take(25),
      factionSilenceData  = PlayerCacheInteractor.factionSilenceComparisonAll.value,
      factionCharmData    = PlayerCacheInteractor.factionCharmComparisonAll.value,
      factionDistressData = PlayerCacheInteractor.factionDistressComparisonAll.value,
      // New debuff category rankings
      topTigerStrikes    = PlayerCacheInteractor.topTigerStrikes.value.take(25),
      topFreezes         = PlayerCacheInteractor.topFreezes.value.take(25),
      topTrips           = PlayerCacheInteractor.topTrips.value.take(25),
      topBubbles         = PlayerCacheInteractor.topBubbles.value.take(25),
      topBracings        = PlayerCacheInteractor.topBracings.value.take(25),
      topShieldStrip     = PlayerCacheInteractor.topShieldStrip.value.take(25),
      topWeaponDisables  = PlayerCacheInteractor.topWeaponDisables.value.take(25),
      topPotionDisables  = PlayerCacheInteractor.topPotionDisables.value.take(25),
      topBdGlider        = PlayerCacheInteractor.topBdGlider.value.take(25),
      topCrystalWings    = PlayerCacheInteractor.topCrystalWings.value.take(25),
      topGliderDisables  = PlayerCacheInteractor.topGliderDisables.value.take(25),
      topProvoked        = PlayerCacheInteractor.topProvoked.value.take(25),
      topDefiance        = PlayerCacheInteractor.topDefiance.value.take(25),
      topGardenDefiance  = PlayerCacheInteractor.topGardenDefiance.value.take(25),
      topPurges          = PlayerCacheInteractor.topPurges.value.take(25),
      topSacDances       = PlayerCacheInteractor.topSacDances.value.take(25),
      topLifeMenders     = PlayerCacheInteractor.topLifeMenders.value.take(25),
      topDeepTranquility = PlayerCacheInteractor.topDeepTranquility.value.take(25),
      topDeependDebuff = PlayerCacheInteractor.topDeependDebuff.value.take(25),
      topThrowDagger = PlayerCacheInteractor.topThrowDagger.value.take(25),
      topStuns = PlayerCacheInteractor.topStuns.value.take(25),
      topStaggers = PlayerCacheInteractor.topStaggers.value.take(25),
      topPetrification = PlayerCacheInteractor.topPetrification.value.take(25),
      topAbsorbLifeforce = PlayerCacheInteractor.topAbsorbLifeforce.value.take(25),
      topCorrosiveBarrage = PlayerCacheInteractor.topCorrosiveBarrage.value.take(25),
      topBlindedByCrows = PlayerCacheInteractor.topBlindedByCrows.value.take(25),
      topMistSunder = PlayerCacheInteractor.topMistSunder.value.take(25),
      topRegularSunder = PlayerCacheInteractor.topRegularSunder.value.take(25),
      topImpaleImmunity = PlayerCacheInteractor.topImpaleImmunity.value.take(25),
      topProtectiveWings = PlayerCacheInteractor.topProtectiveWings.value.take(25),
      topCourageousAction = PlayerCacheInteractor.topCourageousAction.value.take(25),
      topManaBarrier = PlayerCacheInteractor.topManaBarrier.value.take(25),
      topRevive = PlayerCacheInteractor.topRevive.value.take(25),
      topLootPeak = PlayerCacheInteractor.topLootPeak.value.take(25),
      worstLootPeak = PlayerCacheInteractor.worstLootPeak.value.take(25),
      topBuffCount = PlayerCacheInteractor.topBuffCount.value.take(25),
      // New faction comparison data
      factionTigerStrikeData   = PlayerCacheInteractor.factionTigerStrikeComparisonAll.value,
      factionFreezeData        = PlayerCacheInteractor.factionFreezeComparisonAll.value,
      factionTripsData         = PlayerCacheInteractor.factionTripsComparisonAll.value,
      factionBubblesData       = PlayerCacheInteractor.factionBubblesComparisonAll.value,
      factionBracingsData      = PlayerCacheInteractor.factionBracingsComparisonAll.value,
      factionShieldStripData   = PlayerCacheInteractor.factionShieldStripComparisonAll.value,
      factionWeaponDisablesData = PlayerCacheInteractor.factionWeaponDisablesComparisonAll.value,
      factionPotionDisablesData = PlayerCacheInteractor.factionPotionDisablesComparisonAll.value,
      factionBdGliderData      = PlayerCacheInteractor.factionBdGliderComparisonAll.value,
      factionCrystalWingsData  = PlayerCacheInteractor.factionCrystalWingsComparisonAll.value,
      factionGliderDisablesData = PlayerCacheInteractor.factionGliderDisablesComparisonAll.value,
      factionProvokedData      = PlayerCacheInteractor.factionProvokedComparisonAll.value,
    )
  }

  suspend fun exportToPng(data: ExportData): File? {
    val languages = selectedExportLanguages()
    if (!RFConfig.state.value.exportPngEnabled || languages.isEmpty()) return null
    _progress.value = ExportProgress(isExporting = true, current = 0, total = languages.size)
    return withContext(Dispatchers.IO) {
      var firstOutput: File? = null
      try {
        languages.forEachIndexed { index, language ->
          val previousLocale = Locale.getDefault()
          try {
            // Compose resources and DateFormat use the JVM default locale. Change it only for
            // this synchronous render, then restore it before another export or UI work runs.
            Locale.setDefault(language.locale)
            _progress.value = ExportProgress(true, 0f, index, languages.size, language.code, language.nativeLabel)
            val localizedData = captureLocalizedData(data)
            _progress.value = ExportProgress(true, 0.05f, index, languages.size, language.code, language.nativeLabel)
            val imageHeight = calculateImageHeight(localizedData)
            val image = renderExportImage(localizedData, imageHeight) { fraction ->
              _progress.value = ExportProgress(true, fraction, index, languages.size, language.code, language.nativeLabel)
            }
            _progress.value = ExportProgress(true, 0.88f, index, languages.size, language.code, language.nativeLabel)

        val config = RFConfig.state.value
        val exportDir = if (config.lastSessionExportDir.isNotBlank()) {
          Paths.get(config.lastSessionExportDir)
        } else {
          val documentsDir = getDocumentsDirectory() ?: return@withContext null
          val now = Date()
          val year  = SimpleDateFormat("yyyy", Locale.US).format(now)
          val month = SimpleDateFormat("MM",   Locale.US).format(now)
          Paths.get(documentsDir, "RFExports", year, month)
        }
        Files.createDirectories(exportDir)
            val outputFile = exportDir.resolve("${safeFileName(data.sessionTitle)}_${language.code}.png").toFile()

            try {
              ImageIO.write(image, "png", outputFile)
            } finally {
              image.flush()
            }
            if (firstOutput == null) firstOutput = outputFile
            _progress.value = ExportProgress(true, 1f, index + 1, languages.size, language.code, language.nativeLabel)
          } catch (e: Exception) {
            Log.error("ImageExport", "Failed to export ${language.code} PNG: ${e.message}")
          } finally {
            Locale.setDefault(previousLocale)
          }
        }
        firstOutput
      } finally {
        _progress.value = ExportProgress()
        clearImageCaches()
      }
    }
  }

  private suspend fun renderExportImage(data: ExportData, imageHeight: Int, progress: (Float) -> Unit): BufferedImage {
    val renderWidth = IMAGE_WIDTH * EXPORT_RENDER_SCALE
    val renderHeight = imageHeight * EXPORT_RENDER_SCALE
    val renderedImage = BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB)
    try {
      val g2d = renderedImage.createGraphics()
      try {
        applyHighQualityRenderingHints(g2d)
        g2d.scale(EXPORT_RENDER_SCALE.toDouble(), EXPORT_RENDER_SCALE.toDouble())
        drawWallpaperBackground(g2d, IMAGE_WIDTH, imageHeight)
        drawMasonryLayout(g2d, data)
      } finally {
        g2d.dispose()
      }
      progress(0.72f)
      return downsampleImage(renderedImage, IMAGE_WIDTH, imageHeight)
    } finally {
      renderedImage.flush()
    }
  }

  private suspend fun captureLocalizedData(data: ExportData): ExportData {
    return data.copy(
      sessionDate = DateFormat.getDateInstance(DateFormat.SHORT).format(Date()),
      battleSummaryTitle = getString(Res.string.export_title_battle_summary),
      noDataText = getString(Res.string.export_no_data),
      exportHeaderOn = getString(Res.string.export_header_on),
      exportHeaderOff = getString(Res.string.export_header_off),
      exportHeaderOdeLabel = getString(Res.string.export_header_ode_label),
      exportHeaderPveLabel = getString(Res.string.export_header_pve_label),
      exportHeaderKillsLabel = getString(Res.string.export_header_kills_label),
      exportHeaderMostDamage = getString(Res.string.export_header_most_damage),
      exportHeaderKillingBlow = getString(Res.string.export_header_killing_blow),
      combatDamageTitle = getString(if (data.allowPvE) Res.string.export_combat_pve_damage else Res.string.export_combat_pvp_damage),
      combatHealsTitle = getString(if (data.allowPvE) Res.string.export_combat_pve_heals else Res.string.export_combat_pvp_heals),
      combatCCTitle = getString(if (data.allowPvE) Res.string.export_combat_pve_cc else Res.string.export_combat_pvp_cc),
    )
  }

  private fun safeFileName(value: String): String = value
    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    .trim()
    .ifBlank { "session" }

  /*
   * So the reason we downsample the image from a higher resolution is because of the icons and fonts being pixelated
   * if we render at the target resolution. The first iteration of the PNG export was very pixelated and this ended-up
   * being the solution.
   */
  private fun downsampleImage(source: BufferedImage, width: Int, height: Int): BufferedImage {
    val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val sg = scaled.createGraphics()
    // Bilinear is substantially cheaper than bicubic for this large full-image pass,
    // while the 2x render still preserves smooth text, icons, and chart edges.
    sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    sg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
    sg.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED)
    sg.drawImage(source, 0, 0, width, height, null)
    sg.dispose()
    return scaled
  }

  /**
   * Puts my space wallpaper behind the exports just so that way it's not an empty / black surface.
   * Remember, we're trying to recreate the look of a UI screenshot like all the posts on Discord except
   * 100% automated / programmatically.
   */
  private fun drawWallpaperBackground(g2d: Graphics2D, width: Int, height: Int) {
    val config = RFConfig.state.value
    val fallbackUri = Res.getUri("drawable/reoky_wallpaper.png")
    val wallpaperKey = when (config.exportBackgroundSelection) {
      "CUSTOM" -> "CUSTOM:${config.exportCustomBackgroundPath}"
      else -> config.exportBackgroundSelection
    }
    val wallpaper = wallpaperCache.getOrPut(wallpaperKey) {
      try {
        when (config.exportBackgroundSelection) {
          "SOLID_COLOR" -> null
          "CUSTOM" -> {
            val file = File(config.exportCustomBackgroundPath)
            if (!file.isFile) throw IllegalStateException("Custom background is missing")
            ImageIO.read(file) ?: throw IllegalStateException("Custom background is unreadable")
          }
          "SPAGUETTI" -> ImageIO.read(URI(Res.getUri("drawable/spaguetti_wallpaper.png")).toURL())
          "SPACEA" -> ImageIO.read(URI(Res.getUri("drawable/spacea_wallpaper.png")).toURL())
          "BROOKLYYN" -> ImageIO.read(URI(Res.getUri("drawable/brooklyyn_wallpaper.png")).toURL())
          else -> ImageIO.read(URI(fallbackUri).toURL())
        }
      } catch (_: Exception) {
        ImageIO.read(URI(fallbackUri).toURL())
      }
    }

    g2d.color = if (config.exportBackgroundSelection == "SOLID_COLOR") {
      Color(config.exportBackgroundColor, true)
    } else {
      toAwtColor(RFColors.CardBackground)
    }
    g2d.fillRect(0, 0, width, height)

    if (wallpaper != null) {
      val scale = maxOf(width.toDouble() / wallpaper.width, height.toDouble() / wallpaper.height)
      val drawWidth = (wallpaper.width * scale).toInt()
      val drawHeight = (wallpaper.height * scale).toInt()
      val drawX = (width - drawWidth) / 2
      val drawY = (height - drawHeight) / 2
      g2d.drawImage(wallpaper, drawX, drawY, drawWidth, drawHeight, null)
    }

    val dimness = config.exportBackgroundDimness.coerceIn(0f, MAX_EXPORT_BACKGROUND_DIMNESS)
    if (dimness > 0f) {
      g2d.color = Color(0, 0, 0, (dimness * 255f).toInt())
      g2d.fillRect(0, 0, width, height)
    }
  }

  private suspend fun calculateImageHeight(data: ExportData): Int {
    val columnHeights = computeColumnHeights(data)
    return columnHeights.max() + 30
  }

  /**
   * The title card now spans the full image width, so all three columns must start
   * below it.  We track an equal [titleOffset] for columns 1 and 2, while column 0
   * additionally holds the pie-chart and combat blocks.
   */
  private suspend fun computeColumnHeights(data: ExportData): List<Int> {
    val superColWidth = (IMAGE_WIDTH - SUPER_COL_GAP * (SUPER_COLUMN_COUNT - 1)) / SUPER_COLUMN_COUNT

    val pieChartBlock = makePieChartBlock(data, superColWidth)
    val combatBlock   = makeCombatBlock(data, superColWidth)

    val titleOffset    = TITLE_CARD_HEIGHT + 5          // column 0 starts here
    val col0BaseHeight = titleOffset + pieChartBlock.height + 5 + combatBlock.height

    val tripletBlocks = getAllCategoryBlocks(data, superColWidth)
    // Column 0 starts below title; remaining columns start at top (no title offset)
    val colHeights    = MutableList(SUPER_COLUMN_COUNT) { 0 }
    colHeights[0] = col0BaseHeight

    tripletBlocks.forEach { block ->
      val shortestCol = colHeights.indices.minByOrNull { colHeights[it] } ?: 0
      colHeights[shortestCol] += block.height + 5
    }

    return colHeights
  }

  data class Block(val title: String, val height: Int, val draw: (Graphics2D, Int, Int, Int) -> Unit)

  data class TripletBlock(val title: String, val blocks: List<Block>, val height: Int, val draw: (Graphics2D, Int, Int, Int) -> Unit)

  sealed class ColumnData {
    data class SpellData(val spells: List<SpellDamage>) : ColumnData()
    data class ItemData(val items: List<ItemUsage>) : ColumnData()
    data class BuildData(val builds: Map<String, Int>) : ColumnData()
    data class CardData(
      val cards: List<PlayerCard>,
      val getValue: (PlayerCard) -> String,
      val valueColor: Color,
      val getColor: ((PlayerCard) -> Color)? = null, // Optional dynamic color per card
      val showIcons: Boolean = true // Skip skill-tree icons for sections where they waste space
    ) : ColumnData()
  }


  private fun makeCombatBlock(data: ExportData, colWidth: Int): Block {
    val combatRows = maxOf(data.topDamage.size, data.topHeals.size, data.topCC.size).coerceAtLeast(1)
    val combatH = SECTION_HEADER_HEIGHT + (combatRows * ROW_HEIGHT) + CARD_PADDING * 2

    return Block("Combat", combatH) { g2d, x, y, w ->
      drawCardBackgroundTransparent(g2d, x, y, w, combatH)
      val subColW = w / 3

      val icon = if (data.allowPvE) "\u2694" else "\uD83D\uDD25"

      drawSectionHeader(g2d, data.combatDamageTitle, x,               y, subColW, toAwtColor(RFColors.dpsOrange), icon)
      drawSectionHeader(g2d, data.combatHealsTitle,  x + subColW,     y, subColW, toAwtColor(RFColors.healsGreen), "\uD83D\uDC89")
      drawSectionHeader(g2d, data.combatCCTitle,     x + subColW * 2, y, subColW, toAwtColor(RFColors.ccCyan), "\uD83D\uDEE1")

      var rowY = y + SECTION_HEADER_HEIGHT
      val maxRows = maxOf(data.topDamage.size, data.topHeals.size, data.topCC.size).coerceAtLeast(1)
      for (i in 0 until maxRows) {
        if (i < data.topDamage.size) {
          drawRankingRow(g2d, i, data.topDamage[i], data.topDamage[i].sessionDamageTotal.humanReadableAbbreviation(), toAwtColor(RFColors.dpsOrange), x, rowY, subColW)
        }
        if (i < data.topHeals.size) {
          drawRankingRow(g2d, i, data.topHeals[i], data.topHeals[i].sessionHealTotal.humanReadableAbbreviation(), toAwtColor(RFColors.healsGreen), x + subColW, rowY, subColW)
        }
        if (i < data.topCC.size) {
          drawRankingRow(g2d, i, data.topCC[i], data.topCC[i].sessionCCTotal.toString(), toAwtColor(RFColors.ccCyan), x + subColW * 2, rowY, subColW)
        }
        rowY += ROW_HEIGHT
      }
    }
  }

  private const val MAX_ROWS = 25

  private fun calculateHeight(data: ColumnData): Int {
    val numRows = when (data) {
      is ColumnData.SpellData -> data.spells.size
      is ColumnData.ItemData  -> data.items.size
      is ColumnData.BuildData -> data.builds.size
      is ColumnData.CardData  -> data.cards.size
    }
    val rows = numRows.coerceAtLeast(5).coerceAtMost(MAX_ROWS)
    return (SECTION_HEADER_HEIGHT + (rows * ROW_HEIGHT) + CARD_PADDING * 2).coerceAtLeast(CATEGORY_MIN_HEIGHT)
  }

  private fun makePieChartBlock(data: ExportData, superColWidth: Int): Block {
    val chartH = CHART_HEIGHT + CARD_PADDING * 2

    return Block("Charts", chartH) { g2d, x, y, w ->
      drawCardBackgroundTransparent(g2d, x, y, w, chartH)

      val chartSpacing = w / 3
      val chartRadius  = 45
      val chartY       = y + CARD_PADDING + 62

      drawPieChart(g2d, "Silences",   data.factionSilenceData,  chartSpacing / 2, chartY, chartRadius, 0)
      drawPieChart(g2d, "Charms",     data.factionCharmData,    chartSpacing / 2, chartY, chartRadius, chartSpacing)
      drawPieChart(g2d, "Distresses", data.factionDistressData, chartSpacing / 2, chartY, chartRadius, chartSpacing * 2)
    }
  }

  /**
   * Draws the masonry layout.
   *
   * The title now spans the full image width. It used to just be the top-left brick of the masonry. Overall, going for
   * a dynamic layout like masonry because each battle is going to be different have different-sized cards for each section.
   */
  private suspend fun drawMasonryLayout(g2d: Graphics2D, data: ExportData) {
    val superColWidth = (IMAGE_WIDTH - SUPER_COL_GAP * (SUPER_COLUMN_COUNT - 1)) / SUPER_COLUMN_COUNT

    // Title card fits within column 0 (above pie charts)
    val titleH      = drawTitleCard(g2d, data, COLUMN_GAP, 10, superColWidth)
    val titleBottom = 10 + titleH + 5

    // Column 0 starts below the title; remaining columns start at the top
    val columnY       = MutableList(SUPER_COLUMN_COUNT) { 10 }
    columnY[0] = titleBottom
    val columnHeights = MutableList(SUPER_COLUMN_COUNT) { 0 }
    columnHeights[0] = titleH

    val pieBlock = makePieChartBlock(data, superColWidth)
    pieBlock.draw(g2d, COLUMN_GAP, columnY[0], superColWidth)
    columnY[0]       += pieBlock.height + 5
    columnHeights[0] += pieBlock.height + 5

    val combatBlock = makeCombatBlock(data, superColWidth)
    combatBlock.draw(g2d, COLUMN_GAP, columnY[0], superColWidth)
    columnY[0]       += combatBlock.height + 5
    columnHeights[0] += combatBlock.height + 5

    val tripletBlocks = getAllCategoryBlocks(data, superColWidth)
    tripletBlocks.forEach { block ->
      val shortestCol = columnHeights.indices.minByOrNull { columnHeights[it] } ?: 0
      val xPos        = COLUMN_GAP + shortestCol * (superColWidth + SUPER_COL_GAP)
      val drawWidth   = if (shortestCol == SUPER_COLUMN_COUNT - 1) IMAGE_WIDTH - xPos - COLUMN_GAP else superColWidth
      block.draw(g2d, xPos, columnY[shortestCol], drawWidth)
      columnY[shortestCol]       += block.height + 5
      columnHeights[shortestCol] += block.height + 5
    }
  }

  /**
   * Title card — now drawn full-width so there is no wasted space on the right.
   * The session metadata is sourced from ExportData which was salted with
   */
  private fun loadLogo(): BufferedImage? {
    return logoCache ?: run {
      try {
        val uri = Res.getUri("drawable/raidframer.ico")
        val bytes = URI(uri).toURL().openStream().use { it.readBytes() }
        val data = Data.makeFromBytes(bytes)
        val codec = Codec.makeFromData(data)
        val imageInfo = ImageInfo.makeN32Premul(codec.width, codec.height)
        val bitmap = Bitmap()
        bitmap.allocPixels(imageInfo)
        codec.readPixels(bitmap, 0, 0)

        val awtImage = BufferedImage(codec.width, codec.height, BufferedImage.TYPE_INT_ARGB)
        for (py in 0 until codec.height) {
          for (px in 0 until codec.width) {
            awtImage.setRGB(px, py, bitmap.getColor(px, py))
          }
        }
        logoCache = awtImage
      } catch (_: Exception) { }
      logoCache
    }
  }

  private fun drawTitleCard(g2d: Graphics2D, data: ExportData, x: Int, y: Int, width: Int): Int {
    val titleH = TITLE_CARD_HEIGHT

    drawCardBackgroundTransparent(g2d, x, y, width, titleH)

    val logoSize = 64
    val logo = loadLogo()
    var textStartX = x + CARD_PADDING + 4
    if (logo != null) {
      val logoX = x + CARD_PADDING + (logoSize / 2)
      val logoY = y + (titleH - logoSize) / 2
      g2d.drawImage(logo, logoX - logoSize / 2, logoY, logoSize, logoSize, null)
      textStartX = x + CARD_PADDING + logoSize + 12
    }

    val titleFont    = createFont(Font.BOLD,  20f)
    val subtitleFont = createFont(Font.PLAIN, 12f)

    g2d.color = TEXT_PRIMARY
    g2d.font  = titleFont
    g2d.drawString("${AppGlobals.APP_NAME} - ${data.battleSummaryTitle}", textStartX, y + 32)

    val durationStr = formatDuration(data.sessionDurationMs)
    val odeLabel = if (data.allowOdeToRecoveryCountAsHeals) data.exportHeaderOn else data.exportHeaderOff
    val pveLabel = if (data.allowPvE) data.exportHeaderOn else data.exportHeaderOff
    // TODO: uncomment when kill counter mode selection is implemented
    // val killModeLabel = when (data.killCounterMode) {
    //   "KILLING_BLOW" -> data.exportHeaderKillingBlow
    //   else -> data.exportHeaderMostDamage
    // }
    g2d.font  = subtitleFont
    g2d.color = toAwtColor(RFColors.TextSecondary)
    g2d.drawString(
      "${data.sessionTitle}  |  ${data.sessionDate}  |  ${AppGlobals.APP_VERSION}  |  $durationStr  |  ${data.exportHeaderOdeLabel}: $odeLabel  |  ${data.exportHeaderPveLabel}: $pveLabel",
      textStartX, y + 56
    )

    g2d.font  = createFont(Font.PLAIN, 10f)
    val faded = toAwtColor(RFColors.TextSecondary)
    g2d.color = Color(faded.red, faded.green, faded.blue, 120)
    g2d.drawString("${AppGlobals.APP_NAME} v${AppGlobals.APP_VERSION}", textStartX, y + 78)

    return titleH
  }

  private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours   = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "${hours}h ${minutes}m"
    else if (minutes > 0) "${minutes}m ${seconds}s"
    else "${seconds}s"
  }

  private fun drawCardBackground(g2d: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
    g2d.color = CARD_BACKGROUND
    g2d.fillRect(x, y, width, height)
    g2d.color = BORDER_COLOR
    g2d.drawRect(x, y, width, height)
  }

  private fun drawCardBackgroundTransparent(g2d: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
    g2d.color = CARD_BACKGROUND_TRANSPARENT
    g2d.fillRect(x, y, width, height)
    g2d.color = BORDER_COLOR_TRANSPARENT
    g2d.drawRect(x, y, width, height)
  }

  private fun drawSectionHeader(g2d: Graphics2D, title: String, x: Int, y: Int, width: Int, color: Color, icon: String = "") {
    val textFont = createFittingHeaderFont(g2d, title, width, icon)
    g2d.font = textFont
    g2d.color = color
    val textWidth = g2d.fontMetrics.stringWidth(title)

    var totalWidth = textWidth
    val iconFont = createEmojiFont(Font.BOLD, 13f)
    val iconWidth = if (icon.isNotEmpty()) {
      g2d.font = iconFont
      g2d.fontMetrics.stringWidth(icon)
    } else 0
    totalWidth += iconWidth * 2
    if (icon.isNotEmpty()) totalWidth += g2d.fontMetrics.stringWidth("  ")

    val startX = (x + (width - totalWidth) / 2).coerceAtLeast(x + 4)
    var drawX = startX

    if (icon.isNotEmpty()) {
      g2d.font = iconFont
      g2d.drawString(icon, drawX, y + 18)
      drawX += g2d.fontMetrics.stringWidth(icon)
      val spaceW = g2d.fontMetrics.stringWidth(" ")
      drawX += spaceW
    }

    g2d.font = textFont
    g2d.drawString(title, drawX, y + 18)
    drawX += textWidth

    if (icon.isNotEmpty()) {
      val spaceW = g2d.fontMetrics.stringWidth(" ")
      drawX += spaceW
      g2d.font = iconFont
      g2d.drawString(icon, drawX, y + 18)
    }
  }

  /**
   * Truncates [text] with an ellipsis if it exceeds [maxWidth] pixels under the
   * current Graphics2D font.
   */
  private fun fitText(text: String, g2d: Graphics2D, maxWidth: Int): String {
    if (maxWidth <= 0) return ""
    val fm = g2d.fontMetrics
    if (fm.stringWidth(text) <= maxWidth) return text
    val ellipsis = "..."
    var fitted = text
    while (fitted.isNotEmpty() && fm.stringWidth("$fitted$ellipsis") > maxWidth) {
      fitted = fitted.dropLast(1)
    }
    return if (fitted.isEmpty()) "" else "$fitted$ellipsis"
  }

  /**
   * Draws a single player ranking row with:
   *  - rank number
   *  - three skill-tree icons (from the player's current build/spec)
   *  - player name (truncated if needed to avoid colliding with the value)
   *  - faction-status dot to the right of the name
   *  - right-aligned stat value
   */
  private fun drawRankingRow(
    g2d: Graphics2D,
    index: Int,
    card: PlayerCard,
    valueText: String,
    valueColor: Color,
    xOffset: Int,
    y: Int,
    width: Int,
    showIcons: Boolean = true
  ) {
    val rowFont   = createFont(Font.PLAIN, 11f)
    val valueFont = createFont(Font.BOLD,  11f)

    val iconSize = 14
    val iconGap  = 2

    // rank
    g2d.font  = rowFont
    g2d.color = TEXT_PRIMARY
    g2d.drawString("${index + 1}.", xOffset + 8, y + 16)

    // skill-tree icons (the SVGs being rendered)
    var iconX  = xOffset + 28
    if (showIcons) {
      val spec   = SpecType.fromName(card.currentBuild)
      val iconY  = y + (ROW_HEIGHT - iconSize) / 2

      val trees: List<SkillTreeType?> = if (spec != null && spec != SpecType.UNKNOWN) {
        spec.trees.sortedByDisplayOrder().take(3)
      } else {
        listOf(null, null, null)
      }
      trees.forEach { treeType ->
        val img = loadSkillTreeImage(treeType, iconSize * SVG_ICON_RENDER_SCALE)
        if (img != null) g2d.drawImage(img, iconX, iconY, iconSize, iconSize, null)
        iconX += iconSize + iconGap
      }
    }

    val nameStartX = iconX + 2   // small gap after icons

    // value amount (always account for trailing asterisk space)
    val asteriskBounds = g2d.fontMetrics.getStringBounds("*", g2d)
    g2d.font  = valueFont
    g2d.color = valueColor
    val valueBounds = g2d.fontMetrics.getStringBounds(valueText, g2d)
    val valueX      = xOffset + width - valueBounds.width.toInt() - asteriskBounds.width.toInt() - 8
    g2d.drawString(valueText, valueX, y + 16)

    // asterisk if own character (draw visible or invisible to reserve space)
    if (card.name == RFConfig.state.value.playerName) {
      g2d.drawString("*", valueX + valueBounds.width.toInt(), y + 16)
    } else {
      val prevColor = g2d.color
      g2d.color = Color(0, true)
      g2d.drawString("*", valueX + valueBounds.width.toInt(), y + 16)
      g2d.color = prevColor
    }

    // player's character name
    g2d.font  = rowFont
    g2d.color = TEXT_PRIMARY
    val maxNameWidth = (valueX - nameStartX - 10).coerceAtLeast(0)
    val fittedName   = fitText(card.name, g2d, maxNameWidth)
    g2d.drawString(fittedName, nameStartX, y + 16)

    // faction status dots
    val nameWidth = g2d.fontMetrics.stringWidth(fittedName)
    val dotX      = nameStartX + nameWidth + 3
    val dotSize   = 6
    val dotY      = y + (ROW_HEIGHT - dotSize) / 2

    if (dotX + dotSize < valueX - 4) {
      val playerFaction = Faction.fromString(RFConfig.state.value.playerFaction)
      val cardFaction   = Faction.fromString(card.lastKnownFaction)
      val dotComposeColor = playerFaction.getFactionHighlightColor(cardFaction)
      val dotAwtColor     = toAwtColor(dotComposeColor)
      if (dotAwtColor.alpha > 0) {
        val prevHint = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.color = dotAwtColor
        g2d.fillOval(dotX, dotY, dotSize, dotSize)
        if (prevHint != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevHint)
      }
    }
  }

  private fun drawPieChart(g2d: Graphics2D, title: String, factionData: Map<String, Float>, centerX: Int, centerY: Int, radius: Int, xOffset: Int) {
    val titleFont = createFont(Font.BOLD,  11f)
    val labelFont = createFont(Font.PLAIN, 10f)

    g2d.font  = titleFont
    g2d.color = TEXT_PRIMARY
    g2d.drawString(title, xOffset + centerX - g2d.fontMetrics.stringWidth(title) / 2, centerY - radius - 8)

    val allFactions  = listOf("Haranya", "Nuia", "Pirate")
    val displayData  = allFactions.associateWith { faction -> factionData[faction] ?: 0f }
    val total        = displayData.values.sum()
    val pieColors    = listOf(HARANYA_COLOR, NUIA_COLOR, PIRATE_COLOR)

    if (total == 0f) {
      g2d.color = Color(50, 50, 60)
      g2d.fillArc(xOffset + centerX - radius, centerY - radius, radius * 2, radius * 2, 0, 360)

      g2d.font  = labelFont
      g2d.color = toAwtColor(RFColors.TextSecondary)
      g2d.drawString("0", xOffset + centerX - 5, centerY + 4)

      var legendY = centerY + radius + 12
      displayData.entries.forEachIndexed { i, (label, _) ->
        g2d.color = pieColors.getOrNull(i) ?: pieColors.last()
        g2d.fillRect(xOffset + centerX - radius, legendY, 8, 8)
        g2d.color = toAwtColor(RFColors.TextSecondary)
        g2d.drawString("$label: 0", xOffset + centerX - radius + 12, legendY + 8)
        legendY += 14
      }
      return
    }

    var startAngle = 0.0
    displayData.entries.forEachIndexed { i, (_, value) ->
      val angle = ((value / total) * 360).toInt()
      if (angle > 0) {
        g2d.color = pieColors.getOrNull(i) ?: pieColors.last()
        g2d.fillArc(xOffset + centerX - radius, centerY - radius, radius * 2, radius * 2, startAngle.toInt(), angle)
        startAngle += angle
      }
    }

    var legendY = centerY + radius + 12
    g2d.font = labelFont
    displayData.entries.forEachIndexed { i, (label, value) ->
      g2d.color = pieColors.getOrNull(i) ?: pieColors.last()
      g2d.fillRect(xOffset + centerX - radius, legendY, 8, 8)
      g2d.color = TEXT_PRIMARY
      g2d.drawString("$label: ${value.toInt()}", xOffset + centerX - radius + 12, legendY + 8)
      legendY += 14
    }
  }


  private suspend fun getAllCategoryBlocks(data: ExportData, superColWidth: Int): List<TripletBlock> {
    val tripletBlocks  = mutableListOf<TripletBlock>()
    val subColWidth    = (superColWidth - COLUMN_GAP * 2) / 3
    val qualityLabels = LifeMendQuality.entries.associateWith { getString(it.labelRes) }

    val makeTriplet: (List<Triple<String, String, ColumnData>>) -> TripletBlock = { columns ->
      val maxBlockHeight = columns.maxOfOrNull { calculateHeight(it.third) } ?: CATEGORY_MIN_HEIGHT
      val colCount = columns.size.coerceAtLeast(1)
      TripletBlock("", emptyList(), maxBlockHeight) { g2d, x, y, w ->
        val actualSubColWidth = if (w < superColWidth) (w - COLUMN_GAP * (colCount - 1)) / colCount else subColWidth
        val actualAvailW      = actualSubColWidth - CARD_PADDING * 2
        columns.forEachIndexed { index, (title, icon, colData) ->
          val xPos = x + index * (actualSubColWidth + COLUMN_GAP)
          drawCardBackgroundTransparent(g2d, xPos, y, actualAvailW + CARD_PADDING * 2, maxBlockHeight)
          drawSectionHeader(g2d, title, xPos + CARD_PADDING, y, actualAvailW, TEXT_PRIMARY, icon)

          val labelFont = createFont(Font.PLAIN, 10f)
          val valueFont = createFont(Font.BOLD,  10f)
          var rowY = y + SECTION_HEADER_HEIGHT

          when (val d = colData) {
            is ColumnData.SpellData -> {
              if (d.spells.isEmpty()) {
                g2d.font  = labelFont
                g2d.color = toAwtColor(RFColors.TextSecondary)
                g2d.drawString(data.noDataText, xPos + CARD_PADDING + 8, rowY + 14)
              } else {
                val spellColor = when (index) {
                  0 -> HARANYA_COLOR
                  1 -> NUIA_COLOR
                  2 -> PIRATE_COLOR
                  else -> toAwtColor(RFColors.dpsOrange)
                }
                for (i in 0 until minOf(d.spells.size, MAX_ROWS)) {
                  g2d.font  = labelFont
                  g2d.color = TEXT_PRIMARY
                  g2d.drawString("${i + 1}. ${d.spells[i].spell}", xPos + CARD_PADDING + 8, rowY + 14)
                  g2d.font  = valueFont
                  g2d.color = spellColor
                  val valStr = d.spells[i].total.toLong().humanReadableAbbreviation()
                  val bounds = g2d.fontMetrics.getStringBounds(valStr, g2d)
                  g2d.drawString(valStr, xPos + CARD_PADDING + actualAvailW - bounds.width.toInt() - 8, rowY + 14)
                  rowY += ROW_HEIGHT
                }
              }
            }
            is ColumnData.ItemData -> {
              if (d.items.isEmpty()) {
                g2d.font  = labelFont
                g2d.color = toAwtColor(RFColors.TextSecondary)
                g2d.drawString(data.noDataText, xPos + CARD_PADDING + 8, rowY + 14)
              } else {
                val itemColor = when (index) {
                  0 -> HARANYA_COLOR
                  1 -> NUIA_COLOR
                  2 -> PIRATE_COLOR
                  else -> toAwtColor(RFColors.TextSecondary)
                }
                for (i in 0 until minOf(d.items.size, MAX_ROWS)) {
                  g2d.font  = labelFont
                  g2d.color = TEXT_PRIMARY
                  g2d.drawString("${i + 1}. ${d.items[i].itemName}", xPos + CARD_PADDING + 8, rowY + 14)
                  g2d.font  = valueFont
                  g2d.color = itemColor
                  val valStr = d.items[i].count.toLong().humanReadableAbbreviation()
                  val bounds = g2d.fontMetrics.getStringBounds(valStr, g2d)
                  g2d.drawString(valStr, xPos + CARD_PADDING + actualAvailW - bounds.width.toInt() - 8, rowY + 14)
                  rowY += ROW_HEIGHT
                }
              }
            }
            is ColumnData.BuildData -> {
              if (d.builds.isEmpty()) {
                g2d.font  = labelFont
                g2d.color = toAwtColor(RFColors.TextSecondary)
                g2d.drawString(data.noDataText, xPos + CARD_PADDING + 8, rowY + 14)
              } else {
                val buildColor = when (index) {
                  0 -> HARANYA_COLOR
                  1 -> NUIA_COLOR
                  2 -> PIRATE_COLOR
                  else -> toAwtColor(RFColors.TextSecondary)
                }
                d.builds.entries.sortedByDescending { it.value }.take(MAX_ROWS).forEachIndexed { idx, (label, count) ->
                  val displayLabel = data.buildDisplayNames[label] ?: label
                  g2d.font  = labelFont
                  g2d.color = TEXT_PRIMARY
                  g2d.drawString("${idx + 1}. $displayLabel", xPos + CARD_PADDING + 8, rowY + 14)
                  g2d.font  = valueFont
                  g2d.color = buildColor
                  val valStr = count.toLong().humanReadableAbbreviation()
                  val bounds = g2d.fontMetrics.getStringBounds(valStr, g2d)
                  g2d.drawString(valStr, xPos + CARD_PADDING + actualAvailW - bounds.width.toInt() - 8, rowY + 14)
                  rowY += ROW_HEIGHT
                }
              }
            }
            is ColumnData.CardData -> {
              if (d.cards.isEmpty()) {
                g2d.font  = labelFont
                g2d.color = toAwtColor(RFColors.TextSecondary)
                g2d.drawString(data.noDataText, xPos + CARD_PADDING + 8, rowY + 14)
              } else {
                // Reuse the shared drawRankingRow so icons & faction dots appear
                // consistently in every player-facing table throughout the export.
                for (i in 0 until minOf(d.cards.size, MAX_ROWS)) {
                  val cardColor = d.getColor?.invoke(d.cards[i]) ?: d.valueColor
                  drawRankingRow(
                    g2d, i, d.cards[i],
                    d.getValue(d.cards[i]), cardColor,
                    xPos + CARD_PADDING, rowY,
                    actualAvailW,
                    showIcons = d.showIcons
                  )
                  rowY += ROW_HEIGHT
                }
              }
            }
          }
        }
      }
    }

    // 1. Silences, Charms, Distresses
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_silences),  "\uD83D\uDD07", ColumnData.CardData(data.topSilences,  { it.sessionSilenceTotal.toString()  }, SILENCE_COLOR)),
      Triple(getString(Res.string.summary_top_charms),    "\uD83D\uDC96", ColumnData.CardData(data.topCharms,    { it.sessionCharmTotal.toString()    }, CHARM_COLOR)),
      Triple(getString(Res.string.summary_top_distresses), "\uD83D\uDE24", ColumnData.CardData(data.topDistresses, { it.sessionDistressTotal.toString() }, DISTRESS_COLOR)),
    )))

    // 2. Top kills by faction
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_kills_haranya), "\u2694", ColumnData.CardData(data.topKillsHaranya, { it.sessionKillTotal.toString() }, KILLS_HARANYA_COLOR)),
      Triple(getString(Res.string.summary_top_kills_nuia),    "\u2694", ColumnData.CardData(data.topKillsNuia,    { it.sessionKillTotal.toString() }, KILLS_NUIA_COLOR)),
      Triple(getString(Res.string.summary_top_kills_pirate),  "\u2694", ColumnData.CardData(data.topKillsPirate,  { it.sessionKillTotal.toString() }, KILLS_PIRATE_COLOR)),
    )))

    // 3. Heal Ratio / damage taken / heals received
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_heal_ratio), "\uD83E\uDE78", ColumnData.CardData(
        data.topHealRatio.map { it.first },
        { card ->
          val ratio = data.topHealRatio.find { it.first.name == card.name }?.second ?: 0f
          "${(ratio * 100).toInt()}%"
        },
        TEXT_PRIMARY,
        { card ->
          val ratio = data.topHealRatio.find { it.first.name == card.name }?.second ?: 0f
          healRatioColor(ratio)
        }
      )),
      Triple(getString(Res.string.summary_top_damage_taken), "\uD83D\uDD25", ColumnData.CardData(data.topDamageTaken,   { it.sessionDamageTakenTotal.toLong().humanReadableAbbreviation()   }, toAwtColor(RFColors.dpsOrange))),
      Triple(getString(Res.string.summary_top_heals_received), "\uD83D\uDC89", ColumnData.CardData(data.topHealsReceived, { it.sessionHealsReceivedTotal.toLong().humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen))),
    )))

    // 4. Spell damage breakdown
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_haranya_spells_damage), "\uD83D\uDD25", ColumnData.SpellData(data.topDamageSpellsHaranya)),
      Triple(getString(Res.string.summary_top_nuia_spells_damage),    "\uD83D\uDD25", ColumnData.SpellData(data.topDamageSpellsNuia)),
      Triple(getString(Res.string.summary_top_pirate_spells_damage),  "\uD83D\uDD25", ColumnData.SpellData(data.topDamageSpellsPirate)),
    )))

    // 5. Top performance
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_haranya_performance), "\uD83C\uDFC6", ColumnData.CardData(data.topPerformanceHaranya, { it.pvpPerformancePoints().toString() }, HARANYA_COLOR)),
      Triple(getString(Res.string.summary_top_nuia_performance),    "\uD83C\uDFC6", ColumnData.CardData(data.topPerformanceNuia,    { it.pvpPerformancePoints().toString() }, NUIA_COLOR)),
      Triple(getString(Res.string.summary_top_pirate_performance),  "\uD83C\uDFC6", ColumnData.CardData(data.topPerformancePirate,  { it.pvpPerformancePoints().toString() }, PIRATE_COLOR)),
    )))

    // 6. Debuffs / Songs / Buffs
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_debuffs), "\uD83D\uDD25", ColumnData.CardData(data.topDebuffs, { it.sessionDebuffTotal.toString() }, HARANYA_COLOR)),
      Triple(getString(Res.string.summary_top_songs),   "\u2764", ColumnData.CardData(data.topSongs,   { it.sessionSongsTotal.toString()  }, NUIA_COLOR)),
      Triple(getString(Res.string.summary_top_buffs),   "\u26A1", ColumnData.CardData(data.topBuffs,   { it.sessionBuffTotal.toString()   }, PIRATE_COLOR)),
    )))

    // 7. Dances: Sac Dances, Deep Tranquility, Deepend Debuff
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_sac_dances), "\u2665", ColumnData.CardData(data.topSacDances, { it.sessionSacDanceTotal.toString() }, SAC_DANCE_COLOR)),
      Triple(getString(Res.string.summary_top_deep_tranquility), "\u2602", ColumnData.CardData(data.topDeepTranquility, { it.sessionDeepTranquilityTotal.toString() }, DEEP_TRANQUILITY_COLOR)),
      Triple(getString(Res.string.summary_top_deepend_debuff), "\u2B07", ColumnData.CardData(data.topDeependDebuff, { it.sessionDeependDebuffTotal.toString() }, DEEPEND_DEBUFF_COLOR)),
    )))

    // 8. Shield Strip / Weapon Disables / Potion Disables
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_shield_strip), "\u2694", ColumnData.CardData(data.topShieldStrip, { it.sessionShieldStripTotal.toString() }, SHIELD_STRIP_COLOR)),
      Triple(getString(Res.string.summary_top_weapon_disables), "\u2620", ColumnData.CardData(data.topWeaponDisables, { it.sessionWeaponDisablesTotal.toString() }, WEAPON_DISABLES_COLOR)),
      Triple(getString(Res.string.summary_top_potion_disables), "\u2697", ColumnData.CardData(data.topPotionDisables, { it.sessionPotionDisablesTotal.toString() }, POTION_DISABLES_COLOR)),
    )))

    // 9. Absorb, Corrosives, and Crows
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_absorb_lifeforce), "\uD83E\uDE78", ColumnData.CardData(data.topAbsorbLifeforce, { it.sessionAbsorbLifeforceTotal.toString() }, ABSORB_LIFEFORCE_COLOR)),
      Triple(getString(Res.string.summary_top_corrosive_barrage), "\u2622", ColumnData.CardData(data.topCorrosiveBarrage, { it.sessionCorrosiveBarrageTotal.toString() }, CORROSIVE_BARRAGE_COLOR)),
      Triple(getString(Res.string.summary_top_blinded_by_crows), "\uD83E\uDD85", ColumnData.CardData(data.topBlindedByCrows, { it.sessionBlindedByCrowsTotal.toString() }, BLINDED_BY_CROWS_COLOR)),
    )))

    // 10. Provokes, Petrification, Freezes
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_provokes), "\u2757", ColumnData.CardData(data.topProvoked, { it.sessionProvokedTotal.toString() }, PROVOKED_COLOR)),
      Triple(getString(Res.string.summary_top_petrification), "\u26CF", ColumnData.CardData(data.topPetrification, { it.sessionPetrificationTotal.toString() }, PETRIFICATION_COLOR)),
      Triple(getString(Res.string.summary_top_freezes), "\u2744", ColumnData.CardData(data.topFreezes, { it.sessionFreezeTotal.toString() }, FREEZE_COLOR)),
    )))

    // --- Remaining sections ---

    // Ode
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_ode_haranya), "\uD83C\uDFB5", ColumnData.CardData(data.topOdeHaranya, { it.sessionOdeHealsTotal.humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen))),
      Triple(getString(Res.string.summary_top_ode_nuia),    "\uD83C\uDFB5", ColumnData.CardData(data.topOdeNuia,    { it.sessionOdeHealsTotal.humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen))),
      Triple(getString(Res.string.summary_top_ode_pirate),  "\uD83C\uDFB5", ColumnData.CardData(data.topOdePirate,  { it.sessionOdeHealsTotal.humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen))),
    )))

    // Items, Utility, Builds
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_haranya_item_uses), "\uD83D\uDCE6", ColumnData.ItemData(data.topItemUsesHaranya)),
      Triple(getString(Res.string.summary_top_nuia_item_uses),    "\uD83D\uDCE6", ColumnData.ItemData(data.topItemUsesNuia)),
      Triple(getString(Res.string.summary_top_pirate_item_uses),  "\uD83D\uDCE6", ColumnData.ItemData(data.topItemUsesPirate)),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_potion_drinkers), "\uD83E\uDDEA", ColumnData.CardData(data.topPotters,          { it.sessionPotionTotal.toString()    }, POTION_COLOR)),
      Triple(getString(Res.string.summary_top_glider_gamers),   "\u2708", ColumnData.CardData(data.topGliderGamers,     { it.sessionGliderTotal.toString()    }, GLIDER_COLOR)),
      Triple(getString(Res.string.summary_most_item_usages),     "\u2699", ColumnData.CardData(data.topItemSkillCasters, { it.sessionItemSkillTotal.toString() }, ITEM_SKILL_COLOR)),
    )))

    // Special Heals: Mana Barrier, Revive, Life Mends
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_mana_barrier), "\uD83D\uDEE1", ColumnData.CardData(data.topManaBarrier, { it.sessionManaBarrierTotal.toString() }, MANA_BARRIER_COLOR)),
      Triple(getString(Res.string.summary_top_revive), "\u2618", ColumnData.CardData(data.topRevive, { it.sessionReviveTotal.toString() }, REVIVE_COLOR)),
      Triple(getString(Res.string.summary_top_life_mends), "\u2764", ColumnData.CardData(
        data.topLifeMenders,
        { card ->
          "${card.lifeMendTotal} (${card.lifeMendAverage.toLong().humanReadableAbbreviation()} - ${qualityLabels[card.lifeMendQuality]})"
        },
        toAwtColor(RFColors.healsGreen),
        { card -> toAwtColor(card.lifeMendQuality.color) },
        showIcons = false
      )),
    )))

    // CC Debuffs: Trips, Bubbles, Bracings
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_trips), "\u2193", ColumnData.CardData(data.topTrips, { it.sessionTripsTotal.toString() }, TRIPS_COLOR)),
      Triple(getString(Res.string.summary_top_bubbles), "\u25CF", ColumnData.CardData(data.topBubbles, { it.sessionBubblesTotal.toString() }, BUBBLES_COLOR)),
      Triple(getString(Res.string.summary_top_bracings), "\u27A1", ColumnData.CardData(data.topBracings, { it.sessionBracingsTotal.toString() }, BRACINGS_COLOR)),
    )))

    // Glider Debuffs: BD Glider, Crystal Wings, Glider Disables
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_bd_glider), "\u2708", ColumnData.CardData(data.topBdGlider, { it.sessionBdGliderTotal.toString() }, BD_GLIDER_COLOR)),
      Triple(getString(Res.string.summary_top_crystal_wings), "\u2708", ColumnData.CardData(data.topCrystalWings, { it.sessionCrystalWingsTotal.toString() }, CRYSTAL_WINGS_COLOR)),
      Triple(getString(Res.string.summary_top_glider_disables), "\u2708", ColumnData.CardData(data.topGliderDisables, { it.sessionGliderDisablesTotal.toString() }, GLIDER_DISABLES_COLOR)),
    )))

    // Debuffs Continued: Throw Dagger, Stuns, Staggers
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_throw_dagger), "\uD83D\uDDE1", ColumnData.CardData(data.topThrowDagger, { it.sessionThrowDaggerTotal.toString() }, THROW_DAGGER_COLOR)),
      Triple(getString(Res.string.summary_top_stuns), "\u26A0", ColumnData.CardData(data.topStuns, { it.sessionStunsTotal.toString() }, STUNS_COLOR)),
      Triple(getString(Res.string.summary_top_staggers), "\u2195", ColumnData.CardData(data.topStaggers, { it.sessionStaggersTotal.toString() }, STAGGERS_COLOR)),
    )))

    // Special Buffs: Defiance, Divine Blessing, Purges
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_defiance), "\u2694", ColumnData.CardData(data.topDefiance, { it.sessionDefianceTotal.toString() }, DEFIANCE_COLOR)),
      Triple(getString(Res.string.summary_top_garden_defiance), "\u2600", ColumnData.CardData(data.topGardenDefiance, { it.sessionGardenDefianceTotal.toString() }, GARDEN_DEFIANCE_COLOR)),
      Triple(getString(Res.string.summary_top_purges), "\u2728", ColumnData.CardData(data.topPurges, { it.sessionPurgeTotal.toString() }, PURGE_COLOR)),
    )))

    // Special Buffs Continued: Impale Immunity, Protective Wings, Courageous Action
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_impales), "\uD83D\uDEE1", ColumnData.CardData(data.topImpaleImmunity, { it.sessionImpaleImmunityTotal.toString() }, IMPALE_IMMUNITY_COLOR)),
      Triple(getString(Res.string.summary_top_protective_wings), "\uD83E\uDD85", ColumnData.CardData(data.topProtectiveWings, { it.sessionProtectiveWingsTotal.toString() }, PROTECTIVE_WINGS_COLOR)),
      Triple(getString(Res.string.summary_top_courageous_action), "\u2728", ColumnData.CardData(data.topCourageousAction, { it.sessionCourageousActionTotal.toString() }, COURAGEOUS_ACTION_COLOR)),
    )))

    // Special Melee: Tiger Strikes, Mist Sunder, Regular Sunder
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_tiger_strikes), "\u26A1", ColumnData.CardData(data.topTigerStrikes, { it.sessionTigerStrikeTotal.toString() }, TIGER_STRIKE_COLOR)),
      Triple(getString(Res.string.summary_top_mist_sunder), "\u26CF", ColumnData.CardData(data.topMistSunder, { it.sessionMistSunderTotal.toString() }, MIST_SUNDER_COLOR)),
      Triple(getString(Res.string.summary_top_regular_sunder), "\u26CF", ColumnData.CardData(data.topRegularSunder, { it.sessionRegularSunderTotal.toString() }, REGULAR_SUNDER_COLOR)),
    )))

    // Loot buffs: best peak %, worst peak %, most simultaneous buffs (raid-wide)
    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_loot_peak), "\uD83C\uDFC6", ColumnData.CardData(data.topLootPeak, { "${it.sessionPeakLootBuffAmount}%" }, LOOT_BUFF_COLOR)),
      Triple(getString(Res.string.summary_worst_loot_peak), "\uD83D\uDD0C", ColumnData.CardData(data.worstLootPeak, { "${it.sessionPeakLootBuffAmount}%" }, LOOT_BUFF_COLOR)),
      Triple(getString(Res.string.summary_top_buff_count), "\u26A1", ColumnData.CardData(data.topBuffCount, { it.sessionCurrentBuffCount.toString() }, LOOT_BUFF_COLOR)),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_haranya_builds), "\u2694", ColumnData.BuildData(data.buildCountsHaranya)),
      Triple(getString(Res.string.summary_nuia_builds),    "\u2694", ColumnData.BuildData(data.buildCountsNuia)),
      Triple(getString(Res.string.summary_pirate_builds),  "\u2694", ColumnData.BuildData(data.buildCountsPirate)),
    )))

    return tripletBlocks
  }

  private var cachedFont: Font? = null
  private var cachedFontLanguage: String? = null
  private var cachedEmojiFont: Font? = null

  private fun createFont(style: Int, size: Float): Font {
    val language = Locale.getDefault().language
    val baseFont = cachedFont
      ?.takeIf { cachedFontLanguage == language }
      ?.deriveFont(size) ?: run {
      val font = if (language == "zh") {
        listOf("Microsoft YaHei UI", "Microsoft YaHei", "Noto Sans CJK SC", "Dialog")
          .firstNotNullOfOrNull { name ->
            runCatching { Font(name, Font.PLAIN, size.toInt()) }
              .getOrNull()
              ?.takeIf { candidate -> candidate.canDisplay('中') }
          }
          ?: Font("Dialog", Font.PLAIN, size.toInt())
      } else if (language == "ko") {
        try {
          val uri = Res.getUri("font/arkorean_regular.ttf")
          val koreanFont = Font.createFont(Font.TRUETYPE_FONT, URI(uri).toURL().openStream())
          if (koreanFont.canDisplay('한')) {
            koreanFont
          } else {
            Font("Dialog", Font.PLAIN, size.toInt())
          }
        } catch (_: Exception) {
          Font("Dialog", Font.PLAIN, size.toInt())
        }
      } else {
        Font("Dialog", Font.PLAIN, size.toInt())
      }
      cachedFont = font
      cachedFontLanguage = language
      font.deriveFont(style, size)
    }
    return baseFont.deriveFont(style, size)
  }

  private fun createFittingHeaderFont(g2d: Graphics2D, title: String, width: Int, icon: String): Font {
    val maxTextWidth = (width - 20).coerceAtLeast(1)
    var size = 13f
    while (size > 8f) {
      val font = createFont(Font.BOLD, size)
      val iconFont = createEmojiFont(Font.BOLD, size)
      g2d.font = font
      val textWidth = g2d.fontMetrics.stringWidth(title)
      g2d.font = iconFont
      val iconWidth = if (icon.isEmpty()) 0 else g2d.fontMetrics.stringWidth(icon)
      val spacingWidth = if (icon.isEmpty()) 0 else g2d.fontMetrics.stringWidth("  ")
      if (textWidth + iconWidth * 2 + spacingWidth <= maxTextWidth) return font
      size -= 1f
    }
    return createFont(Font.BOLD, 8f)
  }

  private fun createEmojiFont(style: Int, size: Float): Font {
    return cachedEmojiFont?.deriveFont(style, size) ?: run {
      try {
        val font = Font("Segoe UI Emoji", Font.PLAIN, size.toInt())
        cachedEmojiFont = font
        font.deriveFont(style, size)
      } catch (_: Exception) {
        Font("Segoe UI", style, size.toInt())
      }
    }
  }
}
