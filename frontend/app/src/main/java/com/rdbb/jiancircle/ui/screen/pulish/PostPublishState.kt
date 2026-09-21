package com.rdbb.jiancircle.ui.screen.pulish

import com.rdbb.jiancircle.data.model.UploadFileItem
import com.rdbb.jiancircle.data.model.UploadStatus

/** 发帖页 UI 状态 */
data class PostPublishState(
    val content: String = "",
    val uploadItems: List<UploadFileItem> = emptyList(),
    val showSourceSheet: Boolean = false,
    val showExitConfirm: Boolean = false,
    val publishing: Boolean = false,
    val publishSuccess: Boolean = false,
    val toastMessage: String? = null
) {
    /** 发布门禁：有内容且所有图片上传成功、当前不在发布中 */
    val canPublish: Boolean
        get() = (content.isNotBlank() || uploadItems.isNotEmpty())
                && uploadItems.all { it.uploadStatus == UploadStatus.SUCCESS }
                && !publishing

    /** 是否有未发布内容（用于返回拦截） */
    val hasContent: Boolean
        get() = content.isNotBlank() || uploadItems.isNotEmpty()

    /** 剩余可添加附件数（最多 9 个） */
    val remainingSlots: Int
        get() = (MAX_ATTACHMENTS - uploadItems.size).coerceAtLeast(0)

    companion object {
        const val MAX_ATTACHMENTS = 9
    }
}
