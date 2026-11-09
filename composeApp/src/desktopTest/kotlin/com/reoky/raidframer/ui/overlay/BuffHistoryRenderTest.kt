package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.ConfigDao
import com.reoky.raidframer.core.database.ConfigEntity
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.ui.component.graphs.BuffHistoryDataPoint
import com.reoky.raidframer.ui.component.graphs.BuffHistoryGraph
import com.reoky.raidframer.ui.component.graphs.BuffHistoryMode
import com.reoky.raidframer.ui.component.graphs.BuffHistoryPlayerStatus
import com.reoky.raidframer.ui.component.graphs.BuffHistorySnapshotCard
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.raid_buff_history_section_snapshots
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

private const val START_MS = 1_760_000_000_000L
private const val PLAYER_COUNT = 20

private val roster = listOf(
  "Aelith", "Brynn", "Cassian", "Dorian", "Elowen",
  "Fennwick", "Galen", "Hesper", "Isolde", "Jorvan",
  "Kaelis", "Lirael", "Marek", "Nyra", "Orenthal",
  "Pell", "Quillon", "Rhea", "Sable", "Tamsin"
)

private fun statuses(buffed: Int, unknown: Int): Map<String, BuffHistoryPlayerStatus> =
  roster.mapIndexed { index, name ->
    name to when {
      index % 4 == 3 && index < unknown * 4 -> BuffHistoryPlayerStatus.NOT_SCANNABLE
      index < buffed -> BuffHistoryPlayerStatus.BUFFED
      else -> BuffHistoryPlayerStatus.NOT_BUFFED
    }
  }.toMap()

private fun cardAt(offsetMs: Long, buffed: Int, unknown: Int): BuffHistorySnapshotCard {
  val status = statuses(buffed, unknown)
  return BuffHistorySnapshotCard(
    timestamp = START_MS + offsetMs,
    playerNameToStatus = status,
    playerNameToBuffIds = status.filterValues { it != BuffHistoryPlayerStatus.NOT_SCANNABLE }
      .keys.associateWith { setOf(100, 200, 300) }
  )
}

private val graphPoints: List<BuffHistoryDataPoint> = (0 until 34).map { i ->
  val buffed = (11 + (i % 6)).coerceAtMost(PLAYER_COUNT)
  val unknown = if (i % 7 == 0) 3 else 1
  BuffHistoryDataPoint(
    timestamp = START_MS + i * 60_000L,
    buffedCount = buffed,
    notScannableCount = unknown,
    unbuffedCount = PLAYER_COUNT - buffed - unknown,
    totalPlayers = PLAYER_COUNT,
    lootPercentSum = 0,
    lootPercentCount = 0,
    individualBuffCounts = emptyMap()
  )
}

private fun playerCard(name: String, build: String, faction: String): PlayerCard = PlayerCard(
  name = name,
  recentCids = emptyList(),
  lastEvent = 0L,
  lastKnownFaction = faction,
  currentBuild = build,
  cache = null
)

private val playerCards: Map<String, PlayerCard> = roster.mapIndexed { index, name ->
  name to playerCard(
    name = name,
    build = when (index % 4) {
      0 -> "BASTION"
      1 -> "ABOLISHER"
      2 -> "ARCANE_HUNTER"
      else -> "BATTLEPRIEST"
    },
    faction = when (index % 3) {
      0 -> "Nuia"
      1 -> "Haranya"
      else -> "Pirate"
    }
  )
}.toMap()

/** Mirrors the Buff History tab's layout at the raid overlay's default width, using the real pieces. */
@Composable
private fun BuffHistoryHarness() {
  val cards = listOf(
    cardAt(0L, 12, 4),
    cardAt(60_000L, 14, 3),
    cardAt(120_000L, 16, 2),
    cardAt(180_000L, 17, 1),
    cardAt(240_000L, 18, 1)
  )
  val refStart = cards.first().timestamp

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      BuffHistorySelectButton(
        value = "All Selected Buffs",
        active = false,
        expanded = false,
        onClick = {},
        modifier = Modifier.widthIn(max = 170.dp)
      )
      BuffHistorySelectButton(
        value = "Light PvP",
        active = true,
        expanded = false,
        onClick = {},
        modifier = Modifier.widthIn(max = 160.dp)
      )
      BuffHistorySelectButton(
        value = "Select Criteria",
        active = false,
        expanded = true,
        onClick = {},
        modifier = Modifier.widthIn(max = 170.dp)
      )
    }

    Box(modifier = Modifier.fillMaxWidth().weight(3f)) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(BuffHistoryPanelColor, RoundedCornerShape(4.dp))
          .border(1.dp, BuffHistoryHairline, RoundedCornerShape(4.dp))
      ) {
        BuffHistoryGraph(
          dataPoints = graphPoints,
          mode = BuffHistoryMode.ALL_BUFFS,
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 3.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = stringResource(Res.string.raid_buff_history_section_snapshots).uppercase(),
        color = Color(0xFF8B8B8B),
        fontSize = 9.sp,
        letterSpacing = 1.sp,
        maxLines = 1
      )
      Box(modifier = Modifier.weight(1f).height(1.dp).background(BuffHistoryHairline))
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(4f)
        .verticalScroll(rememberScrollState())
        .padding(bottom = 4.dp)
    ) {
      BuffHistorySnapshotList(
        cards = cards,
        refStart = refStart,
        playerCards = playerCards,
        showSpecIcons = true,
        onPlayerClick = {},
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@OptIn(ExperimentalTestApi::class)
class BuffHistoryRenderTest {

  private class FakeConfigDao(private val entity: ConfigEntity) : ConfigDao {
    override suspend fun getConfig(): ConfigEntity? = entity
    override suspend fun insert(config: ConfigEntity) {}
  }

  @Test
  fun renderBuffHistoryForReview() {
    renderTo("build/review/buff_history_render.png") {
      Box(modifier = Modifier.fillMaxSize().background(Color(0xCC121212))) {
        BuffHistoryHarness()
      }
    }
  }

  /** Narrower overlay width: columns collapse and the header has less room to work with. */
  @Test
  fun renderBuffHistoryAtNarrowWidth() {
    renderTo("build/review/buff_history_render_narrow.png") {
      Box(modifier = Modifier.requiredSize(700.dp, 768.dp).background(Color(0xCC121212))) {
        BuffHistoryHarness()
      }
    }
  }

  private fun renderTo(path: String, content: @Composable () -> Unit) {
    // Nothing to capture without a display (e.g. a headless CI agent).
    if (java.awt.GraphicsEnvironment.isHeadless()) return

    RFConfig.init(FakeConfigDao(ConfigEntity().copy(playerFaction = "Nuia")))
    RFConfig.initSync()

    runComposeUiTest {
      setContent { content() }
      waitForIdle()

      val pixels = onRoot().captureToImage().toPixelMap()
      val width = pixels.width
      val height = pixels.height
      val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      var painted = 0
      for (x in 0 until width) {
        for (y in 0 until height) {
          val c = pixels[x, y]
          val a = (c.alpha * 255).toInt()
          val r = (c.red * 255).toInt()
          val g = (c.green * 255).toInt()
          val b = (c.blue * 255).toInt()
          if (a > 0) painted++
          image.setRGB(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
        }
      }
      // The tab must actually compose and paint something.
      assertTrue(painted > width * height / 2, "expected a painted surface, got $painted px")

      val out = File(path)
      out.parentFile.mkdirs()
      ImageIO.write(image, "png", out)
      println("saved ${out.absolutePath} (${width}x${height})")
    }
  }
}
