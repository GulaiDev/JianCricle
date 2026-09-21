package com.rdbb.jiancircle.data.repository

import android.content.Context
import android.net.Uri
import com.rdbb.jiancircle.data.model.ApiResponse
import com.rdbb.jiancircle.data.model.UploadResponse
import com.rdbb.jiancircle.data.remote.ApiService
import com.rdbb.jiancircle.data.remote.ProgressRequestBody
import com.rdbb.jiancircle.util.ImageCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import okhttp3.MultipartBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 上传仓库：图片先本地压缩到缓存文件，再以 multipart 形式上传并回报进度。
 */
@Singleton
class UploadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) {

    /**
     * 上传一张图片。
     * @param onProgress (已发送字节, 总字节)
     */
    suspend fun uploadImage(
        uri: Uri,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): ApiResponse<UploadResponse> = withContext(Dispatchers.IO) {
        try {
            val file: File = ImageCompressor.compressToCache(context, uri)
                ?: return@withContext ApiResponse(
                    code = CODE_LOCAL_ERROR,
                    message = "图片读取失败，请更换图片重试"
                )

            val requestBody = ProgressRequestBody(
                file = file,
                contentType = "image/jpeg",
                onProgress = onProgress
            )
            val part = MultipartBody.Part.createFormData(
                name = "file",
                filename = file.name,
                body = requestBody
            )
            apiService.uploadImage(part)
        } catch (e: IOException) {
            ApiResponse(code = CODE_NETWORK_ERROR, message = "网络连接失败，请检查网络后重试")
        } catch (e: HttpException) {
            val message = when (e.code()) {
                401 -> "登录已过期，请重新登录后再操作"
                413 -> "图片大小超过限制(20MB)"
                415 -> "仅支持 jpg/png/webp 格式的图片"
                else -> "上传失败（${e.code()}），请稍后重试"
            }
            ApiResponse(code = e.code(), message = message)
        } catch (e: SerializationException) {
            ApiResponse(code = CODE_PARSE_ERROR, message = "数据解析失败，请稍后重试")
        }
    }

    private companion object {
        const val CODE_NETWORK_ERROR = -1
        const val CODE_PARSE_ERROR = -2
        const val CODE_LOCAL_ERROR = -3
    }
}
