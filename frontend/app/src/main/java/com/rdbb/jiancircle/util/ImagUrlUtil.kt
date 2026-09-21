package com.rdbb.jiancircle.util

import com.rdbb.jiancircle.data.remote.ApiService

object ImageUrlUtil {
    fun resolve(relativeUrl: String?): String? {
        if (relativeUrl.isNullOrEmpty()) return null
        return if (relativeUrl.startsWith("http")) {
            relativeUrl  // 已是完整 URL
        } else {
            ApiService.BASE_URL.trimEnd('/') + relativeUrl
        }
    }
}