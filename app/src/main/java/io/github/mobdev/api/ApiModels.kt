package io.github.mobdev.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val name: String,
    val pwd: String,
)

@Serializable
data class MessageData(
    @SerialName("Text") val text: TextData? = null,
    @SerialName("Image") val image: ImageData? = null,
)

@Serializable
data class TextData(
    @SerialName("text") val text: String,
)

@Serializable
data class ImageData(
    @SerialName("link") val link: String? = null,
)

@Serializable
data class Message(
    val id: Long = 0L,
    val from: String? = null,
    val to: String? = null,
    val data: MessageData = MessageData(),
    val time: Long = 0L,
)

@Serializable
data class OutgoingMessage(
    val from: String,
    val to: String,
    val data: MessageData,
)
