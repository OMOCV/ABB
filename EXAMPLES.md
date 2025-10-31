# ABB RAPID 程序示例 / ABB RAPID Program Examples

本目录包含多个 ABB RAPID 编程语言的示例程序，用于演示应用程序的功能。

## 示例文件 / Example Files

### 1. sample_program.mod
**主示例程序 - Main Sample Program**

这是一个综合性示例，展示了 RAPID 编程的各种特性:

- ✅ 模块定义 (MODULE/ENDMODULE)
- ✅ 变量声明 (VAR, PERS, CONST)
- ✅ 过程定义 (PROC/ENDPROC)
- ✅ 函数定义 (FUNC/ENDFUNC)
- ✅ 陷阱例程 (TRAP/ENDTRAP)
- ✅ 控制结构 (IF-ELSEIF-ELSE, FOR, WHILE, TEST-CASE)
- ✅ 机器人运动指令 (MoveJ, MoveL)
- ✅ I/O 操作 (SetDO, SetAO)
- ✅ 时间控制 (WaitTime)
- ✅ 消息输出 (TPWrite)

**关键特性:**
```rapid
MODULE MainModule
    VAR num counter := 0;
    PERS robtarget home := [...];
    
    PROC main()
        ! 主程序逻辑
    ENDPROC
    
    FUNC num calculate_value(num input1, num input2)
        ! 函数实现
    ENDFUNC
ENDMODULE
```

### 2. examples/pick_and_place.mod
**拾取和放置应用 - Pick and Place Application**

实际的工业应用示例，展示拾取和放置操作:

**功能:**
- 系统初始化
- 工具和位置数据配置
- 拾取零件
- 放置零件
- 夹具控制
- 循环执行

**关键概念:**
- 工具数据 (tooldata)
- 机器人目标位置 (robtarget)
- 速度数据 (speeddata)
- 区域数据 (zonedata)
- 相对位置计算

**代码片段:**
```rapid
PROC pick_part()
    MoveJ RelTool(pick_pos, 0, 0, 100), v_fast, z_smooth, gripper;
    MoveL pick_pos, v_slow, z_fine, gripper;
    close_gripper;
    MoveL RelTool(pick_pos, 0, 0, 100), v_slow, z_smooth, gripper;
ENDPROC
```

### 3. examples/welding.mod
**焊接应用 - Welding Application**

焊接工艺的自动化程序示例:

**功能:**
- 焊接参数设置
- 焊接路径规划
- 电弧控制
- 多道次焊接
- 冷却时间管理
- 紧急停止处理

**关键概念:**
- 模拟量输出 (SetAO)
- 数字量输出 (SetDO)
- 位置偏移 (Offs)
- 陷阱例程用于紧急处理

**代码片段:**
```rapid
PROC weld_seam()
    MoveL weld_start, v100, fine, tool0;
    start_weld;
    MoveL weld_end, v50, fine, tool0;
    stop_weld;
ENDPROC
```

### 4. examples/math_utils.mod
**数学工具模块 - Math Utilities Module**

可重用的数学函数库:

**功能:**
- 距离计算
- 角度和弧度转换
- 数组平均值计算
- 线性插值
- 数值范围检查和限制

**关键概念:**
- 模块化设计
- 函数库创建
- 数学常量定义
- 数组操作
- 实用工具函数

**代码片段:**
```rapid
FUNC num calculate_distance(robtarget p1, robtarget p2)
    VAR num dx, dy, dz;
    dx := p2.trans.x - p1.trans.x;
    dy := p2.trans.y - p1.trans.y;
    dz := p2.trans.z - p1.trans.z;
    RETURN Sqrt(dx * dx + dy * dy + dz * dz);
ENDFUNC
```

## 如何使用示例 / How to Use Examples

### 在应用中查看 / View in App

1. 启动 ABB Robot Program Reader 应用
2. 点击"选择 ABB 程序文件"
3. 浏览到示例文件位置
4. 选择任一 .mod 文件
5. 查看:
   - 语法高亮的代码
   - 识别的模块
   - 识别的例行程序
   - 变量声明

### 在 ABB RobotStudio 中使用 / Use in ABB RobotStudio

1. 打开 ABB RobotStudio
2. 创建新的系统或打开现有系统
3. 在 RAPID 编辑器中打开示例文件
4. 加载到虚拟控制器
5. 测试和调试

### 学习建议 / Learning Tips

#### 初学者 / Beginners
1. 从 `sample_program.mod` 开始
2. 理解基本结构和语法
3. 实验修改简单的值
4. 观察语法高亮如何工作

#### 中级用户 / Intermediate
1. 研究 `pick_and_place.mod`
2. 理解实际应用的结构
3. 学习工具和位置数据配置
4. 尝试修改路径和参数

#### 高级用户 / Advanced
1. 分析 `welding.mod` 的工艺控制
2. 研究 `math_utils.mod` 的模块化设计
3. 创建自己的实用函数库
4. 结合多个模块构建复杂应用

## 语法高亮演示 / Syntax Highlighting Demo

应用程序会将这些示例文件以不同颜色显示:

- **关键字** (蓝色): `MODULE`, `PROC`, `FUNC`, `IF`, `FOR`, `WHILE`
- **数据类型** (蓝灰色): `num`, `bool`, `robtarget`, `speeddata`
- **函数/指令** (紫色): `MoveJ`, `MoveL`, `SetDO`, `TPWrite`
- **字符串** (绿色): `"文本内容"`
- **注释** (灰色): `! 注释内容`
- **数字** (品红色): `123`, `45.6`, `3.14`

## 代码结构识别 / Code Structure Recognition

应用程序会自动识别并显示:

### 模块信息
```
模块名称: MainModule
类型: NOSTEPIN
例行程序数量: 5
变量数量: 8
```

### 例行程序信息
```
名称: main
类型: PROC
参数: 0
局部变量: 1
代码行: 23-45
```

## 扩展示例 / Extending Examples

您可以基于这些示例创建自己的程序:

### 添加新功能
```rapid
! 在 pick_and_place.mod 中添加质量检查
PROC check_quality()
    VAR bool quality_ok;
    
    ! 检查逻辑
    quality_ok := check_sensor();
    
    IF NOT quality_ok THEN
        TPWrite "质量检查失败!";
        discard_part;
    ENDIF
ENDPROC
```

### 创建新模块
```rapid
MODULE CustomModule
    ! 您的自定义代码
    
    PROC custom_procedure()
        ! 实现您的逻辑
    ENDPROC
ENDMODULE
```

## 测试文件格式 / Test File Formats

示例包含以下文件格式:

| 格式 | 描述 | 用途 |
|------|------|------|
| .mod | 模块文件 | 包含 RAPID 代码的标准模块 |
| .prg | 程序文件 | 可执行的程序文件 |
| .sys | 系统文件 | 系统配置和参数 |

所有格式都被应用程序完全支持。

## 常见问题 / Common Questions

### Q: 这些示例能在真实机器人上运行吗？
A: 这些示例是教学用途的简化版本。在真实机器人上使用前需要:
- 校准工具和工件坐标系
- 调整位置数据
- 配置 I/O 信号
- 进行安全评估

### Q: 如何添加自己的示例？
A: 
1. 创建 .mod 文件
2. 使用标准 RAPID 语法
3. 将文件放在 `examples/` 目录
4. 在应用中打开查看

### Q: 应用支持哪些 RAPID 版本？
A: 应用程序解析器支持 RAPID 4.0 及更高版本的标准语法。

## 更多资源 / Additional Resources

- [ABB RAPID 编程手册](https://library.abb.com)
- [RobotStudio 用户手册](https://new.abb.com/products/robotics/robotstudio)
- [ABB 开发者中心](https://developercenter.robotstudio.com)

## 反馈 / Feedback

如果您创建了有用的示例程序，欢迎分享:
1. Fork 项目
2. 添加您的示例到 `examples/` 目录
3. 更新本文档
4. 提交 Pull Request

---

**Happy Coding with ABB RAPID! 祝 ABB RAPID 编程愉快！** 🤖
