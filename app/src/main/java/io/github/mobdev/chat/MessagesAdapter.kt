package io.github.mobdev.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.github.mobdev.R
import io.github.mobdev.api.ApiClient
import io.github.mobdev.api.Message

class MessagesAdapter(
    private val onImageClick: (String) -> Unit,
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).data.image != null) TYPE_IMAGE else TYPE_TEXT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_IMAGE) {
            ImageViewHolder(inflater.inflate(R.layout.item_message_image, parent, false))
        } else {
            TextViewHolder(inflater.inflate(R.layout.item_message_text, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is TextViewHolder -> holder.bind(message)
            is ImageViewHolder -> holder.bind(message, onImageClick)
        }
    }

    class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val sender: TextView = view.findViewById(R.id.message_sender)
        private val text: TextView = view.findViewById(R.id.message_text)

        fun bind(message: Message) {
            sender.text = message.from
            text.text = message.data.text?.text
        }
    }

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val sender: TextView = view.findViewById(R.id.message_sender)
        private val image: ImageView = view.findViewById(R.id.message_image)

        fun bind(message: Message, onImageClick: (String) -> Unit) {
            sender.text = message.from
            val link = message.data.image?.link
            image.load(ApiClient.thumbUrl(link.orEmpty()))
            image.setOnClickListener { link?.let(onImageClick) }
        }
    }

    private object MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean =
            oldItem.id == newItem.id && oldItem.from == newItem.from

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
            oldItem == newItem
    }

    private companion object {
        const val TYPE_TEXT = 0
        const val TYPE_IMAGE = 1
    }
}
