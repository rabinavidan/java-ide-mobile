package com.javaide.mobile.practice.model

enum class Difficulty {
    EASY,
    MEDIUM,
    HARD;

    companion object {
        /** Safe parse for external input (e.g. imported/authored content) — null, not a throw, on an unrecognized value. */
        fun fromString(value: String): Difficulty? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
