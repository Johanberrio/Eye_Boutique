package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.SaleRepository
import com.example.lentespro.data.SaleStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class HistoryMonthEntry(
    val year: Int,
    val monthValue: Int, 
    val monthName: String,
    val count: Int,
    val lensStats: Map<String, Int> = emptyMap(), // ✅ Sincronizado con la pantalla
    val isStatic: Boolean = false,
    val isOngoing: Boolean = false
)

class SalesHistoryViewModel(private val saleRepo: SaleRepository) : ViewModel() {

    private val zone = ZoneId.of("America/Bogota")
    private val referenceEndDate = LocalDate.of(2026, 5, 13)

    // Datos históricos estáticos
    private val staticData = mapOf(
        (2024 to 8) to 196, (2024 to 9) to 212, (2024 to 10) to 220, (2024 to 11) to 307, (2024 to 12) to 243,
        (2025 to 1) to 230, (2025 to 2) to 254, (2025 to 3) to 268, (2025 to 4) to 298, (2025 to 5) to 256,
        (2025 to 6) to 220, (2025 to 7) to 195, (2025 to 8) to 199, (2025 to 9) to 209, (2025 to 10) to 219,
        (2025 to 11) to 615, (2025 to 12) to 407,
        (2026 to 1) to 435, (2026 to 2) to 238, (2026 to 3) to 248, (2026 to 4) to 326, (2026 to 5) to 296
    )

    val historyEntries: StateFlow<List<HistoryMonthEntry>> = saleRepo.observeSales()
        .map { sales ->
            val today = LocalDate.now(zone)
            val finalizedSales = sales.filter { it.status == SaleStatus.FINALIZADA }

            // 1. Agrupar TODAS las ventas de Firestore por periodos de 30 días
            val dbStatsByPeriod = mutableMapOf<LocalDate, MutableMap<String, Int>>()
            
            finalizedSales.forEach { sale ->
                val saleDate = Instant.ofEpochMilli(sale.finalizedAtEpochMillis ?: sale.createdAtEpochMillis)
                    .atZone(zone).toLocalDate()
                
                val daysDiff = ChronoUnit.DAYS.between(referenceEndDate, saleDate)
                val n = Math.ceil(daysDiff.toDouble() / 30.0).toLong()
                val periodEndDate = referenceEndDate.plusDays(n * 30)
                
                val stats = dbStatsByPeriod.getOrPut(periodEndDate) { mutableMapOf() }
                sale.items.forEach { item ->
                    // Extraer nombre comercial (ej: de "Pattaya Blue (EyeShare)" a "Pattaya Blue")
                    val lensName = item.lensName.ifBlank { item.productName.substringBefore(" (").trim() }
                    stats[lensName] = (stats[lensName] ?: 0) + (item.soldQty ?: 0)
                }
            }

            // 2. Unificar periodos estáticos y dinámicos
            val allPeriodEndDates = mutableSetOf<LocalDate>()
            staticData.keys.forEach { (y, m) -> allPeriodEndDates.add(LocalDate.of(y, m, 13)) }
            allPeriodEndDates.addAll(dbStatsByPeriod.keys)
            
            // Asegurar periodo actual
            var ongoingStart = referenceEndDate.plusDays(1)
            while(!today.isBefore(ongoingStart)) {
                allPeriodEndDates.add(ongoingStart.plusDays(29))
                ongoingStart = ongoingStart.plusDays(30)
            }

            allPeriodEndDates.map { endDate ->
                val year = endDate.year
                val month = endDate.monthValue
                val dbLensStats = dbStatsByPeriod[endDate] ?: emptyMap()
                val dbTotal = dbLensStats.values.sum()
                val staticTotal = staticData[year to month]
                val isOngoing = !today.isAfter(endDate)
                
                HistoryMonthEntry(
                    year = year,
                    monthValue = month,
                    monthName = getMonthName(month) + if (isOngoing) " (En curso)" else "",
                    count = if (dbTotal > 0) dbTotal else (staticTotal ?: 0),
                    lensStats = dbLensStats.toList().sortedByDescending { it.second }.toMap(),
                    isStatic = staticTotal != null && dbTotal == 0,
                    isOngoing = isOngoing
                )
            }.sortedWith(compareByDescending<HistoryMonthEntry> { it.year }.thenByDescending { it.monthValue })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "Enero"; 2 -> "Febrero"; 3 -> "Marzo"; 4 -> "Abril"
            5 -> "Mayo"; 6 -> "Junio"; 7 -> "Julio"; 8 -> "Agosto"
            9 -> "Septiembre"; 10 -> "Octubre"; 11 -> "Noviembre"; 12 -> "Diciembre"
            else -> ""
        }
    }
}

class SalesHistoryViewModelFactory(private val saleRepo: SaleRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SalesHistoryViewModel(saleRepo) as T
}
