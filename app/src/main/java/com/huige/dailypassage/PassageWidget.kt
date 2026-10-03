package com.huige.dailypassage

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews

/**
 * 桌面小部件本体。
 *
 * 两个可点区域：
 *   - 卡片正文（widget_root）→ 翻下一页
 *   - 右下角「换一条」（btn_change）→ 立刻换一条
 */
class PassageWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        State.rollToToday(context)
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

    /** 用户拖动改变小部件尺寸时，重新按新尺寸分页。 */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        render(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        DailyScheduler.scheduleNext(context)
    }

    override fun onDisabled(context: Context) {
        DailyScheduler.cancel(context)
    }

    companion object {

        /** 刷新桌面上所有本小部件实例。跨天判定也走这里，保证自愈。 */
        fun refreshAll(context: Context) {
            State.rollToToday(context)
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, PassageWidget::class.java))
            ids.forEach { render(context, mgr, it) }
        }

        fun render(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_passage)
            val units = PassageRepo.all(context)

            if (units.isEmpty()) {
                views.setTextViewText(R.id.body, "内容库为空")
                mgr.updateAppWidget(widgetId, views)
                return
            }

            val passage = units[State.currentIndex(context, units.size)]
            val perPage = State.charsPerPage(context, mgr, widgetId)
            val pages = Paginator.paginate(passage.text, perPage)
            val page = Prefs.page(context).coerceIn(0, pages.size - 1)

            views.setTextViewText(R.id.title, "${passage.id}  ${passage.name}")
            views.setTextViewText(R.id.body, pages[page])
            views.setTextViewText(R.id.board, passage.board)
            views.setTextViewText(
                R.id.pageInfo,
                if (pages.size > 1) "${page + 1}/${pages.size}" else ""
            )

            views.setOnClickPendingIntent(
                R.id.widget_root, State.action(context, Action.PAGE, widgetId)
            )
            views.setOnClickPendingIntent(
                R.id.btn_change, State.action(context, Action.CHANGE, widgetId)
            )

            mgr.updateAppWidget(widgetId, views)
        }
    }
}
