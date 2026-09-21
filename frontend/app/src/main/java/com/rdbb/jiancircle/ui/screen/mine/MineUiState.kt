package com.rdbb.jiancircle.ui.screen.mine

import com.rdbb.jiancircle.data.model.Post
import com.rdbb.jiancircle.data.model.User

/** 个人中心 UI 状态 */
data class MineUiState(
    val user: User? = null,
    val myPosts: List<Post> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
    val toastMessage: String? = null,
    val logoutSuccess: Boolean = false
)
