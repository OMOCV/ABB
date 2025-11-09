# 签名配置验证指南 / Signing Configuration Verification Guide

本文档说明如何使用签名配置验证工具。

This document explains how to use the signing configuration verification tool.

## 概述 / Overview

在构建已签名的 APK 和 AAB 之前，建议先验证签名配置是否正确。项目提供了一个自动化验证脚本。

Before building signed APK and AAB files, it's recommended to verify that the signing configuration is correct. The project provides an automated verification script.

## 验证脚本 / Verification Script

### 位置 / Location
```
verify-signing-config.sh
```

### 使用方法 / Usage

#### 本地使用 / Local Usage

在项目根目录运行：
Run in the project root directory:

```bash
./verify-signing-config.sh
```

#### CI/CD 使用 / CI/CD Usage

验证脚本已集成到 GitHub Actions 工作流中，会在构建发布版本之前自动运行。

The verification script is integrated into GitHub Actions workflows and runs automatically before building release versions.

## 验证内容 / What is Verified

验证脚本会检查以下内容：

The verification script checks the following:

### 1. 签名配置文件 / Signing Configuration File
- ✓ 检查 `keystore.properties` 或 `demo-keystore.properties` 是否存在
- ✓ Checks if `keystore.properties` or `demo-keystore.properties` exists
- ⚠️ 如果使用演示配置，会发出警告
- ⚠️ Issues a warning if using demo configuration

### 2. 签名参数 / Signing Parameters
- ✓ 验证所有必需参数是否设置：
- ✓ Validates that all required parameters are set:
  - `KEYSTORE_FILE` - 密钥库文件路径 / Keystore file path
  - `KEYSTORE_PASSWORD` - 密钥库密码 / Keystore password
  - `KEY_ALIAS` - 密钥别名 / Key alias
  - `KEY_PASSWORD` - 密钥密码 / Key password

### 3. 密钥库文件 / Keystore File
- ✓ 检查密钥库文件是否存在
- ✓ Checks if keystore file exists
- ✓ 使用 keytool 验证密钥库是否可访问（如果 keytool 可用）
- ✓ Validates keystore accessibility using keytool (if available)
- ✓ 验证密码是否正确
- ✓ Verifies password correctness

### 4. 构建配置 / Build Configuration
- ✓ 检查 `app/build.gradle.kts` 是否存在
- ✓ Checks if `app/build.gradle.kts` exists
- ✓ 验证是否包含 signingConfigs 块
- ✓ Verifies signingConfigs block is present
- ✓ 确认 release 构建类型使用签名配置
- ✓ Confirms release build type uses signing config

### 5. 工作流文件 / Workflow Files
- ✓ 检查 GitHub Actions 工作流文件是否存在
- ✓ Checks if GitHub Actions workflow files exist
- ✓ 验证密钥库解码步骤是否存在
- ✓ Verifies keystore decode step exists
- ✓ 确认引用了签名密钥
- ✓ Confirms signing secrets are referenced

### 6. Gradle Wrapper
- ✓ 检查 gradlew 是否存在
- ✓ Checks if gradlew exists
- ✓ 验证是否可执行
- ✓ Verifies it's executable

## 输出示例 / Output Example

### 成功输出 / Success Output

```
======================================
ABB Signing Configuration Verifier
ABB 签名配置验证器
======================================

=== Step 1: Checking Signing Configuration ===
=== 步骤 1: 检查签名配置 ===

✓ Found keystore.properties file / 找到 keystore.properties 文件

=== Step 2: Validating Signing Parameters ===
=== 步骤 2: 验证签名参数 ===

✓ Parameter KEYSTORE_FILE is set / 参数 KEYSTORE_FILE 已设置
✓ Parameter KEYSTORE_PASSWORD is set / 参数 KEYSTORE_PASSWORD 已设置
✓ Parameter KEY_ALIAS is set / 参数 KEY_ALIAS 已设置
✓ Parameter KEY_PASSWORD is set / 参数 KEY_PASSWORD 已设置

=== Step 3: Checking Keystore File ===
=== 步骤 3: 检查密钥库文件 ===

✓ Keystore file exists: abb-release-key.jks / 密钥库文件存在: abb-release-key.jks
✓ Keystore is valid and accessible / 密钥库有效且可访问

=== Step 4: Checking Build Configuration ===
=== 步骤 4: 检查构建配置 ===

✓ Found build.gradle.kts / 找到 build.gradle.kts
✓ Build file contains signingConfigs / 构建文件包含 signingConfigs
✓ Release build type uses signing config / Release 构建类型使用签名配置

=== Step 5: Checking Workflow Files ===
=== 步骤 5: 检查工作流文件 ===

✓ Found workflow: .github/workflows/build-apk.yml / 找到工作流: .github/workflows/build-apk.yml
✓ Workflow has keystore decode step / 工作流包含密钥库解码步骤
✓ Workflow references signing secrets / 工作流引用签名密钥
✓ Found workflow: .github/workflows/release.yml / 找到工作流: .github/workflows/release.yml
✓ Workflow has keystore decode step / 工作流包含密钥库解码步骤
✓ Workflow references signing secrets / 工作流引用签名密钥

=== Step 6: Checking Gradle Wrapper ===
=== 步骤 6: 检查 Gradle Wrapper ===

✓ Gradle wrapper found / 找到 Gradle wrapper
✓ Gradle wrapper is executable / Gradle wrapper 可执行

======================================
=== Verification Summary ===
=== 验证摘要 ===
======================================

✓ All checks passed! / 所有检查通过！

You can now build signed APK/AAB with:
现在可以使用以下命令构建已签名的 APK/AAB:

  ./gradlew assembleRelease    # Build signed APK
  ./gradlew bundleRelease      # Build signed AAB
```

### 失败输出 / Failure Output

如果验证失败，脚本会显示具体错误：

If verification fails, the script will show specific errors:

```
✗ No keystore properties file found / 未找到密钥库属性文件
  Please create keystore.properties or use demo-keystore.properties
  请创建 keystore.properties 或使用 demo-keystore.properties
```

## 构建流程 / Build Process

### 完整构建流程 / Complete Build Process

1. **验证签名配置 / Verify signing configuration**
   ```bash
   ./verify-signing-config.sh
   ```

2. **构建签名的 APK / Build signed APK**
   ```bash
   ./gradlew assembleRelease
   ```

3. **构建签名的 AAB / Build signed AAB**
   ```bash
   ./gradlew bundleRelease
   ```

### 自动化流程 / Automated Process

在 GitHub Actions 中，工作流会自动执行以下步骤：

In GitHub Actions, the workflow automatically executes these steps:

1. 检出代码 / Checkout code
2. 设置 JDK / Setup JDK
3. 解码密钥库（如果配置了 secrets）/ Decode keystore (if secrets configured)
4. **验证签名配置 / Verify signing configuration** ← 新增步骤 / New step
5. 构建 Debug APK/AAB / Build Debug APK/AAB
6. 构建 Release APK/AAB / Build Release APK/AAB
7. 上传构建产物 / Upload artifacts

## 故障排除 / Troubleshooting

### 验证失败 / Verification Fails

如果验证失败，请按照以下步骤操作：

If verification fails, follow these steps:

1. **检查错误消息 / Check error message**
   - 仔细阅读脚本输出的错误信息
   - Carefully read error messages from the script

2. **创建或更新配置 / Create or update configuration**
   ```bash
   # 生成新的密钥库 / Generate new keystore
   ./generate-keystore.sh
   
   # 或使用演示配置（仅用于开发）/ Or use demo config (dev only)
   cp demo-keystore.properties keystore.properties
   ```

3. **验证密钥库密码 / Verify keystore passwords**
   - 确保 keystore.properties 中的密码正确
   - Ensure passwords in keystore.properties are correct

4. **重新运行验证 / Re-run verification**
   ```bash
   ./verify-signing-config.sh
   ```

### CI/CD 环境 / CI/CD Environment

在 CI/CD 环境中，验证脚本会自动使用环境变量（由 GitHub Secrets 设置）。

In CI/CD environments, the verification script automatically uses environment variables (set by GitHub Secrets).

确保已配置以下 Secrets：
Ensure the following Secrets are configured:

- `KEYSTORE_BASE64` - Base64 编码的密钥库 / Base64-encoded keystore
- `KEYSTORE_PASSWORD` - 密钥库密码 / Keystore password
- `KEY_ALIAS` - 密钥别名 / Key alias
- `KEY_PASSWORD` - 密钥密码 / Key password

## 相关文档 / Related Documentation

- [SIGNING.md](SIGNING.md) - 完整签名配置指南 / Complete signing configuration guide
- [BUILDING.md](BUILDING.md) - 构建指南 / Building guide
- [RELEASE.md](RELEASE.md) - 发布指南 / Release guide

## 安全建议 / Security Recommendations

- ✅ 在构建前始终运行验证脚本 / Always run verification script before building
- ✅ 不要将生产密钥库提交到版本控制 / Don't commit production keystores to version control
- ✅ 使用强密码 / Use strong passwords
- ✅ 定期备份密钥库 / Regularly backup keystores
- ⚠️ 不要在生产中使用演示配置 / Don't use demo configuration in production
