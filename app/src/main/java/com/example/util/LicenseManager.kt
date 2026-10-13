package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.LicenseInfo
import com.example.data.model.SubscriptionTier
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object LicenseManager {

    private const val PREFS_NAME = "billcraft_license_prefs"
    private const val KEY_LICENSE_KEY = "license_key"
    private const val KEY_LICENSE_TIER = "license_tier"
    private const val KEY_LICENSE_END_DATE_STR = "license_end_date_str"
    private const val KEY_LICENSE_END_DATE_MILLIS = "license_end_date_millis"
    private const val KEY_LICENSE_DEVICE_ID = "license_device_id"
    private const val KEY_LICENSE_APP_VERSION = "license_app_version"
    private const val KEY_LAST_NOTIFIED_DAY = "last_notified_day"

    private const val CHANNEL_ID = "billcraft_license_alerts"
    private const val NOTIFICATION_ID = 2001

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Computes standard SHA-256 hex string.
     */
    fun sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Canonical string formula used to generate key offline.
     * Formula: DEVICE_ID:END_DATE:PLAN:APP_VERSION
     */
    fun getCanonicalKeyString(
        deviceId: String,
        endDate: String,
        subscriptionTier: String,
        appVersion: String
    ): String {
        return "${deviceId.trim()}:${endDate.trim()}:${subscriptionTier.trim().uppercase()}:${appVersion.trim()}"
    }

    /**
     * Computes the canonical SHA-256 key for given parameters.
     */
    fun generateCanonicalSha256(
        deviceId: String,
        endDate: String,
        subscriptionTier: String,
        appVersion: String
    ): String {
        val input = getCanonicalKeyString(deviceId, endDate, subscriptionTier, appVersion)
        return sha256(input)
    }

    /**
     * Validates an offline SHA-256 key against device id / MAC address, end date, subscription, and version.
     * Checks direct concatenation (mac+endDate+tier+version) as well as delimiters (: , - , space).
     */
    fun verifyKey(
        inputKeyHex: String,
        deviceId: String,
        endDate: String,
        subscriptionTier: String,
        appVersion: String
    ): Boolean {
        val cleanKey = inputKeyHex.trim().lowercase(Locale.ROOT)
        if (cleanKey.length != 64) return false

        val d = deviceId.trim()
        val e = endDate.trim()
        val s = subscriptionTier.trim().uppercase()
        val v = appVersion.trim()

        val rawMacNoColons = d.replace(":", "")

        val candidates = mutableListOf(
            // 1. Direct concatenation (MAC + EndDate + Tier + Version)
            "$d$e$s$v",
            "$d:$e:$s:$v",
            "$d-$e-$s-$v",
            "$d|$e|$s|$v",
            "$d $e $s $v",
            // 2. MAC without colons
            "$rawMacNoColons$e$s$v",
            "$rawMacNoColons:$e:$s:$v",
            // 3. Lowercase variations
            "${d.lowercase(Locale.ROOT)}$e$s$v",
            "$d$e${s.lowercase(Locale.ROOT)}$v",
            "${d.lowercase(Locale.ROOT)}$e${s.lowercase(Locale.ROOT)}$v",
            "${d.lowercase(Locale.ROOT)}:$e:${s.lowercase(Locale.ROOT)}:$v",
            "${rawMacNoColons.lowercase(Locale.ROOT)}$e${s.lowercase(Locale.ROOT)}$v",
            // 4. Date without hyphens
            "$d${e.replace("-", "")}$s$v",
            "$d:${e.replace("-", "")}:$s:$v"
        )

        return candidates.any { candidate ->
            sha256(candidate).equals(cleanKey, ignoreCase = true)
        }
    }

    /**
     * Parses date string (YYYY-MM-DD) into end-of-day epoch millis.
     */
    fun parseEndDateMillis(dateStr: String): Long? {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("yyyyMMdd", Locale.US)
        )
        for (sdf in formats) {
            try {
                val date = sdf.parse(dateStr.trim())
                if (date != null) {
                    val cal = Calendar.getInstance().apply {
                        time = date
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    return cal.timeInMillis
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    /**
     * Attempts to activate a license entered by the user.
     * Returns Result.success(LicenseInfo) on valid key, or Result.failure with descriptive reason.
     */
    fun activateLicense(
        context: Context,
        inputKeyHex: String,
        planStr: String,
        endDateStr: String
    ): Result<LicenseInfo> {
        val cleanKey = inputKeyHex.trim()
        if (cleanKey.length != 64 || !cleanKey.all { it in "0123456789abcdefABCDEF" }) {
            return Result.failure(IllegalArgumentException("License key must be exactly 64 hexadecimal characters (SHA-256)."))
        }

        val tier = SubscriptionTier.fromString(planStr)
        if (tier == SubscriptionTier.FREE) {
            return Result.failure(IllegalArgumentException("Please select a valid paid tier (BASIC, PREMIUM, or ENTERPRISE)."))
        }

        val endDateMillis = parseEndDateMillis(endDateStr)
            ?: return Result.failure(IllegalArgumentException("Invalid date format. Please use YYYY-MM-DD."))

        val currentMillis = System.currentTimeMillis()
        if (currentMillis > endDateMillis) {
            return Result.failure(IllegalArgumentException("End date cannot be in the past. License is already expired."))
        }

        val currentDeviceId = DeviceUtils.getDeviceId(context)
        val currentMac = DeviceUtils.getMacAddress(context)
        val currentAppVersion = DeviceUtils.getAppVersion(context)

        // Test with Device ID or MAC Address
        val matchesDeviceId = verifyKey(
            inputKeyHex = cleanKey,
            deviceId = currentDeviceId,
            endDate = endDateStr.trim(),
            subscriptionTier = tier.name,
            appVersion = currentAppVersion
        )

        val matchesMac = !matchesDeviceId && verifyKey(
            inputKeyHex = cleanKey,
            deviceId = currentMac,
            endDate = endDateStr.trim(),
            subscriptionTier = tier.name,
            appVersion = currentAppVersion
        )

        val activeMatchedId = when {
            matchesDeviceId -> currentDeviceId
            matchesMac -> currentMac
            else -> null
        }

        if (activeMatchedId == null) {
            return Result.failure(
                IllegalArgumentException(
                    "SHA-256 hash mismatch! Ensure the key was generated offline using:\n" +
                            "MAC Address: $currentMac\n" +
                            "(or Device ID: $currentDeviceId)\n" +
                            "End Date: $endDateStr\n" +
                            "Plan: ${tier.name}\n" +
                            "Version: $currentAppVersion\n\n" +
                            "Formula: SHA256(MAC+EndDate+Plan+Version)"
                )
            )
        }

        // Save valid license
        getPrefs(context).edit().apply {
            putString(KEY_LICENSE_KEY, cleanKey.lowercase(Locale.ROOT))
            putString(KEY_LICENSE_TIER, tier.name)
            putString(KEY_LICENSE_END_DATE_STR, endDateStr.trim())
            putLong(KEY_LICENSE_END_DATE_MILLIS, endDateMillis)
            putString(KEY_LICENSE_DEVICE_ID, activeMatchedId)
            putString(KEY_LICENSE_APP_VERSION, currentAppVersion)
            apply()
        }

        val info = checkAndGetLicenseInfo(context)
        return Result.success(info)
    }

    /**
     * Removes the currently stored license and reverts to FREE tier.
     */
    fun deactivateLicense(context: Context): LicenseInfo {
        getPrefs(context).edit().clear().apply()
        return checkAndGetLicenseInfo(context)
    }

    /**
     * Checks stored license on app launch:
     * - Compares device id / MAC address
     * - Compares app version
     * - Checks today date <= endDate
     * - Verifies SHA-256 integrity
     * - Sends notification if <= 5 days remaining
     */
    fun checkAndGetLicenseInfo(context: Context): LicenseInfo {
        val prefs = getPrefs(context)
        val storedKey = prefs.getString(KEY_LICENSE_KEY, "") ?: ""
        val storedTierStr = prefs.getString(KEY_LICENSE_TIER, "") ?: ""
        val storedEndDateStr = prefs.getString(KEY_LICENSE_END_DATE_STR, "") ?: ""
        val storedEndDateMillis = prefs.getLong(KEY_LICENSE_END_DATE_MILLIS, 0L)
        val storedDeviceId = prefs.getString(KEY_LICENSE_DEVICE_ID, "") ?: ""
        val storedAppVersion = prefs.getString(KEY_LICENSE_APP_VERSION, "") ?: ""

        val currentDeviceId = DeviceUtils.getDeviceId(context)
        val currentMac = DeviceUtils.getMacAddress(context)
        val currentAppVersion = DeviceUtils.getAppVersion(context)

        if (storedKey.isBlank() || storedTierStr.isBlank() || storedEndDateMillis <= 0L) {
            return LicenseInfo(
                tier = SubscriptionTier.FREE,
                statusMessage = "Free Plan: Limited to 2 items & 2 clients. No business logo."
            )
        }

        val now = System.currentTimeMillis()
        val daysRemaining = if (storedEndDateMillis > now) {
            val diff = storedEndDateMillis - now
            (diff / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0) + 1
        } else 0

        // 1. Device ID / MAC address match check
        val matchesDevice = storedDeviceId.equals(currentDeviceId, ignoreCase = true) ||
                storedDeviceId.equals(currentMac, ignoreCase = true)
        if (!matchesDevice) {
            return LicenseInfo(
                tier = SubscriptionTier.FREE,
                statusMessage = "License Inactive: Device ID / MAC mismatch."
            )
        }

        // 2. App Version check
        if (!storedAppVersion.equals(currentAppVersion, ignoreCase = true)) {
            return LicenseInfo(
                tier = SubscriptionTier.FREE,
                statusMessage = "License Inactive: App version mismatch ($storedAppVersion vs $currentAppVersion)."
            )
        }

        // 3. Expiration check (today date <= endDate)
        if (now > storedEndDateMillis) {
            return LicenseInfo(
                tier = SubscriptionTier.FREE,
                endDateMillis = storedEndDateMillis,
                endDateStr = storedEndDateStr,
                statusMessage = "License Expired on $storedEndDateStr. Reverted to Free Plan."
            )
        }

        // 4. SHA-256 Integrity check
        val isHashValid = verifyKey(
            inputKeyHex = storedKey,
            deviceId = storedDeviceId,
            endDate = storedEndDateStr,
            subscriptionTier = storedTierStr,
            appVersion = currentAppVersion
        )

        if (!isHashValid) {
            return LicenseInfo(
                tier = SubscriptionTier.FREE,
                statusMessage = "License Inactive: Corrupted or tampered key."
            )
        }

        val activeTier = SubscriptionTier.fromString(storedTierStr)
        val isExpiringSoon = daysRemaining in 1..5

        // If <= 5 days left, notify user
        if (isExpiringSoon) {
            maybeSendExpiryNotification(context, activeTier, daysRemaining, storedEndDateStr)
        }

        return LicenseInfo(
            tier = activeTier,
            endDateMillis = storedEndDateMillis,
            endDateStr = storedEndDateStr,
            daysRemaining = daysRemaining,
            isActive = true,
            isExpiringSoon = isExpiringSoon,
            licenseKey = storedKey,
            deviceId = currentDeviceId,
            appVersion = currentAppVersion,
            statusMessage = if (isExpiringSoon) {
                "⚠️ Expiring in $daysRemaining day(s) on $storedEndDateStr! Please renew."
            } else {
                "Active until $storedEndDateStr ($daysRemaining days remaining)"
            }
        )
    }

    /**
     * Sends system notification if subscription has 5 or fewer days left.
     * Throttles to once per calendar day.
     */
    private fun maybeSendExpiryNotification(
        context: Context,
        tier: SubscriptionTier,
        daysLeft: Int,
        endDateStr: String
    ) {
        val prefs = getPrefs(context)
        val todayDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastNotified = prefs.getInt(KEY_LAST_NOTIFIED_DAY, -1)

        if (todayDayOfYear == lastNotified) {
            return // Already notified today
        }

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Subscription & License Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifies when subscription licenses are about to expire"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Subscription Expiring Soon ($daysLeft days left)")
                .setContentText("Your ${tier.title} expires on $endDateStr. Enter your new offline license key to renew.")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "Your ${tier.title} subscription expires on $endDateStr ($daysLeft days remaining).\n" +
                                "To avoid service interruption and limit resets, please generate and enter your new offline license key."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
            prefs.edit().putInt(KEY_LAST_NOTIFIED_DAY, todayDayOfYear).apply()
        } catch (_: Exception) {
        }
    }
}
