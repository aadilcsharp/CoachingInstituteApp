package com.example.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.SubscriptionTier
import com.example.ui.components.AppTopBar
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.BillViewModel
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: BillViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val licenseInfo by viewModel.licenseInfo.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { AppTopBar(title = "Settings & Preferences") },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subscription & Offline License Banner Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.SubscriptionLicense) }
                        .testTag("settings_subscription_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (licenseInfo.isExpiringSoon) Color(0xFFFEF3C7)
                        else if (licenseInfo.isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Subscription: ${licenseInfo.tier.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (licenseInfo.isExpiringSoon) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (licenseInfo.tier) {
                                        SubscriptionTier.ENTERPRISE -> Color(0xFF4338CA)
                                        SubscriptionTier.PREMIUM -> Color(0xFF047857)
                                        SubscriptionTier.BASIC -> Color(0xFF1D4ED8)
                                        SubscriptionTier.FREE -> Color(0xFF6B7280)
                                    }
                                ) {
                                    Text(
                                        text = licenseInfo.tier.badgeName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = licenseInfo.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (licenseInfo.isExpiringSoon) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Manage License",
                            tint = if (licenseInfo.isExpiringSoon) Color(0xFF92400E) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Business Profile Preview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Screen.BusinessProfile) }
                        .testTag("settings_business_profile_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.businessName.ifEmpty { "Configure Business Profile" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Currency: ${profile.currencySymbol} • Tax ID: ${profile.taxId.ifEmpty { "Not set" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Quick Currency Selection Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CurrencyRupee,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Default Currency",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Current: ${profile.currencySymbol} (${profile.currencyCode}). Default is set to Indian Rupee (INR ₹).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val quickCurrencies = listOf(
                            Triple("₹", "INR", "₹ INR (Default)"),
                            Triple("$", "USD", "$ USD"),
                            Triple("€", "EUR", "€ EUR"),
                            Triple("£", "GBP", "£ GBP"),
                            Triple("AED", "AED", "AED"),
                            Triple("¥", "JPY", "¥ JPY")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickCurrencies.take(3).forEach { (symbol, code, label) ->
                                val isSelected = profile.currencySymbol == symbol && profile.currencyCode == code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateProfile(
                                            profile.copy(
                                                currencySymbol = symbol,
                                                currencyCode = code
                                            )
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier.testTag("currency_chip_$code")
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickCurrencies.drop(3).forEach { (symbol, code, label) ->
                                val isSelected = profile.currencySymbol == symbol && profile.currencyCode == code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateProfile(
                                            profile.copy(
                                                currencySymbol = symbol,
                                                currencyCode = code
                                            )
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier.testTag("currency_chip_$code")
                                )
                            }
                        }
                    }
                }
            }

            // General Preferences Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        SettingsRow(
                            title = "Business Profile",
                            subtitle = "Update company name, logo, address, contact, and tax info",
                            icon = Icons.Default.Business,
                            onClick = { onNavigate(Screen.BusinessProfile) },
                            testTag = "settings_row_profile"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "Manage Items & Catalog",
                            subtitle = "Add reusable products & services with custom prices and photos",
                            icon = Icons.Default.ShoppingCart,
                            onClick = { onNavigate(Screen.ManageItems) },
                            testTag = "settings_row_manage_items"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "Client Directory",
                            subtitle = "View and manage clients with contact info & billing addresses",
                            icon = Icons.Default.Person,
                            onClick = { onNavigate(Screen.ManageClients) },
                            testTag = "settings_row_manage_clients"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "WhatsApp Broadcast",
                            subtitle = "Share announcements or reminders to selected or all clients",
                            icon = Icons.Outlined.Campaign,
                            onClick = { onNavigate(Screen.WhatsAppBroadcast) },
                            testTag = "settings_row_broadcast"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "Bill Templates Gallery",
                            subtitle = "Browse, preview, and customize PDF templates",
                            icon = Icons.Default.Palette,
                            onClick = { onNavigate(Screen.TemplateManager) },
                            testTag = "settings_row_templates"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "Backup & Restore",
                            subtitle = "Export or restore your local SQLite database as JSON",
                            icon = Icons.Default.Restore,
                            onClick = { onNavigate(Screen.BackupRestore) },
                            testTag = "settings_row_backup"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "Subscription & Offline License",
                            subtitle = "Manage offline SHA-256 license key, view device ID & tiers",
                            icon = Icons.Default.Key,
                            onClick = { onNavigate(Screen.SubscriptionLicense) },
                            testTag = "settings_row_license"
                        )
                    }
                }
            }

            // Testing / Tools Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        SettingsRow(
                            title = "Load Demo Bills",
                            subtitle = "Populate realistic sample bills for testing templates and charts",
                            icon = Icons.Outlined.AutoFixHigh,
                            onClick = { viewModel.seedSampleData() },
                            testTag = "settings_row_demo"
                        )
                    }
                }
            }

            // About Application Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "About BillCraft",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "BillCraft v1.0\nModern mobile billing and expense management application with offline SQLite persistence, native PDF generation, WhatsApp sharing, and interactive spending charts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
