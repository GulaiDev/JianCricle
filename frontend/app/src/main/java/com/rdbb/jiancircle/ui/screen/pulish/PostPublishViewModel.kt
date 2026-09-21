package com.rdbb.jiancircle.ui.screen.pulish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdbb.jiancircle.data.model.PostRequest
import com.rdbb.jiancircle.data.model.UploadFileItem
import com.rdbb.jiancircle.data.model.UploadStatus
import com.rdbb.jiancircle.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostPublishViewModel @Inject constructor(
    private val uploadManager: UploadManager,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PostPublishState())
    val state = _state.asStateFlow()

    fun onContentChange(value: String) {
        if (value.length <= MAX_CONTENT_LENGTH) {
            _state.update { it.copy(content = value) }
        } else {
            _state.update { it.copy(content = value.take(MAX_CONTENT_LENGTH)) }
        }
    }

    // ===== 附件 =====

    /** 添加附件（界面完成文件大小校验后调用）；达到 9 个上限时拦截并提示 */
    fun addAttachment(item: UploadFileItem) {
        val current = _state.value.uploadItems
        if (current.size >= PostPublishState.MAX_ATTACHMENTS) {
            showToast("最多上传${PostPublishState.MAX_ATTACHMENTS}张图片")
            return
        }
        if (current.any { it.id == item.id }) return
        _state.update { it.copy(uploadItems = it.uploadItems + item) }
        startUpload(item)
    }

    /** 失败附件点击重试 */
    fun retryUpload(itemId: String) {
        val item = _state.value.uploadItems.find { it.id == itemId } ?: return
        startUpload(item)
    }

    /** 删除附件并取消进行中的上传任务 */
    fun removeAttachment(itemId: String) {
        uploadManager.cancelUpload(itemId)
        _state.update { state ->
            state.copy(uploadItems = state.uploadItems.filterNot { it.id == itemId })
        }
    }

    private fun startUpload(item: UploadFileItem) {
        updateItem(item.id) {
            it.copy(
                uploadStatus = UploadStatus.UPLOADING,
                progress = 0f,
                errorMessage = null
            )
        }
        uploadManager.startUpload(
            scope = viewModelScope,
            item = item,
            onProgress = { p ->
                updateItem(item.id) { it.copy(progress = p) }
            },
            onSuccess = { url ->
                updateItem(item.id) {
                    it.copy(
                        uploadStatus = UploadStatus.SUCCESS,
                        progress = 1f,
                        remoteUrl = url,
                        errorMessage = null
                    )
                }
            },
            onError = { msg ->
                updateItem(item.id) {
                    it.copy(
                        uploadStatus = UploadStatus.FAILED,
                        errorMessage = msg
                    )
                }
            }
        )
    }

    // ===== 发布 =====

    fun publish() {
        val s = _state.value
        if (!s.canPublish) return
        _state.update { it.copy(publishing = true) }
        viewModelScope.launch {
            val request = PostRequest(
                content = s.content.trim(),
                imageUrls = s.uploadItems.mapNotNull { it.remoteUrl }
            )
            val result = postRepository.createPost(request)
            if (result.isSuccess) {
                uploadManager.cancelAll()
                _state.update {
                    it.copy(publishing = false, publishSuccess = true, uploadItems = emptyList())
                }
            } else {
                _state.update {
                    it.copy(publishing = false, toastMessage = "发布失败：${result.message}")
                }
            }
        }
    }

    // ===== 底部来源弹窗 / 退出确认 / Toast =====

    fun showSourceSheet() = _state.update { it.copy(showSourceSheet = true) }
    fun hideSourceSheet() = _state.update { it.copy(showSourceSheet = false) }
    fun showExitConfirm() = _state.update { it.copy(showExitConfirm = true) }
    fun hideExitConfirm() = _state.update { it.copy(showExitConfirm = false) }

    fun showToast(message: String) {
        _state.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _state.update { it.copy(toastMessage = null) }
    }

    private fun updateItem(
        id: String,
        transform: (UploadFileItem) -> UploadFileItem
    ) {
        _state.update { state ->
            state.copy(
                uploadItems = state.uploadItems.map { if (it.id == id) transform(it) else it }
            )
        }
    }

    override fun onCleared() {
        uploadManager.cancelAll()
    }

    private companion object {
        const val MAX_CONTENT_LENGTH = 500
    }
}
