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
 * 一次渲染所需的尺寸参数，由 [State.metrics] 按小部件在桌面上的真实大小算出。
 */
data class Metrics(
    val fontSize: Float,   // 正文字号 sp
    val titleSize: Float,  // 标题字号 sp
    val metaSize: Float,   // 标签 / 日期 / 页脚字号 sp
    val compact: Boolean,  // 小尺寸时隐藏顶部「标签 + 日期」行
    val padPx: Int,        // 卡片内边距（像素）
    val lines: Int,        // 正文最多显示几行
    val charsPerPage: Int  // 每页容量
)

/**
 * 抽取引擎 + 尺寸自适应 + 点击意图。
 *
 * 随机策略：**真随机**。每次「换一条」在「除当前这条之外」的全部条目里
 * 等概率取一条 —— 点一下必有变化，且长期看每条机会均等。
 * 同一天内容恒定（靠 Prefs.date 锁），避免刷新几次就跳来跳去。
 */
object State {

    private val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
    private val labelFormat = SimpleDateFormat("MM.dd", Locale.CHINA)

    /** 跨天则换一条。所有刷新路径都先过这里，天然自愈。 */
    fun rollToToday(ctx: Context) {
        val today = dayFormat.format(Date())
        if (Prefs.date(ctx) == today) return
        Prefs.setDate(ctx, today)
        advance(ctx)
    }

    /** 卡片右上角显示的日期，如「10.03」。 */
    fun todayLabel(): String = labelFormat.format(Date())

    /** 随机换下一条（手动点「换」或跨天自动）。 */
    fun advance(ctx: Context) {
        val size = PassageRepo.all(ctx).size
        if (size == 0) return
        Prefs.setPos(ctx, randomOther(size, Prefs.pos(ctx)))
        Prefs.setPage(ctx, 0)
    }

    /**
     * 在 [0, size) 内等概率取一个**不等于** current 的下标。
     * 做法：从 current 往后的 size-1 个位置里随机跳一步，
     * 天然落在「除自己以外」的全集上，且分布均匀。
     */
    private fun randomOther(size: Int, current: Int): Int {
        if (size <= 1) return 0
        val cur = current.coerceIn(0, size - 1)
        return (cur + 1 + Random.nextInt(size - 1)) % size
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
        if (size <= 0) return 0
        return Prefs.pos(ctx).coerceIn(0, size - 1)
    }

    /**
     * 按小部件在桌面上的真实尺寸算出该用的字号、行数与每页容量。
     * 拉大则字大、每页多；拉小则字小、每页少。中文按 1 字 ≈ 1 em 估算。
     */
    fun metrics(ctx: Context, mgr: AppWidgetManager, widgetId: Int): Metrics {
        val opt = mgr.getAppWidgetOptions(widgetId)
        val wDp = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        // 取 MAX_HEIGHT：竖屏下的真实高度。横竖屏切换时系统会回调
        // onAppWidgetOptionsChanged，这里会按新尺寸重算，无需特殊处理。
        val hDp = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 150)

        val fontSize = when {
            hDp >= 260 -> 15f
            hDp >= 210 -> 14f
            hDp >= 160 -> 13f
            hDp >= 125 -> 12f
            else -> 11f
        }
        // 阈值取 180dp：桌面按格子吸附，2 行高约 150dp、3 行高约 210dp，
        // 所以 180dp 能干净地把「2 行」判为小尺寸，用小内边距多挤出一点正文。
        val compact = hDp < 180
        val padDp = if (compact) 10 else 14
        val titleSize = (fontSize + 1.5f).coerceAtMost(17f)
        val metaSize = (fontSize - 2.5f).coerceAtLeast(9f)

        // 正文之外被占掉的高度。按各部分实际占高逐项累加，宁可估多（留白）
        // 也不估少 —— 估少的后果是正文溢出、被省略号吃掉字。
        val titleLine = titleSize * 1.5f          // 标题行（竖条比字矮，取字高）
        val footLine = metaSize * 1.5f + 6f       // 页脚（右侧胶囊按钮最高）
        val chrome = if (compact) {
            padDp * 2f + titleLine + 5f + 9f + 5f + footLine + 2f
        } else {
            padDp * 2f + titleLine + 7f + 13f + 8f + footLine + 4f
        }

        val lineHeight = fontSize * 1.55f
        val usable = (hDp - chrome).coerceAtLeast(lineHeight)
        // 行数上限与布局里 body 的 maxLines 保持一致
        val lines = (usable / lineHeight).toInt().coerceIn(1, 20)
        val perLine = ((wDp - padDp * 2 - 10) / fontSize).toInt().coerceAtLeast(6)

        val density = ctx.resources.displayMetrics.density
        return Metrics(
            fontSize = fontSize,
            titleSize = titleSize,
            metaSize = metaSize,
            compact = compact,
            padPx = (padDp * density).toInt(),
            lines = lines,
            charsPerPage = (lines * perLine).coerceIn(12, 600)
        )
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
