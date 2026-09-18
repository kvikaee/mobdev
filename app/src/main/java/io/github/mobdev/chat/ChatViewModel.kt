package io.github.mobdev.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.mobdev.api.ApiClient
import io.github.mobdev.api.LoginRequest
import io.github.mobdev.api.Message
import io.github.mobdev.api.MessageData
import io.github.mobdev.api.OutgoingMessage
import io.github.mobdev.api.TextData
import io.github.mobdev.api.UnauthorizedException
import io.github.mobdev.data.CredentialsStore
import java.io.IOException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ChatViewModel(
    application: Application,
    private val savedState: SavedStateHandle,
) : AndroidViewModel(application) {

    private val credentialsStore = CredentialsStore(application)
    private val api = ApiClient.api

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = EVENTS_BUFFER)
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    init {
        restoreImage()
        restoreSession()
    }

    fun login(name: String, password: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty() || password.isEmpty()) {
            _events.tryEmit(ChatEvent.InvalidCredentials)
            return
        }
        viewModelScope.launch {
            performLogin(normalizedName, password, saveCredentials = true, silent = false)
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                api.logout()
            } catch (e: IOException) {
                // The server may not support logout; the local session is cleared anyway.
            }
        }
        clearSession()
        credentialsStore.clear()
    }

    fun openChannel(channel: String) {
        if (channel == _state.value.currentChannel && _state.value.messages.isNotEmpty()) return
        savedState[KEY_CHANNEL] = channel
        _state.update {
            it.copy(
                currentChannel = channel,
                messages = emptyList(),
                isLoadingMessages = true,
                canLoadMoreMessages = true,
            )
        }
        launchRequest {
            val page = api.messages(channel, PAGE_SIZE, null, reverse = true).reversed()
            _state.update {
                it.copy(
                    messages = page,
                    isLoadingMessages = false,
                    canLoadMoreMessages = page.size == PAGE_SIZE,
                )
            }
        }
    }

    fun closeChannel() {
        savedState.remove<String>(KEY_CHANNEL)
        _state.update {
            it.copy(
                currentChannel = null,
                messages = emptyList(),
                isLoadingMessages = false,
                canLoadMoreMessages = true,
            )
        }
    }

    fun loadMoreMessages() {
        val current = _state.value
        val channel = current.currentChannel ?: return
        if (current.isLoadingMessages || !current.canLoadMoreMessages || current.messages.isEmpty()) return
        val oldestId = current.messages.first().id
        _state.update { it.copy(isLoadingMessages = true) }
        launchRequest {
            val page = api.messages(channel, PAGE_SIZE, oldestId, reverse = true).reversed()
            _state.update {
                it.copy(
                    messages = page + it.messages,
                    isLoadingMessages = false,
                    canLoadMoreMessages = page.size == PAGE_SIZE,
                )
            }
        }
    }

    fun sendMessage(text: String) {
        val message = text.trim()
        if (message.isEmpty()) return
        val current = _state.value
        val channel = current.currentChannel ?: return
        if (current.userName.isBlank()) return
        launchRequest {
            val outgoing = OutgoingMessage(
                from = current.userName,
                to = channel,
                data = MessageData(text = TextData(message)),
            )
            val id = api.sendMessage(outgoing).trim().toLongOrNull() ?: 0L
            val sent = Message(
                id = id,
                from = current.userName,
                to = channel,
                data = MessageData(text = TextData(message)),
                time = System.currentTimeMillis(),
            )
            _state.update { it.copy(messages = it.messages + sent) }
        }
    }

    fun openImage(path: String) {
        savedState[KEY_IMAGE] = path
        _state.update { it.copy(isImageOpen = true, imagePath = path) }
    }

    fun closeImage() {
        savedState.remove<String>(KEY_IMAGE)
        _state.update { it.copy(isImageOpen = false, imagePath = null) }
    }

    private fun restoreImage() {
        val path = savedState.get<String>(KEY_IMAGE) ?: return
        _state.update { it.copy(isImageOpen = true, imagePath = path) }
    }

    private fun restoreSession() {
        val credentials = credentialsStore.load() ?: return
        _state.update { it.copy(userName = credentials.name, isLoggingIn = true) }
        viewModelScope.launch {
            performLogin(
                credentials.name,
                credentials.password,
                saveCredentials = false,
                silent = true,
            )
        }
    }

    private suspend fun performLogin(
        name: String,
        password: String,
        saveCredentials: Boolean,
        silent: Boolean,
    ) {
        _state.update { it.copy(isLoggingIn = true) }
        try {
            val newToken = api.login(LoginRequest(name, password)).trim()
            if (newToken.isEmpty()) {
                _state.update { it.copy(isLoggingIn = false, isLoggedIn = false) }
                if (!silent) _events.tryEmit(ChatEvent.InvalidCredentials)
                return
            }
            ApiClient.setToken(newToken)
            if (saveCredentials) credentialsStore.save(name, password)
            _state.update {
                it.copy(userName = name, isLoggedIn = true, isLoggingIn = false)
            }
            loadChannels()
            restoreOpenChannel()
        } catch (e: UnauthorizedException) {
            _state.update { it.copy(isLoggingIn = false, isLoggedIn = false) }
            if (!silent) _events.tryEmit(ChatEvent.InvalidCredentials)
        } catch (e: HttpException) {
            _state.update { it.copy(isLoggingIn = false, isLoggedIn = false) }
            if (!silent) _events.tryEmit(ChatEvent.InvalidCredentials)
        } catch (e: IOException) {
            _state.update { it.copy(isLoggingIn = false, isLoggedIn = false) }
            _events.tryEmit(ChatEvent.NetworkError)
        }
    }

    private fun loadChannels() {
        launchRequest {
            _state.update { it.copy(isLoadingChannels = true) }
            val channels = api.channels()
            _state.update { it.copy(channels = channels, isLoadingChannels = false) }
        }
    }

    private fun restoreOpenChannel() {
        val channel = savedState.get<String>(KEY_CHANNEL) ?: return
        openChannel(channel)
    }

    private fun launchRequest(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: UnauthorizedException) {
                handleUnauthorized()
            } catch (e: HttpException) {
                _events.tryEmit(ChatEvent.NetworkError)
                resetLoading()
            } catch (e: IOException) {
                _events.tryEmit(ChatEvent.NetworkError)
                resetLoading()
            }
        }
    }

    private fun handleUnauthorized() {
        clearSession()
        _events.tryEmit(ChatEvent.SessionExpired)
    }

    private fun resetLoading() {
        _state.update { it.copy(isLoadingChannels = false, isLoadingMessages = false) }
    }

    private fun clearSession() {
        ApiClient.setToken(null)
        savedState.remove<String>(KEY_CHANNEL)
        savedState.remove<String>(KEY_IMAGE)
        _state.value = ChatUiState()
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val EVENTS_BUFFER = 8
        const val KEY_CHANNEL = "open_channel"
        const val KEY_IMAGE = "open_image"
    }
}
