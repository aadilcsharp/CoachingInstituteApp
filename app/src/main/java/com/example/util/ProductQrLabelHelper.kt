package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.example.data.model.CatalogItemEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ProductQrLabelHelper {

    fun generateProductLabelBitmap(
        context: Context,
        item: CatalogItemEntity,
        currencySymbol: String,
        businessName: String = "BillCraft",
        qrOnly: Boolean = false
    ): Bitmap {
        if (qrOnly) {
            return generateQrOnlyBitmap(item, currencySymbol, businessName)
        }

        val hasImage = !item.imagePath.isNullOrBlank() && File(item.imagePath).exists()
        val width = 500
        val height = if (hasImage) 730 else 660
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. White Background
        canvas.drawColor(Color.WHITE)

        // Outer border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val cardRect = RectF(8f, 8f, width - 8f, height - 8f)
        canvas.drawRoundRect(cardRect, 18f, 18f, borderPaint)

        // 2. Header banner (Navy Indigo)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            style = Paint.Style.FILL
        }
        val headerRect = RectF(8f, 8f, width - 8f, 84f)
        canvas.drawRoundRect(headerRect, 18f, 18f, headerPaint)
        // Square off bottom corners of banner
        canvas.drawRect(8f, 50f, width - 8f, 84f, headerPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(businessName.ifEmpty { "BillCraft" }.uppercase(), width / 2f, 44f, headerTextPaint)

        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 231, 255)
            textSize = 12f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PRODUCT QR CODE & DETAILS", width / 2f, 70f, subHeaderPaint)

        // 3. High-Resolution QR Code
        val qrPayload = item.getQrPayload()
        val qrBitmap = QrCodeGenerator.generateQrBitmap(qrPayload, 300)
        val qrSize = 230f
        val qrX = (width - qrSize) / 2f
        val qrY = 100f

        if (qrBitmap != null) {
            val qrCardRect = RectF(qrX - 8f, qrY - 8f, qrX + qrSize + 8f, qrY + qrSize + 8f)
            val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(248, 250, 252)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(qrCardRect, 14f, 14f, qrBgPaint)
            val qrBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(qrCardRect, 14f, 14f, qrBorderPaint)

            canvas.drawBitmap(qrBitmap, null, RectF(qrX, qrY, qrX + qrSize, qrY + qrSize), Paint(Paint.FILTER_BITMAP_FLAG))
        }

        // 4. Scan ID Pill Badge
        val pillY = qrY + qrSize + 18f
        val pillText = "SCAN ID: ${item.getEffectiveQrCode()}"
        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pillWidth = pillTextPaint.measureText(pillText) + 32f
        val pillRect = RectF((width - pillWidth) / 2f, pillY, (width + pillWidth) / 2f, pillY + 28f)
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(238, 242, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, 14f, 14f, pillBgPaint)
        canvas.drawText(pillText, width / 2f, pillY + 19f, pillTextPaint)

        // 5. Product Image & Text Below QR Code
        val contentStartY = pillY + 44f

        if (hasImage) {
            // Draw thumbnail on left and details on right
            val thumbSize = 84f
            val thumbX = 36f
            val thumbY = contentStartY
            val thumbRect = RectF(thumbX, thumbY, thumbX + thumbSize, thumbY + thumbSize)

            val thumb = ImageOptimizer.loadSampledBitmap(item.imagePath!!, 200)
            if (thumb != null) {
                val thumbBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(203, 213, 225)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                }
                canvas.drawRoundRect(thumbRect, 12f, 12f, thumbBorderPaint)
                canvas.drawBitmap(thumb, null, thumbRect, Paint(Paint.FILTER_BITMAP_FLAG))
                thumb.recycle()
            }

            // Right-side text
            val textX = thumbX + thumbSize + 18f
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val safeName = if (item.name.length > 22) item.name.take(20) + "..." else item.name
            canvas.drawText(safeName, textX, contentStartY + 24f, namePaint)

            val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(16, 185, 129)
                textSize = 21f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val formattedPrice = CurrencyFormatter.format(item.defaultUnitPrice, currencySymbol)
            val unitText = if (!item.unit.isNullOrBlank()) " / ${item.unit}" else ""
            canvas.drawText("$formattedPrice$unitText", textX, contentStartY + 52f, pricePaint)

            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 13f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.LEFT
            }
            val catText = item.category ?: "General"
            canvas.drawText("Category: $catText", textX, contentStartY + 76f, metaPaint)
        } else {
            // Centered layout
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val safeName = if (item.name.length > 28) item.name.take(26) + "..." else item.name
            canvas.drawText(safeName, width / 2f, contentStartY + 14f, namePaint)

            val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(16, 185, 129)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val formattedPrice = CurrencyFormatter.format(item.defaultUnitPrice, currencySymbol)
            val unitText = if (!item.unit.isNullOrBlank()) " / ${item.unit}" else ""
            canvas.drawText("$formattedPrice$unitText", width / 2f, contentStartY + 44f, pricePaint)

            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 14f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }
            val catText = item.category ?: "General"
            canvas.drawText("Category: $catText", width / 2f, contentStartY + 70f, metaPaint)
        }

        // 6. Footer Divider and instructions
        val divY = height - 56f
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            strokeWidth = 1.5f
        }
        canvas.drawLine(24f, divY, width - 24f, divY, divPaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 11.5f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Scan with BillCraft to automatically load item details", width / 2f, height - 26f, footerPaint)

        return bitmap
    }

    private fun generateQrOnlyBitmap(
        item: CatalogItemEntity,
        currencySymbol: String,
        businessName: String
    ): Bitmap {
        val width = 460
        val height = 560
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(8f, 8f, width - 8f, height - 8f), 18f, 18f, borderPaint)

        // Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            style = Paint.Style.FILL
        }
        val headerRect = RectF(8f, 8f, width - 8f, 74f)
        canvas.drawRoundRect(headerRect, 18f, 18f, headerPaint)
        canvas.drawRect(8f, 44f, width - 8f, 74f, headerPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(businessName.ifEmpty { "BillCraft" }.uppercase(), width / 2f, 44f, headerTextPaint)

        // Large QR Code
        val qrPayload = item.getQrPayload()
        val qrBitmap = QrCodeGenerator.generateQrBitmap(qrPayload, 320)
        val qrSize = 260f
        val qrX = (width - qrSize) / 2f
        val qrY = 94f

        if (qrBitmap != null) {
            val qrCardRect = RectF(qrX - 6f, qrY - 6f, qrX + qrSize + 6f, qrY + qrSize + 6f)
            val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(248, 250, 252)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(qrCardRect, 12f, 12f, qrBgPaint)
            canvas.drawBitmap(qrBitmap, null, RectF(qrX, qrY, qrX + qrSize, qrY + qrSize), Paint(Paint.FILTER_BITMAP_FLAG))
        }

        // Product Details Text below QR
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val safeName = if (item.name.length > 28) item.name.take(26) + "..." else item.name
        canvas.drawText(safeName, width / 2f, qrY + qrSize + 36f, namePaint)

        val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(16, 185, 129)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val formattedPrice = CurrencyFormatter.format(item.defaultUnitPrice, currencySymbol)
        val unitText = if (!item.unit.isNullOrBlank()) " / ${item.unit}" else ""
        canvas.drawText("$formattedPrice$unitText  •  ID: ${item.getEffectiveQrCode()}", width / 2f, qrY + qrSize + 64f, pricePaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 11.5f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Scan to get item details automatically", width / 2f, height - 24f, footerPaint)

        return bitmap
    }

    fun saveProductLabelToDownloads(
        context: Context,
        item: CatalogItemEntity,
        currencySymbol: String,
        businessName: String = "BillCraft",
        qrOnly: Boolean = false
    ): Boolean {
        return try {
            val bitmap = generateProductLabelBitmap(context, item, currencySymbol, businessName, qrOnly)
            val prefix = if (qrOnly) "QR" else "Label"
            val fileName = "${prefix}_${item.getEffectiveQrCode()}_${System.currentTimeMillis() % 10000}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BillCraft")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return false

                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Toast.makeText(context, "Saved image to Downloads/BillCraft/$fileName", Toast.LENGTH_LONG).show()
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BillCraft")
                if (!dir.exists()) dir.mkdirs()
                val target = File(dir, fileName)
                FileOutputStream(target).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Toast.makeText(context, "Saved to Downloads/BillCraft/$fileName", Toast.LENGTH_LONG).show()
                true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun shareProductLabel(
        context: Context,
        item: CatalogItemEntity,
        currencySymbol: String,
        businessName: String = "BillCraft",
        qrOnly: Boolean = false
    ) {
        try {
            val bitmap = generateProductLabelBitmap(context, item, currencySymbol, businessName, qrOnly)
            val prefix = if (qrOnly) "product_qr" else "product_label"
            val file = File(context.cacheDir, "${prefix}_${item.getEffectiveQrCode()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Product QR - ${item.name}")
                putExtra(Intent.EXTRA_TEXT, "Product QR for ${item.name} (${currencySymbol}${item.defaultUnitPrice}). Scan to get details.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Product QR"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing QR: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printProductLabel(
        context: Context,
        item: CatalogItemEntity,
        currencySymbol: String,
        businessName: String = "BillCraft",
        qrOnly: Boolean = false
    ) {
        try {
            val bitmap = generateProductLabelBitmap(context, item, currencySymbol, businessName, qrOnly)
            val printHelper = PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
            }
            val jobName = if (qrOnly) "QR_${item.getEffectiveQrCode()}" else "Label_${item.getEffectiveQrCode()}"
            printHelper.printBitmap(jobName, bitmap)
        } catch (e: Exception) {
            Toast.makeText(context, "Printing error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
