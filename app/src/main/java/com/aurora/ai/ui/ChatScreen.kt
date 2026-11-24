package com.aurora.ai.ui

import android.content.Intent
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurora.ai.ChatViewModel
import com.aurora.ai.data.Conv
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        drawerContent = {
            AppDrawer(vm = vm, onClose = { scope.launch { drawerState.close() } })
        }
    ) {
        ChatContent(vm = vm, onMenu = { scope.launch { drawerState.open() } })
    }
}

// ───────────────────────── Konten chat ─────────────────────────

@Composable
private fun ChatContent(vm: ChatViewModel, onMenu: () -> Unit) {
    val scroll = rememberScrollState()
    val messages = vm.messages

    LaunchedEffect(messages.size, vm.loading, vm.currentId) {
        delay(60)
        scroll.animateScrollTo(scroll.maxValue)
    }
    LaunchedEffect(vm.streamText.length) {
        if (vm.streaming) scroll.scrollTo(scroll.maxValue)
    }

    Box(Modifier.fillMaxSize()) {
        BackdropGlow()

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            TopBar(
                onMenu = onMenu,
                onNew = vm::newChat,
                canNew = messages.isNotEmpty() || vm.busy
            )

            Box(Modifier.weight(1f)) {
                if (messages.isEmpty() && !vm.busy) {
                    EmptyState(onPick = vm::send)
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        messages.forEachIndexed { index, m ->
                            if (m.fromUser) {
                                UserBubble(m.text)
                            } else {
                                AiMessage(
                                    text = m.text,
                                    isError = m.isError,
                                    streaming = false,
                                    showRegenerate = index == messages.lastIndex && !vm.busy,
                                    onRegenerate = vm::regenerate
                                )
                            }
                        }
                        if (vm.loading) TypingRow()
                        if (vm.streaming) {
                            AiMessage(
                                text = vm.streamText,
                                isError = false,
                                streaming = true,
                                showRegenerate = false,
                                onRegenerate = {}
                            )
                        }
                        if (!vm.busy && messages.lastOrNull()?.fromUser == true) {
                            ActionChip(
                                label = "Buat jawaban",
                                onClick = vm::regenerate,
                                modifier = Modifier.padding(start = 38.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            InputBar(busy = vm.busy, onSend = vm::send, onStop = vm::stop)
        }
    }
}

// ───────────────────────── Latar ─────────────────────────

@Composable
private fun BackdropGlow() {
    val t = rememberInfiniteTransition(label = "glow")
    val a by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "a"
    )
    val b by t.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "b"
    )

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Ink, InkDeep)))

        fun orb(color: Color, cx: Float, cy: Float, r: Float, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }

        orb(Indigo, w * (0.15f + 0.5f * a), h * (0.10f + 0.08f * b), w * 0.95f, 0.34f)
        orb(Gold, w * (0.95f - 0.45f * b), h * (0.78f + 0.08f * a), w * 0.80f, 0.13f)
    }
}

// ───────────────────────── Ikon (digambar manual, tanpa dependensi) ─────────────────────────

@Composable
private fun MenuIcon(color: Color, iconSize: Dp = 22.dp) {
    Canvas(Modifier.size(iconSize)) {
        val m = size.minDimension
        val sw = 2.dp.toPx()
        drawLine(color, Offset(m * 0.14f, m * 0.34f), Offset(m * 0.86f, m * 0.34f), sw, StrokeCap.Round)
        drawLine(color, Offset(m * 0.14f, m * 0.66f), Offset(m * 0.60f, m * 0.66f), sw, StrokeCap.Round)
    }
}

@Composable
private fun PlusIcon(color: Color, iconSize: Dp = 20.dp) {
    Canvas(Modifier.size(iconSize)) {
        val m = size.minDimension
        val sw = 2.dp.toPx()
        drawLine(color, Offset(m / 2, m * 0.16f), Offset(m / 2, m * 0.84f), sw, StrokeCap.Round)
        drawLine(color, Offset(m * 0.16f, m / 2), Offset(m * 0.84f, m / 2), sw, StrokeCap.Round)
    }
}

@Composable
private fun CrossIcon(color: Color, iconSize: Dp = 14.dp) {
    Canvas(Modifier.size(iconSize)) {
        val m = size.minDimension
        val sw = 1.8.dp.toPx()
        drawLine(color, Offset(m * 0.2f, m * 0.2f), Offset(m * 0.8f, m * 0.8f), sw, StrokeCap.Round)
        drawLine(color, Offset(m * 0.8f, m * 0.2f), Offset(m * 0.2f, m * 0.8f), sw, StrokeCap.Round)
    }
}

@Composable
private fun ArrowUpIcon(color: Color, iconSize: Dp = 22.dp) {
    Canvas(Modifier.size(iconSize)) {
        val m = size.minDimension
        val sw = 2.4.dp.toPx()
        drawLine(color, Offset(m / 2, m * 0.82f), Offset(m / 2, m * 0.2f), sw, StrokeCap.Round)
        drawLine(color, Offset(m / 2, m * 0.2f), Offset(m * 0.24f, m * 0.46f), sw, StrokeCap.Round)
        drawLine(color, Offset(m / 2, m * 0.2f), Offset(m * 0.76f, m * 0.46f), sw, StrokeCap.Round)
    }
}

@Composable
private fun PauseIcon(color: Color, iconSize: Dp = 22.dp) {
    Canvas(Modifier.size(iconSize)) {
        val m = size.minDimension
        val bar = Size(m * 0.17f, m * 0.56f)
        val r = CornerRadius(2.5.dp.toPx())
        drawRoundRect(color, Offset(m * 0.27f, m * 0.22f), bar, r)
        drawRoundRect(color, Offset(m * 0.56f, m * 0.22f), bar, r)
    }
}

@Composable
private fun IconBtn(enabled: Boolean = true, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun AvatarOrb(avatarSize: Int) {
    Box(
        Modifier
            .size(avatarSize.dp)
            .clip(CircleShape)
            .background(GoldBrush),
        contentAlignment = Alignment.Center
    ) {
        Text("✦", color = OnGold, fontSize = (avatarSize * 0.46f).sp, fontWeight = FontWeight.Bold)
    }
}

// ───────────────────────── Top bar ─────────────────────────

@Composable
private fun TopBar(onMenu: () -> Unit, onNew: () -> Unit, canNew: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBtn(onClick = onMenu) { MenuIcon(TextPrimary) }
        Spacer(Modifier.size(4.dp))
        Column(Modifier.weight(1f)) {
            Text("Aurora AI", style = MaterialTheme.typography.titleMedium, color = GoldSoft)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5FD38D))
                )
                Spacer(Modifier.size(6.dp))
                Text("Siap membantu", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
        IconBtn(enabled = canNew, onClick = onNew) { PlusIcon(Gold) }
    }
}

// ───────────────────────── Drawer (menu geser) ─────────────────────────

@Composable
private fun AppDrawer(vm: ChatViewModel, onClose: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = Surface1,
        drawerContentColor = TextPrimary
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarOrb(36)
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("Aurora AI", style = MaterialTheme.typography.titleMedium, color = GoldSoft)
                    Text("Asisten pribadi", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GoldBrush)
                    .clickable {
                        vm.newChat()
                        onClose()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlusIcon(OnGold, 18.dp)
                Spacer(Modifier.size(10.dp))
                Text("Obrolan baru", color = OnGold, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "RIWAYAT",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(8.dp))

            Box(Modifier.weight(1f)) {
                if (vm.conversations.isEmpty()) {
                    Text(
                        "Belum ada obrolan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(vm.conversations, key = { it.id }) { c ->
                            ConvRow(
                                conv = c,
                                selected = c.id == vm.currentId,
                                onClick = {
                                    vm.open(c.id)
                                    onClose()
                                },
                                onDelete = { vm.delete(c.id) }
                            )
                        }
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Hairline)
            )
            Spacer(Modifier.height(8.dp))

            SettingRow(
                title = "Ingat konteks obrolan",
                desc = "Kirim beberapa pesan sebelumnya agar AI nyambung",
                checked = vm.useContext,
                onChange = vm::changeUseContext
            )
            SettingRow(
                title = "Balas Bahasa Indonesia",
                desc = "Arahkan AI menjawab dalam bahasa Indonesia",
                checked = vm.indoReply,
                onChange = vm::changeIndoReply
            )

            Spacer(Modifier.height(6.dp))
            DrawerAction(
                label = "Bersihkan obrolan ini",
                enabled = vm.currentId != null,
                danger = false,
                onClick = { confirmClear = true }
            )
            DrawerAction(
                label = "Hapus semua obrolan",
                enabled = vm.conversations.isNotEmpty(),
                danger = true,
                onClick = { confirmAll = true }
            )
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Bersihkan obrolan ini?",
            text = "Seluruh pesan di obrolan ini akan dihapus.",
            confirm = "Bersihkan",
            onConfirm = {
                confirmClear = false
                vm.clearCurrent()
                onClose()
            },
            onDismiss = { confirmClear = false }
        )
    }
    if (confirmAll) {
        ConfirmDialog(
            title = "Hapus semua obrolan?",
            text = "Seluruh riwayat akan dihapus dan tidak bisa dikembalikan.",
            confirm = "Hapus semua",
            onConfirm = {
                confirmAll = false
                vm.deleteAll()
                onClose()
            },
            onDismiss = { confirmAll = false }
        )
    }
}

@Composable
private fun ConvRow(conv: Conv, selected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Gold.copy(alpha = 0.13f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            conv.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) GoldSoft else TextPrimary,
            modifier = Modifier.weight(1f)
        )
        IconBtn(onClick = onDelete) { CrossIcon(TextMuted) }
    }
}

@Composable
private fun SettingRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Spacer(Modifier.size(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnGold,
                checkedTrackColor = Gold,
                checkedBorderColor = Gold,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Surface2,
                uncheckedBorderColor = Hairline
            )
        )
    }
}

@Composable
private fun DrawerAction(label: String, enabled: Boolean, danger: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (danger) ErrorTint else TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        titleContentColor = GoldSoft,
        textContentColor = TextPrimary,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = ErrorTint) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextMuted) }
        }
    )
}

// ───────────────────────── Layar kosong ─────────────────────────

private val Suggestions = listOf(
    "Ide desain" to "Palet warna elegan untuk brand fashion",
    "Menulis" to "Buat caption Instagram untuk toko kopi",
    "Belajar" to "Jelaskan konsep UI/UX dengan sederhana",
    "Kode" to "Contoh fungsi Kotlin untuk validasi email"
)

@Composable
private fun EmptyState(onPick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvatarOrb(64)
        Spacer(Modifier.height(20.dp))
        Text(
            "Ada yang bisa saya bantu?",
            fontFamily = SerifFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            color = GoldSoft,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tanya apa saja — ide, tulisan, belajar, atau kode.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        Suggestions.chunked(2).forEach { rowItems ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { (tag, prompt) ->
                    SuggestionCard(tag, prompt, Modifier.weight(1f), onPick)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SuggestionCard(tag: String, prompt: String, modifier: Modifier, onPick: (String) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .heightIn(min = 104.dp)
            .clip(shape)
            .background(Surface1.copy(alpha = 0.85f))
            .border(BorderStroke(1.dp, Hairline), shape)
            .clickable { onPick(prompt) }
            .padding(14.dp)
    ) {
        Text(
            tag.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Gold,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ───────────────────────── Pesan ─────────────────────────

@Composable
private fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = OnGold,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp))
                .background(GoldBrush)
                .padding(horizontal = 16.dp, vertical = 11.dp)
        )
    }
}

@Composable
private fun AiMessage(
    text: String,
    isError: Boolean,
    streaming: Boolean,
    showRegenerate: Boolean,
    onRegenerate: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember(text) { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AvatarOrb(28)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            val body: @Composable () -> Unit = {
                MarkdownText(
                    text = if (streaming) "$text ▍" else text,
                    color = if (isError) ErrorTint else TextPrimary
                )
            }
            if (streaming) body() else SelectionContainer { body() }

            if (!streaming) {
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isError) {
                        ActionChip(if (copied) "Tersalin ✓" else "Salin", onClick = {
                            clipboard.setText(AnnotatedString(text))
                            copied = true
                        })
                        ActionChip("Bagikan", onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            val chooser = Intent.createChooser(send, "Bagikan jawaban")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooser)
                        })
                    }
                    if (showRegenerate) {
                        ActionChip(if (isError) "Coba lagi" else "Ulangi", onClick = onRegenerate)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = Gold,
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.dp, Hairline), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun TypingRow() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AvatarOrb(28)
        Spacer(Modifier.size(10.dp))
        val t = rememberInfiniteTransition(label = "typing")
        Row(
            Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { i ->
                val v by t.animateFloat(
                    initialValue = 0.2f,
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
                        .background(Gold)
                )
            }
        }
    }
}

// ───────────────────────── Input ─────────────────────────

@Composable
private fun InputBar(busy: Boolean, onSend: (String) -> Unit, onStop: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val canSend = text.isNotBlank() && !busy
    val shape = RoundedCornerShape(28.dp)

    fun submit() {
        if (canSend) {
            onSend(text)
            text = ""
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Surface2)
                .border(BorderStroke(1.dp, Hairline), shape)
                .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Kirim pesan ke Aurora…", color = TextMuted) },
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = Gold
                )
            )
            Spacer(Modifier.size(6.dp))
            SendButton(
                busy = busy,
                canSend = canSend,
                onClick = { if (busy) onStop() else submit() }
            )
        }
        Text(
            "Aurora AI bisa saja keliru. Periksa kembali info penting.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
    }
}

@Composable
private fun SendButton(busy: Boolean, canSend: Boolean, onClick: () -> Unit) {
    val enabled = busy || canSend
    Box(
        Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(if (busy) SolidColor(TextPrimary) else GoldBrush)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Saat AI bekerja, tombol kirim berubah menjadi tombol jeda (hentikan).
        Crossfade(targetState = busy, label = "sendIcon") { isBusy ->
            if (isBusy) PauseIcon(OnGold) else ArrowUpIcon(OnGold)
        }
    }
}
