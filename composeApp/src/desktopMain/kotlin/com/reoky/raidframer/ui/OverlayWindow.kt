package com.reoky.raidframer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
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

@Composable
fun OverlayWindow(
  title: String,
  initialPosition: WindowPosition,
  initialSize: DpSize,
  overlayType: OverlayType,
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
    // provide the real ComposeWindow to the caller exactly once (when available)
    val composeWindow = this.window
    LaunchedEffect(composeWindow) {
      onWindowCreated(composeWindow)
    }

    val mouseListener = createMouseListener(windowState)
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
fun createMouseListener(windowState: WindowState): MouseAdapter {
  return object : MouseAdapter() {
    private val cornerOffset = Point()

    // calculate offset from the top-left corner of the window where the user clicked
    override fun mousePressed(e: MouseEvent) {
      if (e.isShiftDown) {
        cornerOffset.x = e.x
        cornerOffset.y = e.y
      }
    }

    override fun mouseDragged(e: MouseEvent) {
      if (e.isShiftDown) {
        if (cornerOffset.x == 0 || cornerOffset.y == 0) return
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
  COMBAT, SETTINGS, TRACKER, ABOUT, AGGRO, SUMMARY, FILTERS, DUMMY
}

// Default positions and sizes for various overlay types
fun defaultWindowStateForTypeFor(type: OverlayType): WindowStateEntity {
  return when (type) {
    OverlayType.COMBAT -> WindowStateEntity(
      overlayType = type.name,
      lastPositionXDp = 50f,
      lastPositionYDp = 500f,
      lastWidthDp = 650f,
      lastHeightDp = 192f,
      isVisible = true
    )
    OverlayType.SUMMARY -> WindowStateEntity(
      overlayType = type.name,
      lastPositionXDp = 100f,
      lastPositionYDp = 100f,
      lastWidthDp = 750f,
      lastHeightDp = 750f,
      isVisible = true
    )

    OverlayType.SETTINGS -> WindowStateEntity(
      overlayType = type.name,
      lastPositionXDp = 200f,
      lastPositionYDp = 200f,
      lastWidthDp = 500f,
      lastHeightDp = 500f,
      isVisible = false
    )

    OverlayType.TRACKER -> WindowStateEntity(
      overlayType = type.name,
      lastPositionXDp = 800f,
      lastPositionYDp = 50f,
      lastWidthDp = 300f,
      lastHeightDp = 400f,
      isVisible = false
    )

    OverlayType.AGGRO -> WindowStateEntity(
      overlayType = type.name,
      lastPositionXDp = 50f,
      lastPositionYDp = 700f,
      lastWidthDp = 400f,
      lastHeightDp = 300f,
      isVisible = false
    )

    OverlayType.FILTERS -> WindowStateEntity(
      overlayType = type.name,
      lastPositionXDp = 600f,
      lastPositionYDp = 700f,
      lastWidthDp = 400f,
      lastHeightDp = 300f,
      isVisible = false
    )

    OverlayType.ABOUT -> WindowStateEntity(
      overlayType = type.name,
      lastPositionXDp = 300f,
      lastPositionYDp = 300f,
      lastWidthDp = 480f,
      lastHeightDp = 700f,
      isVisible = true
    )

    OverlayType.DUMMY -> WindowStateEntity(
      overlayType = type.name,
      lastPositionXDp = 0f,
      lastPositionYDp = 0f,
      lastWidthDp = 100f,
      lastHeightDp = 100f,
      isVisible = false
    )
  }
}