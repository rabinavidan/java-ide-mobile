package com.javaide.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Last known cursor position for a given file, so reopening it can jump back there. */
@Entity(tableName = "editor_state")
data class EditorState(
    @PrimaryKey val filePath: String,
    val cursorLine: Int,
    val cursorColumn: Int,
    val updatedAt: Long
)
