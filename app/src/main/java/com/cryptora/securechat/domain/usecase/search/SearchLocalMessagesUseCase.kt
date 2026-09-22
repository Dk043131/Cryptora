package com.cryptora.securechat.domain.usecase.search

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.repository.MessageRepository
import javax.inject.Inject

class SearchLocalMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Searches local decrypted message cache strictly on-device.
     * No search queries are ever transmitted to any remote server.
     */
    suspend operator fun invoke(query: String): Resource<List<Message>> {
        val clean = query.trim()
        if (clean.isBlank()) {
            return Resource.Success(emptyList())
        }
        return messageRepository.searchLocalMessages(clean)
    }
}
