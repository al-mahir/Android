package com.example.mushaf.domain.model.recite.local

/** Levenshtein distance, with a windowed overload so the streaming matcher can score a slice of a
 * long transcript without copying it. */
internal object EditDistance {

    fun between(a: String, b: String): Int = between(a, 0, a.length, b)

    /** Distance between `a[fromIndex, toIndex)` and the whole of [b]. */
    fun between(a: String, fromIndex: Int, toIndex: Int, b: String): Int {
        val aLength = toIndex - fromIndex
        if (aLength == 0) return b.length
        if (b.isEmpty()) return aLength

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..aLength) {
            current[0] = i
            val aChar = a[fromIndex + i - 1]
            for (j in 1..b.length) {
                val cost = if (aChar == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
