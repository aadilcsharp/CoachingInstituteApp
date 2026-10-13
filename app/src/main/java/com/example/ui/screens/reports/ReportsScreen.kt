package com.example.ui.screens.reports

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.BillViewModel
import com.example.util.CsvExporter
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.PdfGenerator
import com.example.util.ShareHelper
import java.util.Calendar

@Composable
fun ReportsScreen(
    viewModel: BillViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allBills by viewModel.allBills.collectAsStateWithLifecycle()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val reportYear by viewModel.reportYear.collectAsStateWithLifecycle()
    val reportMonth by viewModel.reportMonth.collectAsStateWithLifecycle()

    val currencySymbol = profile.currencySymbol

    // Compute month boundaries
    val selectedCal = remember(reportYear, reportMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, reportYear)
            set(Calendar.MONTH, reportMonth)
        }
    }
    val startOfMonth = remember(selectedCal) { DateUtils.getStartOfMonth(selectedCal) }
    val endOfMonth = remember(selectedCal) { DateUtils.getEndOfMonth(selectedCal) }
    val periodName = remember(selectedCal) { DateUtils.formatMonthYear(selectedCal.timeInMillis) }

    // Filter bills for selected month
    val monthBills = remember(allBills, startOfMonth, endOfMonth) {
        allBills.filter { it.bill.billDate in startOfMonth..endOfMonth }
    }

    // Monthly aggregates
    val totalBilled = remember(monthBills) { monthBills.sumOf { it.bill.totalAmount } }
    val totalPaid = remember(monthBills) { monthBills.sumOf { it.bill.paidAmount } }
    val totalOutstanding = remember(monthBills) {
        monthBills.sumOf { (it.bill.totalAmount - it.bill.paidAmount).coerceAtLeast(0.0) }
    }
    val billCount = monthBills.size
    val averageBill = if (billCount > 0) totalBilled / billCount else 0.0

    // Category breakdown
    val categoryBreakdown = remember(monthBills) {
        monthBills.groupBy { it.bill.category }
            .mapValues { entry -> entry.value.sumOf { it.bill.totalAmount } }
            .toList()
            .sortedByDescending { it.second }
            .toMap()
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "Monthly Reports")
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
            // Month & Year Selector Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (reportMonth == 0) {
                                    viewModel.reportYear.value = reportYear - 1
                                    viewModel.reportMonth.value = 11
                                } else {
                                    viewModel.reportMonth.value = reportMonth - 1
                                }
                            },
                            modifier = Modifier.testTag("report_prev_month_btn")
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = periodName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$billCount bills found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                if (reportMonth == 11) {
                                    viewModel.reportYear.value = reportYear + 1
                                    viewModel.reportMonth.value = 0
                                } else {
                                    viewModel.reportMonth.value = reportMonth + 1
                                }
                            },
                            modifier = Modifier.testTag("report_next_month_btn")
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }
                }
            }

            if (monthBills.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.BarChart,
                        title = "No Bills for $periodName",
                        description = "There are no bills or expenses recorded for this month. Choose another month or create a bill.",
                        actionButtonText = "Create Bill",
                        onActionClick = { onNavigate(Screen.CreateEditBill()) }
                    )
                }
            } else {
                // Period Metrics Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Monthly Financial Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Billed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyFormatter.format(totalBilled, currencySymbol),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Avg. Bill Value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyFormatter.format(averageBill, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Collected (Paid)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyFormatter.format(totalPaid, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Outstanding (Unpaid)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyFormatter.format(totalOutstanding, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }

                            // Collection Progress Bar
                            val collectionRate = if (totalBilled > 0) (totalPaid / totalBilled).toFloat() else 0f
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Collection Rate", style = MaterialTheme.typography.labelSmall)
                                    Text("${(collectionRate * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { collectionRate.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = Color(0xFF10B981),
                                    trackColor = Color(0xFFFEE2E2)
                                )
                            }
                        }
                    }
                }

                // Category Breakdown Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Category Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            categoryBreakdown.forEach { (cat, amt) ->
                                val pct = if (totalBilled > 0) (amt / totalBilled * 100).toInt() else 0
                                val progress = if (totalBilled > 0) (amt / totalBilled).toFloat() else 0f

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${CurrencyFormatter.format(amt, currencySymbol)} ($pct%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // Export Options Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Export Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Download or share the complete monthly breakdown with itemized bills.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Export as PDF
                                Button(
                                    onClick = {
                                        val pdfFile = PdfGenerator.generateReportPdf(
                                            context = context,
                                            reportTitle = "Monthly Expense & Billing Report",
                                            periodName = periodName,
                                            bills = monthBills,
                                            profile = profile,
                                            totalBilled = totalBilled,
                                            totalPaid = totalPaid,
                                            totalOutstanding = totalOutstanding,
                                            categoryBreakdown = categoryBreakdown
                                        )
                                        ShareHelper.sharePdf(context, pdfFile, "Monthly Report - $periodName")
                                    },
                                    modifier = Modifier.weight(1f).testTag("export_pdf_report_btn")
                                ) {
                                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PDF Report")
                                }

                                // Export as CSV
                                OutlinedButton(
                                    onClick = {
                                        val csvFile = CsvExporter.exportBillsToCsv(
                                            context = context,
                                            fileNamePrefix = "Report_${periodName.replace(" ", "_")}",
                                            bills = monthBills,
                                            profile = profile
                                        )
                                        ShareHelper.shareCsv(context, csvFile, "Billing Data CSV - $periodName")
                                    },
                                    modifier = Modifier.weight(1f).testTag("export_csv_report_btn")
                                ) {
                                    Icon(Icons.Outlined.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CSV Export")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
