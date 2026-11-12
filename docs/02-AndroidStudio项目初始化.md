# Android Studio 新建项目与模块配置步骤

> 目标：在 `D:\danci` 生成 `:app`、`:data_board_module`、`:board_app` 三个模块的工程骨架。  
> 本文只包含工程配置，不包含业务代码；业务实现从 M2 开始。

## 0. 前置条件

- Android Studio Ladybug（2024.2.1）或更高版本。
- Android SDK Platform 35、Build-Tools 35、JDK 17。
- Git（用于版本管理；未安装时先在 Android Studio 的 Version Control > Git 中配置）。
- 根目录已有的 `README.md`、`docs/`、`.gitignore` 必须保留。

## 1. 新建主工程 `:app`

1. Android Studio 选择 **New Project**。
2. 模板选择 **Phone and Tablet > Empty Views Activity**（不要选 Compose 模板）。
3. 填写：
   - Name：`WordMemoryB4`
   - Package name：`com.example.wordmemory`
   - Save location：`D:\danci`
   - Language：`Kotlin`
   - Minimum SDK：`API 24`
   - Build configuration language：`Kotlin DSL`
4. 点击 Finish，等待 Gradle Sync 完成。
5. 若向导提示目录非空：保留现有 `README.md`、`docs/`、`.gitignore`；可先在临时目录生成工程，再把 `app/`、`gradle/`、`gradlew`、`gradlew.bat`、`build.gradle.kts`、`settings.gradle.kts`、`gradle.properties` 复制到 `D:\danci`，不要覆盖文档和 `.gitignore`。
6. 检查 `app/build.gradle.kts`：
   - `namespace = "com.example.wordmemory"`
   - `applicationId = "com.example.wordmemory"`
   - `minSdk = 24`、`compileSdk = 35`、`targetSdk = 35`

## 2. 新建 `:data_board_module`（Android Library）

1. **File > New > New Module**。
2. 选择 **Android Library**。
3. 填写：
   - Module name：`data_board_module`
   - Package name：`com.example.databoard`
   - Language：`Kotlin`
   - Minimum SDK：`API 24`
   - Build configuration language：`Kotlin DSL`
4. Finish。
5. 确认 `data_board_module/build.gradle.kts` 使用的是 `com.android.library`，不是 `com.android.application`。

> 该模块是 Library，**不能单独安装或从桌面启动**，页面和逻辑放在这里，由 `:board_app` 打包启动。
>
> 如果课程验收强制要求该模块“本身可安装”，只能把插件改成 `com.android.application`，但这会违反“新建 Android Library 模块”的要求，也会让主 App 与伴生端无法共享同一套页面代码；因此本项目选择“Library + 薄宿主”的方案。

## 3. 新建 `:board_app`（独立宿主）

1. **File > New > New Module**。
2. 选择 **Phone and Tablet > Empty Views Activity**。
3. 填写：
   - Module name：`board_app`
   - Package name：`com.example.wordmemory.board`
   - Language：`Kotlin`
   - Minimum SDK：`API 24`
   - Build configuration language：`Kotlin DSL`
4. 在 `board_app/build.gradle.kts` 中设置独立 `applicationId`：

```kotlin
android {
    namespace = "com.example.wordmemory.board"
    defaultConfig {
        applicationId = "com.example.wordmemory.board"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
```

5. 删除向导生成的 `MainActivity` 及其 `MAIN/LAUNCHER` intent-filter（或移除它的 Launcher 属性），确保最终只有一个 Launcher 指向 `DashboardActivity`，否则桌面会出现两个图标。
6. 宿主只保留 Manifest、主题、签名配置和启动声明；看板页面、ViewModel、Repository、图表全部放在 `:data_board_module`。

## 4. 声明三个模块

编辑根目录 `settings.gradle.kts`：

```kotlin
rootProject.name = "WordMemoryB4"

include(":app")
include(":data_board_module")
include(":board_app")
```

同时确认仓库配置包含 Google、Maven Central 和 JitPack（MPAndroidChart 需要）：

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

## 5. 配置模块依赖

### 5.1 根工程插件版本（示例）

若工程使用 Version Catalog，请把版本写入 `gradle/libs.versions.toml`，不要在根工程重复声明。以下为可直接使用的基线：

```kotlin
// 根 build.gradle.kts
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
```

> 若 Android Studio 向导生成了更新的 AGP/Kotlin，请保持向导版本；KSP 版本必须与 Kotlin 版本匹配。

### 5.2 `:app`：Room 与基础依赖

`app/build.gradle.kts`：

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.wordmemory"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wordmemory"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { viewBinding = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

ksp {
    // 导出 Room schema，供迁移测试与版本审查使用
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
```

### 5.3 `:data_board_module`：看板 UI 与图表

`data_board_module/build.gradle.kts`：

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.databoard"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures { viewBinding = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
```

> `:data_board_module` **不添加 Room 依赖**，它只能通过 `ContentResolver` 读取主 App 的数据，避免在伴生端形成第二份数据库。

### 5.4 `:board_app`：只依赖看板 Library

`board_app/build.gradle.kts` 的核心依赖：

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.wordmemory.board"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wordmemory.board"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":data_board_module"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
```

> 依赖方向是 `:board_app → :data_board_module`。`:app` 不依赖看板 Library，保持主 App 独立、体积最小；若后续想让主 App 内也嵌入看板，再给 `:app` 增加 `implementation(project(":data_board_module"))` 和入口即可，但不是本项目必需。

## 6. 主 App：声明只读 Provider

M2 创建 `WordBoardProvider` 后，在 `app/src/main/AndroidManifest.xml` 中加入：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <permission
        android:name="com.example.wordmemory.permission.READ_BOARD"
        android:protectionLevel="signature" />

    <application ...>
        <provider
            android:name=".data.WordBoardProvider"
            android:authorities="com.example.wordmemory.boardprovider"
            android:exported="true"
            android:readPermission="com.example.wordmemory.permission.READ_BOARD" />
    </application>
</manifest>
```

要求：

- 只声明 `readPermission`，不声明 `writePermission`；
- Provider 只实现 `query()`，其余写方法直接抛出 `UnsupportedOperationException`；
- `android:exported="true"` 必须显式写出；
- authority 必须与 `:data_board_module` 中使用的常量完全一致。

## 7. 伴生端：声明 Launcher 与读权限

在 `board_app/src/main/AndroidManifest.xml` 中：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="com.example.wordmemory.permission.READ_BOARD" />

    <application ...>
        <activity
            android:name="com.example.databoard.DashboardActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

说明：

- `DashboardActivity` 位于 `:data_board_module`，由 `:board_app` 的 Manifest 声明为 Launcher；该类必须是 `public`。
- Library 的 Manifest 若也声明了该 Activity，宿主只补充 `intent-filter` 即可，Manifest Merger 会按类名合并，不要重复定义同名 Activity 的不同实现。
- 这样 Library 提供页面，宿主提供独立 APK 与桌面入口；
- 若使用宿主空壳 `MainActivity` 承载 `DashboardFragment`，则 Launcher 指向宿主 Activity，效果相同。

## 8. 签名配置（signature 权限的关键）

- **Debug：** 同一工程内两个 App 模块默认使用同一 debug keystore，`signature` 权限可直接工作。
- **Release：** `:app` 与 `:board_app` 必须使用**同一个签名证书**，否则伴生端查询会被拒绝。
- **安装顺序：** 先安装主 App，再安装伴生端；若先装伴生端，系统可能尚未识别主 App 定义的自定义权限，建议卸载后按“主 App → 伴生端”重装。
- 不要把 keystore、密码提交到 Git；使用 `keystore.properties` 并在 `.gitignore` 中忽略。
- 用 Android Studio 的 **Build > Generate Signed Bundle / APK** 分别打包时，两个模块选择同一个 keystore。

## 9. `.gitignore` 配置

根目录已提供 `.gitignore`，只维护这一份，三个模块不要再各自重复创建。至少覆盖以下内容：

```gitignore
.gradle/
**/build/
.idea/
*.iml
local.properties
keystore.properties
*.jks
*.keystore
.kotlin/
*.apk
*.aab
*.log
```

验证忽略规则是否生效：

```powershell
git check-ignore -v app\build .idea local.properties
```

三条命令均有输出即表示规则生效。

## 10. 初始化 Git 仓库

```powershell
cd D:\danci
git init
git config user.name "你的名字"
git config user.email "你的邮箱"
git add .
git commit -m "chore: init repo and M1 docs"
```

若 Git 未安装，先安装 Git for Windows，或在 Android Studio 的 **Settings > Version Control > Git** 中指定 `git.exe` 路径。

## 11. 验收清单

- [ ] `./gradlew :app:assembleDebug` 通过。
- [ ] `./gradlew :data_board_module:assembleDebug` 通过。
- [ ] `./gradlew :board_app:assembleDebug` 通过。
- [ ] `:data_board_module` 使用 `com.android.library`，没有 Launcher。
- [ ] `:board_app` 使用 `com.android.application`，`applicationId` 与主 App 不同，并有 Launcher。
- [ ] `adb shell pm list packages | findstr wordmemory` 能看到两个包名。
- [ ] 主 App 启动并产生数据后，伴生端能显示概览与图表。
- [ ] 主 App 未安装/无数据时，伴生端显示空态而不是崩溃。
- [ ] 工程依赖中没有 Flutter、React Native、uni-app 等跨平台组件。

## 12. 常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| `data_board_module` 无法运行 | Android Library 没有独立 APK | 安装并启动 `:board_app`，不要直接运行 Library |
| `SecurityException: Permission Denial` | 两个 APK 签名不一致，或伴生端缺少 `uses-permission` | 使用同一签名证书；检查 Manifest 权限声明 |
| `IllegalArgumentException: Unknown URL` | authority 拼写不一致，或主 App 未安装 | 统一 authority；主 App 至少启动一次 |
| `ClassNotFoundException: DashboardActivity` | 宿主 Manifest 未声明，或 Library 未被打包 | 检查 `board_app` 对 `:data_board_module` 的 `implementation` 依赖与 Activity 全限定名 |
| JitPack 依赖解析失败 | 未配置仓库或网络问题 | 在 `settings.gradle.kts` 加 `maven("https://jitpack.io")` 后重新 Sync |
| KSP 报 Kotlin 版本不匹配 | KSP 与 Kotlin 版本不一致 | 按 Kotlin 版本选择对应的 KSP 版本 |
