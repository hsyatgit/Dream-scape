package com.hsyatgit.dreamscape.provider

import com.hsyatgit.dreamscape.data.AsrConfig
import com.hsyatgit.dreamscape.data.LlmConfig
import com.hsyatgit.dreamscape.data.TtsConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

interface LlmProvider {
    val id: String
    suspend fun complete(prompt: String, config: LlmConfig): String
}

interface TtsProvider {
    val id: String
    suspend fun synthesize(text: String, config: TtsConfig): ByteArray
}

interface AsrProvider {
    val id: String
    suspend fun transcribeWav(audio: ByteArray, config: AsrConfig): String
}

object HttpClients {
    val shared: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()
}

class OpenAiCompatibleLlmProvider(
    private val http: OkHttpClient = HttpClients.shared,
) : LlmProvider {
    override val id: String = "openai_compatible"

    override suspend fun complete(prompt: String, config: LlmConfig): String = withContext(Dispatchers.IO) {
        require(config.apiKey.isNotBlank()) { "还没有填写对话模型的密钥" }
        val body = OpenAiEndpoints.chatBody(
            model = config.model.ifBlank { "gpt-4o-mini" },
            messages = listOf("user" to prompt),
            stream = config.streaming,
        )
        val request = Request.Builder()
            .url(OpenAiEndpoints.chatUrl(config.baseUrl))
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("对话模型失败：HTTP ${response.code}")
            if (config.streaming) {
                SseChatParser.collect(payload).ifBlank { error("流式响应是空的") }
            } else {
                ResponsePipeline.apply(
                    payload.encodeToByteArray(),
                    listOf(PipelineStep("parse_json"), PipelineStep("pick", "choices[0].message.content")),
                ).decodeToString()
            }
        }
    }
}

class OpenAiCompatibleTtsProvider(
    private val http: OkHttpClient = HttpClients.shared,
) : TtsProvider {
    override val id: String = "openai_compatible"

    override suspend fun synthesize(text: String, config: TtsConfig): ByteArray = withContext(Dispatchers.IO) {
        require(config.apiKey.isNotBlank()) { "还没有填写语音合成的密钥" }
        val body = OpenAiEndpoints.speechBody(
            model = config.model.ifBlank { "tts-1" },
            voice = config.voice.ifBlank { "alloy" },
            text = text,
        )
        val request = Request.Builder()
            .url(OpenAiEndpoints.speechUrl(config.baseUrl))
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            val bytes = response.body?.bytes() ?: byteArrayOf()
            if (!response.isSuccessful) error("语音合成失败：HTTP ${response.code}")
            bytes
        }
    }
}

class GenericHttpTtsProvider(
    private val http: OkHttpClient = HttpClients.shared,
) : TtsProvider {
    override val id: String = "generic_http"

    override suspend fun synthesize(text: String, config: TtsConfig): ByteArray = withContext(Dispatchers.IO) {
        val url = config.httpUrl.ifBlank { error("还没有填写通用语音接口的地址") }
        val body = BodyTemplate.render(config.bodyTemplate, text, config.voice, config.apiKey)
        val headers = HeaderTemplate.render(config.headersJson, config.apiKey)
        val builder = Request.Builder().url(url)
        headers.forEach { (name, value) -> builder.header(name, value) }
        val method = config.httpMethod.ifBlank { "POST" }.uppercase()
        builder.method(method, body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        http.newCall(builder.build()).execute().use { response ->
            val bytes = response.body?.bytes() ?: byteArrayOf()
            if (!response.isSuccessful) error("通用语音接口失败：HTTP ${response.code}")
            ResponsePipeline.apply(bytes, ResponsePipeline.parseSteps(config.pipelineJson))
        }
    }
}

class OpenAiCompatibleAsrProvider(
    private val http: OkHttpClient = HttpClients.shared,
) : AsrProvider {
    override val id: String = "openai_compatible"

    override suspend fun transcribeWav(audio: ByteArray, config: AsrConfig): String = withContext(Dispatchers.IO) {
        require(config.apiKey.isNotBlank()) { "还没有填写语音识别的密钥" }
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", config.model.ifBlank { "whisper-1" })
            .addFormDataPart(
                "file",
                "audio.wav",
                audio.toRequestBody("audio/wav".toMediaType()),
            )
            .build()
        val request = Request.Builder()
            .url(OpenAiEndpoints.transcriptionUrl(config.baseUrl))
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(multipart)
            .build()
        http.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("语音识别失败：HTTP ${response.code}")
            val root = Json.parseToJsonElement(payload) as? JsonObject ?: error("识别响应不是 JSON")
            (root["text"] as? JsonPrimitive)?.contentOrNull ?: error("识别响应里没有文本")
        }
    }
}
