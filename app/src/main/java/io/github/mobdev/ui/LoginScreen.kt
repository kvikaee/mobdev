package io.github.mobdev.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.mobdev.R
import io.github.mobdev.chat.ChatUiState

@Composable
fun LoginScreen(
    state: ChatUiState,
    onLogin: (String, String) -> Unit,
) {
    var login by remember(state.userName) {
        mutableStateOf(state.userName)
    }

    var password by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.login_title),
        )

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            label = {
                Text(stringResource(R.string.hint_login))
            },
            singleLine = true,
            enabled = !state.isLoggingIn,
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            label = {
                Text(stringResource(R.string.hint_password))
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.isLoggingIn,
        )

        Button(
            onClick = {
                onLogin(login, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            enabled = !state.isLoggingIn,
        ) {
            Text(
                text = stringResource(R.string.action_login),
            )
        }

        if (state.isLoggingIn) {
            CircularProgressIndicator()
        }

        Text(
            text = stringResource(R.string.login_register_hint),
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}