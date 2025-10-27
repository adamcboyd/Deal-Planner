package com.snapoptimizer.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * Helper class for ML Kit Text Recognition (OCR).
 * Processes images and extracts text for flyer and receipt parsing.
 */
class TextRecognitionHelper {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Process an image bitmap and extract text.
     */
    suspend fun processImage(bitmap: Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            throw IOException("Failed to process image: ${e.message}", e)
        }
    }

    /**
     * Process an image from URI and extract text.
     */
    suspend fun processImageUri(context: Context, uri: Uri): String {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            throw IOException("Failed to process image URI: ${e.message}", e)
        }
    }

    /**
     * Close the recognizer when done.
     */
    fun close() {
        recognizer.close()
    }
}
