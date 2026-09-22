package com.cryptora.securechat.core.navigation

import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NavigationCommand {
    data class Navigate(val route: String, val builder: (NavOptionsBuilder.() -> Unit)? = null) : NavigationCommand
    data object NavigateUp : NavigationCommand
    data class PopUpTo(val route: String, val inclusive: Boolean) : NavigationCommand
}

interface AppNavigator {
    val navigationCommands: Flow<NavigationCommand>
    fun navigateTo(route: String, builder: (NavOptionsBuilder.() -> Unit)? = null)
    fun navigateUp()
    fun popUpTo(route: String, inclusive: Boolean)
}

@Singleton
class AppNavigatorImpl @Inject constructor() : AppNavigator {
    private val _navigationCommands = Channel<NavigationCommand>(Channel.BUFFERED)
    override val navigationCommands: Flow<NavigationCommand> = _navigationCommands.receiveAsFlow()

    override fun navigateTo(route: String, builder: (NavOptionsBuilder.() -> Unit)?) {
        _navigationCommands.trySend(NavigationCommand.Navigate(route, builder))
    }

    override fun navigateUp() {
        _navigationCommands.trySend(NavigationCommand.NavigateUp)
    }

    override fun popUpTo(route: String, inclusive: Boolean) {
        _navigationCommands.trySend(NavigationCommand.PopUpTo(route, inclusive))
    }
}
