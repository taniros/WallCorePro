package com.offline.wallcorepro.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline.wallcorepro.config.AppConfig

/**
 * Full-width promotional card shown in the wallpaper feed.
 * All content is driven by [AppConfig.PromoApp] — nothing is hardcoded here.
 */
@Composable
fun PromoAppCard(
    app: AppConfig.PromoApp,
    modifier: Modifier = Modifier
) {
    val context      = LocalContext.current
    val accentColor  = Color(app.accentColor)
    val accentDark   = accentColor.copy(alpha = 0.7f)

    // Subtle shimmer pulse to draw attention without being annoying
    val infiniteAnim = rememberInfiniteTransition(label = "promo_pulse")
    val glowAlpha by infiniteAnim.animateFloat(
        initialValue  = 0.08f,
        targetValue   = 0.18f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "promo_glow"
    )

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = glowAlpha + 0.06f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            accentDark.copy(alpha = glowAlpha)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon
                Box(
                    modifier         = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(accentColor.copy(0.35f), accentColor.copy(0.1f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.iconEmoji, fontSize = 30.sp, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // "Discover Our Apps" label
                    Text(
                        "Discover Our Apps",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            app.name,
                            fontWeight = FontWeight.ExtraBold,
                            style      = MaterialTheme.typography.titleSmall,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                app.badgeText,
                                modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style      = MaterialTheme.typography.labelSmall,
                                color      = accentColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        app.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(12.dp))

                // CTA button
                Button(
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=${app.packageName}"))
                            )
                        } catch (e: Exception) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(app.playStoreUrl))
                            )
                        }
                    },
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Try Free",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 13.sp,
                        color      = Color.White
                    )
                }
            }
        }
    }
}
