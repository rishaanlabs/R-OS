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

/**
 * Renders a stored amount as plain text for an input field.
 *
 * Editing an existing record has to put the amount back into the same box it was typed into, so
 * this is deliberately not [formatMoney]: no currency, no grouping separators, nothing that
 * [parseMajorToMinor] would have to strip again. What comes out here always parses back to the
 * value that went in.
 */
fun minorToEditableMajor(minor: Long): String =
    BigDecimal.valueOf(minor, MINOR_SCALE).toPlainString()
