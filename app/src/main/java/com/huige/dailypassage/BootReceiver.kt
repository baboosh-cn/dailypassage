package com.huige.dailypassage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 手机重启后，系统会清空所有闹钟，必须重新注册。
 * 同时顺手刷新一次桌面内容（若期间跨了天，这里就会推一条）。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            DailyScheduler.scheduleNext(context)
            PassageWidget.refreshAll(context)
        }
    }
}
