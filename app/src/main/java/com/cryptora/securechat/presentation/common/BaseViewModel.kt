package com.cryptora.securechat.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.navigation.AppNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseViewModel(
    protected val dispatchers: DispatcherProvider,
    protected val navigator: AppNavigator
) : ViewModel() {

    protected fun launchOnIO(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(dispatchers.io) {
            block()
        }
    }

    protected fun launchOnDefault(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(dispatchers.default) {
            block()
        }
    }

    fun navigateTo(route: String) {
        navigator.navigateTo(route)
    }

    fun navigateUp() {
        navigator.navigateUp()
    }
}
