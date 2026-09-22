package com.cryptora.securechat.core.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Auth : Screen("auth")
    data object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String): String = "chat/$conversationId"
    }
    data object NoteEditor : Screen("note/{noteId}") {
        fun createRoute(noteId: String = "new"): String = "note/$noteId"
    }
    data object AccessRequests : Screen("access_requests")
    data object ForwardHistory : Screen("forward_history/{messageId}") {
        fun createRoute(messageId: String): String = "forward_history/$messageId"
    }
    data object Profile : Screen("profile")
}
