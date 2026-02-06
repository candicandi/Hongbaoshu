# 红宝匣 2.0 UI 视觉升级方案

目标：保持阅读页的“纸感与秩序感”，全面提升书架与控制模块的视觉精致度，让整体更像现代精品阅读器。

---

## 1. 设计方向（选型）

### 1.1 核心气质

- 关键词：**温和 / 低对比 / 自然 / 纸感**
- 视觉策略：阅读页保持温润、安静；控制与书架也采用同一套柔和材质语言，避免金属感与高对比。

### 1.2 视觉风格标签

- “自然书房” + “纸纤维质感”
- 低饱和、低对比；用细微层级与纸质纹理区分模块

---

## 2. 字体与排版（建议）

### 2.1 字体对

- 书名/标题：`Noto Serif SC`（或 `Source Han Serif SC`）
- UI/按钮/标签：`MiSans` 或 `HarmonyOS Sans`（可替换为 `Source Han Sans SC`）

原则：标题与正文保持衬线一致，工具层用干净无衬线，避免与阅读内容抢戏。

### 2.2 字号与层级

- H1（书架页大标题）：22–24sp
- H2（分组/区块标题）：18sp
- 卡片标题：16–17sp
- 标签/辅助：12–13sp

---

## 3. 色彩体系（建议）

### 3.1 基础色

- 背景主色：`#F6F1E7`（纸感米白）
- 次级背景：`#EFE8DB`（分区/卡片）
- 文字主色：`#2E2A24`
- 文字次色：`#5F584F`

### 3.2 点缀色（自然系）

- 主强调：`#A58B6F`（柔和木色）
- 深强调：`#7D6A54`

### 3.3 辅助状态色

- 成功/完整：`#5E7C67`（低饱和绿）
- 警告/缺失：`#9A6D4B`（低饱和赭）
- 错误/不可用：`#8D5A5A`（低饱和红）

---

## 4. 关键组件风格规范

### 4.1 书架卡片

- 卡片形态：圆角 14–16dp
- 阴影：极轻微（1–2dp）或使用低对比内描边
- 封面区域 2:3 比例，外围 1dp 细描边
- 状态徽章：胶囊形（12–14sp），纸感半透明底

### 4.2 顶部工具栏

- 使用柔和半透明底（alpha 0.9），无高光
- 标题左对齐，右侧导入按钮用“描边 + 木色文字”

### 4.3 导入流程面板

- 步进式卡片：导入中 / 校验中 / 成功 / 失败
- 状态图标使用线性图标，颜色与状态一致

### 4.4 控制面板（朗读/字体/夜间）

- “底部半透明面板 + 圆角 20dp”
- 控件之间留白大，避免“工具条”压迫感
- 滑块样式：细轨道 + 木色圆点

---

## 5. 动效与交互

- 书架卡片入场：淡入 + 8dp 上移
- 面板弹出：下→上，260ms easing
- 状态徽章：轻微“呼吸”透明度变化（2.5s）
- 书架切换时不要大面积动效，避免破坏沉浸感

---

## 6. 重要界面升级规划

### 6.1 Bookshelf（书架）

- 两列网格 + 顶部搜索/导入
- 书架空态：插画式占位 + 文案引导
- 卡片底部展示作者与资源状态

### 6.2 Import（导入流程）

- 独立“导入进度页”或底部 Sheet
- 失败提示含明确原因与“重试”按钮

### 6.3 Reader 控制面板

- 朗读控制：播放/暂停/上一句/下一句按钮采用柔和圆形纸感按钮
- 字体设置：分段式滑块，左右显示字号示意
- 夜间切换：图标切换时带轻微旋转动效

---

## 7. Compose 落地要点

- 统一 Theme tokens：`Colors / Typography / Shapes / Spacing`
- 书架卡片、按钮、标签单独封装为组件
- 所有状态色统一从 `ColorScheme` 读取
- 动效使用 `AnimatedVisibility` + `tween`

---

## 8. 设计验收清单

- 工具栏与面板无“廉价按钮感”
- 书架卡片层级明确、信息密度合理
- 状态徽章与缺失提示清晰可见
- 夜间模式下同样保持高质感

---

## 9. Theme Tokens（Compose 可落地）

### 9.1 Color Tokens（Light）

- `bgPrimary`: `#F6F1E7`
- `bgSecondary`: `#EFE8DB`
- `bgSurface`: `#F2ECE0`
- `textPrimary`: `#2E2A24`
- `textSecondary`: `#5F584F`
- `textTertiary`: `#8A8074`
- `accentPrimary`: `#A58B6F`
- `accentDeep`: `#7D6A54`
- `borderSoft`: `#E1D7C8`
- `chipBg`: `#E9E0D2`

### 9.2 Status Tokens

- `success`: `#5E7C67`
- `warning`: `#9A6D4B`
- `error`: `#8D5A5A`

### 9.3 Color Tokens（Dark）

- `bgPrimary`: `#1C1A17`
- `bgSecondary`: `#25221D`
- `bgSurface`: `#2B2721`
- `textPrimary`: `#EDE7DC`
- `textSecondary`: `#C6BDAF`
- `textTertiary`: `#9E9486`
- `accentPrimary`: `#B59A7A`
- `accentDeep`: `#8B755C`
- `borderSoft`: `#3A342D`
- `chipBg`: `#332E27`

---

## 10. Typography Tokens（Compose 建议）

- `display`: `Noto Serif SC`, 24sp, weight 600
- `headline`: `Noto Serif SC`, 20sp, weight 600
- `title`: `Noto Serif SC`, 18sp, weight 500
- `body`: `Noto Serif SC`, 16sp, weight 400, lineHeight 1.6
- `uiTitle`: `MiSans` 或 `HarmonyOS Sans`, 16sp, weight 500
- `uiBody`: `MiSans` 或 `HarmonyOS Sans`, 14sp, weight 400
- `caption`: `MiSans` 或 `HarmonyOS Sans`, 12sp, weight 400

---

## 11. Shapes & Spacing Tokens

- `radiusCard`: 16dp
- `radiusPanel`: 20dp
- `radiusChip`: 999dp
- `spacingXS`: 4dp
- `spacingS`: 8dp
- `spacingM`: 12dp
- `spacingL`: 16dp
- `spacingXL`: 24dp

---

## 12. 组件级样式细化

### 12.1 BookshelfCard

- 外层：`bgSurface` + `borderSoft` 1dp
- 封面：2:3 比例 + 2dp 内圆角
- 标题：`uiTitle`
- 作者：`caption` + `textSecondary`
- 状态徽章：`chipBg` 背景 + 1dp 描边

### 12.2 PrimaryButton（导入/主要操作）

- 形态：描边按钮 `borderSoft` 1dp
- 文字：`accentPrimary`
- 背景：透明
- 按压态：背景加 4% `accentPrimary` 透明度

### 12.3 ControlPanel（朗读/字体/夜间）

- 背景：`bgSecondary` + 0.95 透明度
- 顶部阴影：2dp
- 按钮：圆形，背景 `bgSurface`，文字 `textPrimary`
- 滑块：轨道 `borderSoft`，thumb `accentPrimary`

---

## 13. 动效规格（建议）

- 卡片入场：`fadeIn + slideY(8dp)`，220ms
- Panel 弹出：`slideInVertically`，260ms
- Toggle：`crossfade`，180ms
