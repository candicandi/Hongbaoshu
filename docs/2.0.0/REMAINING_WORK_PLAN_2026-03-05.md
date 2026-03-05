# 红宝匣 2.0 剩余工作执行计划（2026-03-05）

## 1. 目标与原则

- 目标：完成 2.0 从“功能可用”到“可发布”的收口工作。
- 原则：
- 先 P0（核心体验/稳定性），再 P1（能力补全），最后 P2（体验增强）。
- 每项工作必须有可验证的验收标准。
- 所有改动优先复用现有架构（Reader/Pack/Storage 解耦），避免重构扩散。

## 2. 当前基线（基于 `codex/hongbaoxia-2.0`）

- 已完成：书架、Zip 导入校验、封面策略、导入包文本阅读、按 `packId` 隔离进度与分页缓存。
- 已完成：导入包朗读基础链路（依赖 `ContentLoader.narrationUri`）与“音频缺失提示”基础处理。
- 未完成/待收口：
- 系统“打开方式/分享导入”全链路。
- 重校验能力仅覆盖基础字段，缺失音频统计未闭环。
- 横屏崩溃问题仍在用户报告中，尚未形成稳定复现与修复结论。
- 发布验收流程尚未按 2.0 跑通。

## 3. 详细任务清单

### P0-1 系统导入链路补全（打开方式/分享）

- 范围：
- 支持从系统文件管理器“打开方式”直接导入 zip 包。
- 支持其他 App 通过“分享”发送文件到本 App 导入。
- 接入导入进度/失败重试弹窗，和书架入口导入保持一致体验。
- 实施项：
- `AndroidManifest.xml` 增加 `VIEW/SEND` 相关 `intent-filter`。
- `MainActivity` 新增外部导入 `Uri` 解析与消费流程（含 `onNewIntent`）。
- 导入前尝试申请 `Uri` 读权限持久化（可用则持久化，不可用则降级继续读）。
- 验收标准：
- 从系统文件管理器点选 `.zip/.hbs.zip` 可直接拉起 App 并进入导入流程。
- 从第三方 App 分享 zip 到本 App 可导入成功。
- 导入失败时仍有可重试流程，且不崩溃。

### P0-2 重校验能力增强（补齐缺失音频统计）

- 范围：
- 现有“重新校验”除了 `hasCover/hasFlipSound/hasNarration/isValid`，补齐 `missingNarrationSentenceCount`。
- 实施项：
- `PackInspector` 新增“句子音频覆盖率检查”逻辑：
- 读取 `manifest + book.json`。
- 扫描 `audio/narration` 前缀映射（`<sentenceId>_`）。
- 计算缺失句子数。
- `MainActivity` 的重校验写回 `PackIndex` 时同步更新 `missingNarrationSentenceCount`。
- 导入器 (`ZipPackImporter`) 在导入成功写索引时初始化准确缺失统计（而不是固定 0）。
- 验收标准：
- 对“仅文本包”“部分缺失包”“完整包”重校验后，书架状态文案分别正确。
- 重新校验不改变用户阅读进度，不误删资源。

### P0-3 稳定性与回归（本轮可自动化部分）

- 范围：
- 覆盖本轮改动的编译与基础回归。
- 实施项：
- 本地执行 `assembleDebug` 确保编译通过。
- 回归验证关键路径：
- 书架导入（入口导入 + 外部导入）。
- 打开导入书阅读。
- 朗读按钮在“仅文本/部分缺失/完整音频”三类包下行为符合预期。
- 验收标准：
- Debug 构建通过。
- 手工验证路径无阻断级错误。

### P1-1 书架详情信息增强（本轮先做数据准备）

- 范围：
- 详情页需要展示缺失统计的真实数据来源。
- 实施项：
- 完成索引层字段稳定刷新，确保 UI 能读到最新 `missingNarrationSentenceCount`。
- 验收标准：
- 触发重校验后，详情页/状态文案能反映最新统计。

### P1-2 横屏崩溃专项（分阶段）

- 范围：
- 当前先完成“可定位”的日志与复现基线，不阻塞本轮主线收口。
- 实施项：
- 梳理与记录复现条件（机型、Android 版本、入口路径、操作步骤）。
- 增加关键崩溃点日志（生命周期/页面计算关键参数）以便下一轮精确修复。
- 验收标准：
- 形成可复现步骤与日志样本；下轮可据此直接修复。

### P2-1 体验增强（非阻塞发布）

- 真实翻书动画优化与高级视觉细节。
- 大包导入性能进一步优化（延迟统计/后台扫描）。

## 4. 执行顺序（本轮）

1. 完成 P0-1（外部导入链路）。
2. 完成 P0-2（缺失统计闭环）。
3. 执行 P0-3（构建与回归）。
4. 同步更新本计划文档状态（勾选已完成项，记录验证结果）。

## 5. 风险与对策

- 风险：外部分享来源 `Uri` 权限行为差异大。
- 对策：持久化权限采用 `runCatching`，失败降级为临时读取，不中断导入。
- 风险：缺失统计需解析大体积 `book.json` 可能引起卡顿。
- 对策：仅在导入完成与“手动重校验”时执行，保持 UI 线程外执行。
- 风险：现有工作区存在未提交改动。
- 对策：仅修改目标文件，避免触碰无关改动。

## 6. 完成定义（DoD）

- 代码：实现上述 P0 项并通过编译。
- 文档：本文件状态更新为“已执行结果”，记录通过/未通过项。
- 结果：用户可用书架入口与系统入口两种导入方式；重校验能给出准确缺失统计。

---

## 7. 本轮执行结果（2026-03-05）

### 已完成

- [x] P0-1 系统导入链路补全（代码已落地）
- `AndroidManifest.xml` 新增 `VIEW/SEND/SEND_MULTIPLE` 导入意图过滤。
- `MainActivity` 新增外部导入 URI 解析（`onCreate/onNewIntent`）与消费逻辑。
- 外部 URI 已接入统一导入流程（与书架“导入”同链路）。
- [x] P0-2 重校验能力增强（代码已落地）
- `PackInspector` 新增 `inspectMissingNarrationSentenceCount(root)`。
- `ZipPackImporter` 导入成功时写入真实 `missingNarrationSentenceCount`。
- 书架“重新校验”写回 `missingNarrationSentenceCount`。
- [x] P1-1 数据层准备完成
- 详情/状态展示所需的缺失统计字段可通过导入与重校验刷新。

### 未完成/阻塞

- [ ] P0-3 本地编译验证
- 阻塞原因：当前环境缺少 Java（`JAVA_HOME` 未设置），`./gradlew :app:assembleDebug` 无法执行。
- [ ] P1-2 横屏崩溃专项
- 已补充 `MainActivity.onConfigurationChanged` 方向日志，便于采样；完整复现与修复保留下一轮专项。

### 下一步建议顺序

1. 配置 `JAVA_HOME` 后先跑 `:app:assembleDebug`。
2. 真机回归：外部打开导入、分享导入、重校验缺失统计。
3. 启动横屏崩溃专项复现（按机型/系统版本分组记录）。


## 8. Execution Update (2026-03-06)
### Completed
- [x] Fixed first-launch reader initialization race for builtin pack.
- Evidence: after clean uninstall/install, log order is now `BuiltinMigrator -> ReaderViewModel.load start -> ReaderViewModel.load success`.
- Files: `app/src/main/java/com/xuyutech/hongbaoshu/MainActivity.kt`, `app/src/main/java/com/xuyutech/hongbaoshu/reader/ReaderViewModel.kt`.

### Evidence: builtin narration assets
- Verified from local assets and runtime migration log that builtin pack is missing exactly 4 narration files.
- Missing sentence IDs: `12-023-s009`, `14-001-s004`, `17-001-s006`, `26-007-s001`.
- `12-023-s009`, `14-001-s004`, `17-001-s006` map to ellipsis-only sentences in `text.json`.
- `26-007-s001` maps to the long sentence for "three rules and eight points" in `text.json`.
- Conclusion: current "audio missing" signal is backed by actual asset absence, not by loader false-positive.

### Notes
- Builtin migration now logs missing narration IDs once, so future device-side verification can use direct evidence from `logcat`.
