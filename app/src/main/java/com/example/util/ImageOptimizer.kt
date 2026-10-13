package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageOptimizer {

    private const val MAX_DIMENSION = 1024 // High-res bound (1024px) for crisp zooming on PDF and preview
    private const val JPEG_QUALITY = 90 // High visual fidelity without pixelation or blur

    suspend fun optimizeAndSaveBitmap(context: Context, sourceBitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val finalBitmap = if (sourceBitmap.width > MAX_DIMENSION || sourceBitmap.height > MAX_DIMENSION) {
                val ratio = minOf(
                    MAX_DIMENSION.toFloat() / sourceBitmap.width,
                    MAX_DIMENSION.toFloat() / sourceBitmap.height
                )
                val targetW = (sourceBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (sourceBitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sourceBitmap, targetW, targetH, true)
            } else {
                sourceBitmap
            }

            val outputDir = File(context.filesDir, "item_images").apply { mkdirs() }
            val outputFile = File(outputDir, "item_${System.currentTimeMillis()}_${(100..999).random()}.jpg")

            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (finalBitmap != sourceBitmap) {
                finalBitmap.recycle()
            }
            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun optimizeAndSaveImage(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Measure dimensions without allocating full memory
            var inputStream: InputStream? = contentResolver.openInputStream(sourceUri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            // 2. Calculate subsampling ratio
            var inSampleSize = 1
            val largestDim = max(origWidth, origHeight)
            if (largestDim > MAX_DIMENSION) {
                var currentDim = largestDim
                while (currentDim / 2 >= MAX_DIMENSION) {
                    inSampleSize *= 2
                    currentDim /= 2
                }
            }

            // 3. Decode sampled bitmap
            inputStream = contentResolver.openInputStream(sourceUri)
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (sampledBitmap == null) return@withContext null

            // 4. Scale to exact bound if still larger than max dimension
            val finalBitmap = if (sampledBitmap.width > MAX_DIMENSION || sampledBitmap.height > MAX_DIMENSION) {
                val ratio = minOf(
                    MAX_DIMENSION.toFloat() / sampledBitmap.width,
                    MAX_DIMENSION.toFloat() / sampledBitmap.height
                )
                val targetW = (sampledBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (sampledBitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true).also {
                    if (it != sampledBitmap) sampledBitmap.recycle()
                }
            } else {
                sampledBitmap
            }

            // 5. Persist to internal storage
            val outputDir = File(context.filesDir, "item_images").apply { mkdirs() }
            val outputFile = File(outputDir, "item_${System.currentTimeMillis()}_${(100..999).random()}.jpg")

            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            finalBitmap.recycle()

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun optimizeAndSaveLogoImage(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Measure dimensions
            var inputStream: InputStream? = contentResolver.openInputStream(sourceUri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            // 2. Subsample to max 512px
            val maxLogoDim = 512
            var inSampleSize = 1
            val largestDim = max(origWidth, origHeight)
            if (largestDim > maxLogoDim) {
                var currentDim = largestDim
                while (currentDim / 2 >= maxLogoDim) {
                    inSampleSize *= 2
                    currentDim /= 2
                }
            }

            // 3. Decode sampled bitmap
            inputStream = contentResolver.openInputStream(sourceUri)
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (sampledBitmap == null) return@withContext null

            // 4. Scale to boundary if needed
            val finalBitmap = if (sampledBitmap.width > maxLogoDim || sampledBitmap.height > maxLogoDim) {
                val ratio = minOf(
                    maxLogoDim.toFloat() / sampledBitmap.width,
                    maxLogoDim.toFloat() / sampledBitmap.height
                )
                val targetW = (sampledBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (sampledBitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true).also {
                    if (it != sampledBitmap) sampledBitmap.recycle()
                }
            } else {
                sampledBitmap
            }

            // 5. Check if PNG to preserve transparency
            val mimeType = contentResolver.getType(sourceUri)
            val isPng = mimeType?.contains("png", ignoreCase = true) == true
            val outputDir = File(context.filesDir, "business_logos").apply { mkdirs() }
            val ext = if (isPng) "png" else "jpg"
            val outputFile = File(outputDir, "business_logo_${System.currentTimeMillis()}.$ext")

            FileOutputStream(outputFile).use { out ->
                if (isPng) {
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } else {
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
            }
            finalBitmap.recycle()

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadSampledBitmap(filePath: String, maxTargetDim: Int = 500): Bitmap? {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, boundsOptions)
            val maxDim = max(boundsOptions.outWidth, boundsOptions.outHeight)
            var sampleSize = 1
            if (maxDim > maxTargetDim) {
                sampleSize = max(1, maxDim / maxTargetDim)
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(filePath, decodeOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
