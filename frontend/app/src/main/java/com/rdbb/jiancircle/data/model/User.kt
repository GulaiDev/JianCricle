package com.rdbb.jiancircle.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 注册请求 */
@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String
)

/** 登录请求 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/** 登录响应，对应后端 schemas/auth.py 的 TokenResponse */
@Serializable
data class LoginResponse(
    val token: String,
    @SerialName("token_type")
    val tokenType: String = "bearer",
    val user: User
)

/** 用户简要信息，对应后端 schemas/user.py 的 UserBrief */
@Serializable
data class User(
    val id: Int,
    val username: String,
    val nickname: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val bio: String? = null
)
