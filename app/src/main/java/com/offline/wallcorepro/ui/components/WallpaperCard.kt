package com.offline.wallcorepro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.data.local.WishQuotePool
import com.offline.wallcorepro.domain.model.Wallpaper

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    onFavoriteToggle: ((Wallpaper, Boolean) -> Unit)? = null,
    showTrendingBadge: Boolean = false,
    showQuote: Boolean = true,
    cardHeight: Dp = 230.dp,
    selectedCategories: Set<String> = AppConfig.QuoteCategory.defaults,
    modifier: Modifier = Modifier
) {
    var isFavorite by remember(wallpaper.id) { mutableStateOf(wallpaper.isFavorite) }
    val view = LocalView.current

    // Deterministic quote — same wallpaper ID + same categories → same quote every time.
    // This guarantees the text on this card matches exactly what appears in the detail screen.
    val quote = remember(wallpaper.id, wallpaper.category, selectedCategories) {
        WishQuotePool.getStableQuote(wallpaper.id, wallpaper.category, selectedCategories)
    }

    // Card press scale animation
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "card_scale"
    )

    // Heart pop animation
    var heartPopped by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue      = if (heartPopped) 1.4f else 1f,
        animationSpec    = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label            = "heart_scale",
        finishedListener = { heartPopped = false }
    )

    val context = LocalContext.current

    // Memoize so the request object isn't rebuilt on every recomposition.
    // Cache keys are set explicitly — Coil derives them from the URL by default
    // but explicit keys guarantee the same key is used for both the prefetch
    // (HomeScreen) and the card, producing a cache hit instead of a re-fetch.
    val thumbUrl = remember(wallpaper.id) {
        wallpaper.thumbnailUrl.ifEmpty { wallpaper.imageUrl }
    }
    // Parse the dominant color stored alongside the wallpaper — shows instantly as
    // a placeholder while the thumbnail downloads/decodes, making loading feel fast.
    val placeholderColor = remember(wallpaper.id) {
        try   { Color(android.graphics.Color.parseColor(wallpaper.dominantColor)) }
        catch (_: Exception) { Color(0xFF2A2A2A) }
    }
    val imageRequest = remember(thumbUrl) {
        ImageRequest.Builder(context)
            .data(thumbUrl)
            .size(320, 480)
            .memoryCacheKey(thumbUrl)
            .diskCacheKey(thumbUrl)
            .crossfade(150)
            // Wallpapers have no alpha channel — RGB_565 uses 2 bytes/pixel instead
            // of ARGB_8888's 4 bytes: 2× smaller bitmap = 2× faster decode & less RAM.
            .allowRgb565(true)
            .build()
    }

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Wallpaper image ─────────────────────────────────────────────
            // AsyncImage (NOT SubcomposeAsyncImage) — no sub-composition means
            // 3× faster composition per card during fast scrolling.
            // Uses the memoized imageRequest (built once per unique URL) so
            // recompositions caused by unrelated state changes never rebuild it.
            AsyncImage(
                model              = imageRequest,
                contentDescription = wallpaper.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxSize()
                    .background(placeholderColor)  // dominant color shows instantly while loading
            )

            // ── Bottom gradient scrim (deeper when quote is shown) ────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(if (showQuote) 0.72f else 0.55f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f   to Color.Transparent,
                            0.3f to Color.Black.copy(alpha = 0.15f),
                            1f   to Color.Black.copy(alpha = 0.88f)
                        )
                    )
            )

            // ── Top-left: Category badge ──────────────────────────────────
            if (wallpaper.category.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape    = RoundedCornerShape(8.dp),
                    color    = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text(
                        text     = wallpaper.category,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // ── Top-right: Trending / Premium / Social Proof badges ──────────
            Column(
                modifier            = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Social proof share counter
                if (com.offline.wallcorepro.config.AppConfig.FEATURE_SOCIAL_PROOF) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1B5E20).copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "✨ ${com.offline.wallcorepro.config.AppConfig.getSimulatedShareCount(wallpaper.id)}",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                if (showTrendingBadge || wallpaper.isTrending) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(10.dp), Color.White)
                            Text("Hot", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (wallpaper.isPremium) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.9f)
                    ) {
                        Text(
                            text       = "PRO",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color(0xFF1A0A00),
                            fontWeight = FontWeight.ExtraBold,
                            modifier   = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // ── Bottom area: Quote + title row ────────────────────────────
            Column(
                modifier            = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quote overlay
                if (showQuote) {
                    Text(
                        text      = "\u201C$quote\u201D",
                        style     = TextStyle(
                            fontSize   = 11.sp,
                            lineHeight = 15.sp,
                            fontStyle  = FontStyle.Italic,
                            fontWeight = FontWeight.Normal,
                            color      = Color.White.copy(alpha = 0.93f),
                            textAlign  = TextAlign.Center
                        ),
                        maxLines  = 3,
                        overflow  = TextOverflow.Ellipsis,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }

                // Title + downloads + heart row
                Row(
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (wallpaper.title.isNotEmpty() && wallpaper.title != "Wallpaper") {
                            Text(
                                text       = wallpaper.title,
                                style      = MaterialTheme.typography.labelMedium,
                                color      = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines   = 1,
                                fontSize   = 11.sp
                            )
                        }
                        if (wallpaper.downloadsCount > 0) {
                            Text(
                                text     = "⬇ ${formatCount(wallpaper.downloadsCount)}",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = Color.White.copy(0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Quick-favourite heart button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.Black.copy(0.45f), CircleShape)
                            .pointerInput(isFavorite) {
                                detectTapGestures {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                    val newState = !isFavorite
                                    isFavorite   = newState
                                    heartPopped  = true
                                    onFavoriteToggle?.invoke(wallpaper, newState)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                            tint               = if (isFavorite) Color(0xFFFF5252) else Color.White,
                            modifier           = Modifier
                                .size(18.dp)
                                .scale(heartScale)
                        )
                    }
                }
            }
        }
    }
}

// ─── Shimmer Loading Card ─────────────────────────────────────────────────────

@Composable
fun ShimmerWallpaperCard(modifier: Modifier = Modifier, height: Dp = 230.dp) {
    val shimmerColors = listOf(
        Color(0xFF2A1A1A).copy(alpha = 0.8f),
        Color(0xFF3D2B2B).copy(alpha = 0.4f),
        Color(0xFF2A1A1A).copy(alpha = 0.8f)
    )
    val transition   = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1000f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start  = Offset.Zero,
        end    = Offset(translateAnim, translateAnim)
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
    )
}

// ─── Hero / Featured Card ─────────────────────────────────────────────────────

@Composable
fun HeroWallpaperCard(
    wallpaper: Wallpaper,
    label: String = "✨ Today's Pick",
    onClick: () -> Unit,
    onFavoriteToggle: ((Wallpaper, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFavorite by remember(wallpaper.id) { mutableStateOf(wallpaper.isFavorite) }
    val view = LocalView.current
    var isPressed  by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "hero_scale"
    )

    var heartPopped by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue      = if (heartPopped) 1.5f else 1f,
        animationSpec    = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label            = "hero_heart_scale",
        finishedListener = { heartPopped = false }
    )

    // Deterministic hero quote — same as what the detail screen will show
    val heroQuote = remember(wallpaper.id, wallpaper.category) {
        WishQuotePool.getStableQuote(wallpaper.id, wallpaper.category)
    }

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .height(300.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap   = { onClick() }
                )
            },
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(wallpaper.thumbnailUrl.ifEmpty { wallpaper.imageUrl })
                    .size(800, 900)
                    .crossfade(400)
                    .build(),
                contentDescription = wallpaper.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )

            // Deep gradient scrim for the hero card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f   to Color.Transparent,
                            0.35f to Color.Transparent,
                            0.65f to Color.Black.copy(alpha = 0.4f),
                            1f   to Color.Black.copy(alpha = 0.9f)
                        )
                    )
            )

            // Label chip — top-left
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                shape    = RoundedCornerShape(20.dp),
                color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
            ) {
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.labelMedium,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Heart button — top-right
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.35f))
                    .pointerInput(isFavorite) {
                        detectTapGestures {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            val newState = !isFavorite
                            isFavorite   = newState
                            heartPopped  = true
                            onFavoriteToggle?.invoke(wallpaper, newState)
                        }
                    },
                color = Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint               = if (isFavorite) Color(0xFFFF5252) else Color.White,
                        modifier           = Modifier
                            .size(24.dp)
                            .scale(heartScale)
                    )
                }
            }

            // Bottom: quote + title + photographer
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Hero quote — larger and more prominent
                Text(
                    text  = "\u201C$heroQuote\u201D",
                    style = TextStyle(
                        fontSize   = 13.sp,
                        lineHeight = 18.sp,
                        fontStyle  = FontStyle.Italic,
                        fontWeight = FontWeight.Light,
                        color      = Color.White.copy(alpha = 0.95f),
                        textAlign  = TextAlign.Start
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (wallpaper.title.isNotEmpty()) {
                    Text(
                        text       = wallpaper.title,
                        style      = MaterialTheme.typography.titleMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines   = 1
                    )
                }
            }

            // "Tap to set" hint — bottom-right
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Text(
                    text     = "Tap to set →",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000     -> "${count / 1_000}K"
    else               -> count.toString()
}
