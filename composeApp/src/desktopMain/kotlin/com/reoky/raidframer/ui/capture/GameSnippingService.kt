package com.reoky.raidframer.ui.capture

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Dialog
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.SwingUtilities
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Provides a minimal snipping-tool style selection over the desktop. */
object GameSnippingService {
  suspend fun capture(
    windowsToHide: Collection<Window> = emptyList(),
  ): BufferedImage? = suspendCoroutine { continuation ->
    val hidden = windowsToHide.filter { it.isShowing }
    SwingUtilities.invokeLater {
      hidden.forEach { it.isVisible = false }
      val bounds = desktopBounds()
      val dialog = JDialog(null as java.awt.Frame?, Dialog.ModalityType.APPLICATION_MODAL).apply {
        isUndecorated = true
        isAlwaysOnTop = true
        setBounds(bounds)
        background = Color(0, 0, 0, 1)
      }
      val surface = SelectionSurface()
      surface.cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
      dialog.contentPane = surface
      surface.onComplete = { selection ->
        dialog.dispose()
        hidden.forEach { it.isVisible = true }
        continuation.resume(selection?.let { Robot().createScreenCapture(it) })
      }
      dialog.isVisible = true
    }
  }

  private fun desktopBounds(): Rectangle {
    return GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
      .map { device -> device.defaultConfiguration.bounds }
      .reduce { first, next -> first.union(next) }
  }

  private class SelectionSurface : JComponent() {
    var onComplete: (Rectangle?) -> Unit = {}
    private var start: Point? = null
    private var current: Point? = null

    init {
      isOpaque = false
      val listener = object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
          if (event.button == MouseEvent.BUTTON1) {
            start = event.point
            current = event.point
            repaint()
          }
        }

        override fun mouseDragged(event: MouseEvent) {
          current = event.point
          repaint()
        }

        override fun mouseReleased(event: MouseEvent) {
          if (event.button == MouseEvent.BUTTON1) {
            current = event.point
            val selected = selection()
            onComplete(selected?.takeIf { it.width > 2 && it.height > 2 })
          }
        }
      }
      addMouseListener(listener)
      addMouseMotionListener(listener)
    }

    override fun paintComponent(graphics: java.awt.Graphics) {
      super.paintComponent(graphics)
      val g = graphics.create() as java.awt.Graphics2D
      g.color = Color(0, 0, 0, 125)
      g.fillRect(0, 0, width, height)
      selection()?.let {
        g.color = Color(255, 255, 255, 40)
        g.fill(it)
        g.color = Color.WHITE
        g.stroke = BasicStroke(2f)
        g.draw(it)
      }
      g.dispose()
    }

    private fun selection(): Rectangle? {
      val first = start ?: return null
      val second = current ?: return null
      return Rectangle(
        minOf(first.x, second.x),
        minOf(first.y, second.y),
        kotlin.math.abs(first.x - second.x),
        kotlin.math.abs(first.y - second.y)
      )
    }
  }
}
