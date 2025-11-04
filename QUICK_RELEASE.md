# 快速发布指南 / Quick Release Guide

## 🚀 快速发布新版本 / Quick Release

### 5 步完成发布 / 5 Steps to Release

```bash
# 1. 更新版本号 (编辑 app/build.gradle.kts)
# versionCode = 2
# versionName = "1.1.0"

# 2. 更新 CHANGELOG.md
# 添加新版本的变更记录

# 3. 提交更改
git add .
git commit -m "Prepare for release v1.1.0"
git push origin main

# 4. 创建版本标签
git tag -a v1.1.0 -m "Release version 1.1.0"

# 5. 推送标签（触发自动发布）
git push origin v1.1.0
```

### ✅ 完成！/ Done!

GitHub Actions 将自动：
- 构建 APK
- 创建 Release
- 上传文件

查看结果: https://github.com/OMOCV/Android/releases

---

## 📦 构建产物 / Build Artifacts

### Debug APK
```
ABB-{version}-debug.apk
```
- 用于开发和测试
- 包含调试信息
- 未优化

### Release APK
```
ABB-{version}-release-unsigned.apk
```
- 用于生产环境
- 已优化和混淆
- 未签名（或已签名，如果配置了密钥）

### 校验和文件
```
checksums-sha256.txt
```
- 验证文件完整性
- SHA256 哈希值

---

## 🔍 查看构建状态 / Check Build Status

- **Actions**: https://github.com/OMOCV/Android/actions
- **Releases**: https://github.com/OMOCV/Android/releases
- **Latest**: https://github.com/OMOCV/Android/releases/latest

---

## 📝 版本命名规范 / Version Format

```
v<major>.<minor>.<patch>[-<pre-release>]
```

示例 / Examples:
- `v1.0.0` - 稳定版
- `v1.1.0` - 新功能
- `v1.0.1` - 补丁
- `v2.0.0-beta.1` - 测试版
- `v2.0.0-rc.1` - 候选版

---

## ⚙️ 手动触发构建 / Manual Build

1. 访问 [Actions](https://github.com/OMOCV/Android/actions)
2. 选择 "Build APK"
3. 点击 "Run workflow"
4. 从 Artifacts 下载 APK

---

## 🐛 问题排查 / Troubleshooting

### 构建失败？
```bash
# 本地测试构建
./gradlew clean assembleDebug

# 查看 Actions 日志
# https://github.com/OMOCV/Android/actions
```

### Release 未创建？
- 确认标签格式: `v*.*.*`
- 检查工作流文件是否存在
- 验证推送成功: `git push origin v1.0.0`

### APK 未上传？
- 查看 Actions 构建日志
- 确认构建成功完成
- 检查 Release 页面

---

## 📚 详细文档 / Detailed Docs

- **完整发布指南**: [RELEASE.md](RELEASE.md)
- **构建指南**: [BUILDING.md](BUILDING.md)
- **实施总结**: [BUILD_PUBLISH_SUMMARY.md](BUILD_PUBLISH_SUMMARY.md)
- **变更日志**: [CHANGELOG.md](CHANGELOG.md)

---

## 🔑 需要签名配置？ / Need Signing?

参考 [RELEASE.md](RELEASE.md) 的"APK 签名配置"章节

快速步骤：
1. 生成密钥库
2. 配置 GitHub Secrets
3. 更新 Gradle 配置
4. 更新工作流文件

---

**提示**: 首次发布前，建议先阅读 [RELEASE.md](RELEASE.md) 了解完整流程！
