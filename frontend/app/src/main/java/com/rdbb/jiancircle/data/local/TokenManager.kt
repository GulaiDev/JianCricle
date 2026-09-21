package com.rdbb.jiancircle.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// 顶层扩展属性：整个应用共享同一个 DataStore 实例，切勿在多处重复创建
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * 基于 DataStore Preferences 的 Token 本地存储。
 * DataStore 基于 Kotlin 协程和 Flow，读写都是非阻塞的，替代 SharedPreferences。
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val tokenKey = stringPreferencesKey(KEY_TOKEN)

    /** 观察 token 变化，未登录或已退出时发射 null */
    val tokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[tokenKey]
    }

    /** 一次性读取 token，例如在 OkHttp 拦截器中同步（挂起）获取 */
    suspend fun getToken(): String? = tokenFlow.first()

    /** 登录成功后保存 token */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[tokenKey] = token
        }
    }

    /** 退出登录时清除 token */
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(tokenKey)
        }
    }

    private companion object {
        const val KEY_TOKEN = "token"
    }
}
