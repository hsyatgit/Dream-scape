package com.hsyatgit.dreamscape.domain

enum class PhaseId {
    PRE_TALK,
    RELAXATION,
    CLEARING,
    INDUCTION,
    DEEPENING,
    IMAGERY,
    SUGGESTION,
    TRANSITION,
}

enum class SessionPath { SLEEP, HYPNOSIS, GROUNDING }

enum class SceneId { RAIN_WINDOW, FOREST_CABIN, RIVER_NIGHT, OLD_BOOKSHELF }

enum class StylePref { PERMISSIVE, DIRECT, EXPLAIN, MIXED }

enum class DeepenerKind { BODY_SCAN, STAIRS, COUNT }

enum class SensoryLead { VISUAL, AUDITORY, BODY, MIXED }

data class SafetyAnswers(
    val drivingOrMachinery: Boolean = false,
    val severePsychiatric: Boolean = false,
    val traumaFlashback: Boolean = false,
    val substances: Boolean = false,
    val chronicInsomnia: Boolean = false,
) {
    val blocked: Boolean get() = drivingOrMachinery || substances
    val groundingOnly: Boolean get() = !blocked && (severePsychiatric || traumaFlashback)
}

data class Intake(
    val safety: SafetyAnswers = SafetyAnswers(),
    val path: SessionPath = SessionPath.SLEEP,
    val durationMin: Int = 20,
    val sceneId: SceneId = SceneId.RAIN_WINDOW,
    val sceneChosen: Boolean = true,
    val customSceneNote: String = "",
    val sceneBlocklist: Set<String> = emptySet(),
    val style: StylePref = StylePref.MIXED,
    val sensory: SensoryLead = SensoryLead.MIXED,
    val suggestions: List<String> = emptyList(),
    val name: String = "",
    val seed: Long = 1L,
    val recentSceneIds: List<SceneId> = emptyList(),
    val deepenerPreference: DeepenerKind? = null,
    val avoidFalling: Boolean = false,
)

sealed class ScriptSegment {
    abstract val phase: PhaseId

    data class Speech(
        override val phase: PhaseId,
        val text: String,
    ) : ScriptSegment()

    data class Pause(
        override val phase: PhaseId,
        val durationMs: Long,
    ) : ScriptSegment()
}

data class GuidanceScript(
    val title: String,
    val path: SessionPath,
    val segments: List<ScriptSegment>,
    val source: String,
) {
    fun speechText(): String = segments
        .filterIsInstance<ScriptSegment.Speech>()
        .joinToString("\n") { it.text }

    fun phasesInOrder(): List<PhaseId> = segments.map { it.phase }.distinct()
}

object PhaseOrder {
    val canonical: List<PhaseId> = listOf(
        PhaseId.PRE_TALK,
        PhaseId.RELAXATION,
        PhaseId.CLEARING,
        PhaseId.INDUCTION,
        PhaseId.DEEPENING,
        PhaseId.IMAGERY,
        PhaseId.SUGGESTION,
        PhaseId.TRANSITION,
    )

    val grounding: List<PhaseId> = listOf(
        PhaseId.PRE_TALK,
        PhaseId.RELAXATION,
        PhaseId.CLEARING,
        PhaseId.TRANSITION,
    )
}

object SafetyLines {
    const val CONTROL = "这段话你一直都在掌控，想睁眼随时睁开，想停就停。"
    const val SCENE_SAFE = "这里很安全，一切都安稳。"
    const val SLEEP_CLOSE = "耳朵放松地听，也可以直接睡去。"
    const val WAKE_ROOM = "睁开眼睛，就回到这个房间。"
    const val KEYS = "车钥匙先放桌上。"
}
