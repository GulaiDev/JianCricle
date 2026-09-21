package com.rdbb.jiancircle.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 评论，对应后端 schemas/comment.py 的 CommentResponse */
@Serializable
data class Comment(
    val id: Int = 0,
    @SerialName("post_id")
    val postId: Int = 0,
    val content: String = "",
    val author: User = User(id = 0, username = "", nickname = "未知用户"),
    @SerialName("created_at")
    val createdAt: String = ""
)

/** 发表评论请求，对应后端 CommentCreateRequest */
@Serializable
data class CommentRequest(
    val content: String
)
