package com.hsyatgit.dreamscape.provider

import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class PipelineStep(
    val type: String,
    val path: String? = null,
)

object BodyTemplate {
    fun render(template: String, text: String, voice: String, apiKey: String): String {
        return template
            .replace("{text}", jsonEscape(text))
            .replace("{voice}", jsonEscape(voice))
            .replace("{apiKey}", jsonEscape(apiKey))
    }

    fun jsonEscape(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}

object HeaderTemplate {
    private val json = Json { ignoreUnknownKeys = true }

    fun render(headersJson: String, apiKey: String): Map<String, String> {
        if (headersJson.isBlank()) return emptyMap()
        val rendered = BodyTemplate.render(headersJson, text = "", voice = "", apiKey = apiKey)
        val element = json.parseToJsonElement(rendered) as? JsonObject ?: error("请求头需要是 JSON 对象")
        return element.mapValues { (_, value) ->
            (value as? JsonPrimitive)?.contentOrNull ?: value.toString()
        }
    }
}

object ResponsePipeline {
    private val json = Json { ignoreUnknownKeys = true }
    private val token = Regex("""([A-Za-z_][A-Za-z0-9_]*)|\[(\d+)]""")

    fun parseSteps(raw: String): List<PipelineStep> {
        if (raw.isBlank()) return listOf(PipelineStep("raw_bytes"))
        val array = json.parseToJsonElement(raw) as? JsonArray ?: error("响应管道需要是 JSON 数组")
        return array.map { element ->
            val obj = element as? JsonObject ?: error("管道步骤需要是对象")
            PipelineStep(
                type = (obj["type"] as? JsonPrimitive)?.content ?: error("管道步骤缺少 type"),
                path = (obj["path"] as? JsonPrimitive)?.contentOrNull,
            )
        }
    }

    fun apply(bytes: ByteArray, steps: List<PipelineStep>): ByteArray {
        var current: Any = bytes
        for (step in steps) {
            current = when (step.type) {
                "parse_json" -> json.parseToJsonElement(asText(current))
                "pick" -> pick(asJson(current), step.path ?: error("pick 缺少 path"))
                "base64_decode" -> Base64.getMimeDecoder().decode(asText(current))
                "raw_bytes" -> asBytes(current)
                else -> error("未知管道步骤：${step.type}")
            }
        }
        return asBytes(current)
    }

    private fun pick(element: JsonElement, path: String): JsonElement {
        var current = element
        for (match in token.findAll(path)) {
            val name = match.groupValues[1]
            val index = match.groupValues[2]
            current = if (name.isNotEmpty()) {
                val obj = current as? JsonObject ?: error("路径 $path 在 $name 处不是对象")
                obj[name] ?: error("路径缺少 $name")
            } else {
                val array = current as? JsonArray ?: error("路径 $path 在下标处不是数组")
                array[index.toInt()]
            }
        }
        return current
    }

    private fun asText(value: Any): String = when (value) {
        is ByteArray -> value.decodeToString()
        is String -> value
        is JsonPrimitive -> value.content
        is JsonElement -> value.toString()
        else -> error("无法把 ${value::class.simpleName} 当成文本")
    }

    private fun asJson(value: Any): JsonElement = when (value) {
        is JsonElement -> value
        is String -> json.parseToJsonElement(value)
        is ByteArray -> json.parseToJsonElement(value.decodeToString())
        else -> error("当前步骤没有 JSON")
    }

    private fun asBytes(value: Any): ByteArray = when (value) {
        is ByteArray -> value
        is String -> value.encodeToByteArray()
        is JsonPrimitive -> value.content.encodeToByteArray()
        else -> error("管道结束时仍不是音频字节")
    }
}

object OpenAiEndpoints {
    fun chatUrl(baseUrl: String): String = endpoint(baseUrl, "chat/completions")
    fun speechUrl(baseUrl: String): String = endpoint(baseUrl, "audio/speech")
    fun transcriptionUrl(baseUrl: String): String = endpoint(baseUrl, "audio/transcriptions")

    private fun endpoint(baseUrl: String, path: String): String {
        val root = baseUrl.trim().trimEnd('/')
        return if (root.endsWith("/$path")) root else "$root/$path"
    }

    fun chatBody(model: String, messages: List<Pair<String, String>>, stream: Boolean): String {
        val payload = buildString {
            append("{\"model\":")
            append('"')
            append(BodyTemplate.jsonEscape(model))
            append('"')
            append(",\"temperature\":0.7,\"stream\":")
            append(stream)
            append(",\"messages\":[")
            messages.forEachIndexed { index, (role, content) ->
                if (index > 0) append(',')
                append("{\"role\":\"")
                append(BodyTemplate.jsonEscape(role))
                append("\",\"content\":\"")
                append(BodyTemplate.jsonEscape(content))
                append("\"}")
            }
            append("]}")
        }
        return payload
    }

    fun speechBody(model: String, voice: String, text: String): String =
        """{"model":"${BodyTemplate.jsonEscape(model)}","voice":"${BodyTemplate.jsonEscape(voice)}","input":"${BodyTemplate.jsonEscape(text)}","response_format":"mp3"}"""
}

object SseChatParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun collect(sse: String): String = buildString {
        sse.lineSequence().forEach { line ->
            val data = line.removePrefix("data:").trim()
            if (data.isEmpty() || data == "[DONE]" || !line.startsWith("data:")) return@forEach
            val root = runCatching { json.parseToJsonElement(data) }.getOrNull() as? JsonObject ?: return@forEach
            val choices = root["choices"] as? JsonArray ?: return@forEach
            val first = choices.firstOrNull() as? JsonObject ?: return@forEach
            val delta = (first["delta"] as? JsonObject)?.get("content")
            val message = (first["message"] as? JsonObject)?.get("content")
            val piece = (delta as? JsonPrimitive)?.contentOrNull ?: (message as? JsonPrimitive)?.contentOrNull
            if (piece != null) append(piece)
        }
    }
}

object GenericTtsExample {
    const val METHOD = "POST"
    const val URL = "https://www.dmxapi.cn/v1/chat/completions"
    const val HEADERS =
        """{"Authorization":"Bearer {apiKey}","Content-Type":"application/json"}"""
    const val BODY =
        """{"model":"mimo-v2.5-tts","messages":[{"role":"user","content":"温柔、自然"},{"role":"assistant","content":"{text}"}],"audio":{"format":"wav","voice":"{voice}"}}"""
    const val PIPELINE =
        """[{"type":"parse_json"},{"type":"pick","path":"choices[0].message.audio.data"},{"type":"base64_decode"}]"""
    const val VOICE = "冰糖"
}
