package com.hsyatgit.dreamscape.domain

object SentenceSplitter {
    fun split(text: String, maxChars: Int = 42): List<String> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return emptyList()
        if (codePoints(cleaned) <= maxChars) return listOf(cleaned)
        val rough = mutableListOf<String>()
        val buf = StringBuilder()
        fun flush() {
            val piece = buf.toString().trim().trim('，', '、', '；', ',', ' ')
            if (piece.isNotEmpty()) rough += piece
            buf.setLength(0)
        }
        var index = 0
        while (index < cleaned.length) {
            val cp = cleaned.codePointAt(index)
            buf.appendCodePoint(cp)
            val boundary = cp == '。'.code || cp == '，'.code || cp == '、'.code ||
                cp == '；'.code || cp == ','.code || cp == '.'.code
            val size = codePoints(buf)
            if ((boundary && size >= maxChars / 2) || size >= maxChars) flush()
            index += Character.charCount(cp)
        }
        flush()
        return rough.flatMap { hardSplit(it, maxChars) }
    }

    private fun hardSplit(text: String, maxChars: Int): List<String> {
        if (codePoints(text) <= maxChars) return listOf(text)
        val out = mutableListOf<String>()
        val buf = StringBuilder()
        var index = 0
        while (index < text.length) {
            val cp = text.codePointAt(index)
            buf.appendCodePoint(cp)
            if (codePoints(buf) >= maxChars) {
                out += buf.toString()
                buf.setLength(0)
            }
            index += Character.charCount(cp)
        }
        if (buf.isNotEmpty()) out += buf.toString()
        return out
    }

    fun codePoints(text: CharSequence): Int = Character.codePointCount(text, 0, text.length)
}

object ScriptParser {
    private val pause = Regex("""\[\[pause:\s*(\d{1,7})\s*]]""")
    private val phase = Regex("""\[\[phase:\s*([A-Z_]+)\s*]]""")
    private val ellipsis = Regex("""^[.。…．\s]+$""")

    fun parse(
        raw: String,
        ellipsisPauseMs: Long = 1400,
        maxChars: Int = 42,
    ): List<ScriptSegment> {
        var current = PhaseId.PRE_TALK
        val out = mutableListOf<ScriptSegment>()
        raw.replace("\r\n", "\n").lineSequence().forEach { original ->
            val line = original.trim()
            if (line.isEmpty()) return@forEach
            val phaseMatch = phase.matchEntire(line)
            if (phaseMatch != null) {
                val name = phaseMatch.groupValues[1]
                val resolved = runCatching { PhaseId.valueOf(name) }.getOrNull()
                if (resolved != null) current = resolved
                return@forEach
            }
            out += parseLine(line, current, ellipsisPauseMs, maxChars)
        }
        return out
    }

    private fun parseLine(
        line: String,
        phase: PhaseId,
        ellipsisPauseMs: Long,
        maxChars: Int,
    ): List<ScriptSegment> {
        if (ellipsis.matches(line)) {
            return listOf(ScriptSegment.Pause(phase, ellipsisPauseMs))
        }
        val out = mutableListOf<ScriptSegment>()
        var cursor = 0
        for (match in pause.findAll(line)) {
            val before = line.substring(cursor, match.range.first).trim()
            if (before.isNotEmpty()) out += speech(phase, before, maxChars, ellipsisPauseMs)
            out += ScriptSegment.Pause(phase, match.groupValues[1].toLong())
            cursor = match.range.last + 1
        }
        val tail = line.substring(cursor).trim()
        if (tail.isNotEmpty()) out += speech(phase, tail, maxChars, ellipsisPauseMs)
        return out
    }

    private fun speech(
        phase: PhaseId,
        text: String,
        maxChars: Int,
        ellipsisPauseMs: Long,
    ): List<ScriptSegment> {
        if (ellipsis.matches(text)) return listOf(ScriptSegment.Pause(phase, ellipsisPauseMs))
        return SentenceSplitter.split(text, maxChars).map { ScriptSegment.Speech(phase, it) }
    }
}

object SuggestionSanitizer {
    private val topicBan = listOf("自伤", "自杀", "不想活", "痊愈", "治愈", "药效", "诊断")
    private val bannedChars = listOf("不", "别", "没", "无", "未")
    private val sleepConflict = listOf("清醒", "警觉", "醒来")

    fun clean(lines: List<String>, sleepPath: Boolean): List<String> {
        val out = mutableListOf<String>()
        for (raw in lines) {
            val rewritten = rewrite(raw.trim()) ?: continue
            if (topicBan.any { rewritten.contains(it) }) continue
            if (sleepPath && sleepConflict.any { rewritten.contains(it) }) continue
            if (bannedChars.any { rewritten.contains(it) }) continue
            val clipped = takeCodePoints(rewritten, 15)
            if (clipped.isNotEmpty()) out += clipped
            if (out.size == 4) break
        }
        return out
    }

    private fun rewrite(raw: String): String? {
        if (raw.isEmpty()) return null
        var text = raw.replace(Regex("[？?！!。.]"), "").trim()
        if (text.contains("不焦虑") || text.contains("不紧张") || text.contains("睡不着")) {
            return "你在一点点放松"
        }
        if (text.startsWith("我")) text = "你" + text.removePrefix("我")
        return text
    }

    private fun takeCodePoints(text: String, max: Int): String {
        if (SentenceSplitter.codePoints(text) <= max) return text
        val buf = StringBuilder()
        var count = 0
        var index = 0
        while (index < text.length && count < max) {
            val cp = text.codePointAt(index)
            buf.appendCodePoint(cp)
            count++
            index += Character.charCount(cp)
        }
        return buf.toString()
    }
}

object ScriptRules {
    private val banned = listOf('不', '别', '没', '无', '未')
    private val allowedSafety = setOf(
        SafetyLines.CONTROL,
        SafetyLines.SCENE_SAFE,
        SafetyLines.SLEEP_CLOSE,
        SafetyLines.WAKE_ROOM,
        SafetyLines.KEYS,
    )

    fun violations(script: GuidanceScript): List<String> {
        val problems = mutableListOf<String>()
        script.segments.filterIsInstance<ScriptSegment.Speech>().forEach { line ->
            if (line.text in allowedSafety) return@forEach
            if (line.text.any { it == '？' || it == '?' || it == '！' || it == '!' }) {
                problems += "标点: ${line.text}"
            }
            if (line.text.any { it in banned }) problems += "否定: ${line.text}"
            if (line.text.contains('（') || line.text.contains('）') || line.text.contains('(')) {
                problems += "括号: ${line.text}"
            }
        }
        return problems
    }

    fun acceptable(script: GuidanceScript, intake: Intake): Boolean {
        if (script.segments.isEmpty()) return false
        val text = script.speechText()
        if (!text.contains(SafetyLines.CONTROL)) return false
        val expected = when (intake.path) {
            SessionPath.GROUNDING -> PhaseOrder.grounding
            else -> PhaseOrder.canonical
        }
        if (script.phasesInOrder() != expected) return false
        if (intake.path == SessionPath.SLEEP) {
            if (text.contains("睁开眼")) return false
            if (text.contains(SafetyLines.WAKE_ROOM)) return false
            if (!text.contains(SafetyLines.SLEEP_CLOSE)) return false
        }
        if (intake.path == SessionPath.HYPNOSIS && !text.contains(SafetyLines.WAKE_ROOM)) return false
        return violations(script).isEmpty()
    }
}

object Fade {
    fun volume(phase: PhaseId, indexInPhase: Int, countInPhase: Int, enabled: Boolean): Float {
        if (!enabled || phase != PhaseId.TRANSITION || countInPhase <= 1) return 1f
        val t = indexInPhase.toFloat() / (countInPhase - 1).toFloat()
        return 1f - 0.85f * t
    }
}
