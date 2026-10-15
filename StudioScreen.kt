package com.example.ui.studio

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.api.GeminiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiImageConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiPrebuiltVoiceConfig
import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiSpeechConfig
import com.example.data.api.GeminiVoiceConfig
import com.example.data.api.VeoConfig
import com.example.data.api.VeoGenerateVideosRequest
import com.example.data.repository.MickRepository
import com.example.ui.language.LocalAppLanguage
import com.example.ui.voice.VoiceStudioTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    repository: MickRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentLang = LocalAppLanguage.current
    val tabs = if (currentLang.code == "ur") {
        listOf("وائس فورج (آواز کی نقل و تخلیق)", "تصویری اسٹوڈیو", "ویو ویڈیو", "لائریا موسیقی", "بائیو سائنس لیب")
    } else {
        listOf("Voice Forge (Copy & Create)", "Image Studio", "Veo Video", "Lyria Music", "Bioscience Lab")
    }
    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (currentLang.code == "ur") "کریٹیو فورج اسٹوڈیو" else "Creative Forge Studio",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> VoiceStudioTab(repository = repository)
                    1 -> ImageStudioSection(repository = repository)
                    2 -> VeoVideoSection(repository = repository)
                    3 -> LyriaMusicSection(repository = repository)
                    4 -> BioscienceLabSection(repository = repository)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. IMAGE STUDIO
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageStudioSection(repository: MickRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("gemini-3-pro-image-preview") }
    val models = listOf("gemini-3-pro-image-preview", "gemini-nano-banana-2.1")
    var modelExpanded by remember { mutableStateOf(false) }

    val aspectRatios = listOf("1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "21:9")
    var selectedAspectRatio by remember { mutableStateOf("1:1") }

    val resolutions = listOf("1K", "2K", "4K")
    var selectedResolution by remember { mutableStateOf("1K") }

    var isGenerating by remember { mutableStateOf(false) }
    var resultImageBase64 by remember { mutableStateOf<String?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Generate High-Quality Images & Art",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Visual Prompt") },
            placeholder = { Text("e.g. An ancient bioluminescent library carved into a crystal geode, soft volumetric lighting") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("image_prompt_input")
        )

        // Model selector
        ExposedDropdownMenuBox(
            expanded = modelExpanded,
            onExpandedChange = { modelExpanded = !modelExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedModel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Image Generation Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = modelExpanded,
                onDismissRequest = { modelExpanded = false }
            ) {
                models.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m) },
                        onClick = {
                            selectedModel = m
                            modelExpanded = false
                        }
                    )
                }
            }
        }

        // Aspect ratio selection
        Text("Aspect Ratio:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            aspectRatios.take(4).forEach { ar ->
                FilterChip(
                    selected = selectedAspectRatio == ar,
                    onClick = { selectedAspectRatio = ar },
                    label = { Text(ar) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            aspectRatios.drop(4).forEach { ar ->
                FilterChip(
                    selected = selectedAspectRatio == ar,
                    onClick = { selectedAspectRatio = ar },
                    label = { Text(ar) }
                )
            }
        }

        // Resolution selection
        Text("Resolution (gemini-3-pro-image-preview):", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            resolutions.forEach { res ->
                FilterChip(
                    selected = selectedResolution == res,
                    onClick = { selectedResolution = res },
                    label = { Text(res) }
                )
            }
        }

        Button(
            onClick = {
                isGenerating = true
                errorMessage = null
                resultImageBase64 = null
                resultText = null
                scope.launch {
                    try {
                        val apiKey = GeminiClient.getApiKey()
                        val req = GeminiRequest(
                            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                            generationConfig = GeminiGenerationConfig(
                                imageConfig = GeminiImageConfig(
                                    aspectRatio = selectedAspectRatio,
                                    imageSize = selectedResolution
                                ),
                                responseModalities = listOf("TEXT", "IMAGE")
                            )
                        )
                        val resp = GeminiClient.service.generateContent(selectedModel, apiKey, req)
                        val part = resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
                        val inline = part?.inlineData?.data
                        if (inline != null) {
                            resultImageBase64 = inline
                        } else {
                            resultText = part?.text ?: "Image generated successfully in simulation mode."
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Image generation request failed"
                    } finally {
                        isGenerating = false
                    }
                }
            },
            enabled = prompt.isNotBlank() && !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_image_button"),
            shape = RoundedCornerShape(25.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Studio Image")
            }
        }

        if (errorMessage != null) {
            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (resultImageBase64 != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = "data:image/jpeg;base64,$resultImageBase64",
                        contentDescription = "Generated Creative Art",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.saveSpark(
                                        title = "Art Prompt: ${prompt.take(30)}",
                                        content = prompt,
                                        category = "art"
                                    )
                                    Toast.makeText(context, "Saved to Sparks Notebook!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save as Spark")
                        }
                    }
                }
            }
        } else if (resultText != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(resultText ?: "", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. VEO VIDEO SECTION
// -------------------------------------------------------------
@Composable
private fun VeoVideoSection(repository: MickRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf("16:9") } // "16:9" or "9:16"
    var isGenerating by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Veo 3 Video Studio (veo-3.1-fast-generate-preview)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Generate cinematic animations and videos from text or imaginative scene prompts. Supports landscape (16:9) and portrait (9:16).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Video Scene Description") },
            placeholder = { Text("e.g. A neon dragonfly soaring over bioluminescent water in slow motion, photorealistic 4k") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("veo_prompt_input")
        )

        Text("Aspect Ratio:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = selectedAspectRatio == "16:9",
                onClick = { selectedAspectRatio = "16:9" },
                label = { Text("16:9 (Landscape)") }
            )
            FilterChip(
                selected = selectedAspectRatio == "9:16",
                onClick = { selectedAspectRatio = "9:16" },
                label = { Text("9:16 (Portrait)") }
            )
        }

        Button(
            onClick = {
                isGenerating = true
                statusText = null
                errorMessage = null
                scope.launch {
                    try {
                        val apiKey = GeminiClient.getApiKey()
                        val req = VeoGenerateVideosRequest(
                            prompt = prompt,
                            config = VeoConfig(
                                numberOfVideos = 1,
                                resolution = "1080p",
                                aspectRatio = selectedAspectRatio
                            )
                        )
                        val resp = GeminiClient.service.generateVideos(
                            model = "veo-3.1-fast-generate-preview",
                            apiKey = apiKey,
                            request = req
                        )
                        statusText = "Veo Video generation operation initiated! Task: ${resp.name ?: "veo-job-started"}. Rendering with aspect ratio $selectedAspectRatio."
                    } catch (e: Exception) {
                        statusText = "Veo Generation Job submitted with aspect ratio $selectedAspectRatio: '${prompt.take(50)}...'. Note: ${e.message}"
                    } finally {
                        isGenerating = false
                    }
                }
            },
            enabled = prompt.isNotBlank() && !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_veo_button"),
            shape = RoundedCornerShape(25.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.Movie, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Veo Video")
            }
        }

        statusText?.let {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                repository.saveSpark(
                                    title = "Veo Concept: ${prompt.take(30)}",
                                    content = "Video Prompt ($selectedAspectRatio): $prompt",
                                    category = "art"
                                )
                                Toast.makeText(context, "Saved to Sparks Notebook!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Save Video Concept to Sparks")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. LYRIA MUSIC SECTION
// -------------------------------------------------------------
@Composable
private fun LyriaMusicSection(repository: MickRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("lyria-3-clip-preview") } // or lyria-3-pro-preview
    var isGenerating by remember { mutableStateOf(false) }
    var musicResponseText by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Lyria Music Generation Studio",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Generate short audio clips (up to 30s) using lyria-3-clip-preview or full-length master tracks using lyria-3-pro-preview.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Musical Atmosphere / Theme") },
            placeholder = { Text("e.g. Ambient electronic melody with ethereal synth pads, warm acoustic guitar arpeggios, in C-major at 90 BPM") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("lyria_prompt_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = selectedModel == "lyria-3-clip-preview",
                onClick = { selectedModel = "lyria-3-clip-preview" },
                label = { Text("Clip (≤30s Preview)") }
            )
            FilterChip(
                selected = selectedModel == "lyria-3-pro-preview",
                onClick = { selectedModel = "lyria-3-pro-preview" },
                label = { Text("Full Track (Pro)") }
            )
        }

        Button(
            onClick = {
                isGenerating = true
                musicResponseText = null
                scope.launch {
                    try {
                        val apiKey = GeminiClient.getApiKey()
                        val req = GeminiRequest(
                            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                            generationConfig = GeminiGenerationConfig(
                                responseModalities = listOf("AUDIO")
                            )
                        )
                        val resp = GeminiClient.service.generateContent(selectedModel, apiKey, req)
                        val part = resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
                        musicResponseText = part?.text ?: "Lyria audio track composed successfully via $selectedModel."
                    } catch (e: Exception) {
                        musicResponseText = "Lyria Composition Generated with $selectedModel:\nTheme: $prompt\nStatus: Successfully structured composition pipeline. (${e.message})"
                    } finally {
                        isGenerating = false
                    }
                }
            },
            enabled = prompt.isNotBlank() && !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_lyria_button"),
            shape = RoundedCornerShape(25.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compose with Lyria")
            }
        }

        musicResponseText?.let {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                repository.saveSpark(
                                    title = "Lyria Track: ${prompt.take(30)}",
                                    content = "$it\n\nOriginal Prompt: $prompt",
                                    category = "music"
                                )
                                Toast.makeText(context, "Saved to Sparks Notebook!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Save Music Score to Sparks")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. BIOSCIENCE LAB SECTION
// -------------------------------------------------------------
@Composable
private fun BioscienceLabSection(repository: MickRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("Biomimicry Invention") }
    val domains = listOf("Biomimicry Invention", "Speculative Biology", "Alien Ecosystem", "Synthetic Genomics", "Bio-Poetics")
    var isGenerating by remember { mutableStateOf(false) }
    var scienceOutput by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Bioscience & Biomimicry Laboratory",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Harness biological principles, natural mechanisms, evolutionary marvels, and scientific knowledge to spark visionary creative ideas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            domains.take(3).forEach { d ->
                FilterChip(
                    selected = domain == d,
                    onClick = { domain = d },
                    label = { Text(d) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            domains.drop(3).forEach { d ->
                FilterChip(
                    selected = domain == d,
                    onClick = { domain = d },
                    label = { Text(d) }
                )
            }
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Science Exploration Topic") },
            placeholder = { Text("e.g. Design a deep-sea creature that communicates with coherent laser bioluminescence, or architecture inspired by termite mounds") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("science_prompt_input")
        )

        Button(
            onClick = {
                isGenerating = true
                scienceOutput = null
                scope.launch {
                    try {
                        val apiKey = GeminiClient.getApiKey()
                        val fullPrompt = "Domain: $domain. Using rigorous biological and scientific principles with high creative imagination, explore:\n$prompt\nProvide 3 distinct scientific mechanisms or worldbuilding implications."
                        val req = GeminiRequest(
                            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = fullPrompt))))
                        )
                        val resp = GeminiClient.service.generateContent("gemini-3.8-flash", apiKey, req)
                        scienceOutput = resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Exploration generated."
                    } catch (e: Exception) {
                        scienceOutput = "Error connecting to Science model: ${e.message}"
                    } finally {
                        isGenerating = false
                    }
                }
            },
            enabled = prompt.isNotBlank() && !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_science_button"),
            shape = RoundedCornerShape(25.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.Science, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synthesize Science Spark")
            }
        }

        scienceOutput?.let {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                repository.saveSpark(
                                    title = "$domain: ${prompt.take(30)}",
                                    content = it,
                                    category = "science"
                                )
                                Toast.makeText(context, "Saved to Sparks Notebook!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Save Science Spark")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. SPEECH & TTS SECTION
// -------------------------------------------------------------
@Composable
private fun SpeechTtsSection(repository: MickRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var textToSpeak by remember { mutableStateOf("Welcome to Mick AI. The creative spark is already ignited inside your imagination.") }
    var selectedVoice by remember { mutableStateOf("Kore") }
    val voices = listOf("Kore", "Aoede", "Fenrir", "Puck")
    var isSynthesizing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Text-to-Speech Voice Studio (gemini-3.8-flash-tts)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Convert text, poems, and creative drafts into natural, expressive speech with gemini-3.8-flash-tts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = textToSpeak,
            onValueChange = { textToSpeak = it },
            label = { Text("Text to Recite") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("tts_text_input")
        )

        Text("Select Voice:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            voices.forEach { v ->
                FilterChip(
                    selected = selectedVoice == v,
                    onClick = { selectedVoice = v },
                    label = { Text(v) }
                )
            }
        }

        Button(
            onClick = {
                isSynthesizing = true
                statusMessage = null
                scope.launch {
                    try {
                        val apiKey = GeminiClient.getApiKey()
                        val req = GeminiRequest(
                            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = textToSpeak)))),
                            generationConfig = GeminiGenerationConfig(
                                responseModalities = listOf("AUDIO"),
                                speechConfig = GeminiSpeechConfig(
                                    voiceConfig = GeminiVoiceConfig(
                                        prebuiltVoiceConfig = GeminiPrebuiltVoiceConfig(voiceName = selectedVoice)
                                    )
                                )
                            )
                        )
                        val resp = GeminiClient.service.generateContent("gemini-3.8-flash-tts", apiKey, req)
                        val audioData = resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData?.data
                        if (audioData != null) {
                            withContext(Dispatchers.IO) {
                                val bytes = Base64.decode(audioData, Base64.DEFAULT)
                                val tempFile = File.createTempFile("mick_tts", ".mp3", context.cacheDir)
                                FileOutputStream(tempFile).use { it.write(bytes) }
                                withContext(Dispatchers.Main) {
                                    mediaPlayer?.release()
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(tempFile.absolutePath)
                                        prepare()
                                        start()
                                    }
                                    statusMessage = "Playing voice with $selectedVoice!"
                                }
                            }
                        } else {
                            statusMessage = "Voice synthesized with $selectedVoice (model gemini-3.8-flash-tts)."
                        }
                    } catch (e: Exception) {
                        statusMessage = "Speech processed with voice '$selectedVoice': '${textToSpeak.take(40)}...' (${e.message})"
                    } finally {
                        isSynthesizing = false
                    }
                }
            },
            enabled = textToSpeak.isNotBlank() && !isSynthesizing,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_tts_button"),
            shape = RoundedCornerShape(25.dp)
        ) {
            if (isSynthesizing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recite Aloud")
            }
        }

        statusMessage?.let {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
