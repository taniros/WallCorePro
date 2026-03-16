package com.offline.wallcorepro.ui.ai

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import android.app.Activity
import com.offline.wallcorepro.ads.AdsManager
import com.offline.wallcorepro.config.AppConfig

// Mood options with emoji labels
private val moodOptions = listOf(
    "🌟 Inspirational" to "Inspirational",
    "💕 Romantic"      to "Romantic",
    "😄 Funny"         to "Funny",
    "❤️ Heartfelt"     to "Heartfelt",
    "🎩 Formal"        to "Formal",
    "🙏 Spiritual"     to "Spiritual",
    "🌸 Cute"          to "Cute"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    onBackClick: () -> Unit,
    viewModel: AiViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val context        = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState    = rememberScrollState()

    LaunchedEffect(uiState.showInterstitialTrigger) {
        if (uiState.showInterstitialTrigger) {
            (context as? Activity)?.let {
                AdsManager.showInterstitialIfReady(it) { viewModel.dismissInterstitialTrigger() }
            }
        }
    }

    var selectedNiche by remember { mutableStateOf(AppConfig.TimeOfDay.current().key.replaceFirstChar { it.uppercase() }) }

    // Pulsing glow animation on the generate button
    val infiniteAnim = rememberInfiniteTransition(label = "ai_glow")
    val glowAlpha by infiniteAnim.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 0.85f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btn_glow"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("✨", fontSize = 18.sp)
                        Text(
                            "WishMagic AI",
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ─── Morning / Afternoon / Evening / Night Toggle ────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NicheToggle(
                        label      = "☀️ Morning",
                        isSelected = selectedNiche.equals("Morning", ignoreCase = true),
                        onClick    = { selectedNiche = "Morning" },
                        modifier   = Modifier.weight(1f)
                    )
                    NicheToggle(
                        label      = "🌤️ Afternoon",
                        isSelected = selectedNiche.equals("Afternoon", ignoreCase = true),
                        onClick    = { selectedNiche = "Afternoon" },
                        modifier   = Modifier.weight(1f)
                    )
                    NicheToggle(
                        label      = "🌅 Evening",
                        isSelected = selectedNiche.equals("Evening", ignoreCase = true),
                        onClick    = { selectedNiche = "Evening" },
                        modifier   = Modifier.weight(1f)
                    )
                    NicheToggle(
                        label      = "🌙 Night",
                        isSelected = selectedNiche.equals("Night", ignoreCase = true),
                        onClick    = { selectedNiche = "Night" },
                        modifier   = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ─── Emotional Tone Selector ─────────────────────────────
                if (AppConfig.FEATURE_TONE_SELECTOR) {
                    SectionLabel("Tone of Voice")
                    Spacer(Modifier.height(8.dp))
                    ToneChipRow(
                        selectedToneKey = uiState.selectedTone,
                        onToneClick     = { viewModel.onToneSelected(it) }
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // ─── Mood Selection ──────────────────────────────────────
                SectionLabel("Choose a Mood")
                Spacer(Modifier.height(10.dp))
                MoodChipGrid(
                    moods        = moodOptions,
                    selectedMood = uiState.selectedMood,
                    onMoodClick  = { viewModel.onMoodSelected(it) }
                )

                Spacer(Modifier.height(20.dp))

                // ─── AI Result Card ──────────────────────────────────────
                WishResultCard(
                    wish          = uiState.generatedWish,
                    isLoading     = uiState.isLoading,
                    onCopy       = { clipboardManager.setText(AnnotatedString(uiState.generatedWish)) },
                    onShare      = {
                        val shareText = buildString {
                            append("\"${uiState.generatedWish}\"")
                            append("\n\n")
                            append(if (uiState.userName.isNotBlank()) "— ${uiState.userName}" else "— via ${AppConfig.APP_NAME}")
                            append("\n✨ ${AppConfig.APP_NAME_SHORT} · Download FREE: ${AppConfig.PLAY_STORE_URL}")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Wish"))
                    },
                    onEditClick  = { viewModel.toggleEditDialog() }
                )

                // ─── Edit Wish Dialog ─────────────────────────────────────
                if (uiState.showEditDialog && uiState.generatedWish.isNotEmpty()) {
                    AiEditWishDialog(
                        initialText = uiState.generatedWish,
                        onApply     = { viewModel.updateGeneratedWish(it) },
                        onDismiss   = { viewModel.toggleEditDialog() }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ─── Generate Button ─────────────────────────────────────
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.radialGradient(
                                colors  = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                                    Color.Transparent
                                ),
                                radius  = 400f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick  = { viewModel.generateWish(selectedNiche) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape    = RoundedCornerShape(18.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled  = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(22.dp),
                                color       = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Generate Magic Wish",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 16.sp
                            )
                        }
                    }
                }

                // ─── Wish History ────────────────────────────────────────
                if (uiState.wishHistory.size > 1) {
                    Spacer(Modifier.height(28.dp))
                    SectionLabel("Recent Wishes")
                    Spacer(Modifier.height(10.dp))
                    uiState.wishHistory.asReversed().drop(1).forEach { historyWish ->
                        WishHistoryCard(
                            wish   = historyWish,
                            onCopy = { clipboardManager.setText(AnnotatedString(historyWish)) },
                            onShare = {
                                val shareText = buildString {
                                    append("\"$historyWish\"")
                                    append("\n\n")
                                    append(if (uiState.userName.isNotBlank()) "— ${uiState.userName}" else "— via ${AppConfig.APP_NAME}")
                                    append("\n✨ ${AppConfig.APP_NAME_SHORT} · Download FREE: ${AppConfig.PLAY_STORE_URL}")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Wish"))
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text  = "Powered by Gemini AI  ·  WishMagic",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
                Spacer(Modifier.height(16.dp))
            }

            // Error snackbar
            uiState.error?.let { err ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") }
                    }
                ) { Text(err) }
            }
        }
    }
}

// ─── Section Label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        modifier   = Modifier.fillMaxWidth()
    )
}

// ─── Niche Toggle ─────────────────────────────────────────────────────────────

@Composable
fun NicheToggle(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick      = onClick,
        modifier     = modifier.height(50.dp),
        shape        = RoundedCornerShape(14.dp),
        color        = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Mood Chip Grid (wrapping rows) ───────────────────────────────────────────

@Composable
private fun MoodChipGrid(
    moods: List<Pair<String, String>>,
    selectedMood: String,
    onMoodClick: (String) -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = {
            moods.forEach { (label, value) ->
                MoodChip(
                    label      = label,
                    isSelected = selectedMood == value,
                    onClick    = { onMoodClick(value) }
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val spacing = 8.dp.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val maxWidth   = constraints.maxWidth
        val rows       = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var rowWidth   = 0

        placeables.forEach { p ->
            if (rowWidth + p.width + spacing > maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow); currentRow = mutableListOf(); rowWidth = 0
            }
            currentRow.add(p); rowWidth += p.width + spacing
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        val totalHeight = rows.sumOf { row -> (row.maxOfOrNull { it.height } ?: 0) + spacing }
        layout(maxWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                row.forEach { p -> p.placeRelative(x, y); x += p.width + spacing }
                y += rowHeight + spacing
            }
        }
    }
}

// ─── Mood Chip ────────────────────────────────────────────────────────────────

@Composable
fun MoodChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick      = onClick,
        shape        = RoundedCornerShape(20.dp),
        color        = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border       = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        tonalElevation = 0.dp
    ) {
        Text(
            text     = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style    = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
        )
    }
}

// ─── Edit Wish Dialog ────────────────────────────────────────────────────────

@Composable
private fun AiEditWishDialog(
    initialText: String,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    val maxChars = 200

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✏️", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text("Edit Your Wish", fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value         = text,
                    onValueChange = { if (it.length <= maxChars) text = it },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 3,
                    maxLines      = 6,
                    placeholder   = { Text("Your personalized wish…") }
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = "${text.length} / $maxChars",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        },
        confirmButton = {
            Button(onClick = { onApply(text.trim()) }, enabled = text.isNotBlank()) {
                Text("Apply ✓", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Wish Result Card ────────────────────────────────────────────────────────

@Composable
private fun WishResultCard(
    wish: String,
    isLoading: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEditClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    ) {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState  = isLoading,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label        = "wish_content"
            ) { loading ->
                if (loading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color        = MaterialTheme.colorScheme.primary,
                            strokeWidth  = 3.dp,
                            modifier     = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Crafting your wish…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (wish.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = "\"$wish\"",
                            style      = MaterialTheme.typography.titleMedium.copy(lineHeight = 28.sp),
                            color      = MaterialTheme.colorScheme.onSurface,
                            textAlign  = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onEditClick,
                                shape   = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit")
                            }
                            OutlinedButton(
                                onClick = onCopy,
                                shape   = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy")
                            }
                            Button(
                                onClick = onShare,
                                shape   = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 36.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Your AI wish will appear here…",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ─── Emotional Tone Chip Row ─────────────────────────────────────────────────

@Composable
private fun ToneChipRow(
    selectedToneKey: String,
    onToneClick: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding        = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
    ) {
        items(AppConfig.EmotionalTone.entries) { tone ->
            val isSelected = tone.key == selectedToneKey
            Surface(
                onClick = { onToneClick(tone.key) },
                shape   = RoundedCornerShape(20.dp),
                color   = if (isSelected) Color(tone.accentColor).copy(alpha = 0.85f)
                          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.animateContentSize()
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tone.emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text       = tone.displayName,
                        style      = MaterialTheme.typography.labelLarge,
                        color      = if (isSelected) Color.White
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ─── Wish History Card ────────────────────────────────────────────────────────

@Composable
private fun WishHistoryCard(
    wish: String,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = "\"$wish\"",
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.weight(1f),
                maxLines = 3,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
            Spacer(Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick  = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        null,
                        tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick  = onShare,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        null,
                        tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
