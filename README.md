# ABB Robot Program Reader for Android

[![Build APK](https://github.com/OMOCV/Android/actions/workflows/build-apk.yml/badge.svg)](https://github.com/OMOCV/Android/actions/workflows/build-apk.yml)
[![Release](https://github.com/OMOCV/Android/actions/workflows/release.yml/badge.svg)](https://github.com/OMOCV/Android/actions/workflows/release.yml)
[![GitHub release](https://img.shields.io/github/v/release/OMOCV/Android)](https://github.com/OMOCV/Android/releases/latest)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

这是一个用于读取和查看 ABB 机器人程序的 Android 应用程序。

## 功能特性

- ✅ 支持读取 ABB 机器人程序文件 (.mod, .prg, .sys)
- ✅ 识别和显示模块 (MODULE)
- ✅ 识别和显示例行程序 (PROC, FUNC, TRAP)
- ✅ RAPID 语言语法高亮显示
- ✅ 文件浏览器选择功能
- ✅ 变量和参数识别

## 支持的文件格式

应用程序支持所有 ABB RAPID 编程语言的标准文件格式:

1. **.mod** - 模块文件 (Module files)
2. **.prg** - 程序文件 (Program files)
3. **.sys** - 系统文件 (System files)

## 技术实现

### 架构组件

- **ABBParser** - 解析 RAPID 代码，识别模块、例行程序和变量
- **ABBSyntaxHighlighter** - 语法高亮引擎，支持关键字、数据类型、函数、字符串、注释和数字的高亮
- **ABBDataModels** - 数据模型类 (ABBModule, ABBRoutine, ABBProgramFile)
- **CodeElementAdapter** - RecyclerView 适配器用于显示代码元素
- **MainActivity** - 主活动，处理文件选择和显示

### 语法高亮支持的元素

1. **关键字** (蓝色): MODULE, PROC, FUNC, IF, FOR, WHILE, 等
2. **数据类型** (蓝灰色): num, bool, string, robtarget, speeddata, 等
3. **函数** (紫色): MoveJ, MoveL, WaitTime, SetDO, 等
4. **字符串** (绿色): "..."
5. **注释** (灰色): ! ...
6. **数字** (品红色): 123, 45.6, 等

### 识别的代码结构

#### 模块 (Modules)
```rapid
MODULE ModuleName
    ! 模块内容
ENDMODULE
```

#### 例行程序 (Routines)

1. **PROC** - 过程
```rapid
PROC ProcName()
    ! 过程代码
ENDPROC
```

2. **FUNC** - 函数
```rapid
FUNC num FuncName()
    ! 函数代码
    RETURN value;
ENDFUNC
```

3. **TRAP** - 陷阱例程
```rapid
TRAP TrapName
    ! 陷阱代码
ENDTRAP
```

## 权限要求

应用程序需要以下权限来访问文件系统:

- `READ_EXTERNAL_STORAGE` (Android 6-12)
- `READ_MEDIA_*` (Android 13+)
- `MANAGE_EXTERNAL_STORAGE` (可选，用于完整文件访问)

## 构建项目

### 环境要求

- Android Studio Arctic Fox 或更高版本
- JDK 8 或更高版本
- Android SDK API 34
- Gradle 8.2
- Kotlin 1.9.20

### 构建步骤

1. 克隆仓库:
```bash
git clone https://github.com/OMOCV/Android.git
cd Android
```

2. 使用 Android Studio 打开项目

3. 同步 Gradle 并下载依赖

4. 构建 APK 或 AAB:
```bash
# 构建 APK
./gradlew assembleDebug

# 构建 AAB (Android App Bundle)
./gradlew bundleDebug
```

或者在 Android Studio 中点击 Run 按钮

## 下载安装 / Download & Install

### 从 GitHub Releases 下载 / Download from GitHub Releases

最简单的方式是从 [Releases](https://github.com/OMOCV/Android/releases) 页面下载最新版本的文件：

1. 访问 [最新版本](https://github.com/OMOCV/Android/releases/latest)
2. 下载文件：
   - **APK 文件** - 可直接安装到 Android 设备
   - **AAB 文件** - 用于 Google Play Store 发布或使用 bundletool 安装
3. 在 Android 设备上安装 APK，或使用 bundletool 从 AAB 生成 APK

### 使用 GitHub Actions 构建

项目配置了自动构建工作流，可以在 GitHub Actions 中构建 APK 和 AAB:

#### 开发构建 / Development Builds

1. 进入仓库的 [Actions](https://github.com/OMOCV/Android/actions) 标签页
2. 选择 "Build APK and AAB" 工作流
3. 点击 "Run workflow" 手动触发构建
4. 构建完成后，从 Artifacts 下载生成的 APK 或 AAB 文件

#### 正式发布 / Release Builds

创建版本发布：

```bash
# 创建版本标签
git tag -a v1.0.0 -m "Release version 1.0.0"

# 推送标签到 GitHub
git push origin v1.0.0
```

推送标签后，GitHub Actions 会自动：
- 构建 Debug 和 Release APK
- 构建 Debug 和 Release AAB
- 创建 GitHub Release
- 上传 APK 和 AAB 文件到 Release
- 生成 SHA256 校验文件

输出文件位置:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Debug AAB: `app/build/outputs/bundle/debug/app-debug.aab`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

更多构建选项请参见 [BUILDING.md](BUILDING.md) 和 [BUILD_RESTRICTED.md](BUILD_RESTRICTED.md)

## 使用方法

1. 启动应用
2. 点击"选择 ABB 程序文件"按钮
3. 从文件浏览器中选择 .mod, .prg 或 .sys 文件
4. 应用将显示:
   - 文件信息
   - 识别的模块列表
   - 识别的例行程序列表
   - 带语法高亮的完整代码内容
5. 点击例行程序可以查看该例行程序的具体代码

## 项目结构

```
ABB/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/com/omocv/abb/
│   │       │   ├── MainActivity.kt          # 主活动
│   │       │   ├── ABBParser.kt            # RAPID 解析器
│   │       │   ├── ABBSyntaxHighlighter.kt # 语法高亮引擎
│   │       │   ├── ABBDataModels.kt        # 数据模型
│   │       │   └── CodeElementAdapter.kt    # RecyclerView 适配器
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   ├── activity_main.xml   # 主布局
│   │       │   │   └── item_code_element.xml # 列表项布局
│   │       │   ├── values/
│   │       │   │   ├── strings.xml         # 字符串资源
│   │       │   │   ├── colors.xml          # 颜色资源
│   │       │   │   └── themes.xml          # 主题定义
│   │       │   └── xml/
│   │       │       ├── data_extraction_rules.xml
│   │       │       └── backup_rules.xml
│   │       └── AndroidManifest.xml         # 应用清单
│   ├── build.gradle.kts                    # 应用级构建配置
│   └── proguard-rules.pro                  # ProGuard 规则
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts                        # 项目级构建配置
├── settings.gradle.kts                     # Gradle 设置
├── gradle.properties                       # Gradle 属性
├── gradlew                                 # Gradle Wrapper (Unix)
├── gradlew.bat                            # Gradle Wrapper (Windows)
└── README.md                              # 本文件
```

## ABB RAPID 语言支持

应用程序解析和识别以下 RAPID 语言元素:

### 关键字
- 结构: MODULE, ENDMODULE, PROC, ENDPROC, FUNC, ENDFUNC, TRAP, ENDTRAP
- 变量: VAR, PERS, CONST, ALIAS, LOCAL, TASK
- 控制流: IF, THEN, ELSEIF, ELSE, ENDIF, FOR, FROM, TO, STEP, DO, ENDFOR, WHILE, ENDWHILE, TEST, CASE, DEFAULT, ENDTEST
- 跳转: GOTO, LABEL, RETURN, EXIT
- 逻辑: TRUE, FALSE, AND, OR, NOT, XOR

### 数据类型
num, bool, string, pos, orient, pose, confdata, robtarget, jointtarget, speeddata, zonedata, tooldata, wobjdata, loaddata, clock, intnum

### 常用指令
MoveJ, MoveL, MoveC, MoveAbsJ, WaitTime, SetDO, SetAO, Reset, TPWrite, TPReadNum, TPReadFK, Open, Close, Write, Read, AccSet, VelSet, ConfJ, ConfL, SingArea, PathAccLim, StartLoad, WaitLoad, EOffsOn, EOffsOff, EOffsSet

## 示例 ABB 程序

以下是一个示例 RAPID 程序，展示了应用程序可以识别的元素:

```rapid
MODULE MainModule
    ! 声明变量
    VAR num counter := 0;
    PERS robtarget target1 := [[600, 0, 600], [1, 0, 0, 0], [0, 0, 0, 0], [9E9, 9E9, 9E9, 9E9, 9E9, 9E9]];
    
    ! 主程序
    PROC main()
        counter := 0;
        WHILE counter < 10 DO
            MoveJ target1, v1000, z50, tool0;
            WaitTime 1;
            counter := counter + 1;
        ENDWHILE
        
        TPWrite "Program completed";
    ENDPROC
    
    ! 辅助函数
    FUNC num calculate(num a, num b)
        VAR num result;
        result := a + b;
        RETURN result;
    ENDFUNC
    
    ! 错误处理陷阱
    TRAP error_trap
        TPWrite "Error occurred!";
        Stop;
    ENDTRAP
ENDMODULE
```

## 贡献

欢迎贡献! 请遵循以下步骤:

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

更多详情请参见 [CONTRIBUTING.md](CONTRIBUTING.md)

## 文档 / Documentation

- 📖 [README.md](README.md) - 项目介绍 / Project introduction
- 🔨 [BUILDING.md](BUILDING.md) - 构建指南 / Build guide
- 🚀 [QUICK_RELEASE.md](QUICK_RELEASE.md) - 快速发布指南 / Quick release guide
- 📋 [RELEASE.md](RELEASE.md) - 完整发布指南 / Complete release guide
- 📊 [BUILD_PUBLISH_SUMMARY.md](BUILD_PUBLISH_SUMMARY.md) - 构建发布总结 / Build & publish summary
- 📝 [CHANGELOG.md](CHANGELOG.md) - 变更日志 / Changelog
- 💡 [EXAMPLES.md](EXAMPLES.md) - 使用示例 / Usage examples
- 🤝 [CONTRIBUTING.md](CONTRIBUTING.md) - 贡献指南 / Contributing guide

## 🔐 Privacy / 隐私

- [隐私政策 (中文)](https://omocv.github.io/Android/privacy-policy-zh.html)
- [Privacy Policy (English)](https://omocv.github.io/Android/privacy-policy-en.html)

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 作者

OMOCV

## 致谢

- ABB Robotics 的 RAPID 编程语言规范
- Android 开发社区
- Material Design 组件库

