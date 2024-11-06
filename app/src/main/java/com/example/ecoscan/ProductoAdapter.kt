package com.example.ecoscan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(private val productos: Map<String, Int>) :
    RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val materialTextView: TextView = view.findViewById(R.id.materialTextView)
        val countTextView: TextView = view.findViewById(R.id.countTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val itemList = productos.entries.toList()
        if (position < itemList.size) {
            val item = itemList[position]
            holder.materialTextView.text = item.key
            holder.countTextView.text = item.value.toString()

            holder.itemView.setBackgroundColor(android.graphics.Color.WHITE)
        }
    }

    override fun getItemCount(): Int {
        return productos.size
    }
}