package com.offline.wallcorepro.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Utility functions for wallpaper operations
 */
object WallpaperUtils {

    /**
     * Download image to device's Pictures/WallCorePro directory
     */
    fun downloadWallpaper(
        context: Context,
        imageUrl: String,
        fileName: String,
        onSuccess: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val picturesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "WallCorePro"
            )
            if (!picturesDir.exists()) picturesDir.mkdirs()

            val file = File(picturesDir, "$fileName.jpg")
            val outputStream = FileOutputStream(file)
            val inputStream = URL(imageUrl).openStream()

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Notify gallery
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/jpeg"),
                null
            )

            Timber.d("Downloaded wallpaper to ${file.absolutePath}")
            onSuccess(file)
        } catch (e: Exception) {
            Timber.e(e, "Failed to download wallpaper")
            onError(e)
        }
    }

    /**
     * Get content URI for sharing
     */
    fun getShareUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Format bytes to human-readable string
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))}MB"
        }
    }
}

/**
 * Extension formatting utilities
 */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 2_592_000_000 -> "${diff / 86_400_000}d ago"
        else -> "${diff / 2_592_000_000}mo ago"
    }
}
