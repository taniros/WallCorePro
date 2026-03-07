package com.offline.wallcorepro.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.promotion.ShareCardGenerator

@Composable
fun ExitConfirmationDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current

    // Pulsing emoji animation
    val infiniteAnim = rememberInfiniteTransition(label = "exit")
    val emojiScale by infiniteAnim.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )
    val glowAlpha by infiniteAnim.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 0.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    var showMoreApps by remember { mutableStateOf(false) }
    val activePromos = AppConfig.PROMO_APPS.filter { it.isEnabled }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape     = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1A1035), Color(0xFF0D0A20), Color(0xFF0D0A20))
                            )
                        )
                ) {
                    // Background glow orbs
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-20).dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF7C4DFF).copy(alpha = glowAlpha), Color.Transparent)
                                ), CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-20).dp, y = 20.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFF6F00).copy(alpha = glowAlpha * 0.6f), Color.Transparent)
                                ), CircleShape
                            )
                    )

                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Animated emoji
                        Text(
                            text     = "👋",
                            fontSize = 56.sp,
                            modifier = Modifier.scale(emojiScale)
                        )

                        Spacer(Modifier.height(12.dp))

                        // Title
                        Text(
                            text       = "Leaving so soon?",
                            style      = MaterialTheme.typography.headlineSmall,
                            color      = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign  = TextAlign.Center
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text      = "Beautiful wishes are waiting for you! ✨",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = Color.White.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(20.dp))

                        // App name badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF7C4DFF).copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("☀️", fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text       = AppConfig.APP_NAME,
                                    style      = MaterialTheme.typography.titleSmall,
                                    color      = Color(0xFFD4BFFF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // ── Banner Ad slot ─────────────────────────────────────
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.30f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BannerAdView(modifier = Modifier.padding(4.dp))
                        }

                        Spacer(Modifier.height(20.dp))

                        // ── Action buttons ─────────────────────────────────────

                        // Rate App
                        ExitActionButton(
                            emoji    = "⭐",
                            label    = "Rate Our App",
                            subLabel = "Takes only 30 seconds!",
                            color    = Color(0xFFFF8F00),
                            onClick  = {
                                openPlayStore(context, AppConfig.PACKAGE_NAME)
                                onDismiss()
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        // Share App (organic install driver)
                        if (AppConfig.FEATURE_EXIT_SHARE_PROMPT) {
                            ExitActionButton(
                                emoji    = "💌",
                                label    = "Share App With Friends",
                                subLabel = "They'll love beautiful wishes too!",
                                color    = Color(0xFFE91E63),
                                onClick  = {
                                    ShareCardGenerator.shareInviteCard(context)
                                    onDismiss()
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        // More Apps (only if promo apps exist)
                        if (AppConfig.FEATURE_CROSS_PROMOTION && activePromos.isNotEmpty()) {
                            ExitActionButton(
                                emoji    = "📱",
                                label    = "More Apps From Us",
                                subLabel = "${activePromos.size} free app${if (activePromos.size > 1) "s" else ""} you'll love",
                                color    = Color(0xFF1565C0),
                                onClick  = { showMoreApps = true }
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                        Spacer(Modifier.height(16.dp))

                        // ── Stay / Exit row ────────────────────────────────────
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Exit button
                            OutlinedButton(
                                onClick  = onExit,
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(16.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF5252)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color(0xFFFF5252).copy(alpha = 0.6f)
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFFFF5252))
                                Spacer(Modifier.width(6.dp))
                                Text("Exit", fontWeight = FontWeight.SemiBold)
                            }

                            // Stay button (primary CTA)
                            Button(
                                onClick  = onDismiss,
                                modifier = Modifier.weight(1.5f),
                                shape    = RoundedCornerShape(16.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7C4DFF)
                                )
                            ) {
                                Icon(Icons.Default.Favorite, null,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Keep Exploring", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // More Apps sub-dialog
    if (showMoreApps && activePromos.isNotEmpty()) {
        MoreAppsDialog(
            apps      = activePromos,
            onDismiss = { showMoreApps = false }
        )
    }
}

@Composable
private fun ExitActionButton(
    emoji: String,
    label: String,
    subLabel: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(16.dp),
        color    = color.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.bodyLarge,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = subLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint     = color.copy(alpha = 0.80f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MoreAppsDialog(
    apps: List<AppConfig.PromoApp>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF1A1035),
        title = {
            Text(
                "📱 More Free Apps",
                color      = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                apps.forEach { app ->
                    Surface(
                        shape   = RoundedCornerShape(12.dp),
                        color   = Color(app.accentColor).copy(alpha = 0.15f),
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(app.playStoreUrl))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    ) {
                        Row(
                            modifier          = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.iconEmoji, fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.name,    color = Color.White, fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(app.tagline, color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(app.accentColor).copy(alpha = 0.80f)
                            ) {
                                Text(
                                    text     = "GET",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color    = Color.White,
                                    style    = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF7C4DFF))
            }
        }
    )
}

private fun openPlayStore(context: Context, packageName: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
