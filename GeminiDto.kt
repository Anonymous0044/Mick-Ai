package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null,
    val tools: List<GeminiTool>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val thinkingConfig: GeminiThinkingConfig? = null,
    val imageConfig: GeminiImageConfig? = null,
    val speechConfig: GeminiSpeechConfig? = null,
    val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiThinkingConfig(
    val thinkingLevel: String // "HIGH" or "LOW"
)

@JsonClass(generateAdapter = true)
data class GeminiImageConfig(
    val aspectRatio: String? = null, // "1:1", "16:9", etc.
    val imageSize: String? = null    // "1K", "2K", "4K"
)

@JsonClass(generateAdapter = true)
data class GeminiSpeechConfig(
    val voiceConfig: GeminiVoiceConfig
)

@JsonClass(generateAdapter = true)
data class GeminiVoiceConfig(
    val prebuiltVoiceConfig: GeminiPrebuiltVoiceConfig
)

@JsonClass(generateAdapter = true)
data class GeminiPrebuiltVoiceConfig(
    val voiceName: String = "Kore" // Aoede, Kore, Fenrir, Puck, etc.
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    val googleSearch: Map<String, String>? = null,
    val googleMaps: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class VeoGenerateVideosRequest(
    val prompt: String,
    val config: VeoConfig? = null
)

@JsonClass(generateAdapter = true)
data class VeoConfig(
    val numberOfVideos: Int = 1,
    val resolution: String = "1080p",
    val aspectRatio: String = "16:9" // "16:9" or "9:16"
)

@JsonClass(generateAdapter = true)
data class VeoGenerateVideosResponse(
    val name: String? = null,
    val done: Boolean? = null
)
