package com.student.recipes.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import com.student.recipes.R
import kotlin.math.max

/** Çocuklarını satır satır dizen basit layout (HTML'deki "flex flex-wrap gap-2"). */
class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var horizontalSpacing = 0
    private var verticalSpacing = 0

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout, defStyleAttr, 0)
        horizontalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_pcHorizontalSpacing, 0)
        verticalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_pcVerticalSpacing, 0)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val maxLineWidth = if (widthMode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE
        else widthSize - paddingLeft - paddingRight

        var lineWidth = 0
        var lineHeight = 0
        var totalHeight = 0
        var widest = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            measureChild(child, MeasureSpec.makeMeasureSpec(maxLineWidth.coerceAtLeast(0), MeasureSpec.AT_MOST), MeasureSpec.UNSPECIFIED)
            val w = child.measuredWidth
            val h = child.measuredHeight
            if (lineWidth > 0 && lineWidth + horizontalSpacing + w > maxLineWidth) {
                widest = max(widest, lineWidth)
                totalHeight += lineHeight + verticalSpacing
                lineWidth = w
                lineHeight = h
            } else {
                lineWidth += if (lineWidth > 0) horizontalSpacing + w else w
                lineHeight = max(lineHeight, h)
            }
        }
        widest = max(widest, lineWidth)
        totalHeight += lineHeight

        val width = if (widthMode == MeasureSpec.EXACTLY) widthSize
        else (widest + paddingLeft + paddingRight).let { if (widthMode == MeasureSpec.AT_MOST) minOf(it, widthSize) else it }
        val height = resolveSize(totalHeight + paddingTop + paddingBottom, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val maxRight = r - l - paddingRight
        var x = paddingLeft
        var y = paddingTop
        var lineHeight = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val w = child.measuredWidth
            val h = child.measuredHeight
            if (x > paddingLeft && x + w > maxRight) {
                x = paddingLeft
                y += lineHeight + verticalSpacing
                lineHeight = 0
            }
            child.layout(x, y, x + w, y + h)
            x += w + horizontalSpacing
            lineHeight = max(lineHeight, h)
        }
    }
}
