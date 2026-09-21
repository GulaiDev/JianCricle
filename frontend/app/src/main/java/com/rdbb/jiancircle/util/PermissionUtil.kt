package com.rdbb.jiancircle.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限工具：相机权限状态检查、是否需要展示理由、跳转应用设置页。
 * 相册选择走系统 Photo Picker（PickMultipleVisualMedia），无需申请存储权限。
 */
object PermissionUtil {

    fun hasCameraPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    /** 曾拒绝过权限但未勾选"不再询问"时返回 true，可在应用内说明用途后再请求 */
    fun shouldShowCameraRationale(activity: Activity): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(
            activity, android.Manifest.permission.CAMERA
        )

    /** 跳转本应用的系统设置详情页（权限被永久拒绝时引导用户手动开启） */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
