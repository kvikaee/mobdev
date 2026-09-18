package io.github.mobdev.chat

import io.github.mobdev.api.Message

data class ChatUiState(
    val userName: String = "",
    val isLoggedIn: Boolean = false,
    val isLoggingIn: Boolean = false,
    val isLoadingChannels: Boolean = false,
    val channels: List<String> = emptyList(),
    val currentChannel: String? = null,
    val messages: List<Message> = emptyList(),
    val isLoadingMessages: Boolean = false,
    val canLoadMoreMessages: Boolean = true,
    val isImageOpen: Boolean = false,
    val imagePath: String? = null,
)

sealed interface ChatEvent {
    data object InvalidCredentials : ChatEvent
    data object SessionExpired : ChatEvent
    data object NetworkError : ChatEvent
}