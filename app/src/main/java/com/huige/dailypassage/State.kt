package com.huige.dailypassage

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class Action { PAGE, CHANGE }

/**
 * 抽取引擎 + 尺寸自适应 + 点击意图。
 *
 * 随机策略：洗牌队列。把全部内容单元打乱成一个序列，依次取用，
 * 取完再重洗 —— 一轮之内绝不重复。同一天内容恒定（靠 Prefs.date 锁）。
 */
object State {

    private val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.CHINA)

    /** 跨天则推进一条。所有刷新路径都先过这里，天然自愈。 */
    fun rollToToday(ctx: Context) {
        val today = dayFormat.format(Date())
        if (Prefs.date(ctx) == today) return
        Prefs.setDate(ctx, today)
        advance(ctx)
    }

    /** 换下一条（手动「换一条」或跨天自动）。 */
    fun advance(ctx: Context) {
        val size = PassageRepo.all(ctx).size
        if (size == 0) return
        var q = Prefs.queue(ctx, size)
        var pos = Prefs.pos(ctx) + 1
        if (pos >= q.size) {
            q = shuffle(size)
            Prefs.saveQueue(ctx, q)
            pos = 0
        }
        Prefs.setPos(ctx, pos)
        Prefs.setPage(ctx, 0)
    }

    /** 翻下一页；已是最后一页则回到第一页，形成循环。 */
    fun nextPage(ctx: Context, pageCount: Int) {
        if (pageCount <= 1) {
            Prefs.setPage(ctx, 0)
            return
        }
        val cur = Prefs.page(ctx)
        Prefs.setPage(ctx, if (cur + 1 >= pageCount) 0 else cur + 1)
    }

    fun currentIndex(ctx: Context, size: Int): Int {
        val q = Prefs.queue(ctx, size)
        if (q.isEmpty()) return 0
        val pos = Prefs.pos(ctx).coerceIn(0, q.size - 1)
        return q[pos].coerceIn(0, size - 1)
    }

    private fun shuffle(size: Int): IntArray {
        val arr = IntArray(size) { it }
        for (i in arr.size - 1 downTo 1) {
            val j = Random.nextInt(i + 1)
            val t = arr[i]
            arr[i] = arr[j]
            arr[j] = t
        }
        return arr
    }

    /**
     * 按小部件在桌面上的真实尺寸估算每页能放多少字。
     * 你把小部件拉大，每页就装得多；拉小就自动少放。中文按 1 字 ≈ 1 em 估。
     */
    fun charsPerPage(ctx: Context, mgr: AppWidgetManager, widgetId: Int): Int {
        val opt = mgr.getAppWidgetOptions(widgetId)
        val wDp = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val hDp = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160)

        val width = wDp.coerceAtLeast(180)
        val height = hDp.coerceAtLeast(90)

        val fontSize = 13f
        val lineHeight = fontSize * 1.75f
        val chrome = 98f          // 角标 + 标题 + 页脚 + 内边距占掉的高度
        val textHeight = (height - chrome).coerceAtLeast(20f)

        // 行数上限与布局中 body 的 maxLines 保持一致，避免出现「算了 20 行、只显示 14 行」的丢字
        val lines = (textHeight / lineHeight).toInt().coerceIn(1, 20)
        val perLine = ((width - 26f) / fontSize).toInt().coerceAtLeast(8)
        return (lines * perLine).coerceIn(24, 600)
    }

    fun action(ctx: Context, action: Action, widgetId: Int): PendingIntent {
        val intent = Intent(ctx, WidgetActionReceiver::class.java).apply {
            this.action = action.name
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val code = widgetId * 10 + if (action == Action.PAGE) 1 else 2
        return PendingIntent.getBroadcast(ctx, code, intent, flags)
    }
}
