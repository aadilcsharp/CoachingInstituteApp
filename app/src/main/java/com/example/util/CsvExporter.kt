package com.example.util

import android.content.Context
import com.example.data.model.BillWithItems
import com.example.data.model.BusinessProfileEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object CsvExporter {

    fun exportBillsToCsv(
        context: Context,
        fileNamePrefix: String,
        bills: List<BillWithItems>,
        profile: BusinessProfileEntity
    ): File {
        val outDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(outDir, "${fileNamePrefix}_${System.currentTimeMillis()}.csv")

        FileOutputStream(outFile).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // CSV Header
                writer.appendLine("Bill Number,Date,Due Date,Client Name,Category,Subtotal,Tax %,Tax Amount,Discount %,Discount Amount,Total Amount,Paid Amount,Balance,Status,Payment Method,Notes")

                for (billWithItems in bills) {
                    val b = billWithItems.bill
                    val balance = (b.totalAmount - b.paidAmount).coerceAtLeast(0.0)
                    val row = listOf(
                        escapeCsv(b.billNumber),
                        escapeCsv(DateUtils.formatIsoDate(b.billDate)),
                        escapeCsv(b.dueDate?.let { DateUtils.formatIsoDate(it) } ?: ""),
                        escapeCsv(b.clientName),
                        escapeCsv(b.category),
                        b.subtotal.toString(),
                        b.taxPercent.toString(),
                        b.taxAmount.toString(),
                        b.discountPercent.toString(),
                        b.discountAmount.toString(),
                        b.totalAmount.toString(),
                        b.paidAmount.toString(),
                        balance.toString(),
                        escapeCsv(b.paymentStatus),
                        escapeCsv(b.paymentMethod ?: ""),
                        escapeCsv(b.notes ?: "")
                    ).joinToString(",")
                    writer.appendLine(row)
                }
            }
        }

        return outFile
    }

    private fun escapeCsv(text: String): String {
        return if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }
}
