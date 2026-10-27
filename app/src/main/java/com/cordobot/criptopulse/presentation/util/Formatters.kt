package com.cordobot.criptopulse.presentation.util

import com.cordobot.criptopulse.domain.model.FiatCurrency
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

private val spanish = Locale.forLanguageTag("es-ES")

/** Price with as many decimals as it needs: 79.876,00 US$ but 0,00001234 US$. */
fun formatPrice(value: Double, currency: FiatCurrency): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(currency.languageTag)).apply {
        this.currency = Currency.getInstance(currency.apiCode.uppercase())
        val decimals = when {
            abs(value) >= 1.0 -> 2
            abs(value) >= 0.01 -> 4
            else -> 8
        }
        maximumFractionDigits = decimals
        minimumFractionDigits = minOf(2, decimals)
    }
    return format.format(value)
}

/** Large amounts in Spanish scale: "1,61 B" (billones), "26,84 mil M", "350,2 M". */
fun formatCompact(value: Double, currency: FiatCurrency): String {
    val symbol = Currency.getInstance(currency.apiCode.uppercase()).getSymbol(Locale.forLanguageTag(currency.languageTag))
    val number = NumberFormat.getNumberInstance(spanish).apply { maximumFractionDigits = 2 }
    val absolute = abs(value)
    val text = when {
        absolute >= 1e12 -> number.format(value / 1e12) + " B"
        absolute >= 1e9 -> number.format(value / 1e9) + " mil M"
        absolute >= 1e6 -> number.format(value / 1e6) + " M"
        else -> number.format(value)
    }
    return "$symbol $text"
}

fun formatSupply(value: Double, symbol: String): String =
    NumberFormat.getNumberInstance(spanish).apply { maximumFractionDigits = 0 }.format(value) + " " + symbol

/** "+1,23 %" / "-0,30 %" */
fun formatChange(value: Double?): String {
    if (value == null) return "—"
    val number = NumberFormat.getNumberInstance(spanish).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(abs(value))
    val sign = if (value >= 0) "+" else "-"
    return "$sign$number %"
}

fun relativeTime(thenMillis: Long, nowMillis: Long): String {
    val minutes = (nowMillis - thenMillis) / 60_000
    return when {
        minutes < 1 -> "justo ahora"
        minutes < 60 -> "hace $minutes min"
        minutes < 24 * 60 -> "hace ${minutes / 60} h"
        else -> "hace ${minutes / (24 * 60)} d"
    }
}
