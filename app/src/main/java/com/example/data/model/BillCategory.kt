package com.example.data.model

enum class BillCategory(val displayName: String, val iconName: String) {
    SERVICES("Services", "Handyman"),
    PRODUCTS("Products", "Inventory"),
    CONSULTING("Consulting", "Work"),
    UTILITIES("Utilities", "Bolt"),
    MAINTENANCE("Maintenance", "Build"),
    OFFICE("Office", "Business"),
    FREELANCE("Freelance", "Laptop"),
    OTHER("Other", "Category");

    companion object {
        fun fromString(value: String): BillCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
