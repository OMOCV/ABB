# 贡献指南 / Contributing Guidelines

感谢您对 ABB Robot Program Reader 项目的关注！我们欢迎各种形式的贡献。

## 如何贡献 / How to Contribute

### 报告 Bug / Reporting Bugs

在创建 Bug 报告之前，请先搜索现有的 Issues 以避免重复。

创建 Bug 报告时，请包含:

- **清晰的标题和描述**
- **重现步骤**
- **预期行为**
- **实际行为**
- **截图** (如果适用)
- **环境信息**:
  - Android 版本
  - 设备型号
  - 应用版本

**Bug 报告模板:**

```markdown
### 问题描述
简要描述问题

### 重现步骤
1. 打开应用
2. 点击...
3. 观察到...

### 预期行为
应该发生什么

### 实际行为
实际发生了什么

### 环境
- Android 版本: 
- 设备: 
- 应用版本: 

### 截图
如果适用，添加截图帮助解释问题
```

### 建议功能 / Suggesting Features

我们欢迎新功能建议！请创建一个 Issue，包含:

- **功能描述**: 清楚地描述您想要的功能
- **使用场景**: 解释为什么这个功能有用
- **可能的实现**: 如果您有想法，请分享

### 提交代码 / Submitting Code

#### 开发流程

1. **Fork 仓库**
   - 点击 GitHub 页面右上角的 "Fork" 按钮

2. **克隆您的 Fork**
   ```bash
   git clone https://github.com/YOUR-USERNAME/Android.git
   cd Android
   ```

3. **创建功能分支**
   ```bash
   git checkout -b feature/your-feature-name
   # 或
   git checkout -b fix/your-bug-fix
   ```

4. **进行更改**
   - 编写代码
   - 遵循代码风格指南
   - 添加或更新测试
   - 更新文档

5. **提交更改**
   ```bash
   git add .
   git commit -m "描述性的提交信息"
   ```

6. **推送到您的 Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

7. **创建 Pull Request**
   - 访问原始仓库
   - 点击 "New Pull Request"
   - 选择您的分支
   - 填写 PR 描述

## 代码风格指南 / Code Style Guide

### Kotlin 代码风格

遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)

**关键点:**

1. **命名规范**
   ```kotlin
   // 类名使用 PascalCase
   class ABBParser { }
   
   // 函数和变量使用 camelCase
   fun parseFile(file: File) { }
   val fileName = "example.mod"
   
   // 常量使用大写加下划线
   const val MAX_FILE_SIZE = 1024 * 1024
   ```

2. **缩进**
   - 使用 4 个空格 (不使用 Tab)
   
3. **行长度**
   - 最大 120 字符

4. **注释**
   ```kotlin
   /**
    * 解析 ABB RAPID 文件
    * 
    * @param file 要解析的文件
    * @return 解析后的程序文件对象
    */
   fun parseFile(file: File): ABBProgramFile {
       // 实现...
   }
   ```

### XML 代码风格

1. **属性顺序**
   ```xml
   <View
       android:id="@+id/viewId"
       android:layout_width="match_parent"
       android:layout_height="wrap_content"
       android:padding="16dp"
       app:customAttribute="value"
       tools:text="Preview text" />
   ```

2. **缩进**
   - 使用 4 个空格

3. **资源命名**
   - Layout: `activity_main.xml`, `fragment_detail.xml`, `item_list.xml`
   - ID: `btnSubmit`, `tvTitle`, `rvList`
   - Colors: `abb_primary`, `syntax_keyword`
   - Strings: `app_name`, `error_message`, `btn_submit`

## 提交信息规范 / Commit Message Guidelines

使用清晰、描述性的提交信息:

### 格式

```
<类型>: <简短描述>

<详细描述 (可选)>

<相关 Issue (可选)>
```

### 类型

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式 (不影响功能)
- `refactor`: 代码重构
- `test`: 添加或修改测试
- `chore`: 构建过程或辅助工具的变动

### 示例

```
feat: 添加对 .prg 文件的支持

- 实现 .prg 文件解析器
- 添加 .prg 特定的语法高亮规则
- 更新文档说明支持的文件格式

Closes #123
```

## Pull Request 指南 / Pull Request Guidelines

### PR 检查清单

在提交 PR 之前，请确保:

- [ ] 代码遵循项目的代码风格
- [ ] 所有测试通过
- [ ] 添加了新功能的测试
- [ ] 更新了相关文档
- [ ] 提交信息清晰明了
- [ ] PR 描述完整

### PR 模板

```markdown
## 变更类型
- [ ] Bug 修复
- [ ] 新功能
- [ ] 文档更新
- [ ] 性能改进
- [ ] 其他 (请说明):

## 变更描述
简要描述您的更改

## 相关 Issue
Closes #(issue number)

## 测试
描述您如何测试这些更改

## 截图
如果适用，添加截图

## 检查清单
- [ ] 我的代码遵循项目的代码风格
- [ ] 我已运行测试，所有测试通过
- [ ] 我已更新了相关文档
- [ ] 我的更改不引入新的警告
```

## 测试指南 / Testing Guidelines

### 编写测试

1. **单元测试**
   ```kotlin
   @Test
   fun `parseFile should correctly parse module name`() {
       // Arrange
       val content = "MODULE TestModule\nENDMODULE"
       val file = createTempFile()
       file.writeText(content)
       
       // Act
       val result = abbParser.parseFile(file)
       
       // Assert
       assertEquals(1, result.modules.size)
       assertEquals("TestModule", result.modules[0].name)
   }
   ```

2. **UI 测试**
   ```kotlin
   @Test
   fun testFileSelection() {
       onView(withId(R.id.btnSelectFile))
           .perform(click())
       
       // 验证文件选择器打开
   }
   ```

### 运行测试

```bash
# 单元测试
./gradlew test

# Android 测试
./gradlew connectedAndroidTest
```

## 文档贡献 / Documentation Contributions

文档与代码同样重要！您可以帮助:

- 修正错别字和语法错误
- 改进现有文档的清晰度
- 添加新的示例和教程
- 翻译文档到其他语言

## 代码审查 / Code Review

所有提交都需要经过代码审查。审查者会:

- 检查代码质量
- 验证功能是否正常工作
- 确保遵循项目标准
- 提供建设性反馈

作为贡献者:
- 对反馈保持开放态度
- 及时回应审查意见
- 根据反馈进行必要的修改

## 行为准则 / Code of Conduct

### 我们的承诺

为了营造开放和友好的环境，我们承诺:

- 使用友好和包容的语言
- 尊重不同的观点和经验
- 优雅地接受建设性批评
- 关注对社区最有利的事情
- 对其他社区成员表示同理心

### 不可接受的行为

- 使用性化语言或图像
- 挑衅、侮辱或贬损性评论
- 公开或私下骚扰
- 未经明确许可发布他人的私人信息
- 其他在专业环境中可能被认为不当的行为

## 获取帮助 / Getting Help

如果您需要帮助:

1. 查看 [README.md](README.md) 和 [BUILDING.md](BUILDING.md)
2. 搜索现有的 Issues
3. 在 [Discussions](https://github.com/OMOCV/Android/discussions) 中提问
4. 创建新的 Issue (如果是 bug 或功能请求)

## 许可证 / License

通过贡献代码，您同意您的贡献将根据 MIT 许可证进行授权。

## 感谢 / Thank You

感谢您花时间为这个项目做出贡献！每一个贡献，无论大小，都帮助使这个项目变得更好。

---

**Happy Coding! 祝编程愉快！** 🚀
