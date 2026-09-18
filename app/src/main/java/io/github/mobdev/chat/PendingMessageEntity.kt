package io.github.mobdev.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0L,
    val channel: String,
    val from: String,
    val text: String,
    val time: Long,
)