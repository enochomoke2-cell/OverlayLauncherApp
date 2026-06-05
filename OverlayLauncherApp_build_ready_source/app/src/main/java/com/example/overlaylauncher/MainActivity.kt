package com.example.overlaylauncher

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.widget.FrameLayout
import android.app.Activity
import android.content.Context
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
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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

    private lateinit var rootScroll: ScrollView
    private lateinit var mainLayout: LinearLayout
    private lateinit var appsContainer: LinearLayout
    private lateinit var searchBox: EditText

    private var searchQuery = ""

    private val prefs by lazy {
        getSharedPreferences("ol_pixel_glass_launcher", MODE_PRIVATE)
    }

    private val gridColumns: Int
        get() = prefs.getInt("grid_columns", 4)

    private val iconSizeDp: Int
        get() = prefs.getInt("icon_size", 58)

    private val searchAtBottom: Boolean
        get() = prefs.getBoolean("search_bottom", true)

    private val glassStrong: Boolean
        get() = prefs.getBoolean("glass_strong", true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildLauncher()
    }

    override fun onResume() {
        super.onResume()
        rebuildApps()
    }

    private fun buildLauncher() {
    val liquidRoot = FrameLayout(this)

    val liquidBackground = LiquidGlassBackground(this)
    liquidRoot.addView(
        liquidBackground,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    rootScroll = ScrollView(this).apply {
        setBackgroundColor(Color.TRANSPARENT)
        overScrollMode = ScrollView.OVER_SCROLL_NEVER
        isFillViewport = true
    }
        }

        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(34), dp(18), dp(18))
        }

        val topArea = createTopArea()
        val controls = createQuickControls()
        val search = createSearchBar()

        appsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(14))
        }

        mainLayout.addView(topArea)

        if (!searchAtBottom) {
            mainLayout.addView(search)
        }

        mainLayout.addView(controls)
        mainLayout.addView(appsContainer)

        if (searchAtBottom) {
            mainLayout.addView(search)
        }

        rootScroll.addView(mainLayout)

liquidRoot.addView(
    rootScroll,
    FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
)

setContentView(liquidRoot)

rebuildApps()
    }

    private fun createTopArea(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(24), dp(22), dp(24))
            background = glassPanel()
            elevation = dp(10).toFloat()
        }

        val time = TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            textSize = 44f
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
            text = "OL Pixel Glass"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Pixel-style launcher with liquid glass"
            textSize = 13f
            setTextColor(Color.parseColor("#EEF6FF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }

        card.addView(time)
        card.addView(date)
        card.addView(title)
        card.addView(subtitle)

        return card
    }

    private fun createSearchBar(): EditText {
        searchBox = EditText(this).apply {
            hint = "Search apps"
            textSize = 16f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#EAF2FF"))
            background = searchBackground()
            setPadding(dp(22), dp(15), dp(22), dp(15))

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(14), 0, dp(14))
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
        }

        return searchBox
    }

    private fun createQuickControls(): LinearLayout {
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(6))
        }

        controls.addView(pill("Home") {
            openDefaultLauncherSettings()
        })

        controls.addView(pill("${gridColumns} Grid") {
            val next = if (gridColumns >= 5) 3 else gridColumns + 1
            prefs.edit().putInt("grid_columns", next).apply()
            toast("Grid changed to $next columns")
            buildLauncher()
        })

        controls.addView(pill("Icons ${iconSizeDp}") {
            val next = when (iconSizeDp) {
                52 -> 58
                58 -> 66
                else -> 52
            }
            prefs.edit().putInt("icon_size", next).apply()
            toast("Icon size changed")
            buildLauncher()
        })

        controls.addView(pill("Glass") {
            prefs.edit().putBoolean("glass_strong", !glassStrong).apply()
            toast("Glass style changed")
            buildLauncher()
        })

        return controls
    }

    private fun rebuildApps() {
        if (!::appsContainer.isInitialized) return

        appsContainer.removeAllViews()

        val allApps = getLaunchableApps().filter {
            val label = packageManager.getApplicationLabel(it).toString()
            searchQuery.isBlank() ||
                    label.lowercase(Locale.getDefault()).contains(searchQuery.lowercase(Locale.getDefault()))
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

        if (favoriteApps.isNotEmpty()) {
            appsContainer.addView(sectionTitle("Favorites"))
            appsContainer.addView(appGrid(favoriteApps))
        }

        if (recentApps.isNotEmpty() && searchQuery.isBlank()) {
            appsContainer.addView(sectionTitle("Recent"))
            appsContainer.addView(appGrid(recentApps.take(8)))
        }

        appsContainer.addView(sectionTitle(if (searchQuery.isBlank()) "All Apps" else "Search Results"))

        if (allApps.isEmpty()) {
            appsContainer.addView(emptyState())
        } else {
            appsContainer.addView(appGrid(normalApps))
        }

        val bottomSettings = createBottomSettings()
        appsContainer.addView(bottomSettings)
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

    private fun appCard(app: ApplicationInfo, cellWidth: Int): LinearLayout {
        val label = packageManager.getApplicationLabel(app).toString()
        val isFavorite = getFavorites().contains(app.packageName)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(10))
            background = transparentCard()
            isClickable = true
            isFocusable = true

            val lp = GridLayout.LayoutParams().apply {
                width = cellWidth
                height = dp(126)
                setMargins(dp(2), dp(4), dp(2), dp(4))
            }
            layoutParams = lp
        }

        val iconWrap = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = iconGlass(app.packageName)
            elevation = dp(6).toFloat()

            val wrapSize = dp(iconSizeDp + 16)
            layoutParams = LinearLayout.LayoutParams(wrapSize, wrapSize)
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.loadIcon(packageManager))
            layoutParams = LinearLayout.LayoutParams(dp(iconSizeDp), dp(iconSizeDp))
        }

        iconWrap.addView(icon)

        val name = TextView(this).apply {
            text = if (isFavorite) "★ $label" else label
            textSize = 11.5f
            maxLines = 2
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(2), dp(8), dp(2), 0)
        }

        card.addView(iconWrap)
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
            rebuildApps()
            true
        }

        return card
    }

    private fun createBottomSettings(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = glassPanel()
            elevation = dp(8).toFloat()

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(18), 0, dp(10))
            layoutParams = lp
        }

        val title = TextView(this).apply {
            text = "Customization"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(10))
        }

        val iconPack = TextView(this).apply {
            text = "Icon packs: stock icons active. Third-party icon-pack picker is ready for next parser upgrade."
            textSize = 13f
            setTextColor(Color.parseColor("#D8E7FF"))
            setPadding(0, 0, 0, dp(12))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row.addView(pill("Search ${if (searchAtBottom) "Bottom" else "Top"}") {
            prefs.edit().putBoolean("search_bottom", !searchAtBottom).apply()
            buildLauncher()
        })

        row.addView(pill("Clear Recent") {
            prefs.edit().remove("recent").apply()
            rebuildApps()
            toast("Recent apps cleared")
        })

        row.addView(pill("Refresh") {
            rebuildApps()
            toast("Apps refreshed")
        })

        card.addView(title)
        card.addView(iconPack)
        card.addView(row)

        return card
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
        return (prefs.getString("recent", "") ?: "")
            .split("|")
            .filter { it.isNotBlank() }
    }

    private fun openDefaultLauncherSettings() {
        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    private fun sectionTitle(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#EAF2FF"))
            setPadding(dp(4), dp(18), dp(4), dp(8))
        }
    }

    private fun emptyState(): TextView {
        return TextView(this).apply {
            text = "No apps found"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#D8E7FF"))
            setPadding(0, dp(42), 0, dp(42))
        }
    }

    private fun pill(label: String, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 11.5f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = pillBg()
            setPadding(dp(8), dp(10), dp(8), dp(10))

            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(dp(4), 0, dp(4), 0)
            layoutParams = lp

            setOnClickListener { action() }
        }
    }

    private fun showKeyboard(view: View) {
        view.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }, 120)
    }

    private fun glassPanel(): GradientDrawable {
    val colors = if (glassStrong) {
        intArrayOf(
            Color.parseColor("#72FFFFFF"),
            Color.parseColor("#36FFFFFF"),
            Color.parseColor("#18FFFFFF")
        )
    } else {
        intArrayOf(
            Color.parseColor("#46FFFFFF"),
            Color.parseColor("#24FFFFFF"),
            Color.parseColor("#10FFFFFF")
        )
    }

    return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(34).toFloat()
        setStroke(dp(1), Color.parseColor("#88FFFFFF"))
    }
}

private fun searchBackground(): GradientDrawable {
    return GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(
            Color.parseColor("#78FFFFFF"),
            Color.parseColor("#38FFFFFF"),
            Color.parseColor("#22FFFFFF")
        )
    ).apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(32).toFloat()
        setStroke(dp(1), Color.parseColor("#99FFFFFF"))
    }
}

private fun iconGlass(packageName: String): GradientDrawable {
    val accent = appColor(packageName)

    val start = Color.argb(
        170,
        Color.red(accent),
        Color.green(accent),
        Color.blue(accent)
    )

    val middle = Color.argb(
        90,
        Color.red(accent),
        Color.green(accent),
        Color.blue(accent)
    )

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
        Color.parseColor("#7A5CFF"), // purple
        Color.parseColor("#36D1DC"), // cyan
        Color.parseColor("#64D2FF"), // blue
        Color.parseColor("#FF7AB6"), // pink
        Color.parseColor("#FFD166"), // yellow
        Color.parseColor("#06D6A0"), // green
        Color.parseColor("#FF8A65"), // coral
        Color.parseColor("#B388FF")  // soft violet
    )

    val index = kotlin.math.abs(packageName.hashCode()) % colors.size
    return colors[index]
}
private fun pillBg(): GradientDrawable {
    return GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(
            Color.parseColor("#64FFFFFF"),
            Color.parseColor("#28FFFFFF")
        )
    ).apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(26).toFloat()
        setStroke(dp(1), Color.parseColor("#88FFFFFF"))
    }
}
    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
class LiquidGlassBackground(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val background = LinearGradient(
            0f,
            0f,
            w,
            h,
            intArrayOf(
                Color.parseColor("#07111F"),
                Color.parseColor("#102A43"),
                Color.parseColor("#233A8B"),
                Color.parseColor("#2E8C9A")
            ),
            floatArrayOf(0f, 0.35f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = background
        canvas.drawRect(0f, 0f, w, h, paint)

        drawBlob(canvas, w * 0.18f, h * 0.12f, w * 0.45f, "#7A5CFF", 120)
        drawBlob(canvas, w * 0.88f, h * 0.18f, w * 0.42f, "#36D1DC", 105)
        drawBlob(canvas, w * 0.22f, h * 0.72f, w * 0.52f, "#FF7AB6", 76)
        drawBlob(canvas, w * 0.82f, h * 0.82f, w * 0.50f, "#64D2FF", 88)
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
