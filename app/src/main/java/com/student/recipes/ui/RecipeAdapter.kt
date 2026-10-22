package com.student.recipes.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.student.recipes.R
import com.student.recipes.data.MatchInfo
import com.student.recipes.data.Recipe

/** Bir kart: tarif + (Pantry Matcher'daysa) eşleşme bilgisi. */
data class RecipeCardItem(val recipe: Recipe, val match: MatchInfo? = null)

/**
 * Pantry Matcher ve Recipe Explorer gridlerindeki tarif kartları.
 * showMatch = true -> yüzde rozeti, "You have X of Y" ve ilerleme çubuğu (matcher kartı)
 * showMatch = false -> malzeme miktarları özeti (explorer kartı)
 */
class RecipeAdapter(
    private val showMatch: Boolean,
    private val onClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.Holder>() {

    private var items: List<RecipeCardItem> = emptyList()

    fun submit(newItems: List<RecipeCardItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe_card, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.recipeImage)
        private val matchBadge: TextView = view.findViewById(R.id.matchBadge)
        private val timeBadge: TextView = view.findViewById(R.id.timeBadge)
        private val categoryTag: TextView = view.findViewById(R.id.categoryTag)
        private val calories: TextView = view.findViewById(R.id.caloriesText)
        private val title: TextView = view.findViewById(R.id.recipeTitle)
        private val subtitle: TextView = view.findViewById(R.id.recipeSubtitle)
        private val progressTrack: LinearLayout = view.findViewById(R.id.progressTrack)
        private val progressFill: View = view.findViewById(R.id.progressFill)
        private val progressRest: View = view.findViewById(R.id.progressRest)
        private val viewButton: TextView = view.findViewById(R.id.viewRecipeButton)

        init {
            progressTrack.clipToOutline = true
            val ctx = view.context
            viewButton.text = Icons.iconText(
                ctx, R.string.fa_arrow_right, ctx.getString(R.string.view_full_recipe),
                iconScale = 0.83f, trailing = true
            )
        }

        fun bind(item: RecipeCardItem) {
            val r = item.recipe
            val ctx = itemView.context
            val terracotta = ContextCompat.getColor(ctx, R.color.terracotta)

            loadRecipeImage(image, r.image)

            timeBadge.text = Icons.iconText(ctx, R.string.fa_clock, r.prepTime, regularStyle = true, gap = " ")
            categoryTag.text = r.category
            calories.text = Icons.iconText(ctx, R.string.fa_fire, "${r.calories} kcal", color = terracotta, gap = " ")
            title.text = r.title

            val match = item.match
            if (showMatch && match != null) {
                matchBadge.visibility = View.VISIBLE
                matchBadge.text = ctx.getString(R.string.match_badge, match.percent)
                val badgeColor = when {
                    match.percent == 100 -> R.color.emerald_600
                    match.percent >= 60 -> R.color.terracotta
                    else -> R.color.amber_600
                }
                matchBadge.background = pill(ContextCompat.getColor(ctx, badgeColor))

                subtitle.maxLines = Int.MAX_VALUE
                subtitle.text = HtmlCompat.fromHtml(
                    ctx.getString(R.string.you_have, match.countMatched, match.totalReq),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )

                progressTrack.visibility = View.VISIBLE
                progressFill.setBackgroundColor(
                    ContextCompat.getColor(ctx, if (match.percent == 100) R.color.emerald_500 else R.color.terracotta)
                )
                (progressFill.layoutParams as LinearLayout.LayoutParams).weight = match.percent.toFloat()
                (progressRest.layoutParams as LinearLayout.LayoutParams).weight = (100 - match.percent).toFloat()
                progressTrack.requestLayout()
            } else {
                matchBadge.visibility = View.GONE
                progressTrack.visibility = View.GONE
                subtitle.maxLines = 2
                subtitle.text = r.quantities.joinToString(", ")
            }

            itemView.setOnClickListener { onClick(r) }
        }

        private fun pill(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f * itemView.resources.displayMetrics.density
            setColor(color)
        }
    }

    companion object {
        /** HTML'deki onerror yedeği. */
        const val PLACEHOLDER_URL = "https://placehold.co/600x400/F3ECE1/2D2825/png?text=Food+Recipe"

        fun loadRecipeImage(target: ImageView, url: String) {
            Glide.with(target)
                .load(url)
                .placeholder(R.drawable.bg_image_placeholder)
                .error(Glide.with(target).load(PLACEHOLDER_URL).centerCrop())
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(target)
        }
    }
}
