package com.example.socially

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

/**
 * PostAdapter - Displays posts in RecyclerView
 */
class PostAdapter(
    private val postList: MutableList<Post>,
    private val onLikeClicked: (Int) -> Unit,
    private val onSaveClicked: (Int) -> Unit,
    private val onCommentClicked: (Int) -> Unit,
    private val onShareClicked: (Int) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userAvatar: CircleImageView = view.findViewById(R.id.userAvatar)
        val username: TextView = view.findViewById(R.id.username)
        val postImage: ImageView = view.findViewById(R.id.postimage)
        val likeButton: ImageView = view.findViewById(R.id.likeButton)
        val commentButton: ImageView = view.findViewById(R.id.commentbutton)
        val shareButton: ImageView = view.findViewById(R.id.sharebutton)
        val saveButton: ImageView = view.findViewById(R.id.savebutton)
        val likesCount: TextView = view.findViewById(R.id.likesCounts)
        val caption: TextView = view.findViewById(R.id.caption_post)
        val commentCount: TextView = view.findViewById(R.id.commentCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        // Set username
        holder.username.text = post.username
        
        android.util.Log.d("PostAdapter", "Binding post ${post.postId}: username=${post.username}, imageUrl=${post.postImageUrl}")

        // Set post image
        if (post.postImageUrl.isNotEmpty()) {
            if (post.postImageUrl.startsWith("http")) {
                // Load from URL
                android.util.Log.d("PostAdapter", "Loading image from URL: ${post.postImageUrl}")
                Picasso.get()
                    .load(post.postImageUrl)
                    .placeholder(R.drawable.p_post1)
                    .error(R.drawable.p_post2)
                    .resize(1080, 1080)
                    .centerCrop()
                    .into(holder.postImage, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                            android.util.Log.d("PostAdapter", "✓ Successfully loaded image for post ${post.postId}")
                        }
                        override fun onError(e: Exception?) {
                            android.util.Log.e("PostAdapter", "✗ Failed to load image for post ${post.postId}: ${e?.message}", e)
                        }
                    })
            } else {
                // Load from base64
                android.util.Log.d("PostAdapter", "Decoding base64 image for post ${post.postId}")
                try {
                    val imageBytes = Base64.decode(post.postImageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (bitmap != null) {
                        holder.postImage.setImageBitmap(bitmap)
                        android.util.Log.d("PostAdapter", "Successfully decoded base64 image")
                    } else {
                        android.util.Log.e("PostAdapter", "Bitmap is null after decoding")
                        holder.postImage.setImageResource(R.drawable.p_post1)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PostAdapter", "Error decoding base64 image: ${e.message}")
                    holder.postImage.setImageResource(R.drawable.p_post1)
                }
            }
        } else if (post.image.isNotEmpty()) {
            // Fallback to image field
            android.util.Log.d("PostAdapter", "Using fallback image field for post ${post.postId}")
            try {
                val imageBytes = Base64.decode(post.image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                android.util.Log.e("PostAdapter", "Error decoding fallback image: ${e.message}")
                holder.postImage.setImageResource(R.drawable.p_post1)
            }
        } else {
            android.util.Log.w("PostAdapter", "No image URL found for post ${post.postId}")
            holder.postImage.setImageResource(R.drawable.p_post1)
        }

        // Set avatar
        if (post.avatarUrl.isNotEmpty() && post.avatarUrl.startsWith("http")) {
            Picasso.get()
                .load(post.avatarUrl)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(holder.userAvatar)
        } else {
            holder.userAvatar.setImageResource(R.drawable.profile)
        }

        // Set likes count
        @Suppress("SetTextI18n")
        holder.likesCount.text = "${post.likes} likes"

        // Set caption
        if (post.caption.isNotEmpty()) {
            @Suppress("SetTextI18n")
            holder.caption.text = "${post.username} ${post.caption}"
            holder.caption.visibility = View.VISIBLE
        } else {
            holder.caption.visibility = View.GONE
        }

        // Set comment count
        @Suppress("SetTextI18n")
        holder.commentCount.text = "View all ${post.comments} comments"

        // Set like button state
        if (post.isLiked) {
            holder.likeButton.setImageResource(R.drawable.ic_heart_filled)
        } else {
            holder.likeButton.setImageResource(R.drawable.ic_heart_outline)
        }

        // Set save button state
        if (post.isSaved) {
            holder.saveButton.setImageResource(R.drawable.post_save_icon) // You might have a filled version
        } else {
            holder.saveButton.setImageResource(R.drawable.post_save_icon)
        }

        // Click listeners
        holder.likeButton.setOnClickListener {
            onLikeClicked(position)
        }

        holder.commentButton.setOnClickListener {
            onCommentClicked(position)
        }

        holder.shareButton.setOnClickListener {
            onShareClicked(position)
        }

        holder.saveButton.setOnClickListener {
            onSaveClicked(position)
        }
    }

    override fun getItemCount() = postList.size
}