package com.huige.dailypassage

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 处理小部件上的两种点击：翻页 与 换一条。 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Action.CHANGE.name -> State.advance(context)

            Action.PAGE.name -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                val units = PassageRepo.all(context)
                if (widgetId != -1 && units.isNotEmpty()) {
                    val mgr = AppWidgetManager.getInstance(context)
                    val passage = units[State.currentIndex(context, units.size)]
                    val perPage = State.charsPerPage(context, mgr, widgetId)
                    val pages = Paginator.paginate(passage.text, perPage)
                    State.nextPage(context, pages.size)
                }
            }
        }
        PassageWidget.refreshAll(context)
    }
}
