# B4 单词记忆 + P2 数据看板

> 课程设计 · M1 仓库初始化 + 选题报审；M2 Room 数据层；M3 底部导航；M4 单词本 CRUD  
> 技术路线：Kotlin 原生 Android Studio，不使用 Flutter / React Native / uni-app 等跨平台框架。

## 1. 项目简介

**一句话定位：** 一套以“背—练—测—复盘”闭环为核心的 Kotlin 原生单词记忆 App，并用一个可独立启动的数据看板把学习过程可视化。

- **主 App（B4 单词记忆，`:app`）**：负责词库、学习计划、卡片学习、间隔复习、测验、学习记录与设置，是学习数据的唯一写入方。
- **伴生端（P2 数据看板，`:data_board_module` + `:board_app`）**：以独立 APK 启动，只读展示主 App 的学习概览、趋势、掌握度、词书进度和难词榜。
- **数据边界**：主 App 的 Room 数据库是唯一数据源；伴生端不建库、不复制数据库文件，通过主 App 提供的只读 `ContentProvider` 读取数据。

当前 B4 主 App 已完成 Room 数据层、底部导航骨架和单词本 CRUD：单词列表、下拉刷新、新增/编辑/删除、加载态/空态。P2 伴生端和复习/测验功能按后续里程碑补齐。

## 2. 功能清单

### 2.1 主 App：B4 单词记忆

| 编号 | 功能 | 说明 |
|---|---|---|
| B4-01 | 词库与单词管理 | 内置示例词库；单词增、删、改、查；按词书/首字母搜索；收藏与掌握状态标记 |
| B4-02 | 每日学习计划 | 设置每日新词数与复习数；自定义进度环展示今日完成进度与连续学习天数 |
| B4-03 | 卡片学习 | 单词/释义/音标/例句双面卡片；系统 TTS 发音；认识/不认识操作；翻转动画 |
| B4-04 | 间隔复习与错词本 | 简化 SM-2 间隔重复；到期复习队列；答错自动进入错词本；复习结果回写 |
| B4-05 | 测验 | 中英选择、拼写两种题型；随机组卷；交卷评分；错题与正确答案回顾 |
| B4-06 | 学习记录与统计 | 每日新学/复习数量、正确率、连续天数；按日查看历史记录 |
| B4-07 | 设置与数据管理 | 每日目标、TTS 开关、主题模式、学习数据重置、关于页面 |

> 当前已实现：Room 单词本列表、下拉刷新、新增/编辑/删除、加载态和空态；复习卡片翻转、记住/忘记分支和复习完成空态；错题本列表、详情、移出错题本和错题卡片分享；通知权限申请、每日 WorkManager 复习提醒和权限关闭降级提示；所有主页面及看板统一接入 StateLayout 四态。数据、设置两个 Tab 仍为页面空壳。

### 2.2 伴生端：P2 数据看板

| 编号 | 功能 | 说明 |
|---|---|---|
| P2-01 | 独立启动 | `:board_app` 拥有独立 `applicationId` 和 Launcher，可从桌面直接打开，不依赖主 App 界面 |
| P2-02 | 今日概览 | 总词数、已学、已掌握、今日到期、连续学习天数 |
| P2-03 | 学习趋势 | 近 7/30 天新学与复习数量折线图，辅助观察学习节奏 |
| P2-04 | 掌握度分布 | 未学/学习中/已掌握三档占比图 |
| P2-05 | 词书进度 | 各词书的总词数、已学数、掌握数与完成进度 |
| P2-06 | 难词 Top 10 | 按错误次数排序，展示单词、词书、错误次数、最近错误时间 |
| P2-07 | 只读、刷新与空态 | 仅查询不写入；下拉刷新；主 App 未安装/无数据/无权限时展示明确空态 |

## 3. 技术栈

| 层次 | 选型 |
|---|---|
| 语言 | Kotlin 2.0+ |
| IDE / 构建 | Android Studio（Ladybug 及以上）、Gradle 9.6.0、AGP 9.4.0、Kotlin DSL |
| UI | XML + ViewBinding + Material 3 + RecyclerView + Navigation + 自定义 View |
| 架构 | MVVM + Repository + StateFlow/Flow；单词本、复习、错题本已接入 |
| 本地存储 | Room 2.8.5 + KSP；旧 StudyActivity 的 SharedPreferences 仅作为未使用原型保留 |
| 异步 | Repository 使用 `suspend` + `Dispatchers.IO`，DAO 列表使用 `Flow` |
| 算法 | 简化艾宾浩斯：5min–30d 共 9 级，答对升级、答错重置 |
| 后台任务 | WorkManager 2.10.0，每日 20:00 查询待复习单词 |
| 系统通知 | NotificationChannel + Android 13+ POST_NOTIFICATIONS 动态权限 |
| 文件导出 | MediaStore.Downloads（Android 10+），CSV 复习记录导出 |
| 图片分享 | Canvas Bitmap 生成错题卡片 + FileProvider + 系统分享 |
| 图表 | MPAndroidChart 3.1.0（折线图/饼图/柱状图） |
| 系统能力 | TextToSpeech、ContentProvider / ContentResolver、自定义 View、属性动画 |
| 测试 | JUnit4、AndroidX Test、Espresso、Room 内存数据库 |
| 版本控制 | Git + 根目录 `.gitignore` |
| 明确不用 | Flutter、React Native、uni-app、Compose Multiplatform、WebView 套壳 |

**环境基线：** `minSdk 24`、`compileSdk 37`、`targetSdk 37`、AGP 9.4.0、Gradle 9.6.0、JDK 17+（当前使用 Android Studio 自带 JBR）。

## 4. 主 App 与伴生模块的关系

### 4.1 模块划分

| 模块 | 类型 | 职责 | 启动方式 |
|---|---|---|---|
| `:app` | `com.android.application` | B4 全部业务；依赖 `:core_data` 和 `:data_board_module` | 主 Launcher |
| `:core_data` | `com.android.library` | Room 实体、DAO、`AppDatabase`，主 App 与看板共用 | 不单独启动 |
| `:core_ui` | `com.android.library` | StateLayout、ToastUtils、统一字体/间距/状态样式 | 不单独启动 |
| `:data_board_module` | `com.android.library` | `BoardMainActivity`、看板 UI、`BoardViewModel`、`BoardRepository` | **第二个 Launcher 入口** |

> **关键说明：** `:data_board_module` 是独立 Gradle Module，但以 Android Library 形式被主 App 依赖并合入同一个 APK。`BoardMainActivity` 在合并后的 Manifest 中声明了 `MAIN` / `LAUNCHER`，因此桌面会显示“数据看板”入口，可以单独点击启动。两个 Activity 同 APK、同 UID、同进程，直接共享 `word_memory.db`。

### 4.2 数据流

```text
:app APK（com.example.wordmemory，同一进程）
  ├─ MainActivity             B4 单词记忆
  └─ BoardMainActivity        P2 数据看板（独立 Launcher）
          │
          ▼
     BoardViewModel → BoardRepository
          │
          ▼
     :core_data AppDatabase（Room，word_memory.db）
          ▲
          │
     B4 Repository / DAO
```

### 4.3 数据库访问方式

1. Room 仍是唯一数据源，数据库名 `word_memory.db`，由 `:core_data` 统一提供。
2. 主 App 和看板都依赖 `:core_data`，使用同一个 `AppDatabase` 类和同一个数据库文件。
3. 看板与主 App 同进程，直接调用 DAO 做只读统计，不复制数据库、不通过 ContentProvider。
4. 这样“独立 Module + 独立 Launcher”和“同进程共享数据库”可以同时成立。
5. 如果未来要求看板作为完全独立的 APK 安装，则需要改为 ContentProvider 或 sharedUserId 方案，并处理跨进程/跨 UID 的权限问题；当前里程碑按同进程实现。

## 5. 仓库结构

```text
danci/
├─ app/                     # B4 主 App（含 P2 独立 Launcher 入口）
├─ core_data/               # Room 实体、DAO、AppDatabase（共享数据层）
├─ core_ui/                 # StateLayout、ToastUtils、统一 UI 规范
├─ data_board_module/       # P2 Android Library：BoardMainActivity + 看板 UI
├─ docs/
│  ├─ 01-选题报审.md
│  ├─ 02-AndroidStudio项目初始化.md
│  ├─ 03-数据共享契约.md
│  ├─ 04-数据模型设计.md
│  ├─ 05-艾宾浩斯算法.md
│  ├─ 06-通知与定时提醒.md
│  ├─ 07-数据看板模块.md
│  ├─ 08-错题卡片分享.md
│  ├─ 09-统一状态与UI规范.md
│  └─ 10-课程设计开发文档.md
├─ core_data/schemas/       # Room 导出的数据库 schema
├─ gradle/wrapper/          # Gradle Wrapper
├─ gradlew
├─ gradlew.bat
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle.properties
├─ .gitignore
└─ README.md
```

当前 B4 主 App 已创建并可运行；M2 数据层、M3 底部导航、M4 单词本 CRUD、M5 艾宾浩斯算法、M6 复习卡片流程、M7 错题本、M7.5 通知与 WorkManager 提醒、M8 数据看板/图表/CSV、自选错题卡片分享、全局 StateLayout 已完成。

### 运行 B4 主 App

1. Android Studio 打开 `D:\danci`（不是子目录）。
2. 等待 Gradle Sync 完成。
3. 运行配置选择 `app`。
4. 设备选择 `Pixel 5 (emulator-5554)`，点击 Run。

### 单独启动 P2 数据看板

桌面会出现第二个入口“数据看板”，直接点击即可；也可以在 Android Studio 中运行 `app` 后通过以下命令启动：

```bash
adb shell am start -n com.example.wordmemory/com.example.wordmemory.board.BoardMainActivity
```

## 6. 里程碑

| 阶段 | 内容 | 产出 |
|---|---|---|
| M1 | 仓库初始化、选题报审、模块与数据共享方案 | 本文档、报审文档、初始化步骤、`.gitignore` |
| M2 | Android Studio 工程骨架、B4 可运行原型、Room 三表、DAO、Database | B4 原型可运行；Room 数据层已完成；Provider 待补齐 |
| M3 | B4 主 App 底部导航（单词本/复习/错题本/数据/设置）、五个 Fragment 空壳、蓝橙主题 | 五 Tab 可切换的导航骨架 |
| M4 | WordRepository、WordListViewModel、单词列表、下拉刷新、新增/编辑/删除、空态/加载态 | 单词本基础 CRUD 完成 |
| M5 | EbbinghausUtil、0–8 级复习间隔、答对升级/答错重置、待复习队列查询、单元测试 | 简化艾宾浩斯算法完成 |
| M6 | 复习卡片翻转、记住/忘记分支、事务更新 Word + ReviewRecord、答错写入 WrongRecord、复习完成空态 | 复习主流程联调完成 |
| M7 | 错题本列表（按单词聚合错误次数/最近时间）、错题详情、移出错题本、列表→详情导航 | 错题展示管理完成 |
| M7.5 | Android 13+ 通知权限、每日 WorkManager 提醒、NotificationChannel、权限关闭降级提示 | 后台提醒与降级方案完成 |
| M8 | P2 `data_board_module`、独立 Launcher、共享 `core_data` Room、MPAndroidChart 柱状图/饼图、CSV 导出 | 看板骨架、图表与文件导出完成 |
| 自选 | 错题卡片 Bitmap 生成、FileProvider、系统分享 | 错题图片卡片分享完成 |
| 全局 | StateLayout 四态、统一 Toast/字体/间距、错误重试、ViewModel 生命周期 | 全局状态 UI 统一完成 |

## 7. 文档索引

- [选题报审（教师审核版）](docs/01-选题报审.md)
- [Android Studio 新建项目与模块配置步骤](docs/02-AndroidStudio项目初始化.md)
- [主 App 与伴生端数据共享契约](docs/03-数据共享契约.md)
- [里程碑 2：数据模型设计](docs/04-数据模型设计.md)
- [里程碑 5：艾宾浩斯遗忘曲线算法](docs/05-艾宾浩斯算法.md)
- [M7.5：通知权限与 WorkManager 复习提醒](docs/06-通知与定时提醒.md)
- [M8：P2 数据看板模块](docs/07-数据看板模块.md)
- [自选：错题卡片图片分享](docs/08-错题卡片分享.md)
- [统一状态与 UI 规范](docs/09-统一状态与UI规范.md)
- [课程设计开发文档（可直接导出 PDF）](docs/10-课程设计开发文档.md)
