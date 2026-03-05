# 红宝匣 2.0 架构拆分说明

目标：阅读器与资源解耦，App 作为“阅读器壳”，内容以 Pack 导入并在书架管理。该说明聚焦模块边界、数据流与关键接口。

## 0. 当前落地进度（2026-02-06）

- 已落地：书架首页（Material 3 风格），内置书 `builtin` 也在书架索引中展示并可进入阅读。
- 已落地：Zip Pack 导入（SAF OpenDocument），解压到临时目录校验后原子落盘到 `files/packs/<packId>/`，并写入书架索引。
- 已落地：导入书籍的封面与占位策略
- 封面自动探测常见路径（`images/cover.png|jpg|jpeg`、`cover.png|jpg|jpeg`），并兼容 `manifest.resources.cover.path`。
- 封面加载失败或缺失时：使用“无封面”的纸感占位，避免空白。
- 已落地：进度与分页磁盘缓存按 `packId` 隔离（避免不同书籍之间互相污染）。
- 已落地：导入包的文本阅读接入（基于 `FilePackContentLoader` 从 pack 目录读取 `book.json`）。
- 已落地：书架卡片高度稳定（状态徽章强制单行，避免文案换行撑高）。
- 未落地：导入包的朗读音频播放（当前仅内置书启用朗读控制，导入书提示“朗读暂未接入”）。
- 测试资源包（用于真机验证导入与封面策略）：`tests/fixtures/packs/*.hbs.zip`。

---

## 1. 模块边界

### 1.1 Reader（阅读器层）

职责：
- 纯渲染与交互：分页、翻页、目录、朗读控制、夜间模式、字体设置
- 不直接读 `assets/` 或文件系统
- 只通过 Pack 接口获取 `Book` 与资源 URI

输出：
- 读取进度（章节/页）
- 朗读播放请求（句子 ID 列表）
- UI 状态（工具栏、面板等）

### 1.2 Pack（资源层）

职责：
- Pack 导入、校验、落盘
- Pack 索引与书架数据管理
- 资源寻址（封面、翻页音效、朗读音频）

输出：
- `Book` 模型
- 资源 URI/路径
- 索引信息（能力、缺失统计、有效性）

### 1.3 Storage（存储层）

职责：
- 进度按 `packId` 隔离
- 分页缓存按 `packId + fontSize + screen` 隔离
- 索引持久化

---

## 2. 关键数据流

### 2.1 启动到阅读

1. `BookshelfScreen` 从 `PackIndexStore` 读取书架列表
2. 用户选择 `packId` → `PackRepository.setActive(packId)`
3. `ReaderViewModel` 通过 `PackContentLoader` 加载 `Book`
4. `Reader` 读取 `ProgressStore(packId)` 恢复进度
5. 进入阅读

### 2.2 导入 Pack

1. 用户选择 Zip 文件
2. `PackImporter` 解压到临时目录
3. 校验 manifest + book.json + ID 唯一性
4. 统计缺失音频（可延迟扫描）
5. 原子移动到 `files/packs/<packId>/`
6. 写入 `PackIndexStore`
7. 书架刷新

### 2.3 朗读播放

1. Reader 发起 `play(sentenceId | sentenceIdList)`
2. `AudioManager` 通过 `PackContentLoader.narrationUri(packId, sentenceId)` 获取 URI
3. 播放结束回调 → Reader 请求下一句/翻页

---

## 3. 关键接口（建议）

### 3.1 PackRepository

```
interface PackRepository {
  suspend fun listPacks(): List<PackIndex>
  suspend fun setActive(packId: String)
  suspend fun activePack(): PackRuntime?
}
```

### 3.2 PackContentLoader

```
interface PackContentLoader {
  suspend fun loadBook(packId: String): Book
  fun narrationUri(packId: String, sentenceId: String): Uri?
  fun coverUri(packId: String): Uri?
  fun flipSoundUri(packId: String): Uri?
}
```

### 3.3 PackImporter

```
interface PackImporter {
  suspend fun import(zipUri: Uri): PackImportResult
}
```

---

## 4. 存储布局

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

---

## 5. 兼容与迁移

- v1 内置 assets 迁移为 builtin pack
- 旧进度迁移到 `packId=builtin`
- `applicationId` 是否变更需单独决策（影响升级路径）

---

## 6. 边界与约束

- Reader 不允许直接访问文件系统
- Pack 导入必须原子化，失败清理
- 书架索引与实际文件状态需要支持“重新校验”

---

## 7. 依赖关系（读写方向）

```
BookshelfScreen -> BookshelfViewModel -> PackRepository -> PackIndexStore
ReaderScreen -> ReaderViewModel -> PackContentLoader -> FileSystem
ReaderViewModel -> ProgressStore(packId)
ReaderViewModel -> PageCacheStore(packId)
AudioManager -> PackContentLoader (narration/flip sound)
```

规则：UI 只依赖 ViewModel；Reader 不可访问 PackStore 以外文件系统。

---

## 8. 状态与事件

### 8.1 全局状态

- `activePackId`
- `bookshelfPacks: List<PackIndex>`

### 8.2 事件

- `ImportRequested(uri)` → `PackImporter.import`
- `PackSelected(packId)` → `activePackId` 更新 → `ReaderViewModel` 重新加载
- `PackDeleted(packId)` → 删除文件 + 清理索引 + 清理缓存

---

## 9. 错误与恢复策略

- `FORMAT_UNSUPPORTED`：提示“不支持的资源包格式”
- `MANIFEST_INVALID`：提示“资源包损坏或不完整”
- `BOOK_INVALID`：提示“书籍内容解析失败”
- `VERSION_CONFLICT`：提示“版本冲突”
- `FILE_MISSING`：提示“资源文件缺失”

出现错误时必须清理临时目录并保持书架稳定。
