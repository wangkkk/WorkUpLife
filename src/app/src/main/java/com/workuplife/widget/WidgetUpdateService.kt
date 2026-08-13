package com.workuplife.widget

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.workuplife.R
import com.workuplife.data.PreferenceStore
import com.workuplife.domain.SalaryCalculator
import com.workuplife.domain.SloganProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.util.Locale

class WidgetUpdateService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var preferenceStore: PreferenceStore
    
    private val ticker = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferenceStore = PreferenceStore(applicationContext)
        createNotificationChannel()
        
        val initialNotification = createNotification("同步中...")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(1, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, initialNotification)
        }

        scope.launch {
            combine(preferenceStore.config, ticker) { config, now ->
                val calculator = SalaryCalculator(config)
                val state = calculator.calculateCurrentState(now)
                val slogan = SloganProvider.getSlogan(config, now)
                
                updateNotification(slogan)

                val views = RemoteViews(packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.widget_slogan, slogan)
                views.setTextViewText(R.id.widget_earnings, "¥${DecimalFormat("#,##0.00").format(state.currentEarnings)}")
                
                // 呼吸圆圈：使用 alpha 通道直接操作色值，最稳健
                val circleAlpha = if (now.second % 2 == 0) 255 else 100
                views.setInt(R.id.widget_circle, "setImageAlpha", circleAlpha)
                
                if (state.isWorking) {
                    views.setViewVisibility(R.id.widget_increment, View.VISIBLE)
                    views.setTextViewText(R.id.widget_increment, String.format(Locale.getDefault(), "+¥%.4f", calculator.secondSalary.toDouble()))
                    
                    // 脉冲效果：大小与透明度联动
                    val isEven = now.second % 2 == 0
                    val textSize = if (isEven) 15f else 13f
                    val textAlpha = if (isEven) 255 else 140
                    views.setTextViewTextSize(R.id.widget_increment, android.util.TypedValue.COMPLEX_UNIT_SP, textSize)
                    views.setTextColor(R.id.widget_increment, (textAlpha shl 24) or 0xFFA500)
                } else {
                    views.setViewVisibility(R.id.widget_increment, View.GONE)
                }
                
                views.setProgressBar(R.id.widget_progress, 1000, (state.progress * 1000).toInt(), false)

                val appWidgetManager = AppWidgetManager.getInstance(this@WidgetUpdateService)
                val componentName = ComponentName(this@WidgetUpdateService, WorkWidgetProvider::class.java)
                appWidgetManager.updateAppWidget(componentName, views)
            }.collect { }
        }
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, createNotification(content))
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, "widget_channel")
            .setContentTitle("上班鼓励器")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("widget_channel", "Widget Update", NotificationManager.IMPORTANCE_MIN)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}
