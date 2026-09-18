package io.github.mobdev.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.mobdev.R
import io.github.mobdev.api.ApiClient
import io.github.mobdev.api.Message
import io.github.mobdev.chat.ChatUiState
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onLoadMore: () -> Unit,
    onImageClick: (String) -> Unit,
) {
    var input by remember {
        mutableStateOf("")
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    /*
     * После смены чата нужно один раз прокрутить список
     * к последнему сообщению.
     *
     * При повороте экрана это значение сохраняется,
     * поэтому повторной прокрутки не происходит.
     */
    var shouldScrollToBottom by remember(state.currentChannel) {
        mutableStateOf(true)
    }

    /*
     * Прокручиваем вниз только после того,
     * как сообщения действительно загрузились.
     */
    LaunchedEffect(
        state.currentChannel,
        state.messages.size,
    ) {
        if (
            shouldScrollToBottom &&
            state.messages.isNotEmpty()
        ) {
            listState.scrollToItem(state.messages.lastIndex)
            shouldScrollToBottom = false
        }
    }

    /*
     * Подгружаем старые сообщения только когда пользователь
     * реально прокручивает список вверх до самого верха.
     *
     * Это важно: при обычном создании экрана и при повороте
     * устройства запрос loadMore не выполняется.
     */
    LaunchedEffect(state.currentChannel) {
        snapshotFlow {
            listState.isScrollInProgress to listState.firstVisibleItemIndex
        }.collect { (isScrolling, firstVisibleItemIndex) ->
            if (
                isScrolling &&
                firstVisibleItemIndex == 0 &&
                !state.isLoadingMessages &&
                state.canLoadMoreMessages &&
                state.messages.isNotEmpty()
            ) {
                onLoadMore()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        /*
         * Верхняя панель:
         * Назад + название текущего чата.
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onBack,
            ) {
                Text(
                    text = stringResource(R.string.action_back),
                )
            }

            Text(
                text = state.currentChannel.orEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                fontWeight = FontWeight.Bold,
            )
        }

        /*
         * Список сообщений занимает всё оставшееся место.
         *
         * weight находится здесь, внутри Column,
         * поэтому Compose корректно понимает его.
         */
        MessageList(
            state = state,
            listState = listState,
            onImageClick = onImageClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        /*
         * Поле ввода + кнопка отправки.
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                label = {
                    Text(
                        text = stringResource(R.string.hint_message),
                    )
                },
                maxLines = 4,
            )

            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        onSendMessage(input)
                        input = ""

                        scope.launch {
                            if (state.messages.isNotEmpty()) {
                                listState.animateScrollToItem(
                                    state.messages.lastIndex,
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.padding(start = 8.dp),
                enabled = input.isNotBlank(),
            ) {
                Text(
                    text = stringResource(R.string.action_send),
                )
            }
        }
    }
}

@Composable
private fun MessageList(
    state: ChatUiState,
    listState: LazyListState,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = state.messages,
                key = { message ->
                    "${message.id}:${message.from}:${message.time}"
                },
            ) { message ->
                MessageItem(
                    message = message,
                    onImageClick = onImageClick,
                )
            }
        }

        /*
         * Если сообщения ещё не загрузились —
         * показываем индикатор по центру.
         */
        if (state.isLoadingMessages && state.messages.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
        }

        /*
         * Если уже есть сообщения и мы загружаем
         * предыдущую страницу — показываем индикатор сверху.
         */
        if (state.isLoadingMessages && state.messages.isNotEmpty()) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MessageItem(
    message: Message,
    onImageClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp,
            ),
    ) {
        Text(
            text = message.from.orEmpty(),
            modifier = Modifier.padding(bottom = 4.dp),
            fontWeight = FontWeight.Bold,
        )

        message.data.text?.let { textData ->
            Text(
                text = textData.text,
            )
        }

        message.data.image?.link?.let { path ->
            AsyncImage(
                model = ApiClient.thumbUrl(path),
                contentDescription = stringResource(
                    R.string.image_content_description,
                ),
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .heightIn(max = 200.dp)
                    .clickable {
                        onImageClick(path)
                    },
                contentScale = ContentScale.Fit,
            )
        }
    }
}