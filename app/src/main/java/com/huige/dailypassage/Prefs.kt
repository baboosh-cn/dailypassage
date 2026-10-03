package com.huige.dailypassage

import android.content.Context
import android.content.SharedPreferences

/**
 * 小部件全局状态。桌面上的多个小部件共用一份内容，
 * 保证「今天桌面显示的就是这一条」。
 */
object Prefs {

    private const val FILE = "daily_passage_state"
    private const val K_POS = "pos"
    private const val K_PAGE = "page"
    private const val K_DATE = "date"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** 当前显示的是内容库里的第几条。 */
    fun pos(ctx: Context): Int = sp(ctx).getInt(K_POS, 0)
    fun setPos(ctx: Context, v: Int) = sp(ctx).edit().putInt(K_POS, v).apply()

    /** 当前翻到第几页（0 起）。 */
    fun page(ctx: Context): Int = sp(ctx).getInt(K_PAGE, 0)
    fun setPage(ctx: Context, v: Int) = sp(ctx).edit().putInt(K_PAGE, v).apply()

    /** 上次换内容时的日期，用于「同一天内容恒定」。 */
    fun date(ctx: Context): String = sp(ctx).getString(K_DATE, "") ?: ""
    fun setDate(ctx: Context, v: String) = sp(ctx).edit().putString(K_DATE, v).apply()
}
