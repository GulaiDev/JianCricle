package com.rdbb.jiancircle.ui.screen.home

import com.rdbb.jiancircle.data.model.Post

/**
 * 首页信息流的 UI 状态。
 *
 * 帖子列表、搜索关键词、首屏/分页加载标记与错误信息集中在此，
 * 由 [HomeViewModel] 通过 StateFlow 持有并驱动界面刷新。
 */
data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val searchQuery: String = "",
    /** 首屏加载 / 关键词切换后的整页加载 */
    val loading: Boolean = false,
    /** 滚动到底部的下一页加载 */
    val loadingMore: Boolean = false,
    /** 后端是否还有下一页 */
    val hasMore: Boolean = false,
    /** 整页加载失败且列表为空时展示的错误信息；有列表时错误走 Snackbar */
    val error: String? = null
)
