package com.rdbb.jiancircle.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rdbb.jiancircle.ui.screen.detail.PostDetailScreen
import com.rdbb.jiancircle.ui.screen.login.LoginScreen
import com.rdbb.jiancircle.ui.screen.login.RegisterScreen
import com.rdbb.jiancircle.ui.screen.main.MainScreen
import com.rdbb.jiancircle.ui.screen.pulish.PostPublishScreen
import com.rdbb.jiancircle.ui.screen.splash.SplashScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Main.route) { popUpTo(0) }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) { popUpTo(0) }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Main.route) { popUpTo(0) }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                onPostClick = { postId ->
                    navController.navigate(Screen.Detail.createRoute(postId))
                },
                onPublish = { navController.navigate(Screen.Publish.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(Screen.Detail.route) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId")?.toIntOrNull()
            if (postId != null) {
                PostDetailScreen(postId = postId, onBack = { navController.popBackStack() })
            }
        }
        composable(Screen.Publish.route) {
            PostPublishScreen(
                onBack = { navController.popBackStack() },
                // 发布成功返回主界面，MainScreen 在 ON_RESUME 时刷新首页
                onPublishSuccess = { navController.popBackStack() }
            )
        }
    }
}
