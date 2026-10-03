package com.huige.dailypassage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 每日精确闹钟到点触发。
 * refreshAll 内部会先做跨天判定（推一条并重置页码），再刷新桌面。
 */
class DailyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        PassageWidget.refreshAll(context)
        DailyScheduler.scheduleNext(context)   // 排下一天，保持链条不断
    }
}
