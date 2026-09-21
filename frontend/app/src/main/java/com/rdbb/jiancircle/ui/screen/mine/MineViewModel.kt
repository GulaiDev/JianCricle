package com.rdbb.jiancircle.ui.screen.mine

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.rdbb.jiancircle.data.local.TokenManager
import com.rdbb.jiancircle.data.model.Post
import com.rdbb.jiancircle.data.repository.PostRepository
import com.rdbb.jiancircle.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MineViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(MineUiState())
    val state = _state.asStateFlow()

    private var currentPage = 0
    private var loaded = false
    private val likingPostIds = mutableSetOf<Int>()

    /** 每次进入"我的"Tab 时调用：首次整页加载，之后静默刷新 */
    fun onVisible() {
        if (loaded) silentRefresh() else load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val profileResult = userRepository.getProfile()
            val profile = profileResult.data
            if (!profileResult.isSuccess || profile == null) {
                _state.update { it.copy(loading = false, error = profileResult.message) }
                return@launch
            }
            _state.update { it.copy(user = profile) }

            val postsResult = postRepository.getMyPosts(page = 1, size = PAGE_SIZE)
            val data = postsResult.data
            if (postsResult.isSuccess && data != null) {
                currentPage = 1
                loaded = true
                _state.update {
                    it.copy(
                        myPosts = data.items,
                        loading = false,
                        hasMore = data.has_more,
                        error = null
                    )
                }
            } else {
                _state.update { it.copy(loading = false, error = postsResult.message) }
            }
        }
    }

    /** 不显示整页 loading 的后台刷新（从详情页点赞/发帖后返回时） */
    fun silentRefresh() {
        viewModelScope.launch {
            userRepository.getProfile().data?.let { user ->
                _state.update { it.copy(user = user) }
            }
            val data = postRepository.getMyPosts(page = 1, size = PAGE_SIZE).data
            if (data != null) {
                currentPage = 1
                _state.update { it.copy(myPosts = data.items, hasMore = data.has_more) }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || !s.hasMore) return
        val nextPage = currentPage + 1
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            val result = postRepository.getMyPosts(page = nextPage, size = PAGE_SIZE)
            val data = result.data
            if (result.isSuccess && data != null) {
                currentPage = nextPage
                _state.update {
                    it.copy(
                        myPosts = it.myPosts + data.items,
                        loadingMore = false,
                        hasMore = data.has_more
                    )
                }
            } else {
                _state.update {
                    it.copy(loadingMore = false, toastMessage = result.message)
                }
            }
        }
    }

    /** 我的帖子点赞：乐观更新 + 失败回滚 */
    fun toggleLike(postId: Int) {
        if (postId in likingPostIds) return
        val original = _state.value.myPosts.find { it.id == postId } ?: return
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
                updatePost(original.copy(liked = data.liked, likeCount = data.likeCount))
            } else {
                updatePost(original)
                _state.update { it.copy(toastMessage = result.message) }
            }
            likingPostIds.remove(postId)
        }
    }

    /** 清除 Coil 图片缓存（内存 + 磁盘） */
    fun clearCache() {
        viewModelScope.launch {
            runCatching {
                val loader = context.imageLoader
                loader.memoryCache?.clear()
                loader.diskCache?.clear()
            }
            _state.update { it.copy(toastMessage = "缓存已清除") }
        }
    }

    /** 退出登录：清除本地 Token，UI 观察到 logoutSuccess 后跳转登录页 */
    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
            _state.update { it.copy(logoutSuccess = true) }
        }
    }

    fun clearToast() {
        _state.update { it.copy(toastMessage = null) }
    }

    private fun updatePost(post: Post) {
        _state.update { state ->
            state.copy(myPosts = state.myPosts.map { if (it.id == post.id) post else it })
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
