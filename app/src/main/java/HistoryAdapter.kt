package com.example.ecoscan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HistoryAdapter(private val historyList: List<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        // Inflar el layout de cada elemento de historial
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = historyList[position]
        holder.bind(historyItem)
    }

    override fun getItemCount(): Int = historyList.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewResult: ImageView = itemView.findViewById(R.id.imageViewResult)
        private val textViewResult: TextView = itemView.findViewById(R.id.textViewResult)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)

        fun bind(historyItem: HistoryItem) {
            textViewResult.text = historyItem.result
            textViewDate.text = historyItem.date

            // Cargar imagen desde URL usando Glide
            Glide.with(itemView.context)
                .load(historyItem.imageUrl)
                .into(imageViewResult)
        }
    }
}