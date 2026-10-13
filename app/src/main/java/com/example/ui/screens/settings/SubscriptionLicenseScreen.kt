package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SubscriptionTier
import com.example.ui.components.AppTopBar
import com.example.ui.viewmodel.BillViewModel
import com.example.util.DeviceUtils
import com.example.util.LicenseManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun SubscriptionLicenseScreen(
    viewModel: BillViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val licenseInfo by viewModel.licenseInfo.collectAsStateWithLifecycle()

    val macAddress = remember { DeviceUtils.getMacAddress(context) }
    val deviceId = remember { DeviceUtils.getDeviceId(context) }
    val appVersion = remember { DeviceUtils.getAppVersion(context) }

    // Form inputs for manual key entry
    var selectedPlan by remember { mutableStateOf("PREMIUM") }
    var endDateInput by remember {
        val cal = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        mutableStateOf(sdf.format(cal.time))
    }
    var keyInput by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var formSuccess by remember { mutableStateOf<String?>(null) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            keyInput = text.trim()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Subscription & License",
                onBack = onBack
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current License Status Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (licenseInfo.isActive) {
                            if (licenseInfo.isExpiringSoon) Color(0xFFFEF3C7) // Warning Amber
                            else MaterialTheme.colorScheme.primaryContainer
                        } else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("license_status_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (licenseInfo.isActive) {
                                                if (licenseInfo.isExpiringSoon) Color(0xFFF59E0B)
                                                else MaterialTheme.colorScheme.primary
                                            } else Color.Gray
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (licenseInfo.isActive) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = licenseInfo.tier.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (licenseInfo.isActive) "Active License" else "No Active License",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Plan Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (licenseInfo.tier) {
                                    SubscriptionTier.ENTERPRISE -> Color(0xFF4338CA)
                                    SubscriptionTier.PREMIUM -> Color(0xFF047857)
                                    SubscriptionTier.BASIC -> Color(0xFF1D4ED8)
                                    SubscriptionTier.FREE -> Color(0xFF6B7280)
                                }
                            ) {
                                Text(
                                    text = licenseInfo.tier.badgeName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = licenseInfo.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (licenseInfo.isExpiringSoon) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                        )

                        if (licenseInfo.isActive) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Valid Until: ${licenseInfo.endDateStr} (${licenseInfo.daysRemaining} days remaining)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                TextButton(
                                    onClick = { viewModel.deactivateLicense() },
                                    modifier = Modifier.testTag("btn_deactivate_license")
                                ) {
                                    Text("Remove Key", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Expiry Alert Banner (if <= 5 days remaining)
            if (licenseInfo.isExpiringSoon) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24)),
                        modifier = Modifier.fillMaxWidth().testTag("expiry_warning_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Subscription Expiring Soon!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Only ${licenseInfo.daysRemaining} day(s) left until ${licenseInfo.endDateStr}. Enter a new offline license key below to maintain access to ${licenseInfo.tier.title} features.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Device Parameters Card (Needed for generating the SHA-256 Key offline)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Device License Parameters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Your offline SHA-256 key is uniquely generated using this Phone ID and App Version.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Phone MAC Address
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Phone MAC Address", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = macAddress,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { copyToClipboard("MAC Address", macAddress) },
                                    modifier = Modifier.size(36.dp).testTag("btn_copy_mac_address")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy MAC Address", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Device / Android ID
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Device / Hardware ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = deviceId,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { copyToClipboard("Device ID", deviceId) },
                                    modifier = Modifier.size(36.dp).testTag("btn_copy_device_id")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Device ID", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("App Version", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = appVersion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Algorithm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "SHA-256 (Offline)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Manual Key Activation Form
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Enter License Key",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Select your subscription tier, expiration date, and paste your 64-character SHA-256 license hash.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Plan Selector Chips
                        Column {
                            Text("1. Subscription Tier:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("BASIC", "PREMIUM", "ENTERPRISE").forEach { plan ->
                                    FilterChip(
                                        selected = selectedPlan == plan,
                                        onClick = { selectedPlan = plan },
                                        label = { Text(plan, fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f).testTag("chip_plan_$plan")
                                    )
                                }
                            }
                        }

                        // End Date Input & Quick Presets
                        Column {
                            Text("2. Expiration Date (YYYY-MM-DD):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = endDateInput,
                                onValueChange = {
                                    endDateInput = it.trim()
                                    formError = null
                                },
                                singleLine = true,
                                placeholder = { Text("YYYY-MM-DD") },
                                modifier = Modifier.fillMaxWidth().testTag("input_end_date")
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "1 Month" to 1,
                                    "6 Months" to 6,
                                    "1 Year" to 12
                                ).forEach { (label, months) ->
                                    OutlinedButton(
                                        onClick = {
                                            val cal = Calendar.getInstance().apply { add(Calendar.MONTH, months) }
                                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                            endDateInput = sdf.format(cal.time)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 4.dp)
                                    ) {
                                        Text(label, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // SHA-256 Key Input
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("3. SHA-256 License Hash (64 Hex):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { pasteFromClipboard() }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Paste", fontSize = 11.sp)
                                }
                            }

                            OutlinedTextField(
                                value = keyInput,
                                onValueChange = {
                                    keyInput = it.trim()
                                    formError = null
                                    formSuccess = null
                                },
                                placeholder = { Text("e.g. e3b0c44298fc1c149afbf4c8996fb92427ae...") },
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth().testTag("input_license_key")
                            )
                        }

                        // Error / Success message
                        if (formError != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = formError ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (formSuccess != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFD1FAE5)
                            ) {
                                Text(
                                    text = formSuccess ?: "",
                                    color = Color(0xFF065F46),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Activate Button
                        Button(
                            onClick = {
                                formError = null
                                formSuccess = null
                                val result = viewModel.activateLicense(
                                    inputKey = keyInput,
                                    plan = selectedPlan,
                                    endDate = endDateInput
                                )
                                if (result.isSuccess) {
                                    formSuccess = "✓ License activated successfully for $selectedPlan!"
                                    keyInput = ""
                                } else {
                                    formError = result.exceptionOrNull()?.localizedMessage ?: "Invalid license key."
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_activate_license")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Activate License")
                        }
                    }
                }
            }

            // 4. Offline Key Formula & Generator Helper (for offline generation & testing)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Offline Key Generator & Tester",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Formula: SHA256(MAC_ADDRESS + END_DATE + PLAN + APP_VERSION)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val macDirectString = remember(macAddress, endDateInput, selectedPlan, appVersion) {
                            "$macAddress$endDateInput$selectedPlan$appVersion"
                        }
                        val macColonString = remember(macAddress, endDateInput, selectedPlan, appVersion) {
                            LicenseManager.getCanonicalKeyString(macAddress, endDateInput, selectedPlan, appVersion)
                        }

                        Text(
                            text = "Input string (Direct concatenation):\n$macDirectString\n\nOr colon-separated:\n$macColonString",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val generatedHash = LicenseManager.sha256(macDirectString)
                                    keyInput = generatedHash
                                    copyToClipboard("Generated SHA-256 Key", generatedHash)
                                    Toast.makeText(context, "Key generated using MAC & pasted!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).testTag("btn_auto_fill_mac_key")
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Key from MAC", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val generatedHash = LicenseManager.generateCanonicalSha256(deviceId, endDateInput, selectedPlan, appVersion)
                                    keyInput = generatedHash
                                    copyToClipboard("Generated SHA-256 Key", generatedHash)
                                    Toast.makeText(context, "Key generated using Device ID & pasted!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).testTag("btn_auto_fill_test_key")
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Key from Device ID", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 5. Subscription Plan Feature Comparison Table
            item {
                Text(
                    text = "Subscription Tier Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            listOf(
                SubscriptionTier.FREE,
                SubscriptionTier.BASIC,
                SubscriptionTier.PREMIUM,
                SubscriptionTier.ENTERPRISE
            ).forEach { tier ->
                item {
                    val isCurrent = licenseInfo.tier == tier
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().testTag("tier_card_${tier.name}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = tier.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "CURRENT PLAN",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (tier) {
                                        SubscriptionTier.ENTERPRISE -> Color(0xFF4338CA)
                                        SubscriptionTier.PREMIUM -> Color(0xFF047857)
                                        SubscriptionTier.BASIC -> Color(0xFF1D4ED8)
                                        SubscriptionTier.FREE -> Color(0xFF6B7280)
                                    }
                                ) {
                                    Text(
                                        text = tier.badgeName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = tier.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // Features List
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TierFeatureRow("Client Directory Limit", if (tier.maxClients == Int.MAX_VALUE) "Unlimited" else "Up to ${tier.maxClients} clients")
                                TierFeatureRow("Catalog Items Limit", if (tier.maxCatalogItems == Int.MAX_VALUE) "Unlimited" else "Up to ${tier.maxCatalogItems} items")
                                TierFeatureRow("Business Logo on Bills", if (tier.isLogoAllowed) "Enabled" else "No Logo")
                                TierFeatureRow("Invoice Templates", "${tier.maxTemplates} Template(s)")
                                TierFeatureRow("WhatsApp Bulk Broadcast", if (tier.isBroadcastAllowed) "Enabled" else "Disabled")
                                TierFeatureRow("Product QR Sticker Printing", if (tier.isProductQrStickerAllowed) "Enabled" else "Basic")
                                TierFeatureRow("Database Backup & Restore", if (tier.isBackupRestoreAllowed) "Enabled" else "Disabled")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TierFeatureRow(feature: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (value.contains("Disabled") || value.contains("No Logo")) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )
    }
}
