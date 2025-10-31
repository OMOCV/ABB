# ABB Robot Program Reader for Android

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

4. 构建并运行:
```bash
./gradlew assembleDebug
```

或者在 Android Studio 中点击 Run 按钮

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

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 作者

OMOCV

## 致谢

- ABB Robotics 的 RAPID 编程语言规范
- Android 开发社区
- Material Design 组件库

