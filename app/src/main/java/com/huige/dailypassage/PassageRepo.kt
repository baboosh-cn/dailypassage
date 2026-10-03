package com.huige.dailypassage

import android.content.Context
import org.json.JSONArray

/** 一条「内容单元」= 某个工具的一个可读字段段。 */
data class Passage(
    val key: String,
    val id: String,
    val board: String,
    val name: String,
    val field: String,
    val text: String
)

/**
 * 内容库。构建期已把 md 预解析成 assets/pool.json，
 * 这里只负责读一次、缓存住，不做任何 markdown 解析。
 */
object PassageRepo {

    @Volatile
    private var cache: List<Passage>? = null

    fun all(ctx: Context): List<Passage> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val list = ArrayList<Passage>()
            val raw = ctx.applicationContext.assets
                .open("pool.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Passage(
                        key = o.optString("key"),
                        id = o.optString("id"),
                        board = shortBoard(o.optString("board")),
                        name = o.optString("name"),
                        field = o.optString("field"),
                        text = o.optString("text")
                    )
                )
            }
            cache = list
            return list
        }
    }

    /** 把「一、基本世界观（6）」压成「基本世界观」 */
    private fun shortBoard(raw: String): String {
        var s = raw
        val dot = s.indexOf('、')
        if (dot in 0..3) s = s.substring(dot + 1)
        val p = s.lastIndexOf('（')
        if (p > 0 && s.endsWith("）")) s = s.substring(0, p)
        return s.trim()
    }
}
