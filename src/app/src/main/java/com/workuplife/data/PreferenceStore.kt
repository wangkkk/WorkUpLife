package com.workuplife.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.workuplife.domain.WorkConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferenceStore(private val context: Context) {

    private object Keys {
        val SALARY = doublePreferencesKey("monthly_salary")
        val START_HOUR = intPreferencesKey("start_hour")
        val START_MINUTE = intPreferencesKey("start_minute")
        val END_HOUR = intPreferencesKey("end_hour")
        val END_MINUTE = intPreferencesKey("end_minute")
        val WORK_DAYS = stringPreferencesKey("work_days")
    }

    val config: Flow<WorkConfig> = context.dataStore.data.map { prefs ->
        WorkConfig(
            monthlySalary = prefs[Keys.SALARY] ?: 0.0,
            startTime = LocalTime.of(
                prefs[Keys.START_HOUR] ?: 9,
                prefs[Keys.START_MINUTE] ?: 0
            ),
            endTime = LocalTime.of(
                prefs[Keys.END_HOUR] ?: 18,
                prefs[Keys.END_MINUTE] ?: 0
            ),
            workDays = prefs[Keys.WORK_DAYS]?.split(",")
                ?.filter { it.isNotEmpty() }
                ?.map { DayOfWeek.of(it.toInt()) }
                ?.toSet() ?: setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )
        )
    }

    suspend fun updateConfig(config: WorkConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SALARY] = config.monthlySalary
            prefs[Keys.START_HOUR] = config.startTime.hour
            prefs[Keys.START_MINUTE] = config.startTime.minute
            prefs[Keys.END_HOUR] = config.endTime.hour
            prefs[Keys.END_MINUTE] = config.endTime.minute
            prefs[Keys.WORK_DAYS] = config.workDays.joinToString(",") { it.value.toString() }
        }
    }
}
