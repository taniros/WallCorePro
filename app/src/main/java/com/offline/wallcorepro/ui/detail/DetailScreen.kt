package com.offline.wallcorepro.ui.detail

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.offline.wallcorepro.ads.AdsManager
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.ui.components.BannerAdView
import com.offline.wallcorepro.promotion.AppReviewManager
import timber.log.Timber
import com.offline.wallcorepro.domain.model.WallpaperTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferFullScreen by viewModel.preferFullScreen.collectAsStateWithLifecycle(initialValue = false)
    val context = LocalContext.current
    val view    = LocalView.current

    // Adaptive layout tiers — all overlay padding values scale with screen height so
    // nothing is cut off in landscape mode on phones or on tall tablets.
    //   Compact  (phone portrait,  h >= 700 dp): original generous spacing
    //   Medium   (foldable / landscape phone,  h 400-699 dp): tighter
    //   Compact  (very short landscape,        h < 400 dp): minimal
    val cfg = LocalConfiguration.current
    val screenH = cfg.screenHeightDp
    val leftColBottomPad  = when { screenH >= 700 -> 160.dp; screenH >= 400 -> 90.dp;  else -> 56.dp }
    val rightColBottomPad = when { screenH >= 700 -> 220.dp; screenH >= 400 -> 130.dp; else -> 72.dp }
    val actionBarBottomPad = when { screenH >= 700 -> 36.dp;  screenH >= 400 -> 16.dp;  else -> 8.dp  }
    val actionBarTopPad    = when { screenH >= 700 -> 20.dp;  screenH >= 400 -> 12.dp;  else -> 8.dp  }

    // Full screen: tap to hide/show UI. Start hidden if preferFullScreen is on.
    var uiOverlayVisible by remember(preferFullScreen) { mutableStateOf(!preferFullScreen) }

    // Pinch-to-zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Dialogs
    var showTargetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Wallpaper-set success snackbar
    LaunchedEffect(uiState.wallpaperSetSuccess) {
        if (uiState.wallpaperSetSuccess) {
            snackbarHostState.showSnackbar("Wallpaper set successfully!")
            viewModel.dismissSuccess()
        }
    }

    // Rephrase success snackbar
    LaunchedEffect(uiState.rephraseSuccess) {
        if (uiState.rephraseSuccess) {
            snackbarHostState.showSnackbar("Rephrased!")
            viewModel.dismissRephraseSuccess()
        }
    }

    // Download success snackbar + review trigger
    LaunchedEffect(uiState.downloadSuccess) {
        if (uiState.downloadSuccess) {
            snackbarHostState.showSnackbar("Saved to Pictures/${AppConfig.APP_NAME}!")
            viewModel.clearDownloadSuccess()
            (context as? androidx.activity.ComponentActivity)?.let {
                AppReviewManager.onWallpaperDownloaded(it)
            }
        }
    }

    // Download-with-quote trigger (fires after rewarded ad completes)
    LaunchedEffect(uiState.downloadWithQuoteTrigger) {
        if (uiState.downloadWithQuoteTrigger) {
            viewModel.downloadWithQuoteOverlay(context)
        }
    }

    // Rewarded ad trigger
    LaunchedEffect(uiState.rewardedTrigger) {
        if (uiState.rewardedTrigger) {
            val activity = context as? Activity
            activity?.let {
                AdsManager.showRewardedAd(
                    it,
                    onRewarded = { viewModel.onRewardedShown(true) },
                    onFailed = { viewModel.onRewardedShown(false) }
                )
            }
        }
    }

    // Share sheet trigger
    LaunchedEffect(uiState.shareUri) {
        uiState.shareUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, uiState.shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share wish wallpaper"))
            viewModel.clearShareSignal()
            (context as? androidx.activity.ComponentActivity)?.let {
                AppReviewManager.onWallpaperShared(it)
            }
            if (AppConfig.FEATURE_POST_SHARE_INVITE) {
                kotlinx.coroutines.delay(800)
                val result = snackbarHostState.showSnackbar(
                    message = "Shared! Invite friends to get the app free",
                    actionLabel = "Share App",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    com.offline.wallcorepro.promotion.ShareCardGenerator.shareInviteCard(context)
                }
            }
        }
    }

    // Interstitial ad trigger
    LaunchedEffect(uiState.interstitialTrigger) {
        if (uiState.interstitialTrigger) {
            val activity = context as? Activity
            activity?.let {
                AdsManager.showInterstitialIfReady(
                    it,
                    onAdDismissed = { viewModel.onInterstitialShown() }
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Wallpaper Box fills all remaining space. Menu is overlaid inside — never resizes.
            Box(modifier = Modifier.weight(1f)) {

                // Fullscreen wallpaper image
                uiState.wallpaper?.let { wallpaper ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(wallpaper.imageUrl)
                            .size(1200, 2000)
                            .crossfade(300)
                            .build(),
                        contentDescription = wallpaper.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    scale = newScale
                                    if (newScale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        scale   = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    },
                                    onTap = { uiOverlayVisible = !uiOverlayVisible }
                                )
                            }
                    )

                    // Greeting Text Overlay
                    if (uiOverlayVisible && uiState.isGreetingEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp)
                                .graphicsLayer { scaleX = 1f / scale; scaleY = 1f / scale }
                        ) {
                            GreetingOverlay(text = uiState.greetingText.ifEmpty { uiState.customText })
                            IconButton(
                                onClick  = { viewModel.toggleCustomTextDialog() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-8).dp)
                                    .background(Color(0xFFFF6F00), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Edit,
                                    contentDescription = "Edit text",
                                    tint               = Color.White,
                                    modifier           = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Loading overlay
                if (uiState.isLoading || uiState.isSettingWallpaper) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Top bar: back + share count badge
                Box(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiOverlayVisible,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        if (uiState.shareCountLabel.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF1B5E20).copy(alpha = 0.80f)
                            ) {
                                Text(
                                    text = "\u2728 ${uiState.shareCountLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
                } // end top-bar Box

                // Minimal back button when full-screen (overlay hidden)
                if (!uiOverlayVisible) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                }

                // Left column: overlay style selector + streak badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = leftColBottomPad, start = 12.dp)
                ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiOverlayVisible,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.wishStreak >= 2) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFFF6F00).copy(alpha = 0.85f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier          = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("\uD83D\uDD25", style = MaterialTheme.typography.labelSmall)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text       = "${uiState.wishStreak} day streak!",
                                        style      = MaterialTheme.typography.labelSmall,
                                        color      = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (uiState.isGreetingEnabled) {
                            val currentStyle = AppConfig.OverlayStyle.entries
                                .getOrElse(uiState.overlayStyleIndex) { AppConfig.OverlayStyle.GLOW }
                            SmallFloatingActionButton(
                                onClick        = { viewModel.cycleOverlayStyle() },
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                contentColor   = Color.White,
                                shape          = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = currentStyle.emoji, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text     = currentStyle.displayName,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
                } // end left-col Box

                // Right column: AI generate + rephrase
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp, bottom = rightColBottomPad)
                ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiOverlayVisible,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (uiState.lastMoodUsed.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text       = uiState.lastMoodUsed,
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.Bold,
                                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            if (uiState.isAiGenerating) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(24.dp),
                                    color       = Color(0xFFFF6F00),
                                    strokeWidth = 2.dp
                                )
                            }
                            FloatingActionButton(
                                onClick        = { if (!uiState.isAiGenerating) viewModel.generateAiGreeting() },
                                containerColor = if (uiState.isAiGenerating) Color(0xFF8B4500) else Color(0xFFFF6F00),
                                contentColor   = Color.White,
                                shape          = CircleShape,
                                modifier       = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, "Generate new AI wish")
                            }
                        }
                        if (uiState.generateCount > 0 && !uiState.isAiGenerating) {
                            Text(
                                text     = "Tap for a new wish",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
                } // end right-col Box

                // Bottom menu overlay — inside the Box so wallpaper size never changes
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiOverlayVisible,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                                )
                            )
                    ) {
                        // Toggle strip: With text / No text
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val cleanLabel = if (uiState.shareClean) "No text" else "With text"
                            Surface(
                                onClick = { viewModel.toggleShareClean() },
                                shape   = RoundedCornerShape(14.dp),
                                color   = if (uiState.shareClean) Color(0xFF37474F)
                                          else Color(0xFF1B5E20).copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text       = cleanLabel,
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            if (uiState.customText.isNotBlank()) {
                                Surface(
                                    onClick = { viewModel.toggleCustomTextDialog() },
                                    shape   = RoundedCornerShape(14.dp),
                                    color   = Color(0xFFFF6F00).copy(alpha = 0.85f)
                                ) {
                                    Row(
                                        modifier          = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Edit, null, Modifier.size(12.dp), tint = Color.White)
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Custom text",
                                            style      = MaterialTheme.typography.labelSmall,
                                            color      = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            // Rephrase — visible whenever a wish is overlaid on the wallpaper
                            if (uiState.isGreetingEnabled && !uiState.shareClean) {
                                Surface(
                                    onClick = { if (!uiState.isRephrasing && !uiState.isAiGenerating) viewModel.rephraseCurrentWish() },
                                    shape   = RoundedCornerShape(14.dp),
                                    color   = Color(0xFF37474F).copy(alpha = 0.88f)
                                ) {
                                    Row(
                                        modifier          = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (uiState.isRephrasing) {
                                            CircularProgressIndicator(
                                                modifier    = Modifier.size(12.dp),
                                                color       = Color(0xFF80CBC4),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.Refresh, null, Modifier.size(12.dp), tint = Color(0xFF80CBC4))
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text       = if (uiState.isRephrasing) "Rephrasing..." else "✨ Rephrase",
                                            style      = MaterialTheme.typography.labelSmall,
                                            color      = Color(0xFF80CBC4),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        BottomActionBar(
                            modifier            = Modifier.fillMaxWidth(),
                            isFavorite          = uiState.isFavorite,
                            isDownloading       = uiState.isDownloading,
                            onFavoriteClick     = {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                viewModel.toggleFavorite()
                            },
                            onSetWallpaperClick = {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                showTargetDialog = true
                            },
                            onDownloadClick     = {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                viewModel.onDownload()
                            },
                            onShareClick        = {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                viewModel.prepareAndShare(context)
                            },
                            isSharing           = uiState.isSharing,
                            isGreetingEnabled   = uiState.isGreetingEnabled,
                            onGreetingToggle    = { viewModel.toggleGreeting() },
                            bottomPad           = actionBarBottomPad,
                            topPad              = actionBarTopPad
                        )
                    }
                }
                } // end bottom-menu Box
            } // end wallpaper Box

            // Banner ad with fixed height - prevents oversized ads
            Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                BannerAdView(modifier = Modifier.fillMaxWidth().height(60.dp))
            }
        }
    }

    // Set wallpaper target dialog
    if (showTargetDialog) {
        WallpaperTargetDialog(
            onDismiss = { showTargetDialog = false },
            onTargetSelected = { target ->
                showTargetDialog = false
                viewModel.setWallpaper(context, target)
            }
        )
    }

    // Custom text editor dialog
    if (uiState.showCustomTextDialog) {
        val editableText = uiState.customText.ifBlank { uiState.greetingText }
        CustomTextDialog(
            initialText = editableText,
            onApply     = { viewModel.applyCustomText(it) },
            onClear     = { viewModel.clearCustomText() },
            onDismiss   = { viewModel.toggleCustomTextDialog() }
        )
    }
}

// ─── Custom Text Editor Dialog ────────────────────────────────────────────────

@Composable
private fun CustomTextDialog(
    initialText: String,
    onApply: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val maxChars = 160

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF1A1035),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\u270F\uFE0F", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Write on Wallpaper",
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style      = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column {
                Text(
                    text  = "Write your own message to stamp on the wallpaper",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.60f)
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value         = text,
                    onValueChange = { if (it.length <= maxChars) text = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(
                            "Good morning! Sending you love & sunshine today",
                            color = Color.White.copy(alpha = 0.35f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFFFF6F00),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = Color(0xFFFF6F00)
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text      = "${text.length} / $maxChars",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = if (text.length > maxChars * 0.9) Color(0xFFFF5252)
                                else Color.White.copy(alpha = 0.40f),
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialText.isNotBlank()) {
                    TextButton(onClick = onClear) {
                        Text("Reset", color = Color(0xFFFF5252))
                    }
                }
                Button(
                    onClick = { onApply(text.trim()) },
                    enabled = text.isNotBlank(),
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6F00))
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}

// ─── Bottom Action Bar ────────────────────────────────────────────────────────
//
// Layout (left → right):
//   ♥ Save  |  ↓ Download  |  ● SET (60 dp circle, primary)  |  ↑ Share  |  ✏ Wish
//
// "Set Wallpaper" is the primary action so it sits in the center with a filled
// circle background (60 dp) making it visually distinct.  All other actions are
// uniform 28 dp icon + label columns flanking it symmetrically.

@Composable
private fun BottomActionBar(
    modifier: Modifier,
    isFavorite: Boolean,
    isDownloading: Boolean,
    isSharing: Boolean = false,
    onFavoriteClick: () -> Unit,
    onSetWallpaperClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    isGreetingEnabled: Boolean,
    onGreetingToggle: () -> Unit,
    bottomPad: androidx.compose.ui.unit.Dp = 36.dp,
    topPad: androidx.compose.ui.unit.Dp = 20.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                )
            )
            .padding(bottom = bottomPad, top = topPad)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // 1. Save / Favourite
            if (AppConfig.FEATURE_FAVORITES) {
                ActionButton(
                    icon    = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label   = if (isFavorite) "Saved" else "Save",
                    tint    = if (isFavorite) Color(0xFFFF5252) else Color.White,
                    onClick = onFavoriteClick
                )
            }

            // 2. Download
            if (AppConfig.FEATURE_DOWNLOAD) {
                if (isDownloading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(28.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Saving…",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                } else {
                    ActionButton(
                        icon    = Icons.Default.Download,
                        label   = "Download",
                        tint    = Color.White,
                        onClick = onDownloadClick
                    )
                }
            }

            // 3. Set Wallpaper — CENTER primary action (60 dp filled circle)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.clickable { onSetWallpaperClick() }
            ) {
                Box(
                    modifier        = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Wallpaper,
                        contentDescription = "Set Wallpaper",
                        tint               = Color.White,
                        modifier           = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text       = "Set",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4. Share
            if (AppConfig.FEATURE_SHARE) {
                if (isSharing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(28.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Sharing…",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                } else {
                    ActionButton(
                        icon    = Icons.Default.Share,
                        label   = "Share",
                        tint    = Color.White,
                        onClick = onShareClick
                    )
                }
            }

            // 5. Show / Hide Wish
            ActionButton(
                icon    = if (isGreetingEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                label   = if (isGreetingEnabled) "Hide Wish" else "Show Wish",
                tint    = if (isGreetingEnabled) Color(0xFFFF6F00) else Color.White,
                onClick = onGreetingToggle
            )
        }
    }
}

// ─── Action Button ────────────────────────────────────────────────────────────

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.clickable { onClick() }
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

// ─── Greeting Overlay ─────────────────────────────────────────────────────────

@Composable
private fun GreetingOverlay(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color    = Color.Black.copy(alpha = 0.35f),
        shape    = RoundedCornerShape(24.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            Text(
                text      = text,
                style     = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 32.sp
                ),
                color     = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Wallpaper Target Dialog ──────────────────────────────────────────────────

@Composable
private fun WallpaperTargetDialog(
    onDismiss: () -> Unit,
    onTargetSelected: (WallpaperTarget) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Set Wallpaper As", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WallpaperTargetOption(
                    title    = "Home Screen",
                    subtitle = "Set as your home screen background",
                    onClick  = { onTargetSelected(WallpaperTarget.HOME) }
                )
                WallpaperTargetOption(
                    title    = "Lock Screen",
                    subtitle = "Set as your lock screen background",
                    onClick  = { onTargetSelected(WallpaperTarget.LOCK) }
                )
                WallpaperTargetOption(
                    title    = "Both Screens",
                    subtitle = "Set on home and lock screen",
                    onClick  = { onTargetSelected(WallpaperTarget.BOTH) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun WallpaperTargetOption(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape  = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

