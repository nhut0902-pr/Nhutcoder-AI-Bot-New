package com.example.repository

import com.example.database.MessageDao
import com.example.database.MessageEntity
import com.example.network.ChatMessageDto
import com.example.network.ChatRequest
import com.example.network.ImageRequest
import com.example.network.SpaceZApiService
import kotlinx.coroutines.flow.Flow

class SpaceZRepository(
    private val messageDao: MessageDao,
    private var apiService: SpaceZApiService
) {
    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()
    val allImages: Flow<List<MessageEntity>> = messageDao.getGeneratedImages()

    fun updateApiService(newApiService: SpaceZApiService) {
        this.apiService = newApiService
    }

    suspend fun insertMessage(message: MessageEntity): Long {
        return messageDao.insertMessage(message)
    }

    suspend fun deleteMessageById(id: Int) {
        messageDao.deleteMessageById(id)
    }

    suspend fun clearChat() {
        messageDao.deleteAllMessages()
    }

    // Call Chat Completions API
    suspend fun fetchChatCompletion(
        apiKey: String,
        history: List<MessageEntity>,
        userMessage: String
    ): MessageEntity {
        // Build the messages payload
        val messagesDto = mutableListOf<ChatMessageDto>()
        
        // Add context from recent history (excluding image messages)
        history.filter { !it.isImage }
            .takeLast(10) // up to 10 context messages
            .forEach { msg ->
                messagesDto.add(ChatMessageDto(role = msg.role, content = msg.content))
            }
        
        // Add the current user query
        messagesDto.add(ChatMessageDto(role = "user", content = userMessage))

        val request = ChatRequest(
            model = "cloud-ai",
            messages = messagesDto,
            temperature = 0.7,
            maxTokens = 1024
        )

        val authorizationHeader = "Bearer $apiKey"
        val response = apiService.getChatCompletion(authorizationHeader, request)
        
        val assistantContent = response.choices?.firstOrNull()?.message?.content 
            ?: throw Exception("Không có phản hồi từ máy chủ")

        return MessageEntity(
            role = "assistant",
            content = assistantContent,
            isImage = false,
            timestamp = System.currentTimeMillis()
        )
    }

    // Call Image Generation API
    suspend fun fetchImageGeneration(
        apiKey: String,
        prompt: String,
        size: String = "1024x1024"
    ): MessageEntity {
        val request = ImageRequest(
            prompt = prompt,
            n = 1,
            size = size
        )

        val authorizationHeader = "Bearer $apiKey"
        val response = apiService.generateImage(authorizationHeader, request)
        
        val imageUrl = response.data?.firstOrNull()?.url
            ?: throw Exception("Không thể tạo được ảnh từ phản hồi API")

        return MessageEntity(
            role = "assistant",
            content = prompt,
            isImage = true,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis()
        )
    }
}
