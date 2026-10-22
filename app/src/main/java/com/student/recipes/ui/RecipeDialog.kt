package com.student.recipes.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.student.recipes.R
import com.student.recipes.data.Recipe

/** HTML'deki #recipe-modal: tarif detayları, malzemeler ("In Pantry" işaretli) ve adım adım tarif. */
class RecipeDialog(
    context: Context,
    private val recipe: Recipe,
    private val pantry: Set<String>
) : Dialog(context, R.style.Theme_PantryChef_RecipeDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_recipe)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCanceledOnTouchOutside(true)

        val terracotta = ContextCompat.getColor(context, R.color.terracotta)

        // Arka plana tıklayınca kapat, karta tıklayınca kapatma
        findViewById<View>(R.id.modalScrim).setOnClickListener { dismiss() }
        findViewById<View>(R.id.modalCard).setOnClickListener { }
        findViewById<View>(R.id.modalClose).setOnClickListener { dismiss() }

        RecipeAdapter.loadRecipeImage(findViewById(R.id.modalImage), recipe.image)
        findViewById<TextView>(R.id.modalCategory).text = recipe.category
        findViewById<TextView>(R.id.modalTitle).text = recipe.title

        bindStat(R.id.statTime, R.string.stat_time,
            Icons.iconText(context, R.string.fa_clock, recipe.prepTime, color = terracotta, regularStyle = true, gap = " "))
        bindStat(R.id.statCalories, R.string.stat_calories,
            Icons.iconText(context, R.string.fa_fire, recipe.calories.toString(), color = terracotta, gap = " "))
        bindStat(R.id.statProtein, R.string.stat_protein, recipe.protein)
        bindStat(R.id.statCarbsFat, R.string.stat_carbs_fat, "${recipe.carbs} / ${recipe.fats}")

        findViewById<TextView>(R.id.ingredientsHeading).text = Icons.iconText(
            context, R.string.fa_basket, context.getString(R.string.required_ingredients),
            color = terracotta, iconScale = 0.9f
        )
        findViewById<TextView>(R.id.stepsHeading).text = Icons.iconText(
            context, R.string.fa_list_check, context.getString(R.string.step_by_step),
            color = terracotta, iconScale = 0.9f
        )

        val inflater = LayoutInflater.from(context)

        val ingredientsList = findViewById<LinearLayout>(R.id.ingredientsList)
        recipe.quantities.forEach { q ->
            // HTML ile aynı kural: miktar satırının içinde geçen ilk malzeme adı kilerde mi?
            val ingredient = recipe.ingredients.firstOrNull { q.lowercase().contains(it.lowercase()) }
            val isPantry = ingredient != null && ingredient in pantry

            val row = inflater.inflate(R.layout.item_ingredient_row, ingredientsList, false)
            val text = row.findViewById<TextView>(R.id.ingredientText)
            text.text = q
            if (isPantry) {
                row.setBackgroundResource(R.drawable.bg_ingredient_pantry)
                text.setTextColor(ContextCompat.getColor(context, R.color.charcoal))
                text.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_medium)
            } else {
                row.findViewById<View>(R.id.inPantryBadge).visibility = View.GONE
            }
            ingredientsList.addView(row)
        }

        val stepsList = findViewById<LinearLayout>(R.id.stepsList)
        recipe.instructions.forEachIndexed { idx, step ->
            val row = inflater.inflate(R.layout.item_instruction_step, stepsList, false)
            row.findViewById<TextView>(R.id.stepNumber).text = (idx + 1).toString()
            row.findViewById<TextView>(R.id.stepText).text = step
            stepsList.addView(row)
        }
    }

    private fun bindStat(containerId: Int, @StringRes label: Int, value: CharSequence) {
        val container = findViewById<View>(containerId)
        container.findViewById<TextView>(R.id.statLabel).setText(label)
        container.findViewById<TextView>(R.id.statValue).text = value
    }
}
