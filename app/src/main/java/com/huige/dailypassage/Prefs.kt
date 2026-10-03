package com.huige.dailypassage

import android.content.Context
import android.content.SharedPreferences

/**
 * 小部件全局状态。桌面上的多个小部件共用一份内容，
 * 保证「今天桌面显示的就是这一条」。
 */
object Prefs {

    private const val FILE = "daily_passage_state"
    private const val K_QUEUE = "queue"
    private const val K_POS = "pos"
    private const val K_PAGE = "page"
    private const val K_DATE = "date"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** 洗牌队列：存索引序列。长度对不上（内容库变过）就重新洗。 */
    fun queue(ctx: Context, size: Int): IntArray {
        val raw = sp(ctx).getString(K_QUEUE, null)
        val arr = raw?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toIntArray()
        if (arr != null && arr.size == size) return arr
        val fresh = IntArray(size) { it }
        saveQueue(ctx, fresh)
        return fresh
    }

    fun saveQueue(ctx: Context, q: IntArray) {
        sp(ctx).edit().putString(K_QUEUE, q.joinToString(",")).apply()
    }

    fun pos(ctx: Context): Int = sp(ctx).getInt(K_POS, 0)
    fun setPos(ctx: Context, v: Int) = sp(ctx).edit().putInt(K_POS, v).apply()

    fun page(ctx: Context): Int = sp(ctx).getInt(K_PAGE, 0)
    fun setPage(ctx: Context, v: Int) = sp(ctx).edit().putInt(K_PAGE, v).apply()

    fun date(ctx: Context): String = sp(ctx).getString(K_DATE, "") ?: ""
    fun setDate(ctx: Context, v: String) = sp(ctx).edit().putString(K_DATE, v).apply()
}
