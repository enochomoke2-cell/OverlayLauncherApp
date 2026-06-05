package com.example.overlaylauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var appsContainer: LinearLayout
    private lateinit var searchBox: EditText
    private var searchQuery = ""

    private val prefs by lazy {
        getSharedPreferences("ol_launcher_prefs", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createHomeUi())
    }

    override fun onResume() {
        super.onResume()
        rebuildAppList()
    }

    private fun createHomeUi(): ScrollView {
        val root = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#101725"))
            overScrollMode = ScrollView.OVER_SCROLL_NEVER
        }

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 56, 28, 32)
        }

        val topCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 26, 28, 26)
            background = glassPanelBg()
            elevation = 18f
        }

        val timeText = TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val dateText = TextView(this).apply {
            text = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())
            textSize = 15f
            setTextColor(Color.parseColor("#D8E7FF"))
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 18)
        }

        val title = TextView(this).apply {
            text = "OL Glass Launcher"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Clean home screen • Fast app search • Favorites"
            textSize = 14f
            setTextColor(Color.parseColor("#D8E7FF"))
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 0)
        }

        topCard.addView(timeText)
        topCard.addView(dateText)
        topCard.addView(title)
        topCard.addView(subtitle)

        searchBox = EditText(this).apply {
            hint = "Search apps..."
            textSize = 16f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#BFD7FF"))
            background = searchBg()
            setPadding(28, 18, 28, 18)

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 22, 0, 18)
            layoutParams = lp

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString() ?: ""
                    rebuildAppList()
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            setOnClickListener {
                requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 18)
        }

        controls.addView(pillButton("Home Settings") {
            openDefaultLauncherSettings()
        })

        controls.addView(pillButton("Refresh") {
            rebuildAppList()
            toast("Apps refreshed")
        })

        controls.addView(pillButton("Clear Recent") {
            prefs.edit().remove("recent").apply()
            rebuildAppList()
            toast("Recent apps cleared")
        })

        appsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        main.addView(topCard)
        main.addView(searchBox)
        main.addView(controls)
        main.addView(appsContainer)

        root.addView(main)
        return root
    }

    private fun rebuildAppList() {
        if (!::appsContainer.isInitialized) return

        appsContainer.removeAllViews()

        val apps = getLaunchableApps().filter {
            val label = packageManager.getApplicationLabel(it).toString()
            searchQuery.isBlank() || label.lowercase(Locale.getDefault())
                .contains(searchQuery.lowercase(Locale.getDefault()))
        }

        val favorites = getFavorites()
        val recent = getRecentApps()

        val favoriteApps = apps.filter { favorites.contains(it.packageName) }
        val recentApps = apps.filter {
            recent.contains(it.packageName) && !favorites.contains(it.packageName)
        }
        val normalApps = apps.filter {
            !favorites.contains(it.packageName) && !recent.contains(it.packageName)
        }

        if (favoriteApps.isNotEmpty()) {
            appsContainer.addView(sectionTitle("Favorites"))
            appsContainer.addView(appGrid(favoriteApps))
        }

        if (recentApps.isNotEmpty()) {
            appsContainer.addView(sectionTitle("Recent Apps"))
            appsContainer.addView(appGrid(recentApps.take(8)))
        }

        appsContainer.addView(sectionTitle(if (searchQuery.isBlank()) "All Apps" else "Search Results"))

        if (apps.isEmpty()) {
            appsContainer.addView(emptyState("No apps found"))
        } else {
            appsContainer.addView(appGrid(normalApps))
        }
    }

    private fun appGrid(apps: List<ApplicationInfo>): GridLayout {
        val grid = GridLayout(this).apply {
            columnCount = 3
            setPadding(0, 4, 0, 18)
        }

        apps.forEach { app ->
            grid.addView(appCard(app))
        }

        return grid
    }

    private fun appCard(app: ApplicationInfo): LinearLayout {
        val label = packageManager.getApplicationLabel(app).toString()
        val isFavorite = getFavorites().contains(app.packageName)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 16, 12, 16)
            background = glassCardBg()
            elevation = 12f
            isClickable = true
            isFocusable = true

            val lp = GridLayout.LayoutParams().apply {
                width = 205
                height = 178
                setMargins(8, 8, 8, 8)
            }
            layoutParams = lp
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.loadIcon(packageManager))
            val lp = LinearLayout.LayoutParams(70, 70)
            lp.setMargins(0, 0, 0, 12)
            layoutParams = lp
        }

        val name = TextView(this).apply {
            text = if (isFavorite) "★ $label" else label
            textSize = 12.5f
            maxLines = 2
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        card.addView(icon)
        card.addView(name)

        card.setOnClickListener {
            launchApp(app.packageName)
        }

        card.setOnLongClickListener {
            toggleFavorite(app.packageName)

            val msg = if (getFavorites().contains(app.packageName)) {
                "$label added to favorites"
            } else {
                "$label removed from favorites"
            }

            toast(msg)
            rebuildAppList()
            true
        }

        return card
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            addRecent(packageName)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            toast("Unable to open app")
        }
    }

    private fun getLaunchableApps(): List<ApplicationInfo> {
        return packageManager.getInstalledApplications(0)
            .filter {
                it.packageName != packageName &&
                        packageManager.getLaunchIntentForPackage(it.packageName) != null
            }
            .sortedBy {
                packageManager.getApplicationLabel(it).toString().lowercase(Locale.getDefault())
            }
    }

    private fun toggleFavorite(packageName: String) {
        val favorites = getFavorites().toMutableSet()

        if (favorites.contains(packageName)) {
            favorites.remove(packageName)
        } else {
            favorites.add(packageName)
        }

        prefs.edit().putStringSet("favorites", favorites).apply()
    }

    private fun getFavorites(): Set<String> {
        return prefs.getStringSet("favorites", emptySet()) ?: emptySet()
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
        val current = prefs.getString("recent", "") ?: ""
        return current.split("|").filter { it.isNotBlank() }
    }

    private fun openDefaultLauncherSettings() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        startActivity(intent)
    }

    private fun sectionTitle(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#EAF2FF"))
            setPadding(6, 18, 6, 8)
        }
    }

    private fun emptyState(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#D6E4FF"))
            setPadding(0, 40, 0, 40)
        }
    }

    private fun pillButton(label: String, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedBg("#35FFFFFF", 40, "#45FFFFFF")
            setPadding(12, 12, 12, 12)

            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(5, 0, 5, 0)
            layoutParams = lp

            setOnClickListener { action() }
        }
    }

    private fun glassPanelBg(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor("#D91B2335"),
                Color.parseColor("#BB375A7F"),
                Color.parseColor("#AA7A5CFF"),
                Color.parseColor("#9936D1DC")
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 46f
            setStroke(2, Color.parseColor("#70FFFFFF"))
        }
    }

    private fun glassCardBg(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor("#44FFFFFF"),
                Color.parseColor("#22FFFFFF")
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 34f
            setStroke(1, Color.parseColor("#55FFFFFF"))
        }
    }

    private fun searchBg(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.parseColor("#35FFFFFF"),
                Color.parseColor("#18FFFFFF")
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 40f
            setStroke(2, Color.parseColor("#55FFFFFF"))
        }
    }

    private fun roundedBg(color: String, radius: Int, strokeColor: String? = null): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius.toFloat()
            setColor(Color.parseColor(color))
            if (strokeColor != null) {
                setStroke(2, Color.parseColor(strokeColor))
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
