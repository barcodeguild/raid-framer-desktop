package com.reoky.raidframer.ui.export

import androidx.compose.ui.graphics.Color as ComposeColor
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.sortedByDisplayOrder
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.localizedDisplayNameRes
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.getDocumentsDirectory
import com.reoky.raidframer.core.helpers.getFactionHighlightColor
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.pvpPerformancePoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.arkorean_regular
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
import raid_framer_desktop.composeapp.generated.resources.summary_top_charms
import raid_framer_desktop.composeapp.generated.resources.summary_top_debuffs
import raid_framer_desktop.composeapp.generated.resources.summary_top_damage_taken
import raid_framer_desktop.composeapp.generated.resources.summary_top_distresses
import raid_framer_desktop.composeapp.generated.resources.summary_top_glider_gamers
import raid_framer_desktop.composeapp.generated.resources.summary_top_heals_received
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
import org.jetbrains.compose.resources.getString
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.svg.SVGDOM
import javax.imageio.ImageIO

object ImageExportInteractor {

  private const val IMAGE_WIDTH = 2280
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

  // ── Skill-tree icon cache (SVG → BufferedImage via Skiko) ─────────────────
  private val skillTreeImageCache = mutableMapOf<Pair<SkillTreeType?, Int>, BufferedImage?>()

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
  )

  data class SpellDamage(val spell: String, val total: Double)
  data class ItemUsage(val itemName: String, val count: Int)

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
      topDamage           = PlayerCacheInteractor.topDamage.value.take(50),
      topHeals            = PlayerCacheInteractor.topHeals.value.take(50),
      topCC               = PlayerCacheInteractor.topCC.value.take(50),
      topSilences         = PlayerCacheInteractor.topSilences.value.take(15),
      topCharms           = PlayerCacheInteractor.topCharms.value.take(15),
      topDistresses       = PlayerCacheInteractor.topDistresses.value.take(15),
      topDamageSpellsHaranya = PlayerCacheInteractor.topDamageSpellsHaranya.value.take(15).map { SpellDamage(it.spell, it.total) },
      topDamageSpellsNuia    = PlayerCacheInteractor.topDamageSpellsNuia.value.take(15).map { SpellDamage(it.spell, it.total) },
      topDamageSpellsPirate  = PlayerCacheInteractor.topDamageSpellsPirate.value.take(15).map { SpellDamage(it.spell, it.total) },
      topDebuffs          = PlayerCacheInteractor.topDebuff.value.take(15),
      topSongs            = PlayerCacheInteractor.topSongs.value.take(15),
      topBuffs            = PlayerCacheInteractor.topBuffs.value.take(15),
      topOdeHaranya       = PlayerCacheInteractor.topOdeHaranya.value.take(15),
      topOdeNuia          = PlayerCacheInteractor.topOdeNuia.value.take(15),
      topOdePirate        = PlayerCacheInteractor.topOdePirate.value.take(15),
      topKillsHaranya     = PlayerCacheInteractor.topKillsHaranya.value.take(15),
      topKillsNuia        = PlayerCacheInteractor.topKillsNuia.value.take(15),
      topKillsPirate      = PlayerCacheInteractor.topKillsPirate.value.take(15),
      topDamageTaken      = PlayerCacheInteractor.topDamageTaken.value.take(15),
      topHealsReceived    = PlayerCacheInteractor.topHealsReceived.value.take(15),
      topItemUsesHaranya  = PlayerCacheInteractor.topItemUsesHaranya.value.take(15).map { ItemUsage(getString(it.itemName), it.count) },
      topItemUsesNuia     = PlayerCacheInteractor.topItemUsesNuia.value.take(15).map { ItemUsage(getString(it.itemName), it.count) },
      topItemUsesPirate   = PlayerCacheInteractor.topItemUsesPirate.value.take(15).map { ItemUsage(getString(it.itemName), it.count) },
      topPotters          = PlayerCacheInteractor.topPotters.value.take(15),
      topGliderGamers     = PlayerCacheInteractor.topGliderGamers.value.take(15),
      topItemSkillCasters = PlayerCacheInteractor.topItemSkillCasters.value.take(15),
      buildCountsHaranya  = PlayerCacheInteractor.buildCountsHaranya.value,
      buildCountsNuia     = PlayerCacheInteractor.buildCountsNuia.value,
      buildCountsPirate   = PlayerCacheInteractor.buildCountsPirate.value,
      buildDisplayNames   = (PlayerCacheInteractor.buildCountsHaranya.value.keys +
                              PlayerCacheInteractor.buildCountsNuia.value.keys +
                              PlayerCacheInteractor.buildCountsPirate.value.keys)
        .distinct()
        .associateWith { name -> SpecType.fromName(name)?.let { getString(it.localizedDisplayNameRes) } ?: name },
      topPerformanceHaranya = PlayerCacheInteractor.topPerformanceHaranya.value.take(15),
      topPerformanceNuia    = PlayerCacheInteractor.topPerformanceNuia.value.take(15),
      topPerformancePirate  = PlayerCacheInteractor.topPerformancePirate.value.take(15),
      factionSilenceData  = PlayerCacheInteractor.factionSilenceComparisonAll.value,
      factionCharmData    = PlayerCacheInteractor.factionCharmComparisonAll.value,
      factionDistressData = PlayerCacheInteractor.factionDistressComparisonAll.value,
    )
  }

  suspend fun exportToPng(data: ExportData): File? {
    return withContext(Dispatchers.IO) {
      try {
        val imageHeight = calculateImageHeight(data)
        val renderWidth = IMAGE_WIDTH * EXPORT_RENDER_SCALE
        val renderHeight = imageHeight * EXPORT_RENDER_SCALE
        val renderedImage = BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = renderedImage.createGraphics()

        applyHighQualityRenderingHints(g2d)
        g2d.scale(EXPORT_RENDER_SCALE.toDouble(), EXPORT_RENDER_SCALE.toDouble())

        drawWallpaperBackground(g2d, IMAGE_WIDTH, imageHeight)
        drawMasonryLayout(g2d, data)

        g2d.dispose()

        val image = downsampleImage(renderedImage, IMAGE_WIDTH, imageHeight)

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
        val outputFile = exportDir.resolve("${data.sessionTitle}.png").toFile()

        ImageIO.write(image, "png", outputFile)
        outputFile
      } catch (e: Exception) {
        e.printStackTrace()
        null
      }
    }
  }

  /*
   * So the reason we downsample the image from a higher resolution is because of the icons and fonts being pixelated
   * if we render at the target resolution. The first iteration of the PNG export was very pixelated and this ended-up
   * being the solution.
   */
  private fun downsampleImage(source: BufferedImage, width: Int, height: Int): BufferedImage {
    val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val sg = scaled.createGraphics()
    applyHighQualityRenderingHints(sg)
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
    g2d.color = toAwtColor(RFColors.CardBackground)
    g2d.fillRect(0, 0, width, height)

    try {
      val resUri = Res.getUri("drawable/reoky_wallpaper.png")
      val wallpaper = ImageIO.read(URI(resUri).toURL())
      if (wallpaper != null) {
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
        g2d.drawImage(wallpaper, 0, 0, width, height, null)
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)
      }
    } catch (_: Exception) {
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
    val superColWidth = (IMAGE_WIDTH - SUPER_COL_GAP * 2) / 3

    val pieChartBlock = makePieChartBlock(data, superColWidth)
    val combatBlock   = makeCombatBlock(data, superColWidth)

    val titleOffset    = TITLE_CARD_HEIGHT + 5          // all cols start here
    val col0BaseHeight = titleOffset + pieChartBlock.height + 5 + combatBlock.height

    val tripletBlocks = getAllCategoryBlocks(data, superColWidth)
    val colHeights    = mutableListOf(col0BaseHeight, titleOffset, titleOffset)

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
    data class CardData(val cards: List<PlayerCard>, val getValue: (PlayerCard) -> String, val valueColor: Color) : ColumnData()
  }


  private fun makeCombatBlock(data: ExportData, colWidth: Int): Block {
    val combatRows = maxOf(data.topDamage.size, data.topHeals.size, data.topCC.size).coerceAtLeast(1)
    val combatH = SECTION_HEADER_HEIGHT + (combatRows * ROW_HEIGHT) + CARD_PADDING * 2

    return Block("Combat", combatH) { g2d, x, y, w ->
      drawCardBackgroundTransparent(g2d, x, y, w, combatH)
      val subColW = w / 3

      val damageTitle = if (data.allowPvE) "PvE Damage" else "PvP Damage"
      val healsTitle  = if (data.allowPvE) "PvE Heals"  else "PvP Heals"
      val ccTitle     = if (data.allowPvE) "PvE CC"    else "PvP CC"
      val icon = if (data.allowPvE) "⚔" else "🔥"

      drawSectionHeader(g2d, damageTitle, x,               y, subColW, toAwtColor(RFColors.dpsOrange), icon)
      drawSectionHeader(g2d, healsTitle,  x + subColW,     y, subColW, toAwtColor(RFColors.healsGreen), "💉")
      drawSectionHeader(g2d, ccTitle,     x + subColW * 2, y, subColW, toAwtColor(RFColors.ccCyan), "🛡")

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

  private const val MAX_ROWS = 15

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
    val superColWidth = (IMAGE_WIDTH - SUPER_COL_GAP * 2) / 3

    // Full-width title card
    val titleH      = drawTitleCard(g2d, data, COLUMN_GAP, 10, IMAGE_WIDTH - COLUMN_GAP * 2)
    val titleBottom = 10 + titleH + 5

    val columnY       = mutableListOf(titleBottom, titleBottom, titleBottom)
    val columnHeights = mutableListOf(titleH, titleH, titleH)

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
      val drawWidth   = if (shortestCol == 2) IMAGE_WIDTH - xPos - COLUMN_GAP else superColWidth
      block.draw(g2d, xPos, columnY[shortestCol], drawWidth)
      columnY[shortestCol]       += block.height + 5
      columnHeights[shortestCol] += block.height + 5
    }
  }

  /**
   * Title card — now drawn full-width so there is no wasted space on the right.
   * The session metadata is sourced from ExportData which was salted with
   */
  private var logoCache: BufferedImage? = null

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
    val textFont = createFont(Font.BOLD, 13f)
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
    width: Int
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
    val spec   = SpecType.fromName(card.currentBuild)
    var iconX  = xOffset + 28
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

    val makeTriplet: (List<Triple<String, String, ColumnData>>) -> TripletBlock = { columns ->
      val maxBlockHeight = columns.maxOfOrNull { calculateHeight(it.third) } ?: CATEGORY_MIN_HEIGHT
      TripletBlock("", emptyList(), maxBlockHeight) { g2d, x, y, w ->
        val actualSubColWidth = if (w < superColWidth) (w - COLUMN_GAP * 2) / 3 else subColWidth
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
                  drawRankingRow(
                    g2d, i, d.cards[i],
                    d.getValue(d.cards[i]), d.valueColor,
                    xPos + CARD_PADDING, rowY,
                    actualAvailW
                  )
                  rowY += ROW_HEIGHT
                }
              }
            }
          }
        }
      }
    }

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_haranya_spells_damage), "\uD83D\uDD25", ColumnData.SpellData(data.topDamageSpellsHaranya)),
      Triple(getString(Res.string.summary_top_nuia_spells_damage),    "\uD83D\uDD25", ColumnData.SpellData(data.topDamageSpellsNuia)),
      Triple(getString(Res.string.summary_top_pirate_spells_damage),  "\uD83D\uDD25", ColumnData.SpellData(data.topDamageSpellsPirate)),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_debuffs), "\uD83D\uDD25", ColumnData.CardData(data.topDebuffs, { it.sessionDebuffTotal.toString() }, HARANYA_COLOR)),
      Triple(getString(Res.string.summary_top_songs),   "\u2764", ColumnData.CardData(data.topSongs,   { it.sessionSongsTotal.toString()  }, NUIA_COLOR)),
      Triple(getString(Res.string.summary_top_buffs),   "\u26A1", ColumnData.CardData(data.topBuffs,   { it.sessionBuffTotal.toString()   }, PIRATE_COLOR)),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_ode_haranya), "\uD83C\uDFB5", ColumnData.CardData(data.topOdeHaranya, { it.sessionOdeHealsTotal.humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen))),
      Triple(getString(Res.string.summary_top_ode_nuia),    "\uD83C\uDFB5", ColumnData.CardData(data.topOdeNuia,    { it.sessionOdeHealsTotal.humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen))),
      Triple(getString(Res.string.summary_top_ode_pirate),  "\uD83C\uDFB5", ColumnData.CardData(data.topOdePirate,  { it.sessionOdeHealsTotal.humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen))),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_kills_haranya), "\u2694", ColumnData.CardData(data.topKillsHaranya, { it.sessionKillTotal.toString() }, KILLS_HARANYA_COLOR)),
      Triple(getString(Res.string.summary_top_kills_nuia),    "\u2694", ColumnData.CardData(data.topKillsNuia,    { it.sessionKillTotal.toString() }, KILLS_NUIA_COLOR)),
      Triple(getString(Res.string.summary_top_kills_pirate),  "\u2694", ColumnData.CardData(data.topKillsPirate,  { it.sessionKillTotal.toString() }, KILLS_PIRATE_COLOR)),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_damage_taken),     "\uD83D\uDD25", ColumnData.CardData(data.topDamageTaken,   { it.sessionDamageTakenTotal.toLong().humanReadableAbbreviation()   }, toAwtColor(RFColors.dpsOrange))),
      Triple(getString(Res.string.summary_top_heals_received), "\uD83D\uDC89", ColumnData.CardData(data.topHealsReceived, { it.sessionHealsReceivedTotal.toLong().humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen))),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_haranya_item_uses), "\uD83D\uDCE6", ColumnData.ItemData(data.topItemUsesHaranya)),
      Triple(getString(Res.string.summary_top_nuia_item_uses),    "\uD83D\uDCE6", ColumnData.ItemData(data.topItemUsesNuia)),
      Triple(getString(Res.string.summary_top_pirate_item_uses),  "\uD83D\uDCE6", ColumnData.ItemData(data.topItemUsesPirate)),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_potion_drinkers), "\uD83E\uDDEA", ColumnData.CardData(data.topPotters,          { it.sessionPotionTotal.toString()    }, POTION_COLOR)),
      Triple(getString(Res.string.summary_top_glider_gamers),   "\uD83D\uDD3A", ColumnData.CardData(data.topGliderGamers,     { it.sessionGliderTotal.toString()    }, GLIDER_COLOR)),
      Triple(getString(Res.string.summary_most_item_usages),     "\u2699", ColumnData.CardData(data.topItemSkillCasters, { it.sessionItemSkillTotal.toString() }, ITEM_SKILL_COLOR)),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_haranya_builds), "\u2694", ColumnData.BuildData(data.buildCountsHaranya)),
      Triple(getString(Res.string.summary_nuia_builds),    "\u2694", ColumnData.BuildData(data.buildCountsNuia)),
      Triple(getString(Res.string.summary_pirate_builds),  "\u2694", ColumnData.BuildData(data.buildCountsPirate)),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_haranya_performance), "\uD83C\uDFC6", ColumnData.CardData(data.topPerformanceHaranya, { it.pvpPerformancePoints().toString() }, HARANYA_COLOR)),
      Triple(getString(Res.string.summary_top_nuia_performance),    "\uD83C\uDFC6", ColumnData.CardData(data.topPerformanceNuia,    { it.pvpPerformancePoints().toString() }, NUIA_COLOR)),
      Triple(getString(Res.string.summary_top_pirate_performance),  "\uD83C\uDFC6", ColumnData.CardData(data.topPerformancePirate,  { it.pvpPerformancePoints().toString() }, PIRATE_COLOR)),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      Triple(getString(Res.string.summary_top_silences),  "\uD83D\uDD07", ColumnData.CardData(data.topSilences,  { it.sessionSilenceTotal.toString()  }, SILENCE_COLOR)),
      Triple(getString(Res.string.summary_top_charms),    "\uD83D\uDC96", ColumnData.CardData(data.topCharms,    { it.sessionCharmTotal.toString()    }, CHARM_COLOR)),
      Triple(getString(Res.string.summary_top_distresses), "\uD83D\uDE24", ColumnData.CardData(data.topDistresses, { it.sessionDistressTotal.toString() }, DISTRESS_COLOR)),
    )))

    return tripletBlocks
  }

  private var cachedFont: Font? = null
  private var cachedEmojiFont: Font? = null

  private fun createFont(style: Int, size: Float): Font {
    val baseFont = cachedFont?.deriveFont(size) ?: run {
      try {
        val uri = Res.getUri("font/arkorean_regular.ttf")
        val font = Font.createFont(Font.TRUETYPE_FONT, URI(uri).toURL().openStream()).deriveFont(size)
        cachedFont = font
        return font.deriveFont(style)
      } catch (_: Exception) { }
      try {
        Font("Segoe UI Emoji", style, size.toInt())
      } catch (_: Exception) {
        Font("Segoe UI", style, size.toInt())
      }
    }
    return baseFont.deriveFont(style)
  }

  private fun createEmojiFont(style: Int, size: Float): Font {
    return cachedEmojiFont?.deriveFont(style, size) ?: run {
      try {
        Font("Segoe UI Emoji", style, size.toInt())
      } catch (_: Exception) {
        Font("Segoe UI", style, size.toInt())
      }
    }
  }
}
