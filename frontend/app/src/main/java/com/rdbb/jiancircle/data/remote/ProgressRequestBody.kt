package com.rdbb.jiancircle.data.remote

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File

/**
 * 带进度回调的 multipart 文件体：逐块写入时把已发送字节数通知上层，
 * 供发帖页展示上传进度条。
 */
class ProgressRequestBody(
    private val file: File,
    private val contentType: String = "image/jpeg",
    private val onProgress: (sent: Long, total: Long) -> Unit
) : RequestBody() {

    override fun contentType() = contentType.toMediaTypeOrNull()

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = file.length()
        var sent = 0L
        file.inputStream().use { input ->
            val buffer = ByteArray(SEGMENT_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                sink.write(buffer, 0, read)
                sent += read
                onProgress(sent, total)
            }
        }
        sink.flush()
    }

    private companion object {
        const val SEGMENT_SIZE = 8 * 1024
    }
}
