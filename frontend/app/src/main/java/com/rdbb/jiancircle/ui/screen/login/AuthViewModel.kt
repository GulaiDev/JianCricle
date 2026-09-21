package com.rdbb.jiancircle.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdbb.jiancircle.data.local.TokenManager
import com.rdbb.jiancircle.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onConfirmPasswordChange(v: String) = _state.update { it.copy(confirmPassword = v) }
    fun onNicknameChange(v: String) = _state.update { it.copy(nickname = v) }
    fun onAgreeChange(v: Boolean) = _state.update { it.copy(agreed = v) }

    fun login() {
        val s = _state.value
        if (!s.loginButtonEnabled) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.login(s.username, s.password)
            if (result.isSuccess) {
                tokenManager.saveToken(result.data!!.token)
                _state.update { it.copy(loginSuccess = true, loading = false) }
            } else {
                _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun register() {
        val s = _state.value
        if (!s.registerButtonEnabled) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.register(s.username, s.password, s.nickname)
            if (result.isSuccess) {
                tokenManager.saveToken(result.data!!.token)
                _state.update { it.copy(loginSuccess = true, loading = false) }
            } else {
                _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}