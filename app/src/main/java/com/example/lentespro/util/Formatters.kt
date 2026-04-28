package com.example.lentespro.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.Normalizer
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val money: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

    fun money(value: Double): String = money.format(value)

    @RequiresApi(Build.VERSION_CODES.O)
    fun epochMillisToDateText(epochMillis: Long?): String {
        if (epochMillis == null) return "—"
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("America/Bogota")).toLocalDate()
        return date.format(dateFormatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun dateTextToEpochMillisOrNull(dateText: String): Long? {
        val trimmed = dateText.trim()
        if (trimmed.isBlank()) return null
        val date = LocalDate.parse(trimmed, dateFormatter)
        return date.atStartOfDay(ZoneId.of("America/Bogota")).toInstant().toEpochMilli()
    }

    /**
     * Normaliza un texto eliminando tildes y caracteres especiales, 
     * y convirtiéndolo a mayúsculas para comparaciones consistentes.
     */
    fun normalize(text: String): String {
        val normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        val regex = Regex("\\p{InCombiningDiacriticalMarks}+")
        return regex.replace(normalized, "").uppercase(Locale.getDefault())
    }
}
