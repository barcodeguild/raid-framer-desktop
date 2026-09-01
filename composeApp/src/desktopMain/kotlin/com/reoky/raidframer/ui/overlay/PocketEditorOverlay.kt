package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.pocket.PocketDraftCoordinator
import com.reoky.raidframer.core.pocket.PocketMarkdownBlock
import com.reoky.raidframer.core.pocket.PocketMarkdownInline
import com.reoky.raidframer.core.pocket.parsePocketMarkdown
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.TitleBarComponent
import kotlinx.coroutines.delay

@Composable
fun PocketEditorOverlay(wm: WindowManager? = null) {
  val draft by PocketDraftCoordinator.activeDraft.collectAsState()
  var title by remember(draft?.metadata?.id) { mutableStateOf(draft?.metadata?.title.orEmpty()) }
  var markdown by remember(draft?.metadata?.id) { mutableStateOf(draft?.markdown.orEmpty()) }
  var selectedTab by remember { mutableStateOf(0) }

  LaunchedEffect(title, markdown, draft?.metadata?.id) {
    if (draft != null) {
      delay(750L)
      PocketDraftCoordinator.updateDraft(title, markdown)
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.66f))) {
    TitleBarComponent(
      title = "Pocket Editor",
      onClose = {
        PocketDraftCoordinator.closeEditorSession()
        wm?.closeWindow(OverlayType.POCKET_EDITOR)
      }
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        modifier = Modifier.weight(1f),
        singleLine = true,
        label = { Text("Title (optional)") },
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
        colors = TextFieldDefaults.outlinedTextFieldColors(
          textColor = Color.White,
          cursorColor = Color.White,
          focusedBorderColor = Color.White,
          unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
          focusedLabelColor = Color.White,
          unfocusedLabelColor = Color.White.copy(alpha = 0.70f)
        )
      )
      Button(onClick = {
        if (wm?.isVisible(OverlayType.POCKET_JOURNAL)?.value != true) {
          wm?.openWindow(OverlayType.POCKET_JOURNAL)
        }
      },
        colors = androidx.compose.material.ButtonDefaults.buttonColors(
          backgroundColor = Color.White.copy(alpha = 0.12f),
          contentColor = Color.White
        )
      ) {
        Text("Journal")
      }
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
        .background(Color(0xFF141414).copy(alpha = 0.78f), RoundedCornerShape(14.dp))
        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
        .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      listOf("Edit", "Preview").forEachIndexed { index, label ->
        Button(
          onClick = { selectedTab = index },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
          colors = androidx.compose.material.ButtonDefaults.buttonColors(
            backgroundColor = if (selectedTab == index) Color.White.copy(alpha = 0.14f) else Color.Transparent,
            contentColor = Color.White
          ),
          elevation = androidx.compose.material.ButtonDefaults.elevation(defaultElevation = 2.dp)
        ) { Text(label, fontSize = 12.sp) }
      }
    }
    if (selectedTab == 0) {
      OutlinedTextField(
        value = markdown,
        onValueChange = { markdown = it },
        modifier = Modifier.fillMaxSize().padding(10.dp),
        label = { Text("Markdown") },
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
        colors = TextFieldDefaults.outlinedTextFieldColors(
          textColor = Color.White,
          cursorColor = Color.White,
          focusedBorderColor = Color.White,
          unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
          focusedLabelColor = Color.White,
          unfocusedLabelColor = Color.White.copy(alpha = 0.70f)
        )
      )
    } else {
      Column(
        modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())
      ) {
        PocketMarkdownPreview(markdown)
      }
    }
  }
}

@Composable
private fun PocketMarkdownPreview(markdown: String) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    parsePocketMarkdown(markdown).forEach { block ->
      when (block) {
        is PocketMarkdownBlock.Paragraph -> PocketInlineText(block.content)
        is PocketMarkdownBlock.Heading -> Text(
          text = block.content.toPlainText(),
          color = Color.White,
          fontSize = (22 - block.level * 2).coerceAtLeast(14).sp
        )
        is PocketMarkdownBlock.BulletList -> block.items.forEach { item ->
          Row { Text("• ", color = Color.White); PocketInlineText(item) }
        }
        is PocketMarkdownBlock.OrderedList -> block.items.forEachIndexed { index, item ->
          Row { Text("${index + 1}. ", color = Color.White); PocketInlineText(item) }
        }
        is PocketMarkdownBlock.Quote -> Text(
          text = "> ${block.content.toPlainText()}",
          color = RFColors.TextSecondary,
          fontSize = 14.sp
        )
        is PocketMarkdownBlock.CodeBlock -> Text(
          text = block.code,
          color = Color.White,
          fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
          modifier = Modifier.background(Color.Black.copy(alpha = 0.45f)).padding(8.dp)
        )
      }
    }
  }
}

@Composable
private fun PocketInlineText(content: List<PocketMarkdownInline>) {
  androidx.compose.foundation.text.BasicText(
    text = content.toPlainText(),
    style = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp)
  )
}

private fun List<PocketMarkdownInline>.toPlainText(): String = joinToString("") { inline ->
  when (inline) {
    is PocketMarkdownInline.Plain -> inline.value
    is PocketMarkdownInline.Strong -> inline.content.toPlainText()
    is PocketMarkdownInline.Emphasis -> inline.content.toPlainText()
    is PocketMarkdownInline.Code -> inline.value
    is PocketMarkdownInline.Link -> inline.label.toPlainText()
    is PocketMarkdownInline.Image -> "[Image: ${inline.alt.ifBlank { inline.destination }}]"
    PocketMarkdownInline.Break -> "\n"
  }
}
