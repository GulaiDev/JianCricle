package com.rdbb.jiancircle.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * 文件相关工具：通过 ContentResolver 读取 Uri 的大小/名称，
 * 以及创建拍照用的缓存文件（配合 FileProvider 分享给相机应用）。
 */
object FileUtil {

    /** 读取 Uri 对应文件大小（字节）；无法获取时回退为输入流可读长度 */
    fun getFileSize(context: Context, uri: Uri): Long {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst() && !cursor.isNull(0)) {
                        return cursor.getLong(0)
                    }
                }
        }
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.available().toLong() }
        }.getOrNull() ?: 0L
    }

    /** 读取 Uri 的显示文件名（如 IMG_xxxx.jpg），失败时返回 null */
    fun getDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    /** 创建拍照输出文件：cacheDir/images/photo_<时间戳>.jpg，对应 file_paths.xml 的 cache-path */
    fun createCameraImageFile(context: Context): File {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        return File(dir, "photo_${System.currentTimeMillis()}.jpg")
    }
}
