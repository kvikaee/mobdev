package io.github.mobdev.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.mobdev.R
import io.github.mobdev.chat.ChatViewModel
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: ChatViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loginInput = view.findViewById<EditText>(R.id.input_login)
        val passwordInput = view.findViewById<EditText>(R.id.input_password)
        val loginButton = view.findViewById<Button>(R.id.button_login)
        val progress = view.findViewById<ProgressBar>(R.id.login_progress)

        loginButton.setOnClickListener {
            viewModel.login(loginInput.text.toString(), passwordInput.text.toString())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    progress.isVisible = state.isLoggingIn
                    loginButton.isEnabled = !state.isLoggingIn
                    loginInput.isEnabled = !state.isLoggingIn
                    passwordInput.isEnabled = !state.isLoggingIn
                    if (loginInput.text.isNullOrBlank() && state.userName.isNotBlank()) {
                        loginInput.setText(state.userName)
                    }
                }
            }
        }
    }
}
