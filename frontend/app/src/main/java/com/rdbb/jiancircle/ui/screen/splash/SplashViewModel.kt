package com.rdbb.jiancircle.ui.screen.splash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdbb.jiancircle.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    // null = 判定中（闪屏展示中）；true = 已登录；false = 未登录
    var isLoggedIn by mutableStateOf<Boolean?>(null)
        private set

    init {
        viewModelScope.launch {
            delay(2000)
            isLoggedIn = !tokenManager.getToken().isNullOrEmpty()
        }
    }
}
