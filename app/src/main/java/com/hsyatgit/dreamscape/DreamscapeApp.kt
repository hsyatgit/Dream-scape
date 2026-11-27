package com.hsyatgit.dreamscape

import android.app.Application
import android.content.Context
import com.hsyatgit.dreamscape.data.AppConfig
import com.hsyatgit.dreamscape.data.AsrConfig
import com.hsyatgit.dreamscape.data.ConfigRepository
import com.hsyatgit.dreamscape.data.LlmConfig
import com.hsyatgit.dreamscape.data.TtsConfig
import com.hsyatgit.dreamscape.domain.GuidanceScript
import com.hsyatgit.dreamscape.domain.Intake
import com.hsyatgit.dreamscape.domain.ScriptComposer
import com.hsyatgit.dreamscape.provider.AndroidSpeechRecognizerProvider
import com.hsyatgit.dreamscape.provider.AndroidSystemTtsProvider
import com.hsyatgit.dreamscape.provider.AsrProvider
import com.hsyatgit.dreamscape.provider.GenericHttpTtsProvider
import com.hsyatgit.dreamscape.provider.LlmProvider
import com.hsyatgit.dreamscape.provider.OpenAiCompatibleAsrProvider
import com.hsyatgit.dreamscape.provider.OpenAiCompatibleLlmProvider
import com.hsyatgit.dreamscape.provider.OpenAiCompatibleTtsProvider
import com.hsyatgit.dreamscape.provider.TtsProvider

class DreamscapeApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}

class AppGraph(context: Context) {
    val configRepository = ConfigRepository(context)
    val providers = ProviderFactory(context)

    suspend fun composeScript(intake: Intake): GuidanceScript {
        val config = configRepository.load()
        val complete: (suspend (String) -> String)? =
            if (config.llm.providerId == "openai_compatible" && config.llm.apiKey.isNotBlank()) {
                val llm = providers.llm(config.llm)
                val call: suspend (String) -> String = { prompt -> llm.complete(prompt, config.llm) }
                call
            } else {
                null
            }
        return ScriptComposer(complete = complete).compose(intake)
    }
}

class ProviderFactory(context: Context) {
    private val appContext = context.applicationContext
    private val systemTts by lazy { AndroidSystemTtsProvider(appContext) }
    val systemAsr = AndroidSpeechRecognizerProvider(appContext)
    private val openAiLlm = OpenAiCompatibleLlmProvider()
    private val openAiTts = OpenAiCompatibleTtsProvider()
    private val genericTts = GenericHttpTtsProvider()
    private val openAiAsr = OpenAiCompatibleAsrProvider()

    fun llm(config: LlmConfig): LlmProvider = openAiLlm

    fun tts(config: TtsConfig): TtsProvider = when (config.providerId) {
        "openai_compatible" -> openAiTts
        "generic_http" -> genericTts
        else -> systemTts
    }

    fun asr(config: AsrConfig): AsrProvider = when (config.providerId) {
        "openai_compatible" -> openAiAsr
        else -> systemAsr
    }
}
