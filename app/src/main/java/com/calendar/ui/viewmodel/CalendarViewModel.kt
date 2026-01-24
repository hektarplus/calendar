package com.calendar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calendar.data.Settings
import com.calendar.data.WorkDay
import com.calendar.data.WorkDayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth


class CalendarViewModel(private val repository: WorkDayRepository) : ViewModel() {
    
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()
    
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()
    
    val workDays = repository.workDays
    val advances = repository.advances
    val settings = repository.settings
    
    val calendarData = combine(workDays, advances, settings, currentMonth) { days, advances, settings, month ->
        CalendarData(days, advances, settings, month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarData(emptyMap(), emptyMap(), Settings(), YearMonth.now())
    )
    
    fun changeMonth(months: Int) {
        _currentMonth.value = _currentMonth.value.plusMonths(months.toLong())
    }
    
    fun setMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
    }
    
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }
    
    fun clearSelection() {
        _selectedDate.value = null
    }
    
    fun saveWorkDay(date: LocalDate, hours: Float) {
        viewModelScope.launch {
            if (hours != 0f) {

                repository.saveWorkDay(WorkDay(date, hours))
            } else {
                repository.deleteWorkDay(date)
            }
        }
    }
    
    fun saveSettings(settings: Settings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }
    
    fun saveAdvance(date: LocalDate, amount: Float) {
        viewModelScope.launch {
            if (amount > 0f) {
                repository.saveAdvance(date, amount)
            } else {
                repository.deleteAdvance(date)
            }
        }
    }
    
    fun addHourlyRate(startDate: LocalDate, rate: Float) {
        viewModelScope.launch {
            repository.addHourlyRate(startDate, rate)
        }
    }
    
    fun removeHourlyRate(startDate: LocalDate) {
        viewModelScope.launch {
            repository.removeHourlyRate(startDate)
        }
    }
    
}

data class CalendarData(
    val workDays: Map<String, Float>,
    val advances: Map<String, Float>,
    val settings: Settings,
    val month: YearMonth
)

class CalendarViewModelFactory(private val repository: WorkDayRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
