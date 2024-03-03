import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.CombatOverlayLayout
import ui.RoundedRectShape
import viewmodel.CombatOverlayModel
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

var windowSize = IntSize(478, 192)

fun main() = application {

    val icon = painterResource("desktop.ico")

    /* Main Overlay Window */
    Window(onCloseRequest = ::exitApplication, resizable = false, transparent = true, title = "Raid Framer Overlay", alwaysOnTop = true, focusable = false, undecorated = true) {

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
            .background(Color(0f, 0f, 0f, 0.43f))
            .fillMaxSize()
        ) {
            CombatOverlayLayout(CombatOverlayModel())
        }
    }

    Window(onCloseRequest = ::exitApplication, icon = icon, resizable = false, title = "Raid Framer Settings", alwaysOnTop = true, focusable = true) {
        window.size = Dimension(1000, 1000)
        window.location = Point(100, 100)
        window.isVisible = true

        val combatInteractor: CombatEventInteractor = CombatEventInteractor() {
            //println(it)
        }

        fun exitApplication() {
            combatInteractor.stop()
            System.exit(0)
        }

        combatInteractor.start()
        Box { Text("Hi") }
    }

}
