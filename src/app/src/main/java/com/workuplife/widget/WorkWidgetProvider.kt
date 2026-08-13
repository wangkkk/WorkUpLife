package com.workuplife.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class WorkWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        startUpdateService(context)
    }

    override fun onEnabled(context: Context) {
        startUpdateService(context)
    }

    override fun onDisabled(context: Context) {
        context.stopService(Intent(context, WidgetUpdateService::class.java))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // 任何广播到来时（如重启、解锁），都尝试确保服务在跑
        startUpdateService(context)
    }

    private fun startUpdateService(context: Context) {
        val intent = Intent(context, WidgetUpdateService::class.java)
        context.startForegroundService(intent)
    }
}
