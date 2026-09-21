package com.rdbb.jiancircle.ui.screen.detail

import com.rdbb.jiancircle.data.model.Comment
import com.rdbb.jiancircle.data.model.Post

/** 帖子详情页 UI 状态 */
data class PostDetailUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    /** 帖子与首页评论同时加载中的整页 loading */
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
    val commentInput: String = "",
    val sendingComment: Boolean = false,
    /** 当前打开的大图预览下标；null 表示关闭 */
    val previewIndex: Int? = null
)
