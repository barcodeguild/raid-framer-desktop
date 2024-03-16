package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import java.awt.Dimension
import java.awt.Point
import java.awt.Shape
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D

@Composable
fun OverlayWindow(
  title: String, initialPosition: WindowPosition, initialSize: DpSize,
  isVisible: MutableState<Boolean>, isEverythingVisible: MutableState<Boolean>, isResizable: MutableState<Boolean>,
  onCloseRequest: () -> Unit, windowContent: @Composable (Window) -> Unit
) {
  val windowState = rememberWindowState(
    width = initialSize.width,
    height = initialSize.height,
    position = initialPosition
  )
  Window(
    onCloseRequest = onCloseRequest, resizable = isResizable.value, state = windowState, transparent = true,
    title = title, alwaysOnTop = true, focusable = false, undecorated = true,
    visible = isVisible.value && isEverythingVisible.value
  ) {
    with(LocalDensity.current) {
      window.setBounds(
        windowState.position.x.roundToPx(),
        windowState.position.y.roundToPx(),
        windowState.size.width.roundToPx(),
        windowState.size.height.roundToPx()
      )
      window.shape = OverlayWindow(
        0.0,
        0.0,
        windowState.size.width.roundToPx().toDouble(),
        windowState.size.height.roundToPx().toDouble(),
        8.0,
        8.0
      )
    }
    val mouseListener = createMouseListener(windowState)
    window.addMouseListener(mouseListener)
    window.addMouseMotionListener(mouseListener)
    Box(
      modifier = Modifier
        .background(Color(0f, 0f, 0f, 0.43f))
        .fillMaxSize()
    ) {
      windowContent(window)
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
class OverlayWindow(
  private val x: Double,
  private val y: Double,
  private val width: Double,
  private val height: Double,
  private val arcWidth: Double,
  private val arcHeight: Double
) :
  Shape {
  private val roundRect = RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight)

  override fun contains(x: Double, y: Double) = roundRect.contains(x, y)

  override fun contains(x: Double, y: Double, w: Double, h: Double) = roundRect.contains(x, y, w, h)

  override fun intersects(x: Double, y: Double, w: Double, h: Double) = roundRect.intersects(x, y, w, h)

  override fun contains(p: Point2D) = roundRect.contains(p)

  override fun intersects(r: Rectangle2D) = roundRect.intersects(r)

  override fun contains(r: Rectangle2D) = roundRect.contains(r)

  override fun getPathIterator(at: AffineTransform?) = roundRect.getPathIterator(at ?: AffineTransform())

  override fun getPathIterator(at: AffineTransform?, flatness: Double) =
    roundRect.getPathIterator(at ?: AffineTransform(), flatness)

  override fun getBounds() = roundRect.bounds

  override fun getBounds2D() = roundRect.bounds2D
}