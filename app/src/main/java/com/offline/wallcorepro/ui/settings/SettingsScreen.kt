package com.offline.wallcorepro.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.domain.model.AutoWallpaperInterval
import com.offline.wallcorepro.domain.model.WallpaperTarget
import com.offline.wallcorepro.ui.theme.MoonPurple
import com.offline.wallcorepro.ui.theme.SunriseAmber
import com.offline.wallcorepro.ui.theme.SunriseOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsOfServiceClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showIntervalPicker    by remember { mutableStateOf(false) }
    var showTargetPicker      by remember { mutableStateOf(false) }
    var showNameDialog        by remember { mutableStateOf(false) }
    var showCategoryPicker    by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            AppHeroCard(userName = uiState.userName)

            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // ── Appearance ────────────────────────────────────────
                SettingGroupCard(label = "🎨 Appearance") {
                    SettingsToggleRow(
                        icon      = Icons.Default.DarkMode,
                        iconTint  = MoonPurple,
                        title     = "Dark Mode",
                        subtitle  = "Use the deep moonlit theme",
                        checked   = uiState.isDarkMode,
                        onChecked = { viewModel.setDarkMode(it) }
                    )
                    if (AppConfig.FEATURE_AUTO_THEME) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingsToggleRow(
                            icon      = Icons.Default.AutoAwesome,
                            iconTint  = SunriseAmber,
                            title     = "Energy-Based Theme",
                            subtitle  = "Morning → bright · Night → dark (auto)",
                            checked   = uiState.autoTheme,
                            onChecked = { viewModel.setAutoTheme(it) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Personalization ───────────────────────────────────
                SettingGroupCard(label = "👤 Personalization") {
                    SettingsClickRow(
                        icon     = Icons.Default.Person,
                        iconTint = SunriseOrange,
                        title    = "Your Name",
                        value    = uiState.userName.ifEmpty { "Set name" },
                        onClick  = { showNameDialog = true }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Quote Style ───────────────────────────────────────
                SettingGroupCard(label = "✍️ Quote Style") {
                    // Show active category count as the value hint
                    val activeCatCount = uiState.selectedQuoteCategories.size
                    val allCount       = AppConfig.QuoteCategory.entries.size
                    SettingsClickRow(
                        icon     = Icons.Default.FormatQuote,
                        iconTint = SunriseAmber,
                        title    = "Quote Categories",
                        value    = "$activeCatCount / $allCount active",
                        onClick  = { showCategoryPicker = true }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── More Apps ─────────────────────────────────────────
                if (AppConfig.FEATURE_CROSS_PROMOTION && AppConfig.PROMO_APPS.isNotEmpty()) {
                    SettingGroupCard(label = "📱 More Apps") {
                        AppConfig.PROMO_APPS
                            .filter { it.isEnabled }
                            .forEachIndexed { index, promoApp ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color    = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                                PromoAppRow(
                                    app     = promoApp,
                                    onClick = {
                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW,
                                                    Uri.parse("market://details?id=${promoApp.packageName}"))
                                            )
                                        } catch (e: Exception) {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW,
                                                    Uri.parse(promoApp.playStoreUrl))
                                            )
                                        }
                                    }
                                )
                            }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── Notifications ─────────────────────────────────────
                SettingGroupCard(label = "🔔 Notifications") {
                    SettingsToggleRow(
                        icon      = Icons.Default.WbSunny,
                        iconTint  = SunriseAmber,
                        title     = "Good Morning",
                        subtitle  = "Daily reminder at 7:00 AM",
                        checked   = uiState.isMorningNotifEnabled,
                        onChecked = { viewModel.setMorningNotifEnabled(it, context) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsToggleRow(
                        icon      = Icons.Default.WbSunny,
                        iconTint  = Color(0xFFFF9800),
                        title     = "Good Afternoon",
                        subtitle  = "Daily reminder at 2:00 PM",
                        checked   = uiState.isAfternoonNotifEnabled,
                        onChecked = { viewModel.setAfternoonNotifEnabled(it, context) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsToggleRow(
                        icon      = Icons.Default.Nightlight,
                        iconTint  = Color(0xFF7B1FA2),
                        title     = "Good Evening",
                        subtitle  = "Daily reminder at 7:00 PM",
                        checked   = uiState.isEveningNotifEnabled,
                        onChecked = { viewModel.setEveningNotifEnabled(it, context) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsToggleRow(
                        icon      = Icons.Default.Nightlight,
                        iconTint  = MoonPurple,
                        title     = "Good Night",
                        subtitle  = "Daily reminder at 9:00 PM",
                        checked   = uiState.isNightNotifEnabled,
                        onChecked = { viewModel.setNightNotifEnabled(it, context) }
                    )
                }

                // ── Smart Reminder Intelligence ───────────────────────
                if (AppConfig.FEATURE_SMART_REMINDER &&
                    !uiState.isSmartReminderDismissed &&
                    uiState.smartReminderHours != null) {
                    Spacer(Modifier.height(12.dp))
                    SmartReminderBanner(
                        hours     = uiState.smartReminderHours!!,
                        onApply   = { viewModel.applySmartReminder(context) },
                        onDismiss = { viewModel.dismissSmartReminder() }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Privacy & Security ────────────────────────────────
                if (AppConfig.FEATURE_BIOMETRIC_FAVORITES) {
                    SettingGroupCard(label = "🔒 Privacy & Security") {
                        SettingsToggleRow(
                            icon      = Icons.Default.Fingerprint,
                            iconTint  = Color(0xFF7C4DFF),
                            title     = "Lock Saved Wallpapers",
                            subtitle  = "Require biometrics to view favorites",
                            checked   = uiState.isFavoritesLocked,
                            onChecked = { viewModel.setFavoritesLocked(it) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Auto Wallpaper ────────────────────────────────────
                if (AppConfig.FEATURE_AUTO_WALLPAPER) {
                    SettingGroupCard(label = "⚙️ Auto Wallpaper") {
                        SettingsToggleRow(
                            icon      = Icons.Default.AutoAwesome,
                            iconTint  = SunriseOrange,
                            title     = "Auto-Change Wallpaper",
                            subtitle  = "Automatically rotate wallpapers on schedule",
                            checked   = uiState.isAutoWallpaperEnabled,
                            onChecked = { viewModel.setAutoWallpaperEnabled(it, context) }
                        )
                        AnimatedVisibility(
                            visible = uiState.isAutoWallpaperEnabled,
                            enter   = expandVertically() + fadeIn(),
                            exit    = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                                SettingsClickRow(
                                    icon     = Icons.Default.Timer,
                                    iconTint = SunriseAmber,
                                    title    = "Change Interval",
                                    value    = "${uiState.autoWallpaperIntervalHours}h",
                                    onClick  = { showIntervalPicker = true }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                                SettingsClickRow(
                                    icon     = Icons.Default.Wallpaper,
                                    iconTint = MoonPurple,
                                    title    = "Apply To",
                                    value    = uiState.autoWallpaperTarget.replaceFirstChar { it.uppercase() },
                                    onClick  = { showTargetPicker = true }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── Connect ───────────────────────────────────────────
                SettingGroupCard(label = "⭐ Connect") {
                    SettingsClickRow(
                        icon     = Icons.Default.Star,
                        iconTint = SunriseAmber,
                        title    = "Rate the App",
                        value    = "5 ★",
                        onClick  = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${AppConfig.PACKAGE_NAME}"))
                                )
                            } catch (e: Exception) {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PLAY_STORE_URL))
                                )
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsClickRow(
                        icon     = Icons.Default.Share,
                        iconTint = SunriseOrange,
                        title    = "Invite Friends 💌",
                        value    = "Share a beautiful card",
                        onClick  = {
                            // Shares a branded visual invite card instead of plain text —
                            // images get 3–5× more clicks in chats than plain links.
                            com.offline.wallcorepro.promotion.ShareCardGenerator
                                .shareInviteCard(context)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsClickRow(
                        icon     = Icons.Default.Email,
                        iconTint = MoonPurple,
                        title    = "Send Feedback",
                        value    = "",
                        onClick  = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${AppConfig.FEEDBACK_EMAIL}")
                                putExtra(Intent.EXTRA_SUBJECT, "Feedback — ${AppConfig.APP_NAME} v${AppConfig.VERSION_NAME}")
                            }
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── About ─────────────────────────────────────────────
                SettingGroupCard(label = "ℹ️ About") {
                    SettingsInfoRow(icon = Icons.Default.Apps,         title = "App",       value = AppConfig.APP_NAME)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsInfoRow(icon = Icons.Default.Tag,          title = "Version",   value = "v${AppConfig.VERSION_NAME}")
                }

                Spacer(Modifier.height(12.dp))

                // ── Legal ─────────────────────────────────────────────
                SettingGroupCard(label = "⚖️ Legal") {
                    SettingsClickRow(
                        icon     = Icons.Default.PrivacyTip,
                        iconTint = SunriseOrange,
                        title    = "Privacy Policy",
                        value    = "View →",
                        onClick  = onPrivacyPolicyClick
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsClickRow(
                        icon     = Icons.Default.Gavel,
                        iconTint = MoonPurple,
                        title    = "Terms of Service",
                        value    = "View →",
                        onClick  = onTermsOfServiceClick
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // ── Change Interval Dialog ─────────────────────────────────────────────
    if (showIntervalPicker) {
        AlertDialog(
            onDismissRequest = { showIntervalPicker = false },
            title = { Text("Change Interval", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AutoWallpaperInterval.values().forEach { interval ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAutoWallpaperInterval(interval.hours, context)
                                    showIntervalPicker = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.autoWallpaperIntervalHours == interval.hours,
                                onClick  = {
                                    viewModel.setAutoWallpaperInterval(interval.hours, context)
                                    showIntervalPicker = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(interval.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showIntervalPicker = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Apply To Dialog ────────────────────────────────────────────────────
    if (showTargetPicker) {
        val targets = listOf(
            WallpaperTarget.HOME  to "🏠 Home Screen",
            WallpaperTarget.LOCK  to "🔒 Lock Screen",
            WallpaperTarget.BOTH  to "✨ Both Screens"
        )
        AlertDialog(
            onDismissRequest = { showTargetPicker = false },
            title = { Text("Apply Wallpaper To", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    targets.forEach { (target, label) ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAutoWallpaperTarget(target.name)
                                    showTargetPicker = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.autoWallpaperTarget.equals(target.name, ignoreCase = true),
                                onClick  = {
                                    viewModel.setAutoWallpaperTarget(target.name)
                                    showTargetPicker = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showTargetPicker = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Quote Category Picker Dialog ───────────────────────────────────────
    if (showCategoryPicker) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = {
                Column {
                    Text("Quote Categories", fontWeight = FontWeight.Bold)
                    Text(
                        "Choose the types of quotes shown on wallpapers and in WishMagic AI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AppConfig.QuoteCategory.entries.forEach { cat ->
                        val isSelected = cat.key in uiState.selectedQuoteCategories
                        val isLastOne  = isSelected && uiState.selectedQuoteCategories.size == 1
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .clickable(enabled = !isLastOne) {
                                    viewModel.toggleQuoteCategory(cat.key)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.emoji, fontSize = 22.sp, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    cat.displayName,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (isLastOne) {
                                    Text(
                                        "At least one must stay active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Checkbox(
                                checked         = isSelected,
                                onCheckedChange = if (isLastOne) null else { _ -> viewModel.toggleQuoteCategory(cat.key) },
                                colors          = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCategoryPicker = false },
                    shape   = RoundedCornerShape(12.dp)
                ) { Text("Done") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Your Name Dialog ───────────────────────────────────────────────────
    if (showNameDialog) {
        var nameInput by remember { mutableStateOf(uiState.userName) }
        val keyboard = LocalSoftwareKeyboardController.current

        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Your Name", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "We'll use your name for personalised wishes like:\n\"Good morning, ${nameInput.ifEmpty { "Alex" }}! ☀️\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 24) nameInput = it },
                        label = { Text("Name (optional)") },
                        placeholder = { Text("e.g. Alex") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboard?.hide()
                            viewModel.setUserName(nameInput)
                            showNameDialog = false
                        }),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserName(nameInput)
                        showNameDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ─── App Hero Card ────────────────────────────────────────────────────────────

@Composable
private fun AppHeroCard(userName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF1A0A00), Color(0xFF3D1B00), Color(0xFF2D1B69)))
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier         = Modifier
                    .size(64.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF6F00))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) { Text("🌅", fontSize = 32.sp) }
            Spacer(Modifier.height(12.dp))
            if (userName.isNotBlank()) {
                Text(
                    text  = "Hello, $userName! 👋",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F)
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text       = AppConfig.APP_NAME,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Text(
                text  = "Beautiful wallpapers for every moment",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(0.65f)
            )
        }
    }
}

// ─── Reusable Setting Components ──────────────────────────────────────────────

@Composable
private fun SettingGroupCard(label: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text       = label,
        style      = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier   = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier.size(38.dp).background(iconTint.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SettingsClickRow(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier.size(38.dp).background(iconTint.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
        if (value.isNotEmpty()) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Smart Reminder Banner ────────────────────────────────────────────────────

@Composable
private fun SmartReminderBanner(
    hours: Pair<Int, Int>,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    fun fmt(h: Int): String {
        val suffix = if (h < 12) "AM" else "PM"
        val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
        return "$h12:00 $suffix"
    }
    Card(
        shape     = RoundedCornerShape(18.dp),
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF0A2744), Color(0xFF1A103A)))
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Smart Reminder Suggestion",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("We noticed you open the app around:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFD54F).copy(alpha = 0.2f)) {
                        Text("☀️ Morning · ${fmt(hours.first)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF9C27B0).copy(alpha = 0.2f)) {
                        Text("🌙 Night · ${fmt(hours.second)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFCE93D8), fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApply,
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("✓ Apply Suggestions", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape   = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text("Dismiss", style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)) }
                }
            }
        }
    }
}

// ─── Promo App Row ────────────────────────────────────────────────────────────

@Composable
private fun PromoAppRow(app: AppConfig.PromoApp, onClick: () -> Unit) {
    val accentColor = Color(app.accentColor)
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon circle with accent gradient
        Box(
            modifier         = Modifier
                .size(46.dp)
                .background(
                    Brush.radialGradient(
                        listOf(accentColor.copy(alpha = 0.3f), accentColor.copy(alpha = 0.08f))
                    ),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(app.iconEmoji, fontSize = 24.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.name,
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.width(6.dp))
                // Badge chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        app.badgeText,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = accentColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Text(
                app.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // "GET" action button
        OutlinedButton(
            onClick      = onClick,
            shape        = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            border       = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = Brush.linearGradient(listOf(accentColor, accentColor))
            )
        ) {
            Text(
                "GET",
                fontWeight = FontWeight.Bold,
                color      = accentColor,
                fontSize   = 13.sp
            )
        }
    }
}
