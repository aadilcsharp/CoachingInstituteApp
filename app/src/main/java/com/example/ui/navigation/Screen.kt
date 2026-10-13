package com.example.ui.navigation

sealed class Screen(val route: String) {
    // Bottom Nav Tabs
    data object Dashboard : Screen("dashboard")
    data object Bills : Screen("bills")
    data object Reports : Screen("reports")
    data object Settings : Screen("settings")

    // Sub screens
    data class CreateEditBill(val billId: Long? = null) : Screen("create_edit_bill")
    data class BillDetail(val billId: Long) : Screen("bill_detail")
    data class PdfPreview(val billId: Long) : Screen("pdf_preview")
    data object BusinessProfile : Screen("business_profile")
    data object TemplateManager : Screen("template_manager")
    data object BackupRestore : Screen("backup_restore")
    data object ManageItems : Screen("manage_items")
    data object ManageClients : Screen("manage_clients")
    data object WhatsAppBroadcast : Screen("whatsapp_broadcast")
    data object SubscriptionLicense : Screen("subscription_license")
}
