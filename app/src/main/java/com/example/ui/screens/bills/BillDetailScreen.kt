package com.example.ui.screens.bills

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.BillTemplateType
import com.example.data.model.BillWithItems
import com.example.data.model.PaymentStatus
import com.example.ui.components.AppTopBar
import com.example.ui.components.BillItemRow
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.BillViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.QrCodeGenerator
import com.example.util.ShareHelper
import java.io.File

@Composable
fun BillDetailScreen(
    billId: Long,
    viewModel: BillViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allBills by viewModel.allBills.collectAsStateWithLifecycle()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val previewBitmap by viewModel.previewBitmap.collectAsStateWithLifecycle()
    val isPdfLoading by viewModel.isPdfGenerating.collectAsStateWithLifecycle()

    val billWithItems = remember(allBills, billId) {
        allBills.firstOrNull { it.bill.id == billId }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: PDF Document Preview
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(billWithItems) {
        if (billWithItems != null) {
            viewModel.renderBillPreview(context, billWithItems)
        }
    }

    BackHandler {
        onBack()
    }

    if (billWithItems == null) {
        Scaffold(
            topBar = { AppTopBar(title = "Bill Details", onBack = onBack) }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Bill not found or has been deleted.")
            }
        }
        return
    }

    val bill = billWithItems.bill
    val items = billWithItems.items
    val status = PaymentStatus.fromString(bill.paymentStatus)
    val currencySymbol = profile.currencySymbol

    Scaffold(
        topBar = {
            AppTopBar(
                title = bill.billNumber,
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { onEdit(bill.id) },
                        modifier = Modifier.testTag("detail_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Bill")
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("detail_delete_button")
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete Bill", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = {
            // Action Bar for Sharing
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // WhatsApp Share (Prominent)
                    Button(
                        onClick = {
                            val pdfFile = viewModel.getBillPdfFile(context, billWithItems)
                            ShareHelper.shareViaWhatsApp(context, pdfFile, bill.clientName, bill.billNumber, bill.clientPhone)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).testTag("whatsapp_share_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", fontWeight = FontWeight.Bold)
                    }

                    // Native PDF Share
                    Button(
                        onClick = {
                            val pdfFile = viewModel.getBillPdfFile(context, billWithItems)
                            ShareHelper.sharePdf(context, pdfFile, "Invoice ${bill.billNumber}")
                        },
                        modifier = Modifier.weight(1f).testTag("native_pdf_share_btn")
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF")
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Switch tabs: Overview vs PDF Document Preview
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Details & Items") },
                    modifier = Modifier.testTag("tab_details")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("PDF Preview") },
                    modifier = Modifier.testTag("tab_pdf_preview")
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header summary card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (profile.showLogoOnBill && !profile.logoPath.isNullOrBlank()) {
                                            val logoFile = File(profile.logoPath!!)
                                            if (logoFile.exists()) {
                                                AsyncImage(
                                                    model = logoFile,
                                                    contentDescription = "Business Logo",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                        .padding(2.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = bill.billNumber,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Issued: ${DateUtils.formatDate(bill.billDate)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (bill.dueDate != null) {
                                                Text(
                                                    text = "Due: ${DateUtils.formatDate(bill.dueDate)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    StatusBadge(status = status)
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Total Amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = CurrencyFormatter.format(bill.totalAmount, currencySymbol),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Paid / Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        val balance = (bill.totalAmount - bill.paidAmount).coerceAtLeast(0.0)
                                        Text(
                                            text = "Paid: ${CurrencyFormatter.format(bill.paidAmount, currencySymbol)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF10B981)
                                        )
                                        if (balance > 0) {
                                            Text(
                                                text = "Bal: ${CurrencyFormatter.format(balance, currencySymbol)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                // Quick Status Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (status != PaymentStatus.PAID) {
                                        OutlinedButton(
                                            onClick = { viewModel.updatePaymentStatus(bill, PaymentStatus.PAID) },
                                            modifier = Modifier.weight(1f).testTag("mark_paid_btn")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mark as Paid")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.updatePaymentStatus(bill, PaymentStatus.UNPAID) },
                                            modifier = Modifier.weight(1f).testTag("mark_unpaid_btn")
                                        ) {
                                            Text("Mark as Unpaid")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val pdfFile = viewModel.getBillPdfFile(context, billWithItems)
                                            ShareHelper.savePdfToDownloads(context, pdfFile, "${bill.billNumber}.pdf")
                                        },
                                        modifier = Modifier.weight(1f).testTag("save_pdf_downloads_btn")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save PDF")
                                    }
                                }
                            }
                        }
                    }

                    // Client details
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Client Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(bill.clientName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                if (!bill.clientEmail.isNullOrBlank()) {
                                    Text("Email: ${bill.clientEmail}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!bill.clientPhone.isNullOrBlank()) {
                                    Text("Phone: ${bill.clientPhone}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!bill.clientAddress.isNullOrBlank()) {
                                    Text("Address: ${bill.clientAddress}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Line Items
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Line Items (${items.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                                items.forEach { item ->
                                    BillItemRow(
                                        item = item,
                                        currencySymbol = currencySymbol
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                                    Text(CurrencyFormatter.format(bill.subtotal, currencySymbol), fontWeight = FontWeight.Medium)
                                }

                                if (bill.discountAmount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Discount (${bill.discountPercent}%)", style = MaterialTheme.typography.bodyMedium)
                                        Text("-${CurrencyFormatter.format(bill.discountAmount, currencySymbol)}", color = Color(0xFFDC2626), fontWeight = FontWeight.Medium)
                                    }
                                }

                                if (bill.taxAmount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Tax (${bill.taxPercent}%)", style = MaterialTheme.typography.bodyMedium)
                                        Text(CurrencyFormatter.format(bill.taxAmount, currencySymbol), fontWeight = FontWeight.Medium)
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Final Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(CurrencyFormatter.format(bill.totalAmount, currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // Payment Instructions & QR Code Card
                    if ((profile.showPaymentQrOnBill && profile.paymentQrText.isNotBlank()) || profile.paymentDetails.isNotBlank()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Payment Instructions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        if (profile.showPaymentQrOnBill && profile.paymentQrText.isNotBlank()) {
                                            val qrBitmap = remember(profile.paymentQrText) {
                                                QrCodeGenerator.generateQrBitmap(profile.paymentQrText, 250)
                                            }
                                            if (qrBitmap != null) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(4.dp)
                                                ) {
                                                    Image(
                                                        bitmap = qrBitmap.asImageBitmap(),
                                                        contentDescription = "Payment QR Code",
                                                        modifier = Modifier
                                                            .size(92.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                            .padding(4.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Scan to Pay",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (profile.paymentDetails.isNotBlank()) {
                                                Text(
                                                    text = profile.paymentDetails,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (profile.paymentQrText.isNotBlank()) {
                                                Text(
                                                    text = "UPI / Pay Details: ${profile.paymentQrText}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notes & Terms
                    if (!bill.notes.isNullOrBlank() || !bill.terms.isNullOrBlank()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (!bill.notes.isNullOrBlank()) {
                                        Text("Notes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Text(bill.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    if (!bill.terms.isNullOrBlank()) {
                                        Text("Terms", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Text(bill.terms, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // PDF Document Preview Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Template Selector Bar
                    Text(
                        text = "Switch Bill Template:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(BillTemplateType.entries) { tmpl ->
                            val isSelected = bill.templateId == tmpl.templateId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.updateBillTemplate(bill.id, tmpl.templateId, context)
                                },
                                label = { Text(tmpl.displayName) },
                                modifier = Modifier.testTag("preview_tmpl_${tmpl.templateId}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // In-app rendered PDF bitmap preview container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPdfLoading) {
                            CircularProgressIndicator()
                        } else if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Page Preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Text(
                                text = "Unable to render PDF preview on this device.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Bill?",
            message = "Permanently delete invoice ${bill.billNumber}? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteBill(bill) {
                    onBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
