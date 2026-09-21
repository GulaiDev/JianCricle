package com.rdbb.jiancircle.ui.screen.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rdbb.jiancircle.data.model.Comment
import com.rdbb.jiancircle.data.model.Post
import com.rdbb.jiancircle.util.ImageUrlUtil
import com.rdbb.jiancircle.util.formatTime
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PostDetailScreen(
    postId: Int,
    onBack: () -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(postId) { viewModel.load(postId) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // 评论滚动到底加载下一页
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            total > 0 && last >= total - 3
        }.distinctUntilChanged().collect { nearBottom ->
            if (nearBottom) viewModel.loadMoreComments()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text("帖子详情", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        bottomBar = {
            CommentInputBar(
                value = state.commentInput,
                sending = state.sendingComment,
                enabled = state.post != null,
                onValueChange = viewModel::onCommentInputChange,
                onSend = viewModel::sendComment
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val post = state.post
            val error = state.error
            when {
                state.loading && post == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null && post == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(error, color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.load(postId) }) { Text("点击重试") }
                    }
                }

                post != null -> {
                    LazyColumn(state = listState) {
                        item(key = "post_content") {
                            PostContent(
                                post = post,
                                onLike = viewModel::toggleLike,
                                onImageClick = viewModel::openImagePreview
                            )
                        }
                        item(key = "comment_header") {
                            Text(
                                "评论 ${post.commentCount}",
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                            HorizontalDivider()
                        }
                        items(items = state.comments, key = { "comment_${it.id}" }) { comment ->
                            CommentItem(comment)
                            HorizontalDivider()
                        }
                        if (state.loadingMore) {
                            item(key = "comments_loading") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            }
                        } else if (!state.hasMore && state.comments.isNotEmpty()) {
                            item(key = "comments_end") {
                                Text(
                                    "没有更多评论了",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 大图预览
    val previewIndex = state.previewIndex
    val images = state.post?.imageUrls.orEmpty()
    if (previewIndex != null && images.isNotEmpty()) {
        ImagePreviewDialog(
            images = images,
            initialIndex = previewIndex.coerceIn(0, images.lastIndex),
            onDismiss = viewModel::closeImagePreview
        )
    }
}

/** 帖子正文区：作者、正文、图片网格、点赞评论栏 */
@Composable
private fun PostContent(
    post: Post,
    onLike: () -> Unit,
    onImageClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageUrlUtil.resolve(post.author.avatarUrl),
                contentDescription = "头像",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(post.author.nickname, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    formatTime(post.createdAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(post.content, fontSize = 16.sp)

        if (post.imageUrls.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ImageGrid(urls = post.imageUrls, onImageClick = onImageClick)
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (post.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "点赞",
                tint = if (post.liked) Color(0xFFFF4D4F) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp).clickable(onClick = onLike)
            )
            Spacer(Modifier.width(4.dp))
            Text(post.likeCount.toString(), fontSize = 14.sp)
            Spacer(Modifier.width(24.dp))
            Icon(
                Icons.AutoMirrored.Outlined.Comment,
                contentDescription = "评论",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(post.commentCount.toString(), fontSize = 14.sp)
        }
    }
}

/** 三列方形图片网格，点击查看大图 */
@Composable
private fun ImageGrid(
    urls: List<String>,
    onImageClick: (Int) -> Unit
) {
    val rows = urls.withIndex().chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowItems.forEach { (index, url) ->
                    AsyncImage(
                        model = ImageUrlUtil.resolve(url),
                        contentDescription = "帖子图片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onImageClick(index) }
                            .height(110.dp)
                    )
                }
                // 最后一行不足 3 张时补白占位，保证图片宽度一致
                repeat(3 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** 单条评论 */
@Composable
private fun CommentItem(comment: Comment) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        AsyncImage(
            model = ImageUrlUtil.resolve(comment.author.avatarUrl),
            contentDescription = "头像",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.author.nickname, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(
                    formatTime(comment.createdAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(comment.content, fontSize = 14.sp)
        }
    }
}

/** 底部评论输入栏 */
@Composable
private fun CommentInputBar(
    value: String,
    sending: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("写下你的评论…") },
            enabled = enabled,
            maxLines = 4,
            shape = RoundedCornerShape(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        if (sending) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(2.dp), strokeWidth = 2.dp)
        } else {
            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank()
            ) {
                Icon(
                    Icons.Outlined.Send,
                    contentDescription = "发送评论",
                    tint = if (enabled && value.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}

/** 全屏大图预览，支持多图左右滑动 */
@Composable
private fun ImagePreviewDialog(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { images.size }
    )
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(state = pagerState) { page ->
                AsyncImage(
                    model = ImageUrlUtil.resolve(images[page]),
                    contentDescription = "大图预览",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
            Text(
                "${pagerState.currentPage + 1}/${images.size}",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
            )
        }
    }
}
