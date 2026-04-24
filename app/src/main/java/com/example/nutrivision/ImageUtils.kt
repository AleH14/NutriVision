package com.example.nutrivision

import android.graphics.*
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    fun loadRotatedCircleBitmap(file: File): Bitmap {
        val raw = BitmapFactory.decodeFile(file.absolutePath) ?: throw IllegalArgumentException("Error decodificando")
        val exif = ExifInterface(file.absolutePath)
        val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val rotated = if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also { raw.recycle() }
        } else raw

        val size = minOf(rotated.width, rotated.height)
        val squared = Bitmap.createBitmap(rotated, (rotated.width - size) / 2, (rotated.height - size) / 2, size, size).also { rotated.recycle() }
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squared, 0f, 0f, paint)
        squared.recycle()
        return output
    }

    /**
     * Comprime y redimensiona una imagen para enviarla a la API.
     * Esto reduce el peso de megabytes a kilobytes, acelerando la subida.
     */
    fun compressImageForApi(inputFile: File): File {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(inputFile.absolutePath, options)
        
        val maxSize = 1080
        var inSampleSize = 1
        if (options.outHeight > maxSize || options.outWidth > maxSize) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxSize && halfWidth / inSampleSize >= maxSize) {
                inSampleSize *= 2
            }
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath, options)
        
        // Mantener rotación correcta al comprimir
        val exif = ExifInterface(inputFile.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val compressedFile = File.createTempFile("upload_", ".jpg")
        val out = FileOutputStream(compressedFile)
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        out.flush()
        out.close()
        
        if (finalBitmap != bitmap) bitmap.recycle()
        finalBitmap.recycle()
        
        return compressedFile
    }
}