package com.example.ui.screens.items

import android.Manifest
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CatalogItemEntity
import com.example.ui.components.AppTopBar
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductQrLabelDialog
import com.example.ui.components.QrScannerDialog
import com.example.ui.viewmodel.BillViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ManageItemsScreen(
    viewModel: BillViewModel,
    onBack: () -> Unit,
    onNavigateToLicense: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalogItems by viewModel.allCatalogItems.collectAsStateWithLifecycle()
    val licenseInfo by viewModel.licenseInfo.collectAsStateWithLifecycle()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showItemLimitDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<CatalogItemEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<CatalogItemEntity?>(null) }
    var itemForQrLabel by remember { mutableStateOf<CatalogItemEntity?>(null) }
    var showScannerDialog by remember { mutableStateOf(false) }

    fun checkAndOpenAddItem() {
        if (catalogItems.size >= licenseInfo.tier.maxCatalogItems) {
            showItemLimitDialog = true
        } else {
            itemToEdit = null
            showAddEditDialog = true
        }
    }

    val filteredItems = remember(catalogItems, searchQuery) {
        if (searchQuery.isBlank()) catalogItems
        else {
            val q = searchQuery.trim().lowercase()
            catalogItems.filter {
                it.name.lowercase().contains(q) ||
                (it.description?.lowercase()?.contains(q) == true) ||
                (it.category?.lowercase()?.contains(q) == true) ||
                (it.qrCode.lowercase().contains(q))
            }
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Manage Items Catalog",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showScannerDialog = true },
                        modifier = Modifier.testTag("scan_catalog_qr_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Item QR Code",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    itemToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_catalog_item_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Item")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search catalog items by name, QR ID, category...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(
                            onClick = { showScannerDialog = true },
                            modifier = Modifier.testTag("search_scan_qr_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR Code",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("catalog_search_input")
            )

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (catalogItems.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.ShoppingCart,
                            title = "No Catalog Items Yet",
                            description = "Add reusable products or services here with custom prices and photos. They will show up automatically when creating or editing bills.",
                            actionButtonText = "Add First Item",
                            onActionClick = {
                                itemToEdit = null
                                showAddEditDialog = true
                            }
                        )
                    } else {
                        EmptyStateView(
                            icon = Icons.Default.Search,
                            title = "No Matching Items",
                            description = "Try adjusting your search query.",
                            actionButtonText = "Clear Search",
                            onActionClick = { searchQuery = "" }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        CatalogItemCard(
                            item = item,
                            currencySymbol = profile.currencySymbol,
                            onEdit = {
                                itemToEdit = item
                                showAddEditDialog = true
                            },
                            onDelete = {
                                itemToDelete = item
                            },
                            onShowQr = {
                                itemForQrLabel = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Catalog Item Dialog
    if (showAddEditDialog) {
        AddEditCatalogItemDialog(
            item = itemToEdit,
            currencySymbol = profile.currencySymbol,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedItem ->
                viewModel.saveCatalogItem(updatedItem)
                showAddEditDialog = false
            },
            onPickImage = { uri, onResult ->
                viewModel.optimizeAndSaveItemImage(context, uri, onResult)
            }
        )
    }

    // Delete Confirmation
    itemToDelete?.let { target ->
        ConfirmationDialog(
            title = "Delete Item?",
            message = "Are you sure you want to delete '${target.name}' from your catalog? Existing bills that use this item will remain unaffected.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteCatalogItem(target)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    // Product QR Code & Printable Label Dialog
    itemForQrLabel?.let { targetItem ->
        ProductQrLabelDialog(
            item = targetItem,
            currencySymbol = profile.currencySymbol,
            businessName = profile.businessName,
            onDismiss = { itemForQrLabel = null }
        )
    }

    // Subscription Item Limit Exceeded Dialog
    if (showItemLimitDialog) {
        AlertDialog(
            onDismissRequest = { showItemLimitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Product Catalog Limit")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You have reached the maximum of ${licenseInfo.tier.maxCatalogItems} catalog items allowed on the ${licenseInfo.tier.title}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "To add more products (up to 15 on Basic, 200 on Premium, or Unlimited on Enterprise), enter an offline SHA-256 license key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showItemLimitDialog = false
                        onNavigateToLicense?.invoke()
                    },
                    modifier = Modifier.testTag("btn_upgrade_from_item_limit")
                ) {
                    Text("Upgrade License")
                }
            },
            dismissButton = {
                TextButton(onClick = { showItemLimitDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Live QR Scanner Dialog
    if (showScannerDialog) {
        QrScannerDialog(
            onDismiss = { showScannerDialog = false },
            onQrScanned = { scannedText ->
                coroutineScope.launch {
                    val resolved = viewModel.resolveItemFromQr(scannedText)
                    if (resolved != null) {
                        itemForQrLabel = resolved
                        Toast.makeText(context, "Item found: ${resolved.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        searchQuery = scannedText
                        Toast.makeText(context, "Scanned: $scannedText", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun CatalogItemCard(
    item: CatalogItemEntity,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onShowQr)
            .testTag("catalog_item_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item photo thumbnail
            if (!item.imagePath.isNullOrBlank()) {
                val file = File(item.imagePath)
                if (file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    ItemPlaceholderBox()
                }
            } else {
                ItemPlaceholderBox()
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!item.description.isNullOrBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${CurrencyFormatter.format(item.defaultUnitPrice, currencySymbol)} / ${item.unit ?: "pcs"}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!item.category.isNullOrBlank()) {
                        Text(
                            text = " • ${item.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShowQr, modifier = Modifier.size(36.dp).testTag("item_qr_btn_${item.id}")) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "View & Print QR Code",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp).testTag("item_edit_btn_${item.id}")) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp).testTag("item_delete_btn_${item.id}")) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemPlaceholderBox() {
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun AddEditCatalogItemDialog(
    item: CatalogItemEntity?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (CatalogItemEntity) -> Unit,
    onPickImage: (Uri, (String?) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var unitPrice by remember { mutableStateOf(item?.defaultUnitPrice?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var unit by remember { mutableStateOf(item?.unit ?: "pcs") }
    var category by remember { mutableStateOf(item?.category ?: "General") }
    var qrCode by remember { mutableStateOf(item?.qrCode?.ifBlank { CatalogItemEntity.generateNewQrCode() } ?: CatalogItemEntity.generateNewQrCode()) }
    var imagePath by remember { mutableStateOf(item?.imagePath) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Modern Zero-Permission Photo Picker (Gallery)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickImage(uri) { optimizedPath ->
                imagePath = optimizedPath
            }
        }
    }

    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempUri != null) {
            onPickImage(cameraTempUri!!) { optimizedPath ->
                imagePath = optimizedPath
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val tempFile = File(context.cacheDir, "camera_catalog_${System.currentTimeMillis()}.jpg")
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
            val tempFile = File(context.cacheDir, "camera_catalog_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            cameraTempUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add Catalog Item" else "Edit Catalog Item") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Image Picker Preview
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        if (!imagePath.isNullOrBlank()) {
                            val f = File(imagePath!!)
                            if (f.exists()) {
                                AsyncImage(
                                    model = f,
                                    contentDescription = "Selected Item Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("pick_item_gallery_btn")
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Gallery", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { launchCamera() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("pick_item_camera_btn")
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Camera", fontSize = 12.sp)
                                }
                            }
                            if (imagePath != null) {
                                TextButton(
                                    onClick = { imagePath = null },
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text("Remove Photo", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Item Name *") },
                        placeholder = { Text("e.g. Ergonomic Office Chair") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("catalog_dialog_name_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("catalog_dialog_desc_input")
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = unitPrice,
                            onValueChange = { unitPrice = it },
                            label = { Text("Price ($currencySymbol) *") },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.2f).testTag("catalog_dialog_price_input")
                        )

                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
                            placeholder = { Text("pcs, hrs") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("catalog_dialog_unit_input")
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        placeholder = { Text("Services, Hardware, etc.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("catalog_dialog_cat_input")
                    )
                }

                // QR Code ID field
                item {
                    OutlinedTextField(
                        value = qrCode,
                        onValueChange = { qrCode = it.trim() },
                        label = { Text("Product QR Code / Barcode ID *") },
                        placeholder = { Text("e.g. BC-100234") },
                        supportingText = { Text("Unique code printed on product label for instant scanning") },
                        trailingIcon = {
                            IconButton(onClick = { qrCode = CatalogItemEntity.generateNewQrCode() }) {
                                Icon(Icons.Default.QrCode, contentDescription = "Regenerate QR ID", modifier = Modifier.size(20.dp))
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("catalog_dialog_qr_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Item name is required."
                        return@Button
                    }
                    val price = unitPrice.toDoubleOrNull()
                    if (price == null || price < 0.0) {
                        errorMessage = "Enter a valid non-negative unit price."
                        return@Button
                    }

                    val toSave = CatalogItemEntity(
                        id = item?.id ?: 0L,
                        name = name.trim(),
                        description = description.trim().ifEmpty { null },
                        defaultUnitPrice = price,
                        unit = unit.trim().ifEmpty { "pcs" },
                        category = category.trim().ifEmpty { "General" },
                        imagePath = imagePath,
                        qrCode = qrCode.ifBlank { CatalogItemEntity.generateNewQrCode() }
                    )
                    onSave(toSave)
                },
                modifier = Modifier.testTag("catalog_dialog_save_btn")
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
