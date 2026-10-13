package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ShareHelper {

    private fun getFileUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun sharePdf(context: Context, file: File, subject: String = "Invoice PDF") {
        try {
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Here is your invoice from BillCraft.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Invoice PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun cleanPhoneNumber(rawPhone: String): String {
        var digits = rawPhone.replace(Regex("[^0-9]"), "")
        if (digits.length == 10) {
            // Standard 10-digit phone number, prepend default country code 91
            digits = "91$digits"
        }
        return digits
    }

    fun openWhatsAppChat(context: Context, rawPhone: String, message: String): Boolean {
        val cleanPhone = cleanPhoneNumber(rawPhone)
        if (cleanPhone.isBlank()) return false
        val encodedMsg = Uri.encode(message)
        val waUrl = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val bizIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(bizIntent)
                true
            } catch (_: Exception) {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                    true
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }
    }

    fun shareViaWhatsApp(
        context: Context,
        file: File,
        clientName: String,
        billNumber: String,
        clientPhone: String? = null
    ) {
        val uri = getFileUri(context, file)
        val textMessage = "Hello $clientName, please find attached invoice $billNumber. Thank you!"
        val cleanPhone = clientPhone?.let { cleanPhoneNumber(it) }?.takeIf { it.isNotBlank() }

        // If client phone number is added, attempt direct WhatsApp send to that phone number
        if (cleanPhone != null) {
            try {
                val directWaIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, textMessage)
                    putExtra("jid", "$cleanPhone@s.whatsapp.net")
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(directWaIntent)
                return
            } catch (_: Exception) {
                try {
                    val directBizIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, textMessage)
                        putExtra("jid", "$cleanPhone@s.whatsapp.net")
                        setPackage("com.whatsapp.w4b")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(directBizIntent)
                    return
                } catch (_: Exception) {
                    // Try direct URL if JID send failed
                    val opened = openWhatsAppChat(context, cleanPhone, textMessage)
                    if (opened) return
                }
            }
        }

        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, textMessage)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(whatsappIntent)
        } catch (_: Exception) {
            // WhatsApp not installed, try WhatsApp Business or general chooser
            try {
                val whatsappBizIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, textMessage)
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(whatsappBizIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "WhatsApp is not installed. Opening share options...", Toast.LENGTH_SHORT).show()
                val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, textMessage)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(chooserIntent, "Share Invoice"))
            }
        }
    }

    fun shareCsv(context: Context, file: File, title: String = "Expense Report") {
        try {
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Exported billing data from BillCraft.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share CSV Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareBackupJson(context: Context, jsonContent: String, fileName: String = "billcraft_backup.json") {
        try {
            val file = File(context.cacheDir, fileName).apply {
                writeText(jsonContent)
            }
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BillCraft Database Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Backup File"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting backup: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun savePdfToDownloads(context: Context, sourceFile: File, displayName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BillCraft")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return false

                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                Toast.makeText(context, "Saved to Downloads/BillCraft/$displayName", Toast.LENGTH_LONG).show()
                true
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, displayName)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(targetFile).use { out ->
                        input.copyTo(out)
                    }
                }
                Toast.makeText(context, "Saved to Downloads/$displayName", Toast.LENGTH_LONG).show()
                true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not save file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
