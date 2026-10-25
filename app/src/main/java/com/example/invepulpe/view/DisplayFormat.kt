package com.example.invepulpe.view


import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object DisplayFormat {
    fun money(cents: Long): String = "L " + NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(cents, 2))

    fun priceInput(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()

    fun date(utc: String): String = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val value = parser.parse(utc)
        if (value == null) utc else SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(value)
    } catch (_: java.text.ParseException) { utc }
}
