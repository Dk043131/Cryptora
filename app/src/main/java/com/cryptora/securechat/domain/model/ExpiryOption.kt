package com.cryptora.securechat.domain.model

enum class ExpiryOption(val displayName: String, val durationMillis: Long?) {
    THIRTY_SECONDS("30 seconds", 30L * 1000L),
    FIVE_MINUTES("5 minutes", 5L * 60L * 1000L),
    THIRTY_MINUTES("30 minutes", 30L * 60L * 1000L),
    ONE_HOUR("1 hour", 60L * 60L * 1000L),
    TWENTY_FOUR_HOURS("24 hours", 24L * 60L * 60L * 1000L),
    CUSTOM("Custom duration", null);

    companion object {
        fun fromDurationMillis(millis: Long?): ExpiryOption {
            if (millis == null) return CUSTOM
            return entries.firstOrNull { it.durationMillis == millis } ?: CUSTOM
        }
    }
}
