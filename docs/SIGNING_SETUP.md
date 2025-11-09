# Android APK 签名配置指南 / Android APK Signing Setup Guide

本文档详细介绍如何配置 Android APK 签名，以便在 GitHub Actions 中自动构建签名版本。

This document explains how to configure Android APK signing for automatic signed builds in GitHub Actions.

## 目录 / Table of Contents

1. [创建签名密钥 / Create Signing Key](#创建签名密钥--create-signing-key)
2. [本地签名配置 / Local Signing Configuration](#本地签名配置--local-signing-configuration)
3. [GitHub Actions 签名配置 / GitHub Actions Signing Configuration](#github-actions-签名配置--github-actions-signing-configuration)
4. [验证签名 / Verify Signing](#验证签名--verify-signing)
5. [故障排查 / Troubleshooting](#故障排查--troubleshooting)

## 创建签名密钥 / Create Signing Key

### 步骤 1: 生成密钥库 / Step 1: Generate Keystore

使用 `keytool` 命令生成新的密钥库文件：

Use the `keytool` command to generate a new keystore file:

```bash
keytool -genkey -v -keystore abb-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias abb-key
```

### 步骤 2: 填写密钥信息 / Step 2: Fill in Key Information

命令会提示您输入以下信息：

The command will prompt you for the following information:

1. **密钥库密码 / Keystore password**: 至少 6 个字符，妥善保管
   - At least 6 characters, keep it safe
   
2. **密钥密码 / Key password**: 可以与密钥库密码相同
   - Can be the same as keystore password
   
3. **您的名字和姓氏 / Your name**: 例如 "Zhang San" 或 "John Doe"
   
4. **组织单位 / Organizational unit**: 例如 "Development Team"
   
5. **组织名称 / Organization**: 例如 "OMOCV"
   
6. **城市或地区 / City or Locality**: 例如 "Beijing" 或 "Shanghai"
   
7. **州或省 / State or Province**: 例如 "Beijing"
   
8. **国家代码 / Country code**: 两位字母代码，例如 "CN" 或 "US"

### 示例输出 / Example Output

```
Enter keystore password: [输入密码]
Re-enter new password: [再次输入密码]
What is your first and last name?
  [Unknown]:  Zhang San
What is the name of your organizational unit?
  [Unknown]:  Development Team
What is the name of your organization?
  [Unknown]:  OMOCV
What is the name of your City or Locality?
  [Unknown]:  Beijing
What is the name of your State or Province?
  [Unknown]:  Beijing
What is the two-letter country code for this unit?
  [Unknown]:  CN
Is CN=Zhang San, OU=Development Team, O=OMOCV, L=Beijing, ST=Beijing, C=CN correct?
  [no]:  yes

Generating 2,048 bit RSA key pair and self-signed certificate (SHA256withRSA) with a validity of 10,000 days
        for: CN=Zhang San, OU=Development Team, O=OMOCV, L=Beijing, ST=Beijing, C=CN
[Storing abb-release-key.jks]
```

### 重要提示 / Important Notes

⚠️ **保管好以下信息 / Keep the following information safe:**
- 密钥库文件 `abb-release-key.jks` / Keystore file
- 密钥库密码 / Keystore password
- 密钥别名 `abb-key` / Key alias
- 密钥密码 / Key password

⚠️ **不要提交到版本控制 / Do NOT commit to version control:**
- 密钥库文件已添加到 `.gitignore`
- 永远不要将密钥文件上传到 GitHub 或其他公共平台
- The keystore file is already in `.gitignore`
- Never upload the keystore file to GitHub or other public platforms

## 本地签名配置 / Local Signing Configuration

### 步骤 1: 创建配置文件 / Step 1: Create Configuration File

在项目根目录创建 `keystore.properties` 文件：

Create a `keystore.properties` file in the project root directory:

```properties
KEYSTORE_FILE=abb-release-key.jks
KEYSTORE_PASSWORD=your_keystore_password_here
KEY_ALIAS=abb-key
KEY_PASSWORD=your_key_password_here
```

**替换实际值 / Replace with actual values:**
- `KEYSTORE_FILE`: 密钥库文件的路径（相对于项目根目录）
- `KEYSTORE_PASSWORD`: 您在创建密钥库时设置的密码
- `KEY_ALIAS`: 密钥别名（如果使用上述命令，则为 `abb-key`）
- `KEY_PASSWORD`: 密钥密码

### 步骤 2: 验证文件路径 / Step 2: Verify File Path

确保文件结构如下：

Ensure your file structure looks like this:

```
Android/
├── abb-release-key.jks          # 密钥库文件 / Keystore file
├── keystore.properties          # 签名配置 / Signing configuration
├── app/
├── build.gradle.kts
└── ...
```

### 步骤 3: 构建签名版本 / Step 3: Build Signed Release

```bash
./gradlew assembleRelease
```

签名的 APK 将生成在：

The signed APK will be generated at:
```
app/build/outputs/apk/release/app-release.apk
```

## GitHub Actions 签名配置 / GitHub Actions Signing Configuration

### 步骤 1: 编码密钥库文件 / Step 1: Encode Keystore File

将密钥库文件转换为 Base64 编码：

Convert the keystore file to Base64 encoding:

**Linux / macOS:**
```bash
base64 abb-release-key.jks | tr -d '\n' > keystore.b64
```

**Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("abb-release-key.jks")) | Out-File keystore.b64 -Encoding ASCII
```

**Windows (Git Bash):**
```bash
base64 -w 0 abb-release-key.jks > keystore.b64
```

### 步骤 2: 获取 Base64 内容 / Step 2: Get Base64 Content

查看并复制 `keystore.b64` 的内容：

View and copy the content of `keystore.b64`:

```bash
cat keystore.b64
```

或者在文本编辑器中打开文件并复制全部内容。

Or open the file in a text editor and copy all content.

### 步骤 3: 配置 GitHub Secrets / Step 3: Configure GitHub Secrets

1. **访问仓库设置 / Go to repository settings:**
   - 打开 GitHub 仓库页面
   - 点击 `Settings` 标签
   - 在左侧菜单中选择 `Secrets and variables` > `Actions`

2. **添加 Secrets / Add Secrets:**

   点击 `New repository secret` 并添加以下 secrets：
   
   Click `New repository secret` and add the following secrets:

   | Secret Name | Value | Description |
   |-------------|-------|-------------|
   | `KEYSTORE_BASE64` | [keystore.b64 的内容] | Base64 编码的密钥库文件 / Base64-encoded keystore file |
   | `KEYSTORE_PASSWORD` | [您的密钥库密码] | 密钥库密码 / Keystore password |
   | `KEY_ALIAS` | `abb-key` | 密钥别名 / Key alias |
   | `KEY_PASSWORD` | [您的密钥密码] | 密钥密码 / Key password |

3. **验证配置 / Verify Configuration:**
   
   确保所有 4 个 secrets 都已添加：
   
   Make sure all 4 secrets are added:
   
   - ✅ KEYSTORE_BASE64
   - ✅ KEYSTORE_PASSWORD
   - ✅ KEY_ALIAS
   - ✅ KEY_PASSWORD

### 步骤 4: 触发构建 / Step 4: Trigger Build

签名配置完成后，每次推送代码或创建 tag 时，GitHub Actions 将自动构建签名版本。

After signing is configured, GitHub Actions will automatically build signed versions when you push code or create a tag.

**触发方式 / Trigger methods:**

1. **推送到主分支 / Push to main branch:**
   ```bash
   git push origin main
   ```

2. **创建发布标签 / Create release tag:**
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```

3. **手动触发 / Manual trigger:**
   - 访问 `Actions` 标签
   - 选择 `Build APK and AAB` 工作流
   - 点击 `Run workflow`

## 验证签名 / Verify Signing

### 方法 1: 查看构建日志 / Method 1: Check Build Logs

在 GitHub Actions 的构建日志中查看：

Check in GitHub Actions build logs:

```
✓ Keystore decoded and configured
Building Release APK...
Signing APK with release key...
BUILD SUCCESSFUL
```

### 方法 2: 使用 apksigner 验证 / Method 2: Verify with apksigner

下载构建的 APK 后，使用 `apksigner` 验证：

After downloading the built APK, verify with `apksigner`:

```bash
# 需要 Android SDK build-tools
# Requires Android SDK build-tools
apksigner verify --verbose app-release.apk
```

**成功输出 / Success output:**
```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

### 方法 3: 检查 APK 信息 / Method 3: Check APK Info

```bash
# 使用 aapt 查看 APK 信息
# Use aapt to view APK info
aapt dump badging app-release.apk | grep "package:"
```

查看证书信息：

View certificate info:
```bash
jarsigner -verify -verbose -certs app-release.apk
```

## 故障排查 / Troubleshooting

### 问题 1: 构建失败 "Keystore file not found"

**原因 / Cause:**
- `KEYSTORE_BASE64` secret 未正确配置
- Base64 编码不正确

**解决方案 / Solution:**
1. 重新生成 Base64 编码，确保没有换行符
2. 确认 secret 名称拼写正确（区分大小写）
3. 重新添加 `KEYSTORE_BASE64` secret

### 问题 2: 构建失败 "Incorrect password"

**原因 / Cause:**
- 密码输入错误
- 密码包含特殊字符未正确处理

**解决方案 / Solution:**
1. 验证密码是否正确
2. 确认 `KEYSTORE_PASSWORD` 和 `KEY_PASSWORD` 都已设置
3. 如果密码包含特殊字符，尝试使用更简单的密码重新生成密钥库

### 问题 3: 本地构建成功，但 CI 失败

**原因 / Cause:**
- GitHub Secrets 未配置或配置错误
- 环境变量未正确传递

**解决方案 / Solution:**
1. 检查所有 4 个 secrets 是否都已添加
2. 检查 secret 名称是否与代码中匹配
3. 查看 CI 构建日志，确认 "Decode Keystore" 步骤是否执行

### 问题 4: APK 仍然是未签名的

**原因 / Cause:**
- 签名配置未正确应用
- 构建使用了错误的变体

**解决方案 / Solution:**
1. 确认使用 `assembleRelease` 而不是 `assembleDebug`
2. 检查 `app/build.gradle.kts` 中的签名配置
3. 清理构建缓存：`./gradlew clean`
4. 重新构建：`./gradlew assembleRelease`

### 问题 5: Base64 解码失败

**原因 / Cause:**
- Base64 字符串包含换行符或空格
- 复制粘贴时格式错误

**解决方案 / Solution:**
1. 使用 `tr -d '\n'` 移除换行符（Linux/macOS）
2. 或使用 `-w 0` 选项（Git Bash）：`base64 -w 0 abb-release-key.jks > keystore.b64`
3. 确保复制整个字符串，没有额外的空格或换行

## 安全最佳实践 / Security Best Practices

### 保护密钥库 / Protect Your Keystore

✅ **应该做 / DO:**
- 将密钥库文件备份到安全位置
- 使用强密码（至少 12 个字符，包含大小写字母、数字和特殊字符）
- 定期更新密码（建议每年）
- 将密钥库存储在加密的存储设备中
- 记录密钥信息（在安全的地方）

❌ **不应该做 / DON'T:**
- 不要将密钥库文件提交到 Git
- 不要在代码中硬编码密码
- 不要在不安全的渠道分享密钥信息
- 不要使用简单或常见的密码
- 不要丢失密钥库（无法恢复）

### GitHub Secrets 安全 / GitHub Secrets Security

- ✅ GitHub Secrets 是加密存储的
- ✅ Secrets 不会出现在日志中
- ✅ 只有仓库管理员可以添加/修改 Secrets
- ✅ Secrets 只在构建时通过环境变量传递
- ✅ 构建完成后，临时密钥库文件会被自动删除

### 如果密钥泄露 / If Key is Compromised

如果您怀疑密钥库或密码已泄露：

If you suspect your keystore or password has been compromised:

1. **立即更换密钥 / Immediately change keys:**
   - 生成新的密钥库
   - 更新所有 GitHub Secrets
   - 使用新密钥重新签名所有发布版本

2. **撤销旧版本 / Revoke old versions:**
   - 在 Google Play Console 中撤销使用旧密钥的版本（如果已发布）
   - 删除 GitHub Releases 中受影响的版本

3. **通知用户 / Notify users:**
   - 如果应用已公开发布，通知用户更新到新版本

## 更多资源 / Additional Resources

- [Android 应用签名官方文档](https://developer.android.com/studio/publish/app-signing)
- [GitHub Actions Secrets 文档](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [keytool 命令参考](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)
- [APK 签名方案](https://source.android.com/security/apksigning)

## 获取帮助 / Get Help

如有问题，请：

If you have questions:

1. 查看本文档的故障排查部分
2. 查看 [BUILDING.md](../BUILDING.md) 和 [RELEASE.md](../RELEASE.md)
3. 在 [GitHub Issues](https://github.com/OMOCV/Android/issues) 中提问
4. 参考 [GitHub Discussions](https://github.com/OMOCV/Android/discussions)

---

**最后更新 / Last Updated:** 2024-11-08
