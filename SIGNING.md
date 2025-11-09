# 签名配置指南 / Signing Configuration Guide

本文档说明如何配置 Android 应用签名以构建发布版本。

This document explains how to configure Android application signing to build release versions.

## 什么是应用签名？/ What is App Signing?

Android 要求所有 APK 在安装到设备或更新之前都必须经过数字签名。签名用于：

Android requires that all APKs be digitally signed before they can be installed on a device or updated. Signing is used to:

- 验证应用的来源 / Verify the app's origin
- 确保应用未被篡改 / Ensure the app hasn't been tampered with
- 允许应用更新 / Enable app updates
- 建立应用之间的信任 / Establish trust between apps

## 快速开始 / Quick Start

### 方法 1: 使用生成脚本（推荐）/ Method 1: Using Generation Script (Recommended)

项目提供了一个交互式脚本来生成密钥库：

The project provides an interactive script to generate a keystore:

```bash
./generate-keystore.sh
```

脚本会引导您完成以下步骤：
1. 设置密钥库文件名
2. 设置密钥别名
3. 设置密码
4. 输入证书信息
5. 自动生成密钥库和配置文件

The script will guide you through:
1. Setting keystore file name
2. Setting key alias
3. Setting passwords
4. Entering certificate information
5. Automatically generating keystore and configuration files

### 方法 2: 手动创建 / Method 2: Manual Creation

#### 步骤 1: 生成密钥库 / Step 1: Generate Keystore

使用 `keytool` 命令生成密钥库：

Use the `keytool` command to generate a keystore:

```bash
keytool -genkey -v -keystore abb-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias abb-key
```

命令参数说明 / Command parameters:
- `-keystore`: 密钥库文件名 / Keystore file name
- `-keyalg`: 加密算法（RSA）/ Encryption algorithm (RSA)
- `-keysize`: 密钥长度（2048位）/ Key size (2048 bits)
- `-validity`: 有效期（天数）/ Validity period (days)
- `-alias`: 密钥别名 / Key alias

执行命令后，会提示您输入：
- 密钥库密码（至少6个字符）
- 密钥密码（可以与密钥库密码相同）
- 您的名字、组织等信息

After executing the command, you'll be prompted for:
- Keystore password (at least 6 characters)
- Key password (can be the same as keystore password)
- Your name, organization, etc.

#### 步骤 2: 创建配置文件 / Step 2: Create Configuration File

在项目根目录创建 `keystore.properties` 文件：

Create a `keystore.properties` file in the project root directory:

```properties
KEYSTORE_FILE=abb-release-key.jks
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=abb-key
KEY_PASSWORD=your_key_password
```

**注意 / Note**: 
- 将密码替换为您在步骤 1 中设置的实际密码
- Replace passwords with the actual passwords you set in Step 1
- 此文件已自动添加到 `.gitignore`，不会被提交
- This file is automatically added to `.gitignore` and won't be committed

## 构建签名应用 / Building Signed Application

配置完成后，使用以下命令构建签名的发布版本：

After configuration, use the following commands to build signed release versions:

### 构建签名 APK / Build Signed APK

```bash
./gradlew assembleRelease
```

输出文件 / Output file:
- `app/build/outputs/apk/release/app-release.apk` (已签名 / signed)

### 构建签名 AAB / Build Signed AAB

```bash
./gradlew bundleRelease
```

输出文件 / Output file:
- `app/build/outputs/bundle/release/app-release.aab` (已签名 / signed)

## 验证签名 / Verify Signature

使用以下命令验证 APK 是否已正确签名：

Use the following command to verify that the APK is correctly signed:

```bash
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

成功签名的输出应显示 / Successful signing output should show:
```
jar verified.
```

查看签名详细信息 / View signature details:

```bash
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

## CI/CD 配置 / CI/CD Configuration

### GitHub Actions

项目已经配置了 GitHub Actions 支持。要在 GitHub Actions 中使用签名：

The project is already configured with GitHub Actions support. To use signing in GitHub Actions:

#### 1. 生成 Base64 编码的密钥库 / Generate Base64-encoded Keystore

**Linux/macOS:**
```bash
base64 abb-release-key.jks | tr -d '\n' > keystore.b64
```

**Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("abb-release-key.jks")) > keystore.b64
```

#### 2. 配置 GitHub Secrets

在 GitHub 仓库设置中添加以下 Secrets：

Add the following Secrets in GitHub repository settings:

1. 访问 / Visit: `Settings > Secrets and variables > Actions`
2. 点击 `New repository secret` / Click `New repository secret`
3. 添加以下 secrets / Add the following secrets:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `KEYSTORE_BASE64` | (Base64 content) | Base64 编码的密钥库文件 / Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | (your password) | 密钥库密码 / Keystore password |
| `KEY_ALIAS` | abb-key | 密钥别名 / Key alias |
| `KEY_PASSWORD` | (your password) | 密钥密码 / Key password |

#### 3. 触发构建 / Trigger Build

推送代码或创建标签后，GitHub Actions 会自动构建签名版本：

After pushing code or creating tags, GitHub Actions will automatically build signed versions:

```bash
# 创建版本标签 / Create version tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

## 安全最佳实践 / Security Best Practices

### 1. 保护密钥库文件 / Protect Keystore File

- ✅ **必须做 / Must Do:**
  - 在安全的位置备份密钥库 / Back up keystore in a secure location
  - 使用强密码（至少8个字符，包含大小写字母、数字和符号）/ Use strong passwords (at least 8 characters with mixed case, numbers, and symbols)
  - 限制密钥库文件的访问权限 / Limit access permissions to keystore file
  - 将密钥库存储在加密的存储设备上 / Store keystore on encrypted storage

- ❌ **不要做 / Don't Do:**
  - 不要将密钥库提交到版本控制系统 / Don't commit keystore to version control
  - 不要将密钥库存储在公共可访问的位置 / Don't store keystore in publicly accessible locations
  - 不要在代码或日志中硬编码密码 / Don't hardcode passwords in code or logs
  - 不要共享密钥库密码 / Don't share keystore passwords

### 2. 密钥库丢失怎么办？/ What if Keystore is Lost?

**重要警告 / Important Warning:**

如果您丢失了密钥库文件，您将：

If you lose your keystore file, you will:

- ❌ 无法更新已发布的应用 / Cannot update published apps
- ❌ 需要使用新的包名重新发布应用 / Need to republish app with new package name
- ❌ 用户需要卸载旧版本并重新安装 / Users need to uninstall old version and reinstall
- ❌ 失去所有用户数据和评分 / Lose all user data and ratings

**因此，备份密钥库至关重要！/ Therefore, backing up keystore is crucial!**

建议的备份策略 / Recommended backup strategy:
- 在多个安全位置存储副本 / Store copies in multiple secure locations
- 使用加密的云存储 / Use encrypted cloud storage
- 使用密码管理器存储密码 / Use password manager to store passwords
- 定期验证备份的完整性 / Regularly verify backup integrity

### 3. 不同环境使用不同密钥 / Use Different Keys for Different Environments

- **开发环境 / Development**: 使用测试密钥库 / Use test keystore
- **测试环境 / Testing**: 使用测试密钥库 / Use test keystore
- **生产环境 / Production**: 使用生产密钥库 / Use production keystore

## 故障排查 / Troubleshooting

### 问题 1: "keystore.properties not found"

**原因 / Cause**: 未创建配置文件

**解决方案 / Solution**:
```bash
./generate-keystore.sh
# 或手动创建 keystore.properties 文件
# Or manually create keystore.properties file
```

### 问题 2: "Keystore was tampered with, or password was incorrect"

**原因 / Cause**: 密码错误或密钥库文件损坏

**解决方案 / Solution**:
- 检查 `keystore.properties` 中的密码是否正确
- Check if passwords in `keystore.properties` are correct
- 验证密钥库文件未损坏
- Verify keystore file is not corrupted
- 如果损坏，从备份恢复
- If corrupted, restore from backup

### 问题 3: "Could not find signing config"

**原因 / Cause**: 环境变量或配置文件未正确设置

**解决方案 / Solution**:
- 确保 `keystore.properties` 文件在项目根目录
- Ensure `keystore.properties` file is in project root
- 确保文件内容格式正确
- Ensure file content format is correct
- 重新运行 Gradle sync
- Re-run Gradle sync

### 问题 4: Release APK 未签名

**原因 / Cause**: 签名配置未正确应用

**解决方案 / Solution**:
```bash
# 清理并重新构建
./gradlew clean assembleRelease --stacktrace
```

查看详细错误信息 / View detailed error information:
```bash
./gradlew assembleRelease --info
```

## 示例配置 / Example Configuration

### 开发环境示例 / Development Environment Example

```properties
# keystore.properties (开发 / Development)
KEYSTORE_FILE=debug-key.jks
KEYSTORE_PASSWORD=debug123
KEY_ALIAS=debug-key
KEY_PASSWORD=debug123
```

### 生产环境示例 / Production Environment Example

```properties
# keystore.properties (生产 / Production)
KEYSTORE_FILE=release-key.jks
KEYSTORE_PASSWORD=StrongP@ssw0rd!2024
KEY_ALIAS=release-key
KEY_PASSWORD=StrongKeyP@ss!2024
```

## 相关资源 / Related Resources

- [Android 官方文档: 签名您的应用](https://developer.android.com/studio/publish/app-signing)
- [Android Official Docs: Sign your app](https://developer.android.com/studio/publish/app-signing)
- [构建指南 / Building Guide](BUILDING.md)
- [发布指南 / Release Guide](RELEASE.md)

## 支持 / Support

如有问题，请：
If you have questions, please:

- 查看 [GitHub Issues](https://github.com/OMOCV/Android/issues)
- 阅读 [BUILDING.md](BUILDING.md) 了解更多构建信息
- Read [BUILDING.md](BUILDING.md) for more build information
- 参考 [RELEASE.md](RELEASE.md) 了解发布流程
- Refer to [RELEASE.md](RELEASE.md) for release process
