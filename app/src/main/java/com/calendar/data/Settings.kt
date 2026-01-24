package com.calendar.data

import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

data class Settings(
    val pricePerHour: Float = 0f,
    val currency: String = "$",
    val sickDayPrice: Float = 0f,
    val hourlyRatesHistory: Map<String, Float> = emptyMap(),
    val monthStartDay: Int = 1
) {

    fun getHourlyRateForDate(date: LocalDate): Float {
        date.toString()

        if (hourlyRatesHistory.isNotEmpty()) {
            val applicableRate = hourlyRatesHistory
                .filter { (rateDateStr, _) ->
                    try {
                        val rateDate = LocalDate.parse(rateDateStr)
                        !rateDate.isAfter(date)
                    } catch (_: Exception) {
                        false
                    }
                }
                .maxByOrNull { (rateDateStr, _) ->
                    try {
                        LocalDate.parse(rateDateStr).toEpochDay()
                    } catch (_: Exception) {
                        Long.MIN_VALUE
                    }
                }
            
            if (applicableRate != null) {
                return applicableRate.value
            }
        }

        return if (pricePerHour > 0f) pricePerHour else 0f
    }

    fun getWorkPeriodBounds(month: YearMonth): Pair<LocalDate, LocalDate> {
        val d = monthStartDay.coerceIn(1, 28)
        return if (d <= 1) {
            month.atDay(1) to month.atEndOfMonth()
        } else {
            val start = month.minusMonths(1).atDay(d)
            val end = month.atDay(d).minusDays(1)
            start to end
        }
    }

    fun dateToPeriodYearMonth(date: LocalDate): YearMonth {
        val d = monthStartDay.coerceIn(1, 28)
        if (d <= 1) return YearMonth.from(date)
        return if (date.dayOfMonth >= d) {
            YearMonth.from(date).plusMonths(1)
        } else {
            YearMonth.from(date)
        }
    }
}
