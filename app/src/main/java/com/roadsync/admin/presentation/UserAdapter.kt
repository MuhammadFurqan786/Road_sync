package com.roadsync.admin.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.roadsync.R
import com.roadsync.auth.model.User
import com.roadsync.databinding.ItemLuserListBinding

class UserAdapter(
    private var users: List<User>,
    private val onStatusChange: (User, Boolean) -> Unit,
    private val onDetailsClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemLuserListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.userName.text = user.name
            binding.userPhone.text = user.phone

            // Load profile image
            Glide.with(binding.root.context)
                .load(user.profileImage)
                .placeholder(R.drawable.user_image) // While loading
                .error(R.drawable.user_image)       // If load fails
                .fallback(R.drawable.user_image)    // If URL is null
                .circleCrop()
                .into(binding.userImage)

            binding.statusSwitch.isChecked = user.active
            binding.userEmail.text = user.email

            binding.statusSwitch.setOnCheckedChangeListener { _, isChecked ->
                onStatusChange(user, isChecked)
                user.active = isChecked
            }

            binding.detailsButton.setOnClickListener {
                onDetailsClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding =
            ItemLuserListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    fun updateList(newList: List<User>) {
        users = newList
        notifyDataSetChanged()
    }
}
