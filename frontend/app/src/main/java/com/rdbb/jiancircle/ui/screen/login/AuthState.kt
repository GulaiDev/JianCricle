package com.rdbb.jiancircle.ui.screen.login

/**
 * 登录 / 注册页面的 UI 状态。
 *
 * 表单字段、校验派生状态与加载结果集中在此，
 * 由 [AuthViewModel] 通过 StateFlow 持有并驱动界面刷新。
 */
data class AuthState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nickname: String = "",
    val agreed: Boolean = false,
    val loginSuccess: Boolean = false,
    val error: String? = null,
    val loading: Boolean = false
) {
    val isUsernameValid: Boolean
        get() = username.matches(Regex("^[a-zA-Z0-9_]{3,20}$"))
    val isPasswordValid: Boolean
        get() = password.length in 6..20
    val loginButtonEnabled: Boolean
        get() = isUsernameValid && isPasswordValid && agreed && !loading
    val registerButtonEnabled: Boolean
        get() = isUsernameValid && isPasswordValid
                && nickname.isNotBlank() && confirmPassword == password && !loading
}
