package io.github.mobdev.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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
import io.github.mobdev.chat.MessagesAdapter
import kotlinx.coroutines.launch

class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private val viewModel: ChatViewModel by activityViewModels()

    private val adapter = MessagesAdapter { path -> viewModel.openImage(path) }

    private var lastChannel: String? = null
    private var scrolledToBottom = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_messages)
        val progress = view.findViewById<ProgressBar>(R.id.messages_progress)
        val input = view.findViewById<EditText>(R.id.input_message)
        val sendButton = view.findViewById<Button>(R.id.button_send)
        val backButton = view.findViewById<Button>(R.id.button_back)
        val title = view.findViewById<TextView>(R.id.messages_title)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        backButton.setOnClickListener { viewModel.closeChannel() }
        sendButton.setOnClickListener {
            viewModel.sendMessage(input.text.toString())
            input.text.clear()
        }

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(-1)) {
                    viewModel.loadMoreMessages()
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state.currentChannel != lastChannel) {
                        lastChannel = state.currentChannel
                        scrolledToBottom = false
                    }
                    title.text = state.currentChannel.orEmpty()
                    progress.isVisible = state.isLoadingMessages && state.messages.isEmpty()
                    adapter.submitList(state.messages) {
                        if (!scrolledToBottom && state.messages.isNotEmpty()) {
                            recycler.scrollToPosition(state.messages.size - 1)
                            scrolledToBottom = true
                        }
                    }
                }
            }
        }
    }
}
