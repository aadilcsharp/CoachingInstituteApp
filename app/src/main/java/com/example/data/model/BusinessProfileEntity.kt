package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val businessName: String = "My Business",
    val ownerName: String = "Aadil Sharma",
    val email: String = "billing@mybusiness.in",
    val phone: String = "+91 98765 43210",
    val address: String = "Tech Park, MG Road, Bengaluru, Karnataka 560001",
    val taxId: String = "29AAAAA0000A1Z5", // Standard GSTIN format
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val paymentDetails: String = "HDFC Bank Ltd\nA/C: 50100234567890\nIFSC: HDFC0001234\nUPI ID: business@upi",
    val defaultTemplateId: String = BillTemplateType.MODERN_CLEAN.templateId,
    val defaultTaxPercent: Double = 18.0, // Standard GST rate
    val defaultNotes: String = "Thank you for your business! Payment is requested within 15 days.",
    val defaultTerms: String = "All services subject to standard terms. Late payments accrue 1.5% monthly interest.",
    val logoPath: String? = null,
    val showLogoOnBill: Boolean = true,
    val selectedCategories: String = "Services,Products,Consulting,Utilities,Maintenance,Office,Freelance,Other",
    val paymentQrText: String = "business@upi",
    val showPaymentQrOnBill: Boolean = true
) {
    fun getCategoriesList(): List<String> {
        val list = selectedCategories.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return if (list.isEmpty()) listOf("Services", "Products") else list
    }
}
