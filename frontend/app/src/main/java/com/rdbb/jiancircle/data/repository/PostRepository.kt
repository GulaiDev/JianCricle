package com.rdbb.jiancircle.data.repository

import com.rdbb.jiancircle.data.model.ApiResponse
import com.rdbb.jiancircle.data.model.Comment
import com.rdbb.jiancircle.data.model.CommentRequest
import com.rdbb.jiancircle.data.model.LikeResult
import com.rdbb.jiancircle.data.model.PageResponse
import com.rdbb.jiancircle.data.model.Post
import com.rdbb.jiancircle.data.model.PostRequest
import com.rdbb.jiancircle.data.remote.ApiService
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 帖子仓库：信息流、发帖、我的帖子、详情、点赞与评论。
 *
 * 异常处理约定与 [AuthRepository] 一致：业务错误透传后端 [ApiResponse.code]，
 * 网络/HTTP/解析异常统一在此兜底，转换为失败响应，避免异常向上层泄漏。
 */
@Singleton
class PostRepository @Inject constructor(
    private val apiService: ApiService
) {
    /** 首页信息流；[keyword] 为 null 或空串时不做关键词过滤 */
    suspend fun getFeed(
        page: Int,
        size: Int,
        keyword: String? = null
    ): ApiResponse<PageResponse<Post>> =
        safeCall {
            apiService.getFeed(
                page = page,
                size = size,
                keyword = keyword?.takeIf { it.isNotBlank() }
            )
        }

    /** 发布帖子 */
    suspend fun createPost(request: PostRequest): ApiResponse<Post> =
        safeCall { apiService.createPost(request) }

    /** 帖子详情 */
    suspend fun getPostDetail(postId: Int): ApiResponse<Post> =
        safeCall { apiService.getPostDetail(postId) }

    /** 我的帖子（分页） */
    suspend fun getMyPosts(page: Int, size: Int): ApiResponse<PageResponse<Post>> =
        safeCall { apiService.getMyPosts(page = page, size = size) }

    /** 点赞 / 取消点赞（后端同一接口切换状态） */
    suspend fun toggleLike(postId: Int): ApiResponse<LikeResult> =
        safeCall { apiService.toggleLike(postId) }

    /** 评论列表（分页） */
    suspend fun getComments(
        postId: Int,
        page: Int,
        size: Int
    ): ApiResponse<PageResponse<Comment>> =
        safeCall { apiService.getComments(postId = postId, page = page, size = size) }

    /** 发表评论 */
    suspend fun createComment(postId: Int, content: String): ApiResponse<Comment> =
        safeCall { apiService.createComment(postId, CommentRequest(content = content)) }

    /** 统一执行请求，把网络/HTTP/解析异常转换为失败响应 */
    private suspend fun <T> safeCall(
        block: suspend () -> ApiResponse<T>
    ): ApiResponse<T> =
        try {
            block()
        } catch (e: IOException) {
            ApiResponse(code = CODE_NETWORK_ERROR, message = "网络连接失败，请检查网络后重试")
        } catch (e: HttpException) {
            val message = when (e.code()) {
                401 -> "登录已过期，请重新登录后再操作"
                else -> "服务器异常（${e.code()}），请稍后重试"
            }
            ApiResponse(code = e.code(), message = message)
        } catch (e: SerializationException) {
            ApiResponse(code = CODE_PARSE_ERROR, message = "数据解析失败，请稍后重试")
        }

    private companion object {
        const val CODE_NETWORK_ERROR = -1
        const val CODE_PARSE_ERROR = -2
    }
}
