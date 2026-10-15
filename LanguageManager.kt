package com.example.ui.language

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    URDU("ur", "Urdu", "اردو")
}

object LanguageState {
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)
        private set

    fun setLanguage(language: AppLanguage, context: Context? = null) {
        currentLanguage = language
        context?.let { applyLocale(it, language.code) }
    }

    fun toggleLanguage(context: Context? = null) {
        val next = if (currentLanguage == AppLanguage.ENGLISH) AppLanguage.URDU else AppLanguage.ENGLISH
        setLanguage(next, context)
    }

    private fun applyLocale(context: Context, langCode: String) {
        try {
            val locale = Locale(langCode)
            Locale.setDefault(locale)
            val config = context.resources.configuration
            config.setLocale(locale)
            if (langCode == "ur") {
                config.setLayoutDirection(locale)
            }
        } catch (_: Exception) {}
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }

@Composable
fun ProvideAppLanguage(
    content: @Composable () -> Unit
) {
    val currentLang = LanguageState.currentLanguage
    val layoutDirection = if (currentLang == AppLanguage.URDU) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppLanguage provides currentLang,
        LocalLayoutDirection provides layoutDirection
    ) {
        content()
    }
}

/**
 * Reactive bilingual dictionary lookup for immediate UI updates.
 */
object AppStrings {
    private val enStrings = mapOf(
        "app_name" to "Mick AI",
        "chat" to "Brainstorm",
        "studio" to "Studio",
        "community" to "Lounge",
        "sparks" to "Sparks",
        "profile" to "Profile",
        "language_toggle" to "اردو",
        "voice_studio" to "Voice Forge & Audio",
        "voice_copy" to "Voice Copy (Clone)",
        "voice_create" to "Create Voice",
        "voice_recite" to "Recite & Synthesize",
        "voice_library" to "My Voice Vault",
        "record_mic" to "Record Voice Sample",
        "stop_recording" to "Stop Recording",
        "play_sample" to "Play Recorded Sample",
        "analyze_and_clone" to "Analyze & Copy Voice",
        "cloned_success" to "Voice successfully analyzed & copied!",
        "custom_voice_name" to "Voice Name",
        "custom_voice_gender" to "Vocal Persona / Gender",
        "custom_voice_style" to "Speaking Style",
        "custom_voice_lang" to "Language & Accent",
        "pitch" to "Pitch Adjustment",
        "speed" to "Pace / Speed",
        "preview" to "Preview Voice",
        "save_voice" to "Save Custom Voice",
        "recite_aloud" to "Recite Aloud",
        "guest_mode" to "Guest Mode",
        "upgrade_google" to "Sign In with Google",
        "urdu_language_name" to "اردو (Urdu)",
        "english_language_name" to "English"
    )

    private val urStrings = mapOf(
        "app_name" to "میک اے آئی",
        "chat" to "بات چیت",
        "studio" to "اسٹوڈیو",
        "community" to "لاؤنج",
        "sparks" to "خیالات",
        "profile" to "پروفائل",
        "language_toggle" to "English",
        "voice_studio" to "وائس فورج اور صوتی اسٹوڈیو",
        "voice_copy" to "آواز کی نقل (کلون)",
        "voice_create" to "نئی آواز بنائیں",
        "voice_recite" to "آواز میں سنیں",
        "voice_library" to "محفوظ آوازیں",
        "record_mic" to "صوتی نمونہ ریکارڈ کریں",
        "stop_recording" to "ریکارڈنگ مکمل کریں",
        "play_sample" to "ریکارڈ شدہ آواز سنیں",
        "analyze_and_clone" to "تجزیہ کریں اور آواز کی نقل بنائیں",
        "cloned_success" to "آواز کی نقل کامیابی سے تیار ہو گئی!",
        "custom_voice_name" to "آواز کا نام",
        "custom_voice_gender" to "صوتی کردار اور صنف",
        "custom_voice_style" to "ادائیگی کا انداز",
        "custom_voice_lang" to "زبان اور لہجہ",
        "pitch" to "پچ کنٹرول",
        "speed" to "رفتار اور روانی",
        "preview" to "آواز کا نمونہ سنیں",
        "save_voice" to "نئی آواز محفوظ کریں",
        "recite_aloud" to "باآواز بلند سنیں",
        "guest_mode" to "مہمان موڈ",
        "upgrade_google" to "گوگل اکاؤنٹ سے جوڑیں",
        "urdu_language_name" to "اردو (Urdu)",
        "english_language_name" to "English"
    )

    fun get(key: String, lang: AppLanguage = LanguageState.currentLanguage): String {
        return if (lang == AppLanguage.URDU) {
            urStrings[key] ?: enStrings[key] ?: key
        } else {
            enStrings[key] ?: key
        }
    }
}

@Composable
fun appText(key: String): String {
    val lang = LocalAppLanguage.current
    return AppStrings.get(key, lang)
}
