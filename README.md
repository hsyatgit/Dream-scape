# 眠境 Dreamscape

手机上很多催眠和助眠，是把稿子拆成一行一行来念：发出一句，就按一个固定的等待去接下一句。这个等待从「文字发出去」开始算，不是从「这一句真正说完」开始算。语音合成一旦变慢，下一句的呼吸、下落或画面就会叠到上一句还没说完的时候，人刚要沉进去，就被打断。

眠境是一个语音引导的催眠与助眠 Android 应用。它先把一整场引导写成一篇连续的稿，停顿写在稿子里，再按稿子里的时长播放成真正的静音。这些间隔是按写完的整稿排好的，不跟每一句合成要等多久绑在一起。正式引导开始之前，会先做一次会话前的互动访谈，记下你自己的正向暗示，以及你想停留的放松场景。

这是放松与助眠辅助，不是医疗，也不能代替诊疗。

## 功能

- **助眠**与**日间催眠**两条完整流程。助眠把人送进睡眠，不数数唤醒；日间催眠在结束时把人带回来。
- 会话前互动访谈：安全筛查、你自己的正向暗示、喜欢的放松场景、语气风格、时长。
- 整场先生成完整引导稿，再按句子合成、按顺序播放。句与句之间的停顿是真实静音，时长写在稿子里。
- 合成比播放提前大约两到三句。合成变慢时会等，不会跳过，也不会让下一句盖住上一句。
- 锁屏和息屏后继续播放（前台服务与媒体会话）。助眠收束可以淡出。
- 播放过程中不打开麦克风，也不会做语音识别。
- 语音合成、语音识别、对话模型都是可更换的接口，不把某一家厂商写死。没有配置云端时，用 Android 系统语音即可离线播放内置中文样稿。
- 密钥只加密存在这台手机上。整份配置可以导出、导入，导出时可以选择不带密钥。

## 两种引导

### 日间催眠（结束时唤醒）

适合听完还要起来做事的时候。顺序走完放松、清空、导入、加深、场景和暗示之后，用一段清醒、短促的收束把人带回来：气息变清楚，手指和脚回到当下，眼睛睁开，回到这个房间。日间场次会留下一句很轻的锚点，例如肩膀沉下去时，这层松可以自己回来一点。不会把人留在很深的状态里就结束。

### 助眠（过渡到睡眠，不数数唤醒）

适合躺下、打算睡过去的时候。同样的前段把身体和注意力安顿下来，暗示只朝「更松、更沉、更安稳」走。收束时句子越来越短、停顿越来越长，邀请你随时睡去。这一段**不倒数、不点名唤醒**。录音停在睡眠的门口，而不是把人再数醒。

两条路径的阶段顺序是固定的。时长可以选大约 10、20 或 30 分钟：短场次少写一些场景和暗示，导入不会被抽掉。

## 开始前的互动访谈

引导是单向的：播放开始之后，应用不再提问，也不根据你的表情或呼吸去改词。所以选择都放在开始之前，一次填完。

安全问题会先问。正在开车、走路或操作机器，或者处在酒精或其他物质影响下，应用不会开始。若涉及活动期的严重精神状况、强烈的分离或创伤闪回，应用不会进入深层想象，只提供一段可以睁着眼做的短呼吸着陆，并建议联系专业人员。持续超过一个月、已经影响白天的失眠，仍然可以听放松引导，同时会提示去看医生。

其余是偏好，可以少填：

- 你自己的正向暗示（短句，例如「我在一点点放松」）。应用会收成现在进行、过程导向的说法，不接收疗效承诺、药物或自伤相关内容。
- 想停留的场景：雨窗、林间木屋、河边夜色、旧书架，或你自己描述的安静地方。也可以标明不想听到的内容。
- 语气：更允许式、更简短直接，或解释稍多一点。
- 时长，以及这次是想睡去，还是听完要醒来。

只想马上听时，可以用内置的雨窗样稿，走安全的默认值：助眠、雨窗、大约 10 分钟。

## 为什么这样设计 / 依据

标准流程在文献里大体一致，本应用把它们做成固定顺序的数据，而不是每次临时拼凑：

1. 前谈：说明这是合作，不是被控制；你可以随时睁眼、随时停下。
2. 放松：躺好、呼吸、让身体被接住。
3. 清空思绪：把注意力放在一个锚上，念头经过就让它过去，不要求「什么都别想」。
4. 导入：闭眼与渐进放松。单向语音不用快速、惊吓或混淆式诱导。
5. 加深：身体扫描、楼梯或倒数，一场只用一条主路径。
6. 场景意象：从远到近的安静空间，多感官、慢、安全。
7. 暗示：短的、正向的、能够自然发生的句子。助眠暗示只指向更深、更安稳。
8. 收束：助眠走睡眠过渡；日间催眠走唤醒。日间还可以有一句凭据和一句锚点。

安全句和这个顺序不参与随机。会变的是表层：同一意思的几种说法、场景里的光线和声音、楼梯或倒数的范围。随机有种子，避免紧挨着的两次用同一套细节。这样做是为了听起来不像同一段录音在循环，不是为了宣称比标准化录音更有效。

应用看不见出神深浅，也看不见身体反应，所以整场是开环的：不判断你睡到哪一层，不宣布你已经睡着或眼睛睁不开，播放中不打开麦克风。能参考的只有你在开始前、结束后自己说的感觉。个性化用在称呼、场景、暗示、时长和风格上；效果因人而异，不作保证。

依据方面，Cordi、Schlarb 与 Rasch 2014 年发表在 *SLEEP* 的对照研究里，一段约 13 分钟、结束时不唤醒的催眠录音，让高易感性的年轻女性在午睡中的慢波睡眠增加了八成以上（约 81%）。低易感性参与者没有获益。把暗示改成「睡得更浅」之后，慢波睡眠的增加消失。因此助眠场次不数数唤醒，暗示方向保持为更深、更安稳，也不对所有人承诺同样效果。总体证据是有希望的，但研究质量中等：样本常常较小，对慢性失眠的结论有限。慢性失眠仍应考虑就医；认知行为治疗（CBT-I）是常用的一线非药物途径。

## 架构概览

```
访谈与偏好
    │
    ▼
稿件生成（内置阶段模板，或可选的对话模型）
    │  固定阶段顺序、安全句、随机只动措辞和细节
    ▼
解析成片段：要念的句子 + 带时长的停顿
    │
    ▼
播放引擎：提前合成 2–3 句，顺序播放；停顿=真实静音
    │
    ▼
前台服务 + 媒体会话（息屏继续）
```

三个能力各有一个接口，运行时再选择实现：

| 能力 | 接口 | 内置实现 |
|---|---|---|
| 对话模型 | `LlmProvider` | OpenAI 兼容的 chat completions（地址、密钥、模型、可选流式） |
| 语音合成 | `TtsProvider` | OpenAI 兼容的 `/audio/speech`；通用 HTTP 模板；Android 系统语音 |
| 语音识别 | `AsrProvider` | OpenAI 兼容的 `/audio/transcriptions`；Android `SpeechRecognizer` |

通用 HTTP 语音合成由你填写方法、URL、请求头 JSON 和正文模板，正文里可以用 `{text}`、`{voice}`、`{apiKey}`。响应经过一条管道，例如解析 JSON、按路径取值、Base64 解码，或直接使用原始字节。仓库不包含任何密钥，也不从环境变量读密钥。

## 播放：先写完整稿

旧的做法是「发一句，再 `sleep` 一段算出来的时间」。计时器在文字发出时就启动，跟这段话有没有开始播、有没有播完没有关系。合成延迟又不稳定，快的时候一两秒，慢的时候可以到大约八秒。队列一积压，后面的等待全部错位，呼吸提示或画面提示会在上一句还没说完时到来。

眠境改成：

1. 先生成完整稿，停顿以明确的时长写进稿子。
2. 播放时，停顿就是这段静音，长短按稿子执行，不按这一句合成花了多久来猜。
3. 句子提前合成进一个有上限的队列（大约领先两到三句），再按顺序播放。
4. 合成来不及，就等这一句完成再继续，不跳句，也不用更短的停顿去追进度。
5. 过长的句子会先切开，避免一条合成被截断、丢掉后半句。

艺术上的留白仍然在：准备和导入的句间较短，场景和暗示稍长，助眠收束越来越疏。这些留白是稿子的一部分。技术上的等待只发生在音频还没准备好的时候，不会拿来冒充下一次呼吸指令。

## 获取与构建

### 从 GitHub Actions 下载调试 APK

1. 打开本仓库的 Actions，选择 **Build** 工作流。
2. 推送到仓库、打开拉取请求，或手动运行（workflow_dispatch）都会触发。
3. 运行成功后，在该次运行的 Artifacts 里下载 `dreamscape-debug-apk`。
4. 这是调试签名的安装包，只供个人安装试用。安装前若系统拦截未知来源，需要在系统设置里允许。

工作流使用 JDK 17，并缓存 Gradle。标签上的签名发布构建默认关闭，说明写在 `.github/workflows/build.yml` 里。启用它需要在仓库 Secrets 中配置密钥库（Base64）和密码，密钥库文件不要提交。

### 本地构建

需要 JDK 17 和 Android SDK。

```bash
./gradlew assembleDebug
```

调试 APK 位于 `app/build/outputs/apk/debug/`。单元测试与 lint：

```bash
./gradlew testDebugUnitTest lintDebug
```

包名 `com.hsyatgit.dreamscape`，应用名「眠境」，minSdk 26，targetSdk 为当前稳定的 Android 16（API 36）。界面文案为中文。

## 配置语音、识别与模型

在应用内的「设置」里，为合成、识别、对话模型分别选择实现并填写字段，每项都有测试按钮。

- **对话模型**：OpenAI 兼容接口。填写基地址、密钥、模型名，流式输出可以打开或关闭。不填则只用内置模板，不请求网络。
- **语音合成**：OpenAI 兼容的 `/audio/speech`；或通用 HTTP 模板（方法、URL、请求头 JSON、正文模板、响应管道）。没有可用的云端配置时，使用 Android 系统语音。
- **语音识别**：OpenAI 兼容的 `/audio/transcriptions`，或 Android 系统识别。识别只用于设置里的测试，或你主动发起的输入，不会在引导播放期间启动。
- **密钥**：使用 Android Keystore 与加密存储，只留在本机。
- **导入 / 导出**：一份 JSON。导出时可以去掉密钥。

通用 HTTP 模板的一个真实形状是：向兼容接口 `POST` JSON，正文里把 `{text}` 放进待合成的内容，响应管道为 `parse_json` → 按路径 `choices[0].message.audio.data` 取值 → `base64_decode`。设置页可以填入这组示例字段，密钥仍由你自己填写。

## 许可、贡献与声明

**仅限个人与非商业使用。** 本项目以 [PolyForm Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0) 授权。未经作者书面许可，禁止商业使用。

Required Notice: Copyright (c) 2026 hsyatgit

欢迎为个人、非商业用途提交问题和拉取请求：修构建、改进播放时序、补充场景模板都可以。请不要提交密钥、密钥库或录音成品。改引导文案时，请保持安全句和阶段顺序不变，助眠收束不要加入唤醒倒数。

**声明。** 眠境是放松与睡眠辅助，不是医疗，不是心理治疗，也不能诊断或治愈失眠、焦虑或其他健康问题。请不要在开车、走路或操作机器时使用。若失眠持续、影响白天，或存在严重的心理痛苦、自伤想法，请寻求医生或有资质的专业人员的帮助。听的过程中若感到不适，可以随时睁眼并停止播放。

---

# Dreamscape (English)

Many phone hypnosis and sleep setups speak a script one line at a time: send a line, then wait a fixed delay before the next. That delay starts when the text is sent, not when the sentence has actually finished playing. If synthesis slows down, the next breathing cue or image arrives while the previous sentence is still being spoken, and the trance breaks.

Dreamscape is a voice-guided hypnosis and sleep-aid Android app. It writes the whole session as one continuous script, with pauses stored in the script, then plays those pauses as real silence for the duration written there. The gaps are planned from the finished script. They are not tied to how long each sentence takes to synthesize. Before the formal guidance begins, a pre-session interactive intake records your own positive affirmations and the relaxation scene you want to stay in.

This is a relaxation and sleep aid. It is not medical treatment and it does not replace care from a clinician.

## Features

- Two full flows: **sleep aid** and **daytime hypnosis**. Sleep aid leads into sleep and does not count the listener awake. Daytime hypnosis ends by bringing the listener back.
- A pre-session interactive intake: safety screening, your own positive affirmations, preferred relaxation scenes, speaking style, and length.
- The full guidance script is generated first, then synthesized sentence by sentence and played in order. Pauses between lines are real silence, with durations written in the script.
- Synthesis stays about two or three sentences ahead. If synthesis is slow, playback waits. It does not skip a line, and it does not let the next line talk over the previous one.
- Playback continues with the screen off (foreground service and a media session). Sleep endings can fade out.
- The microphone and speech recognition stay off during playback.
- Speech synthesis, speech recognition, and the chat model are replaceable interfaces. No vendor is hardcoded. With nothing configured in the cloud, Android system speech can play the built-in Chinese sample scripts offline.
- Keys are stored encrypted on the device. The full configuration can be exported and imported, with the option to leave secrets out of the export.

## Two flows

### Daytime hypnosis (with emergence)

For a session you will get up from. After relaxation, clearing, induction, deepening, imagery, and suggestion, a short, clear ending brings you back: the breath becomes distinct, the fingers and feet return to the room, the eyes open. A daytime session also leaves one light anchor, such as a shoulder dropping, so that same ease can return a little on its own later. The session does not stop while you are still left deep.

### Sleep aid (transition into sleep, no wake-up count)

For lying down with the intention of sleeping. The same opening settles the body and the attention. Suggestions point only toward being looser, deeper, and more settled. Toward the end the sentences get shorter and the pauses longer, and you are invited to sleep whenever sleep comes. This ending **does not count upward and does not call you awake**. The recording stops at the threshold of sleep.

Phase order is fixed on both paths. Length is about 10, 20, or 30 minutes. Shorter sessions use less imagery and fewer suggestions. The induction is not removed.

## Pre-session interactive intake

Guidance is one-way. After playback starts, the app does not ask questions or rewrite lines from your expression or breathing. Choices are collected once, before the start.

Safety questions come first. If you are driving, walking, or operating machinery, or if alcohol or another substance is affecting you, the app will not start. If there is an active serious psychiatric condition, strong dissociation, or trauma flashbacks, the app does not enter deep imagery. It offers a short eyes-open breathing and grounding piece and suggests contacting a professional. Insomnia that has lasted more than a month and already affects the day can still use the relaxation guidance, with a note to see a doctor.

The rest is preference, and you can leave most of it blank:

- Your own positive affirmations (short lines, such as “I am easing a little at a time”). The app keeps them in the present, process-oriented form, and it drops claims of cure, medication, or self-harm.
- A scene to stay in: rain on the window, a forest cabin, a river at night, an old bookshelf, or a quiet place you describe. You can also mark images you do not want.
- Style: more permissive, shorter and direct, or with a little more explanation.
- Length, and whether this session should end in sleep or with a return to the room.

If you want to listen immediately, a built-in rain-window script uses safe defaults: sleep aid, rain on the window, about 10 minutes.

## Why it works / Evidence

Published descriptions of hypnosis share the same skeleton. The app stores that skeleton as data with a fixed order, rather than improvising a new structure each time:

1. Pre-talk: this is cooperation, not control. You can open your eyes or stop at any time.
2. Relaxation: lie down, breathe, let the body be supported.
3. Clearing the mind: attention rests on one anchor. Thoughts may pass. The script does not order you to think of nothing.
4. Induction: eye closure and progressive relaxation. One-way audio does not use rapid, startle, or confusion inductions.
5. Deepening: a body scan, a staircase, or a countdown. One session uses one main path.
6. Scene imagery: a quiet place, far to near, with slow and safe sensory detail.
7. Suggestion: short, positive lines that can happen on their own. Sleep suggestions only point toward being deeper and more settled.
8. Ending: sleep transition, or emergence. A daytime session may also include one credential line and one anchor.

Safety lines and this order are not randomized. What can change is the surface: several phrasings of the same meaning, light and sound inside a scene, the range of a staircase or a countdown. Randomization is seeded, and back-to-back sessions avoid repeating the same detail. The aim is to keep the language from wearing smooth, not to claim better results than a standardized recording.

The app cannot see trance depth or physical reactions, so a session is open-loop. It does not judge which layer you have reached, it does not announce that you are asleep or that your eyes will not open, and it does not turn the microphone on during playback. The feelings you report before and after are the only feedback it can use. Personalization covers name, scene, suggestions, length, and style. Response varies from person to person, and nothing here guarantees an effect.

On the evidence: in a controlled study by Cordi, Schlarb, and Rasch, published in *SLEEP* in 2014, a hypnotic recording of about 13 minutes that ended without awakening increased slow-wave sleep by over 80% (about 81%) during a nap in highly suggestible young women. Participants with low suggestibility did not benefit. When the suggestion was reversed toward sleeping more lightly, the increase in slow-wave sleep disappeared. Sleep-aid sessions therefore do not count the listener awake, keep suggestions pointed toward deeper and more settled sleep, and do not promise the same result for everyone. The evidence overall is promising but moderate. Samples are often small, and findings do not automatically extend to chronic insomnia. Lasting insomnia should be discussed with a doctor. Cognitive behavioral therapy for insomnia (CBT-I) is the usual first-line non-drug approach.

## Architecture overview

```
Intake and preferences
    │
    ▼
Script generation (built-in phase templates, or an optional chat model)
    │  fixed phase order and safety lines; randomness only in phrasing and detail
    ▼
Parse into segments: spoken sentences + pauses with durations
    │
    ▼
Playback engine: synthesize 2–3 sentences ahead, play in order; pause = real silence
    │
    ▼
Foreground service + media session (continues with the screen off)
```

Three capabilities are interfaces. The implementation is chosen at runtime:

| Capability | Interface | Built-in implementations |
|---|---|---|
| Chat model | `LlmProvider` | OpenAI-compatible chat completions (base URL, API key, model, optional streaming) |
| Speech synthesis | `TtsProvider` | OpenAI-compatible `/audio/speech`; a generic HTTP template; Android system speech |
| Speech recognition | `AsrProvider` | OpenAI-compatible `/audio/transcriptions`; Android `SpeechRecognizer` |

The generic HTTP synthesizer is whatever you configure: method, URL, header JSON, and a body template with `{text}`, `{voice}`, and `{apiKey}`. The response runs through a pipeline, for example parse JSON, pick a path, Base64-decode, or take the raw bytes. The repository contains no keys, and it does not read keys from environment variables.

## Playback: write the whole script first

The older approach sent one line, then slept for a computed number of milliseconds. The timer started when the text was sent. It had nothing to do with whether that audio had started or finished. Synthesis latency was also unstable: often a couple of seconds, sometimes about eight. Once the queue backed up, every later wait was wrong, and a breathing cue or an image arrived before the previous sentence had finished.

Dreamscape does this instead:

1. Generate the complete script first, with pauses written in as explicit durations.
2. At playback, a pause is that much silence. Its length comes from the script, not from a guess about how long synthesis took.
3. Sentences are synthesized ahead into a bounded queue (about two or three ahead) and played in order.
4. If synthesis is late, playback waits for that sentence. It does not skip, and it does not shorten the written pause to catch up.
5. Long sentences are split before synthesis so one request is not cut off at the end.

The artistic rests stay in the script: shorter gaps in the opening and the induction, slightly longer ones in the scene and the suggestions, and a sleep ending that grows sparser. Those rests are content. Technical waiting happens only while an audio clip is not ready yet. It is not used as a stand-in for the next breathing cue.

## Setup and build

### Download a debug APK from GitHub Actions

1. Open the **Build** workflow under Actions in this repository.
2. It runs on push, on pull requests, and when started manually (workflow_dispatch).
3. After a successful run, download the `dreamscape-debug-apk` artifact from that run.
4. The package is signed with the debug key, for personal installation only. If the system blocks unknown apps, allow installation from that source in system settings.

The workflow uses JDK 17 and caches Gradle. A signed release build on tags is disabled by default. The steps and the required repository secrets (keystore Base64 and passwords) are documented in `.github/workflows/build.yml`. Do not commit a keystore.

### Local build

JDK 17 and the Android SDK are required.

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`. Unit tests and lint:

```bash
./gradlew testDebugUnitTest lintDebug
```

Application id `com.hsyatgit.dreamscape`, app label 眠境, minSdk 26, targetSdk the current stable Android 16 (API 36). User-interface strings are in Chinese.

## API configuration

In the in-app Settings screen, choose an implementation for synthesis, recognition, and the chat model, fill in the fields, and use the test button on each one.

- **Chat model**: an OpenAI-compatible endpoint. Set the base URL, key, and model. Streaming can be on or off. Leave it unset to use only the built-in templates, with no network call.
- **Speech synthesis**: OpenAI-compatible `/audio/speech`, or a generic HTTP template (method, URL, header JSON, body template, response pipeline). If no cloud synthesizer is configured, Android system speech is used.
- **Speech recognition**: OpenAI-compatible `/audio/transcriptions`, or Android system recognition. Recognition is for the test button, or for input you start yourself. It does not run while guidance is playing.
- **Keys**: Android Keystore and encrypted storage, on this device only.
- **Import / export**: one JSON document. Export can omit secrets.

A concrete shape for the generic HTTP template is a `POST` of JSON to a compatible endpoint, with `{text}` placed in the content to be spoken, and a response pipeline of `parse_json`, then pick `choices[0].message.audio.data`, then `base64_decode`. The settings screen can fill in that example. You still supply the key yourself.

## License, contributions, and disclaimer

**Personal and non-commercial use only.** This project is licensed under the [PolyForm Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0). Commercial use is prohibited without the author's written permission.

Required Notice: Copyright (c) 2026 hsyatgit

Issues and pull requests are welcome for personal, non-commercial use: fixing the build, improving playback timing, or adding scene templates. Do not commit keys, keystores, or finished audio recordings. If you change guidance copy, keep the safety lines and the phase order, and do not add a wake-up count to the sleep ending.

**Disclaimer.** Dreamscape is a relaxation and sleep aid. It is not medical treatment, not psychotherapy, and it does not diagnose or cure insomnia, anxiety, or any other health condition. Do not use it while driving, walking, or operating machinery. If insomnia persists or affects the day, or if there is serious psychological distress or thoughts of self-harm, seek a doctor or another qualified professional. If a session feels wrong, open your eyes and stop playback.
