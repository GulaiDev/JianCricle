package com.rdbb.jiancircle.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * 图片压缩工具：上传前把选中的图片等比缩放到最长边 [MAX_EDGE]，
 * 以 JPEG [JPEG_QUALITY] 质量写入缓存文件，减少上传流量。
 *
 * 压缩失败（如格式不支持、解码异常）时回退为直接复制原文件，
 * 由后端做内容校验，保证上传流程不中断。
 */
object ImageCompressor {

    private const val MAX_EDGE = 1280
    private const val JPEG_QUALITY = 85

    /** 将 [uri] 指向的图片压缩/复制到 cacheDir/images，返回可上传的本地文件 */
    fun compressToCache(context: Context, uri: Uri): File? {
        val decoded = decodeDownsampled(context, uri)
        val target = File(context.cacheDir, "images").apply { mkdirs() }
            .let { File(it, "upload_${UUID.randomUUID().toString().replace("-", "")}.jpg") }

        if (decoded != null) {
            val scaled = scaleIfNeeded(decoded)
            val written = runCatching {
                ByteArrayOutputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    target.outputStream().use { it.write(out.toByteArray()) }
                }
                true
            }.getOrDefault(false)
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
            if (written && target.length() > 0) return target
        }

        // 回退：原样复制
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.takeIf { it.length() > 0 }
        }.getOrNull()
    }

    /** 先按 inSampleSize 采样解码，避免大图一次性解码导致 OOM */
    private fun decodeDownsampled(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        val largestEdge = maxOf(bounds.outWidth, bounds.outHeight)
        while (largestEdge / sample > MAX_EDGE * 2) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }.getOrNull()
    }

    /** 采样后仍超过最长边限制时再精确缩放一次 */
    private fun scaleIfNeeded(source: Bitmap): Bitmap {
        val largestEdge = maxOf(source.width, source.height)
        if (largestEdge <= MAX_EDGE) return source
        val ratio = MAX_EDGE.toFloat() / largestEdge
        val w = (source.width * ratio).toInt().coerceAtLeast(1)
        val h = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }
}
