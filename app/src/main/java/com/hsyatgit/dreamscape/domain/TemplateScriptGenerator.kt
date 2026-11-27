package com.hsyatgit.dreamscape.domain

import kotlin.random.Random

class TemplateScriptGenerator {
    fun generate(intake: Intake): GuidanceScript {
        val path = when {
            intake.safety.blocked -> SessionPath.GROUNDING
            intake.safety.groundingOnly -> SessionPath.GROUNDING
            else -> intake.path
        }
        val random = Random(intake.seed)
        val segments = if (path == SessionPath.GROUNDING) {
            grounding(intake)
        } else {
            full(intake, path, random)
        }
        val title = when (path) {
            SessionPath.SLEEP -> "助眠"
            SessionPath.HYPNOSIS -> "日间放松"
            SessionPath.GROUNDING -> "呼吸着陆"
        }
        return GuidanceScript(title, path, segments, source = "template")
    }

    private fun full(intake: Intake, path: SessionPath, random: Random): List<ScriptSegment> {
        val rich = when {
            intake.durationMin <= 10 -> 1
            intake.durationMin >= 30 -> 3
            else -> 2
        }
        val out = mutableListOf<ScriptSegment>()
        fun say(phase: PhaseId, text: String, pauseMs: Long) {
            SentenceSplitter.split(text).forEach { piece ->
                out += ScriptSegment.Speech(phase, piece)
            }
            if (pauseMs > 0) out += ScriptSegment.Pause(phase, pauseMs)
        }

        val name = cleanName(intake.name)
        if (name.isNotEmpty()) say(PhaseId.PRE_TALK, "嗯，$name。", 400)
        say(
            PhaseId.PRE_TALK,
            if (path == SessionPath.SLEEP) "今晚就听到这里。" else "这一段就从这里开始。",
            500,
        )
        say(PhaseId.PRE_TALK, SafetyLines.CONTROL, 700)
        if (intake.style == StylePref.EXPLAIN || intake.style == StylePref.MIXED) {
            say(PhaseId.PRE_TALK, "这段声音只负责说话，放松是你自己在做。", 700)
        }

        say(PhaseId.RELAXATION, "身体先靠好。", 700)
        say(PhaseId.RELAXATION, "肩膀沉到垫子里。", 2200)
        say(PhaseId.RELAXATION, "手机放到一边，屏幕朝下。", 700)
        say(PhaseId.RELAXATION, SafetyLines.KEYS, 700)
        say(PhaseId.RELAXATION, "就按你舒服的样子躺好。", 2200)
        say(PhaseId.RELAXATION, "头放正，或稍稍偏向你习惯的一侧。", 700)
        say(PhaseId.RELAXATION, "脚自己摊开。", 700)
        say(PhaseId.RELAXATION, "手也松开。", 700)

        say(PhaseId.CLEARING, "念头来了，就让它自己过去。", 900)
        say(PhaseId.CLEARING, "像云从天边走过去。", 900)
        say(PhaseId.CLEARING, "注意力留在这一口气上。", 900)

        val breath = listOf(
            listOf("嗯，先听自己这一口气。", "气从鼻子进来。", "再慢慢出去。", "呼——"),
            listOf("下一口，比刚才再软一点。", "进来的时候，胸口自己会开一点。", "出去的时候，肩膀再沉一寸。", "呼——"),
            listOf("气走到肚子里，再自己出来。", "肚子跟着轻轻起，轻轻落。", "呼——"),
        )
        repeat(rich.coerceAtMost(breath.size)) { index ->
            val set = breath[(random.nextInt(breath.size) + index) % breath.size]
            set.forEach { line ->
                val pause = if (line.startsWith("呼")) 3000L else 700L
                say(PhaseId.INDUCTION, line, pause)
            }
        }
        val eyes = listOf(
            "眼睛想合，就合上。",
            "眼皮自己沉着。",
            "亮从眼皮外面退远。",
        )
        eyes.forEach { say(PhaseId.INDUCTION, it, 700) }
        if (intake.style == StylePref.PERMISSIVE || intake.style == StylePref.MIXED) {
            say(PhaseId.INDUCTION, "合着也行，微微留一条缝也行，随你。", 700)
        }
        say(PhaseId.INDUCTION, "手心朝上。", 700)
        say(PhaseId.INDUCTION, "手指一根一根地松开。", 700)
        say(PhaseId.INDUCTION, "腕子里的力，自己会走。", 2200)

        val deepener = chooseDeepener(intake, random)
        when (deepener) {
            DeepenerKind.BODY_SCAN -> bodyScan(rich, ::say)
            DeepenerKind.STAIRS -> stairs(intake.durationMin, ::say)
            DeepenerKind.COUNT -> countdown(intake.durationMin, random, ::say)
        }

        say(PhaseId.IMAGERY, SafetyLines.SCENE_SAFE, 900)
        val scene = resolveScene(intake, random)
        sceneLines(scene, rich, intake.sensory, random).forEach { line ->
            say(PhaseId.IMAGERY, line, 900)
        }
        val custom = cleanCustom(intake.customSceneNote)
        if (custom != null && intake.sceneId == SceneId.RAIN_WINDOW && intake.customSceneNote.isNotBlank()) {
            say(PhaseId.IMAGERY, custom, 900)
        }

        val cores = listOf(
            "身体自己会松。",
            "睡眠自己会来。",
            "气还在走，你只是跟着。",
            "越来越安稳。",
            "肩膀自己会沉。",
        )
        val groups = when {
            intake.durationMin <= 10 -> 2
            intake.durationMin >= 30 -> 5
            else -> 4
        }
        val userLines = SuggestionSanitizer.clean(intake.suggestions, path == SessionPath.SLEEP).take(2)
        repeat(groups) { index ->
            val line = if (index < userLines.size) userLines[index] else cores[(random.nextInt(cores.size) + index) % cores.size]
            say(PhaseId.SUGGESTION, line, 1000)
        }
        if (path == SessionPath.HYPNOSIS) {
            say(PhaseId.SUGGESTION, "你有办法把这一程慢慢走完。", 1000)
            say(PhaseId.SUGGESTION, "以后只要肩膀这么一沉，这层松会自己回来一点。", 1000)
        }

        if (path == SessionPath.SLEEP) {
            var ramp = 3000L
            listOf(
                "声音可以离你远一点。",
                "就这么沉着。",
                "再沉一点。",
                "就这样。",
                SafetyLines.SLEEP_CLOSE,
                "我在这儿，你睡。",
            ).forEach { line ->
                say(PhaseId.TRANSITION, line, ramp)
                ramp = (ramp + 2500L).coerceAtMost(18000L)
            }
        } else {
            listOf(
                "嗯，声音再近一点。",
                "气走得清楚一些。",
                "手指先动一动。",
                "脚也回到这里。",
                SafetyLines.WAKE_ROOM,
                "你回来了，身体是轻的，头是清的。",
            ).forEach { say(PhaseId.TRANSITION, it, 400) }
        }
        return out
    }

    private fun grounding(intake: Intake): List<ScriptSegment> {
        val out = mutableListOf<ScriptSegment>()
        fun say(phase: PhaseId, text: String, pauseMs: Long) {
            out += ScriptSegment.Speech(phase, text)
            out += ScriptSegment.Pause(phase, pauseMs)
        }
        say(PhaseId.PRE_TALK, SafetyLines.CONTROL, 700)
        say(PhaseId.PRE_TALK, "这一小段可以睁着眼听。", 700)
        say(PhaseId.RELAXATION, "脚放在地上，或摊在床上。", 2200)
        say(PhaseId.RELAXATION, "手心贴着腿。", 700)
        say(PhaseId.CLEARING, "注意力留在这一口气上。", 800)
        say(PhaseId.CLEARING, "气从鼻子进来。", 3000)
        say(PhaseId.CLEARING, "再慢慢出去。", 700)
        say(PhaseId.CLEARING, "呼——", 3000)
        say(PhaseId.TRANSITION, "房间还在。", 700)
        say(PhaseId.TRANSITION, "今天先到这里。", 700)
        if (intake.safety.chronicInsomnia) {
            say(PhaseId.TRANSITION, "想休息，就保持这个呼吸。", 700)
        }
        return out
    }

    private fun bodyScan(rich: Int, say: (PhaseId, String, Long) -> Unit) {
        val lines = listOf(
            "从额头开始。",
            "额头上的力，自己散开。",
            "脸颊沉下去一点点。",
            "牙关松开。",
            "双肩往下。",
            "胸口随气起，随气落。",
            "肚子软着。",
            "大腿很重。",
            "膝盖解开。",
            "脚趾一个一个地沉。",
            "整个人像被床接住。",
        )
        val count = if (rich == 1) 6 else lines.size
        lines.take(count).forEach { say(PhaseId.DEEPENING, it, 800) }
    }

    private fun stairs(duration: Int, say: (PhaseId, String, Long) -> Unit) {
        val steps = when {
            duration <= 10 -> 6
            duration >= 30 -> 12
            else -> 10
        }
        say(PhaseId.DEEPENING, "脚边有一段楼梯，往下。", 800)
        for (n in 1..steps) {
            val word = chineseNumber(n)
            say(PhaseId.DEEPENING, "$word 级，往下。", 800)
        }
        say(PhaseId.DEEPENING, "到了一个平台。平台是平的。", 800)
    }

    private fun countdown(duration: Int, random: Random, say: (PhaseId, String, Long) -> Unit) {
        val start = when {
            duration <= 10 -> listOf(5, 8)
            duration >= 30 -> listOf(15, 12)
            else -> listOf(10, 8)
        }.let { it[random.nextInt(it.size)] }
        say(PhaseId.DEEPENING, "我从${chineseNumber(start)}开始，往下。", 800)
        for (n in start downTo 1) {
            say(PhaseId.DEEPENING, "${chineseNumber(n)}……", 800)
        }
        say(PhaseId.DEEPENING, "就停在这儿。", 800)
    }

    private fun chooseDeepener(intake: Intake, random: Random): DeepenerKind {
        if (intake.avoidFalling || intake.sceneBlocklist.any { it.contains("坠") }) {
            return DeepenerKind.BODY_SCAN
        }
        intake.deepenerPreference?.let { return it }
        return DeepenerKind.entries[random.nextInt(DeepenerKind.entries.size)]
    }

    private fun resolveScene(intake: Intake, random: Random): SceneId {
        fun allowed(id: SceneId): Boolean {
            val block = intake.sceneBlocklist
            return when (id) {
                SceneId.RIVER_NIGHT -> block.none { it.contains("水") || it.contains("河") }
                SceneId.RAIN_WINDOW -> block.none { it.contains("雨") || it.contains("黑暗") }
                SceneId.FOREST_CABIN -> block.none { it.contains("林") }
                SceneId.OLD_BOOKSHELF -> true
            }
        }
        if (intake.sceneChosen && allowed(intake.sceneId)) return intake.sceneId
        val pool = SceneId.entries.filter { allowed(it) && it !in intake.recentSceneIds }
            .ifEmpty { SceneId.entries.filter { allowed(it) } }
        if (pool.isEmpty()) return SceneId.OLD_BOOKSHELF
        return pool[random.nextInt(pool.size)]
    }

    private fun sceneLines(scene: SceneId, rich: Int, sensory: SensoryLead, random: Random): List<String> {
        val layers = when (scene) {
            SceneId.RAIN_WINDOW -> rain
            SceneId.FOREST_CABIN -> forest
            SceneId.RIVER_NIGHT -> river
            SceneId.OLD_BOOKSHELF -> shelf
        }
        val take = if (rich == 1) layers.take(6) else layers
        return take.flatMap { layer ->
            val primary = layer[random.nextInt(layer.size)]
            val extra = if (rich >= 3 && layer.size > 1) layer.first { it != primary } else null
            val lead = when (sensory) {
                SensoryLead.AUDITORY -> "声音留在这一层。"
                SensoryLead.BODY -> "身体感觉留在这一层。"
                SensoryLead.VISUAL -> "画面留在这一层。"
                SensoryLead.MIXED -> null
            }
            listOfNotNull(primary, extra, if (rich >= 2) lead else null)
        }
    }

    private val rain = listOf(
        listOf("夜色在很外面。", "夜色在很外面，又宽又静。"),
        listOf("很远的地方，有一点安静的光。"),
        listOf("檐上的雨，一声一声。"),
        listOf("雨丝顺着玻璃，慢慢往下。"),
        listOf("窗台是温的。"),
        listOf("房间里暗着，刚刚好。"),
        listOf("被子盖在身上，有一点沉。"),
        listOf("胸口一起一落。"),
        listOf("眼皮里面是柔软的暗。"),
    )

    private val forest = listOf(
        listOf("山脊是一条安静的线。"),
        listOf("林道上的土是湿的。"),
        listOf("门廊到了，门轻轻带上。"),
        listOf("屋里有一小炉火，火声一下一下。"),
        listOf("一把木椅对着火。"),
        listOf("毯子盖在腿上，重量是软的。"),
        listOf("杯壁是温的，温度停在掌心。"),
        listOf("气跟着那点温度走。"),
        listOf("火声还在，剩下的是静。"),
    )

    private val river = listOf(
        listOf("夜里的天，颜色是均匀的。"),
        listOf("对岸有很远的灯，灯停在原处。"),
        listOf("河面在脚的前方，水走得很慢。"),
        listOf("坡上的草贴着地。"),
        listOf("一块石头又稳又干，把你接住。"),
        listOf("鞋边是细土，凉的，也安静。"),
        listOf("手心放在膝上，手指松开。"),
        listOf("气从胸口走一个来回，很慢。"),
        listOf("水声把其余的声音盖住。"),
    )

    private val shelf = listOf(
        listOf("街很安静，路自己在。"),
        listOf("推门的时候，有一声很轻的响。"),
        listOf("书架之间的通道刚刚好。"),
        listOf("架子一层一层，书脊靠在一起。"),
        listOf("通道尽头有一张桌。"),
        listOf("一本书搁在手上，封面是软的。"),
        listOf("纸页自己打开。"),
        listOf("字迹慢慢淡下去。"),
        listOf("淡完以后，是干净的暗。"),
    )

    private fun cleanName(name: String): String =
        name.trim().filter { it.isLetter() || it.code in 0x4E00..0x9FFF }.take(12)

    private fun cleanCustom(note: String): String? {
        val text = note.trim().replace(Regex("[？?！!（）()]"), "")
        if (text.isEmpty()) return null
        val banned = listOf('不', '别', '没', '无', '未')
        if (text.any { it in banned }) return null
        if (text.contains("坠") || text.contains("溺") || text.contains("医院")) return null
        return SentenceSplitter.split(text).firstOrNull()?.let { "你留在$it" }
    }

    private fun chineseNumber(n: Int): String {
        val digits = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        if (n <= 10) return if (n == 10) "十" else digits[n]
        if (n < 20) return "十" + digits[n - 10]
        return n.toString()
    }
}

class ScriptComposer(
    private val templates: TemplateScriptGenerator = TemplateScriptGenerator(),
    private val complete: (suspend (String) -> String)? = null,
) {
    suspend fun compose(intake: Intake): GuidanceScript {
        val completePrompt = complete
        if (intake.safety.blocked || intake.safety.groundingOnly || completePrompt == null) {
            return templates.generate(intake)
        }
        val drafted = runCatching { completePrompt.invoke(PromptTemplate.render(intake)) }.getOrNull()
        if (drafted != null) {
            val script = GuidanceScript(
                title = "引导",
                path = intake.path,
                segments = ScriptParser.parse(drafted),
                source = "llm",
            )
            if (ScriptRules.acceptable(script, intake)) return script
        }
        return templates.generate(intake)
    }
}

object PromptTemplate {
    fun render(intake: Intake): String = buildString {
        appendLine("你是助眠与放松引导的写稿人。只输出可朗读的中文正文和标记。")
        appendLine("标记格式：[[phase:PHASE]] 与 [[pause:毫秒]]。阶段只能是 PRE_TALK RELAXATION CLEARING INDUCTION DEEPENING IMAGERY SUGGESTION TRANSITION，按这个顺序各出现一次。")
        appendLine("必须包含这句：${SafetyLines.CONTROL}")
        appendLine("必须包含这句：${SafetyLines.SCENE_SAFE}")
        if (intake.path == SessionPath.SLEEP) {
            appendLine("这是助眠。收束必须包含：${SafetyLines.SLEEP_CLOSE}")
            appendLine("禁止出现睁开眼睛、回到这个房间、把人唤醒的倒数。")
        } else {
            appendLine("这是日间。收束必须包含：${SafetyLines.WAKE_ROOM}")
        }
        appendLine("除安全句外，不用「不 别 没 无 未」，不用问号和感叹号，不用括号。")
        appendLine("不要宣布对方已经睡着。")
        appendLine("时长约 ${intake.durationMin} 分钟。场景 ${intake.sceneId}。风格 ${intake.style}。")
        val lines = SuggestionSanitizer.clean(intake.suggestions, intake.path == SessionPath.SLEEP)
        if (lines.isNotEmpty()) appendLine("可使用这些正向句子：${lines.joinToString("；")}")
        appendLine("称呼：${intake.name.ifBlank { "无称呼" }}")
    }
}
