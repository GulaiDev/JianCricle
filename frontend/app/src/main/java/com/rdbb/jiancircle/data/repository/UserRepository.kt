package com.rdbb.jiancircle.data.repository

import com.rdbb.jiancircle.data.model.ApiResponse
import com.rdbb.jiancircle.data.model.User
import com.rdbb.jiancircle.data.remote.ApiService
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** 用户仓库：当前登录用户资料 */
@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
    /** 获取当前登录用户信息（需登录） */
    suspend fun getProfile(): ApiResponse<User> =
        try {
            apiService.getUserProfile()
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
