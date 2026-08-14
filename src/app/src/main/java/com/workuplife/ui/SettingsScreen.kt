package com.workuplife.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.workuplife.domain.WorkConfig
import com.workuplife.ui.theme.NeonGreen
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val LocalTimeSaver = Saver<LocalTime, String>(
    save = { it.toString() },
    restore = { LocalTime.parse(it) }
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentConfig: WorkConfig,
    showBack: Boolean = true, // 新增：是否显示返回按钮
    onSave: (WorkConfig) -> Unit,
    onBack: () -> Unit
) {
    // 核心修复：使用 rememberSaveable 或固定 key，确保 ticking 导致 recompose 时输入框不失焦
    var salary by rememberSaveable { mutableStateOf(currentConfig.monthlySalary.let { if (it <= 0) "" else it.toString() }) }
    var startTime by rememberSaveable(stateSaver = LocalTimeSaver) { mutableStateOf(currentConfig.startTime) }
    var endTime by rememberSaveable(stateSaver = LocalTimeSaver) { mutableStateOf(currentConfig.endTime) }
    var workDays by remember { mutableStateOf(currentConfig.workDays) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    if (showBack) { // 只有配置已存在时才显示返回
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = salary,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) salary = it },
                    label = { Text("月薪 (元)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen)
                )
            }

            item {
                TimeRow("上班时间", startTime) { startTime = it }
            }

            item {
                TimeRow("下班时间", endTime) { endTime = it }
            }

            item {
                Text("工作日", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val isSelected = workDays.contains(day)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                workDays = if (isSelected) workDays - day else workDays + day
                            },
                            label = {
                                val dayName = when (day) {
                                    DayOfWeek.MONDAY -> "一"
                                    DayOfWeek.TUESDAY -> "二"
                                    DayOfWeek.WEDNESDAY -> "三"
                                    DayOfWeek.THURSDAY -> "四"
                                    DayOfWeek.FRIDAY -> "五"
                                    DayOfWeek.SATURDAY -> "六"
                                    DayOfWeek.SUNDAY -> "日"
                                }
                                Text(dayName)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val newConfig = WorkConfig(
                            monthlySalary = salary.toDoubleOrNull() ?: 0.0,
                            startTime = startTime,
                            endTime = endTime,
                            workDays = workDays
                        )
                        onSave(newConfig)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("保存配置", color = Color.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRow(label: String, time: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        TextButton(onClick = { showDialog = true }) {
            Text(time.format(DateTimeFormatter.ofPattern("HH:mm")), color = NeonGreen)
        }
    }

    if (showDialog) {
        val state = rememberTimePickerState(time.hour, time.minute)
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(LocalTime.of(state.hour, state.minute))
                    showDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            },
            text = {
                TimePicker(state = state)
            }
        )
    }
}
