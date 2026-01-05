package com.example.socially

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socially.db.MessageEntity

/**
 * Very small message adapter used by `chat.kt` to display cached messages.
 * Keeps UI minimal: each item shows message text and timestamp.
 * Allows a long-click callback to trigger edit/delete actions in the activity.
 */
class SimpleMessageAdapter(
    private val onMessageLongClick: (MessageEntity) -> Unit = {}
) : RecyclerView.Adapter<SimpleMessageAdapter.MessageViewHolder>() {

    private var items: List<MessageEntity> = emptyList()

    fun submitList(list: List<MessageEntity>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return MessageViewHolder(v)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnLongClickListener {
            onMessageLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val line1: TextView = itemView.findViewById(android.R.id.text1)
        private val line2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(entity: MessageEntity) {
            line1.text = entity.messageText ?: "[media]"
            line2.text = "From: ${entity.senderId} • ${entity.createdAt}"
        }
    }
}
