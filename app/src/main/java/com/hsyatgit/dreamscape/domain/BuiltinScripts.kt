package com.hsyatgit.dreamscape.domain

object BuiltinScripts {
    fun rainWindow(): GuidanceScript = fromProse(
        title = "雨窗样稿",
        prose = RAIN,
    )

    fun forestCabin(): GuidanceScript = fromProse(
        title = "林间木屋样稿",
        prose = FOREST,
    )

    private fun fromProse(title: String, prose: String): GuidanceScript {
        val segments = ScriptParser.parse(prose).toMutableList()
        val hasControl = segments.any { it is ScriptSegment.Speech && it.text.contains("掌控") }
        if (!hasControl) {
            segments.add(0, ScriptSegment.Pause(PhaseId.PRE_TALK, 700))
            segments.add(0, ScriptSegment.Speech(PhaseId.PRE_TALK, SafetyLines.CONTROL))
        }
        val hasSleep = segments.any { it is ScriptSegment.Speech && it.text == SafetyLines.SLEEP_CLOSE }
        if (!hasSleep) {
            segments += ScriptSegment.Speech(PhaseId.TRANSITION, SafetyLines.SLEEP_CLOSE)
            segments += ScriptSegment.Pause(PhaseId.TRANSITION, 3000)
        }
        return GuidanceScript(title, SessionPath.SLEEP, segments, source = "builtin")
    }

    private val RAIN = """
        [[phase:PRE_TALK]]
        今晚就听到这里。
        身体先靠好。
        [[phase:RELAXATION]]
        肩膀沉到垫子里。
        [[pause:2200]]
        手机放到一边，屏幕朝下。
        车钥匙先放桌上。
        就按你舒服的样子躺好。
        头放正，或稍稍偏向你习惯的一侧。
        枕头接着后脑。
        脚自己摊开。
        ……
        手也松开。
        掌心朝上，或轻轻搭在身上。
        下巴轻轻收一点。
        牙关先松开一点点。
        [[phase:CLEARING]]
        念头来了，就让它自己过去。
        像云从天边走过去。
        注意力留在这一口气上。
        [[phase:INDUCTION]]
        嗯，先听自己这一口气。
        气从鼻子进来。
        再慢慢出去。
        呼——
        [[pause:3000]]
        下一口，比刚才再软一点。
        进来的时候，胸口自己会开一点。
        出去的时候，肩膀再沉一寸。
        呼——
        [[pause:3000]]
        气走到肚子里，再自己出来。
        肚子跟着轻轻起，轻轻落。
        气走它自己的路。
        你只是跟着它走。
        眼睛想合，就合上。
        眼皮自己沉着。
        亮从眼皮外面退远。
        就这么合着。
        眼睛后面也软下来。
        眉心松开。
        手心朝上。
        手指一根一根地松开。
        腕子里的力，自己会走。
        前臂变重，沉在身侧。
        两只手就这么待着。
        [[phase:DEEPENING]]
        从额头开始。
        额头上的力，自己散开。
        像有一层温的东西，从发际慢慢往下。
        眉毛这么待着。
        眼角松开。
        脸颊沉下去一点点。
        牙关再松一些。
        舌头回到下面。
        下巴再松一寸。
        ……
        脖子后面，像被人轻轻托着。
        喉咙软着。
        双肩往下，再往下。
        左肩先沉。
        右肩跟着沉。
        两只手臂又重了一点。
        胸口随气起，随气落。
        每一口出去，胸口都更软一点。
        肚子软着。
        腰贴着身下的垫。
        后背被整张接住。
        大腿很重。
        膝盖解开。
        小腿往下坠。
        脚踝松开。
        脚趾一个一个地沉。
        从额头到脚趾，整个人像被床接住。
        再往下一点。
        重量是均匀的。
        你待着就行。
        [[phase:IMAGERY]]
        这里很安全，一切都安稳。
        外面是夜里。
        夜色在很外面，又宽又静。
        ……
        很远的地方，有一点安静的光。
        光很弱，停在很远的地方。
        ……
        檐上的雨，一声一声。
        雨打在檐上，又从檐边落下。
        ……
        雨丝打在窗上。
        顺着玻璃，慢慢往下。
        一条，又一条。
        你看着它们自己走。
        ……
        窗台是温的。
        房间里暗着，刚刚好。
        家具都退到暗里，各待各的。
        ……
        被子盖在身上，有一点沉。
        这点沉正好把你留住。
        ……
        胸口一起一落。
        气还在，雨还在。
        ……
        眼皮里面是柔软的暗。
        你就在这暗里待着。
        往里再走一点。
        雨还在很外面，你在里面。
        [[phase:SUGGESTION]]
        身体自己会松。
        它自己会。
        [[pause:1000]]
        睡眠自己会来。
        像雨自己会落下。
        [[pause:1000]]
        气还在走，你只是跟着。
        跟着就够了。
        [[phase:TRANSITION]]
        嗯。
        声音可以离你远一点。
        [[pause:3000]]
        雨还在很外面。
        房间还在。
        你继续往下。
        [[pause:5500]]
        就这么沉着。
        [[pause:8000]]
        再沉一点。
        [[pause:10500]]
        就这样。
        [[pause:13000]]
        耳朵放松地听，也可以直接睡去。
        [[pause:15500]]
        我在这儿，你睡。
        [[pause:18000]]
    """.trimIndent()

    private val FOREST = """
        [[phase:PRE_TALK]]
        这段话你一直都在掌控，想睁眼随时睁开，想停就停。
        今晚就听到这里。
        [[phase:RELAXATION]]
        身体先靠好。
        肩膀沉到垫子里。
        手机放到一边，屏幕朝下。
        车钥匙先放桌上。
        就按你舒服的样子躺好。
        脚自己摊开。
        手也松开。
        [[phase:CLEARING]]
        念头来了，就让它自己过去。
        注意力留在这一口气上。
        [[phase:INDUCTION]]
        嗯，先听自己这一口气。
        气从鼻子进来。
        再慢慢出去。
        呼——
        [[pause:3000]]
        眼睛想合，就合上。
        手心朝上。
        手指一根一根地松开。
        [[phase:DEEPENING]]
        从额头开始。
        双肩往下。
        胸口随气落。
        肚子软着。
        腿很重。
        整个人像被床接住。
        [[phase:IMAGERY]]
        这里很安全，一切都安稳。
        山在很外面。
        山脊是一条安静的线。
        ……
        林道从山下过来。
        土是湿的，脚步很轻。
        ……
        门廊到了。
        木头的气味，雨后的气味。
        门轻轻带上。
        ……
        屋里有炉火。
        火很小，够看，也够暖。
        火声一下一下。
        ……
        一把木椅对着火。
        你坐下，背靠着椅。
        ……
        毯子盖在腿上。
        毛线的重量是软的。
        ……
        杯壁是温的。
        温度停在掌心。
        ……
        气跟着那点温度走。
        进来，出去。
        ……
        火声还在，已经很远。
        剩下的是静。
        你待在这静里。
        [[phase:SUGGESTION]]
        身体自己会松。
        [[pause:1000]]
        睡眠自己会来。
        [[pause:1000]]
        气还在走，你只是跟着。
        [[phase:TRANSITION]]
        声音可以离你远一点。
        [[pause:3000]]
        火声还在很外面。
        [[pause:5500]]
        就这么沉着。
        [[pause:8000]]
        耳朵放松地听，也可以直接睡去。
        [[pause:10500]]
        我在这儿，你睡。
        [[pause:13000]]
    """.trimIndent()
}

fun PhaseId.zhLabel(): String = when (this) {
    PhaseId.PRE_TALK -> "前谈"
    PhaseId.RELAXATION -> "放松"
    PhaseId.CLEARING -> "清空"
    PhaseId.INDUCTION -> "导入"
    PhaseId.DEEPENING -> "加深"
    PhaseId.IMAGERY -> "场景"
    PhaseId.SUGGESTION -> "暗示"
    PhaseId.TRANSITION -> "收束"
}
