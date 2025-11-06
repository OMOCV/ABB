# Changelog / 更新日志

All notable changes to the ABB Robot Program Reader project will be documented in this file.

本文件记录 ABB 机器人程序读取器项目的所有重要更改。

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
- [ ] 添加代码搜索功能
- [ ] 支持多文件项目浏览
- [ ] 添加代码导出功能
- [ ] 实现深色主题
- [ ] 添加最近打开文件列表
- [ ] 支持代码书签
- [ ] 添加语法错误检测
- [ ] 实现代码折叠功能
- [ ] 支持代码格式化
- [ ] 添加国际化支持 (英语界面)

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
