package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File

object ProfilePhotoStorage {
    fun savePickedPhotoToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "creator_media").apply { mkdirs() }
            dir.listFiles()?.forEach { file ->
                if (file.name.startsWith("creator_avatar_")) {
                    file.delete()
                }
            }
            val destFile = File(dir, "creator_avatar_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(destFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    fun clearSavedPhoto(context: Context) {
        try {
            val dir = File(context.filesDir, "creator_media")
            dir.listFiles()?.forEach { file ->
                if (file.name.startsWith("creator_avatar_")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
        }
    }
}
