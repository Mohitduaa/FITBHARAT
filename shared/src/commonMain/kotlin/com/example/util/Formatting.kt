package com.example.util

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** Fixed-decimal formatting (kotlin-common has no String.format). Rounds half away from zero. */
fun Double.toFixed(decimals: Int): String {
    if (isNaN() || isInfinite()) return toString()
    val factor = 10.0.pow(decimals)
    val scaled = (abs(this) * factor).roundToLong()
    val negative = this < 0 && scaled != 0L
    val intPart = scaled / factor.toLong()
    val fracPart = scaled % factor.toLong()
    val sign = if (negative) "-" else ""
    return if (decimals == 0) "$sign$intPart" else "$sign$intPart.${fracPart.toString().padStart(decimals, '0')}"
}

/** "12,345" style grouping. */
fun Int.withCommas(): String {
    val digits = abs(this).toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (this < 0) "-$grouped" else grouped
}

/** Drops a trailing ".0" / ".50" so 72.0 -> "72" and 0.25 -> "0.25". */
fun Double.toCompactString(maxDecimals: Int = 2): String =
    toFixed(maxDecimals).trimEnd('0').trimEnd('.')
