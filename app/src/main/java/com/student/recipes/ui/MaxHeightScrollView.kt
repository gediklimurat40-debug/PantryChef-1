package com.student.recipes.ui

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView
import com.student.recipes.R

/** Yüksekliği bir üst sınırla kısıtlanan kaydırılabilir alan (HTML'deki "max-h-48 overflow-y-auto"). */
class MaxHeightScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private var maxHeightPx = 0

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightScrollView, defStyleAttr, 0)
        maxHeightPx = a.getDimensionPixelSize(R.styleable.MaxHeightScrollView_pcMaxHeight, 0)
        a.recycle()
        isVerticalScrollBarEnabled = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var spec = heightMeasureSpec
        if (maxHeightPx > 0) {
            val size = MeasureSpec.getSize(heightMeasureSpec)
            val limit = when (MeasureSpec.getMode(heightMeasureSpec)) {
                MeasureSpec.UNSPECIFIED -> maxHeightPx
                else -> minOf(size, maxHeightPx)
            }
            spec = MeasureSpec.makeMeasureSpec(limit, MeasureSpec.AT_MOST)
        }
        super.onMeasure(widthMeasureSpec, spec)
    }
}
