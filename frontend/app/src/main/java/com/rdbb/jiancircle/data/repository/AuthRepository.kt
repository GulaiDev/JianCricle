package com.rdbb.jiancircle.data.repository

import com.rdbb.jiancircle.data.model.ApiResponse
import com.rdbb.jiancircle.data.model.LoginRequest
import com.rdbb.jiancircle.data.model.LoginResponse
import com.rdbb.jiancircle.data.model.RegisterRequest
import com.rdbb.jiancircle.data.remote.ApiService
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证仓库：封装登录、注册等认证相关的数据操作。
 *
 * 约定：
 * - 业务错误（如“用户名或密码错误”“用户名已存在”）后端仍返回 HTTP 200，
 *   错误码在 [ApiResponse.code] 中，直接透传，由 ViewModel 判断 isSuccess；
 * - 网络异常、非 2xx HTTP 错误（如 422 参数校验）、数据解析异常在此统一捕获，
 *   转换为失败的 [ApiResponse]，避免异常向上层泄漏导致崩溃。
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService
) {
    /** 账号密码登录 */
    suspend fun login(username: String, password: String): ApiResponse<LoginResponse> =
        safeCall {
            apiService.login(LoginRequest(username = username, password = password))
        }

    /** 注册（注册成功后端直接返回 token，即注册即登录） */
    suspend fun register(
        username: String,
        password: String,
        nickname: String
    ): ApiResponse<LoginResponse> =
        safeCall {
            apiService.register(
                RegisterRequest(
                    username = username,
                    password = password,
                    nickname = nickname
                )
            )
        }

    /** 统一执行请求，把网络/HTTP/解析异常转换为失败响应 */
    private suspend fun <T> safeCall(
        block: suspend () -> ApiResponse<T>
    ): ApiResponse<T> =
        try {
            block()
        } catch (e: IOException) {
            ApiResponse(code = CODE_NETWORK_ERROR, message = "网络连接失败，请检查网络后重试")
        } catch (e: HttpException) {
            ApiResponse(code = e.code(), message = "服务器异常（${e.code()}），请稍后重试")
        } catch (e: SerializationException) {
            ApiResponse(code = CODE_PARSE_ERROR, message = "数据解析失败，请稍后重试")
        }

    private companion object {
        const val CODE_NETWORK_ERROR = -1
        const val CODE_PARSE_ERROR = -2
    }
}
