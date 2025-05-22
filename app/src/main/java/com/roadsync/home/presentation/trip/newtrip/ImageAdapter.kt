package com.roadsync.home.presentation.trip.newtrip

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.roadsync.R
import com.roadsync.databinding.ItemImageBinding

class ImageAdapter(private val imageList: List<String>) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageList[position]
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.img_trip)
            .error(R.drawable.img_trip)
            .into(holder.binding.image)
    }

    override fun getItemCount(): Int = imageList.size

    class ImageViewHolder(val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root)
}
