package com.hsyatgit.dreamscape.service

import com.hsyatgit.dreamscape.domain.GuidanceScript
import com.hsyatgit.dreamscape.domain.PhaseId
import com.hsyatgit.dreamscape.domain.zhLabel
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackUiState(
    val running: Boolean = false,
    val title: String = "",
    val phase: String = "",
    val line: String = "",
    val index: Int = 0,
    val total: Int = 0,
    val error: String? = null,
    val finished: Boolean = false,
)

interface PlaybackControls {
    fun pause()
    fun resume()
    fun stop()
}

object SessionRuntime {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    @Volatile var script: GuidanceScript? = null
    @Volatile var fadeOut: Boolean = true
    val stopRequested = AtomicBoolean(false)

    @Volatile var controls: PlaybackControls? = null

    fun prepare(script: GuidanceScript, fadeOut: Boolean) {
        this.script = script
        this.fadeOut = fadeOut
        stopRequested.set(false)
        _state.value = PlaybackUiState(
            running = true,
            title = script.title,
            total = script.segments.count { it is com.hsyatgit.dreamscape.domain.ScriptSegment.Speech },
        )
    }

    fun update(phase: PhaseId, line: String, index: Int) {
        _state.value = _state.value.copy(
            running = true,
            phase = phase.zhLabel(),
            line = line,
            index = index,
            error = null,
            finished = false,
        )
    }

    fun finish() {
        _state.value = _state.value.copy(running = false, finished = true, line = "这一段到这里。")
    }

    fun fail(message: String) {
        _state.value = _state.value.copy(running = false, finished = true, error = message)
    }
}
