package com.reoky.raidframer.core.config

import com.reoky.raidframer.core.helpers.getRaidFramerDirectory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object RenderingBackendConfig {
  private const val FILE_NAME = "rendering.ini"
  private const val KEY = "renderApi"
  private const val DEFAULT = "OPENGL"
  private val allowed = setOf("OPENGL", "DIRECTX", "SOFTWARE")

  fun load(): String {
    val path = path() ?: return DEFAULT
    val value = runCatching {
      Files.readAllLines(path, StandardCharsets.UTF_8)
        .asSequence()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith(";") }
        .firstOrNull { it.substringBefore('=').trim().equals(KEY, ignoreCase = true) }
        ?.substringAfter('=', "")
        ?.trim()
        ?.uppercase()
    }.getOrNull()
    return value?.takeIf(allowed::contains) ?: DEFAULT
  }

  fun save(value: String) {
    val normalized = value.uppercase().takeIf(allowed::contains) ?: DEFAULT
    val path = path() ?: return
    runCatching {
      path.parent?.let(Files::createDirectories)
      Files.writeString(
        path,
        "# Raid Framer rendering backend\n# Valid values: OPENGL, DIRECTX, SOFTWARE\n$KEY=$normalized\n",
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
      )
    }
  }

  private fun path(): Path? = getRaidFramerDirectory()?.resolve(FILE_NAME)
}
