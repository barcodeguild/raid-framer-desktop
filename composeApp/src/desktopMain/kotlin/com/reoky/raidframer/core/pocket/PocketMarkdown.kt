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

sealed interface PocketMarkdownBlock {
  data class Paragraph(val content: List<PocketMarkdownInline>) : PocketMarkdownBlock
  data class Heading(val level: Int, val content: List<PocketMarkdownInline>) : PocketMarkdownBlock
  data class BulletList(val items: List<List<PocketMarkdownInline>>) : PocketMarkdownBlock
  data class OrderedList(val items: List<List<PocketMarkdownInline>>) : PocketMarkdownBlock
  data class Quote(val content: List<PocketMarkdownInline>) : PocketMarkdownBlock
  data class CodeBlock(val code: String) : PocketMarkdownBlock
}

sealed interface PocketMarkdownInline {
  data class Plain(val value: String) : PocketMarkdownInline
  data class Strong(val content: List<PocketMarkdownInline>) : PocketMarkdownInline
  data class Emphasis(val content: List<PocketMarkdownInline>) : PocketMarkdownInline
  data class Strikethrough(val content: List<PocketMarkdownInline>) : PocketMarkdownInline
  data class Code(val value: String) : PocketMarkdownInline
  data class Link(val label: List<PocketMarkdownInline>, val destination: String) : PocketMarkdownInline
  data class Image(val alt: String, val destination: String) : PocketMarkdownInline
  data object Break : PocketMarkdownInline
}

private val pocketMarkdownParser: Parser = Parser.builder()
  .extensions(listOf(StrikethroughExtension.create()))
  .build()

fun parsePocketMarkdown(markdown: String): List<PocketMarkdownBlock> {
  val document = pocketMarkdownParser.parse(markdown)
  return childNodesOf(document).mapNotNull { (it as? Block)?.toPocketBlock() }
}

private fun Block.toPocketBlock(): PocketMarkdownBlock? = when (this) {
  is Paragraph -> PocketMarkdownBlock.Paragraph(childNodesOf(this).toPocketInlines())
  is Heading -> PocketMarkdownBlock.Heading(level, childNodesOf(this).toPocketInlines())
  is BulletList -> PocketMarkdownBlock.BulletList(childNodesOf(this).mapNotNull { it.listItemInlines() })
  is OrderedList -> PocketMarkdownBlock.OrderedList(childNodesOf(this).mapNotNull { it.listItemInlines() })
  is BlockQuote -> PocketMarkdownBlock.Quote(childNodesOf(this).flatMap { childNodesOf(it).toPocketInlines() })
  is FencedCodeBlock -> PocketMarkdownBlock.CodeBlock(literal)
  else -> null
}

private fun List<Node>.toPocketInlines(): List<PocketMarkdownInline> = flatMap { it.inlineChildren() }

private fun Node.listItemInlines(): List<PocketMarkdownInline>? =
  (this as? ListItem)?.let { childNodesOf(it).flatMap { childNodesOf(it).toPocketInlines() } }

private fun Node.inlineChildren(): List<PocketMarkdownInline> = when (this) {
  is Text -> listOf(PocketMarkdownInline.Plain(literal))
  is StrongEmphasis -> listOf(PocketMarkdownInline.Strong(childNodesOf(this).toPocketInlines()))
  is Emphasis -> listOf(PocketMarkdownInline.Emphasis(childNodesOf(this).toPocketInlines()))
  is Strikethrough -> listOf(PocketMarkdownInline.Strikethrough(childNodesOf(this).toPocketInlines()))

  is Code -> listOf(PocketMarkdownInline.Code(literal))
  is Link -> listOf(PocketMarkdownInline.Link(childNodesOf(this).toPocketInlines(), destination))
  is Image -> listOf(PocketMarkdownInline.Image(destination = destination, alt = title.orEmpty()))
  is HardLineBreak -> listOf(PocketMarkdownInline.Break)
  else -> childNodesOf(this).flatMap { it.inlineChildren() }
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
