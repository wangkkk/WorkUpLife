package com.workuplife.domain

import java.time.LocalDateTime
import kotlin.random.Random

object SloganProvider {
    private val categories = mapOf(
        "MORNING" to listOf("新的一天，加油打工人！", "又是为五斗米折腰的一天", "早安，今天的薪水正在路上"),
        "START" to listOf("胜利的晨曦，冲鸭！", "刚开工，先给自己倒杯水", "打工魂，开启今日份营业"),
        "WORKING" to listOf("当下微小积淀，长久汇聚成安稳", "每呼吸一下，账户都在变多", "摸鱼也是在赚钱", "忍耐当下平淡，细碎积淀慢慢变多"),
        "NOON" to listOf("午休时间，拒绝内卷", "充个电，下午继续收割收益", "干饭人干饭魂，午饭得吃饱"),
        "NEAR_END" to listOf("胜利的曙光就在前方！", "再坚持一下，自由就在眼前", "夕阳无限好，下班要趁早"),
        "OFF" to listOf("别卷了，下班了！", "回家躺着，钱也在走", "开启快乐私人时间！", "收工！生活才是主线任务"),
        "REST" to listOf("今天是休息日，好好休息！", "充电中，暂停打工", "尽情享受周末吧！")
    )

    fun getSlogan(config: WorkConfig, now: LocalDateTime = LocalDateTime.now()): String {
        val today = now.toLocalDate()
        if (!config.workDays.contains(today.dayOfWeek)) return categories["REST"]!!.random()

        val start = today.atTime(config.startTime)
        var end = today.atTime(config.endTime)
        if (end.isBefore(start)) end = end.plusDays(1)

        val key = when {
            now.isBefore(start.minusMinutes(30)) -> "MORNING"
            now.isBefore(start) -> "START"
            now.isAfter(end) -> "OFF"
            now.isAfter(end.minusMinutes(30)) -> "NEAR_END"
            now.hour in 12..13 -> "NOON"
            else -> "WORKING"
        }
        
        // 使用日期作为随机种子，确保同一时段内文案相对稳定，不会秒变
        val pool = categories[key] ?: categories["WORKING"]!!
        val seed = now.toLocalDate().toEpochDay().toInt() + key.hashCode()
        return pool[Random(seed).nextInt(pool.size)]
    }
}
