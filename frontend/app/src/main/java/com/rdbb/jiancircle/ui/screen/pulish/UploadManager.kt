package com.rdbb.jiancircle.ui.screen.pulish

import com.rdbb.jiancircle.data.model.UploadFileItem
import com.rdbb.jiancircle.data.repository.UploadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 上传任务管理器：按附件 id 维护协程 [Job]，
 * 支持进度回调、失败重试（由 ViewModel 重新 startUpload）、删除取消、ViewModel 销毁时全部取消。
 *
 * 作用域由调用方（ViewModel）传入，确保页面销毁时所有上传协程随 viewModelScope 一起取消。
 */
@Singleton
class UploadManager @Inject constructor(
    private val uploadRepository: UploadRepository
) {
    private val uploadJobs = mutableMapOf<String, Job>()

    fun startUpload(
        scope: CoroutineScope,
        item: UploadFileItem,
        onProgress: (Float) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        uploadJobs[item.id]?.cancel()
        val job = scope.launch {
            try {
                onProgress(0f)
                val response = uploadRepository.uploadImage(
                    uri = item.uri,
                    onProgress = { sent, total ->
                        if (total > 0) onProgress((sent.toFloat() / total).coerceIn(0f, 1f))
                    }
                )
                val data = response.data
                if (response.isSuccess && data != null) {
                    onProgress(1f)
                    onSuccess(data.url)
                } else {
                    onError(response.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.message ?: "上传失败")
            }
        }
        uploadJobs[item.id] = job
    }

    fun cancelUpload(itemId: String) {
        uploadJobs.remove(itemId)?.cancel()
    }

    fun cancelAll() {
        uploadJobs.values.forEach { it.cancel() }
        uploadJobs.clear()
    }
}
