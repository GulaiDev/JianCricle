package com.rdbb.jiancircle.data.model

import kotlinx.serialization.Serializable

/**
 * 后端统一响应包装，对应 backend/app/schemas/common.py 中的 ApiResponse[T]。
 *
 * 所有接口都返回该结构，业务数据放在 [data] 中：
 * ```json
 * { "code": 200, "message": "success", "data": { ... } }
 * ```
 *
 * 使用方式（在 ApiService 中）：
 * ```
 * suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>
 * ```
 */
@Serializable
data class ApiResponse<T>(
    val code: Int = 200,
    val message: String = "success",
    val data: T? = null
) {
    /** 请求是否成功（code == 200） */
    val isSuccess: Boolean get() = code == 200
}

/**
 * 分页响应，对应 backend/app/schemas/common.py 中的 PageResponse[T]。
 *
 * 用法示例：
 * ```
 * suspend fun getFeed(...): ApiResponse<PageResponse<Post>>
 * ```
 */
@Serializable
data class PageResponse<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 10,
    val has_more: Boolean = false
)
