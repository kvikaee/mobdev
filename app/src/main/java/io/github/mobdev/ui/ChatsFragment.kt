package io.github.mobdev.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.mobdev.R
import io.github.mobdev.chat.ChatViewModel
import io.github.mobdev.chat.ChatsAdapter
import kotlinx.coroutines.launch

class ChatsFragment : Fragment(R.layout.fragment_chats) {

    private val viewModel: ChatViewModel by activityViewModels()

    private val adapter = ChatsAdapter { channel -> viewModel.openChannel(channel) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_chats)
        val progress = view.findViewById<ProgressBar>(R.id.chats_progress)
        val empty = view.findViewById<TextView>(R.id.chats_empty)
        val logoutButton = view.findViewById<Button>(R.id.button_logout)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
        logoutButton.setOnClickListener { viewModel.logout() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submit(state.channels, state.currentChannel)
                    progress.isVisible = state.isLoadingChannels
                    empty.isVisible = !state.isLoadingChannels && state.channels.isEmpty()
                }
            }
        }
    }
}
