# 构建指南 / Building Guide

## 环境准备 / Prerequisites

### 必需软件 / Required Software

1. **Android Studio**
   - 版本: Arctic Fox (2020.3.1) 或更高
   - 下载: https://developer.android.com/studio

2. **JDK (Java Development Kit)**
   - 版本: JDK 8 或更高 (推荐 JDK 17)
   - Android Studio 通常自带 JDK

3. **Android SDK**
   - API Level 24 (Android 7.0) - 最低支持
   - API Level 34 (Android 14) - 目标版本
   - Android Studio 会自动下载所需 SDK

### 系统要求 / System Requirements

- **Windows**: Windows 10/11 (64-bit)
- **macOS**: macOS 10.14 或更高
- **Linux**: Ubuntu 18.04 或更高 (64-bit)
- **内存**: 至少 8GB RAM
- **磁盘空间**: 至少 4GB 可用空间

## 构建步骤 / Build Steps

### 方法 1: 使用 Android Studio (推荐)

1. **克隆仓库 / Clone the repository**
   ```bash
   git clone https://github.com/OMOCV/Android.git
   cd Android
   ```

2. **打开项目 / Open the project**
   - 启动 Android Studio
   - 选择 "Open an Existing Project"
   - 浏览到克隆的 `Android` 目录并打开

3. **同步 Gradle / Sync Gradle**
   - Android Studio 会自动提示同步 Gradle
   - 点击 "Sync Now" 或使用菜单: File > Sync Project with Gradle Files
   - 等待依赖下载完成

4. **配置设备 / Configure Device**
   
   **选项 A: 使用真实设备 / Use a real device**
   - 在 Android 设备上启用开发者选项和 USB 调试
   - 通过 USB 连接设备到电脑
   - 设备会出现在 Android Studio 的设备选择器中

   **选项 B: 使用模拟器 / Use an emulator**
   - 点击工具栏中的 AVD Manager 图标
   - 创建新的虚拟设备 (推荐 Pixel 4 或更高)
   - 选择系统镜像 (推荐 API 30 或更高)
   - 启动模拟器

5. **运行应用 / Run the app**
   - 点击工具栏中的绿色运行按钮 (▶️)
   - 或使用快捷键: Shift + F10 (Windows/Linux) 或 Control + R (macOS)
   - 选择目标设备
   - 等待应用构建并安装到设备

### 方法 2: 使用命令行

1. **克隆仓库 / Clone the repository**
   ```bash
   git clone https://github.com/OMOCV/Android.git
   cd Android
   ```

2. **构建 Debug APK**
   ```bash
   # Unix/Linux/macOS
   ./gradlew assembleDebug
   
   # Windows
   gradlew.bat assembleDebug
   ```

3. **构建 Release APK**
   ```bash
   # Unix/Linux/macOS
   ./gradlew assembleRelease
   
   # Windows
   gradlew.bat assembleRelease
   ```

4. **安装到设备**
   ```bash
   # 确保设备已连接并且 USB 调试已启用
   ./gradlew installDebug
   ```

5. **APK 位置 / APK Location**
   - Debug: `app/build/outputs/apk/debug/app-debug.apk`
   - Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## 签名发布版本 / Signing Release Version

### 创建密钥库 / Create Keystore

```bash
keytool -genkey -v -keystore abb-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias abb-key
```

### 配置签名 / Configure Signing

1. 在项目根目录创建 `keystore.properties` 文件:
   ```properties
   storePassword=YOUR_STORE_PASSWORD
   keyPassword=YOUR_KEY_PASSWORD
   keyAlias=abb-key
   storeFile=../abb-release-key.jks
   ```

2. 修改 `app/build.gradle.kts` 添加签名配置:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("../abb-release-key.jks")
               storePassword = "YOUR_STORE_PASSWORD"
               keyAlias = "abb-key"
               keyPassword = "YOUR_KEY_PASSWORD"
           }
       }
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               // ...
           }
       }
   }
   ```

3. 构建签名版本:
   ```bash
   ./gradlew assembleRelease
   ```

## 常见问题 / Common Issues

### 问题 1: Gradle 同步失败

**解决方案:**
- 检查网络连接
- 清除 Gradle 缓存: `./gradlew clean --no-daemon`
- 删除 `.gradle` 目录并重新同步
- 使用国内镜像 (添加到 `build.gradle.kts`):
  ```kotlin
  repositories {
      maven { url = uri("https://maven.aliyun.com/repository/google") }
      maven { url = uri("https://maven.aliyun.com/repository/public") }
      google()
      mavenCentral()
  }
  ```

### 问题 2: SDK 未找到

**解决方案:**
- 在 Android Studio 中: Tools > SDK Manager
- 安装所需的 SDK 版本 (API 34)
- 确保 `local.properties` 文件包含正确的 SDK 路径

### 问题 3: 依赖下载失败

**解决方案:**
- 使用 VPN 或代理
- 使用阿里云镜像 (见问题 1)
- 手动下载依赖并放入本地 Maven 仓库

### 问题 4: 构建缓慢

**解决方案:**
- 在 `gradle.properties` 中增加内存:
  ```properties
  org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
  org.gradle.parallel=true
  org.gradle.caching=true
  ```

## 开发环境配置 / Development Environment Setup

### 代码风格 / Code Style

项目使用 Kotlin 官方代码风格:
- 在 Android Studio: Settings > Editor > Code Style > Kotlin
- 选择 "Set from..." > "Kotlin style guide"

### 推荐插件 / Recommended Plugins

1. **Kotlin** (内置)
2. **Android SDK** (内置)
3. **Material Theme UI** - 更好的 UI 主题
4. **Rainbow Brackets** - 括号高亮
5. **GitToolBox** - 增强的 Git 集成

## 测试构建 / Testing the Build

### 单元测试 / Unit Tests

```bash
./gradlew test
```

### Android 测试 / Android Tests

```bash
# 需要连接设备或启动模拟器
./gradlew connectedAndroidTest
```

### 代码检查 / Code Inspection

```bash
./gradlew lint
```

生成的报告位于: `app/build/reports/lint-results.html`

## 持续集成 / Continuous Integration

### GitHub Actions 示例配置

创建 `.github/workflows/android.yml`:

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew assembleDebug
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

## 更多资源 / Additional Resources

- [Android 开发者文档](https://developer.android.com/docs)
- [Kotlin 语言文档](https://kotlinlang.org/docs/home.html)
- [Gradle 用户指南](https://docs.gradle.org/current/userguide/userguide.html)
- [Material Design 指南](https://material.io/design)

## 获取帮助 / Getting Help

如果遇到问题:

1. 查看 [Issues](https://github.com/OMOCV/Android/issues) 页面
2. 搜索现有问题或创建新问题
3. 提供详细的错误信息和日志
4. 包括您的环境信息 (OS, Android Studio 版本等)
