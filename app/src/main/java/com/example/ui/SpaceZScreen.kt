package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.database.MessageEntity
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun rememberAuthenticatedImageRequest(url: String?, apiKey: String, baseUrl: String): Any? {
    val context = LocalContext.current
    return remember(url, apiKey, baseUrl) {
        if (url == null || url.isEmpty()) return@remember null
        
        // Handle Base64 Data URIs directly
        if (url.startsWith("data:image/") && url.contains("base64,")) {
            try {
                val base64Data = url.substringAfter("base64,")
                val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                return@remember ImageRequest.Builder(context)
                    .data(decodedBytes)
                    .crossfade(true)
                    .build()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Resolve relative URLs if needed
        val resolvedUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            else -> {
                val cleanedBase = baseUrl.trimEnd('/')
                val cleanedPath = url.trimStart('/')
                "$cleanedBase/$cleanedPath"
            }
        }
        
        ImageRequest.Builder(context)
            .data(resolvedUrl)
            .setHeader("Authorization", "Bearer $apiKey")
            .crossfade(true)
            .build()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceZScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val galleryImages by viewModel.galleryImages.collectAsStateWithLifecycle()
    val isDrawingMode by viewModel.isDrawingMode.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val imageSize by viewModel.imageSize.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val selectedBot by viewModel.selectedBot.collectAsStateWithLifecycle()
    val allBots by viewModel.allBots.collectAsStateWithLifecycle()
    val isReasoningMode by viewModel.isReasoningMode.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) }
    var selectedDetailImage by remember { mutableStateOf<MessageEntity?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Observe error state and show toast
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedBot.emoji,
                                fontSize = 20.sp
                            )
                        }
                        Column {
                            Text(
                                text = selectedBot.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isReasoningMode) "Chế độ suy luận logic đang bật" else selectedBot.description,
                                fontSize = 10.sp,
                                color = if (isReasoningMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = if (isReasoningMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.testTag("quick_api_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = "Thiết lập nhanh API Key",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (activeTab == 0 && messages.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllHistory() },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Xóa hội thoại",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Trò chuyện") },
                    label = { Text("Trò chuyện") },
                    modifier = Modifier.testTag("nav_tab_chat")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Triển lãm") },
                    label = { Text("Triển lãm") },
                    modifier = Modifier.testTag("nav_tab_gallery")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Cấu hình") },
                    label = { Text("Cấu hình") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                0 -> ChatTabContent(
                    messages = messages,
                    isDrawingMode = isDrawingMode,
                    isLoading = isLoading,
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    selectedBot = selectedBot,
                    allBots = allBots,
                    isReasoningMode = isReasoningMode,
                    onSelectBot = { viewModel.selectBot(it) },
                    onAddCustomBot = { name, desc, system, greet, emoji -> viewModel.addCustomBot(name, desc, system, greet, emoji) },
                    onToggleReasoningMode = { viewModel.setReasoningMode(it) },
                    onToggleDrawingMode = { viewModel.setDrawingMode(it) },
                    onSendMessage = { text, img -> viewModel.handleSend(text, img) },
                    onDeleteMessage = { viewModel.deleteMessage(it) },
                    onConfigApiKey = { showApiKeyDialog = true }
                )
                1 -> GalleryTabContent(
                    images = galleryImages,
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    onImageClick = { selectedDetailImage = it }
                )
                2 -> SettingsTabContent(
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    imageSize = imageSize,
                    onSaveApiKey = { viewModel.setApiKey(it) },
                    onResetApiKey = { viewModel.resetApiKey() },
                    onSaveBaseUrl = { viewModel.setBaseUrl(it) },
                    onResetBaseUrl = { viewModel.resetBaseUrl() },
                    onSaveSize = { viewModel.setImageSize(it) },
                    onClearHistory = { viewModel.clearAllHistory() }
                )
            }
        }
    }

    // Interactive details dialog
    selectedDetailImage?.let { msg ->
        ImageDetailsDialog(
            message = msg,
            apiKey = apiKey,
            baseUrl = baseUrl,
            onDismiss = { selectedDetailImage = null },
            onDelete = {
                viewModel.deleteMessage(msg.id)
                selectedDetailImage = null
                Toast.makeText(context, "Đã xóa tác phẩm khỏi thư viện", Toast.LENGTH_SHORT).show()
            },
            onCopyPrompt = {
                clipboardManager.setText(AnnotatedString(msg.content))
                Toast.makeText(context, "Đã sao chép prompt vẽ", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showApiKeyDialog) {
        QuickApiKeyDialog(
            currentApiKey = apiKey,
            currentBaseUrl = baseUrl,
            onDismiss = { showApiKeyDialog = false },
            onSave = { key, url ->
                viewModel.setApiKey(key)
                viewModel.setBaseUrl(url)
                showApiKeyDialog = false
            },
            onReset = {
                viewModel.resetApiKey()
                viewModel.resetBaseUrl()
                showApiKeyDialog = false
            },
            onParseCurl = { curl ->
                viewModel.parseAndApplyCurl(curl)
            }
        )
    }
}

private fun sendLocalNotification(context: android.content.Context, title: String, message: String) {
    try {
        val channelId = "spacez_chat_updates"
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "SpaceZ AI Updates",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông tin phản hồi từ AI"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            
        notificationManager.notify(1001, builder.build())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun getMorningGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Chào buổi sáng rực rỡ! ☀️"
        in 12..17 -> "Chào buổi chiều an lành! 🌅"
        in 18..21 -> "Chào buổi tối ấm áp! 🌌"
        else -> "Đêm khuya rồi, chúc ngủ ngon! 🌙"
    }
}

private fun getQuoteOfTheDay(): String {
    val quotes = listOf(
        "Kỹ thuật là biến kiến thức khoa học thành giải pháp thực tế.",
        "Mỗi dòng code đều là một bước tiến mới của sáng tạo.",
        "Sự xuất sắc không phải là một hành động mà là một thói quen.",
        "Hãy luôn tò mò, khám phá những chân trời công nghệ mới.",
        "Mọi vấn đề phức tạp đều có thể giải quyết nếu chia đủ nhỏ.",
        "Bắt đầu ngày mới tràn đầy cảm hứng sáng tạo cùng SpaceZ AI!"
    )
    val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
    return quotes[day % quotes.size]
}

@Composable
fun ChatTabContent(
    messages: List<MessageEntity>,
    isDrawingMode: Boolean,
    isLoading: Boolean,
    apiKey: String,
    baseUrl: String,
    selectedBot: ChatBot,
    allBots: List<ChatBot>,
    isReasoningMode: Boolean,
    onSelectBot: (ChatBot) -> Unit,
    onAddCustomBot: (String, String, String, String, String) -> Unit,
    onToggleReasoningMode: (Boolean) -> Unit,
    onToggleDrawingMode: (Boolean) -> Unit,
    onSendMessage: (String, String?) -> Unit,
    onDeleteMessage: (Int) -> Unit,
    onConfigApiKey: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var attachedImageB64 by remember { mutableStateOf<String?>(null) }
    var showAddBotDialog by remember { mutableStateOf(false) }

    // State parameters for newly integrated smart widgets
    var isBackgroundChatEnabled by remember { mutableStateOf(false) }
    var isWidgetHubExpanded by remember { mutableStateOf(true) }
    var showGreetingDialog by remember { mutableStateOf(false) }
    var showAiChatDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    // Request permission launcher for local notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        isBackgroundChatEnabled = isGranted
        if (isGranted) {
            Toast.makeText(context, "Đã bật chế độ nền & thông báo nhận tin! 🔔", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Chưa cấp quyền thông báo. Vui lòng cấp quyền trong cài đặt.", Toast.LENGTH_SHORT).show()
        }
    }

    val checkAndToggleBackgroundMode = {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                isBackgroundChatEnabled = !isBackgroundChatEnabled
                if (isBackgroundChatEnabled) {
                    Toast.makeText(context, "Đã bật chế độ nền & thông báo nhận tin! 🔔", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Đã tắt chế độ nền.", Toast.LENGTH_SHORT).show()
                }
            } else {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            isBackgroundChatEnabled = !isBackgroundChatEnabled
            if (isBackgroundChatEnabled) {
                Toast.makeText(context, "Đã bật chế độ nền & thông báo nhận tin! 🔔", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Đã tắt chế độ nền.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Trigger local notification when a new bot message response arrives in background or active mode
    var lastIsLoading by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (lastIsLoading && !isLoading && isBackgroundChatEnabled) {
            val lastMessage = messages.lastOrNull { it.role != "user" }
            if (lastMessage != null) {
                sendLocalNotification(
                    context = context,
                    title = "✨ Phản hồi từ ${selectedBot.name}",
                    message = lastMessage.content
                )
            }
        }
        lastIsLoading = isLoading
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val maxDimension = 800
                    val width = bitmap.width
                    val height = bitmap.height
                    val finalBitmap = if (width > maxDimension || height > maxDimension) {
                        val ratio = width.toFloat() / height.toFloat()
                        val (newWidth, newHeight) = if (width > height) {
                            Pair(maxDimension, (maxDimension / ratio).toInt())
                        } else {
                            Pair((maxDimension * ratio).toInt(), maxDimension)
                        }
                        Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                    } else {
                        bitmap
                    }

                    val outputStream = ByteArrayOutputStream()
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                    val bytes = outputStream.toByteArray()
                    val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                    attachedImageB64 = "data:image/jpeg;base64,$base64String"
                    
                    if (finalBitmap != bitmap) {
                        finalBitmap.recycle()
                    }
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Không thể đọc và tối ưu ảnh: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-scroll logic on new responses
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Switcher for Bot Personas
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(allBots.size) { index ->
                val bot = allBots[index]
                val isSelected = bot.id == selectedBot.id
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .clickable { onSelectBot(bot) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(bot.emoji, fontSize = 14.sp)
                        Text(
                            text = bot.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Custom bot creation button
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .clickable { showAddBotDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("➕", fontSize = 12.sp)
                        Text(
                            text = "Tạo Bot",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ⚡ Trung tâm Widgets và Tiện ích Thông minh SpaceZ
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isWidgetHubExpanded = !isWidgetHubExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = "Widgets",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "⚡ Tiện ích thông minh SpaceZ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = "MỚI",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isWidgetHubExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isWidgetHubExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // WIDGET 1: CHÀO BUỔI SÁNG
                            item {
                                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                                val (greetingName, iconEmoji) = when (currentHour) {
                                    in 5..11 -> Pair("Chào Buổi Sáng", "☀️")
                                    in 12..17 -> Pair("Chào Buổi Chiều", "🌅")
                                    in 18..21 -> Pair("Chào Buổi Tối", "🌌")
                                    else -> Pair("Chào Bạn Đêm Khuya", "🌙")
                                }
                                Card(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clickable { showGreetingDialog = true },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(text = iconEmoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = greetingName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Gợi ý & quote ngày",
                                            fontSize = 8.5.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            // WIDGET 2: AI CHAT CHUYÊN SÂU
                            item {
                                Card(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clickable { showAiChatDialog = true },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(text = "🤖", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Ý tưởng AI Chat",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Dùng thử prompt mẫu",
                                            fontSize = 8.5.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            // WIDGET 3: CHAT CHẾ ĐỘ NỀN
                            item {
                                Card(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clickable { checkAndToggleBackgroundMode() },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isBackgroundChatEnabled) 
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f) 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isBackgroundChatEnabled) MaterialTheme.colorScheme.tertiary else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "🛡️", fontSize = 20.sp)
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isBackgroundChatEnabled) Color.Green else Color.LightGray)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Chế Độ Nền",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isBackgroundChatEnabled) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (isBackgroundChatEnabled) "Đang bật chạy nền 🟢" else "Nhấp để bật đẩy nền",
                                            fontSize = 8.5.sp,
                                            color = if (isBackgroundChatEnabled) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            // WIDGET 4: CHIA SẺ MÀN HÌNH / SCREEN TRANSCRIPT
                            item {
                                Card(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clickable { showShareDialog = true },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(text = "📤", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Chia sẻ cuộc họp",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Tóm tắt & chia sẻ",
                                            fontSize = 8.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- HỘI THOẠI POPUPS VÀ BẢNG THÔNG TIN ---

        // 1. Dialog Chào Buổi Sáng
        if (showGreetingDialog) {
            Dialog(onDismissRequest = { showGreetingDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("☀️", fontSize = 28.sp)
                            Column {
                                Text(
                                    text = getMorningGreeting(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = java.text.SimpleDateFormat("EEEE, dd/MM/yyyy", java.util.Locale("vi", "VN")).format(java.util.Date()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Quote section
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💡 Câu nói truyền cảm hứng sáng nay:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${getQuoteOfTheDay()}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = "✍️ Chọn nhanh hành động sáng nay:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        val morningPrompts = listOf(
                            Pair("🗓️ Lập lịch làm việc", "Chào buổi sáng! Hãy giúp tôi lập một thời khóa biểu làm việc cá nhân hiệu quả và cân bằng nhất cho hôm nay."),
                            Pair("☕ mini-project rèn luyện", "Tôi muốn làm ấm bộ não lập trình sáng nay, hãy giao cho tôi một thử thách thuật toán Kotlin/Jetpack Compose cơ bản kèm gợi ý cách giải chi tiết."),
                            Pair("📝 Top 5 việc ưu tiên", "Hãy tạo một danh sách checklist 5 việc cần làm của lập trình viên chuẩn mực buổi sáng để giữ tỉnh táo và năng suất tối đa.")
                        )

                        morningPrompts.forEach { (title, prompt) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        inputText = prompt
                                        showGreetingDialog = false
                                        Toast.makeText(context, "Đã chọn nhiệm vụ buổi sáng! Chúc bạn ngày mới tốt lành 🚀", Toast.LENGTH_SHORT).show()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showGreetingDialog = false }) {
                                Text("Đóng", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Dialog Các Ý Tưởng AI Chat Gợi Ý
        if (showAiChatDialog) {
            Dialog(onDismissRequest = { showAiChatDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🤖 Gợi ý phím tắt hội thoại AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Chọn một prompt cấu trúc chất lượng cao dưới đây để khởi động trợ lý ảo của bạn nhanh chóng:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        val presets = listOf(
                            Triple("🎓 Luyện tiếng Anh", "English Practice", "Hãy đóng vai là một chuyên gia giáo dục và giáo viên dạy tiếng Anh vui tính. Gợi ý cho tôi một chủ đề cuộc sống thú vị để bắt đầu luyện tập hội thoại nhé."),
                            Triple("💻 Viết & Gỡ Lỗi Code", "Code Debugger", "Hãy đóng vai là Kỹ sư phần mềm xuất sắc. Tôi đang cần tư vấn giải pháp viết mã tối ưu bộ nhớ khi vẽ canvas trong Jetpack Compose. Gợi ý cho tôi các tips tốt nhất."),
                            Triple("📝 Tóm tắt tri thức", "Smart Explainer", "Giải thích cơ chế hoạt động của mô hình ngôn ngữ lớn (LLM) và sinh ảnh AI (Diffusion Model) bằng một ngôn ngữ siêu giản dị, ẩn dụ thực tế ví dụ dễ hiểu cho trẻ em 10 tuổi.")
                        )

                        presets.forEach { (label, subtitle, textVal) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        inputText = textVal
                                        showAiChatDialog = false
                                        Toast.makeText(context, "Đã áp dụng mẫu prompt phím tắt AI! 🧠", Toast.LENGTH_SHORT).show()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "($subtitle)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = textVal,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAiChatDialog = false }) {
                                Text("Đóng", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Dialog Chia Sẻ Màn Hình (Screen Transcript)
        if (showShareDialog) {
            Dialog(onDismissRequest = { showShareDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📤", fontSize = 28.sp)
                            Column {
                                Text(
                                    text = "Chia sẻ cuộc trò chuyện",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Đang kết nối xuất bản và chia sẻ màn hình thông minh...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        if (messages.isEmpty()) {
                            Text(
                                text = "Lịch sử trò chuyện hiện trống. Hãy gửi vài tin nhắn trước khi thực hiện chia sẻ màn hình hội thoại nhé!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            )
                        } else {
                            Text(
                                text = "Hội thọại đã chọn gồm ${messages.size} tin nhắn cùng ${selectedBot.emoji} ${selectedBot.name}. Bạn có thể tóm tắt nhanh và chia sẻ nội dung này:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Mock scrollable graphic view of the formatted layout to simulate screen share sheet
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    item {
                                        Text(
                                            text = "🌌 MOCK BANNER SHARE SHEET - SPACEZ AI",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    items(messages.take(3)) { m ->
                                        Text(
                                            text = "${if (m.role == "user") "👥 Bạn" else "${selectedBot.emoji} ${selectedBot.name}"}: ${m.content}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (messages.size > 3) {
                                        item {
                                            Text(
                                                text = "... và ${messages.size - 3} tin nhắn khác ...",
                                                fontSize = 8.sp,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val textBuilder = java.lang.StringBuilder()
                                        textBuilder.append("🌌 NHẬT KÝ CHIA SẺ SPACEZ AI AT: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}\n")
                                        textBuilder.append("Trợ lý ảo: ${selectedBot.emoji} ${selectedBot.name}\n")
                                        textBuilder.append("==================================================\n\n")
                                        messages.forEach { msg ->
                                            val authorName = if (msg.role == "user") "Bạn" else selectedBot.name
                                            textBuilder.append("[${if (msg.role == "user") "Người dùng" else "Trợ lý Robo"}] $authorName: ${msg.content}\n\n")
                                        }
                                        textBuilder.append("==================================================\n")
                                        textBuilder.append("Được tạo và tóm tắt thông thái bằng SpaceZ AI.")

                                        clipboardManager.setText(AnnotatedString(textBuilder.toString()))
                                        Toast.makeText(context, "Đã sao chép nội dung hội thoại cuộc họp vào khay nhớ tạm! 📋", Toast.LENGTH_SHORT).show()
                                        showShareDialog = false
                                    }
                                ) {
                                    Text("Sao chép", fontSize = 12.sp)
                                }

                                Button(
                                    modifier = Modifier.weight(1.2f),
                                    onClick = {
                                        val textBuilder = java.lang.StringBuilder()
                                        textBuilder.append("🌌 CHIA SẺ MÀN HÌNH SPACEZ AI CHAT TRONG NGÀY 🌌\n")
                                        textBuilder.append("Lịch sử hội thoại trò chuyện cùng ${selectedBot.name}\n")
                                        textBuilder.append("--------------------------------------------------\n\n")
                                        messages.forEach { msg ->
                                            val authorName = if (msg.role == "user") "Bạn" else selectedBot.name
                                            textBuilder.append("${if (msg.role == "user") "👤 Bạn" else "${selectedBot.emoji} ${selectedBot.name}"}: ${msg.content}\n\n")
                                        }
                                        textBuilder.append("--------------------------------------------------\n")
                                        textBuilder.append("Được xuất bản nhanh từ SpaceZ client.")

                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Nhật ký chia sẻ hội thoại SpaceZ - ${selectedBot.name}")
                                            putExtra(android.content.Intent.EXTRA_TEXT, textBuilder.toString())
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Chia sẻ cuộc họp hội thoại qua..."))
                                        showShareDialog = false
                                    }
                                ) {
                                    Text("Chia sẻ ngay", fontSize = 12.sp)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showShareDialog = false }) {
                                Text("Đóng", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        if (showAddBotDialog) {
            var botName by remember { mutableStateOf("") }
            var botDesc by remember { mutableStateOf("") }
            var botGreet by remember { mutableStateOf("") }
            var botSystem by remember { mutableStateOf("") }
            var botEmoji by remember { mutableStateOf("🤖") }

            Dialog(onDismissRequest = { showAddBotDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🤖 Tự tạo Chat Bot cá nhân",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        OutlinedTextField(
                            value = botEmoji,
                            onValueChange = { botEmoji = it },
                            label = { Text("Biểu tượng (Emoji)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = botName,
                            onValueChange = { botName = it },
                            label = { Text("Tên Bot") },
                            placeholder = { Text("Ví dụ: Trợ lý Sức Khỏe") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = botDesc,
                            onValueChange = { botDesc = it },
                            label = { Text("Mô tả ngắn") },
                            placeholder = { Text("Ví dụ: Hướng dẫn ăn uống") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = botGreet,
                            onValueChange = { botGreet = it },
                            label = { Text("Lời chào đầu tiên") },
                            placeholder = { Text("Chào bạn! Có gì cần mình giúp ý kiến sức khoẻ hôm nay? 😊") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3
                        )

                        OutlinedTextField(
                            value = botSystem,
                            onValueChange = { botSystem = it },
                            label = { Text("Luật hệ thống (Instructions)") },
                            placeholder = { Text("Đóng vai là bác sĩ khuyên ăn rau xanh...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 4
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showAddBotDialog = false }) {
                                Text("Hủy")
                            }
                            Button(
                                onClick = {
                                    if (botName.trim().isNotEmpty() && botSystem.trim().isNotEmpty()) {
                                        onAddCustomBot(
                                            botName.trim(),
                                            botDesc.trim().ifEmpty { "Bot tự tạo" },
                                            botSystem.trim(),
                                            botGreet.trim().ifEmpty { "Xin chào! Mình sẵn sàng phục vụ rồi. 😊" },
                                            botEmoji.trim().ifEmpty { "🤖" }
                                        )
                                        showAddBotDialog = false
                                    } else {
                                        Toast.makeText(context, "Vui lòng nhập Tên và Luật hệ thống!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Tạo Bot")
                            }
                        }
                    }
                }
            }
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.widthIn(max = 400.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Chào mừng tới SpaceZ AI!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tôi có thể hỗ trợ trò chuyện thông minh hoặc chuyển các ý tưởng của bạn thành kiệt tác hội họa nghệ thuật.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.typography.bodyMedium.color.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Card(
                        onClick = onConfigApiKey,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Cấu hình nhanh API Key",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Nhấp vào đây để đổi mã token SpaceZ của bạn nếu bị lỗi.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Start
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Vài gợi ý tuyệt vời:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    val suggestions = if (isDrawingMode) {
                        listOf(
                            "Mèo phi hành gia câu cá trên Mặt Trăng mộng mơ",
                            "Thành phố thu nhỏ nằm trọn trong chiếc bóng đèn thủy tinh",
                            "Cảnh hoàng hôn rực rỡ phản chiếu cổng không gian cổ kính"
                        )
                    } else {
                        listOf(
                            "Viết một bài thơ ngắn lãng mạn về bầu trời đêm mùa hạ",
                            "Làm thế nào để học lập trình Android đơn giản nhất?",
                            "Cách nấu món Phở bò chuẩn vị truyền thống Hà Nội"
                        )
                    }

                    suggestions.forEach { suggestion ->
                        Card(
                            onClick = {
                                inputText = suggestion
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = suggestion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        apiKey = apiKey,
                        baseUrl = baseUrl,
                        onDelete = { onDeleteMessage(msg.id) },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(msg.content))
                            Toast.makeText(context, "Đã sao chép nội dung", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (isLoading) {
                    item {
                        AssistantTypingIndicator()
                    }
                }
            }
        }

        // Quick Suggestions switch above inputs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = when {
                        isDrawingMode -> Icons.Default.Palette
                        isReasoningMode -> Icons.Default.Psychology
                        else -> Icons.Default.Chat
                    },
                    contentDescription = null,
                    tint = when {
                        isDrawingMode -> MaterialTheme.colorScheme.tertiary
                        isReasoningMode -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = when {
                        isDrawingMode -> "Chế độ: Vẽ tranh nghệ thuật 🎨"
                        isReasoningMode -> "Chế độ: Suy luận chuyên sâu 🧠"
                        else -> "Chế độ: Trò chuyện Trí tuệ 💬"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isDrawingMode -> MaterialTheme.colorScheme.tertiary
                        isReasoningMode -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            // High-fidelity integrated Segmented Toggle for 3 Modes
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(2.dp)
            ) {
                val isChatSelected = !isDrawingMode && !isReasoningMode
                val isReasoningSelected = !isDrawingMode && isReasoningMode
                val isDrawingSelected = isDrawingMode

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isChatSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { 
                            onToggleDrawingMode(false)
                            onToggleReasoningMode(false)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Chat",
                        color = if (isChatSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isReasoningSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                        .clickable { 
                            onToggleDrawingMode(false)
                            onToggleReasoningMode(true)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Suy luận",
                        color = if (isReasoningSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDrawingSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent)
                        .clickable { 
                            onToggleDrawingMode(true)
                            onToggleReasoningMode(false)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Vẽ ảnh",
                        color = if (isDrawingSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom input panel
        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Image preview if attached
                if (attachedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp, bottom = 8.dp)
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(attachedImageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Attached Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                attachedImageUri = null
                                attachedImageB64 = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Photo attachment button (only in Chat / Reasoning mode)
                    if (!isDrawingMode) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Thêm ảnh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isDrawingMode) "Mô tả bức ảnh bạn muốn tạo..." else "Gửi thắc mắc của bạn...",
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDrawingMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if ((inputText.isNotBlank() || attachedImageB64 != null) && !isLoading) {
                                    onSendMessage(inputText, attachedImageB64)
                                    inputText = ""
                                    attachedImageUri = null
                                    attachedImageB64 = null
                                }
                            }
                        )
                    )

                    val buttonColor = if (isDrawingMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    val contentColor = if (isDrawingMode) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
                    val canSend = (inputText.isNotBlank() || attachedImageB64 != null) && !isLoading

                    IconButton(
                        onClick = {
                            if (canSend) {
                                onSendMessage(inputText, attachedImageB64)
                                inputText = ""
                                attachedImageUri = null
                                attachedImageB64 = null
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (!canSend) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    buttonColor
                                }
                            )
                            .testTag("send_button"),
                        enabled = canSend
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Gửi",
                            tint = if (!canSend) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            } else {
                                contentColor
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

fun extractThinkingAndContent(fullText: String): Pair<String?, String> {
    if (fullText.contains("<think>") && fullText.contains("</think>")) {
        val thinking = fullText.substringAfter("<think>").substringBefore("</think>").trim()
        val content = fullText.substringAfter("</think>").trim()
        return Pair(thinking, content)
    }
    if (fullText.contains("<think>")) {
        val thinking = fullText.substringAfter("<think>").trim()
        return Pair(thinking, "")
    }
    return Pair(null, fullText)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: MessageEntity,
    apiKey: String,
    baseUrl: String,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start

    var displayedText by remember(message.content) { mutableStateOf("") }
    val isNewMessage = remember(message.timestamp) {
        System.currentTimeMillis() - message.timestamp < 10000
    }

    if (message.role == "assistant" && isNewMessage && !message.isImage) {
        LaunchedEffect(message.content) {
            val fullString = message.content
            for (i in 0..fullString.length) {
                displayedText = fullString.take(i)
                kotlinx.coroutines.delay(6)
            }
        }
    } else {
        displayedText = message.content
    }

    var thinkingExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SZ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            var expandedMenu by remember { mutableStateOf(false) }

            Box {
                Card(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isUser -> MaterialTheme.colorScheme.primaryContainer
                            message.content.startsWith("⚠️ Lỗi") -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { /* Tap message */ },
                            onLongClick = { expandedMenu = true }
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // User inline uploads previewing
                        if (!message.isImage && message.imageUrl != null) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                val request = rememberAuthenticatedImageRequest(message.imageUrl, apiKey, baseUrl)
                                SubcomposeAsyncImage(
                                    model = request,
                                    contentDescription = "Hình ảnh đính kèm",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        if (message.isImage) {
                            // High fidelity art image render
                            Text(
                                text = "🎨 Ý tưởng: ${message.content}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            message.imageUrl?.let { imgUrl ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val request = rememberAuthenticatedImageRequest(imgUrl, apiKey, baseUrl)
                                    SubcomposeAsyncImage(
                                        model = request,
                                        contentDescription = message.content,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        loading = {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        },
                                        error = {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.BrokenImage,
                                                    contentDescription = "Lỗi tải ảnh",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Text(
                                                    text = "Lỗi kết nối ảnh",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.error,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            val (thinking, mainContent) = extractThinkingAndContent(displayedText)

                            Column {
                                if (thinking != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .clickable { thinkingExpanded = !thinkingExpanded }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Psychology,
                                                    contentDescription = "Logic suy ngẫm",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Lập luận suy nghĩ...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    imageVector = if (thinkingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            if (thinkingExpanded) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = thinking,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontStyle = FontStyle.Italic,
                                                        lineHeight = 16.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (mainContent.isNotEmpty() || thinking == null) {
                                    Text(
                                        text = mainContent.ifEmpty { "Đang suy luận lập luận..." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            isUser -> MaterialTheme.colorScheme.onPrimaryContainer
                                            message.content.startsWith("⚠️ Lỗi") -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sao chép") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            onCopy()
                            expandedMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Xoá tin") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDelete()
                            expandedMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AssistantTypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SZ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.widthIn(max = 240.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "SpaceZ AI đang xử lý...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun GalleryTabContent(
    images: List<MessageEntity>,
    apiKey: String,
    baseUrl: String,
    onImageClick: (MessageEntity) -> Unit
) {
    if (images.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Thư viện trống trơn!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Các tác phẩm mỹ thuật bạn tạo ra sẽ xuất hiện tại đây. Nhấp vào Vẽ ảnh ở Trò chuyện để làm sống dậy ý tưởng của bạn!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.typography.bodySmall.color.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(images, key = { it.id }) { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { onImageClick(msg) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val request = rememberAuthenticatedImageRequest(msg.imageUrl, apiKey, baseUrl)
                        SubcomposeAsyncImage(
                            model = request,
                            contentDescription = msg.content,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        )

                        // Subtle transparent shade overlay showing prompt title
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = msg.content,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    apiKey: String,
    baseUrl: String,
    imageSize: String,
    onSaveApiKey: (String) -> Unit,
    onResetApiKey: () -> Unit,
    onSaveBaseUrl: (String) -> Unit,
    onResetBaseUrl: () -> Unit,
    onSaveSize: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var urlInput by remember(baseUrl) { mutableStateOf(baseUrl) }
    var curlInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Thiết lập máy chủ & Dịch vụ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Vui lòng cấu hình khóa nhận dạng (Bearer) và URL gốc của máy chủ để gửi yêu cầu API.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Mã token SpaceZ Bearer") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_api_key_field"),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Ẩn/Hiện" else "Hiện"
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Base API URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (keyInput.trim().isNotEmpty() && urlInput.trim().isNotEmpty()) {
                                    onSaveApiKey(keyInput.trim())
                                    onSaveBaseUrl(urlInput.trim())
                                    Toast.makeText(context, "Đã lưu thiết đặt thành công!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Các trường dữ liệu không được để trống!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("save_api_key_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Lưu thiết lập", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                onResetApiKey()
                                onResetBaseUrl()
                                keyInput = apiKey
                                urlInput = baseUrl
                                Toast.makeText(context, "Đã khôi phục mặc định", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Khôi phục", fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Cấu hình tự động bằng lệnh CURL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Bạn có thể dán trực tiếp câu lệnh 'curl -X POST ...' chứa mã token và URL endpoint máy chủ của bạn vào đây. Hệ thống sẽ tự phân tích và điền các tham số cài đặt cho bạn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = curlInput,
                        onValueChange = { curlInput = it },
                        label = { Text("Dán câu lệnh curl mẫu") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )

                    Button(
                        onClick = {
                            val trimmedCurl = curlInput.trim()
                            if (trimmedCurl.lowercase().startsWith("curl")) {
                                val bearerRegex = """Authorization:\s*Bearer\s+([^\s"'\\]+)""".toRegex(RegexOption.IGNORE_CASE)
                                val bearerMatch = bearerRegex.find(trimmedCurl)
                                val token = bearerMatch?.groupValues?.get(1)

                                val urlRegex = """https?://[^\s"'\\]+""".toRegex()
                                val urlMatch = urlRegex.find(trimmedCurl)
                                val fullUrl = urlMatch?.value

                                var gotAnything = false
                                if (token != null && token.isNotEmpty() && token != "YOUR_API_KEY") {
                                    keyInput = token
                                    onSaveApiKey(token)
                                    gotAnything = true
                                }
                                if (fullUrl != null && fullUrl.isNotEmpty()) {
                                    try {
                                        val parsedUrl = fullUrl.toHttpUrlOrNull()
                                        if (parsedUrl != null) {
                                            val baseUrlRebuilt = "${parsedUrl.scheme}://${parsedUrl.host}${if (parsedUrl.port != 80 && parsedUrl.port != 443) ":${parsedUrl.port}" else ""}/"
                                            urlInput = baseUrlRebuilt
                                            onSaveBaseUrl(baseUrlRebuilt)
                                            gotAnything = true
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                if (gotAnything) {
                                    Toast.makeText(context, "Đã áp dụng và lưu cấu hình từ CURL thành công!", Toast.LENGTH_LONG).show()
                                    curlInput = ""
                                } else {
                                    Toast.makeText(context, "Không thể phân tách mã hoặc URL từ CURL này. Vui lòng kiểm tra lại.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Đây không phải là một câu lệnh CURL hợp lệ (cần bắt đầu bằng 'curl').", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Phân tách & Áp dụng CURL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Thông số Vẽ ảnh (DALL-E)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Chọn độ phân giải hình ảnh xuất ra từ AI generator:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    val sizes = listOf("512x512", "1024x1024")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizes.forEach { size ->
                            val isSelected = imageSize == size
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { onSaveSize(size) },
                                label = { Text(size, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Bộ nhớ & Tiện ích dọn dẹp",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Điều này sẽ giải phóng tất cả các bản ghi tin nhắn và tệp cache ảnh để giảm tải cơ sở dữ liệu địa phương.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Button(
                        onClick = {
                            onClearHistory()
                            Toast.makeText(context, "Đã xóa toàn bộ dữ liệu lịch sử", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Xóa toàn bộ lịch sử", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Hệ thống liên kết SpaceZ",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Toàn bộ API được kết nối qua đường truyền mã hóa an toàn sử dụng mô hình cloud-ai cao cấp.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageDetailsDialog(
    message: MessageEntity,
    apiKey: String,
    baseUrl: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onCopyPrompt: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Chi tiết tác phẩm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val request = rememberAuthenticatedImageRequest(message.imageUrl, apiKey, baseUrl)
                    SubcomposeAsyncImage(
                        model = request,
                        contentDescription = message.content,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                        }
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Ý tưởng vẽ (Prompt)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopyPrompt,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Prompt")
                    }

                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Xóa ảnh")
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Đóng")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickApiKeyDialog(
    currentApiKey: String,
    currentBaseUrl: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onReset: () -> Unit,
    onParseCurl: (String) -> Boolean
) {
    var keyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var urlInput by remember(currentBaseUrl) { mutableStateOf(currentBaseUrl) }
    var curlInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Thiết lập API & CURL Nhanh",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Dán mã khóa Authorization Space-Z hoặc dán toàn bộ câu lệnh CURL dưới đây. Hệ thống sẽ tự động bóc tách thông tin và tự điền trường dữ liệu thích hợp.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Curl Input parser field
                OutlinedTextField(
                    value = curlInput,
                    onValueChange = { 
                        curlInput = it
                        val trimmed = it.trim()
                        if (trimmed.lowercase().startsWith("curl")) {
                            val bearerRegex = """Authorization:\s*Bearer\s+([^\s"'\\]+)""".toRegex(RegexOption.IGNORE_CASE)
                            val bearerMatch = bearerRegex.find(trimmed)
                            val token = bearerMatch?.groupValues?.get(1)

                            val urlRegex = """https?://[^\s"'\\]+""".toRegex()
                            val urlMatch = urlRegex.find(trimmed)
                            val fullUrl = urlMatch?.value

                            if (token != null && token.isNotEmpty() && token != "YOUR_API_KEY") {
                                keyInput = token
                            }
                            if (fullUrl != null && fullUrl.isNotEmpty()) {
                                try {
                                    val parsedUrl = fullUrl.toHttpUrlOrNull()
                                    if (parsedUrl != null) {
                                        val baseUrlRebuilt = "${parsedUrl.scheme}://${parsedUrl.host}${if (parsedUrl.port != 80 && parsedUrl.port != 443) ":${parsedUrl.port}" else ""}/"
                                        urlInput = baseUrlRebuilt
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    },
                    label = { Text("Dán câu lệnh curl (Tùy chọn)") },
                    placeholder = { Text("curl -X POST https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    singleLine = false
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Mã token SpaceZ Bearer") },
                    modifier = Modifier.fillMaxWidth().testTag("settings_api_key_field_quick"),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Base API URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onReset()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mặc định", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (keyInput.trim().isNotEmpty() && urlInput.trim().isNotEmpty()) {
                                onSave(keyInput.trim(), urlInput.trim())
                            }
                        },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Lưu & Áp Dụng", fontSize = 12.sp)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Đóng")
                }
            }
        }
    }
}
