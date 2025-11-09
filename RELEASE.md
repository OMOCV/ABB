# 发布指南 / Release Guide

本文档描述如何创建和发布 ABB Android 应用的新版本。

This document describes how to create and release a new version of the ABB Android application.

## 版本命名规范 / Version Naming Convention

使用语义化版本控制（Semantic Versioning）：

```
v<major>.<minor>.<patch>[-<pre-release>]
```

示例 / Examples:
- `v1.0.0` - 正式版本 / Stable release
- `v1.1.0` - 新功能版本 / New feature release
- `v1.0.1` - 补丁版本 / Patch release
- `v2.0.0-beta.1` - 测试版本 / Beta release
- `v2.0.0-rc.1` - 候选版本 / Release candidate

## 发布前准备 / Pre-Release Checklist

### 1. 更新版本信息 / Update Version Information

更新 `app/build.gradle.kts` 中的版本号：

```kotlin
android {
    defaultConfig {
        versionCode = 2  // 递增版本号 / Increment version code
        versionName = "1.1.0"  // 更新版本名称 / Update version name
    }
}
```

### 2. 更新变更日志 / Update Changelog

在 `CHANGELOG.md` 中添加新版本的变更记录：

```markdown
## [1.1.0] - 2024-11-02

### Added
- 新功能描述

### Changed
- 修改内容描述

### Fixed
- 修复问题描述
```

### 3. 运行测试 / Run Tests

确保所有测试通过：

```bash
# 单元测试
./gradlew test

# Lint 检查
./gradlew lint

# 本地构建测试
./gradlew assembleDebug
./gradlew assembleRelease
```

### 4. 更新文档 / Update Documentation

- 更新 README.md（如有必要）
- 更新示例代码（如有必要）
- 确保所有文档与新版本一致

## 创建发布 / Creating a Release

### 方法 1: 使用 Git 标签（推荐）

这是推荐的方法，会自动触发 GitHub Actions 构建和发布流程。

1. **提交所有更改**

```bash
git add .
git commit -m "Prepare for release v1.1.0"
```

2. **创建并推送标签**

```bash
# 创建带注释的标签
git tag -a v1.1.0 -m "Release version 1.1.0"

# 推送标签到远程仓库
git push origin v1.1.0
```

3. **查看构建进度**

访问 [GitHub Actions](https://github.com/OMOCV/Android/actions) 查看自动构建进度。

4. **验证发布**

构建完成后，检查 [Releases](https://github.com/OMOCV/Android/releases) 页面：
- 确认 Release 已创建
- 确认 APK 文件已上传
- 确认校验和文件已生成
- 检查发布说明是否完整

### 方法 2: 通过 GitHub Web 界面

1. 访问仓库的 [Releases](https://github.com/OMOCV/Android/releases) 页面
2. 点击 "Draft a new release"
3. 填写以下信息：
   - **Tag**: 创建新标签，如 `v1.1.0`
   - **Release title**: 版本名称，如 "ABB v1.1.0"
   - **Description**: 版本说明（可以从 CHANGELOG.md 复制）
4. 手动上传 APK 文件（如果需要）
5. 点击 "Publish release"

注意：使用此方法需要手动构建 APK 文件。

## 自动发布流程 / Automated Release Process

当推送版本标签时，GitHub Actions 会自动执行以下操作：

### 1. 构建阶段 / Build Phase

```
✓ 检出代码 / Checkout code
✓ 设置 JDK 17 / Setup JDK 17
✓ 构建 Debug APK / Build debug APK
✓ 构建 Release APK / Build release APK (if signing configured)
✓ 重命名 APK 文件 / Rename APK files
✓ 生成校验和 / Generate checksums
```

### 2. 发布阶段 / Release Phase

```
✓ 创建 GitHub Release / Create GitHub release
✓ 上传 APK 文件 / Upload APK files
✓ 上传校验和文件 / Upload checksum file
✓ 生成发布说明 / Generate release notes
✓ 保存构建产物 / Archive build artifacts
```

## APK 签名配置 / APK Signing Configuration

### 开发环境签名 / Development Signing

Debug 版本使用默认的 Android 调试密钥自动签名。

### 生产环境签名 / Production Signing

要发布签名的 Release APK，需要配置发布密钥：

#### 1. 生成密钥库 / Generate Keystore

```bash
keytool -genkey -v -keystore release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias abb-release-key
```

按提示输入：
- 密钥库密码（Store Password）
- 密钥密码（Key Password）
- 您的名字和组织信息

#### 2. 配置 GitHub Secrets

在 GitHub 仓库设置中添加以下 Secrets：

1. 访问 `Settings > Secrets and variables > Actions`
2. 添加以下 secrets：
   - `KEYSTORE_BASE64`: 密钥库文件的 Base64 编码
   - `KEYSTORE_PASSWORD`: 密钥库密码
   - `KEY_ALIAS`: 密钥别名
   - `KEY_PASSWORD`: 密钥密码

**生成 KEYSTORE_BASE64 的 Base64 编码：**

```bash
# Linux/macOS
base64 release-key.jks > keystore.b64

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release-key.jks")) > keystore.b64
```

#### 3. 更新 Gradle 配置

**Note: 项目已配置好签名支持，无需手动修改 Gradle 文件。**

The project is already configured to use signing from environment variables.  
项目已经配置为从环境变量读取签名配置。

在 `app/build.gradle.kts` 中已包含签名配置：

```kotlin
android {
    signingConfigs {
        create("release") {
            // Read signing configuration from environment variables
            // These will be set by CI/CD or from keystore.properties file
            val keystorePath = System.getenv("KEYSTORE_FILE") ?: project.findProperty("KEYSTORE_FILE") as String?
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD") ?: project.findProperty("KEYSTORE_PASSWORD") as String?
            val keyAlias = System.getenv("KEY_ALIAS") ?: project.findProperty("KEY_ALIAS") as String?
            val keyPassword = System.getenv("KEY_PASSWORD") ?: project.findProperty("KEY_PASSWORD") as String?

            if (keystorePath != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }
    
    buildTypes {
        release {
            // Only use signing config if it's properly configured
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

#### 4. 更新工作流

**Note: 工作流已配置好签名支持，无需手动修改。**

The workflows are already configured to use signing secrets.  
工作流已经配置为使用签名密钥。

在 `.github/workflows/build-apk.yml` 和 `.github/workflows/release.yml` 中已包含签名步骤：

```yaml
# Decode and setup signing keystore if secrets are available
- name: Decode Keystore
  if: ${{ secrets.KEYSTORE_BASE64 != '' }}
  run: |
    echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 --decode > $HOME/keystore.jks
    echo "KEYSTORE_FILE=$HOME/keystore.jks" >> $GITHUB_ENV
    echo "KEYSTORE_PASSWORD=${{ secrets.KEYSTORE_PASSWORD }}" >> $GITHUB_ENV
    echo "KEY_ALIAS=${{ secrets.KEY_ALIAS }}" >> $GITHUB_ENV
    echo "KEY_PASSWORD=${{ secrets.KEY_PASSWORD }}" >> $GITHUB_ENV
    echo "✓ Keystore decoded and configured"

- name: Build Release APK
  run: ./gradlew assembleRelease --no-daemon --stacktrace
  continue-on-error: ${{ secrets.KEYSTORE_BASE64 == '' }}
  id: release-apk-build
```

当配置了 GitHub Secrets 后，工作流会自动：
1. 解码 keystore 文件
2. 设置环境变量
3. 构建签名的 Release APK 和 AAB

When GitHub Secrets are configured, the workflow will automatically:
1. Decode the keystore file
2. Set environment variables
3. Build signed Release APK and AAB

## 发布后任务 / Post-Release Tasks

### 1. 验证发布 / Verify Release

- 下载并测试发布的 APK
- 验证版本号是否正确
- 验证所有功能是否正常工作
- 检查发布说明是否准确

### 2. 公告发布 / Announce Release

- 更新项目文档（如果需要）
- 在社交媒体或相关平台发布公告
- 通知用户更新

### 3. 监控反馈 / Monitor Feedback

- 关注 GitHub Issues 中的问题报告
- 准备必要的热修复版本

## 版本回滚 / Version Rollback

如果发现发布版本有严重问题：

1. **删除有问题的 Release**
   - 在 GitHub Releases 页面删除该版本
   - 删除对应的 Git 标签：
     ```bash
     git tag -d v1.1.0
     git push origin :refs/tags/v1.1.0
     ```

2. **创建修复版本**
   - 修复问题
   - 创建新的补丁版本（如 v1.1.1）

3. **发布修复版本**
   - 按照正常发布流程发布修复版本

## 故障排查 / Troubleshooting

### 构建失败 / Build Failed

**问题**: GitHub Actions 构建失败

**解决方案**:
1. 检查 Actions 日志查看具体错误
2. 确保所有依赖都可访问
3. 验证 Gradle 配置正确
4. 本地测试构建是否成功

### 签名失败 / Signing Failed

**问题**: Release APK 签名失败

**解决方案**:
1. 验证 GitHub Secrets 配置正确
2. 检查密钥库文件是否有效
3. 确认密码和别名正确
4. 查看构建日志中的签名错误信息

### Release 未创建 / Release Not Created

**问题**: 推送标签后 Release 未自动创建

**解决方案**:
1. 检查标签格式是否正确（v*.*.*）
2. 确认工作流文件存在且正确
3. 查看 Actions 页面的工作流执行状态
4. 检查仓库权限设置

## 最佳实践 / Best Practices

1. **定期发布** - 保持发布节奏，不要积累太多变更
2. **语义化版本** - 严格遵守语义化版本规范
3. **完整测试** - 发布前进行充分测试
4. **详细日志** - 在 CHANGELOG.md 中记录所有重要变更
5. **安全管理** - 妥善保管签名密钥，不要提交到版本控制
6. **自动化** - 尽量使用自动化流程减少人为错误
7. **及时修复** - 快速响应和修复发布版本中的问题

## 参考资源 / References

- [语义化版本控制](https://semver.org/lang/zh-CN/)
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Android 应用签名](https://developer.android.com/studio/publish/app-signing)
- [Gradle 构建配置](https://developer.android.com/build)

## 支持 / Support

如有发布相关问题，请：
- 查看 [GitHub Discussions](https://github.com/OMOCV/Android/discussions)
- 提交 [Issue](https://github.com/OMOCV/Android/issues)
- 参考 [BUILDING.md](BUILDING.md) 了解构建详情
