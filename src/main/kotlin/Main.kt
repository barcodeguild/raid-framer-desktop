import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import core.helpers.getScreenSizeInDp
import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import ui.overlay.CombatOverlay
import ui.OverlayWindow
import ui.overlay.AggroOverlay
import ui.overlay.SettingsOverlay
import java.awt.Image
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

var windowSize = IntSize(478, 192)


fun main() = application {

  val appState = AppState


  // initalize the database
//  val schema = setOf()
//  val realm = RealmConfiguration.Builder(schema)
//    .name("raidframer.realm")
//    .schemaVersion(1)
//    .build()


  val iconImage = painterResource("raidframer.ico").toAwtImage(
    density = Density(1f),
    layoutDirection = LayoutDirection.Ltr,
    size = Size(32f, 32f)
  )

  /*
   * Starts the application by attaching the interactors and listeners.
   */
  CombatEventInteractor.start()
  OverlayInteractor.start()

  // determine screen size in dp
  val screenSize = getScreenSizeInDp()
  println(screenSize)

  fun loadImageFromDisk(path: String): Image {
    val bufferedImage = ImageIO.read(File(path))
    return bufferedImage.getScaledInstance(-1, -1, Image.SCALE_SMOOTH)
  }

  // *puts system tray entry*
  val tray = SystemTray.get()

  // menu title with icon
  val titleMenuItem = MenuItem(".: Raid Framer :.")
  titleMenuItem.setImage(iconImage)
  tray.menu.add(titleMenuItem)

  // settings and exit buttons
  tray.menu.add(MenuItem("Settings") {
    AppState.isSettingsOverlayVisible.value = true
  })
  tray.menu.add(MenuItem("Exit") {
    exitApplication()
  })

  tray.setImage(iconImage)

  /*
   * Performs tear-down and exits the application.
   */
  fun exitApplication() {
    CombatEventInteractor.stop()
    OverlayInteractor.stop()
    tray.shutdown()
    tray.remove()
    exitProcess(0)
  }

  /* Shows combat-related statistics for the raid. */
  OverlayWindow(
    ".: Raid Framer Combat Overlay :.",
    initialPosition = WindowPosition(64.dp, 768.dp),
    initialSize = DpSize(512.dp, 256.dp),
    isVisible = AppState.isEverythingVisible,
    isEverythingVisible = AppState.isEverythingVisible,
    isResizable = AppState.isEverythingResizable,
    ::exitApplication
  ) {
    CombatOverlay()
  }

  /* Shows who currently has boss aggro. */
  //  OverlayWindow(
  //    ".: Raid Framer Boss Aggro Overlay :.",
  //    initialPosition = WindowPosition(256.dp, 64.dp),
  //    initialSize = DpSize(384.dp, 128.dp),
  //    isVisible = AppState.isEverythingVisible,
  //    isEverythingVisible = AppState.isEverythingVisible,
  //    isResizable = AppState.isEverythingResizable,
  //    ::exitApplication
  //  ) {
  //    AggroOverlay()
  //  }

  OverlayWindow(
    ".: Raid Framer Settings :.",
    initialPosition = WindowPosition(512.dp, 512.dp),
    initialSize = DpSize(480.dp, 512.dp),
    isVisible = AppState.isSettingsOverlayVisible,
    isEverythingVisible = mutableStateOf(true), // always visible because it's a settings window
    isResizable = AppState.isEverythingResizable,
    ::exitApplication
  ) {
    SettingsOverlay()
  }
}