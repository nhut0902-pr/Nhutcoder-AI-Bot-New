package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE isImage = 1 ORDER BY timestamp DESC")
    fun getGeneratedImages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int)
}
