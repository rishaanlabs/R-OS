package com.rishaanlabs.ros.domain.finance

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private const val MINOR_SCALE = 2

fun parseMajorToMinor(value: String): Long {
    val normalized = value.trim().replace(",", "")
    require(normalized.isNotBlank()) { "Amount is required." }
    return BigDecimal(normalized)
        .setScale(MINOR_SCALE, RoundingMode.HALF_UP)
        .movePointRight(MINOR_SCALE)
        .longValueExact()
}

fun formatMoney(minor: Long, currency: String = "MVR"): String {
    val amount = BigDecimal.valueOf(minor, MINOR_SCALE)
    val number = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = MINOR_SCALE
        maximumFractionDigits = MINOR_SCALE
    }.format(amount)
    return "${currency.uppercase()} $number"
}
