package com.hsyatgit.dreamscape.domain

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MicrophoneBlockedException : IllegalStateException("播放进行中，不会打开麦克风")

object CaptureGate {
    private val playbackActive = AtomicBoolean(false)

    fun enterPlayback() {
        playbackActive.set(true)
    }

    fun exitPlayback() {
        playbackActive.set(false)
    }

    fun captureAllowed(): Boolean = !playbackActive.get()

    fun checkCapture() {
        if (playbackActive.get()) throw MicrophoneBlockedException()
    }
}

class GuidanceSequencer(
    private val prefetchDepth: Int = 3,
    private val synthesize: suspend (String) -> ByteArray,
    private val play: suspend (ByteArray, Float) -> Unit,
    private val silence: suspend (Long) -> Unit,
    private val onSpeech: (PhaseId, String) -> Unit = { _, _ -> },
    private val shouldContinue: () -> Boolean = { true },
) {
    suspend fun play(script: GuidanceScript, fadeOut: Boolean) = coroutineScope {
        val depth = prefetchDepth.coerceIn(2, 3)
        val channel = Channel<Prepared>(capacity = depth)
        val producer = launch {
            try {
                for (segment in script.segments) {
                    if (!shouldContinue()) break
                    when (segment) {
                        is ScriptSegment.Pause -> channel.send(Prepared.Rest(segment.durationMs, segment.phase))
                        is ScriptSegment.Speech -> {
                            val audio = synthesize(segment.text)
                            channel.send(Prepared.Clip(audio, segment.phase, segment.text))
                        }
                    }
                }
            } finally {
                channel.close()
            }
        }
        val speechByPhase = script.segments.filterIsInstance<ScriptSegment.Speech>().groupingBy { it.phase }.eachCount()
        val seenInPhase = mutableMapOf<PhaseId, Int>()
        try {
            for (item in channel) {
                if (!shouldContinue()) break
                when (item) {
                    is Prepared.Rest -> silence(item.durationMs)
                    is Prepared.Clip -> {
                        val index = seenInPhase.getOrDefault(item.phase, 0)
                        val count = speechByPhase[item.phase] ?: 1
                        val volume = Fade.volume(item.phase, index, count, fadeOut)
                        seenInPhase[item.phase] = index + 1
                        onSpeech(item.phase, item.text)
                        play(item.audio, volume)
                    }
                }
            }
        } finally {
            producer.cancel()
        }
    }

    private sealed class Prepared {
        data class Clip(val audio: ByteArray, val phase: PhaseId, val text: String) : Prepared()
        data class Rest(val durationMs: Long, val phase: PhaseId) : Prepared()
    }
}

object WavSilence {
    fun pcm16(durationMs: Long, sampleRate: Int = 16000): ByteArray {
        val samples = ((sampleRate * durationMs) / 1000L).toInt().coerceAtLeast(1)
        val dataSize = samples * 2
        val out = ByteArray(44 + dataSize)
        fun writeStr(offset: Int, value: String) {
            value.encodeToByteArray().copyInto(out, offset)
        }
        fun writeInt(offset: Int, value: Int) {
            out[offset] = (value and 0xff).toByte()
            out[offset + 1] = ((value shr 8) and 0xff).toByte()
            out[offset + 2] = ((value shr 16) and 0xff).toByte()
            out[offset + 3] = ((value shr 24) and 0xff).toByte()
        }
        fun writeShort(offset: Int, value: Int) {
            out[offset] = (value and 0xff).toByte()
            out[offset + 1] = ((value shr 8) and 0xff).toByte()
        }
        writeStr(0, "RIFF")
        writeInt(4, 36 + dataSize)
        writeStr(8, "WAVE")
        writeStr(12, "fmt ")
        writeInt(16, 16)
        writeShort(20, 1)
        writeShort(22, 1)
        writeInt(24, sampleRate)
        writeInt(28, sampleRate * 2)
        writeShort(32, 2)
        writeShort(34, 16)
        writeStr(36, "data")
        writeInt(40, dataSize)
        return out
    }
}
