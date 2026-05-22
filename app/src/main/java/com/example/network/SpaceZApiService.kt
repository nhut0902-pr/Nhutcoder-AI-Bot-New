package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class ChatRequest(
    @Json(name = "model") val model: String = "cloud-ai",
    @Json(name = "messages") val messages: List<ChatMessageDto>,
    @Json(name = "temperature") val temperature: Double = 0.7,
    @Json(name = "max_tokens") val maxTokens: Int = 1024
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
    @Json(name = "index") val index: Int,
    @Json(name = "message") val message: ChatMessageDto,
    @Json(name = "finish_reason") val finishReason: String?
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "object") val objectType: String?,
    @Json(name = "created") val created: Long?,
    @Json(name = "model") val model: String?,
    @Json(name = "choices") val choices: List<ChatChoice>?
)

@JsonClass(generateAdapter = true)
data class ImageRequest(
    @Json(name = "model") val model: String = "cloud-ai",
    @Json(name = "prompt") val prompt: String,
    @Json(name = "n") val n: Int = 1,
    @Json(name = "size") val size: String = "1024x1024"
)

@JsonClass(generateAdapter = true)
data class ImageData(
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class ImageResponse(
    @Json(name = "created") val created: Long?,
    @Json(name = "data") val data: List<ImageData>?
)

interface SpaceZApiService {
    @POST("api/v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse

    @POST("api/v1/images/generations")
    suspend fun generateImage(
        @Header("Authorization") authorization: String,
        @Body request: ImageRequest
    ): ImageResponse
}
