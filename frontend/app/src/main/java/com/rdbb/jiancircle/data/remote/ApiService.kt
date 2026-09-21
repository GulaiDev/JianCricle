package com.rdbb.jiancircle.data.remote

import com.rdbb.jiancircle.BuildConfig
import com.rdbb.jiancircle.data.model.ApiResponse
import com.rdbb.jiancircle.data.model.Comment
import com.rdbb.jiancircle.data.model.CommentRequest
import com.rdbb.jiancircle.data.model.LikeResult
import com.rdbb.jiancircle.data.model.LoginRequest
import com.rdbb.jiancircle.data.model.LoginResponse
import com.rdbb.jiancircle.data.model.PageResponse
import com.rdbb.jiancircle.data.model.Post
import com.rdbb.jiancircle.data.model.PostRequest
import com.rdbb.jiancircle.data.model.RegisterRequest
import com.rdbb.jiancircle.data.model.UploadResponse
import com.rdbb.jiancircle.data.model.User
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API 服务接口，由 Hilt 在 NetworkModule 中通过 retrofit.create() 提供单例。
 */
interface ApiService {
    // ===== 认证 =====

    /** 注册 */
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<LoginResponse>

    /** 登录（账号密码） */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    // ===== 用户 =====

    /** 获取当前登录用户信息 */
    @GET("api/user/profile")
    suspend fun getUserProfile(): ApiResponse<User>

    // ===== 图片上传 =====

    /** 上传图片到后端本地存储（multipart，字段名 file） */
    @Multipart
    @POST("api/upload/image")
    suspend fun uploadImage(@Part file: MultipartBody.Part): ApiResponse<UploadResponse>

    // ===== 帖子 =====

    /** 发布帖子 */
    @POST("api/posts")
    suspend fun createPost(@Body request: PostRequest): ApiResponse<Post>

    /** 首页信息流（可按关键词搜索，分页加载） */
    @GET("api/posts/feed")
    suspend fun getFeed(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("keyword") keyword: String? = null
    ): ApiResponse<PageResponse<Post>>

    /** 帖子详情 */
    @GET("api/posts/{post_id}")
    suspend fun getPostDetail(@Path("post_id") postId: Int): ApiResponse<Post>

    /** 我的帖子（分页） */
    @GET("api/posts/mine")
    suspend fun getMyPosts(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): ApiResponse<PageResponse<Post>>

    /** 点赞 / 取消点赞（同一接口切换状态，需登录） */
    @POST("api/posts/{post_id}/like")
    suspend fun toggleLike(@Path("post_id") postId: Int): ApiResponse<LikeResult>

    // ===== 评论 =====

    /** 评论列表（分页，按时间倒序） */
    @GET("api/posts/{post_id}/comments")
    suspend fun getComments(
        @Path("post_id") postId: Int,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): ApiResponse<PageResponse<Comment>>

    /** 发表评论（需登录） */
    @POST("api/posts/{post_id}/comments")
    suspend fun createComment(
        @Path("post_id") postId: Int,
        @Body request: CommentRequest
    ): ApiResponse<Comment>

    companion object {
        /**
         * 后端基础地址，必须以 / 结尾。
         * 来自 BuildConfig.API_BASE_URL，在 local.properties 中通过 BASE_URL 配置（不入库）：
         * - 模拟器访问宿主机：http://10.0.2.2:8000/（默认值）
         * - 真机调试：http://<电脑局域网IP>:8000/
         */
        const val BASE_URL = BuildConfig.API_BASE_URL
    }
}
