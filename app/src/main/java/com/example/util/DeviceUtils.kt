package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

object DeviceUtils {

    /**
     * Gets a stable, unique device identifier.
     * Combines hardware/network MAC address where available with Android ID.
     */
    fun getDeviceId(context: Context): String {
        // 1. Try reading network hardware MAC address
        val mac = getHardwareMacAddress()
        if (mac.isNotBlank() && !mac.equals("02:00:00:00:00:00", ignoreCase = true)) {
            return mac.uppercase(Locale.US)
        }

        // 2. Android ID (guaranteed on all Android devices)
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        if (!androidId.isNullOrBlank()) {
            return androidId.uppercase(Locale.US)
        }

        // 3. Fallback based on build serial/board
        return (Build.BOARD + Build.BRAND + Build.DEVICE).uppercase(Locale.US)
    }

    /**
     * Gets the device's MAC address. If hardware MAC is masked by Android OS (02:00:00:00:00:00),
     * derives a deterministic MAC formatted string from Android ID (e.g. AA:BB:CC:DD:EE:FF).
     */
    fun getMacAddress(context: Context): String {
        val hwMac = getHardwareMacAddress()
        if (hwMac.isNotBlank() && !hwMac.equals("02:00:00:00:00:00", ignoreCase = true)) {
            return hwMac.uppercase(Locale.US)
        }

        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""

        val cleanId = androidId.filter { it in "0123456789abcdefABCDEF" }.padEnd(12, '0').take(12).uppercase(Locale.US)
        return cleanId.chunked(2).joinToString(":")
    }

    /**
     * Attempts to read MAC address from network interfaces (wlan0, eth0, etc.).
     */
    fun getHardwareMacAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback) continue
                val macBytes = intf.hardwareAddress ?: continue
                if (macBytes.isEmpty()) continue

                val res = StringBuilder()
                for (b in macBytes) {
                    res.append(String.format("%02X:", b))
                }
                if (res.isNotEmpty()) {
                    res.deleteCharAt(res.length - 1)
                }
                val macStr = res.toString()
                if (macStr.isNotBlank() && !macStr.equals("02:00:00:00:00:00", ignoreCase = true)) {
                    return macStr
                }
            }
        } catch (_: Exception) {
        }
        return ""
    }

    /**
     * Gets current application version name (e.g. "1.0").
     */
    fun getAppVersion(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }
}
