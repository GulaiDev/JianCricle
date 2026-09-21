package com.rdbb.jiancircle.di


import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.rdbb.jiancircle.data.local.TokenManager
import com.rdbb.jiancircle.data.remote.ApiService

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** 后端基础地址统一定义在 [ApiService.BASE_URL]，供 Retrofit 与图片 URL 拼接共用 */

    /** kotlinx.serialization JSON 解析器配置 */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true   // 忽略后端多返回的字段，避免模型未声明时解析失败
        coerceInputValues = true  // JSON 中的 null 转为属性默认值
        explicitNulls = false     // 序列化时不输出值为 null 的字段
        encodeDefaults = true     // 序列化时包含使用默认值的属性
    }

    /** OkHttp 客户端：认证拦截器 + 日志拦截器 + 超时配置 */
    @Provides
    @Singleton
    fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        // 认证拦截器：自动为每个请求附加 Token
        val authInterceptor = Interceptor { chain ->
            // OkHttp 拦截器是同步的，用 runBlocking 从 DataStore 读取一次 token
            val token = runBlocking { tokenManager.getToken() }
            val request = chain.request().newBuilder()
                .apply {
                    if (!token.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .build()
            chain.proceed(request)
        }

        // 日志拦截器：打印请求/响应的完整内容，便于调试
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /** Retrofit 实例：绑定 baseUrl、OkHttp 客户端和 kotlinx.serialization 转换器 */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(ApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    /** API 服务单例，Repository 中直接注入使用 */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
