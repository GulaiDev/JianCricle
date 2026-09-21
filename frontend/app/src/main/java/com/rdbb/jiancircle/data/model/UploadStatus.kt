package com.rdbb.jiancircle.data.model

/** 附件上传状态机：WAITING → UPLOADING → SUCCESS / FAILED（FAILED 可重试回 UPLOADING） */
enum class UploadStatus {
    WAITING,
    UPLOADING,
    SUCCESS,
    FAILED
}
