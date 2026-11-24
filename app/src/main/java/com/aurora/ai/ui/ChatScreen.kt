package com.aurora.ai.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurora.ai.ChatViewModel
import com.aurora.ai.Msg

@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val listState = rememberLazyListState()
    val messages = vm.messages

    // Auto-scroll ke pesan terbaru (termasuk indikator mengetik).
    LaunchedEffect(messages.size, vm.loading) {
        val total = listState.layoutInfo.totalItemsCount
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            TopBar(showClear = messages.isNotEmpty(), onClear = vm::clear)

            Box(Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    EmptyState(onPick = vm::send)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(messages, key = { it.id }) { msg -> MessageRow(msg) }
                        if (vm.loading) item { TypingRow() }
                    }
                }
            }

            InputBar(enabled = !vm.loading, onSend = vm::send)
        }
    }
}

// ───────────────────────── Background ─────────────────────────

@Composable
fun AuroraBackground() {
    val t = rememberInfiniteTransition(label = "aurora")
    val a by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "a"
    )
    val b by t.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(19_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "b"
    )

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Ink, InkSoft)))

        fun orb(color: Color, cx: Float, cy: Float, r: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.50f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }

        orb(Violet, w * (0.05f + 0.60f * a), h * (0.12f + 0.10f * b), w * 0.85f)
        orb(Pink, w * (0.95f - 0.55f * b), h * (0.55f + 0.15f * a), w * 0.75f)
        orb(Cyan, w * (0.20f + 0.50f * b), h * (0.92f - 0.12f * a), w * 0.70f)
    }
}

// ───────────────────────── Top bar ─────────────────────────

@Composable
private fun TopBar(showClear: Boolean, onClear: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarOrb(size = 36)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Aurora AI", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4ADE80))
                )
                Spacer(Modifier.size(6.dp))
                Text("online", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
        if (showClear) {
            Text(
                "Bersihkan",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GlassFill)
                    .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(50))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun AvatarOrb(size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(AuroraBrushWide),
        contentAlignment = Alignment.Center
    ) {
        Text("✦", color = Color.White, fontSize = (size * 0.46f).sp, fontWeight = FontWeight.Bold)
    }
}

// ───────────────────────── Empty state ─────────────────────────

private val Suggestions = listOf(
    "Beri ide palet warna untuk aplikasi meditasi" to "🎨",
    "Jelaskan prinsip desain glassmorphism" to "🪟",
    "Kritik layout halaman login yang rapi dan modern" to "📐",
    "Buat slogan singkat untuk brand kopi lokal" to "☕"
)

@Composable
private fun EmptyState(onPick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvatarOrb(size = 72)
        Spacer(Modifier.size(20.dp))
        Text(
            "Halo, mau bikin apa hari ini?",
            style = MaterialTheme.typography.headlineLarge.copy(
                brush = AuroraBrushWide,
                fontSize = 30.sp,
                lineHeight = 36.sp
            )
        )
        Spacer(Modifier.size(8.dp))
        Text(
            "Tanya apa saja — ide desain, copywriting, atau sekadar ngobrol.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Spacer(Modifier.size(28.dp))

        Suggestions.forEach { (label, emoji) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassFill)
                    .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(18.dp))
                    .clickable { onPick(label) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 20.sp)
                Spacer(Modifier.size(12.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
    }
}

// ───────────────────────── Messages ─────────────────────────

@Composable
private fun MessageRow(msg: Msg) {
    if (msg.fromUser) UserBubble(msg.text) else AiBubble(msg)
}

@Composable
private fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp))
                .background(AuroraBrush)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun AiBubble(msg: Msg) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(msg.id) { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AvatarOrb(size = 28)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.widthIn(max = 320.dp)) {
            Text(
                msg.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (msg.isError) ErrorTint else TextPrimary,
                modifier = Modifier
                    .clip(shape)
                    .background(GlassFill)
                    .border(
                        BorderStroke(1.dp, if (msg.isError) ErrorTint.copy(alpha = 0.5f) else GlassStroke),
                        shape
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            if (!msg.isError) {
                Text(
                    if (copied) "Tersalin ✓" else "Salin",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier
                        .padding(start = 6.dp, top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            clipboard.setText(AnnotatedString(msg.text))
                            copied = true
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TypingRow() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AvatarOrb(size = 28)
        Spacer(Modifier.size(10.dp))
        val t = rememberInfiniteTransition(label = "typing")
        Row(
            Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(GlassFill)
                .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { i ->
                val v by t.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(550),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(i * 160)
                    ),
                    label = "dot$i"
                )
                Box(
                    Modifier
                        .size(8.dp)
                        .alpha(v)
                        .clip(CircleShape)
                        .background(AuroraBrushWide)
                )
            }
        }
    }
}

// ───────────────────────── Input ─────────────────────────

@Composable
private fun InputBar(enabled: Boolean, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val canSend = enabled && text.isNotBlank()

    fun submit() {
        if (canSend) {
            onSend(text)
            text = ""
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(GlassFill)
                .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(28.dp)),
            placeholder = { Text("Tulis pesan…", color = TextMuted) },
            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = Pink
            )
        )
        Spacer(Modifier.size(10.dp))
        Box(
            Modifier
                .size(52.dp)
                .alpha(if (canSend) 1f else 0.4f)
                .clip(CircleShape)
                .background(AuroraBrush)
                .clickable(enabled = canSend) { submit() },
            contentAlignment = Alignment.Center
        ) {
            Text("↑", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}
