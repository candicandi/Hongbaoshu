# 组件设计与接口 v1.0

聚焦关键模块的输入/输出、状态与交互，便于并行开发。

## 1. ContentLoader
- 职责：读取 `assets` 内文本/音频资源，提供内存模型与 URI。
- 接口（Kotlin 伪代码）：
```kotlin
data class Sentence(val id: String, val content: String)
data class Paragraph(val id: String, val type: ParagraphType, val content: String, val ref: String?, val sentences: List<Sentence>)
data class Chapter(val id: String, val title: String, val paragraphs: List<Paragraph>)
data class Book(val title: String, val author: String, val edition: String, val chapters: List<Chapter>)

interface ContentLoader {
    suspend fun loadBook(): BookLoadResult          // 携带缺失音频列表
    fun narrationUri(sentenceId: String): Uri?      // 句子级音频
    fun narrationByChapter(chapterId: String): List<Uri> // 可选：按章节批量
    fun bgmPlaylist(): List<Uri>
    fun flipSound(): Uri
    fun coverImage(): Uri
}
```
- 细节：
  - JSON 解析：使用 kotlinx.serialization / Moshi，读取 `assets/text/mao_quotes_1966.json`。
  - 朗读音频：扫描 `assets/audio/narration`，构建 `sentenceId -> Uri` 映射；匹配时取文件名前缀到 `_` 处（句子文本过长时仅用前缀匹配）。URI 采用 `asset:///path`，便于 ExoPlayer 的 AssetDataSource 播放。
  - 容错：缺失音频时返回 null，并在 `BookLoadResult.missingSentenceAudioIds` 汇总；UI/播放器据此禁用/跳过。
  - 校验：加载时检查 chapter/paragraph/sentence ID 唯一性，异常直接暴露。

## 2. PageEngine / Renderer
- 输入：`Chapter.paragraphs`（含 sentences），目标视口尺寸、字体、行距。
- 输出：`Page` 列表，包含绘制所需文本块、句子/段落位置信息。
- 要点：
  - 段落/句子 ID 透传到布局结果，便于点击句子播放音频或高亮。
  - 段间距、行高按 TECH_DESIGN 样式。
  - 缓存：`Map<chapterId, List<Page>>`，LruCache 控制。

## 3. AudioManager
- 职责：BGM、句子朗读、翻页音效统一管理。
- 接口（Kotlin 伪代码）：
```kotlin
data class AudioState(
    val bgmEnabled: Boolean,
    val bgmIndex: Int,
    val narrationSentenceId: String? = null,
    val narrationPosition: Long = 0L
)

interface AudioManager {
    val state: StateFlow<AudioState>
    fun playBgm(index: Int? = null)
    fun pauseBgm()
    fun nextBgm()

    fun playSentence(sentenceId: String)
    fun pauseSentence()
    fun resumeSentence()
    fun stopSentence()

    fun playFlip()
}
```
- 细节：
  - 朗读按句子 ID 定位音频；播放完成后可回调驱动 UI 选播下一句（按 PageEngine 顺序）。
  - 状态持久化到 DataStore（bgm 开关/索引，最近朗读句子 ID + 位置可选）。
  - 翻页音：SoundPool 预加载。

## 4. ProgressStore
- DataStore key 约定：
  - `chapter_id`, `page_index`
  - `bgm_enabled`, `bgm_index`
  - `narration_sentence_id`（可选 `narration_position`）
- API：`saveReadingState`, `observeReadingState`, `saveAudioState`, `observeAudioState`。

## 5. UI 交互契约
- CoverScreen：点击/左滑 → 读取进度并跳转 Reader。
- ReaderScreen：
  - PageCurl 手势左右翻页；翻页完成 → 保存 `chapterId/pageIndex`。
  - 句子点击（可选）：触发对应音频播放；播放中高亮句子。
  - 底部朗读按钮：播放当前页首句，自动顺播到页末，进入下一页可选继续/停止。
- TOC：跳转章节首页；若正在朗读，跨章节则停止。
- BGM 控制：播放/暂停/下一首/静音。

## 6. 错误与降级
- 缺音频：朗读按钮置灰或提示“无音频”；不中断阅读。
- 渲染失败：显示简化文本列表，降级 PageCurl。
- 资源缺失：记录日志（Logcat tag `HBS`），UI toast 提示。
