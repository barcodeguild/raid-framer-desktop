import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.RootLayout
import viewmodel.RootLayoutModel
import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import java.awt.Shape
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.*
import java.nio.file.Paths

var windowSize = IntSize(478, 192)

class RoundedRectShape(private val x: Double, private val y: Double, private val width: Double, private val height: Double, private val arcWidth: Double, private val arcHeight: Double) : Shape {
    private val roundRect = RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight)

    override fun contains(x: Double, y: Double) = roundRect.contains(x, y)

    override fun contains(x: Double, y: Double, w: Double, h: Double) = roundRect.contains(x, y, w, h)

    override fun intersects(x: Double, y: Double, w: Double, h: Double) = roundRect.intersects(x, y, w, h)

    override fun contains(p: Point2D) = roundRect.contains(p)

    override fun intersects(r: Rectangle2D) = roundRect.intersects(r)

    override fun contains(r: Rectangle2D) = roundRect.contains(r)

    override fun getPathIterator(at: AffineTransform?) = roundRect.getPathIterator(at ?: AffineTransform())

    override fun getPathIterator(at: AffineTransform?, flatness: Double) = roundRect.getPathIterator(at ?: AffineTransform(), flatness)

    override fun getBounds() = roundRect.bounds

    override fun getBounds2D() = roundRect.bounds2D
}

fun main() = application {

    // create the window
    Window(onCloseRequest = ::exitApplication, resizable = false, transparent = true, title = "Raid Framer", alwaysOnTop = true, focusable = false, undecorated = true) {

        window.size = Dimension(windowSize.width, windowSize.height)

        // listen for window movement changes
        val mouseListener = object : MouseAdapter() {
            private val pressedAt = Point()

            override fun mousePressed(e: MouseEvent) {
                if (e.isShiftDown) {
                    pressedAt.x = e.x
                    pressedAt.y = e.y
                }
            }

            override fun mouseDragged(e: MouseEvent) {
                if (e.isShiftDown) {
                    window.location = Point(e.xOnScreen - pressedAt.x, e.yOnScreen - pressedAt.y)
                }
            }
        }

        window.addMouseListener(mouseListener)
        window.addMouseMotionListener(mouseListener)

        window.shape = RoundedRectShape(0.0, 0.0, windowSize.width.toDouble(), windowSize.height.toDouble(), 16.0, 16.0)
        window.isVisible = true

        Box(modifier = Modifier
            .background(androidx.compose.ui.graphics.Color(0f, 0f, 0f, 0.43f))
            .fillMaxSize()
        ) {
            RootLayout(RootLayoutModel())
        }
    }

    Window(onCloseRequest = ::exitApplication, resizable = false, title = "Raid Framer Settings", alwaysOnTop = true, focusable = true) {
        window.size = Dimension(100, 100)
        window.location = Point(100, 100)
        window.isVisible = true

        val combatInteractor: CombatEventInteractor = CombatEventInteractor() {
            println(it)
        }

        fun exitApplication() {
            combatInteractor.stop()
            System.exit(0)
        }

        combatInteractor.start()
        Box { Text("Hi") }
    }

}
