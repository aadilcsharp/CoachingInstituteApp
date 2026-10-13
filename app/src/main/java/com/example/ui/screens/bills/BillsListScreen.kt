package com.example.ui.screens.bills

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BillCategory
import com.example.data.model.BillWithItems
import com.example.data.model.PaymentStatus
import com.example.ui.components.AppTopBar
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.BillViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.ShareHelper

@Composable
fun BillsListScreen(
    viewModel: BillViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bills by viewModel.filteredBills.collectAsStateWithLifecycle()
    val rawBills by viewModel.allBills.collectAsStateWithLifecycle()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterStatus by viewModel.filterStatus.collectAsStateWithLifecycle()
    val filterCategory by viewModel.filterCategory.collectAsStateWithLifecycle()
    val filterSortBy by viewModel.filterSortBy.collectAsStateWithLifecycle()

    var billToDelete by remember { mutableStateOf<BillWithItems?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Bills & Invoices",
                actions = {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("bills_sort_button")
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Date (Newest first)") },
                                onClick = {
                                    viewModel.filterSortBy.value = "DATE_DESC"
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Oldest first)") },
                                onClick = {
                                    viewModel.filterSortBy.value = "DATE_ASC"
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Amount (High to Low)") },
                                onClick = {
                                    viewModel.filterSortBy.value = "AMOUNT_DESC"
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Amount (Low to High)") },
                                onClick = {
                                    viewModel.filterSortBy.value = "AMOUNT_ASC"
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Screen.CreateEditBill()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_bill_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Bill")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search by bill #, client, category...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("bills_search_input")
            )

            // Status Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filterStatus == null,
                        onClick = { viewModel.filterStatus.value = null },
                        label = { Text("All Status") },
                        modifier = Modifier.testTag("filter_status_all")
                    )
                }
                items(PaymentStatus.entries) { status ->
                    FilterChip(
                        selected = filterStatus == status,
                        onClick = { viewModel.filterStatus.value = if (filterStatus == status) null else status },
                        label = { Text(status.displayName) },
                        modifier = Modifier.testTag("filter_status_${status.name}")
                    )
                }
            }

            // Category Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filterCategory == null,
                        onClick = { viewModel.filterCategory.value = null },
                        label = { Text("All Categories") },
                        modifier = Modifier.testTag("filter_category_all")
                    )
                }
                items(BillCategory.entries) { cat ->
                    FilterChip(
                        selected = filterCategory == cat,
                        onClick = { viewModel.filterCategory.value = if (filterCategory == cat) null else cat },
                        label = { Text(cat.displayName) },
                        modifier = Modifier.testTag("filter_category_${cat.name}")
                    )
                }
            }

            // Results List
            if (bills.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (rawBills.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Receipt,
                            title = "No Bills Created Yet",
                            description = "Tap the plus button below to create your first professional bill.",
                            actionButtonText = "Create Bill",
                            onActionClick = { onNavigate(Screen.CreateEditBill()) }
                        )
                    } else {
                        EmptyStateView(
                            icon = Icons.Default.Search,
                            title = "No Matching Bills",
                            description = "Try adjusting your search query or removing active status/category filters.",
                            actionButtonText = "Reset Filters",
                            onActionClick = {
                                viewModel.searchQuery.value = ""
                                viewModel.filterStatus.value = null
                                viewModel.filterCategory.value = null
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(bills, key = { it.bill.id }) { item ->
                        BillListItemCard(
                            billWithItems = item,
                            currencySymbol = profile.currencySymbol,
                            onClick = { onNavigate(Screen.BillDetail(item.bill.id)) },
                            onSharePdf = {
                                val pdfFile = viewModel.getBillPdfFile(context, item)
                                ShareHelper.sharePdf(context, pdfFile, "Invoice ${item.bill.billNumber}")
                            },
                            onShareWhatsApp = {
                                val pdfFile = viewModel.getBillPdfFile(context, item)
                                ShareHelper.shareViaWhatsApp(context, pdfFile, item.bill.clientName, item.bill.billNumber)
                            },
                            onMarkPaid = {
                                viewModel.updatePaymentStatus(item.bill, PaymentStatus.PAID)
                            },
                            onDuplicate = {
                                viewModel.duplicateBill(item) { newId ->
                                    onNavigate(Screen.BillDetail(newId))
                                }
                            },
                            onDelete = { billToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog for deletion
    billToDelete?.let { target ->
        ConfirmationDialog(
            title = "Delete Bill?",
            message = "Are you sure you want to delete invoice ${target.bill.billNumber}? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteBill(target.bill)
                billToDelete = null
            },
            onDismiss = { billToDelete = null }
        )
    }
}

@Composable
fun BillListItemCard(
    billWithItems: BillWithItems,
    currencySymbol: String,
    onClick: () -> Unit,
    onSharePdf: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onMarkPaid: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val b = billWithItems.bill
    val status = PaymentStatus.fromString(b.paymentStatus)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("bill_card_${b.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = b.billNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = status)
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp).testTag("bill_menu_${b.id}")
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share PDF") },
                            leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onSharePdf()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share on WhatsApp") },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onShareWhatsApp()
                            }
                        )
                        if (status != PaymentStatus.PAID) {
                            DropdownMenuItem(
                                text = { Text("Mark as Paid") },
                                leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onMarkPaid()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Duplicate Bill") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = b.clientName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (b.title.isNotBlank()) {
                Text(
                    text = b.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Issued: ${DateUtils.formatDate(b.billDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${billWithItems.items.size} items • ${b.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.format(b.totalAmount, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (b.paidAmount > 0 && b.paidAmount < b.totalAmount) {
                        val balance = b.totalAmount - b.paidAmount
                        Text(
                            text = "Bal: ${CurrencyFormatter.format(balance, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
