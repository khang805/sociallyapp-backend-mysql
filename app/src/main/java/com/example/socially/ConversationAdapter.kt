package com.example.socially

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.socially.api.UserData
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ConversationAdapter(private val onClick: (UserData) -> Unit) : ListAdapter<UserData, ConversationAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UserData>() {
            override fun areItemsTheSame(oldItem: UserData, newItem: UserData): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: UserData, newItem: UserData): Boolean = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = getItem(position)
        holder.title.text = u.username
        val isOnline = u.is_online == 1  // Convert Int (0/1) to Boolean
        holder.subtitle.text = if (isOnline) "Online" else "Offline"
        holder.onlineDot.visibility = if (isOnline) View.VISIBLE else View.GONE

        // Load profile image with Picasso (caching enabled)
        val imgView = holder.avatar
        if (!u.profile_picture.isNullOrEmpty()) {
            Picasso.get().load(u.profile_picture).fit().centerCrop().into(imgView)
        } else {
            imgView.setImageResource(R.drawable.profile)
        }

        holder.itemView.setOnClickListener { onClick(u) }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: CircleImageView = itemView.findViewById(R.id.conversation_profile)
        val title: TextView = itemView.findViewById(R.id.tvUsername)
        val subtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        val onlineDot: View = itemView.findViewById(R.id.online_dot)
    }
}
