package com.rdbb.jiancircle.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Register : Screen("register")
    object Login : Screen("login")
    /** 主框架（底部导航：首页 / 发布 / 我的） */
    object Main : Screen("main")
    object Publish : Screen("publish")
    object Detail : Screen("post_detail/{postId}") {
        fun createRoute(postId: Int): String = "post_detail/$postId"
    }
}
