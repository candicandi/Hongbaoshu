# 数据与资源规范 v1.0

面向编码的结构与命名约定，确保文本、句子音频和元信息一致。

## 1. JSON 结构（assets/text/mao_quotes_1966.json）
- 根：
  - `book`: `{title, author, edition}`
  - `chapters`: `Chapter[]`
- `Chapter`：
  - `id`: `String`（2 位零填充，如 `01`）
  - `title`: 章节标题
  - `paragraphs`: `Paragraph[]`
- `Paragraph`：
  - `id`: `章节ID-序号`（序号 3 位零填充，如 `01-001`）
  - `type`: `"text"` | `"annotation"`
  - `content`: 段落原文（去除注释括号）
  - `ref`（可选）: `"prev"` 等，指示注释关联
  - `sentences`（仅 `type=text`）：`Sentence[]`
- `Sentence`：
  - `id`: `段落ID-s序号`（序号 3 位零填充，如 `01-001-s001`）
  - `content`: 句子文本（按中文逗号/句号切分）

## 2. 音频命名与映射
- 朗读粒度：句子。
- 文件名格式：`[句子id]_[句子content].wav`，如：`33-031-s011_而且还可以有共产主义世界观方面的共同语言.wav`
- 匹配策略（建议）：
  - 读取目录 `assets/audio/narration/`，按文件名前缀到 `_` 前部分匹配 `sentence.id`。
  - 如需缩短路径，播放时可仅依靠 `句子id_` 前缀匹配，忽略后续正文片段。
- 备用映射：若未来更换命名，可提供 `assets/audio/narration/index.json`：`{ "sentences": [{ "id": "01-001-s001", "file": "01-001-s001_xxx.wav" }] }`

## 3. 资源目录
- `assets/text/mao_quotes_1966.json`
- `assets/audio/bgm/*.mp3`
- `assets/audio/narration/*.wav`（句子级朗读）
- `assets/images/cover.png`
- `assets/sound/page_flip.wav`

## 4. 内容规则
- 段落切分：连续非空行合并为一个段落。
- 句子切分：中文逗号、中文句号分割，尾部符号留在句子内。
- 注释：全角【】包裹的文本作为 `annotation`，不切分句子，`ref` 默认 `"prev"`（紧随上一条正文）。
- ID 唯一性：`chapter.id`、`paragraph.id`、`sentence.id` 全局唯一；生成逻辑见 `scripts/md_to_json.py`。
