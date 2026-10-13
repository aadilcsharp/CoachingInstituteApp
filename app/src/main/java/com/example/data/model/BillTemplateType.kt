package com.example.data.model

enum class BillTemplateType(
    val templateId: String,
    val displayName: String,
    val description: String,
    val primaryColorHex: String,
    val secondaryColorHex: String
) {
    MODERN_CLEAN(
        templateId = "MODERN_CLEAN",
        displayName = "Modern Clean",
        description = "Sleek blue accent bar with crisp tabular layout and status pill.",
        primaryColorHex = "#0284C7",
        secondaryColorHex = "#E0F2FE"
    ),
    EXECUTIVE_NAVY(
        templateId = "EXECUTIVE_NAVY",
        displayName = "Executive Navy",
        description = "Authoritative deep indigo block header with inverted typography.",
        primaryColorHex = "#1E1B4B",
        secondaryColorHex = "#EEF2FF"
    ),
    MINIMALIST_SLATE(
        templateId = "MINIMALIST_SLATE",
        displayName = "Minimalist Slate",
        description = "High-end editorial aesthetic with subtle borders and ample whitespace.",
        primaryColorHex = "#334155",
        secondaryColorHex = "#F1F5F9"
    ),
    EMERALD_CORP(
        templateId = "EMERALD_CORP",
        displayName = "Emerald Pro",
        description = "Fresh corporate green highlights with double-rule totals section.",
        primaryColorHex = "#047857",
        secondaryColorHex = "#D1FAE5"
    );

    companion object {
        fun fromId(id: String): BillTemplateType {
            return entries.firstOrNull { it.templateId.equals(id, ignoreCase = true) } ?: MODERN_CLEAN
        }
    }
}
