package com.example.overlaylauncher

import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    private var isMinimized = false

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
            background = roundedBg("#F7F2FA", 34)
            elevation = 12f
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)
            background = roundedBg("#6750A4", 30)
        }

        val title = TextView(this).apply {
            text = "OL Floating Launcher"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "Drag here • Launch apps faster"
            setTextColor(Color.parseColor("#EADDFF"))
            textSize = 13f
            setPadding(0, 5, 0, 0)
        }

        header.addView(title)
        header.addView(subtitle)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 12)
            gravity = Gravity.CENTER
        }

        val minimizeBtn = smallButton("Minimize", "#EADDFF", "#21005D") {
            showBubble()
        }

        val frontBtn = smallButton("Bring Top", "#D0BCFF", "#21005D") {
            bringOverlayToFront()
        }

        val refreshBtn = smallButton("Refresh", "#E8DEF8", "#1D192B") {
            showFullOverlay()
        }

        val closeBtn = smallButton("Close", "#FFDAD6", "#410002") {
            stopSelf()
        }

        controls.addView(minimizeBtn)
        controls.addView(frontBtn)
        controls.addView(refreshBtn)
        controls.addView(closeBtn)

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 4)
        }

        buildAppSections(list)

        scroll.addView(list)
        root.addView(header)
        root.addView(controls)
        root.addView(scroll, LinearLayout.LayoutParams(560, 760))

        params = createOverlayParams(width = WindowManager.LayoutParams.WRAP_CONTENT, height = WindowManager.LayoutParams.WRAP_CONTENT)
        enableDrag(header, root)

        overlayView = root
        windowManager.addView(root, params)
    }

    private fun showBubble() {
        isMinimized = true
        removeCurrentOverlay()

        val bubble = TextView(this).apply {
            text = "OL"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedBg("#6750A4", 100)
            elevation = 16f
            setOnClickListener {
                showFullOverlay()
            }
            setOnLongClickListener {
                bringOverlayToFront()
                Toast.makeText(this@OverlayService, "Overlay brought to front", Toast.LENGTH_SHORT).show()
                true
            }
        }

        params = createOverlayParams(width = 130, height = 130)
        enableDrag(bubble, bubble)

        overlayView = bubble
        windowManager.addView(bubble, params)
    }

    private fun buildAppSections(list: LinearLayout) {
        val apps = getLaunchableApps()

        val favorites = getFavorites()
        val recent = getRecentApps()

        val favoriteApps = apps.filter { favorites.contains(it.packageName) }
        val recentApps = apps.filter { recent.contains(it.packageName) && !favorites.contains(it.packageName) }
        val normalApps = apps.filter {
            !favorites.contains(it.packageName) && !recent.contains(it.packageName)
        }

        if (favoriteApps.isNotEmpty()) {
            list.addView(sectionTitle("Favorites"))
            favoriteApps.forEach { app ->
                list.addView(appButton(app, true))
            }
        }

        if (recentApps.isNotEmpty()) {
            list.addView(sectionTitle("Recent Apps"))
            recentApps.take(6).forEach { app ->
                list.addView(appButton(app, false))
            }
        }

        list.addView(sectionTitle("All Apps"))
        normalApps.forEach { app ->
            list.addView(appButton(app, false))
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
            setTextColor(Color.parseColor("#1C1B1F"))
            background = roundedBg("#FFFBFE", 24)
            setPadding(20, 12, 20, 12)

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
                val message = if (getFavorites().contains(app.packageName)) {
                    "$label added to favorites"
                } else {
                    "$label removed from favorites"
                }
                Toast.makeText(this@OverlayService, message, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Opening app", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLaunchableApps(): List<ApplicationInfo> {
        return packageManager.getInstalledApplications(0)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy {
                packageManager.getApplicationLabel(it).toString().lowercase(Locale.getDefault())
            }
    }

    private fun sectionTitle(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#6750A4"))
            setPadding(4, 18, 4, 8)
        }
    }

    private fun smallButton(
        label: String,
        bg: String,
        textColor: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.parseColor(textColor))
            background = roundedBg(bg, 22)
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

        val limited = items.take(8).joinToString("|")
        prefs.edit().putString("recent", limited).apply()
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

    private fun createOverlayParams(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 160
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

                    if (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6) {
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
                    if (!moved && isMinimized) {
                        showFullOverlay()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun roundedBg(color: String, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius.toFloat()
            setColor(Color.parseColor(color))
        }
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
