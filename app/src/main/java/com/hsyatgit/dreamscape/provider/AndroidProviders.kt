package com.hsyatgit.dreamscape.provider

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.hsyatgit.dreamscape.data.AsrConfig
import com.hsyatgit.dreamscape.data.TtsConfig
import com.hsyatgit.dreamscape.domain.CaptureGate
import com.hsyatgit.dreamscape.domain.WavSilence
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class AndroidSystemTtsProvider(
    context: Context,
) : TtsProvider {
    override val id: String = "android_system"
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val ready = CompletableDeferred<Unit>()
    private var engine: TextToSpeech? = null

    init {
        engine = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.CHINESE
                ready.complete(Unit)
            } else if (!ready.isCompleted) {
                ready.completeExceptionally(IllegalStateException("系统语音不可用"))
            }
        }
    }

    override suspend fun synthesize(text: String, config: TtsConfig): ByteArray {
        ready.await()
        val speaker = engine ?: error("系统语音不可用")
        return mutex.withLock {
            val file = File(appContext.cacheDir, "tts-${UUID.randomUUID()}.wav")
            suspendCancellableCoroutine { cont ->
                speaker.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        if (cont.isActive) cont.resume(file.readBytes())
                        file.delete()
                    }

                    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) {
                        if (cont.isActive) cont.resumeWithException(IllegalStateException("系统语音合成失败"))
                        file.delete()
                    }
                })
                val result = speaker.synthesizeToFile(text, Bundle(), file, "dreamscape")
                if (result == TextToSpeech.ERROR && cont.isActive) {
                    cont.resumeWithException(IllegalStateException("系统语音合成失败"))
                }
            }
        }
    }

    fun shutdown() {
        engine?.shutdown()
        engine = null
    }
}

class AndroidSpeechRecognizerProvider(
    private val context: Context,
) : AsrProvider {
    override val id: String = "android_speech"

    override suspend fun transcribeWav(audio: ByteArray, config: AsrConfig): String =
        error("系统识别使用麦克风实时听写，不接收音频文件")

    suspend fun listenOnce(): String = withContext(Dispatchers.Main) {
        CaptureGate.checkCapture()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            error("这台设备没有可用的系统语音识别")
        }
        suspendCancellableCoroutine { cont ->
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    recognizer.destroy()
                    if (cont.isActive) cont.resume(text.ifBlank { "没有识别到内容" })
                }

                override fun onError(error: Int) {
                    recognizer.destroy()
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("识别失败（$error）"))
                }
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            }
            recognizer.startListening(intent)
            cont.invokeOnCancellation { recognizer.destroy() }
        }
    }
}

object MicRecorder {
    suspend fun recordWav(context: Context, milliseconds: Int = 3000): ByteArray = withContext(Dispatchers.IO) {
        CaptureGate.checkCapture()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            error("没有麦克风权限")
        }
        val sampleRate = 16000
        val min = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        require(min > 0) { "这台设备暂时不能录音" }
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            min * 2,
        )
        val pcm = ByteArray(sampleRate * 2 * milliseconds / 1000)
        recorder.startRecording()
        var offset = 0
        while (offset < pcm.size) {
            val read = recorder.read(pcm, offset, pcm.size - offset)
            if (read <= 0) break
            offset += read
        }
        recorder.stop()
        recorder.release()
        val header = WavSilence.pcm16(0, sampleRate).copyOf(44)
        val dataSize = offset
        fun writeInt(at: Int, value: Int) {
            header[at] = (value and 0xff).toByte()
            header[at + 1] = (value shr 8 and 0xff).toByte()
            header[at + 2] = (value shr 16 and 0xff).toByte()
            header[at + 3] = (value shr 24 and 0xff).toByte()
        }
        writeInt(4, 36 + dataSize)
        writeInt(40, dataSize)
        header + pcm.copyOf(offset)
    }
}
