package com.dealplanner.ui.camera

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CapturePhotoUriFactory {
    fun create(context: Context, prefix: String): Uri {
        val directory = File(context.cacheDir, "captured_images").apply {
            mkdirs()
        }
        val file = File.createTempFile("${prefix}_", ".jpg", directory)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
