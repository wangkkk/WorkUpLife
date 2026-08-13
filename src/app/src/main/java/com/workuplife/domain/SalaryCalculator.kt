package com.workuplife.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class SalaryCalculator(private val config: WorkConfig) {

    fun calculateTotalWorkDays(yearMonth: YearMonth): Int {
        var count = 0
        for (day in 1..yearMonth.lengthOfMonth()) {
            if (config.workDays.contains(yearMonth.atDay(day).dayOfWeek)) count++
        }
        return count.coerceAtLeast(1)
    }

    fun calculatePassedWorkDays(today: LocalDate): Int {
        var count = 0
        for (day in 1 until today.dayOfMonth) {
            if (config.workDays.contains(today.withDayOfMonth(day).dayOfWeek)) count++
        }
        return count
    }

    val dailySalary: BigDecimal
        get() = BigDecimal.valueOf(config.monthlySalary)
            .divide(BigDecimal.valueOf(calculateTotalWorkDays(YearMonth.now()).toLong()), 4, RoundingMode.HALF_UP)

    val totalWorkSeconds: Long
        get() {
            var duration = Duration.between(config.startTime, config.endTime).seconds
            if (duration <= 0) duration += 24 * 3600
            return duration
        }

    val secondSalary: BigDecimal
        get() = dailySalary.divide(BigDecimal.valueOf(totalWorkSeconds), 12, RoundingMode.HALF_UP)

    val hourlySalary: BigDecimal
        get() = dailySalary.divide(BigDecimal.valueOf(totalWorkSeconds / 3600.0), 4, RoundingMode.HALF_UP)

    fun calculateCurrentState(now: LocalDateTime): SalaryState {
        val today = now.toLocalDate()
        val isWorkDaySet = config.workDays.contains(today.dayOfWeek)
        
        val start = today.atTime(config.startTime)
        var end = today.atTime(config.endTime)
        if (end.isBefore(start) || end.isEqual(start)) end = end.plusDays(1)

        val windowDuration = Duration.between(start, end).seconds
        
        // 判定今日任务是否已完成（超过下班时间）
        val isFinished = now.isAfter(end)

        val progress = when {
            now.isBefore(start) -> 0f
            isFinished -> 1.0f
            else -> Duration.between(start, now).seconds.toFloat() / windowDuration.toFloat()
        }

        val todayEarnings = if (isWorkDaySet) {
            when {
                now.isBefore(start) -> BigDecimal.ZERO
                isFinished -> dailySalary
                else -> secondSalary.multiply(BigDecimal.valueOf(Duration.between(start, now).seconds))
            }
        } else BigDecimal.ZERO

        val passedDays = calculatePassedWorkDays(today)
        val monthlyEarned = dailySalary.multiply(BigDecimal.valueOf(passedDays.toLong())).add(todayEarnings)
        
        return SalaryState(
            currentEarnings = todayEarnings,
            monthlyEarned = monthlyEarned,
            remainingDays = (calculateTotalWorkDays(YearMonth.from(today)) - passedDays - (if (isWorkDaySet) 1 else 0)).coerceAtLeast(0),
            progress = progress,
            isWorking = isWorkDaySet && now.isAfter(start) && !isFinished,
            isFinished = isFinished,
            dailySalary = dailySalary,
            hourlySalary = hourlySalary
        )
    }
}

data class SalaryState(
    val currentEarnings: BigDecimal,
    val monthlyEarned: BigDecimal,
    val remainingDays: Int,
    val progress: Float,
    val isWorking: Boolean,
    val isFinished: Boolean,
    val dailySalary: BigDecimal,
    val hourlySalary: BigDecimal
)
