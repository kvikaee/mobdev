package io.github.mobdev.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.mobdev.R
import io.github.mobdev.api.ApiClient

@Composable
fun ImageScreen(
    path: String?,
    onClose: () -> Unit,
) {
    if (path == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AsyncImage(
            model = ApiClient.imageUrl(path),
            contentDescription = stringResource(
                R.string.image_content_description,
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clickable {
                    onClose()
                },
            contentScale = ContentScale.Fit,
        )

        Button(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.action_close),
            )
        }
    }
}