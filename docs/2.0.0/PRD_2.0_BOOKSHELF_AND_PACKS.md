# 2.0 方案细化：书架、资源包导入与校验

目标：App 本体作为阅读器；文本/音频等以“资源包（Pack）”导入并在书架中管理与使用。
产品名：2.0 起 App 名称为“红宝匣”。

本方案覆盖：
- 书架功能与交互（多书管理）
- 导入资源包格式规范
- 导入校验与“资源缺失/部分资源”处理策略
- 内容与阅读器解耦后的扩展问题

---

## 1. 名词与范围

### 1.1 名词

- Pack（资源包）：包含一本书（或一个内容集合）的文本、音频、封面、音效等资源的可导入单位。
- Book（书籍）：从 Pack 的 `text/book.json` 解析得到的结构化内容（章节/段落/句子）。
- Bookshelf（书架）：本地 Pack 的列表与管理入口。
- Active Pack（当前书）：当前正在阅读的 Pack。
- Reader（阅读器）：渲染/分页/朗读/进度等能力模块，不直接依赖内置内容。

### 1.2 非目标（2.0 不做或不强制）

- 不强制联网下载资源（可以后续加“在线资源商店/下载器”）。
- 不强制 DRM/加密（可作为 2.x 增强）。
- 不强制多书合并为一书（2.0 默认 1 Pack = 1 Book）。

---

## 2. 产品功能：书架（Bookshelf）

### 2.1 功能列表

- 展示本地已导入的书籍列表（Pack 列表）
  - 封面、书名、作者、版本/版次（可选）、导入时间（可选）
  - 资源完整性状态（例如：文本可用/音频缺失/仅文本）
- 选择一本书进入阅读器（设置为 Active Pack）
- 导入资源包（见第 3 节）
- 管理资源包
  - 删除书籍（删除 Pack 本地数据与索引）
  - 重新校验（可选，用于定位损坏/缺失）
  - 查看详情（可选：显示 PackId、格式版本、资源统计、缺失列表）
- 最近阅读（默认按最近打开排序；可增加固定/置顶后续扩展）
- 书架空态与引导（无书时展示“导入资源包”主按钮）

### 2.1.1 书架 UI 形态（建议）

- 默认：两列封面网格
- 卡片信息：封面、书名、作者、资源状态徽章
- 书籍卡片的长按菜单：删除 / 重新校验 / 详情
- 书架顶部操作区：搜索（可选）、导入按钮

### 2.1.2 书架交互细节（建议）

- 点击卡片进入阅读器
- 长按卡片打开操作：删除 / 重新校验 / 详情
- 详情页展示：封面、书名、作者、版本、packId、formatVersion、资源统计、缺失句子数量
- 资源不可用时点击：提示原因并提供“删除 / 重新导入”

### 2.2 状态与降级

- 无任何书籍：展示空态，引导导入
- 仅有“内置默认包（builtin）”：书架显示 builtin，一键进入阅读
- 资源损坏：书架卡片显示“不可用”，点击后提示修复/删除
 - 索引存在但文件缺失：显示“不可用”，允许重新校验或删除

### 2.3 与阅读进度的关系

- 阅读进度、阅读设置等必须按 Pack 维度隔离保存：
  - 进度：章节索引/页码
  - 音频：语速、是否开启朗读（如需要）
  - 其他：夜间模式、字体档位等（如需要）
- 书架展示“上次阅读位置”（可选：章名 + 页码或全书页码）

### 2.4 书架状态徽章（建议）

- 完整：`文本+音频`
- 仅文本：`仅文本`
- 部分缺失：`音频缺失(数量)`
- 不可用：`损坏/不可用`

### 2.5 排序规则（建议）

- 默认排序：`lastOpenedAt` 降序
- 次级排序：`importedAt` 降序
- 搜索（可选）：按书名/作者/版本匹配

---

## 3. 产品功能：导入（Import）

### 3.1 导入入口（建议）

- 书架页右上角“导入”按钮
- 支持系统分享/打开方式导入（外部 App 发送文件到本 App）

### 3.2 导入流程（用户视角）

1. 选择一个 Pack 文件
2. App 展示“导入中”
3. 校验通过：导入成功，书架新增一本书并可直接打开
4. 校验失败：明确错误原因，并提供“取消/重试/删除临时文件”

### 3.2.1 导入失败提示（建议文案）

- “资源包损坏或不完整”
- “资源包格式不支持”
- “书籍内容解析失败”
- “资源包已存在（版本相同）”
- “资源包版本较低，拒绝覆盖”

### 3.3 重复导入与版本策略

- PackId 相同：
  - 若 `packVersion` 更高：提示“更新资源”，导入后覆盖旧包（保留进度）
  - 若 `packVersion` 相同且内容校验一致：提示“已存在”，不重复导入
  - 若 `packVersion` 更低：提示“版本较旧”，默认拒绝或允许“覆盖为旧版本”（可选）
- PackId 不同但内容可能相同：以校验和（checksum）去重（可选）

### 3.4 原子导入策略（实现约束）

- 解压到临时目录 -> 完整校验 -> 原子移动到正式目录
- 任意失败：清理临时目录与半成品，避免书架出现“幽灵书”

---

## 4. 资源包格式规范（Pack Format Spec）

### 4.1 容器与编码

- 容器：Zip 文件
- 建议扩展名：`.hbs.zip`（仅用于识别；实际以 Zip 解包为准）
- 编码：
  - JSON：UTF-8
  - 文件路径：UTF-8

### 4.2 目录结构（规范）

```
<root>/
  manifest.json
  text/
    book.json
  images/
    cover.png              (可选，推荐路径)
  cover.png                (可选，兼容路径)
  sound/
    page_flip.wav.ogg      (可选)
  audio/
    narration/             (可选)
      <sentenceId>_*.mp3
    bgm/                   (可选)
      *.mp3
      playlist.json        (可选)
```

说明：
- `text/book.json` 为必选；其 schema 与当前阅读器 `BookJson` 保持兼容。
- `audio/narration` 中音频文件名建议以 `<sentenceId>_` 作为前缀，用于句子级映射。
- 封面路径推荐使用 `images/cover.png`；若资源包使用其他路径，需在 `manifest.json` 的 `resources.cover.path` 中明确声明。App 也会尝试在若干常见路径中自动探测（例如 `cover.png`、`images/cover.jpg` 等），以降低资源包制作门槛。

### 4.3 manifest.json（规范）

#### 4.3.1 示例

```json
{
  "formatVersion": 1,
  "packId": "com.xuyutech.hbs.builtin.maoquotes",
  "packVersion": 1,
  "book": {
    "title": "书名",
    "author": "作者",
    "edition": "版本/版次"
  },
  "resources": {
    "text": { "path": "text/book.json", "sha256": "..." },
    "cover": { "path": "images/cover.png", "sha256": "..." },
    "flipSound": { "path": "sound/page_flip.wav.ogg", "sha256": "..." },
    "narration": {
      "dir": "audio/narration",
      "codec": "mp3"
    }
  }
}
```

#### 4.3.2 字段说明

- `formatVersion`（必选，int）：资源包格式版本，用于未来升级兼容。
- `packId`（必选，string）：资源包唯一标识。建议使用反向域名或 UUID。
- `packVersion`（必选，int）：同一 packId 的版本号，用于更新/覆盖判断。
- `book`（必选）：用于书架展示的元信息，和 `text/book.json` 中 `book` 保持一致即可。
- `resources.text.path`（必选）：文本 JSON 路径。
- `resources.*.sha256`（建议）：导入校验更精确；可只对关键文件提供。

### 4.4 text/book.json（与现有 schema 对齐）

- 必须能被解析为：
  - `book`: `{ title, author, edition }`
  - `chapters`: `[{ id, title, paragraphs: [...] }]`
  - `paragraphs`: `[{ id, type, content, ref?, sentences? }]`
  - `sentences`: `[{ id, content }]`
- ID 约束（必须）：
  - `chapter.id` 全书唯一
  - `paragraph.id` 全书唯一
  - `sentence.id` 全书唯一

### 4.5 音频映射规则（句子级 narration）

- 默认规则：以文件名的 `<sentenceId>_` 前缀映射到该句子音频。
- 扩展规则（可选，2.x）：支持 `audio/narration/index.json` 显式映射，以解决前缀冲突或更复杂的资源命名。

### 4.6 可选文件与降级策略

- `images/cover.png` 缺失：使用默认封面
- `sound/page_flip.wav.ogg` 缺失：翻页静音
- `audio/narration` 缺失：朗读入口置灰或隐藏

---

## 5. 导入校验与错误处理

### 5.1 校验分级

导入过程按“必选项/可选项”分级处理：

#### 5.1.1 必选项（失败即导入失败）

- Zip 可解压，目录可读取
- `manifest.json` 存在且可解析
- `formatVersion` 支持（目前仅支持 1）
- `packId`、`packVersion` 合法
- `text/book.json` 存在且可解析为书籍结构
- 文本内容 ID 唯一性校验通过

#### 5.1.2 可选项（失败则降级导入，标记缺失）

- `images/cover.png` 缺失：书架使用默认封面
- `sound/page_flip.wav.ogg` 缺失：翻页无音效
- `audio/narration/` 缺失或为空：朗读功能禁用或提示“无音频”
- `audio/narration/` 部分缺失：仅缺失句子不可朗读，阅读不受影响

### 5.2 部分资源策略（重点：只有文本没有音频）

#### 5.2.1 仅文本（无 narration）

- 导入：成功
- 书架：显示“仅文本”状态
- 阅读器：
- 朗读入口置灰/隐藏（或点击提示“该资源包不含音频”）
- 点击句子播放：提示“音频缺失”

#### 5.2.2 部分音频缺失（有 narration 但不全）

- 导入：成功
- 校验：统计缺失 `sentenceId` 列表并存储到索引信息
- 阅读器：
  - 朗读连续播放到缺失句子时：
  - 推荐策略 A：跳过缺失句子继续下一句，并在 UI 轻提示“部分音频缺失”
  - 推荐策略 B：播放停止并提示（更简单但体验差）
  - 点击缺失句子：提示“该句子音频缺失”

### 5.3 损坏/篡改处理

- 若 manifest/关键文件 sha256 不匹配（启用校验时）：
  - 导入失败：提示“资源包损坏或不完整”
- 若导入后文件被系统清理/用户删除（理论上在私有目录不常见，但仍需处理）：
  - 书架显示“不可用”，提供删除入口

### 5.4 原子导入与回滚（实现约束）

- 解压到临时目录 -> 完整校验 -> 原子移动到正式目录
- 任意失败：清理临时目录与半成品，避免书架出现“幽灵书”

---

## 6. 书架数据模型（建议）

### 6.1 PackIndex（索引）

每个 Pack 在本地保存一份索引记录，用于快速展示书架与校验状态：

- `packId`, `packVersion`, `formatVersion`
- `bookTitle`, `bookAuthor`, `bookEdition`
- `importedAt`, `lastOpenedAt`
- `capabilities`：
  - `hasText`（恒为 true）
  - `hasCover`
  - `hasFlipSound`
  - `hasNarration`
- `missingNarrationSentenceCount`（可选）
- `isValid`（是否可用）

### 6.2 进度（Progress）

进度必须按 packId 命名空间隔离：

- `packId`
- `chapterIndex`, `pageIndex`
- `narrationSpeed` 等阅读器设置

### 6.3 资源包导入结果（建议）

- `status`: SUCCESS / FAILED / SKIPPED
- `errorCode`: FORMAT_UNSUPPORTED / MANIFEST_INVALID / BOOK_INVALID / FILE_MISSING / VERSION_CONFLICT
- `message`: 展示给用户的错误信息

---

## 8. 读取/播放与缓存策略（新增）

### 8.1 资源寻址

- 所有资源读取路径必须以 `packId` 为根目录
- 阅读器只通过 Pack 接口获取：
  - `book.json` 解析结果
  - 封面、翻页音效、朗读音频

### 8.2 分页与缓存

- 分页缓存 key 建议：`packId + fontSize + widthPx + heightPx`
- 缓存持久化目录按 packId 分隔，避免跨书污染
- 切换书籍时清理内存级分页缓存

---

## 9. 命名与升级（新增）

- App 名称：2.0 起为“红宝匣”
- packageName 是否变更需单独决策（关系到升级路径）
- 资源包建议扩展名：`.hbs.zip` 或 `.hbx`（待定）

---

## 10. 2.0 需要提前解决的其他问题（扩展）

### 10.1 内置内容迁移为 builtin Pack

- 将当前 assets 作为 builtin pack
- 启动时写入 PackIndex
- 进度按 `packId = builtin` 保存

### 10.2 进度与设置隔离

- 每本书独立进度与设置
- 书架显示“上次阅读位置”

### 10.3 外部文件与权限

- 需要支持系统“打开方式/分享导入”
- SAF Uri 读取需处理持久化权限与中断恢复

### 10.4 大资源包性能

- 导入校验阶段避免全量加载音频
- 朗读音频缺失统计可异步扫描并写回索引

### 10.5 缺失音频策略落地

- 连续朗读：建议跳过缺失句子继续播放
- UI 轻提示缺失（非阻塞）

### 10.6 导入与删除的安全性

- 删除 pack 时必须同步清理索引与缓存
- 删除前如为当前阅读中的 pack，需先退出阅读

---

## 7. 交付顺序（建议）

### 7.1 Milestone A：书架 + builtin 包

- 书架可展示 builtin（当前 assets 内容）
- 点击进入阅读器阅读 builtin
- 进度按 packId 保存（builtin 不影响后续导入书籍）

### 7.2 Milestone B：导入 + 校验 + 书架管理

- 导入 zip
- 校验规则落地
- 书架显示资源完整性状态
- 删除/重导入

### 7.3 Milestone C：完善缺失音频策略

- 连续朗读的缺失处理策略确定并落地
- 书架详情页显示缺失统计（可选）
