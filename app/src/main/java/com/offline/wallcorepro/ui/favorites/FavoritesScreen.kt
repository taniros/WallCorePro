package com.offline.wallcorepro.ui.favorites

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.shape.CircleShape
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.domain.model.Wallpaper
import com.offline.wallcorepro.ui.components.WallpaperCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onWallpaperClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val cfg = LocalConfiguration.current
    val gridColumns = when {
        cfg.screenWidthDp >= 840 -> 4
        cfg.screenWidthDp >= 600 -> 3
        else                     -> 2
    }

    // Biometric lock gate
    if (com.offline.wallcorepro.config.AppConfig.FEATURE_BIOMETRIC_FAVORITES &&
        uiState.isLocked && !uiState.isUnlocked) {
        BiometricLockScreen(
            onUnlock = {
                val executor  = androidx.core.content.ContextCompat.getMainExecutor(context)
                val activity  = context as? androidx.fragment.app.FragmentActivity ?: run {
                    viewModel.onBiometricSuccess(); return@BiometricLockScreen
                }
                val prompt = androidx.biometric.BiometricPrompt(
                    activity, executor,
                    object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: androidx.biometric.BiometricPrompt.AuthenticationResult
                        ) { viewModel.onBiometricSuccess() }
                    }
                )
                val info = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Saved Wallpapers")
                    .setSubtitle("Authenticate to view your favorites")
                    .setNegativeButtonText("Cancel")
                    .build()
                prompt.authenticate(info)
            },
            onBack = onBackClick
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("❤️ ", fontSize = 20.sp)
                        Text(
                            "Saved Wallpapers",
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (uiState.favorites.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text(
                                    text  = "${uiState.favorites.size}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = MaterialTheme.colorScheme.primary
                    )
                }

                uiState.favorites.isEmpty() -> {
                    EmptyFavoritesState(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Quote of the day mini-card
                        QuoteOfDayMiniCard()

                        // Grid
                        LazyVerticalGrid(
                            columns               = GridCells.Fixed(gridColumns),
                            contentPadding        = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp),
                            modifier              = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.favorites,
                                key   = { it.id }
                            ) { wallpaper ->
                                WallpaperCard(
                                    wallpaper        = wallpaper,
                                    onClick          = { onWallpaperClick(wallpaper.id) },
                                    onFavoriteToggle = { w, fav ->
                                        viewModel.toggleFavorite(w.id, fav)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Quote of the Day Mini Card ───────────────────────────────────────────────

@Composable
private fun QuoteOfDayMiniCard() {
    val quote = remember { AppConfig.getQuoteOfDay() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape    = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(
                    listOf(Color(0xFF1A0A00), Color(0xFF2D1B00))
                ))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "💭 Daily Wish",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Copy
                    IconButton(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(quote.text))
                            android.widget.Toast.makeText(context, "Copied!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, tint = MaterialTheme.colorScheme.primary.copy(0.7f), modifier = Modifier.size(14.dp))
                    }

                    // Share
                    IconButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "${quote.text}\n\n— Sent via ${AppConfig.APP_NAME}")
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Wish"))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary.copy(0.7f), modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text       = "\"${quote.text}\"",
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = Color.White,
                    fontStyle  = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "— ${quote.author}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─── Beautiful Empty State ────────────────────────────────────────────────────

@Composable
private fun EmptyFavoritesState(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue  = 0.9f,
        targetValue   = 1.1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_pulse"
    )

    Column(
        modifier              = modifier.padding(40.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        // Animated glowing heart
        Box(
            modifier         = Modifier
                .size(120.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFF5252).copy(0.25f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("💔", fontSize = 56.sp)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text       = "No Saved Wallpapers",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text      = "Tap the ❤️ on any wallpaper to save it here — build your perfect collection of morning and night wallpapers.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Time-based suggestion chip
        val timeOfDay = AppConfig.TimeOfDay.current()
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier            = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(timeOfDay.emoji, fontSize = 18.sp)
                Text(
                    text       = when (timeOfDay) {
                        AppConfig.TimeOfDay.MORNING   -> "Explore morning wallpapers"
                        AppConfig.TimeOfDay.AFTERNOON -> "Discover afternoon wallpapers"
                        AppConfig.TimeOfDay.EVENING   -> "Browse evening wallpapers"
                        AppConfig.TimeOfDay.NIGHT     -> "Discover night wallpapers"
                    },
                    style      = MaterialTheme.typography.labelLarge,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─── Biometric Lock Screen ────────────────────────────────────────────────────

@Composable
private fun BiometricLockScreen(
    onUnlock: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0A1F), Color(0xFF1A103A), Color(0xFF0D0A1F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Surface(
                shape    = CircleShape,
                color    = Color(0xFF7C4DFF).copy(alpha = 0.15f),
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null,
                        modifier = Modifier.size(48.dp), tint = Color(0xFF7C4DFF))
                }
            }
            Text("🔒 Private Collection",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White, fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center)
            Text("This section is protected.\nAuthenticate to view your saved wallpapers.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onUnlock,
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unlock with Biometrics", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack) {
                Text("← Go Back", color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
