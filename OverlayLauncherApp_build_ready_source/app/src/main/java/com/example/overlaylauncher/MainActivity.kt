package com.example.overlaylauncher

import android.content.ComponentName
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var permissionStatus: TextView
    private lateinit var serviceStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createUi())
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun createUi(val defaultLauncherBtn = expressiveButton("Set as Default Launcher", "#D0BCFF", "#21005D") {
    openDefaultLauncherChooser()
}): ScrollView {
        val root = ScrollView(this)
        root.setBackgroundColor(Color.parseColor("#F7F2FA"))

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 56, 36, 36)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "OL Glass Launcher"
            textSize = 30f
            setTextColor(Color.parseColor("#1C1B1F"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Floating overlay launcher with glass UI"
            textSize = 15f
            setTextColor(Color.parseColor("#625B71"))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            background = roundedBg("#FFFBFE", 36)
            elevation = 6f
        }

        permissionStatus = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#1C1B1F"))
            setPadding(0, 0, 0, 14)
        }

        serviceStatus = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#1C1B1F"))
            setPadding(0, 0, 0, 24)
        }

        val permissionBtn = expressiveButton("Grant Overlay Permission", "#6750A4", "#FFFFFF") {
            openOverlayPermission()
        }

        val startBtn = expressiveButton("Start Floating Launcher", "#006D3B", "#FFFFFF") {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, OverlayService::class.java))
                updateStatus()
            } else {
                openOverlayPermission()
            }
        }

        val stopBtn = expressiveButton("Stop Floating Launcher", "#BA1A1A", "#FFFFFF") {
            stopService(Intent(this, OverlayService::class.java))
            updateStatus()
        }

        val appSettingsBtn = expressiveButton("Open App Settings", "#EADDFF", "#21005D") {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        val note = TextView(this).apply {
            text = "Use the overlay permission first, then start the floating launcher."
            textSize = 14f
            setTextColor(Color.parseColor("#625B71"))
            setPadding(0, 26, 0, 0)
        }

        card.addView(permissionStatus)
        card.addView(serviceStatus)
        card.addView(permissionBtn)
        card.addView(startBtn)
        card.addView(stopBtn)
        card.addView(defaultLauncherBtn)
        card.addView(appSettingsBtn)
        card.addView(note)

        container.addView(title)
        container.addView(subtitle)
        container.addView(card)

        root.addView(container)
        return root
    }

    private fun updateStatus() {
        val overlayAllowed = Settings.canDrawOverlays(this)
    private fun openDefaultLauncherChooser() {
    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
    startActivity(intent)
}

        permissionStatus.text = if (overlayAllowed) {
            "Overlay permission: Allowed"
        } else {
            "Overlay permission: Not allowed"
        }

        serviceStatus.text = if (overlayAllowed) {
            "Launcher status: Ready"
        } else {
            "Launcher status: Permission needed"
        }
    }

    private fun openOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun expressiveButton(
        label: String,
        bgColor: String,
        textColor: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 15f
            setTextColor(Color.parseColor(textColor))
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(bgColor, 28)
            setPadding(20, 18, 20, 18)

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 10, 0, 10)
            layoutParams = params

            setOnClickListener { action() }
        }
    }

    private fun roundedBg(color: String, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius.toFloat()
            setColor(Color.parseColor(color))
        }
    }
}
