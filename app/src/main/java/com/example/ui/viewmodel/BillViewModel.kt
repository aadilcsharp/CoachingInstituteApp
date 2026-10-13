package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BillCategory
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BillTemplateType
import com.example.data.model.BillWithItems
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.CatalogItemEntity
import com.example.data.model.ClientEntity
import com.example.data.model.LicenseInfo
import com.example.data.model.PaymentStatus
import com.example.data.model.SubscriptionTier
import com.example.data.repository.BackupRepository
import com.example.data.repository.BillRepository
import com.example.data.repository.BusinessProfileRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.ClientRepository
import com.example.util.CsvExporter
import com.example.util.DateUtils
import com.example.util.ImageOptimizer
import com.example.util.LicenseManager
import com.example.util.PdfGenerator
import com.example.util.PdfViewerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class BillViewModel(
    private val billRepository: BillRepository,
    private val profileRepository: BusinessProfileRepository,
    private val backupRepository: BackupRepository,
    private val catalogRepository: CatalogRepository,
    private val clientRepository: ClientRepository,
    private val context: Context
) : ViewModel() {

    // License & Subscription state
    private val _licenseInfo = MutableStateFlow(LicenseManager.checkAndGetLicenseInfo(context))
    val licenseInfo: StateFlow<LicenseInfo> = _licenseInfo.asStateFlow()

    fun refreshLicenseStatus() {
        _licenseInfo.value = LicenseManager.checkAndGetLicenseInfo(context)
    }

    fun activateLicense(inputKey: String, plan: String, endDate: String): Result<LicenseInfo> {
        val result = LicenseManager.activateLicense(context, inputKey, plan, endDate)
        if (result.isSuccess) {
            _licenseInfo.value = result.getOrThrow()
            _userMessage.value = "License activated successfully! Active Plan: ${_licenseInfo.value.tier.title}."
        }
        return result
    }

    fun deactivateLicense() {
        _licenseInfo.value = LicenseManager.deactivateLicense(context)
        _userMessage.value = "License removed. Switched to Free Plan."
    }

    // Clients Flow & Count
    val allClients: StateFlow<List<ClientEntity>> = clientRepository.allClients
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val clientCount: StateFlow<Int> = clientRepository.clientCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Catalog items
    val allCatalogItems: StateFlow<List<CatalogItemEntity>> = catalogRepository.allCatalogItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Business Profile Flow
    val profileState: StateFlow<BusinessProfileEntity> = profileRepository.profile
        .combine(MutableStateFlow(BusinessProfileEntity())) { saved, default ->
            saved ?: default
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BusinessProfileEntity()
        )

    // Raw bills list from database
    val allBills: StateFlow<List<BillWithItems>> = billRepository.allBillsWithItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter and search state
    val searchQuery = MutableStateFlow("")
    val filterStatus = MutableStateFlow<PaymentStatus?>(null)
    val filterCategory = MutableStateFlow<BillCategory?>(null)
    val filterSortBy = MutableStateFlow("DATE_DESC") // DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC

    // Optional pending item to autofill when navigating to new bill from scan
    var pendingItemForNewBill: CatalogItemEntity? = null

    // Filtered bills
    val filteredBills: StateFlow<List<BillWithItems>> = combine(
        allBills,
        searchQuery,
        filterStatus,
        filterCategory,
        filterSortBy
    ) { bills, query, status, category, sort ->
        var result = bills

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { b ->
                b.bill.billNumber.lowercase().contains(q) ||
                b.bill.clientName.lowercase().contains(q) ||
                b.bill.title.lowercase().contains(q) ||
                b.bill.category.lowercase().contains(q) ||
                (b.bill.notes?.lowercase()?.contains(q) == true)
            }
        }

        if (status != null) {
            result = result.filter { it.bill.paymentStatus.equals(status.name, ignoreCase = true) }
        }

        if (category != null) {
            result = result.filter { it.bill.category.equals(category.name, ignoreCase = true) }
        }

        when (sort) {
            "DATE_ASC" -> result.sortedBy { it.bill.billDate }
            "AMOUNT_DESC" -> result.sortedByDescending { it.bill.totalAmount }
            "AMOUNT_ASC" -> result.sortedBy { it.bill.totalAmount }
            else -> result.sortedByDescending { it.bill.billDate }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selected bill for preview or detail
    private val _selectedBillWithItems = MutableStateFlow<BillWithItems?>(null)
    val selectedBillWithItems: StateFlow<BillWithItems?> = _selectedBillWithItems.asStateFlow()

    // Preview rendered bitmap
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _isPdfGenerating = MutableStateFlow(false)
    val isPdfGenerating: StateFlow<Boolean> = _isPdfGenerating.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Dashboard Time Period filter
    val dashboardPeriod = MutableStateFlow("ALL_TIME") // "THIS_MONTH", "LAST_30_DAYS", "THIS_YEAR", "ALL_TIME"

    // Monthly Report Picker: Year and Month
    val reportYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val reportMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // 0..11

    fun setUserMessage(message: String) {
        _userMessage.value = message
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun selectBill(billWithItems: BillWithItems?) {
        _selectedBillWithItems.value = billWithItems
    }

    fun generateNextBillNumber(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val num = billRepository.generateNextBillNumber()
            onResult(num)
        }
    }

    fun saveBill(
        bill: BillEntity,
        items: List<BillItemEntity>,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Ensure unique billNumber if newly created
                val isTaken = billRepository.isBillNumberTaken(bill.billNumber, bill.id)
                val finalBillNumber = if (isTaken) {
                    val next = billRepository.generateNextBillNumber()
                    _userMessage.value = "Bill # was adjusted to $next to maintain uniqueness."
                    next
                } else {
                    bill.billNumber
                }

                val billToSave = bill.copy(
                    billNumber = finalBillNumber,
                    updatedAt = System.currentTimeMillis()
                )

                val savedId = billRepository.saveBill(billToSave, items)

                // Save client info to directory if not existing
                if (billToSave.clientName.isNotBlank()) {
                    try {
                        clientRepository.saveClientIfNotExists(
                            name = billToSave.clientName,
                            email = billToSave.clientEmail,
                            phone = billToSave.clientPhone,
                            address = billToSave.clientAddress
                        )
                    } catch (_: Exception) {
                        // ignore minor client directory sync errors
                    }
                }

                // Reload selected if editing currently selected
                val updated = billRepository.getBillWithItemsByIdSync(savedId)
                _selectedBillWithItems.value = updated

                _userMessage.value = "Bill saved successfully!"
                onSaved(savedId)
            } catch (e: Exception) {
                _userMessage.value = "Failed to save bill: ${e.localizedMessage}"
            }
        }
    }

    fun deleteBill(bill: BillEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                billRepository.deleteBill(bill)
                if (_selectedBillWithItems.value?.bill?.id == bill.id) {
                    _selectedBillWithItems.value = null
                    _previewBitmap.value = null
                }
                _userMessage.value = "Bill deleted."
                onDeleted()
            } catch (e: Exception) {
                _userMessage.value = "Error deleting bill: ${e.localizedMessage}"
            }
        }
    }

    fun updatePaymentStatus(bill: BillEntity, status: PaymentStatus, paidAmount: Double? = null) {
        viewModelScope.launch {
            try {
                val newPaid = when (status) {
                    PaymentStatus.PAID -> bill.totalAmount
                    PaymentStatus.UNPAID -> 0.0
                    PaymentStatus.PARTIAL -> paidAmount ?: (bill.totalAmount / 2.0)
                }
                val updated = bill.copy(
                    paymentStatus = status.name,
                    paidAmount = newPaid,
                    updatedAt = System.currentTimeMillis()
                )
                billRepository.updateBill(updated)
                // Refresh active
                val reloaded = billRepository.getBillWithItemsByIdSync(bill.id)
                _selectedBillWithItems.value = reloaded
                _userMessage.value = "Payment status updated to ${status.displayName}."
            } catch (e: Exception) {
                _userMessage.value = "Error updating payment: ${e.localizedMessage}"
            }
        }
    }

    fun updateBillTemplate(billId: Long, templateId: String, context: Context) {
        viewModelScope.launch {
            val current = _selectedBillWithItems.value ?: billRepository.getBillWithItemsByIdSync(billId)
            if (current != null) {
                val updatedBill = current.bill.copy(templateId = templateId, updatedAt = System.currentTimeMillis())
                billRepository.updateBill(updatedBill)
                val refreshed = current.copy(bill = updatedBill)
                _selectedBillWithItems.value = refreshed
                renderBillPreview(context, refreshed)
            }
        }
    }

    fun duplicateBill(original: BillWithItems, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val newNumber = billRepository.generateNextBillNumber()
                val duplicatedBill = original.bill.copy(
                    id = 0,
                    billNumber = newNumber,
                    title = if (original.bill.title.isNotEmpty()) "${original.bill.title} (Copy)" else "",
                    billDate = System.currentTimeMillis(),
                    dueDate = original.bill.dueDate?.let { System.currentTimeMillis() + (14L * 86400000L) },
                    paymentStatus = PaymentStatus.UNPAID.name,
                    paidAmount = 0.0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val duplicatedItems = original.items.map { it.copy(id = 0, billId = 0) }
                val newId = billRepository.saveBill(duplicatedBill, duplicatedItems)
                _userMessage.value = "Duplicated as $newNumber"
                val reloaded = billRepository.getBillWithItemsByIdSync(newId)
                _selectedBillWithItems.value = reloaded
                onCreated(newId)
            } catch (e: Exception) {
                _userMessage.value = "Error duplicating bill: ${e.localizedMessage}"
            }
        }
    }

    fun renderBillPreview(context: Context, billWithItems: BillWithItems) {
        viewModelScope.launch(Dispatchers.IO) {
            _isPdfGenerating.value = true
            try {
                val profile = profileRepository.getProfileSync()
                val pdfFile = PdfGenerator.generateBillPdf(context, billWithItems, profile)
                val bitmap = PdfViewerHelper.renderPdfFirstPage(pdfFile)
                _previewBitmap.value = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isPdfGenerating.value = false
            }
        }
    }

    fun getBillPdfFile(context: Context, billWithItems: BillWithItems): File {
        val profile = profileState.value
        return PdfGenerator.generateBillPdf(context, billWithItems, profile)
    }

    fun updateProfile(profile: BusinessProfileEntity) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile)
            _userMessage.value = "Business profile updated."
        }
    }

    fun saveCatalogItem(item: CatalogItemEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val itemToSave = if (item.qrCode.isBlank()) {
                    item.copy(qrCode = CatalogItemEntity.generateNewQrCode())
                } else item
                val id = catalogRepository.saveCatalogItem(itemToSave)
                _userMessage.value = "Catalog item saved."
                onSaved(id)
            } catch (e: Exception) {
                _userMessage.value = "Error saving catalog item: ${e.localizedMessage}"
            }
        }
    }

    suspend fun resolveItemFromQr(scannedText: String): CatalogItemEntity? {
        val trimmed = scannedText.trim()
        val currentItems = allCatalogItems.value

        // 1. Try finding in in-memory catalog by exact QR code or payload
        val exactMatch = currentItems.firstOrNull {
            it.qrCode.equals(trimmed, ignoreCase = true) ||
            it.getEffectiveQrCode().equals(trimmed, ignoreCase = true) ||
            it.getQrPayload() == trimmed
        }
        if (exactMatch != null) return exactMatch

        // 2. Try database lookup by QR code
        val dbMatch = catalogRepository.getItemByQrCode(trimmed)
        if (dbMatch != null) return dbMatch

        // 3. Try parsing structured payload
        val parsed = CatalogItemEntity.parseFromQrPayload(trimmed)
        if (parsed != null) {
            val match = currentItems.firstOrNull {
                it.qrCode.equals(parsed.qrCode, ignoreCase = true) ||
                it.name.equals(parsed.name, ignoreCase = true)
            }
            return match ?: parsed
        }

        // 4. Try matching by ID if number
        val idLong = trimmed.toLongOrNull()
        if (idLong != null) {
            val matchById = currentItems.firstOrNull { it.id == idLong }
            if (matchById != null) return matchById
        }

        // 5. Try matching by name
        return currentItems.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
    }

    fun deleteCatalogItem(item: CatalogItemEntity) {
        viewModelScope.launch {
            try {
                catalogRepository.deleteCatalogItem(item)
                _userMessage.value = "Catalog item removed."
            } catch (e: Exception) {
                _userMessage.value = "Error deleting catalog item: ${e.localizedMessage}"
            }
        }
    }

    fun saveClient(client: ClientEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = clientRepository.saveClient(client)
                _userMessage.value = "Client saved to directory."
                onSaved(id)
            } catch (e: Exception) {
                _userMessage.value = "Error saving client: ${e.localizedMessage}"
            }
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch {
            try {
                clientRepository.deleteClient(client)
                _userMessage.value = "Client removed from directory."
            } catch (e: Exception) {
                _userMessage.value = "Error deleting client: ${e.localizedMessage}"
            }
        }
    }

    fun optimizeAndSaveItemImage(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val path = ImageOptimizer.optimizeAndSaveImage(context, uri)
            onResult(path)
        }
    }

    fun optimizeAndSaveItemBitmap(context: Context, bitmap: android.graphics.Bitmap, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val path = ImageOptimizer.optimizeAndSaveBitmap(context, bitmap)
            onResult(path)
        }
    }

    fun exportDatabaseBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupRepository.exportToJson()
                onReady(json)
            } catch (e: Exception) {
                _userMessage.value = "Backup export failed: ${e.localizedMessage}"
            }
        }
    }

    fun importDatabaseBackup(jsonString: String) {
        viewModelScope.launch {
            val result = backupRepository.importFromJson(jsonString)
            result.onSuccess { count ->
                _userMessage.value = "Successfully restored $count bills!"
            }.onFailure { err ->
                _userMessage.value = "Failed to restore backup: ${err.localizedMessage}"
            }
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            try {
                val profile = profileRepository.getProfileSync()
                val calendar = Calendar.getInstance()

                // Bill 1: Web Design & SEO (Paid)
                calendar.add(Calendar.DAY_OF_YEAR, -5)
                val b1 = BillEntity(
                    billNumber = "INV-2026-1001",
                    title = "E-Commerce Website & SEO Optimization",
                    billDate = calendar.timeInMillis,
                    dueDate = calendar.timeInMillis + (14L * 86400000L),
                    clientName = "Nexus Retailers Ltd",
                    clientEmail = "contact@nexusretail.com",
                    clientPhone = "+1 (555) 321-7788",
                    clientAddress = "742 Evergreen Terrace, Springfield, OR",
                    subtotal = 3200.0,
                    taxPercent = 10.0,
                    taxAmount = 320.0,
                    discountPercent = 5.0,
                    discountAmount = 160.0,
                    totalAmount = 3360.0,
                    paidAmount = 3360.0,
                    paymentStatus = PaymentStatus.PAID.name,
                    category = BillCategory.SERVICES.name,
                    templateId = BillTemplateType.MODERN_CLEAN.templateId,
                    notes = "Includes 3 months of technical maintenance and hosting support.",
                    paymentMethod = "Bank Wire"
                )
                val items1 = listOf(
                    BillItemEntity(billId = 0, name = "Custom Storefront UI Design", description = "Figma responsive mockups & components", quantity = 1.0, unitPrice = 1200.0, unit = "package", amount = 1200.0),
                    BillItemEntity(billId = 0, name = "Frontend React/Compose Implementation", description = "Responsive UI and checkout flow", quantity = 40.0, unitPrice = 40.0, unit = "hrs", amount = 1600.0),
                    BillItemEntity(billId = 0, name = "SEO Audit & Schema Setup", description = "Search engine markup and sitemaps", quantity = 1.0, unitPrice = 400.0, unit = "audit", amount = 400.0)
                )

                // Bill 2: Cloud Infrastructure & DevOps (Partially Paid)
                calendar.add(Calendar.DAY_OF_YEAR, -10)
                val b2 = BillEntity(
                    billNumber = "INV-2026-1002",
                    title = "Cloud Infrastructure Migration",
                    billDate = calendar.timeInMillis,
                    dueDate = calendar.timeInMillis + (7L * 86400000L),
                    clientName = "Acrobatic Technologies",
                    clientEmail = "finance@acrobatic.io",
                    clientPhone = "+1 (555) 890-1234",
                    clientAddress = "101 Silicon Valley Blvd, San Jose, CA",
                    subtotal = 4500.0,
                    taxPercent = 8.0,
                    taxAmount = 360.0,
                    discountPercent = 0.0,
                    discountAmount = 0.0,
                    totalAmount = 4860.0,
                    paidAmount = 2430.0,
                    paymentStatus = PaymentStatus.PARTIAL.name,
                    category = BillCategory.CONSULTING.name,
                    templateId = BillTemplateType.EXECUTIVE_NAVY.templateId,
                    notes = "First milestone completed. Remaining 50% due upon load testing approval.",
                    paymentMethod = "ACH Transfer"
                )
                val items2 = listOf(
                    BillItemEntity(billId = 0, name = "Kubernetes Cluster Architecture", description = "Multi-zone deployment setup", quantity = 1.0, unitPrice = 2500.0, unit = "cluster", amount = 2500.0),
                    BillItemEntity(billId = 0, name = "CI/CD Pipeline Automation", description = "GitHub Actions with automated testing", quantity = 1.0, unitPrice = 1500.0, unit = "pipeline", amount = 1500.0),
                    BillItemEntity(billId = 0, name = "Security Audit & Key Management", description = "Secrets rotation and firewall policies", quantity = 1.0, unitPrice = 500.0, unit = "review", amount = 500.0)
                )

                // Bill 3: Office Ergonomic Hardware Supply (Unpaid)
                calendar.add(Calendar.DAY_OF_YEAR, -2)
                val b3 = BillEntity(
                    billNumber = "INV-2026-1003",
                    title = "Workstation Hardware & Peripherals",
                    billDate = calendar.timeInMillis,
                    dueDate = calendar.timeInMillis + (30L * 86400000L),
                    clientName = "Vertex Creative Studio",
                    clientEmail = "accounts@vertexstudio.com",
                    clientPhone = "+1 (555) 445-9900",
                    clientAddress = "500 Art District Ave, Austin, TX",
                    subtotal = 1850.0,
                    taxPercent = 5.0,
                    taxAmount = 92.5,
                    discountPercent = 0.0,
                    discountAmount = 0.0,
                    totalAmount = 1942.5,
                    paidAmount = 0.0,
                    paymentStatus = PaymentStatus.UNPAID.name,
                    category = BillCategory.PRODUCTS.name,
                    templateId = BillTemplateType.EMERALD_CORP.templateId,
                    notes = "Net 30 payment terms.",
                    paymentMethod = "Credit Card / Invoice"
                )
                val items3 = listOf(
                    BillItemEntity(billId = 0, name = "Ultra-wide 34\" Color-Calibrated Displays", description = "4K HDR IPS Panels", quantity = 2.0, unitPrice = 650.0, unit = "pcs", amount = 1300.0),
                    BillItemEntity(billId = 0, name = "Ergonomic Mechanical Keyboards", description = "Wireless multi-device layout", quantity = 3.0, unitPrice = 150.0, unit = "pcs", amount = 450.0),
                    BillItemEntity(billId = 0, name = "Cable Management & Desk Grommets", description = "Heavy duty trays", quantity = 2.0, unitPrice = 50.0, unit = "sets", amount = 100.0)
                )

                // Bill 4: Monthly Maintenance Retainer (Paid)
                calendar.add(Calendar.DAY_OF_YEAR, -18)
                val b4 = BillEntity(
                    billNumber = "INV-2026-1004",
                    title = "Monthly Server Maintenance Retainer",
                    billDate = calendar.timeInMillis,
                    dueDate = calendar.timeInMillis + (5L * 86400000L),
                    clientName = "BlueHorizon Logistics",
                    clientEmail = "dispatch@bluehorizon.net",
                    clientPhone = "+1 (555) 678-4321",
                    clientAddress = "12 Harbor Way, Seattle, WA",
                    subtotal = 850.0,
                    taxPercent = 0.0,
                    taxAmount = 0.0,
                    discountPercent = 0.0,
                    discountAmount = 0.0,
                    totalAmount = 850.0,
                    paidAmount = 850.0,
                    paymentStatus = PaymentStatus.PAID.name,
                    category = BillCategory.MAINTENANCE.name,
                    templateId = BillTemplateType.MINIMALIST_SLATE.templateId,
                    notes = "Monthly SLA Guarantee: 99.9% uptime with 24/7 incident monitoring.",
                    paymentMethod = "PayPal / UPI"
                )
                val items4 = listOf(
                    BillItemEntity(billId = 0, name = "24/7 Server Monitoring & Health Checks", description = "Automated alerts & backups", quantity = 1.0, unitPrice = 500.0, unit = "month", amount = 500.0),
                    BillItemEntity(billId = 0, name = "Software & OS Security Patching", description = "Kernel updates & log audit", quantity = 1.0, unitPrice = 350.0, unit = "package", amount = 350.0)
                )

                billRepository.saveBill(b1, items1)
                billRepository.saveBill(b2, items2)
                billRepository.saveBill(b3, items3)
                billRepository.saveBill(b4, items4)

                _userMessage.value = "Demo invoices loaded with multiple templates!"
            } catch (e: Exception) {
                _userMessage.value = "Failed to load sample data: ${e.localizedMessage}"
            }
        }
    }
}
