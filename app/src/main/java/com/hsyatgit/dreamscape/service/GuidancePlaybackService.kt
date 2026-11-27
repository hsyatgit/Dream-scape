package com.hsyatgit.dreamscape.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.hsyatgit.dreamscape.DreamscapeApp
import com.hsyatgit.dreamscape.MainActivity
import com.hsyatgit.dreamscape.R
import com.hsyatgit.dreamscape.data.TtsConfig
import com.hsyatgit.dreamscape.domain.CaptureGate
import com.hsyatgit.dreamscape.domain.GuidanceSequencer
import com.hsyatgit.dreamscape.domain.ScriptSegment
import com.hsyatgit.dreamscape.domain.WavSilence
import com.hsyatgit.dreamscape.domain.zhLabel
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@UnstableApi
class GuidancePlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        createChannel()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true,
            )
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        mediaSession = MediaSession.Builder(this, player).build()
        SessionRuntime.controls = object : PlaybackControls {
            override fun pause() { player.pause() }
            override fun resume() { player.play() }
            override fun stop() { stopSession() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> player.pause()
            ACTION_RESUME -> player.play()
            ACTION_STOP -> stopSession()
            else -> startSession()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        CaptureGate.exitPlayback()
        SessionRuntime.controls = null
        job?.cancel()
        scope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun startSession() {
        val script = SessionRuntime.script
        if (script == null) {
            stopSelf()
            return
        }
        startAsForeground("正在准备引导")
        CaptureGate.enterPlayback()
        job?.cancel()
        job = scope.launch {
            val app = application as DreamscapeApp
            val config = app.graph.configRepository.load()
            val tts = app.graph.providers.tts(config.tts)
            try {
                var speechSeen = 0
                val sequencer = GuidanceSequencer(
                    prefetchDepth = config.playback.prefetchDepth,
                    synthesize = { text -> synthesizeWithRetry(text, tts, config.tts) },
                    play = { bytes, volume ->
                        speechSeen += 1
                        SessionRuntime.update(currentPhase, currentLine, speechSeen)
                        startAsForeground(currentLine)
                        playBytes(bytes, volume)
                    },
                    silence = { duration -> playBytes(WavSilence.pcm16(duration), player.volume) },
                    onSpeech = { phase, text ->
                        currentPhase = phase
                        currentLine = text
                    },
                    shouldContinue = { !SessionRuntime.stopRequested.get() },
                )
                sequencer.play(script, SessionRuntime.fadeOut && script.path == com.hsyatgit.dreamscape.domain.SessionPath.SLEEP)
                if (!SessionRuntime.stopRequested.get()) SessionRuntime.finish()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                SessionRuntime.fail(error.message ?: "播放中断")
            } finally {
                CaptureGate.exitPlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private var currentLine: String = ""
    private var currentPhase: com.hsyatgit.dreamscape.domain.PhaseId =
        com.hsyatgit.dreamscape.domain.PhaseId.PRE_TALK

    private suspend fun synthesizeWithRetry(
        text: String,
        tts: com.hsyatgit.dreamscape.provider.TtsProvider,
        config: TtsConfig,
    ): ByteArray = withContext(Dispatchers.IO) {
        try {
            tts.synthesize(text, config)
        } catch (first: Exception) {
            delay(400)
            tts.synthesize(text, config)
        }
    }

    private suspend fun playBytes(bytes: ByteArray, volume: Float) {
        val ext = if (bytes.size > 12 && bytes.copyOfRange(0, 4).decodeToString() == "RIFF") "wav" else "mp3"
        val file = File(cacheDir, "clip-${System.nanoTime()}.$ext")
        file.writeBytes(bytes)
        suspendCancellableCoroutine { cont ->
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED && cont.isActive) {
                        player.removeListener(this)
                        cont.resume(Unit)
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    player.removeListener(this)
                    if (cont.isActive) cont.resumeWithException(error)
                }
            }
            player.addListener(listener)
            player.volume = volume.coerceIn(0.05f, 1f)
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            player.prepare()
            player.play()
            cont.invokeOnCancellation {
                player.removeListener(listener)
                player.stop()
                file.delete()
            }
        }
        file.delete()
    }

    private fun stopSession() {
        SessionRuntime.stopRequested.set(true)
        player.stop()
        job?.cancel()
        CaptureGate.exitPlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startAsForeground(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(text: String): Notification {
        val content = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, GuidancePlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text.ifBlank { currentPhase.zhLabel() })
            .setContentIntent(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(NotificationCompat.Action(0, getString(R.string.action_stop), stop))
            .setStyle(MediaStyleNotificationHelper.MediaStyle(mediaSession))
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "引导播放", NotificationManager.IMPORTANCE_LOW)
        channel.description = "息屏时继续播放放松引导"
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_PAUSE = "com.hsyatgit.dreamscape.PAUSE"
        const val ACTION_RESUME = "com.hsyatgit.dreamscape.RESUME"
        const val ACTION_STOP = "com.hsyatgit.dreamscape.STOP"
        private const val CHANNEL_ID = "guidance_playback"
        private const val NOTIFICATION_ID = 42
    }
}
