package com.example.overlaylauncher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.app.Activity

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }

        val info = TextView(this@MainActivity).apply {
            text = "Overlay Launcher\n\n1. Grant display over other apps permission.\n2. Start floating launcher.\n\nNote: Android does not allow normal apps to embed/run other apps inside this app. This app launches them from a floating overlay."
            textSize = 16f
        }

        val permissionBtn = Button(this@MainActivity).apply {
            text = "Grant Overlay Permission"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        val startBtn = Button(this@MainActivity).apply {
            text = "Start Floating Launcher"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                } else {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            }
        }

        val stopBtn = Button(this@MainActivity).apply {
            text = "Stop Floating Launcher"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        }

        layout.addView(info)
        layout.addView(permissionBtn)
        layout.addView(startBtn)
        layout.addView(stopBtn)

        this@MainActivity.setContentView(layout)
    }
}
