package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "catalog_items",
    indices = [
        Index(value = ["name"]),
        Index(value = ["qrCode"])
    ]
)
data class CatalogItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val defaultUnitPrice: Double = 0.0,
    val unit: String? = "pcs",
    val category: String? = "General",
    val imagePath: String? = null,
    val qrCode: String = generateNewQrCode(),
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns the active QR code identifier.
     * If blank, derives one deterministically from the ID or creation time.
     */
    fun getEffectiveQrCode(): String {
        return if (qrCode.isNotBlank()) qrCode else "BC-ITEM-${if (id > 0) id else System.currentTimeMillis() % 1000000}"
    }

    /**
     * Structured QR payload encoded into the product's QR code.
     * Allows instant offline parsing and cross-compatibility.
     * Format: BILLCRAFT_ITEM:<qrCode>:<name>:<price>:<unit>:<category>
     */
    fun getQrPayload(): String {
        val code = getEffectiveQrCode()
        val safeName = name.replace(":", " ")
        val safeUnit = (unit ?: "pcs").replace(":", " ")
        val safeCat = (category ?: "General").replace(":", " ")
        return "BILLCRAFT_ITEM:$code:$safeName:$defaultUnitPrice:$safeUnit:$safeCat"
    }

    companion object {
        fun generateNewQrCode(): String {
            val ts = (System.currentTimeMillis() % 1000000).toString().padStart(6, '0')
            val rnd = (100..999).random()
            return "BC-$ts$rnd"
        }

        fun parseFromQrPayload(payload: String): CatalogItemEntity? {
            val trimmed = payload.trim()
            if (trimmed.startsWith("BILLCRAFT_ITEM:")) {
                val parts = trimmed.split(":")
                if (parts.size >= 4) {
                    val code = parts[1]
                    val name = parts[2]
                    val price = parts[3].toDoubleOrNull() ?: 0.0
                    val unit = if (parts.size >= 5) parts[4] else "pcs"
                    val cat = if (parts.size >= 6) parts[5] else "General"
                    return CatalogItemEntity(
                        name = name,
                        defaultUnitPrice = price,
                        unit = unit,
                        category = cat,
                        qrCode = code
                    )
                }
            }
            return null
        }
    }
}
