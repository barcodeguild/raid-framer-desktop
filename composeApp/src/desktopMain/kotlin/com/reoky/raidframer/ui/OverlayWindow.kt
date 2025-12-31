package com.reoky.raidframer.ui

import androidx.compose.foundation.background
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
import com.reoky.raidframer.core.database.WindowStateEntity
import java.awt.Point
import java.awt.Rectangle
import java.awt.Shape
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.*
import kotlin.compareTo
import kotlin.text.compareTo

@Composable
fun OverlayWindow(
  title: String,
  initialPosition: WindowPosition,
  initialSize: DpSize,
  windowType: OverlayWindowType,
  isObstructing: MutableState<Boolean>,
  isVisible: MutableState<Boolean>,
  isEverythingVisible: MutableState<Boolean>,
  isResizable: MutableState<Boolean>,
  isFocusable: Boolean,
  onCloseRequest: () -> Unit,
  onWindowCreated: (ComposeWindow) -> Unit = {}, // callback to deliver the real window
  windowContent: @Composable (ComposeWindow) -> Unit
) {
  val windowState = rememberWindowState(
    width = initialSize.width.coerceIn(0.dp, 1000.dp),
    height = initialSize.height.coerceIn(0.dp, 1000.dp),
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
    visible = isVisible.value && isEverythingVisible.value && !isObstructing.value
  ) {

    // custom window shape with rounded corners
    with(LocalDensity.current) {
//      val widthPx = windowState.size.width.roundToPx().coerceIn(0, 10000)
//      val heightPx = windowState.size.height.roundToPx().coerceIn(0, 10000)
//      window.setBounds(
//        windowState.position.x.roundToPx(),
//        windowState.position.y.roundToPx(),
//        widthPx,
//        heightPx
//      )
      window.shape = OverlayWindowShape(
        0.0,
        0.0,
        windowState.size.width.roundToPx().toDouble(),
        windowState.size.height.roundToPx().toDouble(),
        8.0,
        8.0
      )
      //OverlayInteractor.updateWindowStateFor(overlayType, windowState)
    }

    // call window creation callback exactly once (when available)
    val composeWindow = this.window
    LaunchedEffect(composeWindow) {
      onWindowCreated(composeWindow)
    }

    // shift-click mouse listener to allow dragging the window around (tooltips always draggable without shift)
    val mouseListener = createMouseListener(windowState, windowType)
    composeWindow.addMouseListener(mouseListener)
    composeWindow.addMouseMotionListener(mouseListener)

    Box(
      modifier = Modifier
        .background(Color(0f, 0f, 0f, 0.43f))
        .fillMaxSize()
    ) {
      windowContent(composeWindow)
    }
  }
}

/*
 * A custom shift-click listener that allows the user to drag the window around the screen.
 */
fun createMouseListener(windowState: WindowState, overlayWindowType: OverlayWindowType): MouseAdapter {
  return object : MouseAdapter() {
    private val cornerOffset = Point()
    private var isDragAllowed = false

    // calculate offset from the top-left corner of the window where the user clicked
    override fun mousePressed(e: MouseEvent) {
      isDragAllowed = false
      if (e.isShiftDown || overlayWindowType == OverlayWindowType.TOOLTIP) {
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
      if (isDragAllowed && (e.isShiftDown || overlayWindowType == OverlayWindowType.TOOLTIP)) {
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
  COMBAT, SETTINGS, COMPANION, TRACKER, MINI, ABOUT, AGGRO, SUMMARY, FILTERS, DUMMY
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
      lastPositionXDp = 50f,
      lastPositionYDp = 500f,
      lastWidthDp = 650f,
      lastHeightDp = 192f,
      isVisible = true
    )

    OverlayType.SUMMARY -> WindowStateEntity(
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
      lastPositionXDp = 500f,
      lastPositionYDp = 70f,
      lastWidthDp = 560f,
      lastHeightDp = 800f,
      isVisible = true
    )

    OverlayType.COMPANION -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.TOOLTIP,
      lastPositionXDp = 500f,
      lastPositionYDp = 70f,
      lastWidthDp = 560f,
      lastHeightDp = 800f,
      isVisible = false
    )

    OverlayType.MINI -> WindowStateEntity(
      overlayType = type.name,
      windowType = OverlayWindowType.OVERLAY,
      lastPositionXDp = 800f,
      lastPositionYDp = 50f,
      lastWidthDp = 380f,
      lastHeightDp = 160f,
      isVisible = true
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
      lastPositionXDp = 300f,
      lastPositionYDp = 300f,
      lastWidthDp = 500f,
      lastHeightDp = 720f,
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
  }
}