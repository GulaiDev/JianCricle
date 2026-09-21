package com.rdbb.jiancircle.ui.screen.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rdbb.jiancircle.data.model.Post
import com.rdbb.jiancircle.util.ImageUrlUtil
import com.rdbb.jiancircle.util.formatTime

@Composable
fun PostCard(post: Post, onClick: () -> Unit, onLike: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageUrlUtil.resolve(post.author.avatarUrl),
                contentDescription = "头像",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(post.author.nickname, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    formatTime(post.createdAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(post.content, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)

        if (post.imageUrls.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(post.imageUrls.take(3)) { url ->
                    AsyncImage(
                        model = ImageUrlUtil.resolve(url),  // 拼接 BaseUrl
                        contentDescription = "帖子图片",
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                if (post.imageUrls.size > 3) {
                    item {
                        Box(
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+${post.imageUrls.size - 3}", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (post.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "点赞",
                    tint = if (post.liked) Color.Red else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp).clickable(onClick = onLike)
                )
                Spacer(Modifier.width(4.dp))
                Text(post.likeCount.toString(), fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Comment, "评论", Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(post.commentCount.toString(), fontSize = 14.sp)
            }
        }
    }
}