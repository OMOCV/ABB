# Release Instructions for v1.1.0 / v1.1.0 版本发布说明

## Current Status / 当前状态

✅ Version prepared for release v1.1.0  
✅ Version 1.1.0 已准备就绪

The following changes have been completed:
- ✅ Version number updated to 1.1.0 in `app/build.gradle.kts`
- ✅ CHANGELOG.md updated with v1.1.0 release notes
- ✅ All changes committed to the branch

已完成的更改：
- ✅ `app/build.gradle.kts` 中的版本号已更新为 1.1.0
- ✅ CHANGELOG.md 已更新，包含 v1.1.0 的发行说明
- ✅ 所有更改已提交到分支

## Next Steps to Complete the Release / 完成发布的后续步骤

### Step 1: Merge the Pull Request / 第一步：合并 Pull Request

1. Review and approve the pull request
2. Merge the pull request into the `main` branch

1. 审查并批准 Pull Request
2. 将 Pull Request 合并到 `main` 分支

### Step 2: Create and Push the Version Tag / 第二步：创建并推送版本标签

After the PR is merged into `main`, execute the following commands:

PR 合并到 `main` 分支后，执行以下命令：

```bash
# Switch to main branch / 切换到 main 分支
git checkout main

# Pull the latest changes / 拉取最新更改
git pull origin main

# Create the version tag / 创建版本标签
git tag -a v1.1.0 -m "Release version 1.1.0 - Add CI/CD build and publish workflows"

# Push the tag to trigger the release workflow / 推送标签以触发发布工作流
git push origin v1.1.0
```

### Step 3: Monitor the Release Workflow / 第三步：监控发布工作流

1. Go to [GitHub Actions](https://github.com/OMOCV/Android/actions)
2. Watch the "Create Release" workflow execution
3. Wait for the workflow to complete (usually 5-10 minutes)

1. 访问 [GitHub Actions](https://github.com/OMOCV/Android/actions)
2. 观察 "Create Release" 工作流的执行
3. 等待工作流完成（通常需要 5-10 分钟）

### Step 4: Verify the Release / 第四步：验证发布

1. Go to [Releases](https://github.com/OMOCV/Android/releases)
2. Verify that v1.1.0 release was created
3. Check that the following files are attached:
   - `ABB-v1.1.0-debug.apk`
   - `ABB-v1.1.0-release-unsigned.apk` (if release build succeeded)
   - `checksums-sha256.txt`

1. 访问 [Releases](https://github.com/OMOCV/Android/releases)
2. 验证 v1.1.0 版本是否已创建
3. 检查以下文件是否已附加：
   - `ABB-v1.1.0-debug.apk`
   - `ABB-v1.1.0-release-unsigned.apk`（如果 release 构建成功）
   - `checksums-sha256.txt`

## What the Release Workflow Will Do / 发布工作流将执行的操作

When you push the `v1.1.0` tag, the GitHub Actions release workflow will automatically:

当你推送 `v1.1.0` 标签时，GitHub Actions 发布工作流将自动：

1. ✅ Checkout the code / 检出代码
2. ✅ Set up JDK 17 / 设置 JDK 17
3. ✅ Build Debug APK / 构建 Debug APK
4. ✅ Build Release APK (unsigned) / 构建 Release APK（未签名）
5. ✅ Generate SHA256 checksums / 生成 SHA256 校验和
6. ✅ Create a GitHub Release / 创建 GitHub Release
7. ✅ Upload all APK files and checksums / 上传所有 APK 文件和校验和
8. ✅ Generate release notes (Chinese and English) / 生成发布说明（中英文）

## What's New in v1.1.0 / v1.1.0 新增内容

This release adds complete CI/CD infrastructure for building and publishing the application:

此版本添加了完整的 CI/CD 基础设施，用于构建和发布应用程序：

### Build and Release Automation / 构建和发布自动化
- ✅ GitHub Actions workflow for automated APK builds
- ✅ Automatic release creation when version tags are pushed
- ✅ Debug and Release APK generation
- ✅ SHA256 checksum generation for file verification
- ✅ Automatic upload of build artifacts to GitHub Releases

### Documentation / 文档
- ✅ Complete release guide (RELEASE.md)
- ✅ Quick release guide (QUICK_RELEASE.md)
- ✅ Build and publish summary (BUILD_PUBLISH_SUMMARY.md)
- ✅ Enhanced README with badges and download instructions

### CI/CD Workflows / CI/CD 工作流
- ✅ `.github/workflows/build-apk.yml` - Development build workflow
- ✅ `.github/workflows/release.yml` - Release workflow
- ✅ Support for manual workflow triggers
- ✅ Automatic artifact retention (90 days)

## Troubleshooting / 故障排查

### If the workflow fails / 如果工作流失败

1. Check the [Actions](https://github.com/OMOCV/Android/actions) page for error logs
2. Common issues:
   - Gradle build failures: Check dependencies
   - Permission errors: Verify repository settings
   - Network issues: Retry the workflow

### If APKs are not uploaded / 如果 APK 未上传

1. Check that the build completed successfully
2. Verify the APK files exist in the build output
3. Check the workflow logs for upload errors

## Reference Documentation / 参考文档

- [QUICK_RELEASE.md](QUICK_RELEASE.md) - Quick release guide
- [RELEASE.md](RELEASE.md) - Complete release guide
- [BUILD_PUBLISH_SUMMARY.md](BUILD_PUBLISH_SUMMARY.md) - Implementation summary
- [BUILDING.md](BUILDING.md) - Build guide
- [CHANGELOG.md](CHANGELOG.md) - Version history

## Support / 支持

If you encounter any issues, please:
- Check the [documentation](README.md)
- Review the [troubleshooting guide](QUICK_RELEASE.md#troubleshooting)
- Open an [issue](https://github.com/OMOCV/Android/issues)

如果遇到任何问题，请：
- 查看[文档](README.md)
- 查阅[故障排查指南](QUICK_RELEASE.md#troubleshooting)
- 提交[问题报告](https://github.com/OMOCV/Android/issues)

---

**Ready to release!** / **准备发布！**

Once the PR is merged and the tag is pushed, the application will be automatically built and published.

PR 合并并推送标签后，应用程序将自动构建并发布。
