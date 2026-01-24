package com.calendar.data

import org.threeten.bp.LocalDate

data class WorkDay(
    val date: LocalDate,
    val hours: Float = 0f
) {
    fun toKey(): String = date.toString()
}
