package com.reoky.raidframer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.WindowStateEntity
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import java.awt.Point
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.Rectangle
import java.awt.Shape
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.*
import java.lang.reflect.Field
import kotlinx.coroutines.delay

/**
 * A CompositionLocal that child composables can use to signal that the window should not
 * be dragged right now (e.g. while the user is interacting with a Slider thumb).
 * Set the value to `true` to lock dragging, `false` to release it.
 */
val LocalDragLock = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

@Composable
fun OverlayWindow(
  title: String,
  overlayType: OverlayType,
  initialPosition: WindowPosition,
  initialSize: DpSize,
  windowType: OverlayWindowType,
  isVisible: MutableState<Boolean>,
  isEverythingVisible: MutableState<Boolean>,
  isResizable: MutableState<Boolean>,
  isFocusable: Boolean,
  transparentBackground: Boolean = false,
  onCloseRequest: () -> Unit,
  onWindowCreated: (ComposeWindow) -> Unit = {}, // callback to deliver the real window
  windowContent: @Composable (ComposeWindow) -> Unit
) {

  val config by RFConfig.state.collectAsState() // injected into every window

  val windowState = rememberWindowState(
    width = initialSize.width.coerceAtLeast(1.dp),
    height = initialSize.height.coerceAtLeast(1.dp),
    position = initialPosition
  )

  Window(
    onCloseRequest = onCloseRequest,
    resizable = isResizable.value,
    state = windowState,
    transparent = true,
    title = title,
    alwaysOnTop = true,
    focusable = isFocusable,
    undecorated = true,
    visible = isVisible.value && isEverythingVisible.value
  ) {

    // custom window shape with rounded corners
    with(LocalDensity.current) {
      window.shape = OverlayWindowShape(
        0.0,
        0.0,
        windowState.size.width.roundToPx().toDouble(),
        windowState.size.height.roundToPx().toDouble(),
        8.0,
        8.0
      )
    }

    // call window creation callback exactly once (when available)
    val composeWindow = this.window
    LaunchedEffect(composeWindow) {
      onWindowCreated(composeWindow)
    }

    // Apply Discord-style overlay behavior whenever the window becomes visible
    LaunchedEffect(window, isVisible.value, isEverythingVisible.value) {
      if (windowType == OverlayWindowType.OVERLAY && isVisible.value && isEverythingVisible.value) {
        delay(100)
        getHWND(window)?.let { windowHandle ->
          makeDiscordStyleOverlay(windowHandle)
          bringToTopmost(windowHandle)
        }
      }
    }

    // Re-assert topmost when the window gains focus/activation. Register these
    // listeners once and remove them with the Compose window lifecycle.
    DisposableEffect(window, windowType) {
      var adapter: WindowAdapter? = null
      if (windowType == OverlayWindowType.OVERLAY) {
        getHWND(window)?.let { hwnd ->
          val listener = object : WindowAdapter() {
            override fun windowActivated(e: WindowEvent?) {
              bringToTopmost(hwnd)
            }
          }
          adapter = listener
          window.addWindowFocusListener(listener)
          window.addWindowListener(listener)
        }
      }
      onDispose {
        adapter?.let {
          window.removeWindowFocusListener(it)
          window.removeWindowListener(it)
        }
      }
    }

    // shift-click mouse listener to allow dragging the window around (tooltips always draggable without shift)
    val dragLocked = remember { mutableStateOf(false) }

    // A tool-tip is always draggable without shift. Whether the Combat overlay drags freely is
    // decided live from config, so toggling tool-tip mode off immediately restores the normal
    // (no-free-drag) overlay behavior without needing a restart.
    val isTooltipWindow: () -> Boolean = {
      if (overlayType == OverlayType.COMBAT) {
        RFConfig.state.value.combatOverlayAsTooltipEnabled
      } else {
        windowType == OverlayWindowType.TOOLTIP
      }
    }

    DisposableEffect(composeWindow) {
      val mouseListener = createMouseListener(windowState, isTooltipWindow) { dragLocked.value }
      composeWindow.addMouseListener(mouseListener)
      composeWindow.addMouseMotionListener(mouseListener)
      onDispose {
        composeWindow.removeMouseListener(mouseListener)
        composeWindow.removeMouseMotionListener(mouseListener)
      }
    }

    CompositionLocalProvider(LocalDragLock provides dragLocked) {
      val windowColor = Color(config.windowColor).copy(alpha = config.windowOpacity)
      val contentBackground = if (transparentBackground) Color.Transparent else windowColor

      // Shared hover state for OVERLAY-type windows
      val overlayInteractionSource = remember { MutableInteractionSource() }
      val isOverlayHovered by overlayInteractionSource.collectIsHoveredAsState()

      LaunchedEffect(isOverlayHovered) {
        if (windowType == OverlayWindowType.OVERLAY) {
          OverlayHoverState.setHovered(overlayType, isOverlayHovered)
        }
      }

      val hoverModifier = if (windowType == OverlayWindowType.OVERLAY) {
        Modifier.hoverable(interactionSource = overlayInteractionSource)
      } else {
        Modifier
      }

      if (windowType == OverlayWindowType.TOOLTIP) {
        Box(
          modifier = Modifier
            .background(contentBackground)
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f))
        ) {
          windowContent(composeWindow)
        }
      } else {
        Box(
          modifier = Modifier
            .background(contentBackground)
            .fillMaxSize()
            .then(hoverModifier)
        ) {
          windowContent(composeWindow)
        }
      }
    }
  }
}

/*
 * A custom shift-click listener that allows the user to drag the window around the screen.
 * Pass a non-null [isDragLocked] lambda to temporarily suppress dragging (e.g. while a
 * Slider thumb is being interacted with). (This is what was making sliders un-slidable friends!)
 */
fun createMouseListener(
  windowState: WindowState,
  isTooltip: () -> Boolean,
  isDragLocked: () -> Boolean = { false } // activate this when friends are interacting with a dragable/slideable control
): MouseAdapter {
  return object : MouseAdapter() {
    private val cornerOffset = Point()
    private var isDragAllowed = false

    // calculate offset from the top-left corner of the window where the user clicked
    override fun mousePressed(e: MouseEvent) {
      isDragAllowed = false
      if (isDragLocked()) return
      if (e.isShiftDown || isTooltip()) {
        // Define a margin for resizing (e.g. 10 pixels)
        val resizeMargin = 10
        val width = e.component.width
        val height = e.component.height

        // Check if the mouse press is within the resize margin
        val isResizeArea = e.x <= resizeMargin || e.x >= width - resizeMargin ||
            e.y <= resizeMargin || e.y >= height - resizeMargin

        // Only allow dragging if we are NOT in the resize area
        if (!isResizeArea) {
          isDragAllowed = true
          cornerOffset.x = e.x
          cornerOffset.y = e.y
        }
      }
    }

    override fun mouseDragged(e: MouseEvent) {
      if (isDragAllowed && !isDragLocked() && (e.isShiftDown || isTooltip())) {
        val newPositionX = e.locationOnScreen.x - cornerOffset.x
        val newPositionY = e.locationOnScreen.y - cornerOffset.y
        windowState.position = WindowPosition(newPositionX.dp, newPositionY.dp)
      }
    }
  }
}

/*
 * A custom overlay window shape that can be used to create a window with rounded corners.
 */
class OverlayWindowShape(
  private val x: Double,
  private val y: Double,
  private val width: Double,
  private val height: Double,
  private val arcWidth: Double,
  private val arcHeight: Double
) : Shape {
  private val roundRect = RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight)

  override fun contains(x: Double, y: Double) = roundRect.contains(x, y)

  override fun contains(x: Double, y: Double, w: Double, h: Double) = roundRect.contains(x, y, w, h)

  override fun intersects(x: Double, y: Double, w: Double, h: Double) = roundRect.intersects(x, y, w, h)

  override fun contains(p: Point2D) = roundRect.contains(p)

  override fun intersects(r: Rectangle2D) = roundRect.intersects(r)

  override fun contains(r: Rectangle2D) = roundRect.contains(r)

  override fun getPathIterator(at: AffineTransform?): PathIterator = roundRect.getPathIterator(at ?: AffineTransform())

  override fun getPathIterator(at: AffineTransform?, flatness: Double): PathIterator =
    roundRect.getPathIterator(at ?: AffineTransform(), flatness)

  override fun getBounds(): Rectangle = roundRect.bounds

  override fun getBounds2D(): Rectangle2D = roundRect.bounds2D
}

enum class OverlayType {
  COMBAT, SETTINGS, SUMMARY, NEW_SESSION, INSTALL, COMPANION, POKEMON, RAID, TRACKER, MINI, ABOUT, HELP, AGGRO, PLAYER_CARD, FILTERS, DUMMY, BATTLE_GRAPH, ITEM_USE, RAID_CALLER, META_SPECS, POCKET_JOURNAL, POCKET_EDITOR
}

enum class OverlayWindowType {
  OVERLAY, TOOLTIP
}

// Default positions and sizes for various overlay types
fun defaultWindowStateForTypeFor(type: OverlayType): WindowStateEntity {
  return when (type) {
    OverlayType.COMBAT -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.OVERLAY,
      lastPositionXDp = 9f,
      lastPositionYDp = 1031f,
      lastWidthDp = 738f,
      lastHeightDp = 204f,
      isVisible = true
    )

    OverlayType.PLAYER_CARD -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 100f,
      lastPositionYDp = 100f,
      lastWidthDp = 750f,
      lastHeightDp = 750f,
      isVisible = false
    )

    OverlayType.SETTINGS -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 1066f,
      lastPositionYDp = 303f,
      lastWidthDp = 560f,
      lastHeightDp = 800f,
      isVisible = false
    )

    OverlayType.HELP -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 700f,
      lastPositionYDp = 120f,
      lastWidthDp = 720f,
      lastHeightDp = 820f,
      isVisible = false
    )

    OverlayType.COMPANION -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 462f,
      lastPositionYDp = 303f,
      lastWidthDp = 580f,
      lastHeightDp = 800f,
      isVisible = false
    )

    OverlayType.POKEMON -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 16f,
      lastPositionYDp = 206f,
      lastWidthDp = 420f,
      lastHeightDp = 420f,
      isVisible = false
    )

    OverlayType.RAID -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 760f,
      lastPositionYDp = 227f,
      lastWidthDp = 1063f,
      lastHeightDp = 1000f,
      isVisible = false
    )

    OverlayType.MINI -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.OVERLAY,
      lastPositionXDp = 1600f,
      lastPositionYDp = 50f,
      lastWidthDp = 380f,
      lastHeightDp = 160f,
      isVisible = false
    )

    OverlayType.TRACKER -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.OVERLAY,
      lastPositionXDp = 800f,
      lastPositionYDp = 50f,
      lastWidthDp = 300f,
      lastHeightDp = 400f,
      isVisible = false
    )

    OverlayType.NEW_SESSION -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 1154f,
      lastPositionYDp = 439f,
      lastWidthDp = 375f,
      lastHeightDp = 600f,
      isVisible = false
    )

    OverlayType.INSTALL -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 900f,
      lastPositionYDp = 300f,
      lastWidthDp = 500f,
      lastHeightDp = 720f,
      isVisible = false
    )

    OverlayType.AGGRO -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.OVERLAY,
      lastPositionXDp = 50f,
      lastPositionYDp = 700f,
      lastWidthDp = 400f,
      lastHeightDp = 300f,
      isVisible = false
    )

    OverlayType.FILTERS -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 600f,
      lastPositionYDp = 700f,
      lastWidthDp = 400f,
      lastHeightDp = 300f,
      isVisible = false
    )

    OverlayType.ABOUT -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 1661f,
      lastPositionYDp = 302f,
      lastWidthDp = 500f,
      lastHeightDp = 740f,
      isVisible = false
    )

    OverlayType.SUMMARY -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 784f,
      lastPositionYDp = 231f,
      lastWidthDp = 1038f,
      lastHeightDp = 935f,
      isVisible = false
    )

    OverlayType.DUMMY -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 0f,
      lastPositionYDp = 0f,
      lastWidthDp = 100f,
      lastHeightDp = 100f,
      isVisible = false
    )

    OverlayType.BATTLE_GRAPH -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 636f,
      lastPositionYDp = 156f,
      lastWidthDp = 1310f,
      lastHeightDp = 1111f,
      isVisible = false
    )

    OverlayType.ITEM_USE -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.OVERLAY,
      lastPositionXDp = 1600f,
      lastPositionYDp = 100f,
      lastWidthDp = 400f,
      lastHeightDp = 300f,
      isVisible = false
    )

    OverlayType.RAID_CALLER -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.OVERLAY,
      lastPositionXDp = 1800f, // near the top/right by default
      lastPositionYDp = 80f,
      lastWidthDp = 420f,
      lastHeightDp = 175f,
      isVisible = false
    )

    OverlayType.META_SPECS -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP, // opaque tool-tip like Settings
      lastPositionXDp = 1066f,
      lastPositionYDp = 303f,
      lastWidthDp = 560f,
      lastHeightDp = 760f,
      isVisible = false
    )

    OverlayType.POCKET_JOURNAL -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 600f,
      lastPositionYDp = 120f,
      lastWidthDp = 900f,
      lastHeightDp = 800f,
      isVisible = true
    )

    OverlayType.POCKET_EDITOR -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 650f,
      lastPositionYDp = 160f,
      lastWidthDp = 1000f,
      lastHeightDp = 800f,
      isVisible = false
    )
  }
}

/*
 * Simply utility to get the HWND from a Java AWT Window. Jetpack doesn't expose this directly
 * because it's supposed to be cross-platform (including Android, which doesn't have HWNDs).
 * This is so when the user alt-tabs it doesn't alt-tab them to a bunch of overlay windows,
 * when really they just want to go back to / leave the game.
 */
fun getHWND(window: Window): HWND? {
  return try {
    val peerField: Field = java.awt.Component::class.java
      .getDeclaredField("peer")
      .apply { isAccessible = true }

    val peer = peerField.get(window) ?: return null

    // Reflectively read hWnd field
    val hwndField = peer.javaClass.getDeclaredField("hWnd")
    hwndField.isAccessible = true

    val hwndValue = hwndField.getLong(peer)

    if (hwndValue != 0L) {
      HWND(Pointer.createConstant(hwndValue))
    } else null
  } catch (e: Exception) {
    null
  }
}

/*
 * Applies the necessary window styles to make an overlay window that doesn't steal focus
 * and behaves similarly to Discord's in-game overlay.
 */
fun makeDiscordStyleOverlay(hwnd: HWND) {
  val user32 = User32.INSTANCE
  val exStyle = user32.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)
  val newStyle = (exStyle or 0x00000080 or WinUser.WS_EX_LAYERED) and 0x00040000.inv()
  user32.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, newStyle)
}

/*
 * Actively brings the window to the top of the Z-order using SetWindowPos with HWND_TOPMOST.
 * This is needed because the game can push the overlay behind it when regaining focus.
 */
fun bringToTopmost(hwnd: HWND) {
  val user32 = User32.INSTANCE
  val SWP_NOSIZE = 0x0001
  val SWP_NOMOVE = 0x0002
  val SWP_NOACTIVATE = 0x0010
  user32.SetWindowPos(hwnd, HWND(Pointer(0xFFFFFFFFL)), 0, 0, 0, 0, SWP_NOSIZE or SWP_NOMOVE or SWP_NOACTIVATE)
}
