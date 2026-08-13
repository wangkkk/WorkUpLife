package com.workuplife.domain

import java.time.LocalTime
import java.time.DayOfWeek

data class WorkConfig(
    val monthlySalary: Double = 0.0,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(18, 0),
    // 默认全选，确保初次运行即能看到效果
    val workDays: Set<DayOfWeek> = DayOfWeek.entries.toSet()
)
