package com.reoky.raidframer.core.locale

import java.util.Locale

/**
 * Single source of truth for the user's selectable preferred languages.
 *
 * Each entry maps the short language code stored in `ConfigEntity.preferredLanguage`
 * to a [Locale] that Compose Multiplatform Resources can resolve against
 * `composeResources/values-<lang>-r<REGION>/` directories.
 *
 * IMPORTANT: the resource directories must use the matching Compose Multiplatform
 * qualifier format. A bare `values-cn/` is NOT picked up; the directory must be
 * named `values-zh-rCN/` so the `Locale("zh", "CN")` here resolves correctly.
 */
object AppLocale {
  data class Entry(
    val code: String,
    val locale: Locale,
    val nativeLabel: String
  )

  val SYSTEM_DEFAULT = Entry("", Locale.ROOT, "System Default")

  val ENTRIES: List<Entry> = listOf(
    Entry("en", Locale.forLanguageTag("en"), "English"),
    Entry("au", Locale.forLanguageTag("en-AU"), "Australian"),
    Entry("de", Locale.forLanguageTag("de"), "Deutsch"),
    Entry("br", Locale.forLanguageTag("pt-BR"), "Português"),
    Entry("ru", Locale.forLanguageTag("ru"), "Русский"),
    Entry("cn", Locale.forLanguageTag("zh-CN"), "中文"),
    Entry("kr", Locale.forLanguageTag("ko-KR"), "한국어")
  )

  /**
   * Apply the language preference by setting the JVM default locale.
   * No-op when [code] is blank (use system locale).
   */
  fun apply(code: String) {
    if (code.isBlank()) return
    val locale = ENTRIES.firstOrNull { it.code == code }?.locale ?: return
    Locale.setDefault(locale)
    println("Applied language preference: $code -> ${locale.toLanguageTag()}")
  }

  fun entryFor(code: String): Entry =
    ENTRIES.firstOrNull { it.code == code } ?: SYSTEM_DEFAULT
}
