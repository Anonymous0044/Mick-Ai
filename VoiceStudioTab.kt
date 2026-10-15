package com.example.ui.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.CustomVoice
import com.example.data.repository.MickRepository
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.appText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceStudioTab(
    repository: MickRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val helper = remember { VoiceAudioHelper(context) }
    val customVoices by repository.observeCustomVoices().collectAsState(initial = emptyList())
    val currentLang = LocalAppLanguage.current

    DisposableEffect(Unit) {
        onDispose {
            helper.release()
        }
    }

    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(
        "Copy Voice (Clone)" to "آواز کی نقل (کلون)",
        "Create Voice" to "نئی آواز بنائیں",
        "Recite & TTS" to "آواز میں سنیں",
        "Voice Vault" to "میری محفوظ آوازیں"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (currentLang.code == "ur") "وائس فورج اور کلوننگ اسٹوڈیو" else "Voice Forge & Audio Studio",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (currentLang.code == "ur") "اصلی آواز کی نقل، نئی آوازیں، اور اردو و انگریزی میں صوتی تلاوت" else "Clone real voices, sculpt custom personas & synthesize expressive speech in English & Urdu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Sub-Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            subTabs.forEachIndexed { index, (enLabel, urLabel) ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = {
                        Text(
                            text = if (currentLang.code == "ur") urLabel else enLabel,
                            fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.testTag("voice_subtab_$index")
                )
            }
        }

        when (selectedSubTab) {
            0 -> VoiceCopySection(
                repository = repository,
                helper = helper,
                onVoiceCloned = { selectedSubTab = 2 }
            )
            1 -> VoiceCreateSection(
                repository = repository,
                helper = helper,
                onVoiceCreated = { selectedSubTab = 2 }
            )
            2 -> VoiceReciteSection(
                repository = repository,
                helper = helper,
                customVoices = customVoices
            )
            3 -> VoiceVaultSection(
                repository = repository,
                helper = helper,
                customVoices = customVoices,
                onSelectVoiceForTTS = { selectedSubTab = 2 }
            )
        }
    }
}

// -----------------------------------------------------------------
// 1. VOICE COPY (VOICE CLONING FROM MICROPHONE)
// -----------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceCopySection(
    repository: MickRepository,
    helper: VoiceAudioHelper,
    onVoiceCloned: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentLang = LocalAppLanguage.current

    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableIntStateOf(0) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var isPlayingSample by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var cloneAnalysis by remember { mutableStateOf<ClonedVoiceAnalysis?>(null) }
    var customCloneName by remember { mutableStateOf("") }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    // Waveform simulation bars
    val amplitudes = remember { mutableStateListOf(10, 25, 45, 60, 30, 20, 50, 75, 40, 20) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = File(context.cacheDir, "sample_voice_${System.currentTimeMillis()}.m4a")
            val success = helper.startRecording(file)
            if (success) {
                recordedFile = file
                isRecording = true
                recordingDurationSec = 0
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to record a voice sample.", Toast.LENGTH_SHORT).show()
        }
    }

    // Timer & amplitude updates while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isActive && isRecording) {
                delay(200)
                recordingDurationSec += 1
                val amp = (helper.getMaxAmplitude() / 500).coerceIn(10, 100)
                if (amplitudes.isNotEmpty()) {
                    amplitudes.removeAt(0)
                    amplitudes.add(amp)
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (currentLang.code == "ur") "آواز کی نقل اور صوتی کلوننگ" else "Voice Copy & Acoustic Cloning",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (currentLang.code == "ur")
                    "مائیکروفون سے 5 سے 10 سیکنڈ بولیں۔ میک اے آئی آپ کے بولنے کے انداز، پچ اور لہجے کا تجزیہ کر کے کلون شدہ آواز تیار کرے گا۔"
                else
                    "Record a 5–10 second voice sample. Mick AI extracts your pitch, cadence, formant frequencies, and timbre to construct a high-fidelity voice clone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Waveform Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    amplitudes.forEach { amp ->
                        val barHeight = if (isRecording) (amp * 0.7f).dp.coerceIn(10.dp, 65.dp) else 15.dp
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isRecording) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                        )
                    }
                }
            }

            // Duration & status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRecording) "Recording... (${recordingDurationSec / 5}s)" else if (recordedFile != null) "Sample Ready (${recordedFile?.length()?.div(1024)} KB)" else "Ready to Record",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (recordedFile != null && !isRecording) {
                    OutlinedButton(
                        onClick = {
                            if (isPlayingSample) {
                                helper.stopAudio()
                                isPlayingSample = false
                            } else {
                                recordedFile?.let { file ->
                                    isPlayingSample = true
                                    helper.playAudio(file.absolutePath) {
                                        isPlayingSample = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("btn_play_sample")
                    ) {
                        Icon(
                            imageVector = if (isPlayingSample) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPlayingSample) "Stop" else "Play Sample")
                    }
                }
            }

            // Record / Stop Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isRecording) {
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                val file = File(context.cacheDir, "sample_voice_${System.currentTimeMillis()}.m4a")
                                val success = helper.startRecording(file)
                                if (success) {
                                    recordedFile = file
                                    isRecording = true
                                    recordingDurationSec = 0
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_start_recording"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (currentLang.code == "ur") "آواز ریکارڈ کریں" else "Record Voice Sample")
                    }
                } else {
                    Button(
                        onClick = {
                            helper.stopRecording()
                            isRecording = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_stop_recording"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (currentLang.code == "ur") "ریکارڈنگ روکیں" else "Stop Recording")
                    }
                }
            }

            // Analyze and Clone Voice Button
            if (recordedFile != null && !isRecording) {
                Button(
                    onClick = {
                        isAnalyzing = true
                        scope.launch {
                            try {
                                recordedFile?.let { file ->
                                    val analysis = helper.analyzeAudioForCloning(file)
                                    cloneAnalysis = analysis
                                    customCloneName = analysis.suggestedName
                                    statusFeedback = "Analysis complete! Voice blueprint synthesized."
                                }
                            } catch (e: Exception) {
                                statusFeedback = "Analysis error: ${e.message}"
                            } finally {
                                isAnalyzing = false
                            }
                        }
                    },
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_analyze_clone"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onSecondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Extracting Acoustic Features...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (currentLang.code == "ur") "تجزیہ کریں اور آواز کی نقل بنائیں" else "Analyze & Copy Voice (Clone)")
                    }
                }
            }

            // Cloned Voice Results & Save
            cloneAnalysis?.let { analysis ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLang.code == "ur") "صوتی پروفائل تیار ہے!" else "Voice Profile Extracted!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = analysis.timbreDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text("Base: ${analysis.baseVoice}") }
                            )
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text("Pitch: ${String.format("%.2f", analysis.pitch)}x") }
                            )
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text("Pace: ${String.format("%.2f", analysis.speed)}x") }
                            )
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text(analysis.language) }
                            )
                        }

                        OutlinedTextField(
                            value = customCloneName,
                            onValueChange = { customCloneName = it },
                            label = { Text(if (currentLang.code == "ur") "کلون شدہ آواز کا نام" else "Cloned Voice Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_cloned_voice_name")
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    repository.saveCustomVoice(
                                        name = customCloneName.ifBlank { "My Cloned Voice" },
                                        description = analysis.timbreDescription,
                                        baseVoice = analysis.baseVoice,
                                        gender = analysis.gender,
                                        language = analysis.language,
                                        style = analysis.style,
                                        pitch = analysis.pitch,
                                        speed = analysis.speed,
                                        isCloned = true,
                                        sampleAudioPath = recordedFile?.absolutePath,
                                        voiceBlueprint = analysis.voiceBlueprint
                                    )
                                    Toast.makeText(context, "Voice Clone saved to Vault!", Toast.LENGTH_SHORT).show()
                                    onVoiceCloned()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_save_cloned_voice"),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (currentLang.code == "ur") "کلون شدہ آواز والٹ میں محفوظ کریں" else "Save Cloned Voice to Vault")
                        }
                    }
                }
            }

            statusFeedback?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// 2. CREATE VOICE (CUSTOM VOICE SCULPTOR)
// -----------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceCreateSection(
    repository: MickRepository,
    helper: VoiceAudioHelper,
    onVoiceCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentLang = LocalAppLanguage.current

    var voiceName by remember { mutableStateOf("") }
    var selectedBaseVoice by remember { mutableStateOf("Kore") }
    val baseVoices = listOf("Kore", "Aoede", "Fenrir", "Puck")

    var selectedGender by remember { mutableStateOf("Female") }
    val genders = listOf("Female", "Male", "Robotic / AI", "Ethereal")

    var selectedLanguage by remember { mutableStateOf("English") }
    val languages = listOf("English", "Urdu (اردو)", "Multilingual")

    var selectedStyle by remember { mutableStateOf("Warm & Expressive") }
    val styles = listOf(
        "Warm & Expressive",
        "Poetic / Ghazal Reciter",
        "Dramatic Storyteller",
        "Calm & Meditative",
        "Energetic Podcaster",
        "News & Formal"
    )

    var pitch by remember { mutableFloatStateOf(1.0f) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var customPrompt by remember { mutableStateOf("") }
    var isPreviewing by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (currentLang.code == "ur") "وائس ڈیزائنر: نئی آواز بنائیں" else "Voice Sculptor: Create Custom Voice",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (currentLang.code == "ur")
                    "پچ، رفتار، لہجہ اور زبان منتخب کر کے ایک مکمل نئی ڈیجیٹل آواز بنائیں۔"
                else
                    "Sculpt a new virtual voice persona from scratch. Tune pitch, tempo, artistic delivery, and linguistic accent for English and Urdu recitals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = voiceName,
                onValueChange = { voiceName = it },
                label = { Text(if (currentLang.code == "ur") "نئی آواز کا نام" else "Custom Voice Name") },
                placeholder = { Text(if (currentLang.code == "ur") "مثلاً طارق غزل راوی، زہرا" else "e.g. Zenith Narrator, Sufi Bard") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_custom_voice_name")
            )

            // Base Voice selection
            Text("Base Neural Timbre:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                baseVoices.forEach { b ->
                    FilterChip(
                        selected = selectedBaseVoice == b,
                        onClick = { selectedBaseVoice = b },
                        label = { Text(b) }
                    )
                }
            }

            // Gender / Vocal Persona
            Text("Persona / Register:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                genders.forEach { g ->
                    FilterChip(
                        selected = selectedGender == g,
                        onClick = { selectedGender = g },
                        label = { Text(g) }
                    )
                }
            }

            // Language & Accent
            Text("Language & Accent:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { l ->
                    FilterChip(
                        selected = selectedLanguage == l,
                        onClick = { selectedLanguage = l },
                        label = { Text(l) }
                    )
                }
            }

            // Delivery Style
            Text("Delivery Style & Emotion:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                styles.forEach { s ->
                    FilterChip(
                        selected = selectedStyle == s,
                        onClick = { selectedStyle = s },
                        label = { Text(s) }
                    )
                }
            }

            // Pitch Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pitch Adjustment", style = MaterialTheme.typography.labelMedium)
                    Text("${String.format("%.2f", pitch)}x (${if (pitch < 0.9f) "Deep" else if (pitch > 1.1f) "High" else "Natural"})", style = MaterialTheme.typography.labelSmall)
                }
                Slider(
                    value = pitch,
                    onValueChange = { pitch = it },
                    valueRange = 0.5f..1.8f,
                    modifier = Modifier.testTag("slider_pitch")
                )
            }

            // Speed Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Speaking Pace / Speed", style = MaterialTheme.typography.labelMedium)
                    Text("${String.format("%.2f", speed)}x (${if (speed < 0.9f) "Poetic Slow" else if (speed > 1.1f) "Fast" else "Standard"})", style = MaterialTheme.typography.labelSmall)
                }
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.5f..1.8f,
                    modifier = Modifier.testTag("slider_speed")
                )
            }

            // Custom Instructions
            OutlinedTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it },
                label = { Text("Vocal Persona Prompt (Optional)") },
                placeholder = { Text("e.g. Speak with subtle warmth and slow cadence, adding poetic rhythm.") },
                modifier = Modifier.fillMaxWidth()
            )

            // Preview & Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isPreviewing = true
                        val testPhrase = if (selectedLanguage.contains("Urdu")) {
                            "روشنی اور تخیل کا نیا سفر، میک اے آئی کے سنگ۔"
                        } else {
                            "Hello, this is a live test of your sculpted custom voice."
                        }
                        helper.speakWithTts(
                            text = testPhrase,
                            pitch = pitch,
                            speed = speed,
                            languageCode = if (selectedLanguage.contains("Urdu")) "ur" else "en",
                            onDone = { isPreviewing = false }
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_preview_custom_voice")
                ) {
                    Icon(
                        imageVector = if (isPreviewing) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPreviewing) "Speaking..." else "Preview Voice")
                }

                Button(
                    onClick = {
                        scope.launch {
                            val finalName = voiceName.ifBlank { "Custom ${selectedGender} Voice" }
                            val desc = "Custom voice persona ($selectedGender, $selectedLanguage, Style: $selectedStyle)"
                            repository.saveCustomVoice(
                                name = finalName,
                                description = desc,
                                baseVoice = selectedBaseVoice,
                                gender = selectedGender,
                                language = selectedLanguage,
                                style = selectedStyle,
                                pitch = pitch,
                                speed = speed,
                                isCloned = false,
                                voiceBlueprint = "Sculpted: Base=$selectedBaseVoice, Pitch=${pitch}x, Speed=${speed}x. Prompt: $customPrompt"
                            )
                            Toast.makeText(context, "Voice '$finalName' saved to Vault!", Toast.LENGTH_SHORT).show()
                            onVoiceCreated()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_save_custom_voice"),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (currentLang.code == "ur") "محفوظ کریں" else "Save Voice")
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// 3. RECITE & SYNTHESIZE (TEXT TO SPEECH USING CUSTOM / CLONED VOICES)
// -----------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceReciteSection(
    repository: MickRepository,
    helper: VoiceAudioHelper,
    customVoices: List<CustomVoice>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentLang = LocalAppLanguage.current

    var textToRecite by remember {
        mutableStateOf("Welcome to Mick AI. The creative spark is already ignited inside your imagination.")
    }

    var selectedVoiceId by remember {
        mutableStateOf(customVoices.firstOrNull()?.id ?: "preset_kore")
    }

    val activeVoice = customVoices.firstOrNull { it.id == selectedVoiceId }
        ?: customVoices.firstOrNull()

    var isSynthesizing by remember { mutableStateOf(false) }
    var playbackStatus by remember { mutableStateOf<String?>(null) }
    var useGeminiNeural by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (currentLang.code == "ur") "آواز میں سنیں اور صوتی تلاوت" else "Recite & Speech Synthesis",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (currentLang.code == "ur")
                    "کسی بھی محفوظ یا کلون شدہ آواز سے اردو یا انگریزی متن باآواز بلند سنیں۔"
                else
                    "Synthesize written ideas, Urdu ghazals, stories, and prompts aloud with your cloned or custom voice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Select Voice Chips
            Text("Select Voice:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                customVoices.forEach { voice ->
                    val isSelected = (voice.id == selectedVoiceId)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedVoiceId = voice.id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (voice.isCloned) {
                                    Text("🧬 ")
                                } else if (voice.userId != "system") {
                                    Text("✨ ")
                                }
                                Text(voice.name)
                            }
                        },
                        modifier = Modifier.testTag("chip_voice_${voice.id}")
                    )
                }
            }

            // Quick Samples Bar
            Text("Quick Samples (English & Urdu):", style = MaterialTheme.typography.labelSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        textToRecite = "روشنی اور تخیل کا نیا سفر، میک اے آئی کے سنگ۔"
                    },
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("اردو تلاوت", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        textToRecite = "دل ناداں تجھے ہوا کیا ہے، آخر اس درد کی دوا کیا ہے۔"
                    },
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("اردو شاعری (غزل)", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        textToRecite = "Welcome to Mick AI. The creative spark is already ignited inside your imagination."
                    },
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Creative Welcome", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        textToRecite = "Neural synthesizer active. Crafting worlds beyond the edge of perception."
                    },
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Sci-Fi Trailer", fontSize = 12.sp)
                }
            }

            // Input Text Field
            OutlinedTextField(
                value = textToRecite,
                onValueChange = { textToRecite = it },
                label = { Text(if (currentLang.code == "ur") "متن درج کریں" else "Text to Recite") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .testTag("input_recite_text")
            )

            // Engine Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (useGeminiNeural) "Gemini Cloud Neural Audio" else "Native Real-time Engine",
                    style = MaterialTheme.typography.labelMedium
                )
                FilterChip(
                    selected = useGeminiNeural,
                    onClick = { useGeminiNeural = !useGeminiNeural },
                    label = { Text(if (useGeminiNeural) "Cloud Neural" else "Real-time Native") }
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        isSynthesizing = true
                        playbackStatus = "Synthesizing voice with ${activeVoice?.name}..."
                        scope.launch {
                            val v = activeVoice ?: CustomVoice()
                            if (useGeminiNeural) {
                                val target = File(context.cacheDir, "gemini_tts_${System.currentTimeMillis()}.mp3")
                                val ok = helper.synthesizeGeminiVoice(textToRecite, v.baseVoice, target)
                                if (ok) {
                                    helper.playAudio(target.absolutePath) {
                                        isSynthesizing = false
                                        playbackStatus = "Finished playing recited speech."
                                    }
                                } else {
                                    // Fallback to Native TTS
                                    helper.speakWithTts(
                                        text = textToRecite,
                                        pitch = v.pitch,
                                        speed = v.speed,
                                        languageCode = if (v.language.contains("Urdu")) "ur" else "en",
                                        onDone = {
                                            isSynthesizing = false
                                            playbackStatus = "Played with on-device engine."
                                        }
                                    )
                                }
                            } else {
                                helper.speakWithTts(
                                    text = textToRecite,
                                    pitch = v.pitch,
                                    speed = v.speed,
                                    languageCode = if (v.language.contains("Urdu")) "ur" else "en",
                                    onDone = {
                                        isSynthesizing = false
                                        playbackStatus = "Played with on-device engine."
                                    }
                                )
                            }
                        }
                    },
                    enabled = textToRecite.isNotBlank() && !isSynthesizing,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_recite_aloud"),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (isSynthesizing) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (currentLang.code == "ur") "باآواز بلند سنیں" else "Recite Aloud")
                    }
                }

                OutlinedButton(
                    onClick = {
                        helper.stopAudio()
                        isSynthesizing = false
                        playbackStatus = "Playback stopped."
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                }
            }

            // Save to Sparks Button
            Button(
                onClick = {
                    scope.launch {
                        repository.saveSpark(
                            title = "Voice Recital: ${activeVoice?.name}",
                            content = "Voice: ${activeVoice?.name} (${activeVoice?.language})\nText: $textToRecite",
                            category = "voice",
                            tags = listOf("voice", "recital", activeVoice?.language?.lowercase() ?: "en")
                        )
                        Toast.makeText(context, "Saved to Sparks Notebook!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (currentLang.code == "ur") "خیالات (Sparks) میں محفوظ کریں" else "Save Recital to Sparks")
            }

            playbackStatus?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// 4. VOICE VAULT (LIST OF ALL VOICES: CLONED, CREATED, PRESET)
// -----------------------------------------------------------------
@Composable
private fun VoiceVaultSection(
    repository: MickRepository,
    helper: VoiceAudioHelper,
    customVoices: List<CustomVoice>,
    onSelectVoiceForTTS: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (currentLang.code == "ur") "صوتی والٹ (تمام آوازیں)" else "Voice Vault & Presets",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        customVoices.forEach { voice ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (voice.isCloned) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (voice.isCloned) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.primary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (voice.isCloned) Icons.Default.GraphicEq else Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = voice.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (voice.isCloned) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondary
                                ) {
                                    Text(
                                        text = "CLONED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${voice.gender} • ${voice.language} • ${voice.style}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Pitch: ${String.format("%.2f", voice.pitch)}x • Pace: ${String.format("%.2f", voice.speed)}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Test Voice button
                    IconButton(
                        onClick = {
                            val sample = if (voice.language.contains("Urdu")) {
                                "میک اے آئی میں خوش آمدید۔ آپ کی آواز تیار ہے۔"
                            } else {
                                "Testing ${voice.name}. Creative voice ready."
                            }
                            helper.speakWithTts(
                                text = sample,
                                pitch = voice.pitch,
                                speed = voice.speed,
                                languageCode = if (voice.language.contains("Urdu")) "ur" else "en"
                            )
                        }
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Test Voice")
                    }

                    // Delete button if user-created
                    if (voice.userId != "system") {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    repository.deleteCustomVoice(voice.id)
                                    Toast.makeText(context, "Voice removed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Voice", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
