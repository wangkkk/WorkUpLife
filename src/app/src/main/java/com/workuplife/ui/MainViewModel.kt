package com.workuplife.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workuplife.data.PreferenceStore
import com.workuplife.domain.SalaryCalculator
import com.workuplife.domain.SalaryState
import com.workuplife.domain.SloganProvider
import com.workuplife.domain.WorkConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime

class MainViewModel(private val repository: PreferenceStore) : ViewModel() {

    private val ticker: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1000)
        }
    }

    private var cachedSlogan: String = ""
    private var lastSloganUpdate: LocalDateTime = LocalDateTime.MIN

    val uiState: StateFlow<MainUiState> = combine(
        repository.config,
        ticker
    ) { config, now ->
        val calculator = SalaryCalculator(config)
        
        if (cachedSlogan.isEmpty() || now.isAfter(lastSloganUpdate.plusHours(1))) {
            cachedSlogan = SloganProvider.getSlogan(config, now)
            lastSloganUpdate = now
        }

        MainUiState.Success(
            config = config,
            salaryState = calculator.calculateCurrentState(now),
            slogan = cachedSlogan,
            secondIncrement = calculator.secondSalary.toDouble(),
            now = now // 将时间戳传给 UI
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState.Loading
    )

    suspend fun updateConfig(config: WorkConfig) {
        repository.updateConfig(config)
    }
}

sealed interface MainUiState {
    object Loading : MainUiState
    data class Success(
        val config: WorkConfig,
        val salaryState: SalaryState,
        val slogan: String,
        val secondIncrement: Double,
        val now: LocalDateTime
    ) : MainUiState
}
