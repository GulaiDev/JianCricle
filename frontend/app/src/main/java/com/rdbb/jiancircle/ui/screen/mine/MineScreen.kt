package com.rdbb.jiancircle.ui.screen.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rdbb.jiancircle.ui.screen.home.components.PostCard
import com.rdbb.jiancircle.util.ImageUrlUtil
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MineScreen(
    onPostClick: (Int) -> Unit,
    onLogout: () -> Unit,
    viewModel: MineViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // 加载时机由 MainScreen 的 Tab 切换 / ON_RESUME 统一驱动
    LaunchedEffect(state.toastMessage) {
        val message = state.toastMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(state.logoutSuccess) {
        if (state.logoutSuccess) onLogout()
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            total > 0 && last >= total - 3
        }.distinctUntilChanged().collect { nearBottom ->
            if (nearBottom) viewModel.loadMore()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(state = listState) {
                item(key = "mine_header") {
                    MineHeader(
                        nickname = state.user?.nickname ?: "",
                        bio = state.user?.bio,
                        avatarUrl = state.user?.avatarUrl,
                        onClearCache = viewModel::clearCache,
                        onLogoutClick = { showLogoutConfirm = true }
                    )
                }

                when {
                    state.loading && state.myPosts.isEmpty() -> {
                        item(key = "mine_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    state.error != null && state.myPosts.isEmpty() -> {
                        item(key = "mine_error") {
                            MineStatusView(
                                icon = Icons.Outlined.CloudOff,
                                message = state.error!!,
                                actionText = "点击重试",
                                onAction = viewModel::load
                            )
                        }
                    }

                    state.myPosts.isEmpty() -> {
                        item(key = "mine_empty") {
                            MineStatusView(
                                icon = Icons.Outlined.Inbox,
                                message = "你还没有发布过帖子",
                                actionText = "刷新",
                                onAction = viewModel::load
                            )
                        }
                    }

                    else -> {
                        item(key = "my_posts_title") {
                            Text(
                                "我的帖子",
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                            HorizontalDivider()
                        }
                        items(items = state.myPosts, key = { "my_post_${it.id}" }) { post ->
                            PostCard(
                                post = post,
                                onClick = { onPostClick(post.id) },
                                onLike = { viewModel.toggleLike(post.id) }
                            )
                            HorizontalDivider()
                        }
                        if (state.loadingMore) {
                            item(key = "mine_loading_more") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            }
                        } else if (!state.hasMore) {
                            item(key = "mine_no_more") {
                                Text(
                                    "没有更多了",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.logout()
                }) {
                    Text("退出", color = Color(0xFFFF4D4F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/** 头部：标题、用户资料卡、清缓存/退出操作 */
@Composable
private fun MineHeader(
    nickname: String,
    bio: String?,
    avatarUrl: String?,
    onClearCache: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "我的",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageUrlUtil.resolve(avatarUrl),
                contentDescription = "头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nickname.ifBlank { "加载中…" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!bio.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        bio,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onClearCache,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("清除缓存")
            }
            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("退出登录", color = Color(0xFFFF4D4F))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun MineStatusView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(message, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onAction) { Text(actionText) }
    }
}
