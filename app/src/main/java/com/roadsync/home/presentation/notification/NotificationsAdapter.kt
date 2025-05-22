package com.roadsync.home.presentation.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.roadsync.R
import com.roadsync.home.models.NotificationDataModel
import com.roadsync.utils.GlobalKeys
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class NotificationAdapter(
    private val onInviteClick: (NotificationDataModel) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val items = mutableListOf<NotificationDataModel>()

    fun submitList(newList: List<NotificationDataModel>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<AppCompatTextView>(R.id.title)
        val body = itemView.findViewById<AppCompatTextView>(R.id.desc)
        val time = itemView.findViewById<AppCompatTextView>(R.id.time)

        fun bind(item: NotificationDataModel) {
            title.text = item.title
            body.text = item.body
            time.text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))

            itemView.setOnClickListener {
                if (item.type == GlobalKeys.INVITE) {
                    onInviteClick(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size
}
