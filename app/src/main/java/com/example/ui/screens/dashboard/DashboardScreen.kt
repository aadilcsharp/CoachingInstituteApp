package com.example.ui.screens.dashboard

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.BillWithItems
import com.example.data.model.CatalogItemEntity
import com.example.data.model.PaymentStatus
import com.example.ui.components.AppTopBar
import com.example.ui.components.ChartPoint
import com.example.ui.components.DonutSlice
import com.example.ui.components.EmptyStateView
import com.example.ui.components.InteractiveBarChart
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.components.InteractiveLineChart
import com.example.ui.components.ProductQrLabelDialog
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.SummaryCard
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.BillViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import android.widget.Toast
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

@Composable
fun DashboardScreen(
    viewModel: BillViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val allBills by viewModel.allBills.collectAsStateWithLifecycle()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.dashboardPeriod.collectAsStateWithLifecycle()
    val clientCount by viewModel.clientCount.collectAsStateWithLifecycle()
    val catalogItems by viewModel.allCatalogItems.collectAsStateWithLifecycle()
    val licenseInfo by viewModel.licenseInfo.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showScanner by remember { mutableStateOf(false) }
    var scannedItemDetails by remember { mutableStateOf<CatalogItemEntity?>(null) }
    var itemForLabelDialog by remember { mutableStateOf<CatalogItemEntity?>(null) }

    val currencySymbol = profile.currencySymbol

    // Filter bills based on selected period for KPI and charts
    val filteredPeriodBills = remember(allBills, selectedPeriod) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "THIS_MONTH" -> {
                val start = DateUtils.getStartOfMonth(cal)
                allBills.filter { it.bill.billDate >= start }
            }
            "LAST_30_DAYS" -> {
                val thirtyDaysAgo = now - (30L * 86400000L)
                allBills.filter { it.bill.billDate >= thirtyDaysAgo }
            }
            "THIS_YEAR" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val startOfYear = cal.timeInMillis
                allBills.filter { it.bill.billDate >= startOfYear }
            }
            else -> allBills
        }
    }

    // Calculations
    val totalBilled = remember(filteredPeriodBills) { filteredPeriodBills.sumOf { it.bill.totalAmount } }
    val totalPaid = remember(filteredPeriodBills) { filteredPeriodBills.sumOf { it.bill.paidAmount } }
    val totalOutstanding = remember(filteredPeriodBills) {
        filteredPeriodBills.sumOf { (it.bill.totalAmount - it.bill.paidAmount).coerceAtLeast(0.0) }
    }
    val billCount = filteredPeriodBills.size

    // Monthly Chart Points (Last 6 Months)
    val monthlyTrendPoints = remember(allBills) {
        val cal = Calendar.getInstance()
        val list = mutableListOf<ChartPoint>()
        for (i in 5 downTo 0) {
            val monthCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
            }
            val start = DateUtils.getStartOfMonth(monthCal)
            val end = DateUtils.getEndOfMonth(monthCal)
            val monthLabel = DateUtils.formatShortMonth(monthCal.timeInMillis)

            val monthBills = allBills.filter { it.bill.billDate in start..end }
            val billedInMonth = monthBills.sumOf { it.bill.totalAmount }
            val paidInMonth = monthBills.sumOf { it.bill.paidAmount }

            list.add(
                ChartPoint(
                    label = monthLabel,
                    value = billedInMonth,
                    secondaryValue = paidInMonth
                )
            )
        }
        list
    }

    // Category Donut Slices
    val categorySlices = remember(filteredPeriodBills) {
        val categoryColors = listOf(
            Color(0xFF2563EB), // Blue
            Color(0xFF10B981), // Emerald
            Color(0xFFF59E0B), // Amber
            Color(0xFF8B5CF6), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFF06B6D4), // Cyan
            Color(0xFF64748B)  // Slate
        )
        val grouped = filteredPeriodBills.groupBy { it.bill.category }
        grouped.entries.mapIndexed { idx, entry ->
            val color = categoryColors[idx % categoryColors.size]
            val sum = entry.value.sumOf { it.bill.totalAmount }
            DonutSlice(
                label = entry.key,
                value = sum,
                color = color
            )
        }.sortedByDescending { it.value }
    }

    val recentBills = remember(allBills) { allBills.take(5) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Dashboard",
                actions = {
                    IconButton(
                        onClick = { showScanner = true },
                        modifier = Modifier.testTag("dashboard_qr_scan_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Product QR Code",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (allBills.isEmpty()) {
                        TextButton(
                            onClick = { viewModel.seedSampleData() },
                            modifier = Modifier.testTag("seed_demo_button")
                        ) {
                            Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("Load Demo")
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
                modifier = Modifier.testTag("dashboard_create_bill_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Bill")
            }
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subscription Expiry Warning Banner (<= 5 days left)
            if (licenseInfo.isExpiringSoon) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Screen.SubscriptionLicense) }
                            .testTag("dashboard_license_expiry_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Subscription Expiring Soon (${licenseInfo.daysRemaining} days left)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Your ${licenseInfo.tier.title} expires on ${licenseInfo.endDateStr}. Tap here to renew your offline license key.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }

            // Header & Time Period Filters
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = profile.businessName.ifEmpty { "My Business" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Financial Overview",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Period filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL_TIME" to "All Time",
                            "THIS_MONTH" to "This Month",
                            "LAST_30_DAYS" to "30 Days",
                            "THIS_YEAR" to "This Year"
                        ).forEach { (key, label) ->
                            val selected = selectedPeriod == key
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.dashboardPeriod.value = key },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.testTag("filter_period_$key")
                            )
                        }
                    }
                }
            }

            // 4 Summary Metric Cards (2x2 Grid) + Total Clients Card
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Total Billed",
                            value = CurrencyFormatter.formatCompact(totalBilled, currencySymbol),
                            subtitle = "$billCount bills issued",
                            icon = Icons.Default.TrendingUp,
                            iconBgColor = Color(0xFFDBEAFE),
                            iconTint = Color(0xFF1E3A8A),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Collected",
                            value = CurrencyFormatter.formatCompact(totalPaid, currencySymbol),
                            subtitle = if (totalBilled > 0) "${(totalPaid / totalBilled * 100).toInt()}% collected" else "0%",
                            icon = Icons.Default.CheckCircle,
                            iconBgColor = Color(0xFFD1FAE5),
                            iconTint = Color(0xFF065F46),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Outstanding",
                            value = CurrencyFormatter.formatCompact(totalOutstanding, currencySymbol),
                            subtitle = "Pending collection",
                            icon = Icons.Default.Warning,
                            iconBgColor = Color(0xFFFEE2E2),
                            iconTint = Color(0xFF991B1B),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Bills Count",
                            value = billCount.toString(),
                            subtitle = "In selected period",
                            icon = Icons.Default.ReceiptLong,
                            iconBgColor = Color(0xFFFEF3C7),
                            iconTint = Color(0xFF92400E),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Clients",
                            value = clientCount.toString(),
                            subtitle = if (clientCount == 1) "1 in directory" else "$clientCount in directory",
                            icon = Icons.Default.Person,
                            iconBgColor = Color(0xFFEDE9FE),
                            iconTint = Color(0xFF6D28D9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigate(Screen.ManageClients) }
                        )
                        SummaryCard(
                            title = "Catalog & QR",
                            value = catalogItems.size.toString(),
                            subtitle = "Scan & print labels",
                            icon = Icons.Default.QrCode,
                            iconBgColor = Color(0xFFE0E7FF),
                            iconTint = Color(0xFF3730A3),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigate(Screen.ManageItems) }
                        )
                    }
                }
            }

            if (allBills.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                icon = Icons.Default.ReceiptLong,
                                title = "Welcome to BillCraft",
                                description = "Start managing your bills and tracking revenue today. Create your first bill or populate with sample invoices to explore templates.",
                                actionButtonText = "Create First Bill",
                                onActionClick = { onNavigate(Screen.CreateEditBill()) }
                            )
                        }
                    }
                }
            } else {

                // Spending & Revenue Trend Line Chart
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Monthly Billing Trend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Interactive 6-month trajectory",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            InteractiveLineChart(
                                points = monthlyTrendPoints,
                                currencySymbol = currencySymbol
                            )
                        }
                    }
                }

                // Monthly Comparison Bar Chart (Billed vs Collected)
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Billed vs Collected Comparison",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Monthly collection efficiency",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            InteractiveBarChart(
                                bars = monthlyTrendPoints,
                                currencySymbol = currencySymbol
                            )
                        }
                    }
                }

                // Category Expense Breakdown (Donut Chart)
                if (categorySlices.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Category Revenue Breakdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap slices or tags to inspect distribution",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                InteractiveDonutChart(
                                    slices = categorySlices,
                                    currencySymbol = currencySymbol
                                )
                            }
                        }
                    }
                }

                // Recent Bills Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Bills",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { onNavigate(Screen.Bills) },
                            modifier = Modifier.testTag("view_all_bills_button")
                        ) {
                            Text("View All")
                        }
                    }
                }

                items(recentBills, key = { it.bill.id }) { billWithItems ->
                    DashboardBillCard(
                        billWithItems = billWithItems,
                        currencySymbol = currencySymbol,
                        onClick = { onNavigate(Screen.BillDetail(billWithItems.bill.id)) }
                    )
                }
            }
        }
    }

    // 1. Live Camera QR Scanner
    if (showScanner) {
        QrScannerDialog(
            onDismiss = { showScanner = false },
            onQrScanned = { scannedCode ->
                coroutineScope.launch {
                    val resolved = viewModel.resolveItemFromQr(scannedCode)
                    if (resolved != null) {
                        scannedItemDetails = resolved
                        Toast.makeText(context, "Item detected: ${resolved.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Scanned: $scannedCode (Not found in catalog)", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // 2. Scanned Product Details Quick View Dialog
    if (scannedItemDetails != null) {
        val item = scannedItemDetails!!
        ScannedProductDetailsDialog(
            item = item,
            currencySymbol = currencySymbol,
            onDismiss = { scannedItemDetails = null },
            onAddToNewBill = {
                viewModel.pendingItemForNewBill = item
                scannedItemDetails = null
                onNavigate(Screen.CreateEditBill())
            },
            onViewQrSticker = {
                itemForLabelDialog = item
                scannedItemDetails = null
            },
            onManageInCatalog = {
                scannedItemDetails = null
                onNavigate(Screen.ManageItems)
            }
        )
    }

    // 3. Product QR Code & Sticker Printable Dialog
    if (itemForLabelDialog != null) {
        ProductQrLabelDialog(
            item = itemForLabelDialog!!,
            currencySymbol = currencySymbol,
            businessName = profile.businessName,
            onDismiss = { itemForLabelDialog = null }
        )
    }
}

@Composable
fun ScannedProductDetailsDialog(
    item: CatalogItemEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAddToNewBill: () -> Unit,
    onViewQrSticker: () -> Unit,
    onManageInCatalog: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("scanned_product_details_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Item Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Auto-detected from scanned product QR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_scanned_details")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Product Card Preview
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!item.imagePath.isNullOrBlank()) {
                            val f = File(item.imagePath)
                            if (f.exists()) {
                                AsyncImage(
                                    model = f,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                ScannedPlaceholderBox()
                            }
                        } else {
                            ScannedPlaceholderBox()
                        }

                        Spacer(modifier = Modifier.width(14.dp))

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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${CurrencyFormatter.format(item.defaultUnitPrice, currencySymbol)} / ${item.unit ?: "pcs"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            if (!item.category.isNullOrBlank()) {
                                Text(
                                    text = "Category: ${item.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // QR ID Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "QR ID: ${item.getEffectiveQrCode()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddToNewBill,
                        modifier = Modifier.fillMaxWidth().testTag("btn_scanned_add_to_bill")
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Bill with this Item")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onViewQrSticker,
                            modifier = Modifier.weight(1f).testTag("btn_scanned_view_sticker")
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print QR", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onManageInCatalog,
                            modifier = Modifier.weight(1f).testTag("btn_scanned_manage_catalog")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Catalog", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedPlaceholderBox() {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun DashboardBillCard(
    billWithItems: BillWithItems,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val b = billWithItems.bill
    val status = PaymentStatus.fromString(b.paymentStatus)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("recent_bill_card_${b.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = b.billNumber,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${DateUtils.formatDate(b.billDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = b.clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(b.totalAmount, currencySymbol),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = status)
            }
        }
    }
}
