package com.hsyatgit.dreamscape

import com.hsyatgit.dreamscape.data.AppConfig
import com.hsyatgit.dreamscape.data.ConfigCodec
import com.hsyatgit.dreamscape.data.LlmConfig
import com.hsyatgit.dreamscape.data.TtsConfig
import com.hsyatgit.dreamscape.domain.BuiltinScripts
import com.hsyatgit.dreamscape.domain.CaptureGate
import com.hsyatgit.dreamscape.domain.GuidanceScript
import com.hsyatgit.dreamscape.domain.GuidanceSequencer
import com.hsyatgit.dreamscape.domain.Intake
import com.hsyatgit.dreamscape.domain.MicrophoneBlockedException
import com.hsyatgit.dreamscape.domain.PhaseId
import com.hsyatgit.dreamscape.domain.PhaseOrder
import com.hsyatgit.dreamscape.domain.SafetyAnswers
import com.hsyatgit.dreamscape.domain.SafetyLines
import com.hsyatgit.dreamscape.domain.SceneId
import com.hsyatgit.dreamscape.domain.ScriptComposer
import com.hsyatgit.dreamscape.domain.ScriptParser
import com.hsyatgit.dreamscape.domain.ScriptRules
import com.hsyatgit.dreamscape.domain.ScriptSegment
import com.hsyatgit.dreamscape.domain.SentenceSplitter
import com.hsyatgit.dreamscape.domain.SessionPath
import com.hsyatgit.dreamscape.domain.SuggestionSanitizer
import com.hsyatgit.dreamscape.domain.TemplateScriptGenerator
import com.hsyatgit.dreamscape.domain.WavSilence
import com.hsyatgit.dreamscape.provider.BodyTemplate
import com.hsyatgit.dreamscape.provider.GenericTtsExample
import com.hsyatgit.dreamscape.provider.OpenAiEndpoints
import com.hsyatgit.dreamscape.provider.PipelineStep
import com.hsyatgit.dreamscape.provider.ResponsePipeline
import com.hsyatgit.dreamscape.provider.SseChatParser
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DreamscapeTest {
    private val generator = TemplateScriptGenerator()

    @Test
    fun parserKeepsPauseDurationsAndPhases() {
        val segments = ScriptParser.parse(
            """
            [[phase:INDUCTION]]
            气从鼻子进来。
            [[pause:3000]]
            ……
            [[phase:TRANSITION]]
            就这样。
            """.trimIndent(),
        )
        assertEquals(PhaseId.INDUCTION, (segments[0] as ScriptSegment.Speech).phase)
        assertEquals(3000L, (segments[1] as ScriptSegment.Pause).durationMs)
        assertTrue(segments[2] is ScriptSegment.Pause)
        assertEquals(PhaseId.TRANSITION, segments.last().phase)
    }

    @Test
    fun longSentenceIsSplit() {
        val text = "雨丝顺着玻璃慢慢往下，一条又一条，你看着它们自己走，房间里刚刚好。"
        val pieces = SentenceSplitter.split(text, maxChars = 16)
        assertTrue(pieces.size > 1)
        assertTrue(pieces.all { SentenceSplitter.codePoints(it) <= 16 })
    }

    @Test
    fun sleepScriptDoesNotWakeAndKeepsOrder() {
        val script = generator.generate(Intake(path = SessionPath.SLEEP, durationMin = 20, seed = 7))
        val text = script.speechText()
        assertEquals(PhaseOrder.canonical, script.phasesInOrder())
        assertTrue(text.contains(SafetyLines.CONTROL))
        assertTrue(text.contains(SafetyLines.SCENE_SAFE))
        assertTrue(text.contains(SafetyLines.SLEEP_CLOSE))
        assertFalse(text.contains("睁开眼"))
        assertFalse(text.contains(SafetyLines.WAKE_ROOM))
        assertTrue(script.segments.filterIsInstance<ScriptSegment.Pause>().any { it.durationMs >= 3000 })
        assertTrue(ScriptRules.violations(script).isEmpty())
    }

    @Test
    fun hypnosisScriptEmerges() {
        val script = generator.generate(Intake(path = SessionPath.HYPNOSIS, durationMin = 10, seed = 3))
        assertTrue(script.speechText().contains(SafetyLines.WAKE_ROOM))
        assertFalse(script.speechText().contains(SafetyLines.SLEEP_CLOSE))
        assertEquals(PhaseOrder.canonical, script.phasesInOrder())
    }

    @Test
    fun groundingSkipsImagery() {
        val script = generator.generate(
            Intake(safety = SafetyAnswers(severePsychiatric = true), path = SessionPath.SLEEP, seed = 1),
        )
        assertEquals(SessionPath.GROUNDING, script.path)
        assertEquals(PhaseOrder.grounding, script.phasesInOrder())
        assertFalse(script.speechText().contains("楼梯"))
        assertFalse(script.speechText().contains("越来越深"))
    }

    @Test
    fun blockedSituationDoesNotStartImagery() {
        val script = generator.generate(Intake(safety = SafetyAnswers(drivingOrMachinery = true)))
        assertEquals(SessionPath.GROUNDING, script.path)
    }

    @Test
    fun sameSeedIsStableAndAnotherSeedCanDiffer() {
        val first = generator.generate(Intake(seed = 11, sceneChosen = false, durationMin = 30))
        val second = generator.generate(Intake(seed = 11, sceneChosen = false, durationMin = 30))
        val third = generator.generate(Intake(seed = 99, sceneChosen = false, durationMin = 30))
        assertEquals(first.speechText(), second.speechText())
        assertEquals(first.phasesInOrder(), third.phasesInOrder())
    }

    @Test
    fun suggestionsAreCleaned() {
        val cleaned = SuggestionSanitizer.clean(listOf("我不焦虑了", "我的失眠痊愈", "我在一点点放松"), sleepPath = true)
        assertEquals(listOf("你在一点点放松", "你在一点点放松"), cleaned)
    }

    @Test
    fun pipelineDecodesTheChatAudioShape() {
        val payload = Base64.getEncoder().encodeToString("hello-audio".toByteArray())
        val json = """{"choices":[{"message":{"audio":{"data":"$payload"}}}]}"""
        val steps = ResponsePipeline.parseSteps(GenericTtsExample.PIPELINE)
        val audio = ResponsePipeline.apply(json.toByteArray(), steps)
        assertEquals("hello-audio", audio.decodeToString())
        val body = BodyTemplate.render(GenericTtsExample.BODY, "他说\"你好\"", "冰糖", "secret")
        Json.parseToJsonElement(body)
        assertTrue(body.contains("他说\\\"你好\\\""))
        assertFalse(body.contains("secretsecret"))
    }

    @Test
    fun exportCanOmitSecrets() {
        val config = AppConfig(
            llm = LlmConfig(apiKey = "sk-test-SECRET-99"),
            tts = TtsConfig(apiKey = "sk-test-SECRET-99"),
        )
        val hidden = ConfigCodec.toJson(config, includeSecrets = false)
        assertFalse(hidden.contains("sk-test-SECRET-99"))
        val shown = ConfigCodec.toJson(config, includeSecrets = true)
        assertTrue(shown.contains("sk-test-SECRET-99"))
        assertEquals("", ConfigCodec.fromJson(hidden).llm.apiKey)
    }

    @Test
    fun sequencerWaitsAndDoesNotSkip() = runBlocking {
        val events = mutableListOf<String>()
        val sequencer = GuidanceSequencer(
            prefetchDepth = 2,
            synthesize = { text ->
                if (text == "慢") delay(80)
                text.encodeToByteArray()
            },
            play = { bytes, _ -> events += bytes.decodeToString() },
            silence = { ms -> events += "pause:$ms" },
        )
        sequencer.play(
            GuidanceScript(
                title = "t",
                path = SessionPath.SLEEP,
                source = "test",
                segments = listOf(
                    ScriptSegment.Speech(PhaseId.RELAXATION, "甲"),
                    ScriptSegment.Pause(PhaseId.RELAXATION, 500),
                    ScriptSegment.Speech(PhaseId.RELAXATION, "慢"),
                    ScriptSegment.Speech(PhaseId.TRANSITION, "乙"),
                ),
            ),
            fadeOut = false,
        )
        assertEquals(listOf("甲", "pause:500", "慢", "乙"), events)
    }

    @Test
    fun prefetchStaysBounded() = runBlocking {
        val started = AtomicInteger(0)
        val hold = CompletableDeferred<Unit>()
        val playing = CompletableDeferred<Unit>()
        val script = GuidanceScript(
            title = "t",
            path = SessionPath.SLEEP,
            source = "test",
            segments = (1..8).map { ScriptSegment.Speech(PhaseId.RELAXATION, "句$it") },
        )
        val sequencer = GuidanceSequencer(
            prefetchDepth = 3,
            synthesize = {
                started.incrementAndGet()
                it.encodeToByteArray()
            },
            play = { _, _ ->
                playing.complete(Unit)
                hold.await()
            },
            silence = {},
        )
        val job = launch { sequencer.play(script, fadeOut = false) }
        playing.await()
        withTimeout(2000) {
            while (started.get() < 4) delay(10)
        }
        delay(80)
        assertTrue("ahead=${started.get()}", started.get() <= 5)
        hold.complete(Unit)
        job.join()
        assertEquals(8, started.get())
    }

    @Test
    fun microphoneStaysClosedDuringPlayback() {
        CaptureGate.exitPlayback()
        CaptureGate.enterPlayback()
        try {
            assertFalse(CaptureGate.captureAllowed())
            var thrown = false
            try {
                CaptureGate.checkCapture()
            } catch (error: MicrophoneBlockedException) {
                thrown = true
            }
            assertTrue(thrown)
        } finally {
            CaptureGate.exitPlayback()
        }
    }

    @Test
    fun badModelOutputFallsBack() = runBlocking {
        val composer = ScriptComposer(complete = { "这不是合格的稿。" })
        val script = composer.compose(Intake(path = SessionPath.SLEEP, seed = 4))
        assertEquals("template", script.source)
        assertTrue(script.speechText().contains(SafetyLines.SLEEP_CLOSE))
    }

    @Test
    fun builtinRainIsOfflineSleepScript() {
        val script = BuiltinScripts.rainWindow()
        assertEquals("builtin", script.source)
        assertTrue(script.speechText().contains("檐上的雨"))
        assertTrue(script.speechText().contains(SafetyLines.CONTROL))
        assertTrue(script.speechText().contains(SafetyLines.SLEEP_CLOSE))
        assertFalse(script.speechText().contains("睁开眼"))
        assertTrue(BuiltinScripts.forestCabin().speechText().contains("炉火"))
    }

    @Test
    fun openAiPathsAndSse() {
        assertEquals(
            "https://api.example.com/v1/audio/speech",
            OpenAiEndpoints.speechUrl("https://api.example.com/v1/"),
        )
        val text = SseChatParser.collect(
            """
            data: {"choices":[{"delta":{"content":"夜"}}]}

            data: {"choices":[{"delta":{"content":"安"}}]}

            data: [DONE]
            """.trimIndent(),
        )
        assertEquals("夜安", text)
    }

    @Test
    fun silenceWavHasRequestedDuration() {
        val wav = WavSilence.pcm16(1000, sampleRate = 16000)
        assertEquals("RIFF", wav.copyOfRange(0, 4).decodeToString())
        assertEquals(44 + 16000 * 2, wav.size)
    }

    @Test
    fun rawPipelinePassesBytes() {
        val bytes = byteArrayOf(1, 2, 3)
        val out = ResponsePipeline.apply(bytes, listOf(PipelineStep("raw_bytes")))
        assertTrue(bytes.contentEquals(out))
    }
}
