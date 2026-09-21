package com.rdbb.jiancircle.data.model

import android.net.Uri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 帖子附件实体 — 当前仅支持图片。
 *
 * 承载从选中（拍照/相册）到上传完成的全过程状态，
 * [remoteUrl] 在上传成功后保存后端返回的 /static/images/... 相对路径。
 */
data class UploadFileItem(
    /** 前端生成的唯一 ID（UUID），作为列表 key 与上传任务取消依据 */
    val id: String,
    /** 本地图片 Uri（content:// 或 FileProvider uri） */
    val uri: Uri,
    /** 文件名：拍照_时间戳.jpg / IMG_xxxx.jpg */
    val fileName: String,
    /** 固定 "image" */
    val fileType: String = "image",
    /** 文件大小（字节），≤ 20MB */
    val fileSize: Long,
    var uploadStatus: UploadStatus = UploadStatus.WAITING,
    /** 上传进度 0.0 ~ 1.0 */
    var progress: Float = 0f,
    /** 后端返回的 /static/images/... 相对路径 */
    var remoteUrl: String? = null,
    var errorMessage: String? = null
)

/** 上传响应，对应后端 schemas/upload.py 的 UploadResponse */
@Serializable
data class UploadResponse(
    val url: String = "",
    @SerialName("file_name")
    val fileName: String = "",
    @SerialName("file_size")
    val fileSize: Long = 0L
)
