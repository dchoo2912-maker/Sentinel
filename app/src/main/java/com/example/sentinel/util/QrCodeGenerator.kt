package com.example.sentinel.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

object QrCodeGenerator {
    fun generateQrCode(
        content: String,
        size: Int = 512,
        qrColor: Color = Color.Black,
        backgroundColor: Color = Color.White
    ): ImageBitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            val qrArgb = qrColor.toArgb()
            val bgArgb = backgroundColor.toArgb()
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                val offset = y * size
                for (x in 0 until size) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) qrArgb else bgArgb
                }
            }
            val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            android.util.Log.e("QrCodeGenerator", "Error generating QR code", e)
            null
        }
    }
}
