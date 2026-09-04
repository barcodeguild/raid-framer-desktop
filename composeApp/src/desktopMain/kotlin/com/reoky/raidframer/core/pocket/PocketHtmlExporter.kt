package com.reoky.raidframer.core.pocket

import com.reoky.raidframer.core.helpers.getExportDirectory
import com.reoky.raidframer.core.helpers.writeTextAtomically
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.Image
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.AttributeProvider
import org.commonmark.renderer.html.AttributeProviderContext
import org.commonmark.renderer.html.AttributeProviderFactory
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Exports a complete [PocketEntry] to a self-contained folder of HTML under
 * `RFExports/<year>/<month>/<journal title> <timestamp>/`. Attachments referenced by the
 * markdown are copied alongside the generated `index.html` so the entry renders fully offline.
 * A human-readable creation timestamp is appended to the folder name so entries that share a
 * journal title never collide.
 */
object PocketHtmlExporter {

  private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ROOT)
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.ROOT)
  private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

  private val parser by lazy {
    Parser.builder().extensions(listOf(StrikethroughExtension.create(), TablesExtension.create(), TaskListItemsExtension.create())).build()
  }

  private val renderer by lazy {
    HtmlRenderer.builder()
      .attributeProviderFactory(ImageSrcAttributeProviderFactory)
      .build()
  }

  /** Builds the journal folder for an entry and returns its path, or null when it cannot be resolved. */
  fun exportFolderFor(entry: PocketEntry): Path? {
    val root = getExportDirectory()?.let(Path::of) ?: return null
    val date = Instant.ofEpochMilli(entry.metadata.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val base = safeFolderName(entry.metadata.title.ifBlank { "Journal ${dateFormatter.format(date)}" })
    val timestamp = timestampFormatter.format(
      Instant.ofEpochMilli(entry.metadata.createdAt).atZone(ZoneId.systemDefault())
    )
    return root.resolve(date.year.toString()).resolve("%02d".format(date.monthValue)).resolve("$base $timestamp")
  }

  /**
   * Exports [entry] to [target] (created when needed), copying its attachments and writing
   * `index.html`. Returns the target folder on success, or null on failure.
   */
  fun exportEntryToHtml(entry: PocketEntry, target: Path? = null): Path? {
    val folder = target ?: exportFolderFor(entry) ?: return null
    return runCatching {
      Files.createDirectories(folder)
      // Copy referenced attachments so the images render from the exported folder.
      entry.attachments.forEach { attachment ->
        val sourceRoot = Path.of(entry.metadata.markdownPath).parent
        val source = sourceRoot.resolve(attachment.relativePath).normalize()
        val destination = folder.resolve(attachment.relativePath).normalize()
        if (source.startsWith(sourceRoot) && Files.isRegularFile(source) && destination.startsWith(folder)) {
          destination.parent?.let(Files::createDirectories)
          Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
        }
      }
      writeTextAtomically(folder.resolve("index.html"), renderHtml(entry))
      folder
    }.getOrNull()
  }

  private fun renderHtml(entry: PocketEntry): String {
    val title = escape(entry.metadata.title.ifBlank { "Untitled entry" })
    val created = escape(dateTimeFormatter.format(entry.metadata.createdAt.toZonedDateTime()))
    val tags = entry.tags.joinToString("\n") { "    <span class=\"tag\">${escape("#${it.tag}")}</span>" }
    // Render markdown to standard HTML via CommonMark; image srcs are rewritten by
    // ImageSrcAttributeProvider so attachments resolve from this folder.
    val body = renderMarkdownWithPipeTables(entry.markdown)
      .replace(Regex("(?i)<li>\\s*<p>\\s*\\[x\\]\\s*"), "<li class=\"task-list-item\"><input type=\"checkbox\" disabled checked> ")
      .replace(Regex("<li>\\s*<p>\\s*\\[ \\]\\s*"), "<li class=\"task-list-item\"><input type=\"checkbox\" disabled> ")
      .replace(Regex("(?i)<li>\\s*\\[x\\]\\s*"), "<li class=\"task-list-item\"><input type=\"checkbox\" disabled checked> ")
      .replace(Regex("<li>\\s*\\[ \\]\\s*"), "<li class=\"task-list-item\"><input type=\"checkbox\" disabled> ")
    return """
      |<!doctype html>
      |<html lang="en">
      |<head>
      |  <meta charset="utf-8">
      |  <meta name="viewport" content="width=device-width, initial-scale=1">
      |  <title>$title</title>
      |  <style>
      |    :root { color-scheme: light; }
      |    body { font-family: -apple-system, 'Segoe UI', Roboto, Arial, sans-serif; max-width: 860px; margin: 0 auto; padding: 28px 20px; color: #1c1c1e; line-height: 1.6; }
      |    h1 { margin-bottom: 4px; }
      |    .meta { color: #6b6b70; font-size: 14px; margin-bottom: 20px; }
      |    .tags { margin-bottom: 20px; }
    |    .tag { display: inline-block; background: #eef0f3; border-radius: 8px; padding: 2px 8px; margin-right: 6px; font-size: 12px; color: #444; }
    |    table { border-collapse: collapse; width: 100%; margin: 16px 0; }
    |    th, td { border: 1px solid #d5d7dc; padding: 7px 9px; text-align: left; }
    |    th { background: #f1f2f4; }
    |    .task-list-item { list-style: none; margin-left: -1.25em; }
    |    .task-list-item input { margin-right: 0.45em; }
      |    img { max-width: min(100%, 480px); height: auto; display: block; margin: 12px auto; border-radius: 6px; border: 1px solid #ddd; }
      |    blockquote { margin-left: 0; padding-left: 14px; border-left: 3px solid #c9cdd3; color: #555; }
      |    pre { background: #f4f4f6; padding: 12px; border-radius: 6px; overflow-x: auto; }
      |    a { color: #2f6fd0; }
      |  </style>
      |</head>
      |<body>
      |  <h1>$title</h1>
      |  <div class="meta">$created</div>
      |  ${if (tags.isNotEmpty()) "<div class=\"tags\">$tags\n  </div>" else ""}
      |  <hr>
      |  $body
      |</body>
      |</html>
    """.trimMargin()
  }

  private fun renderMarkdownWithPipeTables(markdown: String): String {
    val lines = markdown.lines()
    val output = StringBuilder()
    var index = 0
    while (index < lines.size) {
      val header = lines[index].trim()
      val divider = lines.getOrNull(index + 1)?.trim()
      if (header.startsWith("|") && header.endsWith("|") && divider != null &&
        divider.startsWith("|") && divider.endsWith("|") &&
        divider.trim('|').split('|').all { it.trim().matches(Regex(":?-{3,}:?")) }) {
        val rows = mutableListOf(header)
        index += 2
        while (index < lines.size && lines[index].trim().let { it.startsWith("|") && it.endsWith("|") }) {
          rows += lines[index].trim()
          index++
        }
        output.append("<table><thead><tr>")
        rows.first().trim('|').split('|').forEach { output.append("<th>").append(escape(it.trim())).append("</th>") }
        output.append("</tr></thead><tbody>")
        rows.drop(1).forEach { row ->
          output.append("<tr>")
          row.trim('|').split('|').forEach { output.append("<td>").append(escape(it.trim())).append("</td>") }
          output.append("</tr>")
        }
        output.append("</tbody></table>")
      } else {
        output.append(renderer.render(parser.parse(lines[index]))).append('\n')
        index++
      }
    }
    return output.toString()
  }

  private fun safeFolderName(value: String): String = value
    .replace(Regex("[^a-zA-Z0-9._ -]"), "")
    .trim()
    .replace(Regex("\\s+"), " ")
    .ifBlank { "Journal-Export" }

  private fun escape(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")
}

private fun Long.toZonedDateTime() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())

/**
 * Keeps image paths relative to the exported folder and rejects absolute or parent-traversing
 * paths. CommonMark still handles normal external image URLs unchanged.
 */
private object ImageSrcAttributeProviderFactory : AttributeProviderFactory {
  override fun create(context: AttributeProviderContext): AttributeProvider =
    AttributeProvider { node: Node?, tagName: String?, attributes: MutableMap<String, String>? ->
      if (node is Image && tagName == "img" && attributes != null) {
        val destination = node.destination.replace('\\', '/')
        if (!destination.startsWith("/") && !destination.contains("://")) {
          val relative = Path.of(destination).normalize()
          if (!relative.startsWith("..")) attributes["src"] = relative.toString().replace('\\', '/')
        }
      }
    }
}
