package com.rdbb.jiancircle.ui.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdbb.jiancircle.data.model.Post
import com.rdbb.jiancircle.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PostDetailUiState())
    val state = _state.asStateFlow()

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    private var postId: Int = 0
    private var currentPage = 0
    private val likingPostIds = mutableSetOf<Int>()

    /** 进入页面：加载帖子详情 + 第一页评论 */
    fun load(postId: Int) {
        if (this.postId == postId && _state.value.post != null) return
        this.postId = postId
        _state.update { PostDetailUiState(loading = true) }
        viewModelScope.launch {
            val postResult = postRepository.getPostDetail(postId)
            val post = postResult.data
            if (!postResult.isSuccess || post == null) {
                _state.update { it.copy(loading = false, error = postResult.message) }
                return@launch
            }
            _state.update { it.copy(post = post, loading = false, error = null) }
            loadComments(firstPage = true)
        }
    }

    /** 滚动到评论底部时加载下一页 */
    fun loadMoreComments() {
        val s = _state.value
        if (s.loading || s.loadingMore || !s.hasMore) return
        loadComments(firstPage = false)
    }

    private fun loadComments(firstPage: Boolean) {
        val targetPage = if (firstPage) 1 else currentPage + 1
        if (firstPage) {
            _state.update { it.copy(loading = true) }
        } else {
            _state.update { it.copy(loadingMore = true) }
        }
        viewModelScope.launch {
            val result = postRepository.getComments(
                postId = postId,
                page = targetPage,
                size = PAGE_SIZE
            )
            val data = result.data
            if (result.isSuccess && data != null) {
                currentPage = targetPage
                _state.update {
                    it.copy(
                        comments = if (firstPage) data.items else it.comments + data.items,
                        loading = false,
                        loadingMore = false,
                        hasMore = data.has_more
                    )
                }
            } else {
                _state.update { it.copy(loading = false, loadingMore = false) }
                if (!firstPage) messageChannel.send(result.message)
            }
        }
    }

    fun onCommentInputChange(value: String) {
        if (value.length <= MAX_COMMENT_LENGTH) {
            _state.update { it.copy(commentInput = value) }
        }
    }

    /** 发表评论：成功后插入评论列表头部，评论数 +1，清空输入框 */
    fun sendComment() {
        val content = _state.value.commentInput.trim()
        if (content.isEmpty() || _state.value.sendingComment) return
        _state.update { it.copy(sendingComment = true) }
        viewModelScope.launch {
            val result = postRepository.createComment(postId, content)
            val comment = result.data
            if (result.isSuccess && comment != null) {
                _state.update {
                    it.copy(
                        comments = listOf(comment) + it.comments,
                        post = it.post?.copy(commentCount = it.post.commentCount + 1),
                        commentInput = "",
                        sendingComment = false
                    )
                }
            } else {
                _state.update { it.copy(sendingComment = false) }
                messageChannel.send(result.message)
            }
        }
    }

    /** 帖子点赞：乐观更新 + 失败回滚 */
    fun toggleLike() {
        val original = _state.value.post ?: return
        if (original.id in likingPostIds) return
        likingPostIds.add(original.id)

        val optimistic = original.copy(
            liked = !original.liked,
            likeCount = (original.likeCount + if (original.liked) -1 else 1).coerceAtLeast(0)
        )
        _state.update { it.copy(post = optimistic) }

        viewModelScope.launch {
            val result = postRepository.toggleLike(original.id)
            val data = result.data
            if (result.isSuccess && data != null) {
                _state.update {
                    it.copy(post = original.copy(liked = data.liked, likeCount = data.likeCount))
                }
            } else {
                _state.update { it.copy(post = original) }
                messageChannel.send(result.message)
            }
            likingPostIds.remove(original.id)
        }
    }

    fun openImagePreview(index: Int) {
        _state.update { it.copy(previewIndex = index) }
    }

    fun closeImagePreview() {
        _state.update { it.copy(previewIndex = null) }
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val MAX_COMMENT_LENGTH = 500
    }
}
