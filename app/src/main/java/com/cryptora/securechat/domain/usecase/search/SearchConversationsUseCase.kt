package com.cryptora.securechat.domain.usecase.search

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.repository.ChatRepository
import javax.inject.Inject

class SearchConversationsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(query: String): Resource<List<Conversation>> {
        val clean = query.trim()
        if (clean.isBlank()) {
            return Resource.Success(emptyList())
        }
        return chatRepository.searchConversations(clean)
    }
}
