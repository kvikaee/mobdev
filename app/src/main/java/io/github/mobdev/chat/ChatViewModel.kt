package io.github.mobdev.chat

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.mobdev.api.ApiClient
import io.github.mobdev.api.ImageData
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ChatViewModel(
    application: Application,
    private val savedState: SavedStateHandle,
) : AndroidViewModel(application) {

    private val credentialsStore = CredentialsStore(application)
    private val api = ApiClient.api
    private val messageDao = ChatDatabase.getInstance(application).messageDao()
    private val pendingMessageDao =
        ChatDatabase.getInstance(application).pendingMessageDao()

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = EVENTS_BUFFER)
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            sendPendingMessages()
        }
    }

    init {
        restoreImage()
        restoreSession()

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onCleared() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        super.onCleared()
    }

    fun login(name: String, password: String) {
        val normalizedName = name.trim()

        if (normalizedName.isEmpty() || password.isEmpty()) {
            _events.tryEmit(ChatEvent.InvalidCredentials)
            return
        }

        viewModelScope.launch {
            performLogin(
                normalizedName,
                password,
                saveCredentials = true,
                silent = false,
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                api.logout()
            } catch (e: IOException) {
            }
        }

        clearSession()
        credentialsStore.clear()
    }

    fun openChannel(channel: String) {
        if (
            channel == _state.value.currentChannel &&
            _state.value.messages.isNotEmpty()
        ) {
            return
        }

        savedState[KEY_CHANNEL] = channel

        _state.update {
            it.copy(
                currentChannel = channel,
                messages = emptyList(),
                isLoadingMessages = true,
                canLoadMoreMessages = true,
            )
        }

        viewModelScope.launch {
            val cachedMessages = messageDao
                .observeMessages(channel)
                .first()

            _state.update {
                it.copy(
                    messages = cachedMessages.map { message ->
                        message.toMessage()
                    },
                    isLoadingMessages = true,
                )
            }

            try {
                val page = api.messages(
                    channel,
                    PAGE_SIZE,
                    null,
                    reverse = true,
                ).reversed()

                messageDao.upsertMessages(
                    page.map { message ->
                        message.toEntity(channel)
                    },
                )

                _state.update {
                    it.copy(
                        messages = page,
                        isLoadingMessages = false,
                        canLoadMoreMessages = page.size == PAGE_SIZE,
                    )
                }

                sendPendingMessages()
            } catch (e: UnauthorizedException) {
                handleUnauthorized()
            } catch (e: HttpException) {
                _events.tryEmit(ChatEvent.NetworkError)
                _state.update {
                    it.copy(isLoadingMessages = false)
                }
            } catch (e: IOException) {
                _events.tryEmit(ChatEvent.NetworkError)
                _state.update {
                    it.copy(isLoadingMessages = false)
                }
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

        if (
            current.isLoadingMessages ||
            !current.canLoadMoreMessages ||
            current.messages.isEmpty()
        ) {
            return
        }

        val oldestId = current.messages.first().id

        _state.update {
            it.copy(isLoadingMessages = true)
        }

        launchRequest {
            val page = api
                .messages(
                    channel,
                    PAGE_SIZE,
                    oldestId,
                    reverse = true,
                )
                .reversed()

            messageDao.upsertMessages(
                page.map { message ->
                    message.toEntity(channel)
                },
            )

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

        viewModelScope.launch {
            if (!isNetworkAvailable()) {
                savePendingMessage(
                    channel = channel,
                    from = current.userName,
                    text = message,
                )
                return@launch
            }

            sendMessageToServer(
                channel = channel,
                from = current.userName,
                text = message,
            )
        }
    }

    private suspend fun sendMessageToServer(
        channel: String,
        from: String,
        text: String,
    ) {
        try {
            val outgoing = OutgoingMessage(
                from = from,
                to = channel,
                data = MessageData(
                    text = TextData(text),
                ),
            )

            val id = api
                .sendMessage(outgoing)
                .trim()
                .toLongOrNull()
                ?: 0L

            val sent = Message(
                id = id,
                from = from,
                to = channel,
                data = MessageData(
                    text = TextData(text),
                ),
                time = System.currentTimeMillis(),
            )

            messageDao.upsertMessages(
                listOf(sent.toEntity(channel)),
            )

            if (_state.value.currentChannel == channel) {
                _state.update {
                    it.copy(
                        messages = it.messages + sent,
                    )
                }
            }
        } catch (e: UnauthorizedException) {
            handleUnauthorized()
        } catch (e: HttpException) {
            savePendingMessage(
                channel = channel,
                from = from,
                text = text,
            )
        } catch (e: IOException) {
            savePendingMessage(
                channel = channel,
                from = from,
                text = text,
            )
        }
    }

    private suspend fun savePendingMessage(
        channel: String,
        from: String,
        text: String,
    ) {
        val time = System.currentTimeMillis()

        pendingMessageDao.insertMessage(
            PendingMessageEntity(
                channel = channel,
                from = from,
                text = text,
                time = time,
            ),
        )

        val localMessage = Message(
            id = -time,
            from = from,
            to = channel,
            data = MessageData(
                text = TextData(text),
            ),
            time = time,
        )

        messageDao.upsertMessages(
            listOf(localMessage.toEntity(channel)),
        )

        if (_state.value.currentChannel == channel) {
            _state.update {
                it.copy(
                    messages = it.messages + localMessage,
                )
            }
        }
    }

    private fun sendPendingMessages() {
        viewModelScope.launch {
            val pendingMessages = pendingMessageDao
                .observePendingMessages()
                .first()

            for (pending in pendingMessages) {
                if (!isNetworkAvailable()) {
                    return@launch
                }

                try {
                    val outgoing = OutgoingMessage(
                        from = pending.from,
                        to = pending.channel,
                        data = MessageData(
                            text = TextData(pending.text),
                        ),
                    )

                    val id = api
                        .sendMessage(outgoing)
                        .trim()
                        .toLongOrNull()
                        ?: 0L

                    val sent = Message(
                        id = id,
                        from = pending.from,
                        to = pending.channel,
                        data = MessageData(
                            text = TextData(pending.text),
                        ),
                        time = pending.time,
                    )

                    messageDao.deleteMessage(
                        messageId = -pending.time,
                    )

                    messageDao.upsertMessages(
                        listOf(sent.toEntity(pending.channel)),
                    )

                    pendingMessageDao.deleteMessage(pending.localId)

                } catch (e: UnauthorizedException) {
                    handleUnauthorized()
                    return@launch
                } catch (e: HttpException) {
                    return@launch
                } catch (e: IOException) {
                    return@launch
                }
            }

            val currentChannel = _state.value.currentChannel

            if (currentChannel != null) {
                val cachedMessages = messageDao
                    .observeMessages(currentChannel)
                    .first()

                _state.update {
                    it.copy(
                        messages = cachedMessages.map { message ->
                            message.toMessage()
                        },
                    )
                }
            }
        }
    }

    fun openImage(path: String) {
        savedState[KEY_IMAGE] = path

        _state.update {
            it.copy(
                isImageOpen = true,
                imagePath = path,
            )
        }
    }

    fun closeImage() {
        savedState.remove<String>(KEY_IMAGE)

        _state.update {
            it.copy(
                isImageOpen = false,
                imagePath = null,
            )
        }
    }

    private fun restoreImage() {
        val path = savedState.get<String>(KEY_IMAGE) ?: return

        _state.update {
            it.copy(
                isImageOpen = true,
                imagePath = path,
            )
        }
    }

    private fun restoreSession() {
        val credentials = credentialsStore.load() ?: return

        _state.update {
            it.copy(
                userName = credentials.name,
                isLoggingIn = true,
            )
        }

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
        _state.update {
            it.copy(isLoggingIn = true)
        }

        try {
            val newToken = api
                .login(LoginRequest(name, password))
                .trim()

            if (newToken.isEmpty()) {
                _state.update {
                    it.copy(
                        isLoggingIn = false,
                        isLoggedIn = false,
                    )
                }

                if (!silent) {
                    _events.tryEmit(ChatEvent.InvalidCredentials)
                }

                return
            }

            ApiClient.setToken(newToken)

            if (saveCredentials) {
                credentialsStore.save(name, password)
            }

            _state.update {
                it.copy(
                    userName = name,
                    isLoggedIn = true,
                    isLoggingIn = false,
                )
            }

            loadChannels()
            restoreOpenChannel()
            sendPendingMessages()

        } catch (e: UnauthorizedException) {
            _state.update {
                it.copy(
                    isLoggingIn = false,
                    isLoggedIn = false,
                )
            }

            if (!silent) {
                _events.tryEmit(ChatEvent.InvalidCredentials)
            }
        } catch (e: HttpException) {
            _state.update {
                it.copy(
                    isLoggingIn = false,
                    isLoggedIn = false,
                )
            }

            if (!silent) {
                _events.tryEmit(ChatEvent.InvalidCredentials)
            }
        } catch (e: IOException) {
            _state.update {
                it.copy(
                    isLoggingIn = false,
                    isLoggedIn = false,
                )
            }

            _events.tryEmit(ChatEvent.NetworkError)
        }
    }

    private fun loadChannels() {
        launchRequest {
            _state.update {
                it.copy(isLoadingChannels = true)
            }

            val channels = api.channels()

            _state.update {
                it.copy(
                    channels = channels,
                    isLoadingChannels = false,
                )
            }
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

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
        )
    }

    private fun handleUnauthorized() {
        clearSession()
        _events.tryEmit(ChatEvent.SessionExpired)
    }

    private fun resetLoading() {
        _state.update {
            it.copy(
                isLoadingChannels = false,
                isLoadingMessages = false,
            )
        }
    }

    private fun clearSession() {
        ApiClient.setToken(null)

        savedState.remove<String>(KEY_CHANNEL)
        savedState.remove<String>(KEY_IMAGE)

        _state.value = ChatUiState()
    }

    private fun Message.toEntity(channel: String): MessageEntity =
        MessageEntity(
            id = id,
            channel = channel,
            from = from,
            to = to,
            text = data.text?.text,
            imagePath = data.image?.link,
            time = time,
        )

    private fun MessageEntity.toMessage(): Message =
        Message(
            id = id,
            from = from,
            to = to,
            data = MessageData(
                text = text?.let { TextData(it) },
                image = imagePath?.let { ImageData(it) },
            ),
            time = time,
        )

    private companion object {
        const val PAGE_SIZE = 20
        const val EVENTS_BUFFER = 8
        const val KEY_CHANNEL = "open_channel"
        const val KEY_IMAGE = "open_image"
    }
}