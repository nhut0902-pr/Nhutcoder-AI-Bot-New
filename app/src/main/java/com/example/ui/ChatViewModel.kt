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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class ChatBot(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val greeting: String,
    val emoji: String,
    val isCustom: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val PREFS_NAME = "spacez_prefs"
    private val KEY_API_KEY = "api_key"
    private val DEFAULT_API_KEY = "sk-aa7cb65b7a952c7b61730e9bed1961de6c39b343e4133f233115c2fbbff1cdc0"

    private val KEY_BASE_URL = "base_url"
    private val DEFAULT_BASE_URL = "https://preview-chat-ac8286e7-6c59-44be-852f-a1ebbfd27ab4.space-z.ai/"

    private val sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Preset Bots config
    val PRESET_BOTS = listOf(
        ChatBot(
            id = "companion",
            name = "SpaceZ AI",
            description = "Trợ lý đa năng",
            systemPrompt = "Bạn là SpaceZ AI, một trợ lý đắc lực, thông minh và thân thiện. Hãy trả lời câu hỏi một cách chính xác, ngắn gọn và hữu ích bằng tiếng Việt.",
            greeting = "Dạ, em nghe ạ! Anh/chị cần trợ giúp gì không ạ? 😊",
            emoji = "🤖"
        ),
        ChatBot(
            id = "tutor",
            name = "Gia sư Master",
            description = "Dạy học & Toán Lý Hoá",
            systemPrompt = "Bạn là một gia sư giảng dạy chuyên nghiệp, tài giỏi và kiên nhẫn. Khi trả lời câu hỏi về học tập, toán, lý, hóa, văn, ngoại ngữ, hãy chia nhỏ vấn đề và giải thích cặn kẽ từng bước bài bản để học sinh dễ hiểu nhất.",
            greeting = "Chào bạn! Mình là Gia sư Master giảng dạy học tập. Bạn cần mình giải đáp bài toán hay giải thích bài học nào hôm nay? 🎓",
            emoji = "🎓"
        ),
        ChatBot(
            id = "coder",
            name = "Kỹ sư Code",
            description = "Chuyên gia Lập trình",
            systemPrompt = "Bạn là một Chuyên gia Lập trình, Kỹ sư Phần mềm cấp cao. Luôn đưa ra lời khuyên thiết kế, phân tích mã nguồn một cách tối ưu, cấu trúc sạch đẹp và viết code chính xác kèm giải thích.",
            greeting = "Kỹ sư Code xin chào! Bạn đang xây dựng dự án gì đấy? Gửi lỗi hoặc đoạn code cần debug qua đây cho mình nhé! 💻",
            emoji = "💻"
        ),
        ChatBot(
            id = "philosopher",
            name = "Triết gia Logic",
            description = "Lập luận sâu sắc",
            systemPrompt = "Bạn là một Triết gia logic tài ba, có lối tư duy phản biện. Bạn thích phân tích vấn đề đa chiều và chia sẻ suy luận khách quan.",
            greeting = "Xin chào nhà tư tưởng! Hãy cùng suy luận và phản biện bất cứ chủ đề hay câu hỏi hóc búa nào bạn muốn nhé. 🧠",
            emoji = "🧠"
        ),
        ChatBot(
            id = "english",
            name = "English Coach",
            description = "Luyện Tiếng Anh",
            systemPrompt = "You are an expert English Coach. Correct any grammar mistakes in user's Vietnamese or English messages, suggest better vocabulary, and keep the responses highly encouraging. Speak predominantly in English.",
            greeting = "Hi there! I am your English Coach. Ready to level up your English skills? Ask me anything or just chat with me in English! 🇬🇧",
            emoji = "🇬🇧"
        ),
        ChatBot(
            id = "alien",
            name = "Hành tinh Xoẹt",
            description = "Vui nhộn & Hài hước",
            systemPrompt = "Bạn là người ngoài hành tinh từ hành tinh Xoẹt xa xôi, vô cùng hài hước, dí dỏm. Luôn dùng các biểu tượng đĩa bay, UFO, người ngoài hành tinh và trả lời đầy ngộ nghĩnh, xem Trái Đất là nơi thích khám phá.",
            greeting = "Xoẹt... Xoẹt! Sinh vật Trái Đất nghe rõ trả lời! Ta vừa đáp UFO xuống đây để kết bạn với ngươi đây! 👽🚀",
            emoji = "👽"
        )
    )

    // File based Custom bots helper
    private fun saveCustomBotsToPrefs(bots: List<ChatBot>) {
        val serialized = bots.filter { it.isCustom }.joinToString(";;;") { 
            "${it.id}:::${it.name}:::${it.description}:::${it.systemPrompt}:::${it.greeting}:::${it.emoji}"
        }
        sharedPreferences.edit().putString("serialized_bots", serialized).apply()
    }

    private fun loadCustomBotsFromPrefs(): List<ChatBot> {
        val serialized = sharedPreferences.getString("serialized_bots", "") ?: ""
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(";;;").mapNotNull { block ->
            val parts = block.split(":::")
            if (parts.size >= 6) {
                ChatBot(
                    id = parts[0],
                    name = parts[1],
                    description = parts[2],
                    systemPrompt = parts[3],
                    greeting = parts[4],
                    emoji = parts[5],
                    isCustom = true
                )
            } else null
        }
    }

    // Active custom bots state list
    private val _customBots = MutableStateFlow<List<ChatBot>>(emptyList())
    val customBots: StateFlow<List<ChatBot>> = _customBots.asStateFlow()

    // All available bots helper list
    private val _allBots = MutableStateFlow<List<ChatBot>>(emptyList())
    val allBots: StateFlow<List<ChatBot>> = _allBots.asStateFlow()

    private fun updateAllBotsList() {
        val list = mutableListOf<ChatBot>()
        list.addAll(PRESET_BOTS)
        list.addAll(_customBots.value)
        _allBots.value = list
    }

    // Selected bot state
    private val _selectedBot = MutableStateFlow<ChatBot>(PRESET_BOTS[0])
    val selectedBot: StateFlow<ChatBot> = _selectedBot.asStateFlow()

    // Reasoning/Thinking mode
    private val _isReasoningMode = MutableStateFlow(false)
    val isReasoningMode: StateFlow<Boolean> = _isReasoningMode.asStateFlow()

    // Api Key state (defaulted to your key, completely ready to go!)
    private val _apiKey = MutableStateFlow(sharedPreferences.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY)
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    // Base URL state
    private val _baseUrl = MutableStateFlow(sharedPreferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

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

    private fun createApiService(url: String): SpaceZApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val formattedUrl = if (url.endsWith("/")) url else "$url/"

        val retrofit = Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        return retrofit.create(SpaceZApiService::class.java)
    }

    init {
        val database = AppDatabase.getDatabase(application)
        val messageDao = database.messageDao()
        val apiService = createApiService(_baseUrl.value)
        repository = SpaceZRepository(messageDao, apiService)

        // Initialize lists
        val loadedCustom = loadCustomBotsFromPrefs()
        _customBots.value = loadedCustom
        updateAllBotsList()

        // Load active bot preference
        val activeBotId = sharedPreferences.getString("selected_bot_id", "companion") ?: "companion"
        val activeBot = _allBots.value.find { it.id == activeBotId } ?: PRESET_BOTS[0]
        _selectedBot.value = activeBot

        // Load reasoning mode preference
        _isReasoningMode.value = sharedPreferences.getBoolean("is_reasoning_mode", false)
    }

    fun selectBot(bot: ChatBot) {
        _selectedBot.value = bot
        sharedPreferences.edit().putString("selected_bot_id", bot.id).apply()
        
        // Insert entry welcoming greeting from the selected bot
        viewModelScope.launch {
            val welcomeMsg = MessageEntity(
                role = "assistant",
                content = bot.greeting,
                isImage = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(welcomeMsg)
        }
    }

    fun addCustomBot(name: String, desc: String, systemPrompt: String, greeting: String, emoji: String) {
        val newBot = ChatBot(
            id = "custom_" + System.currentTimeMillis(),
            name = name,
            description = desc,
            systemPrompt = systemPrompt,
            greeting = greeting,
            emoji = emoji,
            isCustom = true
        )
        val current = _customBots.value.toMutableList()
        current.add(newBot)
        _customBots.value = current
        saveCustomBotsToPrefs(current)
        updateAllBotsList()
    }

    fun setReasoningMode(enabled: Boolean) {
        _isReasoningMode.value = enabled
        sharedPreferences.edit().putBoolean("is_reasoning_mode", enabled).apply()
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

    fun setBaseUrl(newUrl: String) {
        val trimmed = newUrl.trim()
        _baseUrl.value = trimmed
        sharedPreferences.edit().putString(KEY_BASE_URL, trimmed).apply()
        
        try {
            val newService = createApiService(trimmed)
            repository.updateApiService(newService)
        } catch (e: Exception) {
            _errorMessage.value = "URL không hợp lệ: ${e.localizedMessage}"
        }
    }

    fun resetBaseUrl() {
        _baseUrl.value = DEFAULT_BASE_URL
        sharedPreferences.edit().putString(KEY_BASE_URL, DEFAULT_BASE_URL).apply()
        val newService = createApiService(DEFAULT_BASE_URL)
        repository.updateApiService(newService)
    }

    fun parseAndApplyCurl(curlCommand: String): Boolean {
        val trimmed = curlCommand.trim()
        if (!trimmed.lowercase().startsWith("curl")) {
            return false
        }

        val bearerRegex = """Authorization:\s*Bearer\s+([^\s"'\\]+)""".toRegex(RegexOption.IGNORE_CASE)
        val bearerMatch = bearerRegex.find(trimmed)
        val token = bearerMatch?.groupValues?.get(1)

        val urlRegex = """https?://[^\s"'\\]+""".toRegex()
        val urlMatch = urlRegex.find(trimmed)
        val fullUrl = urlMatch?.value

        var applied = false
        if (token != null && token.isNotEmpty() && token != "YOUR_API_KEY") {
            setApiKey(token)
            applied = true
        }

        if (fullUrl != null && fullUrl.isNotEmpty()) {
            try {
                val parsedUrl = fullUrl.toHttpUrlOrNull()
                if (parsedUrl != null) {
                    val pathSegments = parsedUrl.pathSegments
                    // Rebuild the host part, stripping path suffix
                    val baseUrlRebuilt = "${parsedUrl.scheme}://${parsedUrl.host}${if (parsedUrl.port != 80 && parsedUrl.port != 443) ":${parsedUrl.port}" else ""}/"
                    setBaseUrl(baseUrlRebuilt)
                    applied = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return applied
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

    fun handleSend(text: String, attachedImageB64: String? = null) {
        val prompt = text.trim()
        if (prompt.isEmpty() && attachedImageB64 == null) return

        val currentModeIsDrawing = _isDrawingMode.value

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Insert user message to DB immediately
            val userMsg = MessageEntity(
                role = "user",
                content = if (currentModeIsDrawing) "🎨 Tạo ảnh: $prompt" else prompt,
                isImage = false,
                imageUrl = attachedImageB64,
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
                    // Inject system prompt with bot configuration and optional reasoning instructions
                    val bot = _selectedBot.value
                    var systemPromptToSend = bot.systemPrompt
                    
                    if (_isReasoningMode.value) {
                        systemPromptToSend += "\n[Yêu cầu quan trọng: Hãy bật tư duy suy luận logic đỉnh cao. Định dạng câu trả lời bằng cách đặt suy nghĩ, lập luận từng bước của bạn trong cặp thẻ <think>...</think> ở đầu, rồi chia sẻ câu trả lời đầy đủ chính thức bám sát bên dưới thẻ đóng.]"
                    }
                    
                    val enrichedPrompt = if (attachedImageB64 != null) {
                        "[Hình ảnh đính kèm đã tải lên thành công] Câu hỏi/mô tả của người dùng về bức ảnh này: $prompt\n(Hãy giả lập khả năng thị giác phân tích ảnh này nếu bạn chưa truy xuất trực tiếp được, hãy trả lời cực kỳ tinh tế và sát sườn)"
                    } else {
                        prompt
                    }

                    // Call Chat Completion
                    val history = messages.value
                    val responseMsg = repository.fetchChatCompletion(
                        apiKey = currentKey,
                        history = history,
                        userMessage = enrichedPrompt,
                        systemPrompt = systemPromptToSend
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
