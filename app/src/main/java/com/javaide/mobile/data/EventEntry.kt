package com.javaide.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per logged/activity/crash event, shown newest-first on the History screen. */
@Entity(tableName = "events")
data class EventEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val level: String,
    val category: String,
    val message: String
)
