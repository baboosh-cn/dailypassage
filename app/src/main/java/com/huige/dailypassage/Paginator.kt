package com.huige.dailypassage

/**
 * 分页器。先按上限切，再往回找一个标点符号断句，
 * 避免把句子拦腰砍断。
 */
object Paginator {

    private const val BREAKS = "。！？；!?;"

    fun paginate(text: String, charsPerPage: Int): List<String> {
        // 下限设成 8：小部件很小时也严格按算出来的容量切，
        // 否则「算 12 字却按 24 字切」会导致正文溢出、被省略号吃掉。
        val limit = charsPerPage.coerceAtLeast(8)
        if (text.length <= limit) return listOf(text)

        val pages = ArrayList<String>()
        var start = 0
        while (start < text.length) {
            var end = (start + limit).coerceAtMost(text.length)
            if (end < text.length) {
                val floor = start + limit / 2
                var cut = -1
                var i = end - 1
                while (i >= floor) {
                    if (BREAKS.indexOf(text[i]) >= 0) {
                        cut = i + 1
                        break
                    }
                    i--
                }
                if (cut > start) end = cut
            }
            val piece = text.substring(start, end).trim()
            if (piece.isNotEmpty()) pages.add(piece)
            start = end
        }
        return if (pages.isEmpty()) listOf(text) else pages
    }
}
