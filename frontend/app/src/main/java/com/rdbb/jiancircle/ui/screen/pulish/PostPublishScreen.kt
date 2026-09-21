package com.rdbb.jiancircle.ui.screen.pulish

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rdbb.jiancircle.data.model.UploadFileItem
import com.rdbb.jiancircle.ui.screen.pulish.components.AttachmentAddButton
import com.rdbb.jiancircle.ui.screen.pulish.components.AttachmentItem
import com.rdbb.jiancircle.ui.screen.pulish.components.SourceActionSheet
import com.rdbb.jiancircle.util.FileUtil
import com.rdbb.jiancircle.util.PermissionUtil
import java.io.File
import java.util.UUID

/** 权限提示弹窗类型 */
private enum class CameraPermissionDialog { RATIONALE, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostPublishScreen(
    viewModel: PostPublishViewModel = hiltViewModel(),
    onPublishSuccess: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 发布成功后通知外层切回首页
    LaunchedEffect(state.publishSuccess) {
        if (state.publishSuccess) onPublishSuccess()
    }

    // Toast
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // 拍照输出的临时 Uri（需在启动相机前创建）
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var permissionDialog by remember { mutableStateOf<CameraPermissionDialog?>(null) }

    // 拍照结果
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        val uri = tempCameraUri
        if (isSuccess && uri != null) {
            val fileSize = FileUtil.getFileSize(context, uri)
            viewModel.addAttachment(
                UploadFileItem(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    fileName = "拍照_${System.currentTimeMillis()}.jpg",
                    fileSize = fileSize,
                )
            )
        }
    }

    // 相册多选（系统 Photo Picker，无需存储权限）
    val pickMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { uri ->
            val fileSize = FileUtil.getFileSize(context, uri)
            if (fileSize > MAX_FILE_SIZE) {
                viewModel.showToast("图片超过20MB，已跳过")
                return@forEach
            }
            viewModel.addAttachment(
                UploadFileItem(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    fileName = FileUtil.getDisplayName(context, uri)
                        ?: "image_${System.currentTimeMillis()}.jpg",
                    fileSize = fileSize,
                )
            )
        }
    }

    // 调起相机拍照（需在权限 launcher 之前定义，供其回调中引用）
    fun launchCamera() {
        val file: File = FileUtil.createCameraImageFile(context)
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        tempCameraUri = uri
        runCatching { takePictureLauncher.launch(uri) }
            .onFailure { viewModel.showToast("无法启动相机") }
    }

    // 相机权限请求
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            val activity = context as? Activity
            permissionDialog = if (activity != null
                && PermissionUtil.shouldShowCameraRationale(activity)
            ) {
                CameraPermissionDialog.RATIONALE
            } else {
                CameraPermissionDialog.SETTINGS
            }
        }
    }

    // 系统返回键：有内容时弹确认
    androidx.activity.compose.BackHandler(enabled = state.hasContent) {
        viewModel.showExitConfirm()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (state.hasContent) viewModel.showExitConfirm() else onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    "发布帖子",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = viewModel::publish,
                    enabled = state.canPublish
                ) {
                    if (state.publishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "发布",
                            fontSize = 14.sp,
                            color = if (state.canPublish) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = state.content,
                onValueChange = viewModel::onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 120.dp, max = 240.dp),
                placeholder = { Text("分享你的想法…") },
                maxLines = 10
            )
            Text(
                "${state.content.length}/500",
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 16.dp, top = 4.dp),
                fontSize = 12.sp,
                color = if (state.content.length >= 500) {
                    Color(0xFFFAAD14)
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )

            Spacer(Modifier.height(12.dp))

            // 附件预览行
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = state.uploadItems, key = { it.id }) { item ->
                    AttachmentItem(
                        item = item,
                        onRemove = { viewModel.removeAttachment(item.id) },
                        onRetry = { viewModel.retryUpload(item.id) }
                    )
                }
                if (state.uploadItems.size < PostPublishState.MAX_ATTACHMENTS) {
                    item {
                        AttachmentAddButton(onClick = viewModel::showSourceSheet)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "提示：最多上传9张图片，单文件不超过20MB",
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }

    // 图片来源选择
    if (state.showSourceSheet) {
        SourceActionSheet(
            onTakePhoto = {
                viewModel.hideSourceSheet()
                if (PermissionUtil.hasCameraPermission(context)) {
                    launchCamera()
                } else {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            },
            onPickFromGallery = {
                viewModel.hideSourceSheet()
                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = viewModel::hideSourceSheet
        )
    }

    // 退出确认
    if (state.showExitConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::hideExitConfirm,
            title = { Text("放弃编辑？") },
            text = { Text("当前内容将不会保存") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.hideExitConfirm()
                    onBack()
                }) { Text("放弃", color = Color(0xFFFF4D4F)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideExitConfirm) { Text("继续编辑") }
            }
        )
    }

    // 相机权限说明 / 跳设置引导
    permissionDialog?.let { type ->
        AlertDialog(
            onDismissRequest = { permissionDialog = null },
            title = { Text("需要相机权限") },
            text = {
                Text(
                    if (type == CameraPermissionDialog.RATIONALE) {
                        "拍照需要使用相机权限，请授权后继续"
                    } else {
                        "相机权限已被关闭，请在系统设置中手动开启"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    permissionDialog = null
                    if (type == CameraPermissionDialog.RATIONALE) {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    } else {
                        PermissionUtil.openAppSettings(context)
                    }
                }) { Text(if (type == CameraPermissionDialog.RATIONALE) "去授权" else "去设置") }
            },
            dismissButton = {
                TextButton(onClick = { permissionDialog = null }) { Text("取消") }
            }
        )
    }
}

private const val MAX_FILE_SIZE = 20L * 1024 * 1024
