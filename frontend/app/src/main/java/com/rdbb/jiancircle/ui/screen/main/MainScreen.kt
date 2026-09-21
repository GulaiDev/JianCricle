package com.rdbb.jiancircle.ui.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rdbb.jiancircle.ui.screen.home.HomeScreen
import com.rdbb.jiancircle.ui.screen.home.HomeViewModel
import com.rdbb.jiancircle.ui.screen.mine.MineScreen
import com.rdbb.jiancircle.ui.screen.mine.MineViewModel

private const val TAB_HOME = 0
private const val TAB_MINE = 1

/**
 * 主框架：底部导航承载首页 / 我的两个 Tab，
 * 中间发布按钮跳转到独立的全屏发帖页。
 * HomeViewModel / MineViewModel 绑定在 Main 的导航栈条目上，
 * 子页面跳转返回后状态保留。
 */
@Composable
fun MainScreen(
    onPostClick: (Int) -> Unit,
    onPublish: () -> Unit,
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    mineViewModel: MineViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_HOME) }

    // 首次切到"我的"时加载资料；之后每次切回静默刷新
    LaunchedEffect(selectedTab) {
        if (selectedTab == TAB_MINE) mineViewModel.onVisible()
    }

    // 从发帖页 / 详情页返回主界面时静默同步首页最新数据
    val lifecycleOwner = LocalLifecycleOwner.current
    var firstResume by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResume) {
                    firstResume = false
                } else {
                    homeViewModel.refresh()
                    if (selectedTab == TAB_MINE) mineViewModel.onVisible()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == TAB_HOME,
                    onClick = { selectedTab = TAB_HOME },
                    icon = {
                        Icon(
                            if (selectedTab == TAB_HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "首页"
                        )
                    },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onPublish,
                    icon = {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = "发布",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = { Text("发布") }
                )
                NavigationBarItem(
                    selected = selectedTab == TAB_MINE,
                    onClick = { selectedTab = TAB_MINE },
                    icon = {
                        Icon(
                            if (selectedTab == TAB_MINE) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "我的"
                        )
                    },
                    label = { Text("我的") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                TAB_HOME -> HomeScreen(
                    viewModel = homeViewModel,
                    onPostClick = onPostClick
                )

                TAB_MINE -> MineScreen(
                    viewModel = mineViewModel,
                    onPostClick = onPostClick,
                    onLogout = onLogout
                )
            }
        }
    }
}
