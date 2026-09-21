package com.rdbb.jiancircle.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdbb.jiancircle.data.model.Post
import com.rdbb.jiancircle.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState(loading = true))
    val state = _state.asStateFlow()

    /** 一次性提示消息（如加载失败、点赞失败），界面用 Snackbar 展示 */
    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    /** 搜索输入触发流，经防抖后发起请求 */
    private val searchTrigger = MutableStateFlow("")

    /** 当前已加载页（从 1 开始），0 表示尚未成功加载过 */
    private var currentPage = 0

    /** 当前请求使用的关键词（已 trim），空串表示不过滤 */
    private var keyword: String = ""

    /** 点赞请求进行中的帖子，防止短时间内重复点击 */
    private val likingPostIds = mutableSetOf<Int>()

    init {
        // 首屏加载
        loadFirstPage()
        // 搜索关键词防抖：停顿 [SEARCH_DEBOUNCE_MS] 毫秒后按新关键词重新加载
        viewModelScope.launch {
            searchTrigger
                .debounce(SEARCH_DEBOUNCE_MS)
                .filter { it.trim() != keyword }
                .collect {
                    keyword = it.trim()
                    loadFirstPage()
                }
        }
    }

    /** 搜索框内容变化 */
    fun onSearchChange(value: String) {
        _state.update { it.copy(searchQuery = value) }
        searchTrigger.value = value
    }

    /** 点击刷新按钮：按当前关键词重新加载第一页 */
    fun refresh() {
        keyword = _state.value.searchQuery.trim()
        loadFirstPage()
    }

    /** 滚动接近列表底部时加载下一页 */
    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || !s.hasMore) return
        val nextPage = currentPage + 1
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            val result = postRepository.getFeed(
                page = nextPage,
                size = PAGE_SIZE,
                keyword = keyword.ifBlank { null }
            )
            val data = result.data
            if (result.isSuccess && data != null) {
                currentPage = nextPage
                _state.update {
                    it.copy(
                        posts = it.posts + data.items,
                        loadingMore = false,
                        hasMore = data.has_more
                    )
                }
            } else {
                _state.update { it.copy(loadingMore = false) }
                messageChannel.send(result.message)
            }
        }
    }

    /** 点赞 / 取消点赞：先乐观更新，请求失败再回滚 */
    fun toggleLike(postId: Int) {
        if (postId in likingPostIds) return
        val original = _state.value.posts.find { it.id == postId } ?: return
        // 未登录时后端会返回 401，失败分支会回滚乐观更新并提示重新登录
        likingPostIds.add(postId)

        val optimistic = original.copy(
            liked = !original.liked,
            likeCount = (original.likeCount + if (original.liked) -1 else 1).coerceAtLeast(0)
        )
        updatePost(optimistic)

        viewModelScope.launch {
            val result = postRepository.toggleLike(postId)
            val data = result.data
            if (result.isSuccess && data != null) {
                updatePost(
                    original.copy(liked = data.liked, likeCount = data.likeCount)
                )
            } else {
                // 失败回滚到点赞前状态并提示
                updatePost(original)
                messageChannel.send(result.message)
            }
            likingPostIds.remove(postId)
        }
    }

    /** 加载第一页（首屏、搜索、刷新共用） */
    private fun loadFirstPage() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val hadPosts = _state.value.posts.isNotEmpty()
            val result = postRepository.getFeed(
                page = 1,
                size = PAGE_SIZE,
                keyword = keyword.ifBlank { null }
            )
            val data = result.data
            if (result.isSuccess && data != null) {
                currentPage = 1
                _state.update {
                    it.copy(
                        posts = data.items,
                        loading = false,
                        loadingMore = false,
                        hasMore = data.has_more,
                        error = null
                    )
                }
            } else {
                _state.update {
                    // 列表已有数据时保留旧列表，错误用 Snackbar 提示；否则展示整页错误态
                    it.copy(
                        loading = false,
                        loadingMore = false,
                        error = if (hadPosts) null else result.message
                    )
                }
                if (hadPosts) messageChannel.send(result.message)
            }
        }
    }

    private fun updatePost(post: Post) {
        _state.update { state ->
            state.copy(
                posts = state.posts.map { if (it.id == post.id) post else it }
            )
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
