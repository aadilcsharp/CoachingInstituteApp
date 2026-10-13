package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BillTemplateType
import com.example.data.model.BillWithItems
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.PaymentStatus
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points
    private const val MARGIN_X = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    fun generateBillPdf(
        context: Context,
        billWithItems: BillWithItems,
        profile: BusinessProfileEntity
    ): File {
        val bill = billWithItems.bill
        val items = billWithItems.items
        val templateType = BillTemplateType.fromId(bill.templateId)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawBillPage(context, canvas, bill, items, profile, templateType)

        document.finishPage(page)

        val fileName = "Invoice_${bill.billNumber.replace("[^a-zA-Z0-9-]".toRegex(), "_")}.pdf"
        val outDir = File(context.cacheDir, "bills").apply { mkdirs() }
        val outFile = File(outDir, fileName)

        FileOutputStream(outFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        return outFile
    }

    private fun drawBillPage(
        context: Context,
        canvas: Canvas,
        bill: BillEntity,
        items: List<BillItemEntity>,
        profile: BusinessProfileEntity,
        template: BillTemplateType
    ) {
        val primaryColor = android.graphics.Color.parseColor(template.primaryColorHex)
        val secondaryColor = android.graphics.Color.parseColor(template.secondaryColorHex)
        val textDark = Color.rgb(15, 23, 42)
        val textMuted = Color.rgb(100, 116, 139)
        val borderLight = Color.rgb(226, 232, 240)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Check if business logo should be drawn on bill (only allowed on BASIC tier and above)
        val isLogoAllowedByTier = LicenseManager.checkAndGetLicenseInfo(context).tier.isLogoAllowed
        val shouldDrawLogo = isLogoAllowedByTier && profile.showLogoOnBill && !profile.logoPath.isNullOrBlank()
        val logoFile = if (shouldDrawLogo) File(profile.logoPath!!) else null
        val logoBitmap = if (logoFile != null && logoFile.exists()) {
            ImageOptimizer.loadSampledBitmap(profile.logoPath!!, 500)
        } else null
        val filterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }

        // 1. Header background according to template
        when (template) {
            BillTemplateType.EXECUTIVE_NAVY -> {
                paint.color = primaryColor
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 130f, paint)

                var textStartX = MARGIN_X
                if (logoBitmap != null) {
                    // White rounded badge for logo contrast on dark navy header
                    paint.color = Color.WHITE
                    val logoBgRect = RectF(MARGIN_X, 28f, MARGIN_X + 62f, 90f)
                    canvas.drawRoundRect(logoBgRect, 8f, 8f, paint)
                    val logoInnerRect = RectF(MARGIN_X + 4f, 32f, MARGIN_X + 58f, 86f)
                    canvas.drawBitmap(logoBitmap, null, logoInnerRect, filterPaint)
                    textStartX = MARGIN_X + 74f
                }

                // White text on dark navy
                paint.color = Color.WHITE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 22f
                canvas.drawText(profile.businessName.ifEmpty { "BUSINESS INVOICE" }, textStartX, 55f, paint)

                paint.typeface = Typeface.DEFAULT
                paint.textSize = 10f
                paint.color = Color.rgb(224, 231, 255)
                canvas.drawText(profile.ownerName, textStartX, 72f, paint)
                canvas.drawText("${profile.email}  |  ${profile.phone}", textStartX, 86f, paint)
                if (profile.address.isNotEmpty()) {
                    canvas.drawText(profile.address, textStartX, 100f, paint)
                }

                // INVOICE title top right
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 24f
                paint.color = Color.WHITE
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("INVOICE", PAGE_WIDTH - MARGIN_X, 55f, paint)

                paint.typeface = Typeface.DEFAULT
                paint.textSize = 11f
                paint.color = Color.rgb(224, 231, 255)
                canvas.drawText(bill.billNumber, PAGE_WIDTH - MARGIN_X, 74f, paint)
                paint.textAlign = Paint.Align.LEFT
            }

            BillTemplateType.MODERN_CLEAN -> {
                // Top colorful accent bar
                paint.color = primaryColor
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 8f, paint)

                var textStartX = MARGIN_X
                if (logoBitmap != null) {
                    val logoRect = RectF(MARGIN_X, 26f, MARGIN_X + 58f, 84f)
                    canvas.drawBitmap(logoBitmap, null, logoRect, filterPaint)
                    textStartX = MARGIN_X + 70f
                }

                // Business Name & Info
                paint.color = primaryColor
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 22f
                canvas.drawText(profile.businessName.ifEmpty { "BUSINESS INVOICE" }, textStartX, 50f, paint)

                paint.typeface = Typeface.DEFAULT
                paint.textSize = 10f
                paint.color = textMuted
                var yBio = 68f
                if (profile.ownerName.isNotEmpty()) {
                    canvas.drawText(profile.ownerName, textStartX, yBio, paint)
                    yBio += 14f
                }
                canvas.drawText("${profile.email}  •  ${profile.phone}", textStartX, yBio, paint)
                yBio += 14f
                if (profile.address.isNotEmpty()) {
                    canvas.drawText(profile.address, textStartX, yBio, paint)
                }

                // Top right INVOICE badge
                paint.color = secondaryColor
                val badgeRect = RectF(PAGE_WIDTH - MARGIN_X - 150f, 32f, PAGE_WIDTH - MARGIN_X, 84f)
                canvas.drawRoundRect(badgeRect, 6f, 6f, paint)

                paint.color = primaryColor
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 16f
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("INVOICE", PAGE_WIDTH - MARGIN_X - 14f, 54f, paint)

                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = textDark
                canvas.drawText(bill.billNumber, PAGE_WIDTH - MARGIN_X - 14f, 72f, paint)
                paint.textAlign = Paint.Align.LEFT
            }

            BillTemplateType.EMERALD_CORP -> {
                var barX = MARGIN_X
                if (logoBitmap != null) {
                    val logoRect = RectF(MARGIN_X, 26f, MARGIN_X + 58f, 84f)
                    canvas.drawBitmap(logoBitmap, null, logoRect, filterPaint)
                    barX = MARGIN_X + 68f
                }

                paint.color = primaryColor
                canvas.drawRect(barX, 30f, barX + 6f, 105f, paint)

                val textStartX = barX + 16f
                paint.color = primaryColor
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 22f
                canvas.drawText(profile.businessName.ifEmpty { "BUSINESS INVOICE" }, textStartX, 52f, paint)

                paint.typeface = Typeface.DEFAULT
                paint.textSize = 10f
                paint.color = textMuted
                canvas.drawText("${profile.ownerName}  |  ${profile.email}", textStartX, 70f, paint)
                canvas.drawText(profile.phone, textStartX, 84f, paint)
                if (profile.address.isNotEmpty()) {
                    canvas.drawText(profile.address, textStartX, 98f, paint)
                }

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 24f
                paint.color = primaryColor
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("INVOICE", PAGE_WIDTH - MARGIN_X, 55f, paint)

                paint.typeface = Typeface.DEFAULT
                paint.textSize = 11f
                paint.color = textDark
                canvas.drawText(bill.billNumber, PAGE_WIDTH - MARGIN_X, 74f, paint)
                paint.textAlign = Paint.Align.LEFT
            }

            BillTemplateType.MINIMALIST_SLATE -> {
                var textStartX = MARGIN_X
                if (logoBitmap != null) {
                    val logoRect = RectF(MARGIN_X, 26f, MARGIN_X + 58f, 84f)
                    canvas.drawBitmap(logoBitmap, null, logoRect, filterPaint)
                    textStartX = MARGIN_X + 70f
                }

                paint.color = textDark
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 22f
                canvas.drawText(profile.businessName.ifEmpty { "BUSINESS INVOICE" }, textStartX, 52f, paint)

                paint.typeface = Typeface.DEFAULT
                paint.textSize = 10f
                paint.color = textMuted
                canvas.drawText("${profile.email}   ${profile.phone}", textStartX, 70f, paint)
                if (profile.address.isNotEmpty()) {
                    canvas.drawText(profile.address, textStartX, 84f, paint)
                }

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 20f
                paint.color = textDark
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(bill.billNumber, PAGE_WIDTH - MARGIN_X, 52f, paint)
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 11f
                paint.color = textMuted
                canvas.drawText("TAX INVOICE", PAGE_WIDTH - MARGIN_X, 70f, paint)
                paint.textAlign = Paint.Align.LEFT

                // Clean divider line
                paint.color = borderLight
                paint.strokeWidth = 1f
                canvas.drawLine(MARGIN_X, 105f, PAGE_WIDTH - MARGIN_X, 105f, paint)
            }
        }

        logoBitmap?.recycle()

        // 2. Bill Meta & Bill To Section (Y: 145 to 225)
        val metaY = if (template == BillTemplateType.EXECUTIVE_NAVY) 155f else 135f

        // Billed To Box
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        paint.color = textMuted
        canvas.drawText("BILLED TO", MARGIN_X, metaY, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        paint.color = textDark
        canvas.drawText(bill.clientName, MARGIN_X, metaY + 18f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        paint.color = textMuted
        var clientY = metaY + 34f
        if (!bill.clientEmail.isNullOrBlank()) {
            canvas.drawText(bill.clientEmail, MARGIN_X, clientY, paint)
            clientY += 14f
        }
        if (!bill.clientPhone.isNullOrBlank()) {
            canvas.drawText(bill.clientPhone, MARGIN_X, clientY, paint)
            clientY += 14f
        }
        if (!bill.clientAddress.isNullOrBlank()) {
            canvas.drawText(bill.clientAddress, MARGIN_X, clientY, paint)
        }

        // Invoice Dates & Payment Status Box (Right Side)
        val colRightX = PAGE_WIDTH - MARGIN_X - 180f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        paint.color = textMuted
        canvas.drawText("INVOICE DETAILS", colRightX, metaY, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        paint.color = textDark
        canvas.drawText("Issue Date: ${DateUtils.formatDate(bill.billDate)}", colRightX, metaY + 18f, paint)

        if (bill.dueDate != null) {
            canvas.drawText("Due Date: ${DateUtils.formatDate(bill.dueDate)}", colRightX, metaY + 34f, paint)
        }

        if (profile.taxId.isNotEmpty()) {
            canvas.drawText("Tax ID: ${profile.taxId}", colRightX, metaY + 50f, paint)
        }

        // Payment Status Tag
        val status = PaymentStatus.fromString(bill.paymentStatus)
        val statusBg = when (status) {
            PaymentStatus.PAID -> Color.rgb(209, 250, 229)
            PaymentStatus.PARTIAL -> Color.rgb(254, 243, 199)
            PaymentStatus.UNPAID -> Color.rgb(254, 226, 226)
        }
        val statusText = when (status) {
            PaymentStatus.PAID -> Color.rgb(6, 95, 70)
            PaymentStatus.PARTIAL -> Color.rgb(146, 64, 14)
            PaymentStatus.UNPAID -> Color.rgb(153, 27, 27)
        }

        val statusPill = RectF(colRightX, metaY + 62f, colRightX + 90f, metaY + 80f)
        paint.color = statusBg
        canvas.drawRoundRect(statusPill, 4f, 4f, paint)
        paint.color = statusText
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(status.displayName.uppercase(Locale.US), colRightX + 45f, metaY + 74f, paint)
        paint.textAlign = Paint.Align.LEFT

        // 3. Table Header (Y ~ 255)
        val tableTopY = 255f
        paint.color = secondaryColor
        val thRect = RectF(MARGIN_X, tableTopY, PAGE_WIDTH - MARGIN_X, tableTopY + 24f)
        canvas.drawRoundRect(thRect, 4f, 4f, paint)

        paint.color = primaryColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas.drawText("ITEM DESCRIPTION", MARGIN_X + 12f, tableTopY + 16f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("QTY", MARGIN_X + 310f, tableTopY + 16f, paint)
        canvas.drawText("UNIT PRICE", MARGIN_X + 410f, tableTopY + 16f, paint)
        canvas.drawText("TOTAL", PAGE_WIDTH - MARGIN_X - 12f, tableTopY + 16f, paint)
        paint.textAlign = Paint.Align.LEFT

        // 4. Table Items
        var currentY = tableTopY + 42f
        val symbol = profile.currencySymbol

        paint.typeface = Typeface.DEFAULT
        items.forEachIndexed { index, item ->
            val hasImage = !item.imagePath.isNullOrBlank()
            val hasDesc = !item.description.isNullOrBlank()
            val rowTotalHeight = when {
                hasImage && hasDesc -> 48f
                hasImage -> 42f
                hasDesc -> 28f
                else -> 20f
            }

            // Zebra striping background
            if (index % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN_X, currentY - 14f, PAGE_WIDTH - MARGIN_X, currentY + rowTotalHeight - 8f, paint)
            }

            // 1. FIRST: Item Name comes first at the top
            val textStartX = MARGIN_X + 12f
            paint.color = textDark
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            canvas.drawText(item.name, textStartX, currentY, paint)

            // QTY, Unit Price, Amount on the top line
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            paint.color = textDark
            paint.textAlign = Paint.Align.RIGHT

            val qtyText = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            canvas.drawText(qtyText, MARGIN_X + 310f, currentY, paint)
            canvas.drawText(CurrencyFormatter.format(item.unitPrice, symbol), MARGIN_X + 410f, currentY, paint)
            canvas.drawText(CurrencyFormatter.format(item.amount, symbol), PAGE_WIDTH - MARGIN_X - 12f, currentY, paint)
            paint.textAlign = Paint.Align.LEFT

            // 2. SECOND: Picture of product right below item name
            var subY = currentY + 4f
            var itemBitmap: android.graphics.Bitmap? = null
            var nextTextX = textStartX
            if (hasImage) {
                // High-resolution bitmap (500px) with bilinear filtering so it remains crisp on zoom
                itemBitmap = ImageOptimizer.loadSampledBitmap(item.imagePath!!, 500)
                if (itemBitmap != null) {
                    val imgSize = 30f
                    val imgRect = RectF(textStartX, subY, textStartX + imgSize, subY + imgSize)

                    // Rounded border frame
                    paint.color = borderLight
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.5f
                    canvas.drawRoundRect(imgRect, 4f, 4f, paint)
                    paint.style = Paint.Style.FILL

                    val innerRect = RectF(textStartX + 1f, subY + 1f, textStartX + imgSize - 1f, subY + imgSize - 1f)
                    canvas.drawBitmap(itemBitmap, null, innerRect, filterPaint)
                    nextTextX = textStartX + imgSize + 8f
                }
            }

            // 3. Description right next to or below product photo
            if (hasDesc) {
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 8.5f
                paint.color = textMuted
                val descY = if (hasImage) subY + 14f else subY + 8f
                canvas.drawText(item.description!!, nextTextX, descY, paint)
            }

            itemBitmap?.recycle()

            // Line separator
            val sepY = currentY + rowTotalHeight - 6f
            paint.color = borderLight
            paint.strokeWidth = 0.5f
            canvas.drawLine(MARGIN_X, sepY, PAGE_WIDTH - MARGIN_X, sepY, paint)

            currentY = sepY + 16f
        }

        // 5. Totals & Summary Section
        val summaryY = currentY + 15f
        val summaryLeftX = PAGE_WIDTH - MARGIN_X - 220f

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        paint.color = textMuted
        canvas.drawText("Subtotal", summaryLeftX, summaryY, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.color = textDark
        canvas.drawText(CurrencyFormatter.format(bill.subtotal, symbol), PAGE_WIDTH - MARGIN_X - 12f, summaryY, paint)
        paint.textAlign = Paint.Align.LEFT

        var runningY = summaryY + 18f
        if (bill.discountAmount > 0) {
            paint.color = textMuted
            canvas.drawText("Discount (${bill.discountPercent}%)", summaryLeftX, runningY, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.rgb(220, 38, 38)
            canvas.drawText("-${CurrencyFormatter.format(bill.discountAmount, symbol)}", PAGE_WIDTH - MARGIN_X - 12f, runningY, paint)
            paint.textAlign = Paint.Align.LEFT
            runningY += 18f
        }

        if (bill.taxAmount > 0) {
            paint.color = textMuted
            canvas.drawText("Tax (${bill.taxPercent}%)", summaryLeftX, runningY, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = textDark
            canvas.drawText(CurrencyFormatter.format(bill.taxAmount, symbol), PAGE_WIDTH - MARGIN_X - 12f, runningY, paint)
            paint.textAlign = Paint.Align.LEFT
            runningY += 18f
        }

        // Total Highlight Box
        val totalBox = RectF(summaryLeftX - 10f, runningY, PAGE_WIDTH - MARGIN_X, runningY + 34f)
        paint.color = secondaryColor
        canvas.drawRoundRect(totalBox, 6f, 6f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        paint.color = primaryColor
        canvas.drawText("Total Due", summaryLeftX, runningY + 22f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 14f
        canvas.drawText(CurrencyFormatter.format(bill.totalAmount, symbol), PAGE_WIDTH - MARGIN_X - 12f, runningY + 22f, paint)
        paint.textAlign = Paint.Align.LEFT

        runningY += 46f
        if (bill.paidAmount > 0) {
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            paint.color = Color.rgb(16, 185, 129)
            canvas.drawText("Amount Paid", summaryLeftX, runningY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(CurrencyFormatter.format(bill.paidAmount, symbol), PAGE_WIDTH - MARGIN_X - 12f, runningY, paint)
            paint.textAlign = Paint.Align.LEFT
            runningY += 18f

            val balance = (bill.totalAmount - bill.paidAmount).coerceAtLeast(0.0)
            paint.color = textMuted
            canvas.drawText("Balance Due", summaryLeftX, runningY, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = if (balance > 0) Color.rgb(220, 38, 38) else Color.rgb(16, 185, 129)
            canvas.drawText(CurrencyFormatter.format(balance, symbol), PAGE_WIDTH - MARGIN_X - 12f, runningY, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        // 6. Payment Instructions & QR Code (Bottom left)
        var noteY = summaryY
        val shouldDrawPaymentQr = profile.showPaymentQrOnBill && profile.paymentQrText.isNotBlank()
        val qrBitmap = if (shouldDrawPaymentQr) {
            QrCodeGenerator.generateQrBitmap(profile.paymentQrText, 300)
        } else null

        if (qrBitmap != null) {
            val qrBoxSize = 70f
            val qrRect = RectF(MARGIN_X, noteY, MARGIN_X + qrBoxSize, noteY + qrBoxSize)

            paint.color = borderLight
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(qrRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            val qrInnerRect = RectF(MARGIN_X + 4f, noteY + 4f, MARGIN_X + qrBoxSize - 4f, noteY + qrBoxSize - 4f)
            canvas.drawBitmap(qrBitmap, null, qrInnerRect, filterPaint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7.5f
            paint.color = primaryColor
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("SCAN TO PAY", MARGIN_X + (qrBoxSize / 2f), noteY + qrBoxSize + 11f, paint)
            paint.textAlign = Paint.Align.LEFT

            val textLeftX = MARGIN_X + qrBoxSize + 12f
            var payTextY = noteY + 8f

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            paint.color = primaryColor
            canvas.drawText("PAYMENT METHOD & DETAILS", textLeftX, payTextY, paint)
            payTextY += 13f

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8.5f
            paint.color = textMuted
            for (line in profile.paymentDetails.lines()) {
                if (line.isNotBlank()) {
                    canvas.drawText(line, textLeftX, payTextY, paint)
                    payTextY += 11f
                }
            }

            if (profile.paymentQrText.isNotBlank()) {
                paint.color = textDark
                canvas.drawText("UPI / QR: ${profile.paymentQrText}", textLeftX, payTextY, paint)
                payTextY += 11f
            }

            noteY = maxOf(noteY + qrBoxSize + 18f, payTextY + 6f)
        } else if (profile.paymentDetails.isNotEmpty()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            paint.color = primaryColor
            canvas.drawText("PAYMENT METHOD & DETAILS", MARGIN_X, noteY, paint)
            noteY += 14f

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8.5f
            paint.color = textMuted
            for (line in profile.paymentDetails.lines()) {
                canvas.drawText(line, MARGIN_X, noteY, paint)
                noteY += 12f
            }
            noteY += 6f
        }

        if (!bill.notes.isNullOrBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            paint.color = primaryColor
            canvas.drawText("NOTES", MARGIN_X, noteY, paint)
            noteY += 14f

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8.5f
            paint.color = textMuted
            for (line in bill.notes.lines()) {
                canvas.drawText(line, MARGIN_X, noteY, paint)
                noteY += 12f
            }
        }

        // Footer at bottom of page
        paint.color = borderLight
        paint.strokeWidth = 0.5f
        canvas.drawLine(MARGIN_X, PAGE_HEIGHT - 45f, PAGE_WIDTH - MARGIN_X, PAGE_HEIGHT - 45f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8f
        paint.color = textMuted
        canvas.drawText("Generated by BillCraft • Thank you for your business!", MARGIN_X, PAGE_HEIGHT - 30f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page 1 of 1", PAGE_WIDTH - MARGIN_X, PAGE_HEIGHT - 30f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    fun generateReportPdf(
        context: Context,
        reportTitle: String,
        periodName: String,
        bills: List<BillWithItems>,
        profile: BusinessProfileEntity,
        totalBilled: Double,
        totalPaid: Double,
        totalOutstanding: Double,
        categoryBreakdown: Map<String, Double>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val primaryColor = Color.rgb(37, 99, 235) // Modern Indigo
        val textDark = Color.rgb(15, 23, 42)
        val textMuted = Color.rgb(100, 116, 139)
        val borderLight = Color.rgb(226, 232, 240)
        val symbol = profile.currencySymbol

        // Header
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 80f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        canvas.drawText(reportTitle, MARGIN_X, 42f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        paint.color = Color.rgb(219, 234, 254)
        canvas.drawText("Period: $periodName • Generated on ${DateUtils.formatDate(System.currentTimeMillis())}", MARGIN_X, 60f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(profile.businessName, PAGE_WIDTH - MARGIN_X, 42f, paint)
        paint.textAlign = Paint.Align.LEFT

        // 3 Key Metrics Cards
        val cardWidth = (CONTENT_WIDTH - 20f) / 3f
        val cardY = 100f
        val cardHeight = 65f

        // Billed Card
        drawMetricCard(canvas, MARGIN_X, cardY, cardWidth, cardHeight, "TOTAL BILLED", CurrencyFormatter.format(totalBilled, symbol), Color.rgb(239, 246, 255), Color.rgb(37, 99, 235))
        // Paid Card
        drawMetricCard(canvas, MARGIN_X + cardWidth + 10f, cardY, cardWidth, cardHeight, "TOTAL COLLECTED", CurrencyFormatter.format(totalPaid, symbol), Color.rgb(240, 253, 244), Color.rgb(16, 185, 129))
        // Outstanding Card
        drawMetricCard(canvas, MARGIN_X + (cardWidth + 10f) * 2f, cardY, cardWidth, cardHeight, "OUTSTANDING", CurrencyFormatter.format(totalOutstanding, symbol), Color.rgb(254, 242, 242), Color.rgb(239, 68, 68))

        // Category Breakdown Section
        var sectionY = 195f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        paint.color = textDark
        canvas.drawText("EXPENSE & REVENUE BY CATEGORY", MARGIN_X, sectionY, paint)
        sectionY += 15f

        categoryBreakdown.entries.take(5).forEach { (category, amount) ->
            val percent = if (totalBilled > 0) (amount / totalBilled * 100).toInt() else 0
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9.5f
            paint.color = textMuted
            canvas.drawText(category, MARGIN_X, sectionY, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.color = textDark
            canvas.drawText("${CurrencyFormatter.format(amount, symbol)} ($percent%)", PAGE_WIDTH - MARGIN_X, sectionY, paint)
            paint.textAlign = Paint.Align.LEFT

            // Mini progress bar
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawRect(MARGIN_X, sectionY + 4f, PAGE_WIDTH - MARGIN_X, sectionY + 7f, paint)
            paint.color = primaryColor
            val fillWidth = if (totalBilled > 0) (amount / totalBilled * CONTENT_WIDTH).toFloat() else 0f
            canvas.drawRect(MARGIN_X, sectionY + 4f, MARGIN_X + fillWidth, sectionY + 7f, paint)

            sectionY += 22f
        }

        // Recent Bills Table
        sectionY += 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        paint.color = textDark
        canvas.drawText("RECORDED BILLS (${bills.size})", MARGIN_X, sectionY, paint)
        sectionY += 15f

        // Table Header
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(MARGIN_X, sectionY, PAGE_WIDTH - MARGIN_X, sectionY + 20f, paint)

        paint.color = textMuted
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawText("DATE", MARGIN_X + 6f, sectionY + 14f, paint)
        canvas.drawText("BILL #", MARGIN_X + 70f, sectionY + 14f, paint)
        canvas.drawText("CLIENT", MARGIN_X + 160f, sectionY + 14f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("STATUS", MARGIN_X + 380f, sectionY + 14f, paint)
        canvas.drawText("TOTAL", PAGE_WIDTH - MARGIN_X - 6f, sectionY + 14f, paint)
        paint.textAlign = Paint.Align.LEFT

        sectionY += 28f
        paint.typeface = Typeface.DEFAULT
        bills.take(15).forEach { billWithItems ->
            val b = billWithItems.bill
            paint.textSize = 9f
            paint.color = textMuted
            canvas.drawText(DateUtils.formatIsoDate(b.billDate), MARGIN_X + 6f, sectionY, paint)

            paint.color = textDark
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(b.billNumber, MARGIN_X + 70f, sectionY, paint)

            paint.typeface = Typeface.DEFAULT
            paint.color = textDark
            val clientTruncated = if (b.clientName.length > 20) b.clientName.take(18) + ".." else b.clientName
            canvas.drawText(clientTruncated, MARGIN_X + 160f, sectionY, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.color = when (PaymentStatus.fromString(b.paymentStatus)) {
                PaymentStatus.PAID -> Color.rgb(16, 185, 129)
                PaymentStatus.PARTIAL -> Color.rgb(217, 119, 6)
                PaymentStatus.UNPAID -> Color.rgb(239, 68, 68)
            }
            canvas.drawText(b.paymentStatus, MARGIN_X + 380f, sectionY, paint)

            paint.color = textDark
            canvas.drawText(CurrencyFormatter.format(b.totalAmount, symbol), PAGE_WIDTH - MARGIN_X - 6f, sectionY, paint)
            paint.textAlign = Paint.Align.LEFT

            paint.color = borderLight
            canvas.drawLine(MARGIN_X, sectionY + 8f, PAGE_WIDTH - MARGIN_X, sectionY + 8f, paint)
            sectionY += 20f
        }

        // Footer
        paint.color = textMuted
        paint.textSize = 8f
        canvas.drawText("Generated by BillCraft Mobile Expense & Billing Manager", MARGIN_X, PAGE_HEIGHT - 30f, paint)

        document.finishPage(page)

        val fileName = "Report_${periodName.replace(" ", "_")}.pdf"
        val outDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(outDir, fileName)

        FileOutputStream(outFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        return outFile
    }

    private fun drawMetricCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        label: String,
        value: String,
        bgColor: Int,
        valueColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = bgColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(label, x + 12f, y + 22f, paint)

        paint.textSize = 15f
        paint.color = valueColor
        canvas.drawText(value, x + 12f, y + 48f, paint)
    }
}
