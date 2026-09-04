package com.reoky.raidframer.core.pocket

import org.commonmark.node.Block
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension

sealed interface PocketMarkdownBlock {
  data class Paragraph(val content: List<PocketMarkdownInline>) : PocketMarkdownBlock
  data class Heading(val level: Int, val content: List<PocketMarkdownInline>) : PocketMarkdownBlock
  data class BulletList(val items: List<ListItemContent>) : PocketMarkdownBlock
  data class OrderedList(val items: List<ListItemContent>) : PocketMarkdownBlock
  data class Quote(val content: List<PocketMarkdownInline>) : PocketMarkdownBlock
  data class CodeBlock(val code: String) : PocketMarkdownBlock
  data class Table(val headers: List<List<PocketMarkdownInline>>, val rows: List<List<List<PocketMarkdownInline>>>) : PocketMarkdownBlock
}

data class ListItemContent(val content: List<PocketMarkdownInline>, val checked: Boolean? = null)

sealed interface PocketMarkdownInline {
  data class Plain(val value: String) : PocketMarkdownInline
  data class Strong(val content: List<PocketMarkdownInline>) : PocketMarkdownInline
  data class Emphasis(val content: List<PocketMarkdownInline>) : PocketMarkdownInline
  data class Strikethrough(val content: List<PocketMarkdownInline>) : PocketMarkdownInline
  data class Code(val value: String) : PocketMarkdownInline
  data class Link(val label: List<PocketMarkdownInline>, val destination: String) : PocketMarkdownInline
  data class Image(val alt: String, val destination: String) : PocketMarkdownInline
  data class Highlight(val content: List<PocketMarkdownInline>) : PocketMarkdownInline
  data object Break : PocketMarkdownInline
}

private val pocketMarkdownParser: Parser = Parser.builder()
  .extensions(listOf(StrikethroughExtension.create(), TablesExtension.create(), TaskListItemsExtension.create()))
  .build()

fun parsePocketMarkdown(markdown: String): List<PocketMarkdownBlock> {
  val taskStates = markdown.lineSequence().mapNotNull { line ->
    Regex("^\\s*[-+*]\\s+\\[([ xX])\\]").find(line)?.groupValues?.get(1)?.equals("x", true)
  }.iterator()
  val blocks = mutableListOf<PocketMarkdownBlock>()
  val lines = markdown.lines()
  var i = 0
  while (i < lines.size) {
    val line = lines[i].trim()
    val nextLine = lines.getOrNull(i + 1)?.trim()
    if (isTableStart(line, nextLine)) {
      val header = splitTableRow(line)
      i += 2 // Skip header and divider
      val rows = mutableListOf<List<List<PocketMarkdownInline>>>()
      while (i < lines.size && isTableRow(lines[i].trim())) {
        rows.add(splitTableRow(lines[i].trim()))
        i++
      }
      blocks.add(PocketMarkdownBlock.Table(header, rows))
    } else {
      val nonTableLines = mutableListOf<String>()
      while (i < lines.size) {
        val curr = lines[i].trim()
        val next = lines.getOrNull(i + 1)?.trim()
        if (isTableStart(curr, next)) break
        nonTableLines.add(lines[i])
        i++
      }
      if (nonTableLines.isNotEmpty()) {
        val chunk = nonTableLines.joinToString("\n")
        if (chunk.isNotBlank()) {
          val doc = pocketMarkdownParser.parse(chunk)
          blocks.addAll(childNodesOf(doc).mapNotNull { (it as? Block)?.toPocketBlock(taskStates) })
        }
      }
    }
  }
  return blocks
}

private fun isTableStart(header: String, divider: String?): Boolean {
  if (divider == null) return false
  if (!isTableRow(header) || !isTableRow(divider)) return false
  val divParts = divider.trim().trim('|').split('|').map(String::trim)
  return divParts.isNotEmpty() && divParts.all { it.matches(Regex(":?-{2,}:?")) }
}

private fun isTableRow(line: String): Boolean {
  val trimmed = line.trim()
  return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length >= 2
}

private fun splitTableRow(line: String): List<List<PocketMarkdownInline>> {
  return line.trim().trim('|').split('|').map { cellText ->
    val cellDoc = pocketMarkdownParser.parse(cellText.trim())
    val inlines = childNodesOf(cellDoc).flatMap { childNodesOf(it).toPocketInlines() }
    inlines.ifEmpty { listOf(PocketMarkdownInline.Plain(cellText.trim())) }
  }
}

private fun List<PocketMarkdownInline>.toPlainText(): String = joinToString("") {
  when (it) {
    is PocketMarkdownInline.Plain -> it.value
    is PocketMarkdownInline.Strong -> it.content.toPlainText()
    is PocketMarkdownInline.Emphasis -> it.content.toPlainText()
    is PocketMarkdownInline.Strikethrough -> it.content.toPlainText()
    is PocketMarkdownInline.Code -> it.value
    is PocketMarkdownInline.Link -> it.label.toPlainText()
    is PocketMarkdownInline.Image -> it.alt
    is PocketMarkdownInline.Highlight -> it.content.toPlainText()
    PocketMarkdownInline.Break -> "\n"
  }
}

private fun Block.toPocketBlock(taskStates: Iterator<Boolean>): PocketMarkdownBlock? = when (this) {
  is Paragraph -> PocketMarkdownBlock.Paragraph(childNodesOf(this).toPocketInlines())
  is Heading -> PocketMarkdownBlock.Heading(level, childNodesOf(this).toPocketInlines())
  is BulletList -> PocketMarkdownBlock.BulletList(childNodesOf(this).mapNotNull { it.listItemContent(taskStates) })
  is OrderedList -> PocketMarkdownBlock.OrderedList(childNodesOf(this).mapNotNull { it.listItemContent(taskStates) })
  is BlockQuote -> PocketMarkdownBlock.Quote(childNodesOf(this).flatMap { childNodesOf(it).toPocketInlines() })
  is FencedCodeBlock -> PocketMarkdownBlock.CodeBlock(literal)
  is TableBlock -> {
    val rows = childNodesOf(this).filterIsInstance<TableRow>()
    val cells = rows.map { row -> childNodesOf(row).filterIsInstance<TableCell>().map { childNodesOf(it).toPocketInlines() } }
    PocketMarkdownBlock.Table(cells.firstOrNull().orEmpty(), cells.drop(1))
  }
  else -> null
}

private fun List<Node>.toPocketInlines(): List<PocketMarkdownInline> = flatMap { it.inlineChildren() }

private fun Node.listItemContent(taskStates: Iterator<Boolean>): ListItemContent? = (this as? ListItem)?.let { item ->
  var rawInlines = childNodesOf(item).flatMap { childNodesOf(it).toPocketInlines() }
  var checked: Boolean? = null
  val first = rawInlines.firstOrNull()
  if (first is PocketMarkdownInline.Plain) {
    val str = first.value
    val match = Regex("^\\[([ xX])\\]\\s*").find(str)
    if (match != null) {
      checked = match.groupValues[1].equals("x", ignoreCase = true)
      val remainder = str.substring(match.range.last + 1)
      rawInlines = if (remainder.isEmpty()) {
        rawInlines.drop(1)
      } else {
        listOf(PocketMarkdownInline.Plain(remainder)) + rawInlines.drop(1)
      }
    }
  }
  if (checked == null) {
    checked = taskStates.nextOrNull()
  }
  ListItemContent(rawInlines, checked)
}

private fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null

private fun Node.inlineChildren(): List<PocketMarkdownInline> = when (this) {
  is Text -> parseHighlights(literal)
  is StrongEmphasis -> listOf(PocketMarkdownInline.Strong(childNodesOf(this).toPocketInlines()))
  is Emphasis -> listOf(PocketMarkdownInline.Emphasis(childNodesOf(this).toPocketInlines()))
  is Strikethrough -> listOf(PocketMarkdownInline.Strikethrough(childNodesOf(this).toPocketInlines()))

  is Code -> listOf(PocketMarkdownInline.Code(literal))
  is Link -> listOf(PocketMarkdownInline.Link(childNodesOf(this).toPocketInlines(), destination))
  is Image -> listOf(PocketMarkdownInline.Image(destination = destination, alt = title.orEmpty()))
  is HardLineBreak -> listOf(PocketMarkdownInline.Break)
  else -> childNodesOf(this).flatMap { it.inlineChildren() }
}

private fun parseHighlights(text: String): List<PocketMarkdownInline> {
  if (!text.contains("==")) return listOf(PocketMarkdownInline.Plain(text))
  val parts = text.split("==")
  if (parts.size < 3) return listOf(PocketMarkdownInline.Plain(text))
  val result = mutableListOf<PocketMarkdownInline>()
  for (idx in parts.indices) {
    if (parts[idx].isEmpty()) continue
    if (idx % 2 == 1 && idx < parts.size - 1) {
      result.add(PocketMarkdownInline.Highlight(listOf(PocketMarkdownInline.Plain(parts[idx]))))
    } else {
      result.add(PocketMarkdownInline.Plain(parts[idx]))
    }
  }
  return result
}


private fun childNodesOf(node: Node): List<Node> {
  val children = mutableListOf<Node>()
  var child = node.firstChild
  while (child != null) {
    children += child
    child = child.next
  }
  return children
}
