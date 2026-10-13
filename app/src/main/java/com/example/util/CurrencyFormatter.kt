package com.example.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    fun format(amount: Double, symbol: String = "₹"): String {
        val isIndian = symbol == "₹" || symbol.equals("INR", ignoreCase = true)
        val formattedNumber = if (isIndian) {
            formatIndianCurrencyNumber(amount)
        } else {
            val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            formatter.format(amount)
        }
        return "$symbol$formattedNumber"
    }

    private fun formatIndianCurrencyNumber(amount: Double): String {
        val isNegative = amount < 0
        val absAmount = abs(amount)
        val wholePart = absAmount.toLong()
        val decimalPart = String.format(Locale.US, "%.2f", absAmount - wholePart).substring(2)

        val wholeStr = wholePart.toString()
        val formattedWhole = if (wholeStr.length <= 3) {
            wholeStr
        } else {
            val lastThree = wholeStr.substring(wholeStr.length - 3)
            val remaining = wholeStr.substring(0, wholeStr.length - 3)
            val builder = StringBuilder()
            var count = 0
            for (i in remaining.length - 1 downTo 0) {
                builder.append(remaining[i])
                count++
                if (count == 2 && i != 0) {
                    builder.append(',')
                    count = 0
                }
            }
            builder.reverse().toString() + "," + lastThree
        }

        val prefix = if (isNegative) "-" else ""
        return "$prefix$formattedWhole.$decimalPart"
    }

    fun formatCompact(amount: Double, symbol: String = "₹"): String {
        val isIndian = symbol == "₹" || symbol.equals("INR", ignoreCase = true)
        return if (isIndian) {
            when {
                amount >= 10_000_000 -> String.format(Locale.US, "%s%.2f Cr", symbol, amount / 10_000_000.0)
                amount >= 100_000 -> String.format(Locale.US, "%s%.2f L", symbol, amount / 100_000.0)
                amount >= 1_000 -> String.format(Locale.US, "%s%.1f K", symbol, amount / 1_000.0)
                else -> format(amount, symbol)
            }
        } else {
            when {
                amount >= 1_000_000 -> String.format(Locale.US, "%s%.1fM", symbol, amount / 1_000_000.0)
                amount >= 1_000 -> String.format(Locale.US, "%s%.1fK", symbol, amount / 1_000.0)
                else -> format(amount, symbol)
            }
        }
    }
}
