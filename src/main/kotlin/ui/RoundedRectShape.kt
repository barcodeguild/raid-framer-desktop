package ui

import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D

class RoundedRectShape(private val x: Double, private val y: Double, private val width: Double, private val height: Double, private val arcWidth: Double, private val arcHeight: Double) :
  Shape {
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