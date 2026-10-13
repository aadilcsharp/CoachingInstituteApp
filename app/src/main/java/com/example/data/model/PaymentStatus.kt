package com.example.data.model

enum class PaymentStatus(val displayName: String) {
    PAID("Paid"),
    PARTIAL("Partially Paid"),
    UNPAID("Unpaid");

    companion object {
        fun fromString(value: String): PaymentStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNPAID
        }
    }
}
