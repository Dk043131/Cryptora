package com.cryptora.securechat.core.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.designsystem.CryptoraDimens
import com.cryptora.securechat.presentation.auth.AuthScreen
import com.cryptora.securechat.presentation.auth.AuthViewModel
import com.cryptora.securechat.presentation.home.HomeScreen
import com.cryptora.securechat.presentation.home.HomeViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    LaunchedEffect(key1 = Unit) {
        navigator.navigationCommands.collect { command ->
            when (command) {
                is NavigationCommand.Navigate -> {
                    navController.navigate(command.route) {
                        command.builder?.invoke(this)
                    }
                }
                is NavigationCommand.NavigateUp -> {
                    navController.navigateUp()
                }
                is NavigationCommand.PopUpTo -> {
                    navController.popBackStack(command.route, command.inclusive)
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { (it * 0.25f).toInt() },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(320))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -(it * 0.25f).toInt() },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(280))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -(it * 0.25f).toInt() },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(320))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { (it * 0.25f).toInt() },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(280))
        }
    ) {
        composable(Screen.Auth.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            AuthScreen(viewModel = viewModel)
        }

        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(viewModel = viewModel)
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) {
            val viewModel: com.cryptora.securechat.presentation.chat.ChatViewModel = hiltViewModel()
            com.cryptora.securechat.presentation.chat.ChatScreen(viewModel = viewModel)
        }

        composable(Screen.AccessRequests.route) {
            val viewModel: com.cryptora.securechat.presentation.access.AccessRequestsViewModel = hiltViewModel()
            com.cryptora.securechat.presentation.access.AccessRequestsScreen(viewModel = viewModel)
        }

        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) {
            val viewModel: com.cryptora.securechat.presentation.notes.NotesViewModel = hiltViewModel()
            com.cryptora.securechat.presentation.notes.NotesSheet(
                viewModel = viewModel,
                onDismissRequest = { navigator.navigateUp() }
            )
        }

        composable(
            route = Screen.ForwardHistory.route,
            arguments = listOf(navArgument("messageId") { type = NavType.StringType })
        ) {
            val viewModel: com.cryptora.securechat.presentation.forwarding.ForwardHistoryViewModel = hiltViewModel()
            com.cryptora.securechat.presentation.forwarding.ForwardHistoryScreen(viewModel = viewModel)
        }
    }
}
