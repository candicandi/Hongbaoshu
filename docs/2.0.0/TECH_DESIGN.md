# 红宝书 App 技术方案 v1.0 / v2.0

> 目标：基于 PRD 的纯离线阅读体验，首发 Android 8.0+，Kotlin + Jetpack Compose，单 Activity 架构。所有文本/音频/图片随包内置，无网络/登录/统计。

---

## 0. v2.0 关键变化（红宝匣）

2.0 目标：阅读器与内容解耦，App 仅提供阅读器能力；文本/音频等通过“资源包（Pack）”导入并在书架管理。

### 0.1 目标与非目标

- 目标：书架、多书管理、资源包导入与校验、按 packId 隔离进度与缓存。
- 非目标：在线商店/下载、DRM/加密、多书合并为一书。

### 0.2 架构分层（新增）

- **Reader 层**：分页、渲染、朗读、进度控制，不直接读 assets。
- **Pack 层**：导入、校验、索引、资源寻址，负责提供 `Book` 与资源 URI。
- **Storage 层**：按 `packId` 存进度与缓存。

### 0.3 核心新模块

- `PackRepository`：管理 pack 索引、当前 pack、资源路径。
- `PackImporter`：导入 zip，校验 manifest 与 book.json，原子化落盘。
- `PackIndexStore`：书架列表与资源状态缓存（持久化）。
- `PackContentLoader`：替代 `ContentLoader`，从 pack 目录解析 `book.json` 与资源 URI。

### 0.4 资源目录与导入（概要）

- 资源包格式见 `docs/PRD_2.0_BOOKSHELF_AND_PACKS.md`。
- 导入流程：解压到临时目录 → 校验 → 生成索引 → 原子移动到 `files/packs/<packId>/`。

---

## 1. 技术栈与整体架构
- **框架**：Kotlin、Jetpack Compose、单 Activity（`MainActivity`），NavHost 控制 Cover → Reader → TOC。`SplashActivity` 仅做冷启封面与跳转逻辑。
- **UI 渲染**：Compose + `AndroidView` 嵌入 PageCurl View（将开源 `android-pagecurl` 源码内置到 `ui/pagecurl` 包，避免远程依赖不稳定）。翻页点击/滑动由 Compose 手势转发。
- **音频**：ExoPlayer（BGM、朗读分别用独立实例），SoundPool 播翻页音效，统一封装在 `AudioManager`。
- **存储**：Preferences DataStore 记录阅读进度、音乐状态、朗读状态。所有内容文件放 `assets/`，不写外部存储。
- **数据模型**：`Chapter(id, title, paragraphs: List<Paragraph>)`；`ReadingState(chapterId, pageIndex)`；`AudioState(bgmEnabled, bgmIndex, narrationPlayingChapterId?)`。
- **模块划分**：ContentLoader → Renderer(PagePager + PageCurl) → AudioManager → ProgressStore → UI 层（Cover/Reader/TOC/Controls）。

## 2. 资源与数据格式
- 目录结构（随包）：
  - `assets/text/mao_quotes_1966.json`（正文与元数据）
  - `assets/audio/bgm/*.mp3`
  - `assets/audio/narration/` 
  - `assets/images/cover.png`（封面）
  - `assets/sound/page_flip.wav`
- JSON 设计（包含书籍名称、作者、章节、正文与注释，按自然段）：
```json
{
  "book": { "title": "毛主席语录", "author": "毛泽东", "edition": "1966" },
  "chapters": [
    {
      "id": "01",
      "title": "毛主席语录",
      "paragraphs": [
        { "id": "01-001", "type": "text", "content": "世界上怕就怕“认真”二字，共产党就最讲认真。" },
        { "id": "01-002", "type": "text", "content": "……下一段正文……" },
        { "id": "01-003", "type": "annotation", "content": "注释内容，说明上一段背景或解释。", "ref": "prev" }
      ]
    }
  ]
}
```
- 说明：
  - 每个自然段一条记录，带唯一 `id`（推荐 `章节ID-段落序号`，序号零填充如 `01-001`）；正文使用 `type: "text"`，注释 `type: "annotation"`，`ref` 可选（如 `"prev"` 或段落序号）。
  - 章节 ID 与朗读音频命名一致（`chapter_<id>.mp3`）方便匹配。
  - 正文段落含 `sentences` 数组，每句带唯一 `id`（格式 `段落ID-s序号`），便于句子级音频映射。
  - 句子级音频命名方案：`[句子id]_[句子内容].wav`（示例：`33-031-s011_而且还可以有共产主义世界观方面的共同语言.wav`）；播放时可仅按前缀 `句子id_` 匹配，忽略文件中正文片段，避免路径长度问题。
- 加载策略：首次启动通过 `ContentLoader` 读取 JSON → 生成内部模型 `List<Chapter>`（`Paragraph` 包含 `id`、`type`、`content`、`ref?`）→ 分页按屏幕尺寸计算并缓存。

## 3. 核心模块设计
### 3.1 ContentLoader
- 负责从 assets 读取 JSON/封面/音频 URI；提供 `AssetDataSource` 或 `AssetFileDescriptor` 给 ExoPlayer。
- JSON 解析流程：
  - 打开 `assets/text/mao_quotes_1966.json` → 读取 `book` 元信息与 `chapters`。
  - 将 `paragraphs` 解析为内部模型，保留顺序；`annotation` 类型可用于渲染不同样式或悬浮提示。
  - 输出内部模型：`List<Chapter>`，其中 `Paragraph` 包含 `id`、`type`、`content`、`ref?`，正文段落包含 `sentences[{id, content}]`。
- 暴露接口：`loadChapters()`, `getNarrationUri(chapterId)`, `getBgmPlaylist()`, `getFlipSound()`, `getCoverImage()`.
  - 实现侧应校验 ID 唯一性，并汇总缺失句子音频 `missingSentenceAudioIds` 以便 UI/播放层降级。

### 3.2 Renderer（分页与翻页）
- `PageEngine`：根据章节文本、屏幕尺寸、字体、行距，计算分页（可用 TextMeasurer 估算行数，按屏幕高度切分），结果缓存 `Map<chapterId, List<Page>>`。
- `PageCurlView`：封装开源卷页算法，支持左右滑与点击区域（左区域上一页/右区域下一页），提供回调 `onPageChanged(chapterId, pageIndex)`。
- `ReaderScreen`：Compose 容器，显示当前页 Canvas/Bitmap；通过 `AndroidView` 承载 `PageCurlView`，传入当前 Page 内容位图。
- 文本渲染（基于 JSON 中的类型）：
  - 书名/章节标题：`book.title` 用封面或页眉处大号字显示；章节标题在章节起始页顶部使用中号、加粗、居中（如 `fontSize=20sp`，`fontWeight=Medium`，`lineHeight=1.3`）。
  - 正文段落（`type=text`）：仿宋/苹方/Noto Serif SC，`fontSize=16sp`，`lineHeight=1.6`，首行缩进 2 字。
  - 注释段落（`type=annotation`）：字号略小（如 `14sp`），颜色次要（如深灰 `#555`），可左侧加竖线或悬浮 tooltip；若有 `ref=prev`，在排版上紧跟上一段且减少上边距。
  - 段落间距：正文段落上下 8–12dp，注释与正文之间 4–8dp；分页时保留这些间距以避免断行突兀。
  - 字体选择：Android 使用 `NotoSerifSC-Regular`（内置或系统），注释可用同字体但小字号，不混用衬线/非衬线以避免跳跃。

### 3.3 AudioManager
- 组件：`bgmPlayer`（循环播放、下一首）、`narrationPlayer`（按句子播放，不跨章节续播）、`soundPool`（翻页音）。
- 状态：`AudioState` 存 DataStore（`bgmEnabled`, `bgmIndex`, `narrationSentenceId`?，`narrationPosition` 可选）。
- 交互：
  - 翻页：触发 `soundPool.play(page_flip)`.
  - BGM 控制面板：播放/暂停、下一首、静音。
  - 朗读按钮：按当前句子 ID 播放对应音频；可按段内顺序自动播放下一句；离开章节自动 `stop()`.

### 3.4 ProgressStore
- DataStore Key：
  - `chapter_id`、`page_index`
  - `bgm_enabled`、`bgm_index`
  - `narration_enabled`（或当前章节 ID）
- API：`saveReadingState`, `observeReadingState`, `saveAudioState`, `observeAudioState`。

### 3.5 UI 页面与组件
- **CoverScreen**：静态封面图，全屏展示。点击或左滑→读取 ProgressStore 跳转目标页。
- **ReaderScreen**：主要阅读界面，包含 PageCurl 视图、顶部 TOC 按钮、右上 BGM 控制按钮、底部朗读按钮/浮层。
- **文本样式约定**：章节标题中号加粗居中（约 20sp），正文 16sp 衬线字体、1.6 行距、首行缩进 2 字；注释 14sp 深灰、与正文间距缩小、可左侧竖线或 tooltip。
- **TOCSheet**：全屏弹层列表，显示章节标题，点击→`PageEngine` 定位章节首页并跳转。
- **MusicControlPanel**：悬浮卡片，显示当前曲目序号、播放/暂停/下一首/静音。
- **NarrationBar**：底部浮层，展示章节名与播放/暂停。

## 4. 关键流程
- **冷启动**：`SplashActivity` 显示封面→读取进度→导航到 Reader 并定位页→开始 BGM（若开启）。
- **翻页**：手势命中→`PageCurlView` 动画→完成后回调保存进度→播放翻页音效。
- **目录跳转**：点击 TOC→展示列表→选择章节→更新当前章节与页索引→停止朗读（若切换章节）→关闭弹层。
- **朗读模式**：点击朗读→`narrationPlayer.setMediaItem(chapterUri)`→播放并显示 `NarrationBar`；翻页仍可操作；进入新章节自动停止。
- **句子顺播规则**：默认仅播放当前页句子列表，页内顺播完成后停止；跨章节/页需用户再触发（避免误播）。
- **BGM 控制**：按钮打开面板→播放/暂停/下一首/静音；状态同步到 DataStore；若静音则释放或暂停播放器。

## 5. 性能与稳定性
- 封面图使用压缩 PNG/JPG，首帧加载控制 <1s；封面阶段不做重 CPU 操作，仅并行预加载 JSON。
- 分页缓存：首章启动时同步分页，其他章节懒分页；使用 LruCache 控制内存；屏幕旋转（如启用）需触发重分页。
- 音频：音频码率 96–128 kbps；ExoPlayer 复用实例减少初始化开销；切歌淡出/淡入避免爆音。
- 线程：分页在 Dispatchers.Default，IO 操作在 Dispatchers.IO，UI 只做渲染。
- 错误处理：资源缺失时 fallback 提示并中断对应功能，避免崩溃；Audio/ExoPlayer 异常捕获后重建实例。

## 6. 包体积与资源管理
- 图片 WebP/压缩 JPG，分辨率适配最大设备宽度。
- 音频批量压缩为 AAC/MP3，立体声 44.1kHz，码率控制在总体包体积 50–150 MB 目标内。
- PageCurl 源码内置，移除未用类，proguard 关闭或按需混淆。

## 7. 测试计划
- **单元**：ContentLoader 解析 JSON（元信息、章节数、段落/句子顺序与 ID 唯一性、注释解析）、分页算法（给定屏幕参数生成页数）、ProgressStore 读写、AudioManager 状态机。
- **仪表**：导航流（封面→阅读→目录→跳转）、翻页手势与动画触发、BGM 面板操作、朗读模式启停、进度恢复（重启后回到上次页）。
- **体验**：60fps 翻页、音频切换无爆音、旋转/后台恢复状态正确、无网络权限。

## 8. 开发里程碑
1) 资源落盘与 ContentLoader 验证 → 2) 分页 + PageCurl 渲染 → 3) AudioManager（BGM/翻页音/朗读） → 4) 进度持久化 → 5) UI polish（控制面板、TOC） → 6) 性能与包体积收尾。

## 9. 风险与对策
- PageCurl 开源实现兼容性：优先内置源码并裁剪；如出现性能问题，降级为简化左右滑翻页动画。
- 资源体积：若朗读音频过大，按章节拆分压缩并评估码率；必要时首次启动解压压缩包到内部存储。
- 不同设备字体/排版差异：分页依赖屏幕与字体度量，需在常见分辨率上回归测试。

---

# 红宝匣 App 技术方案 v2.0（新增细化）

## 1. 包与书架的核心数据模型

### 1.1 PackIndex（书架索引）

- `packId`, `packVersion`, `formatVersion`
- `bookTitle`, `bookAuthor`, `bookEdition`
- `importedAt`, `lastOpenedAt`
- `capabilities`：`hasText`, `hasCover`, `hasFlipSound`, `hasNarration`
- `missingNarrationSentenceCount`
- `isValid`

### 1.2 PackRuntime（运行时）

- `activePackId`
- `activeBook: Book`
- `baseDir: File`
- `resourceResolver`：封面/翻页/朗读等资源寻址

### 1.3 Manifest 与索引模型（建议 Kotlin 数据类）

```
data class PackManifest(
  val formatVersion: Int,
  val packId: String,
  val packVersion: Int,
  val book: BookMetadata,
  val resources: PackResources
)

data class PackResources(
  val text: ResourceItem,
  val cover: ResourceItem? = null,
  val flipSound: ResourceItem? = null,
  val narration: NarrationResource? = null
)

data class ResourceItem(
  val path: String,
  val sha256: String? = null
)

data class NarrationResource(
  val dir: String,
  val codec: String
)

data class PackIndex(
  val packId: String,
  val packVersion: Int,
  val formatVersion: Int,
  val bookTitle: String,
  val bookAuthor: String,
  val bookEdition: String,
  val importedAt: Long,
  val lastOpenedAt: Long? = null,
  val hasCover: Boolean,
  val hasFlipSound: Boolean,
  val hasNarration: Boolean,
  val missingNarrationSentenceCount: Int = 0,
  val isValid: Boolean = true
)
```

## 2. 存储布局

```
files/
  packs/
    <packId>/
      manifest.json
      text/book.json
      images/cover.png
      sound/page_flip.wav.ogg
      audio/narration/*.mp3
cache/
  page_cache/
    <packId>/
      <fontSize>_<width>_<height>.json
```

### 2.1 索引持久化建议

- 方案 A：DataStore 保存 `List<PackIndex>` 的 JSON 串
- 方案 B：Room 表 `pack_index`（更易查询与排序）

建议：2.0 先用 DataStore，保持实现简单。Pack 数量通常较少，性能可接受。

## 3. Reader 与 Pack 解耦接口

### 3.1 PackContentLoader（替换现有 ContentLoader）

- `loadBook(packId): Book`
- `narrationUri(packId, sentenceId): Uri?`
- `flipSound(packId): Uri?`
- `coverImage(packId): Uri?`

### 3.2 ReaderViewModel 输入变化

- 初始化时由 `activePackId` 决定加载内容
- `ProgressStore` 改为按 `packId` 命名空间隔离

### 3.3 ActivePack 状态管理

- `AppState` 持有 `activePackId`
- `BookshelfScreen` 选择 pack 后更新 `activePackId`
- `ReaderViewModel` 根据 `activePackId` 重建内容与缓存

## 4. 导入流程（实现细节）

- `PackImporter.import(zipUri)`：
  - 解压到 `cache/pack_import/<tmpId>/`
  - 校验 manifest + text/book.json
  - 校验 ID 唯一性
  - 统计音频缺失（可延迟扫描）
  - 生成 PackIndex
  - 原子移动到 `files/packs/<packId>/`
  - 索引写入 `PackIndexStore`

### 4.1 校验实现细节

- 校验 `packId`：仅允许 `[a-zA-Z0-9._-]`，长度建议 <= 120
- 校验 `formatVersion`：仅支持 1
- 校验 `book.json`：解析成功且 ID 唯一
- 可选校验 `sha256`：仅对 manifest 内声明的关键资源进行

### 4.2 版本冲突策略落地

- `packId` 相同且 `packVersion` 更高：允许覆盖，保留进度
- `packVersion` 相同：提示已存在，返回 SKIPPED
- `packVersion` 更低：默认拒绝，可通过设置允许覆盖

### 4.3 原子落盘建议

- 临时目录：`cache/pack_import/<tmpId>/`
- 目标目录：`files/packs/<packId>/`
- 使用 `renameTo` 或 `Files.move` 完成原子替换
- 失败需清理临时目录与半成品

## 5. builtin Pack 迁移

- 首次启动 v2.0：
  - 检测 `packs/builtin` 是否存在
  - 若不存在：将 assets 复制为 builtin pack
  - 写入 PackIndex
  - 进度存入 `packId=builtin`

### 5.1 builtin 复制策略

- 复制 `assets/text`, `assets/audio`, `assets/images`, `assets/sound`
- 生成 `manifest.json`
- 生成 `PackIndex`

## 6. 书架与阅读流程

- `BookshelfScreen` 读取 PackIndex 列表
- 选择书籍：
  - `activePackId = packId`
  - 打开 `ReaderScreen`
- 返回书架：保持 `activePackId`，仅暂停朗读

### 6.1 Bookshelf UI 状态绑定

- 书架列表数据来自 `PackIndexStore.observe()`
- 卡片 UI 根据 `hasNarration` 与 `missingNarrationSentenceCount` 渲染状态
- 不可用 pack 卡片禁止进入阅读器

## 7. 兼容与升级

- App 名称改为“红宝匣”
- `applicationId` 是否变更需单独决策（影响升级路径）
- 旧版本进度可通过迁移工具写入 `packId=builtin`

---

## 8. 代码结构建议（包划分）

```
com.xuyutech.hongbaoshu
  app/                  // AppState, Router, MainActivity
  bookshelf/            // BookshelfScreen, BookshelfViewModel
  pack/
    import/             // PackImporter, validators
    repository/         // PackRepository, PackIndexStore
    model/              // PackManifest, PackIndex
    loader/             // PackContentLoader
  reader/               // 现有 Reader + PageEngine
  audio/                // AudioManager
  storage/              // ProgressStore, PageCacheStore
```

## 9. SAF 与文件权限

- 使用系统文件选择器导入 Zip
- 对外部 `Uri` 读取需 `ContentResolver.openInputStream`
- 可选：持久化权限 `takePersistableUriPermission`

## 10. 2.0 测试清单（新增）

- PackImporter：格式错误/缺文件/重复导入/版本升级
- PackIndexStore：新增/删除/重校验
- builtin 迁移：首次启动写入索引与文件
- Reader：切换 pack 后进度隔离
