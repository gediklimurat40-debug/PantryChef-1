package com.student.recipes.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** Grid kartları arasında eşit boşluk (HTML'deki "gap-6"). Kenarlara boşluk eklemez. */
class GridSpacingDecoration(private val spanCount: Int, private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val column = position % spanCount
        outRect.left = column * spacing / spanCount
        outRect.right = spacing - (column + 1) * spacing / spanCount
        outRect.top = if (position >= spanCount) spacing else 0
    }
}
