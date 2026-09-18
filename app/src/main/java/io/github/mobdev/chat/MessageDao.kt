package io.github.mobdev.chat

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query(
        """
        SELECT * FROM messages
        WHERE channel = :channel
        ORDER BY time ASC
        """
    )
    fun observeMessages(channel: String): Flow<List<MessageEntity>>

    @Upsert
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Query(
        """
        DELETE FROM messages
        WHERE id = :messageId
        """
    )
    suspend fun deleteMessage(messageId: Long)
}