# Changelog / 更新日志

All notable changes to the ABB Robot Program Reader project will be documented in this file.

本文件记录 ABB 机器人程序读取器项目的所有重要更改。

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.2.0] - 2025-11-10

### Added / 新增
- ✅ 液态玻璃主题效果 (Glassmorphism theme effect)
  - 半透明背景效果
  - 毛玻璃卡片样式
  - 现代化的视觉设计
  - 支持深色和浅色模式
- ✅ 代码编辑功能 (Code editing functionality)
  - 启用/禁用编辑模式
  - 实时代码编辑
  - 保存代码更改
  - 未保存更改提示
- ✅ 代码替换功能 (Code replace functionality)
  - 搜索和替换对话框
  - 全局替换所有匹配项
  - 在例行程序中替换
  - 在模块中替换
  - 显示替换计数
- ✅ 增强的语法检查 (Enhanced syntax checking)
  - 全面的语法验证
  - 检测所有代码块配对（MODULE/ENDMODULE, PROC/ENDPROC, FUNC/ENDFUNC, TRAP/ENDTRAP, IF/ENDIF, FOR/ENDFOR, WHILE/ENDWHILE, TEST/ENDTEST）
  - 检测变量名有效性
  - 检测未关闭的代码块
  - 详细的错误报告（包含行号和错误信息）
- ✅ 实时语法检查 (Real-time syntax checking)
  - 编辑时自动检查语法
  - 延迟检查避免性能问题
  - 即时错误反馈
- ✅ 文件名保持功能 (Preserve original filename)
  - 使用内容解析器获取真实文件名
  - 不再使用随机临时文件名
  - 保持原始文件名用于显示和存储

### Changed / 更改
- 优化主题设计，采用流行的液态玻璃效果
- 改进语法检查算法，提供更准确的验证
- 优化语法错误消息的准确性，精确定位错误位置而非泛指 (Improved syntax error message accuracy to pinpoint exact error locations instead of vague descriptions)
  - 区分"无开放块"和"错误的END语句"两种情况
  - 显示开放块的具体行号和类型
  - 明确指出期望的END语句类型
- 增强代码查看器功能，支持编辑和替换
- 改进文件处理逻辑，保持原始文件名

### Fixed / 修复
- 修复文件打开时文件名被修改的问题
- 修复语法检查不准确的问题

### Technical Details / 技术细节
- 新增 `SyntaxError` 数据类用于存储语法错误信息
- 增强 `ABBParser.validateSyntax()` 方法提供全面的语法验证
- 更新 `CodeViewerActivity` 支持编辑和替换功能
- 新增 `dialog_replace.xml` 布局文件
- 更新颜色和主题资源支持液态玻璃效果
- 添加 EditText 支持代码编辑
- 实现 TextWatcher 用于实时语法检查

## [2.1.0] - 2025-11-09

### Added / 新增
- ✅ 全屏代码查看器 (Full-screen code viewer)
  - 文件内容全屏显示，不在特定框架内
  - 显示行号
  - 支持水平和垂直滚动
- ✅ 代码搜索功能 (Code search functionality)
  - 搜索对话框
  - 高亮显示搜索结果
  - 显示匹配数量
- ✅ 代码导出功能 (Code export functionality)
  - 通过分享功能导出代码
  - 支持导出到其他应用
- ✅ 深色主题 (Dark theme)
  - 支持系统主题切换
  - 手动切换深色/浅色模式
  - 主题偏好持久化
- ✅ 最近打开文件列表 (Recent files list)
  - 保存最近打开的文件
  - 快速访问最近文件
  - 最多保存10个最近文件
- ✅ 代码书签功能 (Code bookmarks)
  - 添加书签到特定行
  - 查看和跳转到书签
  - 书签持久化存储
- ✅ 语法错误检测 (Syntax error detection)
  - 检测MODULE/ENDMODULE匹配
  - 检测PROC/ENDPROC匹配
  - 显示语法错误列表
- ✅ 代码格式化功能 (Code formatting)
  - 自动缩进
  - 格式化代码结构
  - 提高代码可读性
- ✅ 国际化支持 (Internationalization)
  - 英语界面支持
  - 中文界面支持
  - 根据系统语言自动切换

### Changed / 更改
- 优化代码显示方式，采用全屏布局
- 改进用户界面，增加工具栏菜单
- 增强语法高亮效果

### Technical Details / 技术细节
- 新增 `CodeViewerActivity` 用于全屏代码查看
- 使用 SharedPreferences 存储用户偏好和书签
- 实现菜单系统用于快速访问功能
- 支持深色主题的资源文件

## [1.1.0] - 2025-11-06

### Added / 新增
- ✅ GitHub Actions 自动构建和发布工作流
  - 自动构建 Debug 和 Release APK
  - 版本标签触发自动发布
  - 生成 SHA256 校验和文件
  - 自动创建 GitHub Release
  - APK 文件自动上传到 Release
- ✅ 发布文档
  - RELEASE.md - 完整的发布流程指南
  - QUICK_RELEASE.md - 快速发布指南
  - BUILD_PUBLISH_SUMMARY.md - 构建发布总结
  - APK 签名配置说明
  - 版本管理最佳实践
- ✅ README 增强
  - 添加构建状态徽章
  - 添加发布版本徽章
  - 添加许可证徽章
  - 添加下载和安装说明
  - 完善发布流程文档
- ✅ CI/CD 工作流
  - .github/workflows/build-apk.yml - 开发构建工作流
  - .github/workflows/release.yml - 正式发布工作流
  - 支持手动触发构建
  - 自动构建产物上传

### Changed / 更改
- 更新 BUILDING.md 添加发布和分发章节
- 增强文档体系，提供中英文双语支持

## [1.0.0] - 2025-10-31

### Added / 新增
- ✅ 完整的 Android 应用程序结构
- ✅ ABB RAPID 文件解析器 (ABBParser)
  - 支持 .mod (模块) 文件
  - 支持 .prg (程序) 文件
  - 支持 .sys (系统) 文件
- ✅ 语法高亮引擎 (ABBSyntaxHighlighter)
  - 关键字高亮
  - 数据类型高亮
  - 函数/指令高亮
  - 字符串高亮
  - 注释高亮
  - 数字高亮
- ✅ 模块识别功能
  - MODULE/ENDMODULE 解析
  - 模块类型检测
  - 模块变量提取
- ✅ 例行程序识别
  - PROC/ENDPROC 解析
  - FUNC/ENDFUNC 解析
  - TRAP/ENDTRAP 解析
  - 参数和局部变量识别
- ✅ 用户界面
  - Material Design 3 主题
  - 文件选择功能
  - 模块列表展示
  - 例行程序列表展示
  - 代码内容显示（带语法高亮）
  - RecyclerView 适配器
- ✅ 文件访问权限
  - Android 6-12 存储权限支持
  - Android 13+ 媒体权限支持
  - 可选的完整文件访问权限
- ✅ 文档
  - README.md (中英文)
  - BUILDING.md (构建指南)
  - CONTRIBUTING.md (贡献指南)
  - EXAMPLES.md (示例文档)
  - CHANGELOG.md (本文件)
- ✅ 示例程序
  - sample_program.mod (综合示例)
  - pick_and_place.mod (拾取放置)
  - welding.mod (焊接应用)
  - math_utils.mod (数学工具)
- ✅ 项目配置
  - Gradle 8.2 构建系统
  - Kotlin 1.9.20
  - Android Gradle Plugin 8.1.4
  - 最小 SDK: API 24 (Android 7.0)
  - 目标 SDK: API 34 (Android 14)

### Technical Details / 技术细节
- **架构**: MVVM 设计模式
- **语言**: Kotlin
- **UI 框架**: Material Design Components
- **依赖**:
  - androidx.core:core-ktx:1.12.0
  - androidx.appcompat:appcompat:1.6.1
  - com.google.android.material:material:1.11.0
  - androidx.constraintlayout:constraintlayout:2.1.4
  - androidx.recyclerview:recyclerview:1.3.2

### Features / 功能特性

#### 1. 文件解析 / File Parsing
- 正确识别 RAPID 关键字和结构
- 处理嵌套的控制结构
- 支持多行注释和内联注释
- 提取变量声明和类型信息

#### 2. 语法高亮 / Syntax Highlighting
- 使用正则表达式模式匹配
- 支持所有主要的 RAPID 语法元素
- 可配置的颜色方案
- 高性能文本渲染

#### 3. 用户体验 / User Experience
- 直观的文件选择界面
- 响应式布局设计
- 支持水平滚动查看长代码行
- 点击例行程序查看具体内容
- 中文本地化支持

#### 4. 代码质量 / Code Quality
- Kotlin 编码规范
- 清晰的代码结构
- 完整的注释文档
- 模块化设计

## [Unreleased] / 未发布

### Planned / 计划中
- [ ] 支持多文件项目浏览
- [ ] 实现代码折叠功能

### Future Enhancements / 未来增强
- [ ] 代码编辑功能
- [ ] 语法自动完成
- [ ] 实时语法检查
- [ ] 代码重构工具
- [ ] 版本控制集成
- [ ] 云端同步
- [ ] 协作编辑
- [ ] 模拟器集成
- [ ] 机器人连接功能
- [ ] 程序上传/下载

## Version History / 版本历史

### Version 1.0.0 - Initial Release
**发布日期**: 2025-10-31

这是 ABB Robot Program Reader 的首个正式版本。包含所有核心功能，支持读取和查看 ABB RAPID 程序文件，提供完整的语法高亮和代码结构识别。

**主要功能**:
- 完整的 RAPID 文件解析
- 美观的语法高亮显示
- 模块和例行程序识别
- Material Design 用户界面
- 丰富的示例程序
- 详细的文档

**已知限制**:
- 仅支持只读查看，不支持编辑
- 不支持实时连接到机器人
- 语法检查功能有限
- 仅支持中文界面

**兼容性**:
- Android 7.0 (API 24) 及以上
- 支持 ARM 和 x86 架构
- 需要至少 50MB 存储空间

## How to Read This Changelog / 如何阅读本更新日志

- **Added** / **新增**: 新功能
- **Changed** / **更改**: 现有功能的变更
- **Deprecated** / **弃用**: 即将移除的功能
- **Removed** / **移除**: 已移除的功能
- **Fixed** / **修复**: Bug 修复
- **Security** / **安全**: 安全相关的更新

## Links / 链接

- [项目仓库 / Repository](https://github.com/OMOCV/Android)
- [问题跟踪 / Issue Tracker](https://github.com/OMOCV/Android/issues)
- [发布页面 / Releases](https://github.com/OMOCV/Android/releases)

## Contributing / 贡献

查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解如何为项目做出贡献。

## License / 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

---

**Note**: 所有日期使用 YYYY-MM-DD 格式 (ISO 8601)
