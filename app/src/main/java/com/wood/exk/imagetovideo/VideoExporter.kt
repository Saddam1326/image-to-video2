package com.wood.exk.imagetovideo

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object VideoExporter {

    fun exportToGallery(ctx: Context, videoFile: File): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ImageToVideo")
                }
                val uri = ctx.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    ctx.contentResolver.openOutputStream(it)?.use { out ->
                        videoFile.inputStream().use { inp -> inp.copyTo(out) }
                    }
                    true
                } ?: false
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES), "ImageToVideo")
                dir.mkdirs()
                val dest = File(dir, videoFile.name)
                videoFile.copyTo(dest, overwrite = true)
                true
            }
        } catch (_: Exception) { false }
    }
}
