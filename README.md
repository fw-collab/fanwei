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

> 当前已实现：Room 单词本列表、下拉刷新、新增/编辑/删除、加载态和空态；复习卡片翻转、记住/忘记分支和复习完成空态；错题本列表、详情和移出错题本；通知权限申请、每日 WorkManager 复习提醒和权限关闭降级提示。数据、设置两个 Tab 仍为页面空壳。

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
| 图表 | MPAndroidChart 3.1.0（折线图/饼图/柱状图） |
| 系统能力 | TextToSpeech、ContentProvider / ContentResolver、自定义 View、属性动画 |
| 测试 | JUnit4、AndroidX Test、Espresso、Room 内存数据库 |
| 版本控制 | Git + 根目录 `.gitignore` |
| 明确不用 | Flutter、React Native、uni-app、Compose Multiplatform、WebView 套壳 |

**环境基线：** `minSdk 24`、`compileSdk 37`、`targetSdk 37`、AGP 9.4.0、Gradle 9.6.0、JDK 17+（当前使用 Android Studio 自带 JBR）。

## 4. 主 App 与伴生模块的关系

### 4.1 模块划分

| 模块 | 类型 | 职责 | 可独立安装 |
|---|---|---|---|
| `:app` | `com.android.application` | B4 全部业务；Room 数据库；只读 `WordBoardProvider` | 是 |
| `:data_board_module` | `com.android.library` | P2 看板 UI、ViewModel、Repository、图表；不含 Room | 否 |
| `:board_app` | `com.android.application` | 伴生端薄宿主：独立 `applicationId`、Launcher、主题与签名配置 | 是 |

> **关键说明：** Android Library 不能单独安装，也没有可被桌面启动的 APK。因此“`data_board_module` 为 Android Library”与“伴生模块独立可启动”要同时成立，必须增加一个极薄的 `:board_app` 宿主。业务代码和页面放在 Library，宿主只负责打包与启动。

### 4.2 数据流

```text
:app（主 App APK，com.example.wordmemory）
  B4 UI → ViewModel → Repository → Room（word_memory.db，唯一数据源）
                                      │
                                      ▼
                            WordBoardProvider（只读 ContentProvider）
                                      ▲
                                      │ ContentResolver 查询
:board_app（伴生 APK，com.example.wordmemory.board） → :data_board_module（看板 UI + Repository）
```

### 4.3 “读取主 App Room 数据库”的实现口径

Android 应用沙箱不允许另一个 APK 直接打开 `/data/data/com.example.wordmemory/databases/word_memory.db`。本项目采用 **主 App 进程内只读 Provider** 的方式：

1. Room 仍是唯一数据源，所有学习数据只由主 App 写入。
2. 主 App 在自身进程内用 Room DAO 查询，并通过 `WordBoardProvider` 暴露为只读 URI。
3. `:data_board_module` 的 Repository 通过 `ContentResolver` 查询，不依赖 Room，也不接触数据库文件。
4. Provider 使用 `signature` 级自定义权限，仅允许同一签名证书的伴生端读取；不开放写权限。
5. 主 App 需已安装并至少启动过一次以完成建库；被系统强制停止时，Provider 需等主 App 进程恢复后才可查询。

**明确不采用的方案：** 直接打开主 App 数据库文件、`sharedUserId`、把数据库放到外部存储、在伴生端另建一份 Room 副本。这些方案分别存在沙箱限制、弃用/安全、并发损坏和数据不一致问题。

## 5. 仓库结构

```text
danci/
├─ app/                     # B4 主 App（已创建，可运行）
├─ data_board_module/       # P2 Android Library（M3 创建）
├─ board_app/               # P2 独立宿主（M3 创建）
├─ docs/
│  ├─ 01-选题报审.md
│  ├─ 02-AndroidStudio项目初始化.md
│  ├─ 03-数据共享契约.md
│  └─ 04-数据模型设计.md
├─ app/schemas/             # Room 导出的数据库 schema
├─ gradle/wrapper/          # Gradle Wrapper
├─ gradlew
├─ gradlew.bat
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle.properties
├─ .gitignore
└─ README.md
```

当前 B4 主 App 已创建并可运行；M2 数据层、M3 底部导航、M4 单词本 CRUD、M5 艾宾浩斯算法、M6 复习卡片流程、M7 错题本、M7.5 通知与 WorkManager 提醒已完成；P2 的两个模块待后续按文档创建。

### 运行 B4 主 App

1. Android Studio 打开 `D:\danci`（不是子目录）。
2. 等待 Gradle Sync 完成。
3. 运行配置选择 `app`。
4. 设备选择 `Pixel 5 (emulator-5554)`，点击 Run。

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
| M8 | P2 看板 UI、图表、独立宿主；数据/设置与答辩材料 | 可演示的完整双端项目 |

## 7. 文档索引

- [选题报审（教师审核版）](docs/01-选题报审.md)
- [Android Studio 新建项目与模块配置步骤](docs/02-AndroidStudio项目初始化.md)
- [主 App 与伴生端数据共享契约](docs/03-数据共享契约.md)
- [里程碑 2：数据模型设计](docs/04-数据模型设计.md)
- [里程碑 5：艾宾浩斯遗忘曲线算法](docs/05-艾宾浩斯算法.md)
- [M7.5：通知权限与 WorkManager 复习提醒](docs/06-通知与定时提醒.md)
