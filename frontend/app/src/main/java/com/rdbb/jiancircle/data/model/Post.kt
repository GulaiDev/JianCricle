package com.rdbb.jiancircle.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 帖子，对应后端 schemas/post.py 的 PostResponse。
 *
 * 所有字段都带默认值，配合 Json { coerceInputValues = true }，
 * 后端返回 null 或缺少字段时使用默认值而不是解析失败。
 */
@Serializable
data class Post(
    val id: Int = 0,
    val content: String = "",
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    @SerialName("like_count")
    val likeCount: Int = 0,
    @SerialName("comment_count")
    val commentCount: Int = 0,
    /** 当前登录用户是否已点赞（未登录时恒为 false） */
    val liked: Boolean = false,
    /** 作者信息；后端异常缺数据时兜底为占位用户 */
    val author: User = User(id = 0, username = "", nickname = "未知用户"),
    @SerialName("created_at")
    val createdAt: String = ""
)

/** 点赞接口返回结果，对应后端 schemas/post.py 的 LikeResult */
@Serializable
data class LikeResult(
    val liked: Boolean,
    @SerialName("like_count")
    val likeCount: Int
)
