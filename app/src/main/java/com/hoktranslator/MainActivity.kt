package com.hoktranslator

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SCREEN_CAPTURE_REQUEST = 1001
    }

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        projectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager

        val startButton = findViewById<Button>(R.id.btnStart)
        statusText = findViewById(R.id.txtStatus)

        startButton.setOnClickListener {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {

        if (!Settings.canDrawOverlays(this)) {

            statusText.text =
                "Hãy cấp quyền hiển thị trên ứng dụng khác."

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )

            startActivity(intent)

        } else {

            requestScreenCapture()
        }
    }

    private fun requestScreenCapture() {

        statusText.text =
            "Đang yêu cầu quyền chụp màn hình..."

        val intent =
            projectionManager.createScreenCaptureIntent()

        startActivityForResult(
            intent,
            SCREEN_CAPTURE_REQUEST
        )
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode != SCREEN_CAPTURE_REQUEST) {
            return
        }

        if (resultCode != Activity.RESULT_OK || data == null) {

            statusText.text =
                "Chưa cấp quyền chụp màn hình."

            return
        }

        statusText.text =
            "Đã cấp quyền. Đang khởi động..."

        val serviceIntent =
            Intent(
                this,
                ScreenCaptureService::class.java
            )

        serviceIntent.putExtra(
            "resultCode",
            resultCode
        )

        serviceIntent.putExtra(
            "data",
            data
        )

        startForegroundService(serviceIntent)
    }
}
