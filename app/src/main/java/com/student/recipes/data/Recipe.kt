package com.student.recipes.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** recipes.json içindeki tek bir tarif. */
data class Recipe(
    val id: Int,
    val title: String,
    val category: String,
    val tags: List<String>,
    val prepTime: String,
    val calories: Int,
    val protein: String,
    val carbs: String,
    val fats: String,
    val image: String,
    val ingredients: List<String>,
    val quantities: List<String>,
    val instructions: List<String>
)

/** Pantry Matcher'da bir tarifin eşleşme sonucu (HTML'deki countMatched / totalReq / percent). */
data class MatchInfo(
    val countMatched: Int,
    val totalReq: Int,
    val percent: Int
)

object RecipeRepository {

    fun loadFromAssets(context: Context, fileName: String = "recipes.json"): List<Recipe> {
        val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        return List(array.length()) { parse(array.getJSONObject(it)) }
    }

    private fun parse(o: JSONObject) = Recipe(
        id = o.optInt("id"),
        title = o.optString("title"),
        category = o.optString("category"),
        tags = o.stringList("tags"),
        prepTime = o.optString("prepTime"),
        calories = o.optInt("calories"),
        protein = o.optString("protein"),
        carbs = o.optString("carbs"),
        fats = o.optString("fats"),
        image = o.optString("image"),
        ingredients = o.stringList("ingredients"),
        quantities = o.stringList("quantities"),
        instructions = o.stringList("instructions")
    )

    private fun JSONObject.stringList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return List(arr.length()) { arr.optString(it) }
    }

    /** HTML'deki renderMatchedRecipes hesabı: eşleşen malzeme sayısı ve yüzde. */
    fun match(recipe: Recipe, pantry: Set<String>): MatchInfo {
        val total = recipe.ingredients.size
        val matched = recipe.ingredients.count { it in pantry }
        val percent = if (total == 0) 0 else Math.round(matched.toDouble() / total * 100).toInt()
        return MatchInfo(matched, total, percent)
    }
}
