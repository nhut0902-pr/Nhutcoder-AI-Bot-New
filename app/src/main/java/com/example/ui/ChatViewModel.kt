package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.MessageEntity
import com.example.network.SpaceZApiService
import com.example.repository.SpaceZRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val PREFS_NAME = "spacez_prefs"
    private val KEY_API_KEY = "api_key"
    private val DEFAULT_API_KEY = "sk-aa7cb65b7a952c7b61730e9bed1961de6c39b343e4133f233115c2fbbff1cdc0"

    private val sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Api Key state (defaulted to your key, completely ready to go!)
    private val _apiKey = MutableStateFlow(sharedPreferences.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY)
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    // Mode Toggle: false = Chat completions, true = Image generations
    private val _isDrawingMode = MutableStateFlow(false)
    val isDrawingMode: StateFlow<Boolean> = _isDrawingMode.asStateFlow()

    // UI loading indicator state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Transient error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Sizing specifications: standard choices
    private val _imageSize = MutableStateFlow("1024x1024")
    val imageSize: StateFlow<String> = _imageSize.asStateFlow()

    private val repository: SpaceZRepository

    init {
        val database = AppDatabase.getDatabase(application)
        val messageDao = database.messageDao()

        // Logging interceptor for deep telemetry/debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://preview-chat-ac8286e7-6c59-44be-852f-a1ebbfd27ab4.space-z.ai/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val apiService = retrofit.create(SpaceZApiService::class.java)
        repository = SpaceZRepository(messageDao, apiService)
    }

    // List of messages ordered chronically
    val messages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Image-only gallery content
    val galleryImages: StateFlow<List<MessageEntity>> = repository.allImages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setDrawingMode(isDrawing: Boolean) {
        _isDrawingMode.value = isDrawing
    }

    fun setImageSize(size: String) {
        _imageSize.value = size
    }

    fun setApiKey(newKey: String) {
        _apiKey.value = newKey
        sharedPreferences.edit().putString(KEY_API_KEY, newKey).apply()
    }

    fun resetApiKey() {
        _apiKey.value = DEFAULT_API_KEY
        sharedPreferences.edit().putString(KEY_API_KEY, DEFAULT_API_KEY).apply()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun deleteMessage(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteMessageById(id)
            } catch (e: Exception) {
                _errorMessage.value = "Không thể xoá: ${e.localizedMessage}"
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                repository.clearChat()
            } catch (e: Exception) {
                _errorMessage.value = "Không thể xoá lịch sử: ${e.localizedMessage}"
            }
        }
    }

    fun handleSend(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty()) return

        val currentModeIsDrawing = _isDrawingMode.value

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Insert user message to DB immediately
            val userMsg = MessageEntity(
                role = "user",
                content = if (currentModeIsDrawing) "🎨 Tạo ảnh: $prompt" else prompt,
                isImage = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(userMsg)

            try {
                val currentKey = _apiKey.value.trim()
                if (currentKey.isEmpty()) {
                    throw IllegalStateException("API Key trống. Vui lòng thiết lập khóa trong cài đặt.")
                }

                if (currentModeIsDrawing) {
                    // Call Image Generation
                    val responseMsg = repository.fetchImageGeneration(
                        apiKey = currentKey,
                        prompt = prompt,
                        size = _imageSize.value
                    )
                    repository.insertMessage(responseMsg)
                } else {
                    // Call Chat Completion
                    val history = messages.value
                    val responseMsg = repository.fetchChatCompletion(
                        apiKey = currentKey,
                        history = history,
                        userMessage = prompt
                    )
                    repository.insertMessage(responseMsg)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Đã xảy ra lỗi không mong đợi"
                
                // Add an error note to feed
                val errorMsg = MessageEntity(
                    role = "assistant",
                    content = "⚠️ Lỗi: ${_errorMessage.value}",
                    isImage = false,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertMessage(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
