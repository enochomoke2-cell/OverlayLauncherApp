package com.example.overlaylauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : Activity() {

    private lateinit var appsContainer: LinearLayout
    private lateinit var searchBox: EditText
    private lateinit var rootLayout: LinearLayout

    private var searchQuery = ""
    private var selectedCategory = "All"

    private val prefs by lazy {
        getSharedPreferences("ol_liquid_launcher_v3", MODE_PRIVATE)
    }

    private val gridColumns: Int
        get() = prefs.getInt("grid_columns", 4)

    private val iconSize: Int
        get() = prefs.getInt("icon_size", 58)

    private val listMode: Boolean
        get() = prefs.getBoolean("list_mode", false)

    private val favoritesOnly: Boolean
        get() = prefs.getBoolean("favorites_only", false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        rebuildApps()
    }

    private fun buildUi() {
        val frame = FrameLayout(this)

        frame.addView(
            LiquidBackground(this),
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            overScrollMode = ScrollView.OVER_SCROLL_NEVER
            isFillViewport = true
        }

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(34), dp(18), dp(24))
        }

        rootLayout.addView(topWidget())
        rootLayout.addView(searchArea())
        rootLayout.addView(quickActions())
        rootLayout.addView(dockArea())
        rootLayout.addView(categoryBar())

        appsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }

        rootLayout.addView(appsContainer)
        rootLayout.addView(todoWidget())
        rootLayout.addView(financeWidget())
        rootLayout.addView(settingsWidget())

        scroll.addView(rootLayout)
        frame.addView(
            scroll,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(frame)
        rebuildApps()
    }

    private fun topWidget(): LinearLayout {
        val card = glassCard().apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(24), dp(22), dp(24))
        }

        val time = TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            textSize = 46f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val date = TextView(this).apply {
            text = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())
            textSize = 14.5f
            setTextColor(Color.parseColor("#EEF6FF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(14))
        }

        val title = TextView(this).apply {
            text = "OL Liquid Launcher"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Pixel-style • Liquid glass • Smart apps"
            textSize = 13f
            setTextColor(Color.parseColor("#DDEBFF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }

        card.addView(time)
        card.addView(date)
        card.addView(title)
        card.addView(subtitle)

        return card
    }

    private fun searchArea(): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        searchBox = EditText(this).apply {
            hint = "Search apps, calculate, or search web"
            textSize = 15.5f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#EAF2FF"))
            background = roundedGradient(
                intArrayOf(
                    Color.parseColor("#78FFFFFF"),
                    Color.parseColor("#35FFFFFF"),
                    Color.parseColor("#18FFFFFF")
                ),
                dp(32),
                "#99FFFFFF"
            )
            setPadding(dp(22), dp(15), dp(22), dp(15))

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(16), 0, dp(10))
            layoutParams = lp

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString() ?: ""
                    rebuildApps()
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            setOnClickListener {
                requestFocus()
                showKeyboard(this)
            }

            setOnLongClickListener {
                webSearch(searchQuery.ifBlank { "Google" })
                true
            }
        }

        box.addView(searchBox)

        val result = calculatorResult(searchQuery)
        if (result != null) {
            val calc = glassText("= $result", 18f, true).apply {
                setPadding(dp(18), dp(12), dp(18), dp(12))
                background = roundedSolid("#45FFFFFF", dp(22), "#70FFFFFF")
                setOnClickListener {
                    toast("Result: $result")
                }
            }
            box.addView(calc)
        }

        return box
    }

    private fun quickActions(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(10))
        }

        row.addView(chip("Web") {
            webSearch(searchQuery.ifBlank { "latest news" })
        })

        row.addView(chip("YouTube") {
            openUrl("https://www.youtube.com/results?search_query=${Uri.encode(searchQuery.ifBlank { "trending" })}")
        })

        row.addView(chip("Maps") {
            openUrl("geo:0,0?q=${Uri.encode(searchQuery.ifBlank { "near me" })}")
        })

        row.addView(chip("Store") {
            openUrl("market://search?q=${Uri.encode(searchQuery.ifBlank { "icon pack" })}")
        })

        return row
    }

    private fun dockArea(): LinearLayout {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassBg()
            setPadding(dp(14), dp(14), dp(14), dp(14))
            elevation = dp(8).toFloat()

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(8), 0, dp(8))
            layoutParams = lp
        }

        wrapper.addView(glassText("Dock", 15f, true))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        }

        val apps = getDockApps()
        if (apps.isEmpty()) {
            row.addView(glassText("Long-press apps to favorite them", 13f, false))
        } else {
            apps.take(5).forEach { app ->
                row.addView(dockIcon(app))
            }
        }

        wrapper.addView(row)
        return wrapper
    }

    private fun dockIcon(app: ApplicationInfo): LinearLayout {
        val label = packageManager.getApplicationLabel(app).toString()

        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))

            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(dp(3), 0, dp(3), 0)
            layoutParams = lp
        }

        val iconWrap = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = iconGlass(app.packageName)
            val size = dp(64)
            layoutParams = LinearLayout.LayoutParams(size, size)
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.loadIcon(packageManager))
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        }

        val name = TextView(this).apply {
            text = label.take(10)
            textSize = 10.5f
            maxLines = 1
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, dp(6), 0, 0)
        }

        iconWrap.addView(icon)
        item.addView(iconWrap)
        item.addView(name)

        item.setOnClickListener {
            launchApp(app.packageName)
        }

        return item
    }

    private fun categoryBar(): LinearLayout {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
        }

        row1.addView(chip(if (favoritesOnly) "Favorites Only" else "Show All") {
            prefs.edit().putBoolean("favorites_only", !favoritesOnly).apply()
            rebuildApps()
        })

        row1.addView(chip(if (listMode) "List View" else "Grid View") {
            prefs.edit().putBoolean("list_mode", !listMode).apply()
            rebuildApps()
        })

        row1.addView(chip("${gridColumns} Columns") {
            val next = if (gridColumns >= 5) 3 else gridColumns + 1
            prefs.edit().putInt("grid_columns", next).apply()
            rebuildApps()
        })

        row1.addView(chip("Icon $iconSize") {
            val next = when (iconSize) {
                52 -> 58
                58 -> 66
                else -> 52
            }
            prefs.edit().putInt("icon_size", next).apply()
            rebuildApps()
        })

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(8))
        }

        listOf("All", "Social", "Productivity", "Games", "Media").forEach { cat ->
            row2.addView(chip(if (selectedCategory == cat) "● $cat" else cat) {
                selectedCategory = cat
                rebuildApps()
            })
        }

        wrapper.addView(row1)
        wrapper.addView(row2)
        return wrapper
    }

    private fun rebuildApps() {
        if (!::appsContainer.isInitialized) return

        appsContainer.removeAllViews()

        val allApps = getLaunchableApps()
            .filter { app ->
                val label = packageManager.getApplicationLabel(app).toString()
                val matchesSearch = searchQuery.isBlank() ||
                        label.lowercase(Locale.getDefault())
                            .contains(searchQuery.lowercase(Locale.getDefault()))
                val matchesFav = !favoritesOnly || getFavorites().contains(app.packageName)
                val matchesCat = selectedCategory == "All" || categoryName(app) == selectedCategory

                matchesSearch && matchesFav && matchesCat
            }

        val favorites = getFavorites()
        val recent = getRecentApps()

        val favoriteApps = allApps.filter { favorites.contains(it.packageName) }
        val recentApps = allApps.filter {
            recent.contains(it.packageName) && !favorites.contains(it.packageName)
        }
        val normalApps = allApps.filter {
            !favorites.contains(it.packageName) && !recent.contains(it.packageName)
        }

        if (calculatorResult(searchQuery) == null && searchQuery.isNotBlank() && allApps.isEmpty()) {
            appsContainer.addView(emptyState("No apps found. Long-press search to Google it."))
            return
        }

        if (favoriteApps.isNotEmpty()) {
            appsContainer.addView(sectionTitle("Favorites"))
            appsContainer.addView(appGroup(favoriteApps))
        }

        if (!favoritesOnly && recentApps.isNotEmpty() && searchQuery.isBlank()) {
            appsContainer.addView(sectionTitle("Recent"))
            appsContainer.addView(appGroup(recentApps.take(8)))
        }

        appsContainer.addView(sectionTitle(if (searchQuery.isBlank()) "$selectedCategory Apps" else "Search Results"))

        if (normalApps.isEmpty() && favoriteApps.isEmpty() && recentApps.isEmpty()) {
            appsContainer.addView(emptyState("No apps found"))
        } else {
            appsContainer.addView(appGroup(normalApps))
        }
    }

    private fun appGroup(apps: List<ApplicationInfo>): View {
        return if (listMode) appList(apps) else appGrid(apps)
    }

    private fun appGrid(apps: List<ApplicationInfo>): GridLayout {
        val displayWidth = resources.displayMetrics.widthPixels
        val usableWidth = displayWidth - dp(36)
        val cellWidth = usableWidth / gridColumns

        val grid = GridLayout(this).apply {
            columnCount = gridColumns
            setPadding(0, dp(4), 0, dp(12))
        }

        apps.forEach { app ->
            grid.addView(appCard(app, cellWidth))
        }

        return grid
    }

    private fun appList(apps: List<ApplicationInfo>): LinearLayout {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        apps.forEach { app ->
            list.addView(appRow(app))
        }

        return list
    }

    private fun appCard(app: ApplicationInfo, cellWidth: Int): LinearLayout {
        val label = packageManager.getApplicationLabel(app).toString()
        val isFavorite = getFavorites().contains(app.packageName)
        val launches = getLaunchCount(app.packageName)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(10), dp(6), dp(8))
            background = transparentCard()
            isClickable = true
            isFocusable = true

            val lp = GridLayout.LayoutParams().apply {
                width = cellWidth
                height = dp(132)
                setMargins(dp(2), dp(4), dp(2), dp(4))
            }
            layoutParams = lp
        }

        val iconWrap = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = iconGlass(app.packageName)
            elevation = dp(6).toFloat()

            val wrapSize = dp(iconSize + 16)
            layoutParams = LinearLayout.LayoutParams(wrapSize, wrapSize)
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.loadIcon(packageManager))
            layoutParams = LinearLayout.LayoutParams(dp(iconSize), dp(iconSize))
        }

        val name = TextView(this).apply {
            text = if (isFavorite) "★ $label" else label
            textSize = 11.3f
            maxLines = 2
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(2), dp(7), dp(2), 0)
        }

        val usage = TextView(this).apply {
            text = if (launches > 0) "$launches opens" else categoryName(app)
            textSize = 9.5f
            maxLines = 1
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#D8E7FF"))
        }

        iconWrap.addView(icon)
        card.addView(iconWrap)
        card.addView(name)
        card.addView(usage)

        card.setOnClickListener {
            launchApp(app.packageName)
        }

        card.setOnLongClickListener {
            showAppMenu(app)
            true
        }

        return card
    }

    private fun appRow(app: ApplicationInfo): LinearLayout {
        val label = packageManager.getApplicationLabel(app).toString()
        val isFavorite = getFavorites().contains(app.packageName)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedSolid("#22FFFFFF", dp(24), "#50FFFFFF")

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(5), 0, dp(5))
            layoutParams = lp
        }

        val iconWrap = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = iconGlass(app.packageName)
            layoutParams = LinearLayout.LayoutParams(dp(58), dp(58))
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.loadIcon(packageManager))
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        }

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(this).apply {
            text = if (isFavorite) "★ $label" else label
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        val sub = TextView(this).apply {
            text = "${categoryName(app)} • ${getLaunchCount(app.packageName)} opens"
            textSize = 12f
            setTextColor(Color.parseColor("#D8E7FF"))
        }

        iconWrap.addView(icon)
        info.addView(title)
        info.addView(sub)

        row.addView(iconWrap)
        row.addView(info)

        row.setOnClickListener {
            launchApp(app.packageName)
        }

        row.setOnLongClickListener {
            showAppMenu(app)
            true
        }

        return row
    }

    private fun todoWidget(): LinearLayout {
        val card = glassCard().apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        card.addView(glassText("Todo", 16f, true))

        val items = getStringList("todo_items")
        val preview = TextView(this).apply {
            text = if (items.isEmpty()) "No tasks yet" else items.take(3).joinToString("\n") { "• $it" }
            textSize = 13f
            setTextColor(Color.parseColor("#EAF2FF"))
            setPadding(0, dp(8), 0, dp(12))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row.addView(chip("Add Task") {
            inputDialog("Add Task", "Task name") { value ->
                val updated = getStringList("todo_items").toMutableList()
                updated.add(value)
                saveStringList("todo_items", updated)
                refreshAll()
            }
        })

        row.addView(chip("Clear") {
            prefs.edit().remove("todo_items").apply()
            refreshAll()
        })

        card.addView(preview)
        card.addView(row)
        return card
    }

    private fun financeWidget(): LinearLayout {
        val card = glassCard().apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        card.addView(glassText("Finance Tracker", 16f, true))

        val income = prefs.getFloat("income_total", 0f)
        val expense = prefs.getFloat("expense_total", 0f)
        val balance = income - expense

        val summary = TextView(this).apply {
            text = "Income: $income\nExpenses: $expense\nBalance: $balance"
            textSize = 13f
            setTextColor(Color.parseColor("#EAF2FF"))
            setPadding(0, dp(8), 0, dp(12))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row.addView(chip("+ Income") {
            inputDialog("Add Income", "Amount") { value ->
                val amount = value.toFloatOrNull()
                if (amount != null) {
                    prefs.edit().putFloat("income_total", income + amount).apply()
                    refreshAll()
                } else {
                    toast("Invalid amount")
                }
            }
        })

        row.addView(chip("- Expense") {
            inputDialog("Add Expense", "Amount") { value ->
                val amount = value.toFloatOrNull()
                if (amount != null) {
                    prefs.edit().putFloat("expense_total", expense + amount).apply()
                    refreshAll()
                } else {
                    toast("Invalid amount")
                }
            }
        })

        row.addView(chip("Reset") {
            prefs.edit()
                .remove("income_total")
                .remove("expense_total")
                .apply()
            refreshAll()
        })

        card.addView(summary)
        card.addView(row)
        return card
    }

    private fun settingsWidget(): LinearLayout {
        val card = glassCard().apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        card.addView(glassText("Launcher Customization", 16f, true))

        val selectedPack = prefs.getString("selected_icon_pack_label", "Stock Android Icons")
            ?: "Stock Android Icons"

        val info = TextView(this).apply {
            text = "Icon pack: $selectedPack\nCustomize icons, layout, app drawer, and home behavior."
            textSize = 13f
            setTextColor(Color.parseColor("#EAF2FF"))
            setPadding(0, dp(8), 0, dp(12))
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row1.addView(chip("Icon Pack") {
            showIconPackPicker()
        })

        row1.addView(chip("${gridColumns} Grid") {
            val next = if (gridColumns >= 5) 3 else gridColumns + 1
            prefs.edit().putInt("grid_columns", next).apply()
            toast("Grid changed to $next")
            refreshAll()
        })

        row1.addView(chip("Icon $iconSize") {
            val next = when (iconSize) {
                52 -> 58
                58 -> 66
                else -> 52
            }
            prefs.edit().putInt("icon_size", next).apply()
            refreshAll()
        })

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        row2.addView(chip(if (listMode) "List View" else "Grid View") {
            prefs.edit().putBoolean("list_mode", !listMode).apply()
            refreshAll()
        })

        row2.addView(chip(if (favoritesOnly) "Favorites Only" else "Show All") {
            prefs.edit().putBoolean("favorites_only", !favoritesOnly).apply()
            refreshAll()
        })

        row2.addView(chip("Home App") {
            openDefaultLauncherSettings()
        })

        val row3 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        row3.addView(chip("Reset Icons") {
            prefs.edit()
                .remove("selected_icon_pack")
                .remove("selected_icon_pack_label")
                .apply()
            toast("Back to stock icons")
            refreshAll()
        })

        row3.addView(chip("Clear Recent") {
            prefs.edit().remove("recent").apply()
            rebuildApps()
            toast("Recent cleared")
        })

        row3.addView(chip("Reset Layout") {
            prefs.edit()
                .putInt("grid_columns", 4)
                .putInt("icon_size", 58)
                .putBoolean("list_mode", false)
                .putBoolean("favorites_only", false)
                .apply()
            toast("Layout reset")
            refreshAll()
        })

        card.addView(info)
        card.addView(row1)
        card.addView(row2)
        card.addView(row3)

        return card
    }

    private fun showIconPackPicker() {
        val packs = findIconPacks()

        if (packs.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Icon Packs")
                .setMessage("No supported icon packs found. Install an icon pack from Play Store, then return here.")
                .setPositiveButton("Open Play Store") { _, _ ->
                    openUrl("market://search?q=icon pack")
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val labels = mutableListOf("Stock Android Icons")
        labels.addAll(packs.map { it.first })

        AlertDialog.Builder(this)
            .setTitle("Choose Icon Pack")
            .setItems(labels.toTypedArray()) { _, which ->
                if (which == 0) {
                    prefs.edit()
                        .remove("selected_icon_pack")
                        .remove("selected_icon_pack_label")
                        .apply()
                    toast("Using stock icons")
                } else {
                    val selected = packs[which - 1]
                    prefs.edit()
                        .putString("selected_icon_pack", selected.second)
                        .putString("selected_icon_pack_label", selected.first)
                        .apply()
                    toast("Selected ${selected.first}")
                }

                refreshAll()
            }
            .show()
    }

    private fun findIconPacks(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)

        for (pkg in packages) {
            val packageName = pkg.packageName
            if (packageName == this.packageName) continue

            try {
                val iconPackContext = createPackageContext(
                    packageName,
                    Context.CONTEXT_IGNORE_SECURITY
                )

                val input = iconPackContext.assets.open("appfilter.xml")
                input.close()

                val appInfo = pkg.applicationInfo
                val label = if (appInfo != null) {
                    packageManager.getApplicationLabel(appInfo).toString()
                } else {
                    packageName
                }

                result.add(label to packageName)
            } catch (_: Exception) {
            }
        }

        return result.sortedBy { it.first.lowercase(Locale.getDefault()) }
    }

    private fun getLaunchableApps(): List<ApplicationInfo> {
        return packageManager.getInstalledApplications(0)
            .filter {
                it.packageName != packageName &&
                        packageManager.getLaunchIntentForPackage(it.packageName) != null
            }
            .sortedWith(
                compareByDescending<ApplicationInfo> { getLaunchCount(it.packageName) }
                    .thenBy { packageManager.getApplicationLabel(it).toString().lowercase(Locale.getDefault()) }
            )
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            incrementLaunchCount(packageName)
            addRecent(packageName)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            toast("Unable to open app")
        }
    }

    private fun showAppMenu(app: ApplicationInfo) {
        val label = packageManager.getApplicationLabel(app).toString()
        val isFav = getFavorites().contains(app.packageName)

        val options = arrayOf(
            if (isFav) "Remove from favorites" else "Add to favorites",
            "Uninstall",
            "App info"
        )

        AlertDialog.Builder(this)
            .setTitle(label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        toggleFavorite(app.packageName)
                        rebuildApps()
                    }
                    1 -> {
                        val intent = Intent(Intent.ACTION_DELETE)
                        intent.data = Uri.parse("package:${app.packageName}")
                        startActivity(intent)
                    }
                    2 -> {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:${app.packageName}")
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun toggleFavorite(packageName: String) {
        val favorites = getFavorites().toMutableSet()

        if (favorites.contains(packageName)) {
            favorites.remove(packageName)
            toast("Removed from favorites")
        } else {
            favorites.add(packageName)
            toast("Added to favorites")
        }

        prefs.edit().putStringSet("favorites", favorites).apply()
    }

    private fun getFavorites(): Set<String> {
        return prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    }

    private fun getDockApps(): List<ApplicationInfo> {
        val favorites = getFavorites()
        val apps = getLaunchableApps()
        val favApps = apps.filter { favorites.contains(it.packageName) }
        return if (favApps.isNotEmpty()) favApps else apps.take(5)
    }

    private fun addRecent(packageName: String) {
        val current = prefs.getString("recent", "") ?: ""
        val items = current
            .split("|")
            .filter { it.isNotBlank() && it != packageName }
            .toMutableList()

        items.add(0, packageName)

        prefs.edit().putString("recent", items.take(10).joinToString("|")).apply()
    }

    private fun getRecentApps(): List<String> {
        return (prefs.getString("recent", "") ?: "")
            .split("|")
            .filter { it.isNotBlank() }
    }

    private fun incrementLaunchCount(packageName: String) {
        val key = "launch_count_$packageName"
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    private fun getLaunchCount(packageName: String): Int {
        return prefs.getInt("launch_count_$packageName", 0)
    }

    private fun categoryName(app: ApplicationInfo): String {
        return when (app.category) {
            ApplicationInfo.CATEGORY_GAME -> "Games"
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE -> "Media"
            ApplicationInfo.CATEGORY_SOCIAL -> "Social"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
            else -> "Other"
        }
    }

    private fun openDefaultLauncherSettings() {
        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    private fun webSearch(query: String) {
        val url = "https://www.google.com/search?q=${Uri.encode(query)}"
        openUrl(url)
    }

    private fun openUrl(value: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(value))
            startActivity(intent)
        } catch (_: Exception) {
            toast("No app available")
        }
    }

    private fun calculatorResult(input: String): String? {
        val trimmed = input.replace(" ", "")
        if (trimmed.isBlank()) return null
        if (!trimmed.any { it in "+-*/()" }) return null
        if (!trimmed.all { it.isDigit() || it in "+-*/()." }) return null

        return try {
            val value = ExpressionParser(trimmed).parse()
            if (value % 1.0 == 0.0) value.toLong().toString() else "%.4f".format(value)
        } catch (_: Exception) {
            null
        }
    }

    private fun sectionTitle(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(4), dp(18), dp(4), dp(8))
        }
    }

    private fun emptyState(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 14.5f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#EAF2FF"))
            setPadding(0, dp(42), 0, dp(42))
        }
    }

    private fun chip(label: String, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 11.2f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedGradient(
                intArrayOf(
                    Color.parseColor("#55FFFFFF"),
                    Color.parseColor("#22FFFFFF")
                ),
                dp(24),
                "#77FFFFFF"
            )
            setPadding(dp(7), dp(10), dp(7), dp(10))

            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(dp(3), 0, dp(3), 0)
            layoutParams = lp

            setOnClickListener { action() }
        }
    }

    private fun glassCard(): LinearLayout {
        return LinearLayout(this).apply {
            background = glassBg()
            elevation = dp(10).toFloat()

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(8), 0, dp(10))
            layoutParams = lp
        }
    }

    private fun glassText(textValue: String, size: Float, bold: Boolean): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(Color.WHITE)
        }
    }

    private fun glassBg(): GradientDrawable {
        return roundedGradient(
            intArrayOf(
                Color.parseColor("#70FFFFFF"),
                Color.parseColor("#32FFFFFF"),
                Color.parseColor("#16FFFFFF")
            ),
            dp(32),
            "#88FFFFFF"
        )
    }

    private fun transparentCard(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(22).toFloat()
            setColor(Color.parseColor("#00FFFFFF"))
        }
    }

    private fun iconGlass(packageName: String): GradientDrawable {
        val accent = appColor(packageName)

        val start = Color.argb(170, Color.red(accent), Color.green(accent), Color.blue(accent))
        val middle = Color.argb(90, Color.red(accent), Color.green(accent), Color.blue(accent))
        val end = Color.parseColor("#26FFFFFF")

        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(start, middle, end)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(24).toFloat()
            setStroke(dp(1), Color.parseColor("#99FFFFFF"))
        }
    }

    private fun appColor(packageName: String): Int {
        val colors = intArrayOf(
            Color.parseColor("#7A5CFF"),
            Color.parseColor("#36D1DC"),
            Color.parseColor("#64D2FF"),
            Color.parseColor("#FF7AB6"),
            Color.parseColor("#FFD166"),
            Color.parseColor("#06D6A0"),
            Color.parseColor("#FF8A65"),
            Color.parseColor("#B388FF")
        )

        val index = abs(packageName.hashCode()) % colors.size
        return colors[index]
    }

    private fun roundedSolid(color: String, radius: Int, strokeColor: String? = null): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius.toFloat()
            setColor(Color.parseColor(color))
            if (strokeColor != null) {
                setStroke(dp(1), Color.parseColor(strokeColor))
            }
        }
    }

    private fun roundedGradient(colors: IntArray, radius: Int, strokeColor: String): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius.toFloat()
            setStroke(dp(1), Color.parseColor(strokeColor))
        }
    }

    private fun inputDialog(title: String, hint: String, onDone: (String) -> Unit) {
        val input = EditText(this).apply {
            this.hint = hint
            setSingleLine(true)
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotBlank()) onDone(value)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getStringList(key: String): List<String> {
        return (prefs.getString(key, "") ?: "")
            .split("||")
            .filter { it.isNotBlank() }
    }

    private fun saveStringList(key: String, values: List<String>) {
        prefs.edit().putString(key, values.joinToString("||")).apply()
    }

    private fun refreshAll() {
        buildUi()
    }

    private fun showKeyboard(view: View) {
        view.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }, 120)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

class LiquidBackground(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        paint.shader = LinearGradient(
            0f,
            0f,
            w,
            h,
            intArrayOf(
                Color.parseColor("#07111F"),
                Color.parseColor("#102A43"),
                Color.parseColor("#1C2B72"),
                Color.parseColor("#2E8C9A")
            ),
            floatArrayOf(0f, 0.36f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )

        canvas.drawRect(0f, 0f, w, h, paint)

        drawBlob(canvas, w * 0.18f, h * 0.12f, w * 0.46f, "#7A5CFF", 120)
        drawBlob(canvas, w * 0.86f, h * 0.20f, w * 0.42f, "#36D1DC", 105)
        drawBlob(canvas, w * 0.22f, h * 0.72f, w * 0.52f, "#FF7AB6", 78)
        drawBlob(canvas, w * 0.82f, h * 0.82f, w * 0.48f, "#64D2FF", 88)
        drawBlob(canvas, w * 0.52f, h * 0.48f, w * 0.60f, "#FFFFFF", 28)

        paint.shader = null
        paint.color = Color.parseColor("#22FFFFFF")
        canvas.drawCircle(w * 0.15f, h * 0.06f, w * 0.12f, paint)

        paint.color = Color.parseColor("#16FFFFFF")
        canvas.drawCircle(w * 0.86f, h * 0.55f, w * 0.18f, paint)
    }

    private fun drawBlob(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        color: String,
        alpha: Int
    ) {
        val baseColor = Color.parseColor(color)

        val centerColor = Color.argb(
            alpha,
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )

        val edgeColor = Color.argb(
            0,
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )

        paint.shader = RadialGradient(
            cx,
            cy,
            radius,
            centerColor,
            edgeColor,
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(cx, cy, radius, paint)
        paint.shader = null
    }
}

class ExpressionParser(private val input: String) {
    private var pos = -1
    private var ch = 0

    fun parse(): Double {
        nextChar()
        val x = parseExpression()
        if (pos < input.length) throw IllegalArgumentException("Unexpected")
        return x
    }

    private fun nextChar() {
        ch = if (++pos < input.length) input[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            x = when {
                eat('+'.code) -> x + parseTerm()
                eat('-'.code) -> x - parseTerm()
                else -> return x
            }
        }
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            x = when {
                eat('*'.code) -> x * parseFactor()
                eat('/'.code) -> x / parseFactor()
                else -> return x
            }
        }
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor()
        if (eat('-'.code)) return -parseFactor()

        val x: Double
        val startPos = pos

        if (eat('('.code)) {
            x = parseExpression()
            eat(')'.code)
        } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
            while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
            x = input.substring(startPos, pos).toDouble()
        } else {
            throw IllegalArgumentException("Unexpected")
        }

        return x
    }
}
