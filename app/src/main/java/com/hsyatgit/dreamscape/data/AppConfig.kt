package com.hsyatgit.dreamscape.data

import com.hsyatgit.dreamscape.provider.GenericTtsExample
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LlmConfig(
    val providerId: String = "none",
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val streaming: Boolean = false,
)

@Serializable
data class TtsConfig(
    val providerId: String = "android_system",
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "tts-1",
    val voice: String = "alloy",
    val httpMethod: String = "POST",
    val httpUrl: String = "",
    val headersJson: String = """{"Authorization":"Bearer {apiKey}","Content-Type":"application/json"}""",
    val bodyTemplate: String = """{"model":"{model}","input":"{text}","voice":"{voice}"}""",
    val pipelineJson: String = """[{"type":"raw_bytes"}]""",
)

@Serializable
data class AsrConfig(
    val providerId: String = "android_speech",
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "whisper-1",
)

@Serializable
data class PlaybackConfig(
    val prefetchDepth: Int = 3,
    val fadeOut: Boolean = true,
    val maxSentenceChars: Int = 42,
)

@Serializable
data class AppConfig(
    val llm: LlmConfig = LlmConfig(),
    val tts: TtsConfig = TtsConfig(),
    val asr: AsrConfig = AsrConfig(),
    val playback: PlaybackConfig = PlaybackConfig(),
) {
    fun withoutSecrets(): AppConfig = copy(
        llm = llm.copy(apiKey = ""),
        tts = tts.copy(apiKey = ""),
        asr = asr.copy(apiKey = ""),
    )

    fun withGenericTtsExample(): AppConfig = copy(
        tts = tts.copy(
            providerId = "generic_http",
            httpMethod = GenericTtsExample.METHOD,
            httpUrl = GenericTtsExample.URL,
            headersJson = GenericTtsExample.HEADERS,
            bodyTemplate = GenericTtsExample.BODY,
            pipelineJson = GenericTtsExample.PIPELINE,
            voice = GenericTtsExample.VOICE,
        ),
    )
}

object ConfigCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun toJson(config: AppConfig, includeSecrets: Boolean): String {
        val payload = if (includeSecrets) config else config.withoutSecrets()
        return json.encodeToString(AppConfig.serializer(), payload)
    }

    fun fromJson(raw: String): AppConfig = json.decodeFromString(AppConfig.serializer(), raw)
}
