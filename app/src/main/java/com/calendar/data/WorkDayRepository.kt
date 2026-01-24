package com.calendar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.threeten.bp.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "work_calendar")

class WorkDayRepository(private val context: Context) {
    private val gson = Gson()
    
    companion object {
        private val WORK_DAYS_KEY = stringPreferencesKey("work_days")
        private val ADVANCES_KEY = stringPreferencesKey("advances")
        private val PRICE_PER_HOUR_KEY = floatPreferencesKey("price_per_hour")
        private val CURRENCY_KEY = stringPreferencesKey("currency")
        private val SICK_DAY_PRICE_KEY = floatPreferencesKey("sick_day_price")
        private val HOURLY_RATES_HISTORY_KEY = stringPreferencesKey("hourly_rates_history")
        private val MONTH_START_DAY_KEY = intPreferencesKey("month_start_day")
    }
    
    // Work Days
    val workDays: Flow<Map<String, Float>> = context.dataStore.data.map { preferences ->
        val json = preferences[WORK_DAYS_KEY] ?: "{}"
        val type = object : TypeToken<Map<String, Float>>() {}.type
        gson.fromJson(json, type) ?: emptyMap()
    }
    
    suspend fun saveWorkDay(workDay: WorkDay) {
        context.dataStore.edit { preferences ->
            val json = preferences[WORK_DAYS_KEY] ?: "{}"
            val type = object : TypeToken<MutableMap<String, Float>>() {}.type
            val map = gson.fromJson<MutableMap<String, Float>>(json, type) ?: mutableMapOf()
            map[workDay.toKey()] = workDay.hours
            preferences[WORK_DAYS_KEY] = gson.toJson(map)
        }
    }
    
    suspend fun deleteWorkDay(date: LocalDate) {
        context.dataStore.edit { preferences ->
            val json = preferences[WORK_DAYS_KEY] ?: "{}"
            val type = object : TypeToken<MutableMap<String, Float>>() {}.type
            val map = gson.fromJson<MutableMap<String, Float>>(json, type) ?: mutableMapOf()
            map.remove(date.toString())
            preferences[WORK_DAYS_KEY] = gson.toJson(map)
        }
    }
    
    // Advances
    val advances: Flow<Map<String, Float>> = context.dataStore.data.map { preferences ->
        val json = preferences[ADVANCES_KEY] ?: "{}"
        val type = object : TypeToken<Map<String, Float>>() {}.type
        gson.fromJson(json, type) ?: emptyMap()
    }
    
    suspend fun saveAdvance(date: LocalDate, amount: Float) {
        context.dataStore.edit { preferences ->
            val json = preferences[ADVANCES_KEY] ?: "{}"
            val type = object : TypeToken<MutableMap<String, Float>>() {}.type
            val map = gson.fromJson<MutableMap<String, Float>>(json, type) ?: mutableMapOf()
            if (amount > 0f) {
                map[date.toString()] = amount
            } else {
                map.remove(date.toString())
            }
            preferences[ADVANCES_KEY] = gson.toJson(map)
        }
    }
    
    suspend fun deleteAdvance(date: LocalDate) {
        context.dataStore.edit { preferences ->
            val json = preferences[ADVANCES_KEY] ?: "{}"
            val type = object : TypeToken<MutableMap<String, Float>>() {}.type
            val map = gson.fromJson<MutableMap<String, Float>>(json, type) ?: mutableMapOf()
            map.remove(date.toString())
            preferences[ADVANCES_KEY] = gson.toJson(map)
        }
    }
    
    // Settings
    val settings: Flow<Settings> = context.dataStore.data.map { preferences ->
        val pricePerHour = preferences[PRICE_PER_HOUR_KEY] ?: 0f
        val currency = preferences[CURRENCY_KEY] ?: "$"
        val sickDayPrice = preferences[SICK_DAY_PRICE_KEY] ?: 0f
        val monthStartDay = (preferences[MONTH_START_DAY_KEY] ?: 1).coerceIn(1, 28)

        val historyJson = preferences[HOURLY_RATES_HISTORY_KEY] ?: "{}"
        val historyType = object : TypeToken<Map<String, Float>>() {}.type
        val hourlyRatesHistory = gson.fromJson<Map<String, Float>>(historyJson, historyType) ?: emptyMap()

        val finalHistory = if (pricePerHour > 0f && hourlyRatesHistory.isEmpty()) {

            mapOf(LocalDate.now().toString() to pricePerHour)
        } else {
            hourlyRatesHistory
        }
        
        Settings(
            pricePerHour = pricePerHour,
            currency = currency,
            sickDayPrice = sickDayPrice,
            hourlyRatesHistory = finalHistory,
            monthStartDay = monthStartDay
        )
    }
    
    suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { preferences ->
            preferences[PRICE_PER_HOUR_KEY] = settings.pricePerHour
            preferences[CURRENCY_KEY] = settings.currency
            preferences[SICK_DAY_PRICE_KEY] = settings.sickDayPrice
            preferences[MONTH_START_DAY_KEY] = settings.monthStartDay.coerceIn(1, 28)
            val historyJson = preferences[HOURLY_RATES_HISTORY_KEY] ?: "{}"
            val historyType = object : TypeToken<MutableMap<String, Float>>() {}.type
            val history = gson.fromJson<MutableMap<String, Float>>(historyJson, historyType) ?: mutableMapOf()
            if (settings.pricePerHour > 0f) {
                history[LocalDate.now().toString()] = settings.pricePerHour
            }
            preferences[HOURLY_RATES_HISTORY_KEY] = gson.toJson(history)
        }
    }


    suspend fun addHourlyRate(startDate: LocalDate, rate: Float) {
        context.dataStore.edit { preferences ->
            val historyJson = preferences[HOURLY_RATES_HISTORY_KEY] ?: "{}"
            val historyType = object : TypeToken<MutableMap<String, Float>>() {}.type
            val history = gson.fromJson<MutableMap<String, Float>>(historyJson, historyType) ?: mutableMapOf()
            
            history[startDate.toString()] = rate
            preferences[HOURLY_RATES_HISTORY_KEY] = gson.toJson(history)
            preferences[PRICE_PER_HOUR_KEY] = rate
        }
    }

    suspend fun removeHourlyRate(startDate: LocalDate) {
        context.dataStore.edit { preferences ->
            val historyJson = preferences[HOURLY_RATES_HISTORY_KEY] ?: "{}"
            val historyType = object : TypeToken<MutableMap<String, Float>>() {}.type
            val history = gson.fromJson<MutableMap<String, Float>>(historyJson, historyType) ?: mutableMapOf()
            
            history.remove(startDate.toString())
            preferences[HOURLY_RATES_HISTORY_KEY] = gson.toJson(history)

            if (history.isEmpty()) {
                preferences[PRICE_PER_HOUR_KEY] = 0f
            } else {
                
                val lastRate = history.maxByOrNull { (dateStr, _) ->
                    try {
                        LocalDate.parse(dateStr).toEpochDay()
                    } catch (_: Exception) {
                        Long.MIN_VALUE
                    }
                }?.value ?: 0f
                preferences[PRICE_PER_HOUR_KEY] = lastRate
            }
        }
    }
}
