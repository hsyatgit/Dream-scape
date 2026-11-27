package com.hsyatgit.dreamscape.ui

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hsyatgit.dreamscape.DreamscapeApp
import com.hsyatgit.dreamscape.data.AppConfig
import com.hsyatgit.dreamscape.data.ConfigCodec
import com.hsyatgit.dreamscape.domain.BuiltinScripts
import com.hsyatgit.dreamscape.domain.CaptureGate
import com.hsyatgit.dreamscape.domain.GuidanceScript
import com.hsyatgit.dreamscape.domain.Intake
import com.hsyatgit.dreamscape.domain.SafetyAnswers
import com.hsyatgit.dreamscape.domain.SceneId
import com.hsyatgit.dreamscape.domain.SessionPath
import com.hsyatgit.dreamscape.domain.StylePref
import com.hsyatgit.dreamscape.provider.MicRecorder
import com.hsyatgit.dreamscape.service.GuidancePlaybackService
import com.hsyatgit.dreamscape.service.SessionRuntime
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun DreamscapeNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("intake/{path}") { entry ->
            IntakeScreen(pathName = entry.arguments?.getString("path") ?: "sleep", nav = nav)
        }
        composable("session") { SessionScreen(nav) }
        composable("settings") { SettingsScreen(nav) }
    }
}

@OptIn(UnstableApi::class)
private fun startPlayback(context: android.content.Context, script: GuidanceScript) {
    val fade = (context.applicationContext as DreamscapeApp).graph.configRepository.load().playback.fadeOut
    SessionRuntime.prepare(script, fade)
    ContextCompat.startForegroundService(
        context,
        Intent(context, GuidancePlaybackService::class.java),
    )
}

@Composable
private fun HomeScreen(nav: NavHostController) {
    val context = LocalContext.current
    val pending = remember { mutableStateOf<() -> Unit>({}) }
    val gate = rememberStartGate { pending.value.invoke() }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("眠境", style = MaterialTheme.typography.headlineLarge)
            Text("语音引导的放松与助眠。整场先写成一篇稿，停顿是稿子里的静音。")
            Text(
                "这是放松辅助，不是医疗。开车、走路或操作机器时不要播放。",
                color = MaterialTheme.colorScheme.secondary,
            )
            Button(onClick = { nav.navigate("intake/sleep") }, modifier = Modifier.fillMaxWidth()) {
                Text("开始助眠")
            }
            Button(onClick = { nav.navigate("intake/hypnosis") }, modifier = Modifier.fillMaxWidth()) {
                Text("日间放松")
            }
            OutlinedButton(
                onClick = {
                    pending.value = {
                        startPlayback(context, BuiltinScripts.rainWindow())
                        nav.navigate("session")
                    }
                    gate()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("播放雨窗样稿") }
            OutlinedButton(
                onClick = {
                    pending.value = {
                        startPlayback(context, BuiltinScripts.forestCabin())
                        nav.navigate("session")
                    }
                    gate()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("播放林间样稿") }
            TextButton(onClick = { nav.navigate("settings") }) { Text("设置") }
        }
    }
}

@Composable
private fun rememberStartGate(onReady: () -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onReady()
    }
    return {
        if (Build.VERSION.SDK_INT >= 33) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onReady()
        }
    }
}

@Composable
private fun IntakeScreen(pathName: String, nav: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as DreamscapeApp
    val scope = rememberCoroutineScope()
    var driving by remember { mutableStateOf(false) }
    var psychiatric by remember { mutableStateOf(false) }
    var trauma by remember { mutableStateOf(false) }
    var substance by remember { mutableStateOf(false) }
    var chronic by remember { mutableStateOf(false) }
    var minutes by remember { mutableStateOf(20) }
    var scene by remember { mutableStateOf(SceneId.RAIN_WINDOW) }
    var style by remember { mutableStateOf(StylePref.MIXED) }
    var suggestions by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var block by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val path = if (pathName == "hypnosis") SessionPath.HYPNOSIS else SessionPath.SLEEP
    val blocked = driving || substance
    val ready = remember { mutableStateOf<() -> Unit>({}) }
    val gate = rememberStartGate { ready.value.invoke() }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (path == SessionPath.SLEEP) "助眠访谈" else "日间访谈", style = MaterialTheme.typography.headlineSmall)
            Text("播放开始后不再提问。安全问题先答。")
            ToggleRow("正在开车、走路或操作机器", driving) { driving = it }
            ToggleRow("有活动期的严重精神状况、强烈分离，或近期惊恐、自伤想法", psychiatric) { psychiatric = it }
            ToggleRow("放松或闭眼时容易闪回、强烈不安", trauma) { trauma = it }
            ToggleRow("现在受酒精或其他物质影响", substance) { substance = it }
            ToggleRow("失眠已超过一个月，并影响白天", chronic) { chronic = it }
            if (blocked) {
                Text("现在不开始。请先到可以躺下或坐好的地方，并等物质影响过去。")
            }
            if (psychiatric || trauma) {
                Text("这次只做一段可以睁眼的短呼吸，不做深层想象。若需要，请联系专业人员。")
            }
            if (chronic) {
                Text("可以听放松引导。持续失眠建议同时看医生，认知行为治疗是常用的一线非药物途径。")
            }
            Text("时长")
            RowChips(listOf(10 to "10 分钟", 20 to "20 分钟", 30 to "30 分钟"), minutes) { minutes = it }
            Text("场景")
            RowChips(
                listOf(
                    SceneId.RAIN_WINDOW to "雨窗",
                    SceneId.FOREST_CABIN to "林间",
                    SceneId.RIVER_NIGHT to "河边",
                    SceneId.OLD_BOOKSHELF to "书架",
                ),
                scene,
            ) { scene = it }
            Text("风格")
            RowChips(
                listOf(
                    StylePref.MIXED to "混合",
                    StylePref.PERMISSIVE to "允许",
                    StylePref.DIRECT to "直接",
                    StylePref.EXPLAIN to "多解释一点",
                ),
                style,
            ) { style = it }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("称呼，可以空着") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = suggestions,
                onValueChange = { suggestions = it },
                label = { Text("你自己的正向句子，一行一句") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = block,
                onValueChange = { block = it },
                label = { Text("不想听到的，例如水、黑暗、坠落") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (status.isNotEmpty()) Text(status)
            Button(
                onClick = {
                    if (blocked) {
                        status = "这次不播放。"
                    } else {
                        ready.value = {
                            scope.launch {
                                val intake = Intake(
                                    safety = SafetyAnswers(driving, psychiatric, trauma, substance, chronic),
                                    path = path,
                                    durationMin = minutes,
                                    sceneId = scene,
                                    sceneBlocklist = block.split('，', ',', ' ', '、').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
                                    style = style,
                                    suggestions = suggestions.split('\n', '；', ';').map { it.trim() }.filter { it.isNotEmpty() },
                                    name = name,
                                    seed = System.currentTimeMillis(),
                                    avoidFalling = block.contains("坠"),
                                )
                                val script = app.graph.composeScript(intake)
                                startPlayback(context, script)
                                nav.navigate("session")
                            }
                        }
                        gate()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !blocked,
            ) { Text(if (psychiatric || trauma) "开始短呼吸" else "生成并开始") }
            TextButton(onClick = { nav.popBackStack() }) { Text("返回") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SessionScreen(nav: NavHostController) {
    val state by SessionRuntime.state.collectAsState()
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(state.title.ifBlank { "引导" }, style = MaterialTheme.typography.titleLarge)
            Text(state.phase.ifBlank { "准备" }, color = MaterialTheme.colorScheme.secondary)
            Text(state.line.ifBlank { "稿子正在准备。" }, fontSize = 28.sp)
            if (state.total > 0) Text("${state.index} / ${state.total}")
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.finished) Text("播放结束。助眠场次不会把你数醒。")
            Button(onClick = { SessionRuntime.controls?.pause() }, modifier = Modifier.fillMaxWidth()) {
                Text("暂停")
            }
            Button(onClick = { SessionRuntime.controls?.resume() }, modifier = Modifier.fillMaxWidth()) {
                Text("继续")
            }
            OutlinedButton(onClick = {
                SessionRuntime.controls?.stop()
                nav.popBackStack("home", inclusive = false)
            }, modifier = Modifier.fillMaxWidth()) { Text("停止") }
        }
    }
}

@Composable
private fun SettingsScreen(nav: NavHostController, vm: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val config by vm.config.collectAsState()
    val message by vm.message.collectAsState()
    var includeSecrets by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(ConfigCodec.toJson(config, includeSecrets).toByteArray())
            }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
            vm.import(text)
        }
    }
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.testAsr() else vm.setMessage("没有麦克风权限，识别测试未开始。")
    }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("设置", style = MaterialTheme.typography.headlineSmall)
            Text("密钥只加密存在这台手机上。")
            Text("对话模型")
            RowChips(
                listOf("none" to "只用模板", "openai_compatible" to "OpenAI 兼容"),
                config.llm.providerId,
            ) { vm.update(config.copy(llm = config.llm.copy(providerId = it))) }
            OutlinedTextField(config.llm.baseUrl, { vm.update(config.copy(llm = config.llm.copy(baseUrl = it))) }, Modifier.fillMaxWidth(), label = { Text("基地址") })
            OutlinedTextField(config.llm.apiKey, { vm.update(config.copy(llm = config.llm.copy(apiKey = it))) }, Modifier.fillMaxWidth(), label = { Text("密钥") })
            OutlinedTextField(config.llm.model, { vm.update(config.copy(llm = config.llm.copy(model = it))) }, Modifier.fillMaxWidth(), label = { Text("模型") })
            ToggleRow("流式输出", config.llm.streaming) { vm.update(config.copy(llm = config.llm.copy(streaming = it))) }
            Button(onClick = { vm.testLlm() }) { Text("测试对话模型") }

            Text("语音合成")
            RowChips(
                listOf(
                    "android_system" to "系统语音",
                    "openai_compatible" to "OpenAI 兼容",
                    "generic_http" to "通用 HTTP",
                ),
                config.tts.providerId,
            ) { vm.update(config.copy(tts = config.tts.copy(providerId = it))) }
            OutlinedTextField(config.tts.baseUrl, { vm.update(config.copy(tts = config.tts.copy(baseUrl = it))) }, Modifier.fillMaxWidth(), label = { Text("合成基地址") })
            OutlinedTextField(config.tts.apiKey, { vm.update(config.copy(tts = config.tts.copy(apiKey = it))) }, Modifier.fillMaxWidth(), label = { Text("合成密钥") })
            OutlinedTextField(config.tts.model, { vm.update(config.copy(tts = config.tts.copy(model = it))) }, Modifier.fillMaxWidth(), label = { Text("合成模型") })
            OutlinedTextField(config.tts.voice, { vm.update(config.copy(tts = config.tts.copy(voice = it))) }, Modifier.fillMaxWidth(), label = { Text("音色") })
            if (config.tts.providerId == "generic_http") {
                OutlinedTextField(config.tts.httpMethod, { vm.update(config.copy(tts = config.tts.copy(httpMethod = it))) }, Modifier.fillMaxWidth(), label = { Text("方法") })
                OutlinedTextField(config.tts.httpUrl, { vm.update(config.copy(tts = config.tts.copy(httpUrl = it))) }, Modifier.fillMaxWidth(), label = { Text("URL") })
                OutlinedTextField(config.tts.headersJson, { vm.update(config.copy(tts = config.tts.copy(headersJson = it))) }, Modifier.fillMaxWidth(), label = { Text("请求头 JSON") })
                OutlinedTextField(config.tts.bodyTemplate, { vm.update(config.copy(tts = config.tts.copy(bodyTemplate = it))) }, Modifier.fillMaxWidth(), label = { Text("正文模板") })
                OutlinedTextField(config.tts.pipelineJson, { vm.update(config.copy(tts = config.tts.copy(pipelineJson = it))) }, Modifier.fillMaxWidth(), label = { Text("响应管道") })
                TextButton(onClick = { vm.update(config.withGenericTtsExample()) }) { Text("填入通用模板示例") }
            }
            Button(onClick = { vm.testTts() }) { Text("测试语音合成") }

            Text("语音识别")
            RowChips(
                listOf("android_speech" to "系统识别", "openai_compatible" to "OpenAI 兼容"),
                config.asr.providerId,
            ) { vm.update(config.copy(asr = config.asr.copy(providerId = it))) }
            OutlinedTextField(config.asr.baseUrl, { vm.update(config.copy(asr = config.asr.copy(baseUrl = it))) }, Modifier.fillMaxWidth(), label = { Text("识别基地址") })
            OutlinedTextField(config.asr.apiKey, { vm.update(config.copy(asr = config.asr.copy(apiKey = it))) }, Modifier.fillMaxWidth(), label = { Text("识别密钥") })
            OutlinedTextField(config.asr.model, { vm.update(config.copy(asr = config.asr.copy(model = it))) }, Modifier.fillMaxWidth(), label = { Text("识别模型") })
            Button(onClick = {
                if (!CaptureGate.captureAllowed()) {
                    vm.setMessage("播放进行中，不会打开麦克风。")
                } else {
                    mic.launch(Manifest.permission.RECORD_AUDIO)
                }
            }) { Text("测试语音识别") }

            ToggleRow("助眠收束淡出", config.playback.fadeOut) {
                vm.update(config.copy(playback = config.playback.copy(fadeOut = it)))
            }
            ToggleRow("导出时包含密钥", includeSecrets) { includeSecrets = it }
            Button(onClick = { vm.save(); export.launch("dreamscape-config.json") }) { Text("保存并导出") }
            OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/*")) }) { Text("导入") }
            if (message.isNotEmpty()) Text(message)
            TextButton(onClick = { nav.popBackStack() }) { Text("返回") }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> RowChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

class SettingsViewModel(app: android.app.Application) : AndroidViewModel(app) {
    private val graph = (app as DreamscapeApp).graph
    private val _config = MutableStateFlow(graph.configRepository.load())
    val config = _config.asStateFlow()
    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    fun update(next: AppConfig) {
        _config.value = next
        graph.configRepository.save(next)
    }

    fun save() {
        graph.configRepository.save(_config.value)
    }

    fun setMessage(text: String) {
        _message.value = text
    }

    fun import(raw: String) {
        runCatching { ConfigCodec.fromJson(raw) }
            .onSuccess {
                _config.value = it
                graph.configRepository.save(it)
                _message.value = "已导入。"
            }
            .onFailure { _message.value = "导入失败：${it.message}" }
    }

    fun testLlm() {
        viewModelScope.launch {
            val current = _config.value
            _message.value = try {
                if (current.llm.providerId != "openai_compatible" || current.llm.apiKey.isBlank()) {
                    "当前使用内置模板，未请求网络。"
                } else {
                    graph.providers.llm(current.llm).complete("只回复一个字：好", current.llm)
                }
            } catch (error: Exception) {
                error.message ?: "对话模型测试失败"
            }
        }
    }

    fun testTts() {
        viewModelScope.launch {
            val current = _config.value
            try {
                val bytes = graph.providers.tts(current.tts).synthesize("夜色在很外面。", current.tts)
                playPreview(bytes)
                _message.value = "已合成 ${bytes.size} 字节。"
            } catch (error: Exception) {
                _message.value = error.message ?: "语音合成测试失败"
            }
        }
    }

    fun testAsr() {
        viewModelScope.launch {
            val current = _config.value
            try {
                CaptureGate.checkCapture()
                val text = if (current.asr.providerId == "openai_compatible") {
                    val wav = MicRecorder.recordWav(getApplication(), 3000)
                    graph.providers.asr(current.asr).transcribeWav(wav, current.asr)
                } else {
                    graph.providers.systemAsr.listenOnce()
                }
                _message.value = text
            } catch (error: Exception) {
                _message.value = error.message ?: "识别测试失败"
            }
        }
    }

    private fun playPreview(bytes: ByteArray) {
        val ext = if (bytes.size > 4 && bytes.copyOfRange(0, 4).decodeToString() == "RIFF") "wav" else "mp3"
        val file = File(getApplication<DreamscapeApp>().cacheDir, "preview.$ext")
        file.writeBytes(bytes)
        val player = MediaPlayer()
        player.setDataSource(file.absolutePath)
        player.setOnCompletionListener { it.release() }
        player.prepare()
        player.start()
    }
}
