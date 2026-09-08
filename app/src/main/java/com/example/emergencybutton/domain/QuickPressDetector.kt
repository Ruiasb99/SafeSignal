package com.example.emergencybutton.domain

/** Monotonic times only. Count distinct presses in a sliding two-second window. */
class QuickPressDetector {
    private val presses = ArrayDeque<Long>()
    private var lastPress: Long? = null

    fun press(now: Long): Boolean {
        val previous = lastPress
        if (previous != null && now - previous < 80L) return false
        lastPress = now
        while (presses.isNotEmpty() && now - presses.first() > 2_000L) presses.removeFirst()
        presses.addLast(now)
        if (presses.size < 3) return false
        presses.clear()
        return true
    }

    fun reset() { presses.clear(); lastPress = null }
}

/** Reads only establish a baseline. Never interpret a retained characteristic value as a press. */
class ButtonEventDecoder {
    private var released = false
    private var sequence: Long? = null

    fun baseline(value: String) {
        released = value.trim().equals("Idle", ignoreCase = true)
        sequence = parseSequence(value)
    }

    fun notification(value: String): Boolean {
        val text = value.trim()
        if (text.equals("Idle", ignoreCase = true)) { released = true; return false }
        if (text.equals("SOS", ignoreCase = true)) {
            val press = released
            released = false
            return press
        }
        val next = parseSequence(text) ?: return false
        val previous = sequence
        // Monotonic sequence within a connection; reset on a new connection/board boot.
        if (previous != null && next <= previous) return false
        sequence = next
        return true
    }

    fun reset() { released = false; sequence = null }
    private fun parseSequence(value: String): Long? = value.trim()
        .takeIf { it.matches(Regex("PRESS:[0-9]{1,10}")) }
        ?.substringAfter(":")?.toLongOrNull()?.takeIf { it in 0..0xFFFFFFFFL }
}
