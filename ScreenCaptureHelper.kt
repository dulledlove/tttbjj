
package com.example.clickcolor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.MediaProjection
import android.media.ImageReader
import android.os.*
import android.widget.Toast

class ScreenCaptureHelper(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {

    private val metrics = context.resources.displayMetrics
    private val width = metrics.widthPixels
    private val height = metrics.heightPixels
    private val dpi = metrics.densityDpi
    private val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
    private val handler = Handler(Looper.getMainLooper())

    private val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
    private val xTarget = prefs.getInt("x", 500)
    private val yTarget = prefs.getInt("y", 800)
    private val targetColor = prefs.getInt("color", Color.RED)

    init {
        mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, null
        )
    }

    fun startColorMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height, Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    val color = bitmap.getPixel(xTarget, yTarget)
                    if (color == targetColor) {
                        AutoTapService.performGlobalTap(context, xTarget, yTarget)
                        Toast.makeText(context, "Color match! Tap triggered", Toast.LENGTH_SHORT).show()
                    }
                    image.close()
                    bitmap.recycle()
                }
                handler.postDelayed(this, 1000)
            }
        })
    }
}
