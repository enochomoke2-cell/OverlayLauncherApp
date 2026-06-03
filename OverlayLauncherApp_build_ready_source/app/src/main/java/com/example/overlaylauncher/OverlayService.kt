package com.example.overlaylauncher

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    private var isMinimized = false
    private var isFullScreen = false
    private var searchQuery = ""

    private val prefs by lazy {
        getSharedPreferences("ol_glass_launcher_prefs", MODE_PRIVATE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showFullOverlay()
    }

    private fun showFullOverlay() {
        isMinimized = false
        removeCurrentOverlay()

        val display = resources.displayMetrics
        val panelWidth = if (isFullScreen) {
            (display.widthPixels * 0.94).toInt()
        } else {
            (display.widthPixels * 0.86).toInt()
        }

        val panelHeight = if (isFullScreen) {
            (display.heightPixels * 0.82).toInt()
        } else {
            (display.heightPixels * 0.68).toInt()
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            background = glassPanelBg()
            elevation = 24f
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 20, 22, 20)
            background = glassHeaderBg()
        }

        val title = TextView(this).apply {
            text = "OL Glass Launcher"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        val subtitle = TextView(this).apply {
            text = "Search • Favorites • Recent • Floating apps"
            textSize = 13f
            setTextColor(Color.parseColor("#EAF2FF"))
            setPadding(0, 5, 0, 0)
        }

        header.addView(title)
        header.addView(subtitle)

        val searchBox = EditText(this).apply {
            hint = "Search apps"
            textSize = 16f
            setSingleLine(true)
            setText(searchQuery)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#BFD7FF"))
            background = searchBg()
            setPadding(28, 16, 28, 16)

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 18, 0, 14)
            layoutParams = lp

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString() ?: ""
                    rebuildAppsGrid()
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            setOnClickListener {
                requestFocus()
                showKeyboard(this)
            }

            setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    showKeyboard(view)
                }
            }
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 2, 0, 12)
        }

        controls.addView(pill("Minimize", "#35FFFFFF") {
            showBubble()
        })

        controls.addView(pill(if (isFullScreen) "Normal" else "Full Screen", "#35FFFFFF") {
            toggleFullScreen()
        })

        controls.addView(pill("Bring Top", "#35FFFFFF") {
            bringOverlayToFront()
            toast("Overlay brought to front")
        })

        controls.addView(pill("Close", "#55FF3B30") {
            stopSelf()
        })

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val appContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "apps_container"
        }

        scroll.addView(appContainer)

        root.addView(header)
        root.addView(searchBox)
        root.addView(controls)
        root.addView(scroll, LinearLayout.LayoutParams(panelWidth, panelHeight))

        params = createOverlayParams(
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            focusable = true,
            startX = if (isFullScreen) 12 else null,
            startY = if (isFullScreen) 40 else null
        )

        enableDrag(header, root)

        overlayView = root
        windowManager.addView(root, params)

        rebuildAppsGrid()
    }

    private fun rebuildAppsGrid() {
        val root = overlayView as? LinearLayout ?: return
        val scroll = root.getChildAt(3) as? ScrollView ?: return
        val container = scroll.getChildAt(0) as? LinearLayout ?: return

        container.removeAllViews()

        val apps = getLaunchableApps().filter {
            val label = packageManager.getApplicationLabel(it).toString()
            searchQuery.isBlank() || label.lowercase(Locale.getDefault())
                .contains(searchQuery.lowercase(Locale.getDefault()))
        }

        val favorites = getFavorites()
        val recent = getRecentApps()

        val favoriteApps = apps.filter { favorites.contains(it.packageName) }
        val recentApps = apps.filter { recent.contains(it.packageName) && !favorites.contains(it.packageName) }
        val normalApps = apps.filter {
            !favorites.contains(it.packageName) && !recent.contains(it.packageName)
        }

        if (favoriteApps.isNotEmpty()) {
            container.addView(sectionTitle("Favorites"))
            container.addView(appGrid(favoriteApps))
        }

        if (recentApps.isNotEmpty()) {
            container.addView(sectionTitle("Recent"))
            container.addView(appGrid(recentApps.take(8)))
        }

        container.addView(sectionTitle(if (searchQuery.isBlank()) "All Apps" else "Search Results"))

        if (normalApps.isEmpty() && favoriteApps.isEmpty() && recentApps.isEmpty()) {
            container.addView(emptyState())
        } else {
            container.addView(appGrid(normalApps))
        }
    }

    private fun appGrid(apps: List<ApplicationInfo>): GridLayout {
        val grid = GridLayout(this).apply {
            columnCount = 3
            setPadding(0, 2, 0, 8)
        }

        apps.forEach { app ->
            grid.addView(appCard(app))
        }

        return grid
    }

    private fun appCard(app: ApplicationInfo): LinearLayout {
        val label = packageManager.getApplicationLabel(app).toString()
        val isFav = getFavorites().contains(app.packageName)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 14, 12, 14)
            background = glassCardBg()
            elevation = 10f
            isClickable = true
            isFocusable = true

            val lp = GridLayout.LayoutParams().apply {
                width = 185
                height = 165
                setMargins(8, 8, 8, 8)
            }
            layoutParams = lp
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.loadIcon(packageManager))
            val lp = LinearLayout.LayoutParams(62, 62)
            lp.setMargins(0, 0, 0, 10)
            layoutParams = lp
        }

        val name = TextView(this).apply {
            text = if (isFav) "★ $label" else label
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
            rebuildAppsGrid()
            true
        }

        return card
    }

    private fun showBubble() {
        isMinimized = true
        removeCurrentOverlay()

        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bubbleBg()
            elevation = 25f
        }

        val text = TextView(this).apply {
            text = "OL"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        bubble.addView(text)

        bubble.setOnClickListener {
            showFullOverlay()
        }

        bubble.setOnLongClickListener {
            bringOverlayToFront()
            toast("Overlay brought to front")
            true
        }

        params = createOverlayParams(
            width = 132,
            height = 132,
            focusable = false
        )

        enableDrag(bubble, bubble)

        overlayView = bubble
        windowManager.addView(bubble, params)
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            addRecent(packageName)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            showBubble()
        } else {
            toast("Unable to open app")
        }
    }

    private fun toggleFullScreen() {
        isFullScreen = !isFullScreen

        if (isFullScreen) {
            prefs.edit()
                .putInt("before_full_x", params.x)
                .putInt("before_full_y", params.y)
                .apply()
        } else {
            prefs.edit()
                .putInt("overlay_x", prefs.getInt("before_full_x", 80))
                .putInt("overlay_y", prefs.getInt("before_full_y", 160))
                .apply()
        }

        showFullOverlay()
    }

    private fun bringOverlayToFront() {
        val view = overlayView ?: return

        try {
            windowManager.removeView(view)
        } catch (_: Exception) {}

        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {}
    }

    private fun getLaunchableApps(): List<ApplicationInfo> {
        return packageManager.getInstalledApplications(0)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
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

    private fun sectionTitle(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#EAF2FF"))
            setPadding(6, 18, 6, 8)
        }
    }

    private fun emptyState(): TextView {
        return TextView(this).apply {
            text = "No apps found"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#D6E4FF"))
            setPadding(0, 40, 0, 40)
        }
    }

    private fun pill(label: String, color: String, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedBg(color, 40, "#45FFFFFF")
            setPadding(12, 10, 12, 10)

            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(5, 0, 5, 0)
            layoutParams = lp

            setOnClickListener { action() }
        }
    }

    private fun createOverlayParams(
        width: Int,
        height: Int,
        focusable: Boolean,
        startX: Int? = null,
        startY: Int? = null
    ): WindowManager.LayoutParams {
        val flags = if (focusable) {
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }

        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX ?: prefs.getInt("overlay_x", 80)
            y = startY ?: prefs.getInt("overlay_y", 160)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
    }

    private fun enableDrag(touchView: View, targetView: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        touchView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (abs(dx) > 6 || abs(dy) > 6) {
                        moved = true
                    }

                    params.x = initialX + dx
                    params.y = initialY + dy

                    try {
                        windowManager.updateViewLayout(targetView, params)
                    } catch (_: Exception) {}

                    true
                }

                MotionEvent.ACTION_UP -> {
                    prefs.edit()
                        .putInt("overlay_x", params.x)
                        .putInt("overlay_y", params.y)
                        .apply()

                    if (!moved && isMinimized) {
                        showFullOverlay()
                    }

                    true
                }

                else -> false
            }
        }
    }

    private fun showKeyboard(view: View) {
        view.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }, 150)
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

    private fun glassHeaderBg(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.parseColor("#50FFFFFF"),
                Color.parseColor("#22FFFFFF")
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 38f
            setStroke(2, Color.parseColor("#60FFFFFF"))
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

    private fun bubbleBg(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor("#CC7A5CFF"),
                Color.parseColor("#AA36D1DC")
            )
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(3, Color.parseColor("#90FFFFFF"))
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

    private fun removeCurrentOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
    }

    override fun onDestroy() {
        removeCurrentOverlay()
        super.onDestroy()
    }
}
