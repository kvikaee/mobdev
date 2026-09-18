package io.github.mobdev.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mobdev.R
import io.github.mobdev.chat.ChatEvent
import io.github.mobdev.chat.ChatUiState
import io.github.mobdev.chat.ChatViewModel

@Composable
fun ChatApp(
    viewModel: ChatViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var dialogMessage by remember {
        mutableStateOf<Int?>(null)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            dialogMessage = when (event) {
                ChatEvent.InvalidCredentials ->
                    R.string.error_invalid_credentials

                ChatEvent.SessionExpired ->
                    R.string.error_session_expired

                ChatEvent.NetworkError ->
                    R.string.error_network
            }
        }
    }

    BackHandler(
        enabled = state.isImageOpen || state.currentChannel != null,
    ) {
        when {
            state.isImageOpen -> viewModel.closeImage()
            state.currentChannel != null -> viewModel.closeChannel()
        }
    }

    val orientation = LocalConfiguration.current.orientation
    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            LandscapeContent(
                state = state,
                viewModel = viewModel,
            )
        } else {
            PortraitContent(
                state = state,
                viewModel = viewModel,
            )
        }

        if (state.isImageOpen && state.imagePath != null) {
            ImageScreen(
                path = state.imagePath,
                onClose = viewModel::closeImage,
            )
        }
    }

    dialogMessage?.let { messageResId ->
        AlertDialog(
            onDismissRequest = {
                dialogMessage = null
            },
            text = {
                Text(text = stringResource(messageResId))
            },
            confirmButton = {
                Button(
                    onClick = {
                        dialogMessage = null
                    },
                ) {
                    Text(text = stringResource(R.string.action_ok))
                }
            },
        )
    }
}

@Composable
private fun PortraitContent(
    state: ChatUiState,
    viewModel: ChatViewModel,
) {
    when {
        !state.isLoggedIn -> {
            LoginScreen(
                state = state,
                onLogin = viewModel::login,
            )
        }

        state.currentChannel != null -> {
            MessagesScreen(
                state = state,
                onBack = viewModel::closeChannel,
                onSendMessage = viewModel::sendMessage,
                onLoadMore = viewModel::loadMoreMessages,
                onImageClick = viewModel::openImage,
            )
        }

        else -> {
            ChatsScreen(
                state = state,
                onChannelClick = viewModel::openChannel,
                onLogout = viewModel::logout,
            )
        }
    }
}

@Composable
private fun LandscapeContent(
    state: ChatUiState,
    viewModel: ChatViewModel,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.weight(1f),
        ) {
            if (state.isLoggedIn) {
                ChatsScreen(
                    state = state,
                    onChannelClick = viewModel::openChannel,
                    onLogout = viewModel::logout,
                )
            } else {
                LoginScreen(
                    state = state,
                    onLogin = viewModel::login,
                )
            }
        }

        androidx.compose.foundation.layout.Box(
            modifier = Modifier.weight(1f),
        ) {
            if (!state.isLoggedIn) {
                PlaceholderScreen()
            } else if (state.currentChannel != null) {
                MessagesScreen(
                    state = state,
                    onBack = viewModel::closeChannel,
                    onSendMessage = viewModel::sendMessage,
                    onLoadMore = viewModel::loadMoreMessages,
                    onImageClick = viewModel::openImage,
                )
            } else {
                PlaceholderScreen()
            }
        }
    }
}