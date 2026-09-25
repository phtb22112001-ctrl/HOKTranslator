package com.hoktranslator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class ScreenCaptureService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingButton: TextView? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val resultCode = intent?.getIntExtra(
            "resultCode",
            -1
        ) ?: -1

        val data: Intent? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(
                    "data",
                    Intent::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra("data")
            }

        if (resultCode != -1 && data != null) {

            startTranslatorService()

            startScreenCapture(
                resultCode,
                data
            )

            showFloatingButton()

        } else {

            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startTranslatorService() {

        val notification = Notification.Builder(
            this,
            "hok_translator"
        )
            .setContentTitle("HOK Translator")
            .setContentText("Đang dịch màn hình...")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )

        } else {

            startForeground(
                1,
                notification
            )
        }
    }

    private fun startScreenCapture(
        resultCode: Int,
        data: Intent
    ) {

        val projectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        mediaProjection =
            projectionManager.getMediaProjection(
                resultCode,
                data
            )

        val metrics =
            resources.displayMetrics

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay =
            mediaProjection?.createVirtualDisplay(
                "HOKTranslator",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
    }

    private fun showFloatingButton() {

        windowManager =
            getSystemService(
                WINDOW_SERVICE
            ) as WindowManager

        if (floatingButton != null) {
            return
        }

        val button = TextView(this)

        button.text = "译"
        button.textSize = 22f
        button.setTextColor(Color.WHITE)

        button.setBackgroundColor(
            Color.rgb(35, 105, 220)
        )

        button.gravity = Gravity.CENTER

        button.setOnClickListener {

            captureScreen()
        }

        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params =
            WindowManager.LayoutParams(
                70,
                70,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

        params.gravity =
            Gravity.CENTER_VERTICAL or Gravity.RIGHT

        params.x = 20

        windowManager.addView(
            button,
            params
        )

        floatingButton = button
    }

    private fun captureScreen() {

        val image =
            imageReader?.acquireLatestImage()

        if (image == null) {
            return
        }

        try {

            val plane =
                image.planes[0]

            val buffer =
                plane.buffer

            val pixelStride =
                plane.pixelStride

            val rowStride =
                plane.rowStride

            val rowPadding =
                rowStride -
                    pixelStride *
                    image.width

            // Ảnh màn hình đã được lấy ở đây.
            // Bước tiếp theo sẽ đưa ảnh vào OCR.

        } finally {

            image.close()
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    "hok_translator",
                    "HOK Translator",
                    NotificationManager.IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    override fun onDestroy() {

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.close()
        imageReader = null

        mediaProjection?.stop()
        mediaProjection = null

        floatingButton?.let {

            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

        floatingButton = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}
