package com.workuplife.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workuplife.ui.theme.NeonGreen
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: MainUiState.Success,
    onNavigateToSettings: () -> Unit
) {
    // 四个模块独立状态，默认标题和数值全隐藏（星号）
    var showMonthEarned by remember { mutableStateOf(false) }
    var showMonthRemaining by remember { mutableStateOf(false) }
    var showDailySalary by remember { mutableStateOf(false) }
    var showHourlySalary by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState() // 新增滚动状态
    val commaFormat = DecimalFormat("#,##0.00")
    val timeDisplay = uiState.now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState) // 开启滚动
            .padding(16.dp)
            .navigationBarsPadding(), // 适配导航栏避让
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "上班鼓励器", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                SloganCircle()
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = uiState.slogan, color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = timeDisplay, color = Color.Gray, fontSize = 14.sp)
        }

        // Main Card (今日收益默认显示)
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF1A2421),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("今日", color = Color.Gray, fontSize = 16.sp)
                    // 只有在工作中（未超时、是工作日）才显示秒增动效
                    if (uiState.salaryState.isWorking) {
                        JumpingIncrement(uiState.secondIncrement, trigger = uiState.now.second)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 主收益颜色随状态变化
                Text(
                    text = "¥${commaFormat.format(uiState.salaryState.currentEarnings)}",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = if (uiState.salaryState.isFinished) Color.Gray else NeonGreen
                )
                
                // 如果已满勤，显示已暂停状态
                if (uiState.salaryState.isFinished) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("○ 已暂停", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LiveIndicator()
                }

                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { uiState.salaryState.progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = if (uiState.salaryState.isFinished) Color.Gray else NeonGreen,
                    trackColor = Color.DarkGray.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.salaryState.isFinished) "今日已满勤" else "今日进度 ${String.format(Locale.getDefault(), "%.1f", uiState.salaryState.progress * 100)}%",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        // Sub Grid (四模块独立揭秘)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard(
                    label = "本月已赚",
                    value = "¥${commaFormat.format(uiState.salaryState.monthlyEarned)}",
                    isMasked = !showMonthEarned,
                    modifier = Modifier.weight(1f),
                    onClick = { showMonthEarned = !showMonthEarned }
                )
                InfoCard(
                    label = "本月剩余",
                    value = "${uiState.salaryState.remainingDays}天",
                    isMasked = !showMonthRemaining,
                    modifier = Modifier.weight(1f),
                    onClick = { showMonthRemaining = !showMonthRemaining }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard(
                    label = "日薪",
                    value = "¥${commaFormat.format(uiState.salaryState.dailySalary)}",
                    isMasked = !showDailySalary,
                    modifier = Modifier.weight(1f),
                    onClick = { showDailySalary = !showDailySalary }
                )
                InfoCard(
                    label = "时薪",
                    value = "¥${commaFormat.format(uiState.salaryState.hourlySalary)}",
                    isMasked = !showHourlySalary,
                    modifier = Modifier.weight(1f),
                    onClick = { showHourlySalary = !showHourlySalary }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3548)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("修改", color = Color.White)
        }
    }
}

@Composable
fun SloganCircle() {
    val infiniteTransition = rememberInfiniteTransition(label = "slogan_circle")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(NeonGreen.copy(alpha = alpha))
    )
}

@Composable
fun InfoCard(
    label: String, 
    value: String, 
    isMasked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isMasked) "****" else label, 
                color = Color.Gray, 
                fontSize = 12.sp
            )
            Text(
                text = if (isMasked) "****" else value, 
                color = Color.White, 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun JumpingIncrement(amount: Double, trigger: Int) {
    val alpha = remember { Animatable(0f) }
    val yOffset = remember { Animatable(0f) }
    val scale = remember { Animatable(0.5f) }

    LaunchedEffect(trigger) {
        launch {
            alpha.snapTo(1f)
            alpha.animateTo(0f, animationSpec = tween(800))
        }
        launch {
            yOffset.snapTo(0f)
            yOffset.animateTo(-20f, animationSpec = tween(800, easing = LinearOutSlowInEasing))
        }
        launch {
            scale.snapTo(0.5f)
            scale.animateTo(1.2f, animationSpec = tween(800, easing = LinearOutSlowInEasing))
        }
    }

    Text(
        text = String.format(Locale.getDefault(), "+¥%.4f", amount),
        color = Color(0xFFFFA500).copy(alpha = alpha.value),
        fontSize = 22.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .offset(y = yOffset.value.dp)
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
    )
}

@Composable
fun LiveIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(NeonGreen.copy(alpha = alpha)))
        Spacer(modifier = Modifier.width(6.dp))
        Text("LIVE", color = NeonGreen.copy(alpha = alpha), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

