package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bills",
    indices = [
        Index(value = ["billNumber"], unique = true),
        Index(value = ["billDate"]),
        Index(value = ["clientName"]),
        Index(value = ["paymentStatus"])
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billNumber: String,
    val title: String = "",
    val billDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val clientName: String,
    val clientEmail: String? = null,
    val clientPhone: String? = null,
    val clientAddress: String? = null,
    val subtotal: Double = 0.0,
    val taxPercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentStatus: String = PaymentStatus.UNPAID.name,
    val category: String = BillCategory.SERVICES.name,
    val templateId: String = BillTemplateType.MODERN_CLEAN.templateId,
    val notes: String? = null,
    val terms: String? = null,
    val paymentMethod: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
