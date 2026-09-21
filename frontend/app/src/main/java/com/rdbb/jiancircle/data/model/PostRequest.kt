package com.rdbb.jiancircle.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 发帖请求，对应后端 schemas/post.py 的 PostCreateRequest */
@Serializable
data class PostRequest(
    val content: String,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList()
)
