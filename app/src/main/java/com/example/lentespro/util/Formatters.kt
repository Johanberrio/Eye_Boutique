package com.example.lentespro.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val money: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

    fun money(value: Double): String = money.format(value)

    fun epochMillisToDateText(epochMillis: Long?): String {
        if (epochMillis == null) return "—"
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(dateFormatter)
    }

    fun dateTextToEpochMillisOrNull(dateText: String): Long? {
        val trimmed = dateText.trim()
        if (trimmed.isBlank()) return null
        val date = LocalDate.parse(trimmed, dateFormatter)
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
