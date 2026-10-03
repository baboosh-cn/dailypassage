package com.huige.dailypassage

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews

/**
 * 桌面小部件本体。
 *
 * 两个可点区域：
 *   - 卡片正文（widget_root）→ 翻下一页
 *   - 底部脚注右侧「↻ 换一条」（btn_change）→ 随机换一条
 *
 * 版式随尺寸自适应：字号、行数、内边距都由 State.metrics 决定。
 * 深色模式不在这里判断 —— 交给 values-night/colors.xml 做同名覆盖。
 *
 * 注意：布局里用到的控件必须是 RemoteViews 支持的那几种。
 * 往 widget_passage.xml 里加控件前先看该文件顶部注释，
 * 用错控件（例如 <View>）编译能过，但桌面渲染时会报
 * 「载入窗口小部件时出现问题」。check_project.py 已加规则拦截。
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

    /** 用户拖动改变小部件尺寸时，重新按新尺寸排版与分页。 */
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

        /**
         * 渲染一个实例。
         *
         * 整段包了 try/catch：万一读内容、算尺寸或分页出问题，
         * 就把原因写在卡片正文里，而不是让桌面留个空白格子。
         * （桌面侧的渲染失败 —— 比如用了不支持的控件 —— 这边抓不到，
         * 那种只能靠 check_project.py 在提交前拦下来。）
         */
        fun render(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_passage)
            try {
                val m = State.metrics(context, mgr, widgetId)
                applyMetrics(views, m)

                val units = PassageRepo.all(context)
                if (units.isEmpty()) {
                    views.setTextViewText(R.id.body, "内容库为空")
                } else {
                    val passage = units[State.currentIndex(context, units.size)]
                    val pages = Paginator.paginate(passage.text, m.charsPerPage)
                    val page = Prefs.page(context).coerceIn(0, pages.size - 1)

                    // 正文是唯一主角；条目名与页码都降级成底部脚注
                    views.setTextViewText(R.id.body, pages[page])
                    views.setTextViewText(R.id.foot, "${passage.id}　${passage.name}")
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
                }
            } catch (t: Throwable) {
                views.setTextViewText(R.id.body, "渲染异常：${t.javaClass.simpleName}")
                views.setTextViewText(R.id.foot, t.message?.take(60) ?: "")
                views.setTextViewText(R.id.pageInfo, "")
            }
            mgr.updateAppWidget(widgetId, views)
        }

        /** 把尺寸参数写进 RemoteViews：字号与内边距。 */
        private fun applyMetrics(views: RemoteViews, m: Metrics) {
            views.setTextViewTextSize(R.id.body, TypedValue.COMPLEX_UNIT_SP, m.fontSize)
            views.setTextViewTextSize(R.id.foot, TypedValue.COMPLEX_UNIT_SP, m.metaSize)
            views.setTextViewTextSize(R.id.pageInfo, TypedValue.COMPLEX_UNIT_SP, m.metaSize)
            views.setTextViewTextSize(R.id.btn_change, TypedValue.COMPLEX_UNIT_SP, m.metaSize)
            views.setViewPadding(R.id.widget_root, m.padPx, m.padPx, m.padPx, m.padPx)
        }
    }
}
