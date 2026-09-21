package com.rdbb.jiancircle.ui.screen.pulish.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rdbb.jiancircle.data.model.UploadFileItem
import com.rdbb.jiancircle.data.model.UploadStatus

/**
 * 附件缩略图：右上角删除；上传中底部进度条+居中 loading；
 * 失败半透明遮罩 + 重试按钮；成功正常展示。
 */
@Composable
fun AttachmentItem(
    item: UploadFileItem,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFECF6F3))
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = "附件图片",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 删除按钮（右上角小圆钮）
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "删除图片",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }

        when (item.uploadStatus) {
            UploadStatus.WAITING,
            UploadStatus.UPLOADING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { if (item.progress > 0f) item.progress else 0f },
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                }
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 6.dp)
                )
            }

            UploadStatus.FAILED -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = onRetry),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "重新上传",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            UploadStatus.SUCCESS -> Unit
        }
    }
}
