package io.github.mobdev

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.mobdev.chat.ChatEvent
import io.github.mobdev.chat.ChatUiState
import io.github.mobdev.chat.ChatViewModel
import io.github.mobdev.ui.ChatsFragment
import io.github.mobdev.ui.ImageFragment
import io.github.mobdev.ui.LoginFragment
import io.github.mobdev.ui.MessagesFragment
import io.github.mobdev.ui.PlaceholderFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            clearFragments()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBack()
                }
            },
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.events.collect { event -> showEvent(event) }
            }
        }
    }

    private fun render(state: ChatUiState) {
        if (isLandscape()) {
            renderLandscape(state)
        } else {
            renderPortrait(state)
        }

        if (state.isImageOpen && state.imagePath != null) {
            showFragment(R.id.overlay_container, FRAGMENT_IMAGE) { ImageFragment() }
        } else {
            removeFragment(FRAGMENT_IMAGE)
        }
    }

    private fun renderLandscape(state: ChatUiState) {
        if (state.isLoggedIn) {
            showFragment(R.id.main_container, FRAGMENT_CHATS) { ChatsFragment() }
            if (state.currentChannel != null) {
                showFragment(R.id.detail_container, FRAGMENT_MESSAGES) { MessagesFragment() }
                removeFragment(FRAGMENT_PLACEHOLDER)
            } else {
                showFragment(R.id.detail_container, FRAGMENT_PLACEHOLDER) { PlaceholderFragment() }
                removeFragment(FRAGMENT_MESSAGES)
            }
            removeFragment(FRAGMENT_LOGIN)
        } else {
            showFragment(R.id.main_container, FRAGMENT_LOGIN) { LoginFragment() }
            removeFragment(FRAGMENT_CHATS)
            removeFragment(FRAGMENT_MESSAGES)
            removeFragment(FRAGMENT_PLACEHOLDER)
        }
    }

    private fun renderPortrait(state: ChatUiState) {
        when {
            !state.isLoggedIn -> {
                showFragment(R.id.main_container, FRAGMENT_LOGIN) { LoginFragment() }
                removeFragment(FRAGMENT_CHATS)
                removeFragment(FRAGMENT_MESSAGES)
            }
            state.currentChannel != null -> {
                showFragment(R.id.main_container, FRAGMENT_MESSAGES) { MessagesFragment() }
                removeFragment(FRAGMENT_LOGIN)
                removeFragment(FRAGMENT_CHATS)
            }
            else -> {
                showFragment(R.id.main_container, FRAGMENT_CHATS) { ChatsFragment() }
                removeFragment(FRAGMENT_LOGIN)
                removeFragment(FRAGMENT_MESSAGES)
            }
        }
    }

    private fun handleBack() {
        val state = viewModel.state.value
        when {
            state.isImageOpen -> viewModel.closeImage()
            state.currentChannel != null -> viewModel.closeChannel()
            else -> finish()
        }
    }

    private fun showEvent(event: ChatEvent) {
        val message = getString(
            when (event) {
                ChatEvent.InvalidCredentials -> R.string.error_invalid_credentials
                ChatEvent.SessionExpired -> R.string.error_session_expired
                ChatEvent.NetworkError -> R.string.error_network
            },
        )
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(R.string.action_ok, null)
            .show()
    }

    private fun showFragment(containerId: Int, tag: String, factory: () -> Fragment) {
        if (supportFragmentManager.findFragmentByTag(tag) != null) return
        supportFragmentManager.commitNow(allowStateLoss = true) {
            replace(containerId, factory(), tag)
        }
    }

    private fun removeFragment(tag: String) {
        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: return
        supportFragmentManager.commitNow(allowStateLoss = true) {
            remove(fragment)
        }
    }

    private fun clearFragments() {
        supportFragmentManager.fragments.toList().forEach { fragment ->
            supportFragmentManager.commitNow(allowStateLoss = true) {
                remove(fragment)
            }
        }
    }

    private fun isLandscape(): Boolean =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private companion object {
        const val FRAGMENT_LOGIN = "login"
        const val FRAGMENT_CHATS = "chats"
        const val FRAGMENT_MESSAGES = "messages"
        const val FRAGMENT_IMAGE = "image"
        const val FRAGMENT_PLACEHOLDER = "placeholder"
    }
}
