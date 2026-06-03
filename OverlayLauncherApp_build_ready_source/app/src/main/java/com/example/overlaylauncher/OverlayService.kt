package com.example.overlaylauncher

import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
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
    private var searchQuery = ""

    private val prefs by lazy {
        getSharedPreferences("overlay_launcher_prefs", MODE_PRIVATE)
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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 22, 22, 22)
            background = glassBg()
            elevation = 18f
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 18)
            background = roundedBg("#AA6750A4", 36, "#55FFFFFF")
        }

        val title = TextView(this).apply {
            text = "OL Glass Launcher"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        val subtitle = TextView(this).apply {
            text = "Floating launcher • Favorites • Search • Recent apps"
            textSize = 13f
            setTextColor(Color.parseColor("#F2E7FE"))
            setPadding(0, 5, 0, 0)
        }

        header.addView(title)
        header.addView(subtitle)

        val searchBox = EditText(this).apply {
            hint = "Search apps..."
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#D8CFF2"))
            setSingleLine(true)
            background = roundedBg("#33FFFFFF", 30, "#44FFFFFF")
            setPadding(22, 12, 22, 12)
            setText(searchQuery)

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 16, 0, 12)
            layoutParams = lp

            setOnEditorActionListener { _, _, _ ->
                searchQuery = text.toString()
                showFullOverlay()
                true
            }
        }

        val controls1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 6)
        }

        controls1.addView(smallGlassButton("Search", "#CC6750A4") {
            searchQuery = searchBox.text.toString()
            showFullOverlay()
        })

        controls1.addView(smallGlassButton("Clear", "#665F6368") {
            searchQuery = ""
            showFullOverlay()
        })

        controls1.addView(smallGlassButton("Minimize", "#CC006D3B") {
            showBubble()
        })

        controls1.addView(smallGlassButton("Close", "#CCBA1A1A") {
            stopSelf()
        })

        val controls2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 12)
        }

        controls2.addView(smallGlassButton("Bring Top", "#CC6750A4") {
            bringOverlayToFront()
            toast("Overlay brought to front")
        })

        controls2.addView(smallGlassButton("Refresh", "#CC625B71") {
            showFullOverlay()
        })

        controls2.addView(smallGlassButton("Settings", "#CC1D6C8D") {
            openAppSettings()
        })

        controls2.addView(smallGlassButton("Clear Recent", "#CC7D5260") {
            prefs.edit().remove("recent").apply()
            showFullOverlay()
        })

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 4)
        }

        buildAppSections(list)
        scroll.addView(list)

        root.addView(header)
        root.addView(searchBox)
        root.addView(controls1)
        root.addView(controls2)
        root.addView(scroll, LinearLayout.LayoutParams(620, 780))

        params = createOverlayParams(
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT
        )

        enableDrag(header, root)

        overlayView = root
        windowManager.addView(root, params)
    }

    private fun showBubble() {
        isMinimized = true
        removeCurrentOverlay()

        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBg("#BB6750A4", 100, "#88FFFFFF")
            elevation = 20f
        }

        val text = TextView(this).apply {
            text = "OL"
            textSize = 21f
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

        params = createOverlayParams(width = 135, height = 135)
        enableDrag(bubble, bubble)

        overlayView = bubble
        windowManager.addView(bubble, params)
    }

    private fun buildAppSections(list: LinearLayout) {
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
            list.addView(sectionTitle("★ Favorites"))
            favoriteApps.forEach { list.addView(appButton(it, true)) }
        }

        if (recentApps.isNotEmpty()) {
            list.addView(sectionTitle("Recent Apps"))
            recentApps.take(8).forEach { list.addView(appButton(it, false)) }
        }

        list.addView(sectionTitle(if (searchQuery.isBlank()) "All Apps" else "Search Results"))

        if (normalApps.isEmpty() && favoriteApps.isEmpty() && recentApps.isEmpty()) {
            list.addView(emptyState())
        } else {
            normalApps.forEach { list.addView(appButton(it, false)) }
        }
    }

    private fun appButton(app: ApplicationInfo, isFavorite: Boolean): Button {
        val label = packageManager.getApplicationLabel(app).toString()
        val star = if (isFavorite) "★ " else ""

        return Button(this).apply {
            text = "$star$label"
            textSize = 14f
            isAllCaps = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.WHITE)
            background = roundedBg("#33FFFFFF", 26, "#55FFFFFF")
            setPadding(24, 14, 24, 14)

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 7, 0, 7)
            layoutParams = lp

            setOnClickListener {
                launchApp(app.packageName)
            }

            setOnLongClickListener {
                toggleFavorite(app.packageName)
                val msg = if (getFavorites().contains(app.packageName)) {
                    "$label added to favorites"
                } else {
                    "$label removed from favorites"
                }
                toast(msg)
                showFullOverlay()
                true
            }
        }
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            addRecent(packageName)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)

            val autoMinimize = prefs.getBoolean("auto_minimize", true)
            if (autoMinimize) {
                showBubble()
            }
        } else {
            toast("Unable to open app")
        }
    }

    private fun getLaunchableApps(): List<ApplicationInfo> {
        return packageManager.getInstalledApplications(0)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy {
                packageManager.getApplicationLabel(it).toString().lowercase(Locale.getDefault())
            }
    }

    private fun sectionTitle(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(8, 18, 8, 8)
        }
    }

    private fun emptyState(): TextView {
        return TextView(this).apply {
            text = "No apps found"
            textSize = 14f
            setTextColor(Color.parseColor("#F2E7FE"))
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }
    }

    private fun smallGlassButton(label: String, bg: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 11.5f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = roundedBg(bg, 24, "#55FFFFFF")
            setPadding(8, 6, 8, 6)

            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(4, 0, 4, 0)
            layoutParams = lp

            setOnClickListener { action() }
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

    private fun bringOverlayToFront() {
        val view = overlayView ?: return

        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
        }

        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun createOverlayParams(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("overlay_x", 80)
            y = prefs.getInt("overlay_y", 160)
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
                    } catch (_: Exception) {
                    }

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

    private fun glassBg(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor("#CC1C1B1F"),
                Color.parseColor("#AA6750A4"),
                Color.parseColor("#AA006D77")
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 42f
            setStroke(2, Color.parseColor("#66FFFFFF"))
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
            } catch (_: Exception) {
            }
        }
        overlayView = null
    }

    override fun onDestroy() {
        removeCurrentOverlay()
        super.onDestroy()
    }
}
