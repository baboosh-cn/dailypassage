package com.huige.dailypassage

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 处理小部件上的两种点击：翻页 与 换一条。 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 点击处理整体兜底：任何一个环节出错都不该让广播接收器崩掉，
        // 崩掉的后果是这一次点击没反应、且系统日志里只留一行异常。
        try {
            when (intent.action) {
                Action.CHANGE.name -> State.advance(context)

                Action.PAGE.name -> {
                    val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                    val units = PassageRepo.all(context)
                    if (widgetId != -1 && units.isNotEmpty()) {
                        val mgr = AppWidgetManager.getInstance(context)
                        val passage = units[State.currentIndex(context, units.size)]
                        val perPage = State.metrics(context, mgr, widgetId).charsPerPage
                        val pages = Paginator.paginate(passage.text, perPage)
                        State.nextPage(context, pages.size)
                    }
                }
            }
        } catch (t: Throwable) {
            // 忽略：下面的 refreshAll 仍会把当前状态画出来
        }
        PassageWidget.refreshAll(context)
    }
}
