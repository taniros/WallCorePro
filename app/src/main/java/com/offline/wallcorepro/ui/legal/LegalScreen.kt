package com.offline.wallcorepro.ui.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline.wallcorepro.config.AppConfig

// ─── Privacy Policy ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBackClick: () -> Unit) {
    LegalPageScaffold(
        title      = "Privacy Policy",
        emoji      = "🔒",
        onBackClick = onBackClick
    ) {
        LegalSection("Last updated: March 2025")

        LegalSection(
            title = "1. Information We Collect",
            body  = "We do not collect any personally identifiable information. " +
                    "${AppConfig.APP_NAME} works fully offline after the initial wallpaper sync. " +
                    "We do not require you to create an account or provide your name, email, or phone number."
        )

        LegalSection(
            title = "2. Wallpaper Content",
            body  = "Wallpapers are sourced from Pexels and Pixabay via their public APIs. " +
                    "All images are provided under their respective free-use licenses. " +
                    "We do not host or store images on our own servers."
        )

        LegalSection(
            title = "3. Local Storage",
            body  = "The app stores wallpapers in a local cache on your device to allow offline access. " +
                    "Downloaded wallpapers are saved to your device gallery only when you explicitly choose to download them. " +
                    "All locally stored data stays on your device and is never transmitted to us."
        )

        LegalSection(
            title = "4. Notifications",
            body  = "If you enable notifications, the app schedules daily Good Morning and Good Night reminders " +
                    "using Android's WorkManager. These notifications are processed entirely on your device. " +
                    "No notification data is sent to external servers."
        )

        LegalSection(
            title = "5. Advertising",
            body  = "This app uses Google AdMob to display advertisements. " +
                    "AdMob may collect and use data in accordance with Google's Privacy Policy " +
                    "(https://policies.google.com/privacy). " +
                    "You may opt out of personalised ads through your device settings under " +
                    "Google > Ads > Opt out of Ads Personalisation."
        )

        LegalSection(
            title = "6. Firebase Analytics & Crashlytics",
            body  = "We use Firebase Analytics and Crashlytics to understand app usage and improve stability. " +
                    "These services may collect anonymous crash reports and usage statistics. " +
                    "No personally identifiable information is included in these reports."
        )

        LegalSection(
            title = "7. Permissions",
            body  = "The app requests the following permissions:\n\n" +
                    "• INTERNET — to fetch wallpapers from Pexels and Pixabay.\n" +
                    "• WRITE_EXTERNAL_STORAGE — to save downloaded wallpapers to your gallery (Android 9 and below).\n" +
                    "• SET_WALLPAPER — to apply a wallpaper directly to your home screen.\n" +
                    "• POST_NOTIFICATIONS — to send Good Morning and Good Night reminders (optional).\n" +
                    "• USE_BIOMETRIC — to lock the Saved Wallpapers section (optional, only if you enable it)."
        )

        LegalSection(
            title = "8. Third-Party Links",
            body  = "The app may contain links to third-party apps or the Google Play Store. " +
                    "We are not responsible for the privacy practices of any third-party services."
        )

        LegalSection(
            title = "9. Children's Privacy",
            body  = "This app is not directed to children under 13. " +
                    "We do not knowingly collect information from children. " +
                    "All content is nature-themed and family-friendly."
        )

        LegalSection(
            title = "10. Changes to This Policy",
            body  = "We may update this Privacy Policy from time to time. " +
                    "Any changes will be reflected in the app with a new effective date."
        )

        LegalSection(
            title = "11. Contact Us",
            body  = "If you have any questions about this Privacy Policy, please contact us at:\n\n" +
                    AppConfig.FEEDBACK_EMAIL
        )
    }
}

// ─── Terms of Service ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(onBackClick: () -> Unit) {
    LegalPageScaffold(
        title      = "Terms of Service",
        emoji      = "⚖️",
        onBackClick = onBackClick
    ) {
        LegalSection("Last updated: March 2025")

        LegalSection(
            title = "1. Acceptance of Terms",
            body  = "By downloading or using ${AppConfig.APP_NAME}, you agree to be bound by these Terms of Service. " +
                    "If you do not agree to these terms, please do not use the app."
        )

        LegalSection(
            title = "2. License",
            body  = "We grant you a personal, non-exclusive, non-transferable, limited license to use " +
                    "${AppConfig.APP_NAME} on your Android device for personal, non-commercial purposes."
        )

        LegalSection(
            title = "3. Wallpaper Content & Intellectual Property",
            body  = "Wallpapers displayed in the app are sourced from Pexels and Pixabay. " +
                    "Each image is subject to the original photographer's license terms. " +
                    "When you download a wallpaper for personal use, you agree to respect the " +
                    "applicable license (Pexels License / Pixabay License). " +
                    "You may NOT use downloaded images for commercial purposes unless explicitly permitted by the original license."
        )

        LegalSection(
            title = "4. User-Generated Content",
            body  = "You may write custom text to overlay on wallpapers. " +
                    "You are solely responsible for any text you add. " +
                    "Do not add content that is offensive, illegal, or violates any third-party rights."
        )

        LegalSection(
            title = "5. Prohibited Uses",
            body  = "You agree not to:\n\n" +
                    "• Use the app for any commercial purpose without our written consent.\n" +
                    "• Reproduce, distribute, or publicly display app content without permission.\n" +
                    "• Reverse engineer, decompile, or attempt to extract the source code.\n" +
                    "• Use the app in any way that violates applicable laws or regulations.\n" +
                    "• Upload or share content that infringes on intellectual property rights."
        )

        LegalSection(
            title = "6. Disclaimer of Warranties",
            body  = "The app is provided \"as is\" without warranties of any kind, either express or implied. " +
                    "We do not warrant that the app will be error-free, uninterrupted, or free of viruses. " +
                    "We make no guarantees about the accuracy or completeness of wallpaper descriptions or quotes."
        )

        LegalSection(
            title = "7. Limitation of Liability",
            body  = "To the maximum extent permitted by applicable law, we shall not be liable for any " +
                    "indirect, incidental, special, or consequential damages arising from your use of the app, " +
                    "including but not limited to loss of data, loss of profits, or any other losses."
        )

        LegalSection(
            title = "8. Advertising",
            body  = "The app displays ads provided by Google AdMob. By using the app, you acknowledge that " +
                    "advertisements may be shown. Ad revenue supports the continued development of the app."
        )

        LegalSection(
            title = "9. Modifications",
            body  = "We reserve the right to modify or discontinue the app at any time without notice. " +
                    "We may also update these Terms of Service at any time. " +
                    "Your continued use of the app after changes constitutes acceptance of the new terms."
        )

        LegalSection(
            title = "10. Governing Law",
            body  = "These terms shall be governed by and construed in accordance with applicable laws, " +
                    "without regard to conflict of law provisions."
        )

        LegalSection(
            title = "11. Contact",
            body  = "For any questions regarding these Terms, please contact us at:\n\n" +
                    AppConfig.FEEDBACK_EMAIL
        )
    }
}

// ─── Shared scaffolding ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegalPageScaffold(
    title: String,
    emoji: String,
    onBackClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(emoji, fontSize = 18.sp)
                        Text(
                            text       = title,
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
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LegalSection(body: String) {
    Text(
        text  = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun LegalSection(title: String, body: String) {
    Card(
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text  = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Start
            )
        }
    }
}
