package com.example.nutrivision

import android.graphics.*
import androidx.exifinterface.media.ExifInterface
import java.io.File

object ImageUtils {

    /**
     * Carga una imagen desde un File, la rota según los datos EXIF
     * y la recorta en un círculo.
     */
    fun loadRotatedCircleBitmap(file: File): Bitmap {
        // 1. Decodificar el bitmap
        val raw = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalArgumentException("No se pudo decodificar la imagen")

        // 2. Leer rotación EXIF
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else                                  -> 0f
        }

        // 3. Rotar si es necesario
        val rotated = if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                .also { if (it !== raw) raw.recycle() }
        } else {
            raw
        }

        // 4. Recortar al cuadrado central
        val size = minOf(rotated.width, rotated.height)
        val xOffset = (rotated.width - size) / 2
        val yOffset = (rotated.height - size) / 2
        val squared = Bitmap.createBitmap(rotated, xOffset, yOffset, size, size)
            .also { if (it !== rotated) rotated.recycle() }

        // 5. Recortar en círculo
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squared, 0f, 0f, paint)
        squared.recycle()

        return output
    }
}