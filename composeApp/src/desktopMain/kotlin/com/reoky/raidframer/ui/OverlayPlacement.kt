package com.reoky.raidframer.ui

import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit

internal fun monitorWorkAreas(): List<Rectangle> {
  val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
  return (listOf(environment.defaultScreenDevice) + environment.screenDevices).distinct().map { device ->
    val config = device.defaultConfiguration
    val bounds = config.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
    Rectangle(bounds.x + insets.left, bounds.y + insets.top,
      bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom)
  }
}

internal fun reachablePosition(window: Rectangle, areas: List<Rectangle>): Point {
  val strip = Rectangle(window.x, window.y, window.width, minOf(24, window.height))
  return if (areas.any { area ->
      val visible = area.intersection(strip)
      visible.width >= minOf(48, strip.width) && visible.height >= strip.height
    }) window.location else areas.first().location
}