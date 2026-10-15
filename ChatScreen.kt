package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.GeminiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiInlineData
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiPrebuiltVoiceConfig
import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiSpeechConfig
import com.example.data.api.GeminiThinkingConfig
import com.example.data.api.GeminiTool
import com.example.data.api.GeminiVoiceConfig
import com.example.data.model.Message
import com.example.data.model.Session
import com.example.data.repository.MickRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    repository: MickRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val sessionsFlow = remember { repository.observeSessions() }
    val sessions by sessionsFlow.collectAsState(initial = emptyList())

    var activeSession by remember { mutableStateOf<Session?>(null) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }

    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    // Modes
    var isHighThinking by remember { mutableStateOf(false) }
    var isLowLatency by remember { mutableStateOf(false) }
    var isSearchGrounded by remember { mutableStateOf(false) }
    var isMapsGrounded by remember { mutableStateOf(false) }

    // Attachment
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var attachedBase64 by remember { mutableStateOf<String?>(null) }

    var showNewSessionDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    // Auto-select or create first session
    LaunchedEffect(sessions) {
        if (activeSession == null && sessions.isNotEmpty()) {
            activeSession = sessions.first()
        } else if (activeSession == null && sessions.isEmpty()) {
            // Automatically bootstrap the very first session
            scope.launch {
                val newSess = repository.createSession("Creative Brainstorming", "general")
                activeSession = newSess
                // Seed initial message as mandated: "Hi, I'm Mick AI. What are we creating today?"
                repository.addMessage(
                    sessionId = newSess.id,
                    role = "model",
                    text = "Hi, I'm Mick AI. What are we creating today?"
                )
            }
        }
    }

    // Observe messages for current active session
    LaunchedEffect(activeSession?.id) {
        val currentSessId = activeSession?.id
        if (currentSessId != null) {
            repository.observeMessages(currentSessId).collect { msgs ->
                if (msgs.isEmpty()) {
                    // Seed initial message if new empty session
                    repository.addMessage(
                        sessionId = currentSessId,
                        role = "model",
                        text = "Hi, I'm Mick AI. What are we creating today?"
                    )
                } else {
                    messages = msgs
                }
            }
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(context.contentResolver, uri)
                            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                decoder.setTargetSize(512, 512)
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val raw = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            Bitmap.createScaledBitmap(raw, 512, 512, true)
                        }
                    }
                    attachedBitmap = bitmap
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    attachedBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Creative direction starter chips
    val creativeSparks = listOf(
        "⚡ 3 Plot Twists",
        "🌿 Biomimicry Spark",
        "🎵 Song Concept",
        "✨ Worldbuilding Hook",
        "🖋️ Poetic Metaphors",
        "💡 Brand Taglines",
        "🌌 Sci-Fi Tech"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Brainstorm Sessions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { showNewSessionDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New Session")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(sessions, key = { it.id }) { sess ->
                            val isSelected = activeSession?.id == sess.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        activeSession = sess
                                        scope.launch { drawerState.close() }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sess.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = sess.category.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repository.deleteSession(sess.id)
                                                if (activeSession?.id == sess.id) {
                                                    activeSession = null
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Session",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = activeSession?.title ?: "Mick AI Studio",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = "Mick AI • Creative Partner",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("open_sessions_drawer_button")
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "Sessions", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            },
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Toggles row (High Thinking, Low Latency, Search Grounded, Maps Grounded)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = isHighThinking,
                            onClick = {
                                isHighThinking = !isHighThinking
                                if (isHighThinking) isLowLatency = false
                            },
                            label = { Text("High Thinking") },
                            leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null, Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("chat_toggle_high_thinking")
                        )
                    }
                    item {
                        FilterChip(
                            selected = isLowLatency,
                            onClick = {
                                isLowLatency = !isLowLatency
                                if (isLowLatency) isHighThinking = false
                            },
                            label = { Text("Low Latency") },
                            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.testTag("chat_toggle_low_latency")
                        )
                    }
                    item {
                        FilterChip(
                            selected = isSearchGrounded,
                            onClick = { isSearchGrounded = !isSearchGrounded },
                            label = { Text("Search Grounded") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, Modifier.size(16.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = isMapsGrounded,
                            onClick = { isMapsGrounded = !isMapsGrounded },
                            label = { Text("Maps Grounded") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, Modifier.size(16.dp)) }
                        )
                    }
                }

                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id.ifBlank { it.text.hashCode().toString() } }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            onSaveSpark = {
                                scope.launch {
                                    repository.saveSpark(
                                        title = "Spark: ${msg.text.take(30)}",
                                        content = msg.text,
                                        category = "prompt"
                                    )
                                    Toast.makeText(context, "Saved to Sparks Notebook!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShareToCommunity = {
                                scope.launch {
                                    repository.createCommunityPost(
                                        title = "Mick AI Creation: ${msg.text.take(30)}",
                                        content = msg.text,
                                        category = "story"
                                    )
                                    Toast.makeText(context, "Published to Community Lounge!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onReadAloud = {
                                scope.launch {
                                    try {
                                        val apiKey = GeminiClient.getApiKey()
                                        val req = GeminiRequest(
                                            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = msg.text)))),
                                            generationConfig = GeminiGenerationConfig(
                                                responseModalities = listOf("AUDIO"),
                                                speechConfig = GeminiSpeechConfig(
                                                    voiceConfig = GeminiVoiceConfig(
                                                        prebuiltVoiceConfig = GeminiPrebuiltVoiceConfig(voiceName = "Kore")
                                                    )
                                                )
                                            )
                                        )
                                        val resp = GeminiClient.service.generateContent("gemini-3.8-flash-tts", apiKey, req)
                                        val audioData = resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData?.data
                                        if (audioData != null) {
                                            withContext(Dispatchers.IO) {
                                                val bytes = Base64.decode(audioData, Base64.DEFAULT)
                                                val tempFile = File.createTempFile("mick_speech", ".mp3", context.cacheDir)
                                                FileOutputStream(tempFile).use { it.write(bytes) }
                                                withContext(Dispatchers.Main) {
                                                    mediaPlayer?.release()
                                                    mediaPlayer = MediaPlayer().apply {
                                                        setDataSource(tempFile.absolutePath)
                                                        prepare()
                                                        start()
                                                    }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "TTS synthesized message audio", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Audio error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }

                    if (isGenerating) {
                        item {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isHighThinking) "Mick AI is deeply contemplating creative possibilities..." else "Mick AI is crafting creative directions...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Creative Spark chips carousel
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(creativeSparks) { chipLabel ->
                        AssistChip(
                            onClick = {
                                inputText = when (chipLabel) {
                                    "⚡ 3 Plot Twists" -> "Give me 3 unexpected, original plot twists for a story where "
                                    "🌿 Biomimicry Spark" -> "Explore a biomimicry design inspired by "
                                    "🎵 Song Concept" -> "Brainstorm lyrics and chords for a song about "
                                    "✨ Worldbuilding Hook" -> "Design a unique worldbuilding system where "
                                    "🖋️ Poetic Metaphors" -> "Write vivid metaphors and poetic imagery describing "
                                    "💡 Brand Taglines" -> "Generate 5 bold, inspiring brand taglines for "
                                    "🌌 Sci-Fi Tech" -> "Design speculative technology that harnesses "
                                    else -> chipLabel
                                }
                            },
                            label = { Text(chipLabel, style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                // Attachment Preview if present
                attachedBitmap?.let { bmp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Attached Image",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Image attached for vision analysis", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            attachedBitmap = null
                            attachedBase64 = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }

                // Input bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("chat_attach_image_button")
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach Image", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Brainstorm with Mick AI...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if ((inputText.isNotBlank() || attachedBase64 != null) && !isGenerating) {
                                    val userText = inputText
                                    val currentSessId = activeSession?.id ?: return@IconButton
                                    val imagePayload = attachedBase64
                                    inputText = ""
                                    attachedBitmap = null
                                    attachedBase64 = null
                                    isGenerating = true

                                    scope.launch {
                                        // Save user message to Firestore
                                        repository.addMessage(
                                            sessionId = currentSessId,
                                            role = "user",
                                            text = userText,
                                            multimodalType = if (imagePayload != null) "image" else "none"
                                        )

                                        // Call Gemini
                                        try {
                                            val apiKey = GeminiClient.getApiKey()
                                            val partsList = mutableListOf<GeminiPart>()
                                            partsList.add(GeminiPart(text = userText))
                                            if (imagePayload != null) {
                                                partsList.add(GeminiPart(inlineData = GeminiInlineData("image/jpeg", imagePayload)))
                                            }

                                            // Determine model
                                            val selectedModel = when {
                                                imagePayload != null -> "gemini-4-argon"
                                                isHighThinking -> "gemini-4-argon"
                                                isLowLatency -> "gemini-3.1-flash-lite"
                                                else -> "gemini-3.8-flash"
                                            }

                                            // Generation config
                                            val genConfig = when {
                                                isHighThinking -> GeminiGenerationConfig(
                                                    thinkingConfig = GeminiThinkingConfig(thinkingLevel = "HIGH")
                                                )
                                                else -> GeminiGenerationConfig(temperature = 0.8f)
                                            }

                                            // Tools
                                            val toolsList = when {
                                                isSearchGrounded -> listOf(GeminiTool(googleSearch = emptyMap()))
                                                isMapsGrounded -> listOf(GeminiTool(googleMaps = emptyMap()))
                                                else -> null
                                            }

                                            val systemInstruction = GeminiContent(
                                                parts = listOf(
                                                    GeminiPart(
                                                        text = """
You are Mick AI, a creative and inspiring AI assistant. Your purpose is to spark ideas, refine creative work, and help users think originally.

Identity:
- Name: Mick AI
- Role: creative partner, brainstorming companion, idea accelerator
- Tone: warm, imaginative, encouraging, clear, slightly poetic but not overdone
- Style: concise when asked, expansive when brainstorming; ask clarifying questions when needed

Core behaviors:
1. Listen for the user's goal, audience, format, constraints.
2. Offer multiple directions (at least 3) before narrowing down.
3. Build on user's ideas; never dismiss them.
4. Use vivid language, metaphors, examples, and structured prompts.
5. When writing, match requested voice, genre, length, and reading level.
6. When stuck, ask one focused question.
7. Never claim to be human. Never claim to be the user.
8. If asked for factual/medical/legal/financial advice, give general info and recommend professional help.
9. Protect privacy.
10. Keep a consistent persona: Muse AI.

Response format:
- Start with a short, energizing line.
- Then provide ideas, options, or draft (at least 3 distinct directions).
- End with one small next-step suggestion.
                                                        """.trimIndent()
                                                    )
                                                )
                                            )

                                            val request = GeminiRequest(
                                                contents = listOf(GeminiContent(role = "user", parts = partsList)),
                                                systemInstruction = systemInstruction,
                                                generationConfig = genConfig,
                                                tools = toolsList
                                            )

                                            val response = GeminiClient.service.generateContent(selectedModel, apiKey, request)
                                            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                                ?: "Every spark begins in darkness. Let's illuminate this from another angle!"

                                            // Save model response to Firestore
                                            repository.addMessage(
                                                sessionId = currentSessId,
                                                role = "model",
                                                text = replyText,
                                                multimodalType = if (isHighThinking) "high_thinking" else "none"
                                            )
                                        } catch (e: Exception) {
                                            repository.addMessage(
                                                sessionId = currentSessId,
                                                role = "model",
                                                text = "A ripple in the ether paused our spark: ${e.message}. What angle shall we explore next?"
                                            )
                                        } finally {
                                            isGenerating = false
                                        }
                                    }
                                }
                            },
                            enabled = (inputText.isNotBlank() || attachedBase64 != null) && !isGenerating,
                            modifier = Modifier.testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank() || attachedBase64 != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewSessionDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf("general") }
        AlertDialog(
            onDismissRequest = { showNewSessionDialog = false },
            title = { Text("New Brainstorming Canvas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Session Name") },
                        placeholder = { Text("e.g. Cyberpunk Novel Outlining") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val newSess = repository.createSession(newTitle, newCategory)
                            activeSession = newSess
                            repository.addMessage(
                                sessionId = newSess.id,
                                role = "model",
                                text = "Hi, I'm Mick AI. What are we creating today?"
                            )
                            showNewSessionDialog = false
                            drawerState.close()
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showNewSessionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: Message,
    onSaveSpark: () -> Unit,
    onShareToCommunity: () -> Unit,
    onReadAloud: () -> Unit
) {
    val isUser = message.role == "user"
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Mick AI",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Action row for model messages
            if (!isUser) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onReadAloud,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Listen Aloud", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onSaveSpark,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Save to Sparks", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onShareToCommunity,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share to Community", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Mick AI Response", message.text))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
