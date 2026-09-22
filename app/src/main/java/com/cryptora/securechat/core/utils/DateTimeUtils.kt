package com.cryptora.securechat.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {
    fun formatRemainingTime(remainingMillis: Long): String {
        if (remainingMillis <= 0) return "00:00"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun isExpired(timestamp: Long, durationMillis: Long): Boolean {
        if (durationMillis <= 0) return false
        return System.currentTimeMillis() > (timestamp + durationMillis)
    }
}
