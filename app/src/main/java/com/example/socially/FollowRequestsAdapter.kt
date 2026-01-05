package com.example.socially

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

/**
 * Adapter for displaying follow requests in RecyclerView
 */
class FollowRequestsAdapter(
    private val followRequestsList: List<FollowRequestData>,
    private val onAcceptClicked: (Int) -> Unit,
    private val onRejectClicked: (Int) -> Unit
) : RecyclerView.Adapter<FollowRequestsAdapter.FollowRequestViewHolder>() {

    inner class FollowRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: de.hdodenhof.circleimageview.CircleImageView =
            itemView.findViewById(R.id.userAvatar)
        private val usernameText: TextView = itemView.findViewById(R.id.username)
        private val requestTimeText: TextView = itemView.findViewById(R.id.requestTime)
        private val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        private val rejectButton: Button = itemView.findViewById(R.id.rejectButton)

        fun bind(followRequest: FollowRequestData) {
            usernameText.text = followRequest.username
            requestTimeText.text = followRequest.message

            // Load profile picture
            if (!followRequest.profilePicture.isNullOrEmpty()) {
                Picasso.get()
                    .load(followRequest.profilePicture)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.profile)
            }

            acceptButton.setOnClickListener {
                onAcceptClicked(bindingAdapterPosition)
            }

            rejectButton.setOnClickListener {
                onRejectClicked(bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_request, parent, false)
        return FollowRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowRequestViewHolder, position: Int) {
        holder.bind(followRequestsList[position])
    }

    override fun getItemCount(): Int = followRequestsList.size
}

