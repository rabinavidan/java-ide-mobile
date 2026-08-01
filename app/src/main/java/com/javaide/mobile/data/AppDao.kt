package com.javaide.mobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppDao {

    @Insert
    suspend fun insertEvent(event: EventEntry)

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    suspend fun getEvents(): List<EventEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEditorState(state: EditorState)

    @Query("SELECT * FROM editor_state WHERE filePath = :filePath LIMIT 1")
    suspend fun getEditorState(filePath: String): EditorState?

    /** Cleans up cursor-position rows left behind under a renamed/deleted project directory. */
    @Query("DELETE FROM editor_state WHERE filePath LIKE :pathPrefix || '%'")
    suspend fun deleteEditorStatesUnderPath(pathPrefix: String)
}
