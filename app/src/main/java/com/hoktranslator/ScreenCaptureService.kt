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

    companion object {
        private const val CHANNEL_ID = "hok_translator"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var windowManager: WindowManager

    private var floatingButton: TextView? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    /**
     * Called when Android stops the MediaProjection session.
     */
    private val projectionCallback =
        object : MediaProjection.Callback() {

            override fun onStop() {
                virtualDisplay?.release()
                virtualDisplay = null

                imageReader?.close()
                imageReader = null

                mediaProjection = null
            }
        }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode =
            intent.getIntExtra(
                "resultCode",
                -1
            )

        val data: Intent? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                intent.getParcelableExtra(
                    "data",
                    Intent::class.java
                )

            } else {

                @Suppress("DEPRECATION")
                intent.getParcelableExtra("data")
            }

        if (resultCode != -1 && data != null) {

            try {

                /*
                 * Android requires the foreground service
                 * to be started with the MEDIA_PROJECTION
                 * foreground-service type.
                 */
                startTranslatorService()

                startScreenCapture(
                    resultCode,
                    data
                )

                showFloatingButton()

            } catch (e: Exception) {

                e.printStackTrace()

                stopSelf()
            }

        } else {

            stopSelf()
        }

        return START_NOT_STICKY
    }

    /**
     * Start the foreground service.
     */
    private fun startTranslatorService() {

        val notification =
            Notification.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "HOK Translator"
                )
                .setContentText(
                    "Nút dịch đang hoạt động"
                )
                .setSmallIcon(
                    android.R.drawable.ic_menu_search
                )
                .setOngoing(true)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    /**
     * Start MediaProjection screen capture.
     */
    private fun startScreenCapture(
        resultCode: Int,
        data: Intent
    ) {

        val projectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        /*
         * Get the MediaProjection object using
         * the permission granted by the user.
         */
        mediaProjection =
            projectionManager.getMediaProjection(
                resultCode,
                data
            )

        if (mediaProjection == null) {
            throw IllegalStateException(
                "MediaProjection is null"
            )
        }

        /*
         * Android requires a MediaProjection.Callback
         * to be registered before creating the
         * VirtualDisplay.
         */
        mediaProjection?.registerCallback(
            projectionCallback,
            null
        )

        val metrics =
            resources.displayMetrics

        val width =
            metrics.widthPixels

        val height =
            metrics.heightPixels

        val density =
            metrics.densityDpi

        /*
         * ImageReader receives screenshots from
         * the VirtualDisplay.
         */
        imageReader =
            ImageReader.newInstance(
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

        if (virtualDisplay == null) {
            throw IllegalStateException(
                "Unable to create VirtualDisplay"
            )
        }
    }

    /**
     * Display the floating "译" button.
     */
    private fun showFloatingButton() {

        windowManager =
            getSystemService(
                WINDOW_SERVICE
            ) as WindowManager

        if (floatingButton != null) {
            return
        }

        val button =
            TextView(this)

        button.text = "译"
        button.textSize = 22f

        button.setTextColor(
            Color.WHITE
        )

        button.setBackgroundColor(
            Color.rgb(
                35,
                105,
                220
            )
        )

        button.gravity =
            Gravity.CENTER

        /*
         * When the button is pressed,
         * capture the latest screen frame.
         *
         * OCR and translation will be
         * connected here later.
         */
        button.setOnClickListener {

            captureScreen()
        }

        val windowType =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                WindowManager.LayoutParams
                    .TYPE_APPLICATION_OVERLAY

            } else {

                @Suppress("DEPRECATION")
                WindowManager.LayoutParams
                    .TYPE_PHONE
            }

        val params =
            WindowManager.LayoutParams(
                70,
                70,
                windowType,
                WindowManager.LayoutParams
                    .FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

        params.gravity =
            Gravity.CENTER_VERTICAL or
                    Gravity.RIGHT

        params.x = 20

        windowManager.addView(
            button,
            params
        )

        floatingButton = button
    }

    /**
     * Get the latest screenshot frame.
     *
     * At this stage we only acquire the image.
     * OCR/translation will be added later.
     */
    private fun captureScreen() {

        val reader =
            imageReader ?: return

        val image =
            reader.acquireLatestImage()
                ?: return

        try {

            /*
             * The image is now available.
             *
             * The next stage can convert this
             * Image into a Bitmap and send it
             * to OCR.
             */

            val width =
                image.width

            val height =
                image.height

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
                        pixelStride * width

            /*
             * Variables are intentionally kept here
             * for the next OCR/Bitmap implementation.
             */
            @Suppress("UNUSED_VARIABLE")
            val availableBytes =
                buffer.remaining()

            @Suppress("UNUSED_VARIABLE")
            val actualWidth =
                width +
                        rowPadding /
                        pixelStride

            @Suppress("UNUSED_VARIABLE")
            val actualHeight =
                height

        } finally {

            image.close()
        }
    }

    /**
     * Create notification channel.
     */
    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "HOK Translator",
                    NotificationManager
                        .IMPORTANCE_LOW
                )

            channel.description =
                "HOK Translator screen translation service"

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

        /*
         * Stop VirtualDisplay.
         */
        virtualDisplay?.release()
        virtualDisplay = null

        /*
         * Release ImageReader.
         */
        imageReader?.close()
        imageReader = null

        /*
         * Unregister MediaProjection callback
         * before stopping the projection.
         */
        mediaProjection?.let {

            try {
                it.unregisterCallback(
                    projectionCallback
                )
            } catch (_: Exception) {
            }

            try {
                it.stop()
            } catch (_: Exception) {
            }
        }

        mediaProjection = null

        /*
         * Remove floating button.
         */
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
