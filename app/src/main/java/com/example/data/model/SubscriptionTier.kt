package com.example.data.model

enum class SubscriptionTier(
    val title: String,
    val badgeName: String,
    val maxClients: Int,
    val maxCatalogItems: Int,
    val isLogoAllowed: Boolean,
    val isDigitalSignatureAllowed: Boolean,
    val maxTemplates: Int, // 1 for Classic, 2 for Classic+Modern, 5 for All
    val isBroadcastAllowed: Boolean,
    val isProductQrStickerAllowed: Boolean,
    val isBackupRestoreAllowed: Boolean,
    val isAdvancedReportsAllowed: Boolean,
    val description: String
) {
    FREE(
        title = "Free Plan",
        badgeName = "FREE",
        maxClients = 2,
        maxCatalogItems = 2,
        isLogoAllowed = false,
        isDigitalSignatureAllowed = false,
        maxTemplates = 1,
        isBroadcastAllowed = false,
        isProductQrStickerAllowed = false,
        isBackupRestoreAllowed = false,
        isAdvancedReportsAllowed = false,
        description = "Basic billing functionality: Up to 2 items, 2 clients, no custom logo."
    ),
    BASIC(
        title = "Basic Plan",
        badgeName = "BASIC",
        maxClients = 10,
        maxCatalogItems = 15,
        isLogoAllowed = true,
        isDigitalSignatureAllowed = false,
        maxTemplates = 2,
        isBroadcastAllowed = false,
        isProductQrStickerAllowed = false,
        isBackupRestoreAllowed = false,
        isAdvancedReportsAllowed = false,
        description = "Up to 10 clients, 15 items, business logo on bills & payment QR codes."
    ),
    PREMIUM(
        title = "Premium Plan",
        badgeName = "PREMIUM",
        maxClients = 100,
        maxCatalogItems = 200,
        isLogoAllowed = true,
        isDigitalSignatureAllowed = true,
        maxTemplates = 5,
        isBroadcastAllowed = true,
        isProductQrStickerAllowed = true,
        isBackupRestoreAllowed = false,
        isAdvancedReportsAllowed = true,
        description = "Up to 100 clients, 200 items, all 5 templates, WhatsApp broadcast & product QR sticker printing."
    ),
    ENTERPRISE(
        title = "Enterprise Plan",
        badgeName = "ENTERPRISE",
        maxClients = Int.MAX_VALUE,
        maxCatalogItems = Int.MAX_VALUE,
        isLogoAllowed = true,
        isDigitalSignatureAllowed = true,
        maxTemplates = 5,
        isBroadcastAllowed = true,
        isProductQrStickerAllowed = true,
        isBackupRestoreAllowed = true,
        isAdvancedReportsAllowed = true,
        description = "Unlimited clients & items, full branding, database backup & restore, CSV export & all features."
    );

    fun hasFeature(featureName: String): Boolean {
        return when (featureName) {
            FEATURE_LOGO -> isLogoAllowed
            FEATURE_SIGNATURE -> isDigitalSignatureAllowed
            FEATURE_BROADCAST -> isBroadcastAllowed
            FEATURE_QR_STICKER -> isProductQrStickerAllowed
            FEATURE_BACKUP_RESTORE -> isBackupRestoreAllowed
            FEATURE_ADVANCED_REPORTS -> isAdvancedReportsAllowed
            else -> true
        }
    }

    companion object {
        const val FEATURE_LOGO = "logo"
        const val FEATURE_SIGNATURE = "signature"
        const val FEATURE_BROADCAST = "broadcast"
        const val FEATURE_QR_STICKER = "qr_sticker"
        const val FEATURE_BACKUP_RESTORE = "backup_restore"
        const val FEATURE_ADVANCED_REPORTS = "advanced_reports"

        fun fromString(name: String?): SubscriptionTier {
            if (name == null) return FREE
            return entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } ?: FREE
        }
    }
}

data class LicenseInfo(
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val endDateMillis: Long = 0L,
    val endDateStr: String = "",
    val daysRemaining: Int = 0,
    val isActive: Boolean = false,
    val isExpiringSoon: Boolean = false,
    val licenseKey: String = "",
    val deviceId: String = "",
    val appVersion: String = "",
    val statusMessage: String = "Free Plan (No License Active)"
)
