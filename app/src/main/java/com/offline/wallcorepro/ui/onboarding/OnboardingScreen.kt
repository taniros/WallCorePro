package com.offline.wallcorepro.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val isGuide: Boolean = false,
    val guideSteps: List<String> = emptyList()
)

private val pages = listOf(
    OnboardingPage(
        emoji     = "☀️",
        title     = "Morning Greetings",
        subtitle  = "Start every day with beautiful morning wishes and heartfelt greetings that set the tone for success and joy.",
        gradient  = listOf(Color(0xFFFFF3E0), Color(0xFFFFCC02), Color(0xFFFF6F00))
    ),
    OnboardingPage(
        emoji     = "🌙",
        title     = "Nightly Wishes",
        subtitle  = "End your day with serene good night wishes and peaceful moonlight wallpapers to help you unwind and dream sweet.",
        gradient  = listOf(Color(0xFF090E1A), Color(0xFF1A1035), Color(0xFF2D1B69))
    ),
    OnboardingPage(
        emoji     = "✨",
        title     = "Share the Magic",
        subtitle  = "Easily copy and share stunning morning and night greetings with your loved ones to keep your connections bright.",
        gradient  = listOf(Color(0xFF1A1015), Color(0xFF3D1B00), Color(0xFFFF6F00))
    ),
    OnboardingPage(
        emoji       = "📖",
        title       = "Quick Start Guide",
        subtitle    = "Here's how to get the most from the app:",
        gradient    = listOf(Color(0xFF0D1B3E), Color(0xFF1A237E), Color(0xFF311B92)),
        isGuide     = true,
        guideSteps  = listOf(
            "1. Browse — scroll to find beautiful morning & night wallpapers",
            "2. Tap a wallpaper — add your message or use the AI wish",
            "3. WishAI — generate unique personalised wishes in seconds",
            "4. Share — send to loved ones or set as your wallpaper",
            "5. Save favorites — heart the ones you love for quick access"
        )
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState   = rememberPagerState(pageCount = { pages.size })
    val scope        = rememberCoroutineScope()
    val isLastPage   = pagerState.currentPage == pages.lastIndex

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Animated gradient background ──────────────────────────────
        val bgGradient = pages[pagerState.currentPage].gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(bgGradient))
        )

        // ── Pager ─────────────────────────────────────────────────────
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        // ── Bottom controls ───────────────────────────────────────────
        Column(
            modifier              = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(24.dp)
        ) {
            // Dot indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { idx ->
                    val selected = pagerState.currentPage == idx
                    val width    by animateDpAsState(
                        targetValue    = if (selected) 24.dp else 8.dp,
                        animationSpec  = tween(300),
                        label          = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            // Next / Get Started button
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor   = Color(0xFF1A0A00)
                )
            ) {
                Text(
                    text       = if (isLastPage) "✨ Get Started" else "Next →",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Skip
            if (!isLastPage) {
                TextButton(onClick = onFinish) {
                    Text(
                        text  = "Skip",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val bounceAnim = rememberInfiniteTransition(label = "bounce")
    val emojiOffsetY by bounceAnim.animateFloat(
        initialValue  = 0f,
        targetValue   = -16f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_bounce"
    )

    Column(
        modifier              = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(bottom = 180.dp)
            .padding(top = 48.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = if (page.isGuide) Arrangement.Top else Arrangement.Center
    ) {
        // Floating emoji (smaller on guide page)
        Text(
            text     = page.emoji,
            fontSize = if (page.isGuide) 56.sp else 96.sp,
            modifier = Modifier.offset(y = if (page.isGuide) 0.dp else emojiOffsetY.dp)
        )

        Spacer(Modifier.height(if (page.isGuide) 16.dp else 40.dp))

        // Title
        Text(
            text       = page.title,
            style      = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White,
            textAlign  = TextAlign.Center,
            lineHeight = 32.sp
        )

        Spacer(Modifier.height(12.dp))

        // Subtitle
        Text(
            text      = page.subtitle,
            style     = MaterialTheme.typography.bodyLarge,
            color     = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        // Guide steps (only when isGuide)
        if (page.isGuide && page.guideSteps.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (step in page.guideSteps) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text      = step,
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = Color.White.copy(alpha = 0.95f),
                            lineHeight = 20.sp,
                            modifier  = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private val EaseInOut = androidx.compose.animation.core.Easing { t ->
    if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
}
