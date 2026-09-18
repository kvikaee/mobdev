package io.github.mobdev.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import io.github.mobdev.R
import io.github.mobdev.api.ApiClient
import io.github.mobdev.chat.ChatViewModel
import kotlinx.coroutines.launch

class ImageFragment : Fragment(R.layout.fragment_image) {

    private val viewModel: ChatViewModel by activityViewModels()

    private var loadedPath: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val image = view.findViewById<ImageView>(R.id.full_image)
        val closeButton = view.findViewById<View>(R.id.button_close_image)

        image.setOnClickListener { viewModel.closeImage() }
        closeButton.setOnClickListener { viewModel.closeImage() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val path = state.imagePath ?: return@collect
                    if (path != loadedPath) {
                        loadedPath = path
                        image.load(ApiClient.imageUrl(path))
                    }
                }
            }
        }
    }
}
