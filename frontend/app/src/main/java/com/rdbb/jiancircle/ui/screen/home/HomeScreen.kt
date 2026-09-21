package com.rdbb.jiancircle.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rdbb.jiancircle.ui.screen.home.components.PostCard
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPostClick: (Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // 一次性错误消息（分页失败、点赞失败等）用 Snackbar 提示
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // 滚动到接近底部时自动加载下一页
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            total > 0 && lastVisible >= total - PREFETCH_DISTANCE
        }
            .distinctUntilChanged()
            .collect { nearBottom -> if (nearBottom) viewModel.loadMore() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeSearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchChange,
                onRefresh = viewModel::refresh
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val errorMessage = state.error
            when {
                // 首屏 / 切换关键词加载中
                state.loading && state.posts.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // 加载失败且没有可展示的旧数据
                errorMessage != null && state.posts.isEmpty() -> {
                    StatusView(
                        icon = Icons.Outlined.CloudOff,
                        message = errorMessage,
                        actionText = "点击重试",
                        onAction = viewModel::refresh,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 空列表
                state.posts.isEmpty() -> {
                    val message = if (state.searchQuery.isBlank()) {
                        "还没有帖子，快发布第一条吧"
                    } else {
                        "没有找到「${state.searchQuery.trim()}」相关帖子"
                    }
                    StatusView(
                        icon = Icons.Outlined.Inbox,
                        message = message,
                        actionText = "刷新",
                        onAction = viewModel::refresh,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 信息流列表
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(items = state.posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                onClick = { onPostClick(post.id) },
                                onLike = { viewModel.toggleLike(post.id) }
                            )
                            HorizontalDivider()
                        }

                        if (state.loadingMore) {
                            item(key = LOADING_MORE_KEY) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            }
                        } else if (!state.hasMore) {
                            item(key = NO_MORE_KEY) {
                                Text(
                                    text = "没有更多了",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 顶部搜索栏：圆角输入框 + 清除按钮 + 手动刷新 */
@Composable
private fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // enableEdgeToEdge 下自定义 topBar 需自行让出状态栏高度
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("搜索帖子") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "清除搜索")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "刷新")
        }
    }
}

/** 居中展示的状态占位（加载失败、空列表） */
@Composable
private fun StatusView(
    icon: ImageVector,
    message: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onAction) {
            Text(actionText)
        }
    }
}

/** 距列表底部多少条时触发预加载 */
private const val PREFETCH_DISTANCE = 3
private const val LOADING_MORE_KEY = "loading_more_footer"
private const val NO_MORE_KEY = "no_more_footer"
