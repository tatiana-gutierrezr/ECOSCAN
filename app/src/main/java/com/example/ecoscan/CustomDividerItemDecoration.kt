package com.example.ecoscan

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView

class CustomDividerItemDecoration(
    context: Context,
    private val dividerDrawable: Drawable
) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        // Iteramos sobre los items visibles
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)

            // Calculamos si el item actual no es el último
            if (i < parent.adapter?.itemCount?.minus(1) ?: 0) {
                // Calculamos la posición del divisor
                val params = child.layoutParams as RecyclerView.LayoutParams
                val top = child.bottom + params.bottomMargin
                val bottom = top + dividerDrawable.intrinsicHeight

                // Dibujamos el divisor solo si no es el último item
                dividerDrawable.setBounds(left, top, right, bottom)
                dividerDrawable.draw(c)
            }
        }
    }
}