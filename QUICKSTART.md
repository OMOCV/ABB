# 快速开始指南 / Quick Start Guide

欢迎使用 ABB Robot Program Reader！本指南将帮助您快速上手。

## 5 分钟快速开始 / 5-Minute Quick Start

### 第一步: 安装应用 (1 分钟)

#### 方式 A: 从源代码构建
```bash
# 克隆仓库
git clone https://github.com/OMOCV/Android.git
cd Android

# 使用 Android Studio 打开
# 或使用命令行构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

#### 方式 B: 从 Release 下载
1. 访问 [Releases 页面](https://github.com/OMOCV/Android/releases)
2. 下载最新的 APK 文件
3. 在 Android 设备上安装

### 第二步: 启动应用 (30 秒)

1. 在应用列表中找到 "ABB Robot Program Reader"
2. 点击图标启动应用
3. 如果是首次使用，应用会请求文件访问权限
4. 点击"允许"授予权限

### 第三步: 打开示例文件 (1 分钟)

1. 点击屏幕上的 **"选择 ABB 程序文件"** 按钮
2. 浏览到示例文件位置
   - 推荐先尝试: `sample_program.mod`
   - 其他示例: `examples/pick_and_place.mod`
3. 选择文件
4. 等待应用解析（通常在 1 秒内完成）

### 第四步: 探索功能 (2 分钟)

#### 查看文件信息
```
顶部显示:
✓ 文件名: sample_program.mod
✓ 文件类型: MOD
```

#### 浏览模块列表
```
模块卡片显示:
✓ MainModule
  - 类型: NOSTEPIN
  - 例行程序: 5
  - 变量: 8
```

#### 检查例行程序
```
例行程序列表显示:
✓ main (PROC, 参数: 0, 行: 23-45)
✓ initialize_robot (PROC, 参数: 0, 行: 48-55)
✓ calculate_value (FUNC, 参数: 2, 行: 89-98)
```

#### 查看代码
- 向下滚动查看完整的带语法高亮的代码
- 观察不同颜色代表不同的代码元素
- 点击任一例行程序名称查看其具体代码

### 第五步: 尝试其他功能 (1 分钟)

1. **点击例行程序**: 在例行程序列表中点击任一项
   - 代码区域会更新显示该例行程序的代码
   - 底部会显示 Toast 提示

2. **水平滚动**: 在代码区域左右滑动
   - 查看长代码行的完整内容

3. **打开其他文件**: 点击顶部按钮选择其他示例文件
   - 尝试 `examples/pick_and_place.mod`
   - 尝试 `examples/welding.mod`
   - 尝试 `examples/math_utils.mod`

## 常见使用场景 / Common Use Cases

### 场景 1: 学习 ABB RAPID 编程

**目标**: 理解 RAPID 语法和程序结构

**步骤**:
1. 打开 `sample_program.mod`
2. 阅读注释（灰色文本）了解代码说明
3. 观察语法高亮，识别不同的代码元素
4. 对比模块、过程、函数的不同结构
5. 尝试理解控制流语句（IF, WHILE, FOR）

**关键观察点**:
- 蓝色关键字: MODULE, PROC, IF, WHILE
- 紫色指令: MoveJ, MoveL, TPWrite
- 绿色字符串: "文本内容"
- 灰色注释: ! 注释说明

### 场景 2: 分析现有程序

**目标**: 快速了解程序功能和结构

**步骤**:
1. 打开要分析的 .mod 文件
2. 查看模块列表，了解程序组织方式
3. 浏览例行程序列表，识别主要功能
4. 点击感兴趣的例行程序查看实现
5. 注意变量声明和数据类型

**关键技巧**:
- 主程序通常命名为 `main`
- FUNC 返回值，PROC 不返回值
- TRAP 用于异常处理
- VAR 是局部变量，PERS 是持久变量

### 场景 3: 检查代码语法

**目标**: 验证代码是否符合 RAPID 语法

**步骤**:
1. 打开待检查的文件
2. 观察语法高亮是否正常
3. 检查关键字是否正确显示为蓝色
4. 确认字符串被正确识别（绿色）
5. 查看模块和例行程序是否被正确识别

**常见语法错误指示**:
- 关键字未高亮: 可能拼写错误
- 字符串未变绿: 可能缺少引号
- 模块/例行程序未列出: 可能结构不完整

### 场景 4: 对比不同的编程风格

**目标**: 学习不同的代码组织方式

**步骤**:
1. 打开 `examples/pick_and_place.mod`
   - 观察实际应用的结构
2. 打开 `examples/welding.mod`
   - 注意工艺控制的实现
3. 打开 `examples/math_utils.mod`
   - 学习函数库的设计
4. 对比它们的:
   - 变量组织方式
   - 函数命名规范
   - 注释风格
   - 错误处理方法

## 提示和技巧 / Tips and Tricks

### 💡 提高阅读效率

1. **先看结构，后看细节**
   - 首先浏览模块和例行程序列表
   - 理解程序的整体结构
   - 然后深入查看具体实现

2. **利用颜色快速定位**
   - 紫色 = 机器人指令（重点关注）
   - 蓝色 = 控制结构（了解逻辑）
   - 绿色 = 用户消息（理解输出）

3. **关注注释**
   - 注释（灰色）通常包含重要说明
   - 了解程序员的意图和设计思路

### 💡 组织您的文件

建议的文件夹结构:
```
/ABBPrograms/
  ├── Projects/
  │   ├── Project1/
  │   │   ├── main.mod
  │   │   └── utils.mod
  │   └── Project2/
  │       └── program.mod
  ├── Examples/
  │   ├── sample_program.mod
  │   └── pick_and_place.mod
  └── Templates/
      └── template.mod
```

### 💡 学习路径

**初学者**:
1. 从 `sample_program.mod` 开始
2. 理解基本语法和结构
3. 尝试阅读和理解每个例行程序
4. 注意语法高亮的颜色含义

**中级用户**:
1. 研究 `examples/pick_and_place.mod`
2. 理解实际应用的编程模式
3. 学习工具和位置数据的使用
4. 观察错误处理和安全措施

**高级用户**:
1. 分析 `examples/welding.mod` 的工艺控制
2. 研究 `examples/math_utils.mod` 的模块化设计
3. 对比不同的编程风格和最佳实践
4. 考虑如何应用到自己的项目

## 故障排除 / Troubleshooting

### 问题: 应用无法打开文件

**可能原因和解决方案**:

1. **权限未授予**
   - 设置 > 应用 > ABB Robot Program Reader > 权限
   - 启用存储权限

2. **不支持的文件格式**
   - 确认文件扩展名为 .mod, .prg, 或 .sys
   - 文件名区分大小写

3. **文件损坏**
   - 尝试用文本编辑器打开文件验证
   - 确保文件编码为 UTF-8 或 ASCII

### 问题: 代码高亮不正确

**可能原因和解决方案**:

1. **语法不标准**
   - 检查 RAPID 语法是否正确
   - 确保关键字拼写正确

2. **特殊字符**
   - 某些特殊字符可能影响解析
   - 尝试删除或替换特殊字符

### 问题: 模块或例行程序未显示

**可能原因和解决方案**:

1. **语法结构不完整**
   - 确保有 MODULE ... ENDMODULE
   - 确保有 PROC ... ENDPROC 或 FUNC ... ENDFUNC

2. **注释或字符串中包含关键字**
   - 这是正常现象，解析器会正确处理

## 下一步 / Next Steps

### 深入学习
- 📖 阅读 [README.md](README.md) 了解完整功能
- 📖 查看 [EXAMPLES.md](EXAMPLES.md) 了解示例详情
- 📖 参考 [UI_GUIDE.md](UI_GUIDE.md) 了解界面细节

### 参与贡献
- 🤝 阅读 [CONTRIBUTING.md](CONTRIBUTING.md)
- 🐛 报告 Bug 或建议功能
- 💻 提交代码改进

### 获取帮助
- 💬 在 [Issues](https://github.com/OMOCV/Android/issues) 提问
- 📧 联系维护者
- 🌟 给项目加星支持

## 额外资源 / Additional Resources

### ABB RAPID 学习资源
- [ABB RobotStudio 官网](https://new.abb.com/products/robotics/robotstudio)
- [RAPID 编程手册](https://library.abb.com)
- [ABB 开发者中心](https://developercenter.robotstudio.com)

### Android 开发资源
- [Android 开发者文档](https://developer.android.com)
- [Material Design 指南](https://material.io/design)
- [Kotlin 语言文档](https://kotlinlang.org/docs)

## 反馈 / Feedback

您的反馈对我们很重要！

- ⭐ 喜欢这个应用？给我们一个星标！
- 💡 有建议？创建一个 Issue
- 🐛 发现 Bug？报告给我们
- 📝 想要新功能？告诉我们

---

**开始您的 ABB RAPID 编程之旅吧！Happy Coding! 祝编程愉快！** 🚀
