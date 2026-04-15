package com.example.simtec_mobileapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    private var records: List<HistoryItem>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    data class HistoryItem(
        val date: String,
        val type: String,
        val time: String,
        val isEntry: Boolean
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvType: TextView = view.findViewById(R.id.tvHistoryType)
        val tvTime: TextView = view.findViewById(R.id.tvHistoryTime)
        val ivArrow: ImageView = view.findViewById(R.id.ivHistoryArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = records[position]
        holder.tvDate.text = item.date
        holder.tvType.text = "${item.type}:"

        val color = if (item.isEntry) R.color.green_entry else R.color.coral
        holder.tvTime.text = " ${item.time}"
        holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.context, color))

        if (item.isEntry) {
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_up)
        } else {
            holder.ivArrow.setImageResource(R.drawable.ic_arrow_down)
        }

        if (position < records.size - 1) {
            val divider = View(holder.itemView.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1
                )
                setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
            }
        }
    }

    override fun getItemCount() = records.size

    fun updateRecords(newRecords: List<HistoryItem>) {
        records = newRecords
        notifyDataSetChanged()
    }
}
