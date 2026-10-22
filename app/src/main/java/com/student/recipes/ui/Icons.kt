package com.student.recipes.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.student.recipes.R

/** Font Awesome ikonlarını metnin içinde göstermek için yardımcılar (HTML'deki <i class="fa-..."> karşılığı). */
object Icons {

    private var solid: Typeface? = null
    private var regular: Typeface? = null

    fun solid(context: Context): Typeface =
        solid ?: (ResourcesCompat.getFont(context, R.font.fa_solid) ?: Typeface.DEFAULT).also { solid = it }

    fun regular(context: Context): Typeface =
        regular ?: (ResourcesCompat.getFont(context, R.font.fa_regular) ?: Typeface.DEFAULT).also { regular = it }

    /**
     * İkon + metin. Örn: iconText(ctx, R.string.fa_fire, "620 kcal", color = terracotta)
     * @param iconScale ikonun metne göre boyutu (ör. text-[10px] / text-xs = 0.83)
     * @param trailing true ise ikon metnin sonuna konur (ör. "View Full Recipe →")
     */
    fun iconText(
        context: Context,
        @StringRes icon: Int,
        text: CharSequence,
        @ColorInt color: Int? = null,
        iconScale: Float = 1f,
        regularStyle: Boolean = false,
        trailing: Boolean = false,
        gap: String = "  "
    ): CharSequence {
        val glyph = context.getString(icon)
        val face = if (regularStyle) regular(context) else solid(context)
        val sb = SpannableStringBuilder()
        fun appendIcon() {
            val start = sb.length
            sb.append(glyph)
            sb.setSpan(TypefaceSpanCompat(face), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (color != null) sb.setSpan(ForegroundColorSpan(color), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (iconScale != 1f) sb.setSpan(RelativeSizeSpan(iconScale), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (trailing) {
            sb.append(text).append(gap)
            appendIcon()
        } else {
            appendIcon()
            sb.append(gap).append(text)
        }
        return sb
    }
}

/** API 28 öncesinde de çalışan Typeface span'i. */
class TypefaceSpanCompat(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(tp: TextPaint) = apply(tp)
    override fun updateMeasureState(tp: TextPaint) = apply(tp)
    private fun apply(paint: Paint) {
        paint.typeface = typeface
        paint.isFakeBoldText = false
        paint.textSkewX = 0f
    }
}
