package com.student.recipes

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.student.recipes.data.Recipe
import com.student.recipes.data.RecipeRepository
import com.student.recipes.ui.FlowLayout
import com.student.recipes.ui.GridSpacingDecoration
import com.student.recipes.ui.Icons
import com.student.recipes.ui.RecipeAdapter
import com.student.recipes.ui.RecipeCardItem
import com.student.recipes.ui.RecipeDialog

class MainActivity : AppCompatActivity() {

    private enum class Tab { MATCHER, EXPLORER }

    /** Recipe Explorer kategori butonları: filtre anahtarı -> etiket (HTML ile aynı sıra ve emojiler). */
    private val categoryFilters = listOf(
        "All" to "",
        "Breakfast" to "🍳 Breakfast",
        "Lunch" to "🥗 Lunch",
        "Dinner" to "🍝 Dinner",
        "Dessert" to "🍰 Dessert",
        "Quick & Easy" to "⚡ Quick & Easy",
        "Vegetarian" to "🌱 Vegetarian"
    )

    // ---- Veri ve durum (HTML'deki recipesDatabase, allIngredients, selectedPantry, currentTab, activeCategory) ----
    private var recipes: List<Recipe> = emptyList()
    private val allIngredients = mutableListOf<String>()
    private val selectedPantry = linkedSetOf("Eggs", "Milk", "Butter", "Garlic", "Tomatoes", "Bread", "Olive Oil")
    private var currentTab = Tab.MATCHER
    private var activeCategory = "All"

    // ---- View'lar ----
    private lateinit var tabMatcher: NestedScrollView
    private lateinit var tabExplorer: NestedScrollView
    private lateinit var navMatcher: TextView
    private lateinit var navExplorer: TextView
    private lateinit var pantryChips: FlowLayout
    private lateinit var customInput: EditText
    private lateinit var filterSpinner: Spinner
    private lateinit var matcherCount: TextView
    private lateinit var matcherEmpty: View
    private lateinit var explorerSearch: EditText
    private lateinit var categoryButtons: LinearLayout
    private lateinit var explorerCount: TextView
    private lateinit var explorerEmpty: View

    private val matcherAdapter = RecipeAdapter(showMatch = true) { openModal(it) }
    private val explorerAdapter = RecipeAdapter(showMatch = false) { openModal(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()

        try {
            recipes = RecipeRepository.loadFromAssets(this)
        } catch (e: Exception) {
            matcherCount.text = getString(R.string.load_error, e.message)
        }

        // Tüm tariflerdeki benzersiz malzemeler, alfabetik
        allIngredients.addAll(recipes.flatMap { it.ingredients }.distinct().sorted())

        savedInstanceState?.let { restoreState(it) }

        setupHeader()
        setupMatcher()
        setupExplorer()

        renderPantryChips()
        renderMatchedRecipes()
        filterExplorerRecipes()
        switchTab(currentTab)
    }

    private fun bindViews() {
        tabMatcher = findViewById(R.id.tabMatcher)
        tabExplorer = findViewById(R.id.tabExplorer)
        navMatcher = findViewById(R.id.navMatcher)
        navExplorer = findViewById(R.id.navExplorer)
        pantryChips = findViewById(R.id.pantryChips)
        customInput = findViewById(R.id.customIngredientInput)
        filterSpinner = findViewById(R.id.matchFilterSpinner)
        matcherCount = findViewById(R.id.matcherCountText)
        matcherEmpty = findViewById(R.id.matcherEmpty)
        explorerSearch = findViewById(R.id.explorerSearch)
        categoryButtons = findViewById(R.id.categoryButtons)
        explorerCount = findViewById(R.id.explorerCountText)
        explorerEmpty = findViewById(R.id.explorerEmpty)
    }

    // =====================================================================
    // HEADER
    // =====================================================================

    private fun setupHeader() {
        val brand = SpannableStringBuilder(getString(R.string.brand_pantry))
        val start = brand.length
        brand.append(getString(R.string.brand_chef))
        brand.setSpan(ForegroundColorSpan(color(R.color.terracotta)), start, brand.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        findViewById<TextView>(R.id.brandTitle).text = brand

        navMatcher.text = Icons.iconText(this, R.string.fa_cookie_bite, getString(R.string.tab_matcher))
        navExplorer.text = Icons.iconText(this, R.string.fa_book_open, getString(R.string.tab_explorer))

        findViewById<View>(R.id.brandLogo).setOnClickListener { switchTab(Tab.MATCHER) }
        navMatcher.setOnClickListener { switchTab(Tab.MATCHER) }
        navExplorer.setOnClickListener { switchTab(Tab.EXPLORER) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val glow = ContextCompat.getColor(this, R.color.terracotta)
            findViewById<View>(R.id.logoIcon).apply { outlineSpotShadowColor = glow; outlineAmbientShadowColor = glow }
            findViewById<View>(R.id.addToPantryButton).apply { outlineSpotShadowColor = glow; outlineAmbientShadowColor = glow }
        }
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        tabMatcher.visibility = if (tab == Tab.MATCHER) View.VISIBLE else View.GONE
        tabExplorer.visibility = if (tab == Tab.EXPLORER) View.VISIBLE else View.GONE
        styleNav(navMatcher, tab == Tab.MATCHER)
        styleNav(navExplorer, tab == Tab.EXPLORER)
    }

    private fun styleNav(button: TextView, active: Boolean) {
        button.setBackgroundResource(if (active) R.drawable.bg_nav_active else R.drawable.bg_nav_inactive)
        button.setTextColor(color(if (active) R.color.white else R.color.charcoal))
        button.elevation = if (active) dp(1).toFloat() else 0f
    }

    // =====================================================================
    // PANTRY MATCHER
    // =====================================================================

    private fun setupMatcher() {
        findViewById<TextView>(R.id.resetPantryButton).text =
            Icons.iconText(this, R.string.fa_rotate_left, getString(R.string.reset_pantry), gap = " ")
        findViewById<View>(R.id.resetPantryButton).setOnClickListener { clearSelectedPantry() }
        findViewById<View>(R.id.addToPantryButton).setOnClickListener { addCustomIngredient() }

        // Enter / klavyedeki "Tamam" -> ekle
        customInput.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_DONE || isEnter) {
                addCustomIngredient()
                true
            } else false
        }

        // "Show:" filtresi
        val options = listOf(
            getString(R.string.filter_all),
            getString(R.string.filter_exact),
            getString(R.string.filter_high)
        )
        filterSpinner.adapter = object : ArrayAdapter<String>(this, R.layout.item_spinner, options) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.text = Icons.iconText(context, R.string.fa_chevron_down, options[position],
                    color = color(R.color.charcoal_60), iconScale = 0.7f, trailing = true)
                return v
            }
        }.apply { setDropDownViewResource(R.layout.item_spinner_dropdown) }
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = renderMatchedRecipes()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        setupGrid(findViewById(R.id.matcherGrid), matcherAdapter)
        setupEmptyState(matcherEmpty, R.string.fa_utensils, R.string.no_matches_title, R.string.no_matches_body)
    }

    private fun renderPantryChips() {
        pantryChips.removeAllViews()
        val semibold = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans_semibold)
        allIngredients.forEach { ing ->
            val selected = ing in selectedPantry
            val chip = TextView(this).apply {
                typeface = semibold
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dp(14), dp(6), dp(14), dp(6))
                setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
                setTextColor(color(if (selected) R.color.white else R.color.charcoal))
                text = Icons.iconText(context, if (selected) R.string.fa_check else R.string.fa_plus, ing,
                    iconScale = 0.83f, gap = " ")
                if (selected) elevation = dp(1).toFloat()
                isClickable = true
                setOnClickListener { togglePantryItem(ing) }
            }
            pantryChips.addView(chip)
        }
    }

    private fun togglePantryItem(ing: String) {
        if (!selectedPantry.remove(ing)) selectedPantry.add(ing)
        renderPantryChips()
        renderMatchedRecipes()
    }

    private fun addCustomIngredient() {
        val value = customInput.text.toString().trim()
        if (value.isEmpty()) return
        // İlk harfi büyük yap (HTML ile aynı)
        val formatted = value.substring(0, 1).uppercase() + value.substring(1)
        if (formatted !in allIngredients) {
            allIngredients.add(formatted)
            allIngredients.sort()
        }
        selectedPantry.add(formatted)
        customInput.setText("")
        renderPantryChips()
        renderMatchedRecipes()
    }

    private fun clearSelectedPantry() {
        selectedPantry.clear()
        renderPantryChips()
        renderMatchedRecipes()
    }

    private fun renderMatchedRecipes() {
        val matched = recipes
            .map { RecipeCardItem(it, RecipeRepository.match(it, selectedPantry)) }
            .sortedWith(compareByDescending<RecipeCardItem> { it.match!!.percent }.thenByDescending { it.match!!.countMatched })

        val filtered = when (filterSpinner.selectedItemPosition) {
            1 -> matched.filter { it.match!!.percent == 100 }
            2 -> matched.filter { it.match!!.percent >= 75 }
            else -> matched
        }

        matcherCount.text = getString(R.string.matcher_count, filtered.size, selectedPantry.size)
        matcherAdapter.submit(filtered)
        matcherEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // =====================================================================
    // RECIPE EXPLORER
    // =====================================================================

    private fun setupExplorer() {
        categoryFilters.forEachIndexed { index, (key, label) ->
            val button = TextView(this).apply {
                text = if (key == "All") "All (${recipes.size})" else label
                tag = key
                typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_bold)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dp(16), dp(8), dp(16), dp(8))
                maxLines = 1
                isClickable = true
                setOnClickListener { setCategoryFilter(key) }
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (index > 0) lp.marginStart = dp(8)
            categoryButtons.addView(button, lp)
        }
        styleCategoryButtons()

        explorerSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = filterExplorerRecipes()
        })

        setupGrid(findViewById(R.id.explorerGrid), explorerAdapter)
        setupEmptyState(explorerEmpty, R.string.fa_magnifying_glass, R.string.no_search_title, R.string.no_search_body)
    }

    private fun setCategoryFilter(category: String) {
        activeCategory = category
        styleCategoryButtons()
        filterExplorerRecipes()
    }

    private fun styleCategoryButtons() {
        for (i in 0 until categoryButtons.childCount) {
            val button = categoryButtons.getChildAt(i) as TextView
            val active = button.tag == activeCategory
            button.setBackgroundResource(if (active) R.drawable.bg_cat_active else R.drawable.bg_cat_inactive)
            button.setTextColor(color(if (active) R.color.white else R.color.charcoal))
            button.elevation = if (active) dp(1).toFloat() else 0f
        }
    }

    private fun filterExplorerRecipes() {
        val query = explorerSearch.text.toString().lowercase()
        val filtered = recipes.filter { r ->
            val matchesCat = activeCategory == "All" || r.category == activeCategory || activeCategory in r.tags
            val matchesQuery = r.title.lowercase().contains(query) ||
                r.ingredients.any { it.lowercase().contains(query) }
            matchesCat && matchesQuery
        }

        explorerCount.text = getString(R.string.explorer_count, filtered.size)
        explorerAdapter.submit(filtered.map { RecipeCardItem(it) })
        explorerEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // =====================================================================
    // MODAL
    // =====================================================================

    private fun openModal(recipe: Recipe) {
        RecipeDialog(this, recipe, selectedPantry.toSet()).show()
    }

    // =====================================================================
    // Yardımcılar
    // =====================================================================

    private fun setupGrid(grid: RecyclerView, adapter: RecipeAdapter) {
        val columns = resources.getInteger(R.integer.recipe_grid_columns)
        grid.layoutManager = GridLayoutManager(this, columns)
        grid.addItemDecoration(GridSpacingDecoration(columns, dp(24)))
        grid.adapter = adapter
    }

    private fun setupEmptyState(root: View, @StringRes icon: Int, @StringRes title: Int, @StringRes body: Int) {
        root.findViewById<TextView>(R.id.emptyIcon).setText(icon)
        root.findViewById<TextView>(R.id.emptyTitle).setText(title)
        root.findViewById<TextView>(R.id.emptyBody).setText(body)
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)

    private fun dp(value: Int) = Math.round(value * resources.displayMetrics.density)

    // ---- Ekran döndürmede durumu koru ----

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_PANTRY, ArrayList(selectedPantry))
        outState.putStringArrayList(KEY_INGREDIENTS, ArrayList(allIngredients))
        outState.putString(KEY_TAB, currentTab.name)
        outState.putString(KEY_CATEGORY, activeCategory)
    }

    private fun restoreState(state: Bundle) {
        state.getStringArrayList(KEY_PANTRY)?.let { selectedPantry.clear(); selectedPantry.addAll(it) }
        state.getStringArrayList(KEY_INGREDIENTS)?.let { allIngredients.clear(); allIngredients.addAll(it) }
        state.getString(KEY_TAB)?.let { currentTab = Tab.valueOf(it) }
        state.getString(KEY_CATEGORY)?.let { activeCategory = it }
    }

    companion object {
        private const val KEY_PANTRY = "pantry"
        private const val KEY_INGREDIENTS = "ingredients"
        private const val KEY_TAB = "tab"
        private const val KEY_CATEGORY = "category"
    }
}
