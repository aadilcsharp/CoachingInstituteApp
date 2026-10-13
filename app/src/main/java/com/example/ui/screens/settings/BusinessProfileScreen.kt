package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.BusinessProfileEntity
import com.example.ui.components.AppTopBar
import com.example.ui.viewmodel.BillViewModel
import com.example.util.ImageOptimizer
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.launch
import java.io.File

data class CurrencyPreset(
    val symbol: String,
    val code: String,
    val displayName: String
)

val CURRENCY_PRESETS = listOf(
    CurrencyPreset("₹", "INR", "₹ INR (Indian Rupee)"),
    CurrencyPreset("$", "USD", "$ USD (US Dollar)"),
    CurrencyPreset("€", "EUR", "€ EUR (Euro)"),
    CurrencyPreset("£", "GBP", "£ GBP (British Pound)"),
    CurrencyPreset("AED", "AED", "AED (UAE Dirham)"),
    CurrencyPreset("¥", "JPY", "¥ JPY (Japanese Yen)"),
    CurrencyPreset("A$", "AUD", "A$ AUD (Australian Dollar)"),
    CurrencyPreset("C$", "CAD", "C$ CAD (Canadian Dollar)"),
    CurrencyPreset("SAR", "SAR", "SAR (Saudi Riyal)"),
    CurrencyPreset("S$", "SGD", "S$ SGD (Singapore Dollar)")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusinessProfileScreen(
    viewModel: BillViewModel,
    onBack: () -> Unit,
    onNavigateToLicense: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val licenseInfo by viewModel.licenseInfo.collectAsStateWithLifecycle()

    var businessName by remember(profile) { mutableStateOf(profile.businessName) }
    var ownerName by remember(profile) { mutableStateOf(profile.ownerName) }
    var email by remember(profile) { mutableStateOf(profile.email) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var address by remember(profile) { mutableStateOf(profile.address) }
    var taxId by remember(profile) { mutableStateOf(profile.taxId) }
    var currencySymbol by remember(profile) { mutableStateOf(profile.currencySymbol) }
    var currencyCode by remember(profile) { mutableStateOf(profile.currencyCode) }
    var paymentDetails by remember(profile) { mutableStateOf(profile.paymentDetails) }
    var defaultTaxPercent by remember(profile) { mutableDoubleStateOf(profile.defaultTaxPercent) }
    var defaultNotes by remember(profile) { mutableStateOf(profile.defaultNotes) }
    var defaultTerms by remember(profile) { mutableStateOf(profile.defaultTerms) }
    var logoPath by remember(profile) { mutableStateOf(profile.logoPath) }
    var showLogoOnBill by remember(profile) { mutableStateOf(profile.showLogoOnBill) }
    var selectedCategories by remember(profile) { mutableStateOf(profile.selectedCategories) }
    var paymentQrText by remember(profile) { mutableStateOf(profile.paymentQrText) }
    var showPaymentQrOnBill by remember(profile) { mutableStateOf(profile.showPaymentQrOnBill) }

    var isOptimizingLogo by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isOptimizingLogo = true
                val path = ImageOptimizer.optimizeAndSaveLogoImage(context, uri)
                if (path != null) {
                    logoPath = path
                }
                isOptimizingLogo = false
            }
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Business Profile",
                onBack = onBack,
                actions = {
                    Button(
                        onClick = {
                            val updated = profile.copy(
                                businessName = businessName.trim(),
                                ownerName = ownerName.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                taxId = taxId.trim(),
                                currencySymbol = currencySymbol.trim().ifEmpty { "₹" },
                                currencyCode = currencyCode.trim().ifEmpty { "INR" },
                                paymentDetails = paymentDetails.trim(),
                                defaultTaxPercent = defaultTaxPercent,
                                defaultNotes = defaultNotes.trim(),
                                defaultTerms = defaultTerms.trim(),
                                logoPath = logoPath,
                                showLogoOnBill = showLogoOnBill,
                                selectedCategories = selectedCategories.trim().ifEmpty { "Services,Products" },
                                paymentQrText = paymentQrText.trim(),
                                showPaymentQrOnBill = showPaymentQrOnBill
                            )
                            viewModel.updateProfile(updated)
                            onBack()
                        },
                        modifier = Modifier.padding(end = 8.dp).testTag("save_profile_button")
                    ) {
                        Text("Save")
                    }
                }
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
            // Currency & Taxes Card (Prominently at the top of configuration)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Currency & Billing Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Choose Currency (INR is default):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Currencies Filter Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CURRENCY_PRESETS) { preset ->
                                val isSelected = currencySymbol == preset.symbol && currencyCode == preset.code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        currencySymbol = preset.symbol
                                        currencyCode = preset.code
                                    },
                                    label = {
                                        Text(
                                            preset.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("currency_chip_${preset.code}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = currencySymbol,
                                onValueChange = { currencySymbol = it },
                                label = { Text("Currency Symbol") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("profile_currency_input")
                            )

                            OutlinedTextField(
                                value = currencyCode,
                                onValueChange = { currencyCode = it },
                                label = { Text("Code (e.g. INR)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("profile_currency_code_input")
                            )
                        }

                        // Default Tax Rate with Indian GST Presets (0%, 5%, 12%, 18%, 28%)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Default Tax / GST Rate (%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf(0.0, 5.0, 12.0, 18.0, 28.0)) { rate ->
                                    FilterChip(
                                        selected = defaultTaxPercent == rate,
                                        onClick = { defaultTaxPercent = rate },
                                        label = { Text("${rate.toInt()}% GST") }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = if (defaultTaxPercent == 0.0) "" else defaultTaxPercent.toString(),
                                onValueChange = { defaultTaxPercent = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text("Custom Tax %") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().testTag("profile_tax_percent_input")
                            )
                        }
                    }
                }
            }

            // Product & Service Categories Selection Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Product & Service Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select which categories appear on bill creation. If you select only one (e.g. Products), only that category will be available on the create bill page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val standardCategories = listOf(
                            "Services", "Products", "Consulting", "Utilities", "Maintenance", "Office", "Freelance", "Other"
                        )
                        val selectedSet = selectedCategories.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toSet()

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            standardCategories.forEach { cat ->
                                val isSelected = selectedSet.contains(cat)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newSet = if (isSelected) {
                                            if (selectedSet.size > 1) selectedSet - cat else selectedSet
                                        } else {
                                            selectedSet + cat
                                        }
                                        selectedCategories = newSet.joinToString(",")
                                    },
                                    label = { Text(cat) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.testTag("category_chip_${cat.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = { selectedCategories = "Products" }) {
                                Text("Only Products", fontSize = 12.sp)
                            }
                            TextButton(onClick = { selectedCategories = "Services" }) {
                                Text("Only Services", fontSize = 12.sp)
                            }
                            TextButton(onClick = { selectedCategories = standardCategories.joinToString(",") }) {
                                Text("Select All", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Company Logo & Branding Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Company Logo & Branding",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!licenseInfo.tier.isLogoAllowed) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "Requires BASIC Plan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (!licenseInfo.tier.isLogoAllowed) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "🔒 Logo feature is locked on the ${licenseInfo.tier.title}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Upgrade to the BASIC plan (or higher) to display your business logo on printed bills and invoices.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (onNavigateToLicense != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = onNavigateToLicense,
                                            modifier = Modifier.testTag("btn_upgrade_for_logo")
                                        ) {
                                            Text("Upgrade License", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Upload your business logo to display on bills and invoices. You can toggle whether to show it on bills below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (!logoPath.isNullOrBlank() && File(logoPath!!).exists()) {
                                    AsyncImage(
                                        model = File(logoPath!!),
                                        contentDescription = "Business Logo",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(4.dp)
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                imagePicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier.testTag("change_logo_button")
                                        ) {
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Change Logo")
                                        }

                                        OutlinedButton(
                                            onClick = { logoPath = null },
                                            modifier = Modifier.testTag("remove_logo_button")
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Remove", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            imagePicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.testTag("upload_logo_button")
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Upload Business Logo")
                                    }
                                }
                            }

                            if (isOptimizingLogo) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Optimizing logo...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Checkbox whether to show logo on bill or not
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showLogoOnBill = !showLogoOnBill }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = showLogoOnBill,
                                    onCheckedChange = { showLogoOnBill = it },
                                    modifier = Modifier.testTag("checkbox_show_logo_on_bill")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Show logo on bills and invoices",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (logoPath.isNullOrBlank()) {
                                            "When checked, your logo will appear on bills as soon as one is uploaded."
                                        } else if (showLogoOnBill) {
                                            "Logo will be displayed on all printed bills and PDF downloads."
                                        } else {
                                            "Logo is stored, but will not be shown on printed bills and PDFs."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Business Identity Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Business Identity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Company / Business Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("profile_business_name_input")
                        )

                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Proprietor / Representative Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("profile_owner_name_input")
                        )

                        OutlinedTextField(
                            value = taxId,
                            onValueChange = { taxId = it },
                            label = { Text("GSTIN / Tax ID / PAN") },
                            placeholder = { Text("e.g. 29AAAAA0000A1Z5") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("profile_tax_id_input")
                        )
                    }
                }
            }

            // Contact Information Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Contact Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Billing Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth().testTag("profile_email_input")
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Billing Phone") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().testTag("profile_phone_input")
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Business Address (Street, City, State, PIN)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("profile_address_input")
                        )
                    }
                }
            }

            // Payment Instructions Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Payment Instructions & Bank / UPI Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = paymentDetails,
                            onValueChange = { paymentDetails = it },
                            label = { Text("Bank / UPI Details (Printed on invoices)") },
                            placeholder = { Text("Bank Name\nA/C No:\nIFSC Code:\nUPI ID: yourname@upi") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("profile_bank_details_input")
                        )
                    }
                }
            }

            // Payment QR Code Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Payment QR Code (UPI / Scan to Pay)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter your UPI ID, payment link, or payment text. It will be converted into a scannable QR code and placed on your bills and PDF invoices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = paymentQrText,
                            onValueChange = { paymentQrText = it },
                            label = { Text("UPI ID or Payment QR Text") },
                            placeholder = { Text("e.g. business@upi or upi://pay?pa=...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("profile_qr_text_input")
                        )

                        // Live preview if text is entered
                        if (paymentQrText.isNotBlank()) {
                            val previewBitmap = remember(paymentQrText) {
                                QrCodeGenerator.generateQrBitmap(paymentQrText, 250)
                            }
                            if (previewBitmap != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Image(
                                        bitmap = previewBitmap.asImageBitmap(),
                                        contentDescription = "Live QR Code Preview",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                            .padding(4.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Live QR Preview",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Clients scanning this code will be directed to: $paymentQrText",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPaymentQrOnBill = !showPaymentQrOnBill }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = showPaymentQrOnBill,
                                onCheckedChange = { showPaymentQrOnBill = it },
                                modifier = Modifier.testTag("checkbox_show_qr_on_bill")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Show payment QR code on bills and invoices",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (showPaymentQrOnBill) {
                                        "QR code will be placed on bills and PDFs for instant scanning."
                                    } else {
                                        "QR code is configured, but will not be printed on bills."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Default Notes and Terms
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Default Invoice Notes & Terms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = defaultNotes,
                            onValueChange = { defaultNotes = it },
                            label = { Text("Default Notes") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("profile_default_notes_input")
                        )

                        OutlinedTextField(
                            value = defaultTerms,
                            onValueChange = { defaultTerms = it },
                            label = { Text("Default Terms & Conditions") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("profile_default_terms_input")
                        )
                    }
                }
            }

            // Save Profile Button
            item {
                Button(
                    onClick = {
                        val updated = profile.copy(
                            businessName = businessName.trim(),
                            ownerName = ownerName.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            taxId = taxId.trim(),
                            currencySymbol = currencySymbol.trim().ifEmpty { "₹" },
                            currencyCode = currencyCode.trim().ifEmpty { "INR" },
                            paymentDetails = paymentDetails.trim(),
                            defaultTaxPercent = defaultTaxPercent,
                            defaultNotes = defaultNotes.trim(),
                            defaultTerms = defaultTerms.trim(),
                            logoPath = logoPath,
                            showLogoOnBill = showLogoOnBill,
                            selectedCategories = selectedCategories.trim().ifEmpty { "Services,Products" },
                            paymentQrText = paymentQrText.trim(),
                            showPaymentQrOnBill = showPaymentQrOnBill
                        )
                        viewModel.updateProfile(updated)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("bottom_save_profile_button")
                ) {
                    Text("Save Business Profile", fontSize = 16.sp)
                }
            }
        }
    }
}
