package io.github.mobdev.chat

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.mobdev.R

class ChatsAdapter(
    private val onChannelClick: (String) -> Unit,
) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

    private var channels: List<String> = emptyList()
    private var selectedChannel: String? = null

    fun submit(channels: List<String>, selectedChannel: String?) {
        this.channels = channels
        this.selectedChannel = selectedChannel
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val channel = channels[position]
        holder.bind(channel, channel == selectedChannel)
    }

    override fun getItemCount(): Int = channels.size

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val channelName: TextView = view.findViewById(R.id.channel_name)

        fun bind(channel: String, isSelected: Boolean) {
            channelName.text = channel
            channelName.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            itemView.setBackgroundColor(
                if (isSelected) ContextCompat.getColor(itemView.context, R.color.channel_selected)
                else ContextCompat.getColor(itemView.context, android.R.color.transparent)
            )
            itemView.setOnClickListener { onChannelClick(channel) }
        }
    }
}
