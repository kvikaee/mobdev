package io.github.mobdev.chat

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMessageDao {

    @Query(
        """
        SELECT * FROM pending_messages
        ORDER BY localId ASC
        """
    )
    fun observePendingMessages(): Flow<List<PendingMessageEntity>>

    @Upsert
    suspend fun insertMessage(message: PendingMessageEntity)

    @Query(
        """
        DELETE FROM pending_messages
        WHERE localId = :localId
        """
    )
    suspend fun deleteMessage(localId: Long)
}