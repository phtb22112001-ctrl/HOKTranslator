package com.hoktranslator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class ScreenCaptureService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingButton: TextView? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startTranslatorService()
        showFloatingButton()
    }

    private fun startTranslatorService() {

        val notification = Notification.Builder(
            this,
            "hok_translator"
        )
            .setContentTitle("HOK Translator")
            .setContentText("Nút dịch đang hoạt động")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .build()

        startForeground(1, notification)
    }

    private fun showFloatingButton() {

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        val button = TextView(this)

        button.text = "译"
        button.textSize = 22f
        button.setTextColor(Color.WHITE)
        button.setBackgroundColor(
            Color.rgb(35, 105, 220)
        )
        button.gravity = Gravity.CENTER

        button.setOnClickListener {

            // Sẽ kết nối chức năng:
            // 1. Chụp màn hình
            // 2. OCR tiếng Trung
            // 3. Dịch sang tiếng Việt
            // ở bước tiếp theo.
        }

        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params = WindowManager.LayoutParams(
            70,
            70,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity =
            Gravity.CENTER_VERTICAL or Gravity.RIGHT

        params.x = 20

        windowManager.addView(button, params)

        floatingButton = button
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "hok_translator",
                "HOK Translator",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {

        floatingButton?.let {
            windowManager.removeView(it)
        }

        floatingButton = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
