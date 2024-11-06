package com.example.ecoscan

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ContactMargin: RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = 19
        outRect.top=19
        outRect.left=19
        outRect.right=19
        super.getItemOffsets(outRect, view, parent, state)
    }
}