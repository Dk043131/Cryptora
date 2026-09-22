package com.cryptora.securechat.domain.model

enum class SearchFilterTab(val title: String) {
    ALL("All"),
    USERS("Users"),
    MESSAGES("Messages"),
    NOTES("Notes"),
    CHATS("Chats")
}

data class UnifiedSearchResult(
    val query: String = "",
    val users: List<UserProfile> = emptyList(),
    val messages: List<Message> = emptyList(),
    val notes: List<Note> = emptyList(),
    val conversations: List<Conversation> = emptyList()
) {
    val isEmpty: Boolean
        get() = users.isEmpty() && messages.isEmpty() && notes.isEmpty() && conversations.isEmpty()

    val totalCount: Int
        get() = users.size + messages.size + notes.size + conversations.size
}
