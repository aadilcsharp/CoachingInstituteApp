package com.example.ui.screens.bills

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ui.components.QrScannerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.BillCategory
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BillTemplateType
import com.example.data.model.CatalogItemEntity
import com.example.data.model.ClientEntity
import com.example.data.model.PaymentStatus
import com.example.ui.components.AppTopBar
import com.example.ui.components.BillItemRow
import com.example.ui.components.ConfirmationDialog
import com.example.ui.viewmodel.BillViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.ImageOptimizer
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditBillScreen(
    billId: Long?,
    viewModel: BillViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val licenseInfo by viewModel.licenseInfo.collectAsStateWithLifecycle()
    val allBills by viewModel.allBills.collectAsStateWithLifecycle()
    val catalogItems by viewModel.allCatalogItems.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val currencySymbol = profile.currencySymbol

    // Form fields
    var billNumber by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientAddress by remember { mutableStateOf("") }
    var billDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    val allowedCategories = remember(profile.selectedCategories) { profile.getCategoriesList() }
    var categoryName by remember(allowedCategories) { mutableStateOf(allowedCategories.firstOrNull() ?: "Products") }
    var templateId by remember { mutableStateOf(profile.defaultTemplateId) }
    var taxPercent by remember { mutableDoubleStateOf(profile.defaultTaxPercent) }
    var discountPercent by remember { mutableDoubleStateOf(0.0) }
    var paidAmount by remember { mutableDoubleStateOf(0.0) }
    var paymentMethod by remember { mutableStateOf("Bank Transfer") }
    var notes by remember { mutableStateOf(profile.defaultNotes) }
    var terms by remember { mutableStateOf(profile.defaultTerms) }

    val items = remember { mutableStateListOf<BillItemEntity>() }

    // Dialog & Dropdown states
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEditIndex by remember { mutableStateOf<Int?>(null) }
    var dialogInitialTab by remember { mutableIntStateOf(0) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Client dropdown & add dialog state
    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var showAddClientDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    // Initialize or load bill
    LaunchedEffect(billId) {
        if (billId != null && billId > 0) {
            val existing = allBills.firstOrNull { it.bill.id == billId }
            if (existing != null) {
                val b = existing.bill
                billNumber = b.billNumber
                title = b.title
                clientName = b.clientName
                clientEmail = b.clientEmail ?: ""
                clientPhone = b.clientPhone ?: ""
                clientAddress = b.clientAddress ?: ""
                billDate = b.billDate
                dueDate = b.dueDate
                categoryName = b.category
                templateId = b.templateId
                taxPercent = b.taxPercent
                discountPercent = b.discountPercent
                paidAmount = b.paidAmount
                paymentMethod = b.paymentMethod ?: "Bank Transfer"
                notes = b.notes ?: ""
                terms = b.terms ?: ""

                items.clear()
                items.addAll(existing.items)
            }
        } else {
            viewModel.generateNextBillNumber { nextNum ->
                billNumber = nextNum
            }
            // Auto-populate item if arriving from QR scan
            viewModel.pendingItemForNewBill?.let { pending ->
                if (items.none { it.name.equals(pending.name, ignoreCase = true) }) {
                    items.add(
                        BillItemEntity(
                            billId = 0L,
                            name = pending.name,
                            description = pending.description,
                            quantity = 1.0,
                            unitPrice = pending.defaultUnitPrice,
                            unit = pending.unit,
                            amount = pending.defaultUnitPrice,
                            imagePath = pending.imagePath
                        )
                    )
                }
                viewModel.pendingItemForNewBill = null
            }
        }
    }

    // Calculations
    val subtotal = items.sumOf { it.amount }
    val discountAmount = (subtotal * (discountPercent / 100.0)).coerceAtLeast(0.0)
    val afterDiscount = (subtotal - discountAmount).coerceAtLeast(0.0)
    val taxAmount = (afterDiscount * (taxPercent / 100.0)).coerceAtLeast(0.0)
    val totalAmount = (afterDiscount + taxAmount).coerceAtLeast(0.0)

    val paymentStatus = when {
        paidAmount >= totalAmount && totalAmount > 0.0 -> PaymentStatus.PAID
        paidAmount > 0.0 -> PaymentStatus.PARTIAL
        else -> PaymentStatus.UNPAID
    }

    BackHandler {
        showDiscardDialog = true
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (billId == null || billId <= 0) "Create Bill" else "Edit Bill",
                onBack = { showDiscardDialog = true },
                actions = {
                    Button(
                        onClick = {
                            if (clientName.isBlank()) {
                                errorMessage = "Please enter client or company name."
                                return@Button
                            }
                            if (items.isEmpty()) {
                                errorMessage = "Please add at least one line item."
                                return@Button
                            }

                            val billEntity = BillEntity(
                                id = billId ?: 0L,
                                billNumber = billNumber.trim(),
                                title = title.trim(),
                                billDate = billDate,
                                dueDate = dueDate,
                                clientName = clientName.trim(),
                                clientEmail = clientEmail.trim().ifEmpty { null },
                                clientPhone = clientPhone.trim().ifEmpty { null },
                                clientAddress = clientAddress.trim().ifEmpty { null },
                                subtotal = subtotal,
                                taxPercent = taxPercent,
                                taxAmount = taxAmount,
                                discountPercent = discountPercent,
                                discountAmount = discountAmount,
                                totalAmount = totalAmount,
                                paidAmount = paidAmount.coerceIn(0.0, totalAmount),
                                paymentStatus = paymentStatus.name,
                                category = categoryName,
                                templateId = templateId,
                                notes = notes.trim().ifEmpty { null },
                                terms = terms.trim().ifEmpty { null },
                                paymentMethod = paymentMethod.trim().ifEmpty { null },
                                createdAt = if (billId != null && billId > 0) 0L else System.currentTimeMillis()
                            )

                            viewModel.saveBill(billEntity, items.toList()) { savedId ->
                                onSaved(savedId)
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp).testTag("save_bill_button")
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
            // Validation Error Alert
            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { errorMessage = null }) {
                                Icon(Icons.Default.Delete, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // 1. Basic Information Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Bill Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = billNumber,
                                onValueChange = { billNumber = it },
                                label = { Text("Bill ID / Number *") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f).testTag("bill_number_input")
                            )

                            // Category Selector
                            var catExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = catExpanded,
                                onExpandedChange = { catExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = categoryName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                        .testTag("bill_category_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = catExpanded,
                                    onDismissRequest = { catExpanded = false }
                                ) {
                                    allowedCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                categoryName = cat
                                                catExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Subject / Purpose (Optional)") },
                            placeholder = { Text("e.g. Website Revamp, Consulting Retainer") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("bill_title_input")
                        )

                        // Date Pickers (Issue Date and Due Date)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Issue Date Picker
                            OutlinedTextField(
                                value = DateUtils.formatDate(billDate),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Bill Date") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = billDate }
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                cal.set(y, m, d)
                                                billDate = cal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("bill_date_picker")
                            )

                            // Due Date Picker
                            OutlinedTextField(
                                value = dueDate?.let { DateUtils.formatDate(it) } ?: "Not set",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Due Date") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = dueDate ?: (billDate + (14L * 86400000L))
                                        }
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                cal.set(y, m, d)
                                                dueDate = cal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick Due Date")
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("bill_due_date_picker")
                            )
                        }
                    }
                }
            }

            // 2. Client / Recipient Details Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Client / Recipient Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = { showAddClientDialog = true },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_new_client_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Client", fontSize = 12.sp)
                            }
                        }

                        // Client Name with Dropdown
                        ExposedDropdownMenuBox(
                            expanded = clientDropdownExpanded,
                            onExpandedChange = { clientDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = {
                                    clientName = it
                                    clientDropdownExpanded = true
                                },
                                label = { Text("Client / Business Name *") },
                                placeholder = { Text("Acme Corp or Jane Doe") },
                                singleLine = true,
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (clientName.isNotBlank()) {
                                            IconButton(onClick = {
                                                clientName = ""
                                                clientEmail = ""
                                                clientPhone = ""
                                                clientAddress = ""
                                            }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear client", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded)
                                    }
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                                    .fillMaxWidth()
                                    .testTag("client_name_input")
                            )

                            val matchingClients = remember(clients, clientName) {
                                if (clientName.isBlank()) {
                                    clients
                                } else {
                                    clients.filter {
                                        it.name.contains(clientName.trim(), ignoreCase = true) ||
                                        (it.phone?.contains(clientName.trim(), ignoreCase = true) == true) ||
                                        (it.email?.contains(clientName.trim(), ignoreCase = true) == true)
                                    }
                                }
                            }

                            ExposedDropdownMenu(
                                expanded = clientDropdownExpanded,
                                onDismissRequest = { clientDropdownExpanded = false }
                            ) {
                                if (matchingClients.isNotEmpty()) {
                                    matchingClients.take(8).forEach { client ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = client.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    val details = listOfNotNull(
                                                        client.phone?.takeIf { it.isNotBlank() },
                                                        client.email?.takeIf { it.isNotBlank() }
                                                    ).joinToString(" • ")
                                                    if (details.isNotBlank()) {
                                                        Text(
                                                            text = details,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            onClick = {
                                                clientName = client.name
                                                clientEmail = client.email ?: ""
                                                clientPhone = client.phone ?: ""
                                                clientAddress = client.address ?: ""
                                                clientDropdownExpanded = false
                                            }
                                        )
                                    }
                                    HorizontalDivider()
                                }

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "+ Add New Client to Directory",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        clientDropdownExpanded = false
                                        showAddClientDialog = true
                                    }
                                )
                            }
                        }

                        // Directory status indicator chip
                        val matchedClient = clients.firstOrNull { it.name.equals(clientName.trim(), ignoreCase = true) }
                        if (matchedClient != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Client linked from directory: ${matchedClient.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else if (clientName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "New client: will be automatically saved to directory when bill is saved",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = clientEmail,
                                onValueChange = { clientEmail = it },
                                label = { Text("Email") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.weight(1f).testTag("client_email_input")
                            )

                            OutlinedTextField(
                                value = clientPhone,
                                onValueChange = { clientPhone = it },
                                label = { Text("Phone") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f).testTag("client_phone_input")
                            )
                        }

                        OutlinedTextField(
                            value = clientAddress,
                            onValueChange = { clientAddress = it },
                            label = { Text("Billing Address") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("client_address_input")
                        )
                    }
                }
            }

            // 3. Line Items Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Line Items (${items.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showQrScanner = true },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("scan_product_qr_button")
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan QR", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        itemToEditIndex = null
                                        dialogInitialTab = 0
                                        showAddItemDialog = true
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("select_from_catalog_button")
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("From Catalog", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        itemToEditIndex = null
                                        dialogInitialTab = 1
                                        showAddItemDialog = true
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("add_item_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Item", fontSize = 12.sp)
                                }
                            }
                        }

                        if (items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No items added yet. Tap \"Add Item\" above to add billable goods or services.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items.forEachIndexed { index, item ->
                                BillItemRow(
                                    item = item,
                                    currencySymbol = currencySymbol,
                                    onEdit = {
                                        itemToEditIndex = index
                                        showAddItemDialog = true
                                    },
                                    onDelete = {
                                        items.removeAt(index)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Financial Calculations Card (Tax, Discounts, Totals, Payment)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Financial Summary & Taxes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Subtotal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                CurrencyFormatter.format(subtotal, currencySymbol),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Tax Input & Presets
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tax Rate (%)", style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = if (taxPercent == 0.0) "" else taxPercent.toString(),
                                    onValueChange = {
                                        taxPercent = it.toDoubleOrNull() ?: 0.0
                                    },
                                    placeholder = { Text("0") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(100.dp).testTag("tax_percent_input")
                                )
                            }
                            // Quick Tax / GST presets
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                items(listOf(0.0, 5.0, 12.0, 18.0, 28.0)) { rate ->
                                    FilterChip(
                                        selected = taxPercent == rate,
                                        onClick = { taxPercent = rate },
                                        label = { Text("${rate.toInt()}%") }
                                    )
                                }
                            }
                        }

                        // Discount Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Discount (%)", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = if (discountPercent == 0.0) "" else discountPercent.toString(),
                                onValueChange = {
                                    discountPercent = it.toDoubleOrNull() ?: 0.0
                                },
                                placeholder = { Text("0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(100.dp).testTag("discount_percent_input")
                            )
                        }

                        HorizontalDivider()

                        // Grand Total Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grand Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = CurrencyFormatter.format(totalAmount, currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Payment Status & Received Amount
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Amount Paid", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("Status: ${paymentStatus.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedTextField(
                                    value = if (paidAmount == 0.0) "" else paidAmount.toString(),
                                    onValueChange = {
                                        paidAmount = it.toDoubleOrNull() ?: 0.0
                                    },
                                    placeholder = { Text("0.00") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(130.dp).testTag("paid_amount_input")
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { paidAmount = totalAmount },
                                    modifier = Modifier.weight(1f).testTag("mark_fully_paid_btn")
                                ) {
                                    Text("Mark Fully Paid")
                                }
                                OutlinedButton(
                                    onClick = { paidAmount = 0.0 },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reset (Unpaid)")
                                }
                            }
                        }

                        // Payment Method
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = { paymentMethod = it },
                            label = { Text("Payment Method") },
                            placeholder = { Text("Bank Transfer, Cash, Card, UPI...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("payment_method_input")
                        )
                    }
                }
            }

            // 5. Bill Template Selector Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PDF Bill Template",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Choose which design template will be applied when rendering the PDF.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(BillTemplateType.entries) { index, tmpl ->
                                val isUnlocked = index < licenseInfo.tier.maxTemplates
                                val isSelected = templateId == tmpl.templateId
                                val requiredPlan = if (index == 1) "BASIC" else "PREMIUM"

                                Surface(
                                    onClick = {
                                        if (isUnlocked) {
                                            templateId = tmpl.templateId
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "${tmpl.displayName} requires the $requiredPlan plan (currently on ${licenseInfo.tier.title}).",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else if (!isUnlocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.width(140.dp).testTag("template_chip_${tmpl.templateId}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(Color(android.graphics.Color.parseColor(tmpl.primaryColorHex)), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            if (!isUnlocked) {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = "Locked",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = tmpl.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (!isUnlocked) "Requires $requiredPlan" else tmpl.description,
                                            fontSize = 10.sp,
                                            maxLines = 2,
                                            color = if (!isUnlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Notes & Terms Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Notes & Terms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Customer Notes (Printed on bill)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("bill_notes_input")
                        )

                        OutlinedTextField(
                            value = terms,
                            onValueChange = { terms = it },
                            label = { Text("Terms & Conditions") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("bill_terms_input")
                        )
                    }
                }
            }

            // Bottom Save Button
            item {
                Button(
                    onClick = {
                        if (clientName.isBlank()) {
                            errorMessage = "Please enter client or company name."
                            return@Button
                        }
                        if (items.isEmpty()) {
                            errorMessage = "Please add at least one line item."
                            return@Button
                        }

                        val billEntity = BillEntity(
                            id = billId ?: 0L,
                            billNumber = billNumber.trim(),
                            title = title.trim(),
                            billDate = billDate,
                            dueDate = dueDate,
                            clientName = clientName.trim(),
                            clientEmail = clientEmail.trim().ifEmpty { null },
                            clientPhone = clientPhone.trim().ifEmpty { null },
                            clientAddress = clientAddress.trim().ifEmpty { null },
                            subtotal = subtotal,
                            taxPercent = taxPercent,
                            taxAmount = taxAmount,
                            discountPercent = discountPercent,
                            discountAmount = discountAmount,
                            totalAmount = totalAmount,
                            paidAmount = paidAmount.coerceIn(0.0, totalAmount),
                            paymentStatus = paymentStatus.name,
                            category = categoryName,
                            templateId = templateId,
                            notes = notes.trim().ifEmpty { null },
                            terms = terms.trim().ifEmpty { null },
                            paymentMethod = paymentMethod.trim().ifEmpty { null },
                            createdAt = if (billId != null && billId > 0) 0L else System.currentTimeMillis()
                        )

                        viewModel.saveBill(billEntity, items.toList()) { savedId ->
                            onSaved(savedId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("bottom_save_bill_button")
                ) {
                    Text("Save Bill & Review", fontSize = 16.sp)
                }
            }
        }
    }

    // Add / Edit Line Item Dialog
    if (showAddItemDialog) {
        val isEditing = itemToEditIndex != null
        val existingItem = itemToEditIndex?.let { items.getOrNull(it) }

        var activeTab by remember { mutableIntStateOf(if (isEditing) 1 else dialogInitialTab) }
        var catalogSearch by remember { mutableStateOf("") }

        var itemName by remember { mutableStateOf(existingItem?.name ?: "") }
        var itemDesc by remember { mutableStateOf(existingItem?.description ?: "") }
        var itemQty by remember {
            mutableStateOf(
                existingItem?.quantity?.let {
                    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                } ?: "1"
            )
        }
        var itemPrice by remember {
            mutableStateOf(
                existingItem?.unitPrice?.let {
                    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                } ?: ""
            )
        }
        var itemUnit by remember { mutableStateOf(existingItem?.unit ?: "pcs") }
        var itemImagePath by remember { mutableStateOf<String?>(existingItem?.imagePath) }
        var showDialogScanner by remember { mutableStateOf(false) }
        var saveToCatalog by remember { mutableStateOf(!isEditing) }
        var isOptimizingImage by remember { mutableStateOf(false) }
        var itemError by remember { mutableStateOf<String?>(null) }

        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    isOptimizingImage = true
                    val path = ImageOptimizer.optimizeAndSaveImage(context, uri)
                    if (path != null) {
                        itemImagePath = path
                    }
                    isOptimizingImage = false
                }
            }
        }

        var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && cameraTempUri != null) {
                coroutineScope.launch {
                    isOptimizingImage = true
                    val path = ImageOptimizer.optimizeAndSaveImage(context, cameraTempUri!!)
                    if (path != null) {
                        itemImagePath = path
                    }
                    isOptimizingImage = false
                }
            }
        }

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                val tempFile = File(context.cacheDir, "camera_item_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                cameraTempUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Camera permission needed to take photo", Toast.LENGTH_SHORT).show()
            }
        }

        fun launchCamera() {
            val hasCamera = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (hasCamera) {
                val tempFile = File(context.cacheDir, "camera_item_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                cameraTempUri = uri
                cameraLauncher.launch(uri)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        val filteredCatalog = remember(catalogItems, catalogSearch) {
            if (catalogSearch.isBlank()) catalogItems
            else {
                val q = catalogSearch.trim().lowercase()
                catalogItems.filter {
                    it.name.lowercase().contains(q) ||
                    (it.description?.lowercase()?.contains(q) == true) ||
                    (it.category?.lowercase()?.contains(q) == true)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = {
                Text(
                    text = if (isEditing) "Edit Line Item" else "Add Line Item",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isEditing) {
                        TabRow(selectedTabIndex = activeTab) {
                            Tab(
                                selected = activeTab == 0,
                                onClick = { activeTab = 0 },
                                text = { Text("From Catalog (${catalogItems.size})", fontSize = 12.sp) }
                            )
                            Tab(
                                selected = activeTab == 1,
                                onClick = { activeTab = 1 },
                                text = { Text("New Item", fontSize = 12.sp) }
                            )
                        }
                    }

                    if (itemError != null) {
                        Text(
                            text = itemError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (activeTab == 0 && !isEditing) {
                        // Catalog Selector Tab
                        OutlinedTextField(
                            value = catalogSearch,
                            onValueChange = { catalogSearch = it },
                            label = { Text("Search Catalog") },
                            placeholder = { Text("Search by name or category") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (catalogSearch.isNotEmpty()) {
                                        IconButton(onClick = { catalogSearch = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    IconButton(
                                        onClick = { showDialogScanner = true },
                                        modifier = Modifier.testTag("dialog_catalog_scan_qr_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Scan QR",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (catalogItems.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No saved catalog items yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = { activeTab = 1 },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Create New Item Instead")
                                    }
                                }
                            }
                        } else if (filteredCatalog.isEmpty()) {
                            Text(
                                text = "No catalog items matched '$catalogSearch'",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Text(
                                text = "Tap an item to select:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredCatalog.forEach { catItem ->
                                    val isSelected = itemName == catItem.name
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                itemName = catItem.name
                                                itemDesc = catItem.description ?: ""
                                                itemPrice = if (catItem.defaultUnitPrice % 1.0 == 0.0) {
                                                    catItem.defaultUnitPrice.toInt().toString()
                                                } else {
                                                    catItem.defaultUnitPrice.toString()
                                                }
                                                itemUnit = catItem.unit ?: "pcs"
                                                itemImagePath = catItem.imagePath
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!catItem.imagePath.isNullOrBlank()) {
                                                val file = File(catItem.imagePath)
                                                if (file.exists()) {
                                                    AsyncImage(
                                                        model = file,
                                                        contentDescription = catItem.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ShoppingCart,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = catItem.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                if (!catItem.description.isNullOrBlank()) {
                                                    Text(
                                                        text = catItem.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = CurrencyFormatter.format(catItem.defaultUnitPrice, currencySymbol),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                if (!catItem.unit.isNullOrBlank()) {
                                                    Text(
                                                        text = "per ${catItem.unit}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (itemName.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Selected: $itemName",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = itemQty,
                                    onValueChange = { itemQty = it },
                                    label = { Text("Quantity") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f).testTag("dialog_item_qty_input")
                                )

                                OutlinedTextField(
                                    value = itemPrice,
                                    onValueChange = { itemPrice = it },
                                    label = { Text("Price ($currencySymbol)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f).testTag("dialog_item_price_input")
                                )
                            }
                        }
                    } else {
                        // Custom / New Item Tab or Edit Item Form
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showDialogScanner = true },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("dialog_scan_qr_autofill_btn")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan QR to Autofill", fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Item Name / Service *") },
                            placeholder = { Text("e.g. Website Design, Consulting, Hardware") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("dialog_item_name_input")
                        )

                        OutlinedTextField(
                            value = itemDesc,
                            onValueChange = { itemDesc = it },
                            label = { Text("Description (Optional)") },
                            placeholder = { Text("e.g. Includes domain, hosting and SSL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = itemQty,
                                onValueChange = { itemQty = it },
                                label = { Text("Qty *") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("dialog_item_qty_input")
                            )

                            OutlinedTextField(
                                value = itemUnit,
                                onValueChange = { itemUnit = it },
                                label = { Text("Unit") },
                                placeholder = { Text("pcs, hrs, nos") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = itemPrice,
                            onValueChange = { itemPrice = it },
                            label = { Text("Unit Price ($currencySymbol) *") },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_item_price_input")
                        )

                        // Item Image Picker Section
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Item Photo (Shows on Bill & PDF)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (!itemImagePath.isNullOrBlank()) {
                                        val imgFile = File(itemImagePath!!)
                                        if (imgFile.exists()) {
                                            AsyncImage(
                                                model = imgFile,
                                                contentDescription = "Item Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                imagePicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Gallery", fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { launchCamera() },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Camera", fontSize = 11.sp)
                                        }
                                        IconButton(
                                            onClick = { itemImagePath = null },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove photo",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    imagePicker.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddPhotoAlternate,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Gallery", fontSize = 12.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { launchCamera() },
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoCamera,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Camera", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    if (isOptimizingImage) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Optimizing...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        if (!isEditing) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { saveToCatalog = !saveToCatalog },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = saveToCatalog,
                                    onCheckedChange = { saveToCatalog = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = "Save item to Catalog",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Reusable across all future bills with custom photo and price",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    val parsedQty = itemQty.toDoubleOrNull() ?: 0.0
                    val parsedPrice = itemPrice.toDoubleOrNull() ?: 0.0
                    if (parsedQty > 0 && parsedPrice > 0) {
                        Text(
                            text = "Line Total: ${CurrencyFormatter.format(parsedQty * parsedPrice, currencySymbol)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = itemQty.toDoubleOrNull()
                        val p = itemPrice.toDoubleOrNull()
                        if (itemName.isBlank()) {
                            itemError = "Item name is required."
                            return@Button
                        }
                        if (q == null || q <= 0.0) {
                            itemError = "Enter a valid positive quantity."
                            return@Button
                        }
                        if (p == null || p < 0.0) {
                            itemError = "Enter a valid unit price."
                            return@Button
                        }

                        val newItem = BillItemEntity(
                            id = existingItem?.id ?: 0L,
                            billId = billId ?: 0L,
                            name = itemName.trim(),
                            description = itemDesc.trim().ifEmpty { null },
                            quantity = q,
                            unitPrice = p,
                            unit = itemUnit.trim().ifEmpty { null },
                            amount = q * p,
                            imagePath = itemImagePath
                        )

                        if (isEditing && itemToEditIndex != null) {
                            items[itemToEditIndex!!] = newItem
                        } else {
                            items.add(newItem)
                        }

                        // Save to reusable catalog if option was selected
                        if (saveToCatalog && !isEditing) {
                            viewModel.saveCatalogItem(
                                CatalogItemEntity(
                                    name = itemName.trim(),
                                    description = itemDesc.trim().ifEmpty { null },
                                    defaultUnitPrice = p,
                                    unit = itemUnit.trim().ifEmpty { null },
                                    category = categoryName,
                                    imagePath = itemImagePath,
                                    qrCode = CatalogItemEntity.generateNewQrCode()
                                )
                            )
                        }

                        showAddItemDialog = false
                    },
                    modifier = Modifier.testTag("dialog_save_item_button")
                ) {
                    Text(if (isEditing) "Save" else "Add to Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )

        if (showDialogScanner) {
            QrScannerDialog(
                onDismiss = { showDialogScanner = false },
                onQrScanned = { scannedCode ->
                    coroutineScope.launch {
                        val resolved = viewModel.resolveItemFromQr(scannedCode)
                        if (resolved != null) {
                            itemName = resolved.name
                            itemDesc = resolved.description ?: ""
                            itemPrice = if (resolved.defaultUnitPrice % 1.0 == 0.0) resolved.defaultUnitPrice.toInt().toString() else resolved.defaultUnitPrice.toString()
                            itemUnit = resolved.unit ?: "pcs"
                            itemImagePath = resolved.imagePath
                            activeTab = 1
                            Toast.makeText(context, "Filled from QR: ${resolved.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            itemName = scannedCode
                            Toast.makeText(context, "Scanned: $scannedCode", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    // Discard Confirmation Dialog
    if (showDiscardDialog) {
        ConfirmationDialog(
            title = "Discard Changes?",
            message = "Any unsaved modifications to this bill will be lost.",
            confirmText = "Discard",
            isDestructive = true,
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    // Add Client Dialog
    if (showAddClientDialog) {
        var newClientName by remember { mutableStateOf(clientName) }
        var newClientEmail by remember { mutableStateOf(clientEmail) }
        var newClientPhone by remember { mutableStateOf(clientPhone) }
        var newClientAddress by remember { mutableStateOf(clientAddress) }
        var clientNameError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddClientDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Client")
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Save client info to your directory. It will autofill on future bills and be tracked on the dashboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newClientName,
                        onValueChange = {
                            newClientName = it
                            clientNameError = false
                        },
                        label = { Text("Client / Business Name *") },
                        placeholder = { Text("e.g. Nexus Tech Ltd") },
                        isError = clientNameError,
                        supportingText = if (clientNameError) {
                            { Text("Client name is required") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_client_name_input")
                    )

                    OutlinedTextField(
                        value = newClientEmail,
                        onValueChange = { newClientEmail = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("billing@client.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_client_email_input")
                    )

                    OutlinedTextField(
                        value = newClientPhone,
                        onValueChange = { newClientPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91 98765 43210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_client_phone_input")
                    )

                    OutlinedTextField(
                        value = newClientAddress,
                        onValueChange = { newClientAddress = it },
                        label = { Text("Billing Address") },
                        placeholder = { Text("Street, City, State, ZIP") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("new_client_address_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newClientName.isBlank()) {
                            clientNameError = true
                            return@Button
                        }
                        val clientEntity = ClientEntity(
                            name = newClientName.trim(),
                            email = newClientEmail.trim().ifEmpty { null },
                            phone = newClientPhone.trim().ifEmpty { null },
                            address = newClientAddress.trim().ifEmpty { null }
                        )
                        viewModel.saveClient(clientEntity) {
                            clientName = clientEntity.name
                            clientEmail = clientEntity.email ?: ""
                            clientPhone = clientEntity.phone ?: ""
                            clientAddress = clientEntity.address ?: ""
                            showAddClientDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_new_client_dialog_button")
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClientDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Product QR Code Live Scanner Dialog
    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onQrScanned = { scannedCode ->
                coroutineScope.launch {
                    val resolved = viewModel.resolveItemFromQr(scannedCode)
                    if (resolved != null) {
                        val newItem = BillItemEntity(
                            billId = billId ?: 0L,
                            name = resolved.name,
                            description = resolved.description,
                            quantity = 1.0,
                            unitPrice = resolved.defaultUnitPrice,
                            unit = resolved.unit,
                            amount = resolved.defaultUnitPrice,
                            imagePath = resolved.imagePath
                        )
                        items.add(newItem)
                        Toast.makeText(
                            context,
                            "Scanned & Added: ${resolved.name} (${currencySymbol}${resolved.defaultUnitPrice})",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Scanned QR: $scannedCode (No matching catalog item found)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}
