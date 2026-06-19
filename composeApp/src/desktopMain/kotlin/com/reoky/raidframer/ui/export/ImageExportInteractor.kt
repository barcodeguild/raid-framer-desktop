package com.reoky.raidframer.ui.export

import androidx.compose.ui.graphics.Color as ComposeColor
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.helpers.humanReadableAbbreviation
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.PlayerCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import raid_framer_desktop.composeapp.generated.resources.Res
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
import javax.imageio.ImageIO
import kotlin.io.path.exists

object ImageExportInteractor {

  private const val IMAGE_WIDTH = 2280
  private const val ROW_HEIGHT = 24
  private const val SECTION_HEADER_HEIGHT = 32
  private const val CHART_HEIGHT = 170
  private const val CATEGORY_MIN_HEIGHT = 100
  private const val COLUMN_GAP = 10
  private const val CARD_PADDING = 8
  private const val SUPER_COL_GAP = 10

  private val CARD_BACKGROUND = Color(0, 0, 0)
  private val CARD_BACKGROUND_TRANSPARENT = Color(0, 0, 0, 110)
  private val BORDER_COLOR = Color(55, 55, 70)
  private val BORDER_COLOR_TRANSPARENT = Color(55, 55, 70, 150)

  private fun toAwtColor(composeColor: ComposeColor): Color {
    return Color(composeColor.red, composeColor.green, composeColor.blue, composeColor.alpha)
  }

  private val TEXT_PRIMARY = toAwtColor(RFColors.TextPrimary)
  private val HARANYA_COLOR = toAwtColor(RFColors.factionHaranya)
  private val NUIA_COLOR = toAwtColor(RFColors.factionNuia)
  private val PIRATE_COLOR = toAwtColor(RFColors.factionPirate)

  private fun getDocumentsDirectory(): String? {
    if (System.getProperty("os.name").lowercase().contains("win")) {
      val userProfile = System.getenv("USERPROFILE")
      if (!userProfile.isNullOrBlank()) {
        val oneDriveDocs = Paths.get(userProfile, "OneDrive", "Documents")
        if (oneDriveDocs.exists()) return oneDriveDocs.toString()
        val regularDocs = Paths.get(userProfile, "Documents")
        if (regularDocs.exists()) return regularDocs.toString()
      }
    }
    val home = System.getProperty("user.home") ?: return null
    return Paths.get(home, "Documents").toString()
  }

  data class ExportData(
    val sessionTitle: String,
    val sessionDate: String,
    val sessionDurationMs: Long,
    val allowPvE: Boolean,
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
    val factionSilenceData: Map<String, Float>,
    val factionCharmData: Map<String, Float>,
    val factionDistressData: Map<String, Float>,
  )

  data class SpellDamage(val spell: String, val total: Double)
  data class ItemUsage(val itemName: String, val count: Int)

  suspend fun captureSnapshot(): ExportData {
    val config = RFConfig.state.value
    val sessionStart = config.lastSessionStart
    val durationMs = if (config.lastSessionDurationMs > 0) {
      config.lastSessionDurationMs
    } else if (sessionStart > 0) {
      System.currentTimeMillis() - sessionStart
    } else {
      0L
    }

    return ExportData(
      sessionTitle = config.lastSessionTitle.ifBlank { "session" },
      sessionDate = DateFormat.getDateInstance(DateFormat.SHORT).format(Date()),
      sessionDurationMs = durationMs,
      allowPvE = config.allowPVEDamage,
      topDamage = PlayerCacheInteractor.topDamage.value.take(50),
      topHeals = PlayerCacheInteractor.topHeals.value.take(50),
      topCC = PlayerCacheInteractor.topCC.value.take(50),
      topSilences = PlayerCacheInteractor.topSilences.value.take(15),
      topCharms = PlayerCacheInteractor.topCharms.value.take(15),
      topDistresses = PlayerCacheInteractor.topDistresses.value.take(15),
      topDamageSpellsHaranya = PlayerCacheInteractor.topDamageSpellsHaranya.value.take(15).map { SpellDamage(it.spell, it.total) },
      topDamageSpellsNuia = PlayerCacheInteractor.topDamageSpellsNuia.value.take(15).map { SpellDamage(it.spell, it.total) },
      topDamageSpellsPirate = PlayerCacheInteractor.topDamageSpellsPirate.value.take(15).map { SpellDamage(it.spell, it.total) },
      topDebuffs = PlayerCacheInteractor.topDebuff.value.take(15),
      topSongs = PlayerCacheInteractor.topSongs.value.take(15),
      topBuffs = PlayerCacheInteractor.topBuffs.value.take(15),
      topOdeHaranya = PlayerCacheInteractor.topOdeHaranya.value.take(15),
      topOdeNuia = PlayerCacheInteractor.topOdeNuia.value.take(15),
      topOdePirate = PlayerCacheInteractor.topOdePirate.value.take(15),
      topKillsHaranya = PlayerCacheInteractor.topKillsHaranya.value.take(15),
      topKillsNuia = PlayerCacheInteractor.topKillsNuia.value.take(15),
      topKillsPirate = PlayerCacheInteractor.topKillsPirate.value.take(15),
      topDamageTaken = PlayerCacheInteractor.topDamageTaken.value.take(15),
      topHealsReceived = PlayerCacheInteractor.topHealsReceived.value.take(15),
      topItemUsesHaranya = PlayerCacheInteractor.topItemUsesHaranya.value.take(15).map { ItemUsage(getString(it.itemName), it.count) },
      topItemUsesNuia = PlayerCacheInteractor.topItemUsesNuia.value.take(15).map { ItemUsage(getString(it.itemName), it.count) },
      topItemUsesPirate = PlayerCacheInteractor.topItemUsesPirate.value.take(15).map { ItemUsage(getString(it.itemName), it.count) },
      topPotters = PlayerCacheInteractor.topPotters.value.take(15),
      topGliderGamers = PlayerCacheInteractor.topGliderGamers.value.take(15),
      topItemSkillCasters = PlayerCacheInteractor.topItemSkillCasters.value.take(15),
      buildCountsHaranya = PlayerCacheInteractor.buildCountsHaranya.value,
      buildCountsNuia = PlayerCacheInteractor.buildCountsNuia.value,
      buildCountsPirate = PlayerCacheInteractor.buildCountsPirate.value,
      factionSilenceData = PlayerCacheInteractor.factionSilenceComparisonAll.value,
      factionCharmData = PlayerCacheInteractor.factionCharmComparisonAll.value,
      factionDistressData = PlayerCacheInteractor.factionDistressComparisonAll.value,
    )
  }

  suspend fun exportToPng(data: ExportData): File? {
    return withContext(Dispatchers.IO) {
      try {
        val imageHeight = calculateImageHeight(data)
        val image = BufferedImage(IMAGE_WIDTH, imageHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        drawWallpaperBackground(g2d, IMAGE_WIDTH, imageHeight)
        drawMasonryLayout(g2d, data)

        g2d.dispose()

        val config = RFConfig.state.value
        val exportDir = if (config.lastSessionExportDir.isNotBlank()) {
          Paths.get(config.lastSessionExportDir)
        } else {
          val documentsDir = getDocumentsDirectory() ?: return@withContext null
          val now = Date()
          val year = SimpleDateFormat("yyyy", Locale.US).format(now)
          val month = SimpleDateFormat("MM", Locale.US).format(now)
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

  private fun drawWallpaperBackground(g2d: Graphics2D, width: Int, height: Int) {
    g2d.color = toAwtColor(RFColors.CardBackground)
    g2d.fillRect(0, 0, width, height)

    try {
      val resUri = Res.getUri("drawable/reoky_wallpaper.png")
      if (resUri != null) {
        val wallpaper = ImageIO.read(URI(resUri.toString()).toURL())
        if (wallpaper != null) {
          val scaled = wallpaper.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH)
          g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
          g2d.drawImage(scaled, 0, 0, null)
          g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)
        }
      }
    } catch (_: Exception) {
    }
  }

  private fun calculateImageHeight(data: ExportData): Int {
    val columnHeights = computeColumnHeights(data)
    return columnHeights.max() + 30
  }

  private fun computeColumnHeights(data: ExportData): List<Int> {
    val superColWidth = (IMAGE_WIDTH - SUPER_COL_GAP * 2) / 3

    val pieChartBlock = makePieChartBlock(data, superColWidth)
    val combatBlock = makeCombatBlock(data, superColWidth)
    val col0BaseHeight = 80 + pieChartBlock.height + 10 + combatBlock.height

    val tripletBlocks = getAllCategoryBlocks(data, superColWidth)
    val colHeights = mutableListOf(col0BaseHeight, 10, 10)

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

      val damageTitle = if (data.allowPvE) "⚔️ PvE Damage ⚔️" else "🔥 PvP Damage 🔥"
      val healsTitle = if (data.allowPvE) "💉 PvE Heals 💉" else "💉 PvP Heals 💉"
      val ccTitle = if (data.allowPvE) "🛡️ PvE CC 🛡️" else "🛡️ PvP CC 🛡️"

      drawSectionHeader(g2d, damageTitle, x, y, subColW, toAwtColor(RFColors.dpsOrange))
      drawSectionHeader(g2d, healsTitle, x + subColW, y, subColW, toAwtColor(RFColors.healsGreen))
      drawSectionHeader(g2d, ccTitle, x + subColW * 2, y, subColW, toAwtColor(RFColors.ccCyan))

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
      is ColumnData.ItemData -> data.items.size
      is ColumnData.BuildData -> data.builds.size
      is ColumnData.CardData -> data.cards.size
    }
    val rows = numRows.coerceAtLeast(5).coerceAtMost(MAX_ROWS)
    return (SECTION_HEADER_HEIGHT + (rows * ROW_HEIGHT) + CARD_PADDING * 2).coerceAtLeast(CATEGORY_MIN_HEIGHT)
  }

  private fun makePieChartBlock(data: ExportData, superColWidth: Int): Block {
    val chartH = CHART_HEIGHT + CARD_PADDING * 2

    return Block("Charts", chartH) { g2d, x, y, w ->
      drawCardBackgroundTransparent(g2d, x, y, w, chartH)

      val chartSpacing = w / 3
      val chartRadius = 45
      val chartY = y + CARD_PADDING + 62

      drawPieChart(g2d, "Silences", data.factionSilenceData, (chartSpacing / 2).toInt(), chartY, chartRadius, 0)
      drawPieChart(g2d, "Charms", data.factionCharmData, (chartSpacing / 2).toInt(), chartY, chartRadius, chartSpacing.toInt())
      drawPieChart(g2d, "Distresses", data.factionDistressData, (chartSpacing / 2).toInt(), chartY, chartRadius, (chartSpacing * 2).toInt())
    }
  }

  private fun drawMasonryLayout(g2d: Graphics2D, data: ExportData) {
    val superColWidth = (IMAGE_WIDTH - SUPER_COL_GAP * 2) / 3
    val columnY = mutableListOf(10, 10, 10)
    val columnHeights = mutableListOf(0, 0, 0)

    val titleH = drawTitleCard(g2d, data, COLUMN_GAP, columnY[0], superColWidth)
    columnY[0] += titleH + 5
    columnHeights[0] = titleH

    val pieBlock = makePieChartBlock(data, superColWidth)
    pieBlock.draw(g2d, COLUMN_GAP, columnY[0], superColWidth)
    columnY[0] += pieBlock.height + 5
    columnHeights[0] += pieBlock.height + 5

    val combatBlock = makeCombatBlock(data, superColWidth)
    combatBlock.draw(g2d, COLUMN_GAP, columnY[0], superColWidth)
    columnY[0] += combatBlock.height + 5
    columnHeights[0] += combatBlock.height + 5

    val tripletBlocks = getAllCategoryBlocks(data, superColWidth)
    tripletBlocks.forEach { block ->
      val shortestCol = columnHeights.indices.minByOrNull { columnHeights[it] } ?: 0
      val xPos = COLUMN_GAP + shortestCol * (superColWidth + SUPER_COL_GAP)
      val drawWidth = if (shortestCol == 2) IMAGE_WIDTH - xPos - COLUMN_GAP else superColWidth
      block.draw(g2d, xPos, columnY[shortestCol], drawWidth)
      columnY[shortestCol] += block.height + 5
      columnHeights[shortestCol] += block.height + 5
    }
  }

  private fun drawTitleCard(g2d: Graphics2D, data: ExportData, x: Int, y: Int, width: Int): Int {
    val titleH = 90

    drawCardBackgroundTransparent(g2d, x, y, width, titleH)

    val titleFont = createFont(Font.BOLD, 20f)
    val subtitleFont = createFont(Font.PLAIN, 12f)

    g2d.color = TEXT_PRIMARY
    g2d.font = titleFont
    g2d.drawString("${AppGlobals.APP_NAME} - Battle Summary", x + CARD_PADDING + 4, y + 32)

    val durationStr = formatDuration(data.sessionDurationMs)
    g2d.font = subtitleFont
    g2d.color = toAwtColor(RFColors.TextSecondary)
    g2d.drawString("${data.sessionTitle}  •  ${data.sessionDate}  •  ${AppGlobals.APP_VERSION}  •  ${durationStr}", x + CARD_PADDING + 4, y + 56)

    g2d.font = createFont(Font.PLAIN, 10f)
    val faded = toAwtColor(RFColors.TextSecondary)
    g2d.color = Color(faded.red, faded.green, faded.blue, 120)
    g2d.drawString("${AppGlobals.APP_NAME} v${AppGlobals.APP_VERSION}", x + CARD_PADDING + 4, y + 78)

    return titleH
  }

  private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
      "${hours}h ${minutes}m"
    } else if (minutes > 0) {
      "${minutes}m ${seconds}s"
    } else {
      "${seconds}s"
    }
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

  private fun drawSectionHeader(g2d: Graphics2D, title: String, x: Int, y: Int, width: Int, color: Color) {
    g2d.font = createFont(Font.BOLD, 13f)
    g2d.color = color
    val titleWidth = g2d.fontMetrics.stringWidth(title)
    val centeredX = x + (width - titleWidth) / 2
    g2d.drawString(title, centeredX.coerceAtLeast(x + 4), y + 18)
  }

  private fun drawRankingRow(g2d: Graphics2D, index: Int, card: PlayerCard, valueText: String, valueColor: Color, xOffset: Int, y: Int, width: Int) {
    val rowFont = createFont(Font.PLAIN, 11f)
    val valueFont = createFont(Font.BOLD, 11f)

    g2d.font = rowFont
    g2d.color = TEXT_PRIMARY

    g2d.drawString("${index + 1}.", xOffset + 8, y + 16)
    g2d.drawString(card.name, xOffset + 28, y + 16)

    g2d.font = valueFont
    g2d.color = valueColor
    val bounds = g2d.fontMetrics.getStringBounds(valueText, g2d)
    g2d.drawString(valueText, xOffset + width - bounds.width.toInt() - 8, y + 16)
  }

  private fun drawPieChart(g2d: Graphics2D, title: String, factionData: Map<String, Float>, centerX: Int, centerY: Int, radius: Int, xOffset: Int) {
    val titleFont = createFont(Font.BOLD, 11f)
    val labelFont = createFont(Font.PLAIN, 10f)

    g2d.font = titleFont
    g2d.color = TEXT_PRIMARY
    g2d.drawString(title, xOffset + centerX - g2d.fontMetrics.stringWidth(title) / 2, centerY - radius - 8)

    val allFactions = listOf("Haranya", "Nuia", "Pirate")
    val displayData = allFactions.associateWith { faction -> factionData[faction] ?: 0f }
    val total = displayData.values.sum()

    if (total == 0f) {
      val greyBg = Color(50, 50, 60)
      g2d.color = greyBg
      g2d.fillArc(xOffset + centerX - radius, centerY - radius, radius * 2, radius * 2, 0, 360)

      g2d.font = labelFont
      g2d.color = toAwtColor(RFColors.TextSecondary)
      g2d.drawString("0", xOffset + centerX - 5, centerY + 4)

      var legendY = centerY + radius + 12
      val colors = listOf(Color(0xFFAB47BC.toInt()), Color(0xFFEC407A.toInt()), Color(0xFF7E57C2.toInt()))
      displayData.entries.forEachIndexed { index, (label, _) ->
        g2d.color = colors.getOrNull(index) ?: colors.last()
        g2d.fillRect(xOffset + centerX - radius, legendY, 8, 8)
        g2d.color = toAwtColor(RFColors.TextSecondary)
        g2d.drawString("$label: 0", xOffset + centerX - radius + 12, legendY + 8)
        legendY += 14
      }
      return
    }

    var startAngle = 0.0
    val colors = listOf(
      Color(0xFFAB47BC.toInt()),
      Color(0xFFEC407A.toInt()),
      Color(0xFF7E57C2.toInt()),
      Color(0xFF66BB6A.toInt()),
      Color(0xFFFFA726.toInt()),
    )

    displayData.entries.forEachIndexed { index, (label, value) ->
      val angle = ((value / total) * 360).toInt()
      if (angle > 0) {
        g2d.color = colors.getOrNull(index) ?: colors.last()
        g2d.fillArc(xOffset + centerX - radius, centerY - radius, radius * 2, radius * 2, startAngle.toInt(), angle)
        startAngle += angle
      }
    }

    var legendY = centerY + radius + 12
    g2d.font = labelFont
    displayData.entries.forEachIndexed { index, (label, value) ->
      g2d.color = colors.getOrNull(index) ?: colors.last()
      g2d.fillRect(xOffset + centerX - radius, legendY, 8, 8)
      g2d.color = TEXT_PRIMARY
      g2d.drawString("$label: ${value.toInt()}", xOffset + centerX - radius + 12, legendY + 8)
      legendY += 14
    }
  }

  private fun getAllCategoryBlocks(data: ExportData, superColWidth: Int): List<TripletBlock> {
    val tripletBlocks = mutableListOf<TripletBlock>()
    val subColWidth = (superColWidth - COLUMN_GAP * 2) / 3
    val availW = subColWidth - CARD_PADDING * 2

    val makeTriplet: (List<Pair<String, ColumnData>>) -> TripletBlock = { columns ->
      val maxBlockHeight = columns.maxOfOrNull { calculateHeight(it.second) } ?: CATEGORY_MIN_HEIGHT
      TripletBlock("", emptyList(), maxBlockHeight) { g2d, x, y, w ->
        val actualSubColWidth = if (w < superColWidth) {
          (w - COLUMN_GAP * 2) / 3
        } else {
          subColWidth
        }
        val actualAvailW = actualSubColWidth - CARD_PADDING * 2
        columns.forEachIndexed { index, (title, colData) ->
          val xPos = x + index * (actualSubColWidth + COLUMN_GAP)
          drawCardBackgroundTransparent(g2d, xPos, y, actualAvailW + CARD_PADDING * 2, maxBlockHeight)
          val icon = when {
            title.contains("Haranya") || title.contains("Debuffs") -> "\uD83D\uDD25"
            title.contains("Nuia") || title.contains("Songs") -> "\u2764"
            title.contains("Pirate") || title.contains("Buffs") -> "\u26A1"
            title.contains("Ode") -> "\uD83C\uDFB5"
            title.contains("Kills") -> "\u2694"
            title.contains("Dmg Taken") -> "\uD83D\uDD25"
            title.contains("Heals Recv") -> "\uD83D\uDC89"
            title.contains("Items") -> "\uD83D\uDCE6"
            title.contains("Potion") -> "\uD83E\uDdea"
            title.contains("Glider") -> "\uD83D\uDD3A"
            title.contains("Item Skills") -> "\u2699"
            title.contains("Builds") -> "\u2694"
            title.contains("Spell Damage") -> "\uD83D\uDD25"
            else -> ""
          }
          val headerTitle = if (icon.isNotEmpty()) "$icon $title $icon" else title
          drawSectionHeader(g2d, headerTitle, xPos + CARD_PADDING, y, actualAvailW, TEXT_PRIMARY)

          val labelFont = createFont(Font.PLAIN, 10f)
          val valueFont = createFont(Font.BOLD, 10f)
          var rowY = y + SECTION_HEADER_HEIGHT

          when (val d = colData) {
            is ColumnData.SpellData -> {
              if (d.spells.isEmpty()) {
                g2d.font = labelFont
                g2d.color = toAwtColor(RFColors.TextSecondary)
                g2d.drawString("No data", xPos + CARD_PADDING + 8, rowY + 14)
              } else {
                val spellColor = when {
                  title.contains("Haranya") -> HARANYA_COLOR
                  title.contains("Nuia") -> NUIA_COLOR
                  title.contains("Pirate") -> PIRATE_COLOR
                  else -> toAwtColor(RFColors.dpsOrange)
                }
                for (i in 0 until minOf(d.spells.size, MAX_ROWS)) {
                  g2d.font = labelFont
                  g2d.color = TEXT_PRIMARY
                  g2d.drawString("${i + 1}. ${d.spells[i].spell}", xPos + CARD_PADDING + 8, rowY + 14)
                  g2d.font = valueFont
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
                g2d.font = labelFont
                g2d.color = toAwtColor(RFColors.TextSecondary)
                g2d.drawString("No data", xPos + CARD_PADDING + 8, rowY + 14)
              } else {
                val itemColor = when {
                  title.contains("Haranya") -> HARANYA_COLOR
                  title.contains("Nuia") -> NUIA_COLOR
                  title.contains("Pirate") -> PIRATE_COLOR
                  else -> toAwtColor(RFColors.TextSecondary)
                }
                for (i in 0 until minOf(d.items.size, MAX_ROWS)) {
                  g2d.font = labelFont
                  g2d.color = TEXT_PRIMARY
                  g2d.drawString("${i + 1}. ${d.items[i].itemName}", xPos + CARD_PADDING + 8, rowY + 14)
                  g2d.font = valueFont
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
                g2d.font = labelFont
                g2d.color = toAwtColor(RFColors.TextSecondary)
                g2d.drawString("No data", xPos + CARD_PADDING + 8, rowY + 14)
              } else {
                val buildColor = when {
                  title.contains("Haranya") -> HARANYA_COLOR
                  title.contains("Nuia") -> NUIA_COLOR
                  title.contains("Pirate") -> PIRATE_COLOR
                  else -> toAwtColor(RFColors.TextSecondary)
                }
                d.builds.entries.sortedByDescending { it.value }.take(MAX_ROWS).forEachIndexed { idx, (label, count) ->
                  g2d.font = labelFont
                  g2d.color = TEXT_PRIMARY
                  g2d.drawString("${idx + 1}. ${label}", xPos + CARD_PADDING + 8, rowY + 14)
                  g2d.font = valueFont
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
                g2d.font = labelFont
                g2d.color = toAwtColor(RFColors.TextSecondary)
                g2d.drawString("No data", xPos + CARD_PADDING + 8, rowY + 14)
              } else {
                for (i in 0 until minOf(d.cards.size, MAX_ROWS)) {
                  g2d.font = labelFont
                  g2d.color = TEXT_PRIMARY
                  g2d.drawString("${i + 1}. ${d.cards[i].name}", xPos + CARD_PADDING + 8, rowY + 14)
                  g2d.font = valueFont
                  g2d.color = d.valueColor
                  val valStr = d.getValue(d.cards[i])
                  val bounds = g2d.fontMetrics.getStringBounds(valStr, g2d)
                  g2d.drawString(valStr, xPos + CARD_PADDING + actualAvailW - bounds.width.toInt() - 8, rowY + 14)
                  rowY += ROW_HEIGHT
                }
              }
            }
          }
        }
      }
    }

    tripletBlocks.add(makeTriplet(listOf(
      "Spell Damage Haranya" to ColumnData.SpellData(data.topDamageSpellsHaranya),
      "Spell Damage Nuia" to ColumnData.SpellData(data.topDamageSpellsNuia),
      "Spell Damage Pirate" to ColumnData.SpellData(data.topDamageSpellsPirate),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      "Debuffs" to ColumnData.CardData(data.topDebuffs, { it.sessionDebuffTotal.toString() }, HARANYA_COLOR),
      "Songs" to ColumnData.CardData(data.topSongs, { it.sessionSongsTotal.toString() }, NUIA_COLOR),
      "Buffs" to ColumnData.CardData(data.topBuffs, { it.sessionBuffTotal.toString() }, PIRATE_COLOR),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      "Ode Haranya" to ColumnData.CardData(data.topOdeHaranya, { it.sessionOdeHealsTotal.humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen)),
      "Ode Nuia" to ColumnData.CardData(data.topOdeNuia, { it.sessionOdeHealsTotal.humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen)),
      "Ode Pirate" to ColumnData.CardData(data.topOdePirate, { it.sessionOdeHealsTotal.humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen)),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      "Kills Haranya" to ColumnData.CardData(data.topKillsHaranya, { it.sessionKillTotal.toString() }, Color(0xFF66BB6A.toInt())),
      "Kills Nuia" to ColumnData.CardData(data.topKillsNuia, { it.sessionKillTotal.toString() }, Color(0xFFFFA726.toInt())),
      "Kills Pirate" to ColumnData.CardData(data.topKillsPirate, { it.sessionKillTotal.toString() }, Color(0xFFEF5350.toInt())),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      "Dmg Taken" to ColumnData.CardData(data.topDamageTaken, { it.sessionDamageTakenTotal.toLong().humanReadableAbbreviation() }, toAwtColor(RFColors.dpsOrange)),
      "Heals Received" to ColumnData.CardData(data.topHealsReceived, { it.sessionHealsReceivedTotal.toLong().humanReadableAbbreviation() }, toAwtColor(RFColors.healsGreen)),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      "Items Haranya" to ColumnData.ItemData(data.topItemUsesHaranya),
      "Items Nuia" to ColumnData.ItemData(data.topItemUsesNuia),
      "Items Pirate" to ColumnData.ItemData(data.topItemUsesPirate),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      "Potion Drinkers" to ColumnData.CardData(data.topPotters, { it.sessionPotionTotal.toString() }, Color(0xFF26A69A.toInt())),
      "Glider Gamers" to ColumnData.CardData(data.topGliderGamers, { it.sessionGliderTotal.toString() }, Color(0xFF42A5F5.toInt())),
      "Item Skills" to ColumnData.CardData(data.topItemSkillCasters, { it.sessionItemSkillTotal.toString() }, Color(0xFFFFCA28.toInt())),
    )))

    tripletBlocks.add(makeTriplet(listOf(
      "Builds Haranya" to ColumnData.BuildData(data.buildCountsHaranya),
      "Builds Nuia" to ColumnData.BuildData(data.buildCountsNuia),
      "Builds Pirate" to ColumnData.BuildData(data.buildCountsPirate),
    )))

    return tripletBlocks
  }

  private fun createFont(style: Int, size: Float): Font {
    return try {
      Font("Segoe UI Emoji", style, size.toInt())
    } catch (_: Exception) {
      Font("Segoe UI", style, size.toInt())
    }
  }
}
