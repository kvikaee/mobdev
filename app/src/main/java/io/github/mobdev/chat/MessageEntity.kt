package io.github.mobdev.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: Long,
    val channel: String,
    val from: String?,
    val to: String?,
    val text: String?,
    val imagePath: String?,
    val time: Long,
)