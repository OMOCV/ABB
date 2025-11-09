# 签名快速参考 / Signing Quick Reference

## 🚀 快速开始 / Quick Start

### 首次设置 / First Time Setup

```bash
# 1. 生成密钥库 / Generate keystore
./generate-keystore.sh

# 按提示输入信息 / Follow the prompts to enter information
```

### 构建签名应用 / Build Signed App

```bash
# 构建签名 APK / Build signed APK
./gradlew assembleRelease

# 构建签名 AAB / Build signed AAB
./gradlew bundleRelease
```

### 验证签名 / Verify Signature

```bash
# 验证 APK 签名 / Verify APK signature
jarsigner -verify -verbose app/build/outputs/apk/release/app-release.apk

# 查看签名详情 / View signature details
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

## 📁 重要文件 / Important Files

| 文件 / File | 说明 / Description | 提交? / Commit? |
|-------------|-------------------|----------------|
| `abb-release-key.jks` | 密钥库文件 / Keystore file | ❌ 不要提交 / NO |
| `keystore.properties` | 签名配置 / Signing config | ❌ 不要提交 / NO |
| `generate-keystore.sh` | 生成脚本 / Generation script | ✅ 提交 / YES |
| `SIGNING.md` | 详细文档 / Detailed docs | ✅ 提交 / YES |

## ⚙️ GitHub Actions 配置 / GitHub Actions Setup

### 需要的 Secrets / Required Secrets

在 `Settings > Secrets and variables > Actions` 添加:

Add in `Settings > Secrets and variables > Actions`:

1. **KEYSTORE_BASE64** - Base64 编码的密钥库
2. **KEYSTORE_PASSWORD** - 密钥库密码
3. **KEY_ALIAS** - 密钥别名
4. **KEY_PASSWORD** - 密钥密码

### 生成 Base64 / Generate Base64

```bash
# Linux/macOS
base64 abb-release-key.jks | tr -d '\n' > keystore.b64

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("abb-release-key.jks")) > keystore.b64
```

## 🔐 安全提醒 / Security Reminders

### ✅ 必须做 / Must Do
- 备份密钥库到安全位置 / Backup keystore securely
- 使用强密码 / Use strong passwords
- 限制文件访问权限 / Limit file access

### ❌ 不要做 / Don't Do
- 不要提交密钥库 / Don't commit keystore
- 不要分享密码 / Don't share passwords
- 不要在代码中硬编码 / Don't hardcode in code

## 🆘 常见问题 / Common Issues

### 问题: "keystore.properties not found"

**解决 / Solution:**
```bash
./generate-keystore.sh
```

### 问题: "password incorrect"

**解决 / Solution:**
检查 `keystore.properties` 中的密码是否正确
Check passwords in `keystore.properties`

### 问题: Release APK 未签名

**解决 / Solution:**
```bash
./gradlew clean assembleRelease --stacktrace
```

## 📚 更多信息 / More Information

- 详细文档 / Detailed docs: [SIGNING.md](SIGNING.md)
- 构建指南 / Build guide: [BUILDING.md](BUILDING.md)
- 发布指南 / Release guide: [RELEASE.md](RELEASE.md)

## 🔄 完整流程示例 / Complete Workflow Example

```bash
# 1. 首次设置 / First time setup
./generate-keystore.sh
# 按提示输入: 密码、证书信息等
# Enter: passwords, certificate info, etc.

# 2. 构建应用 / Build app
./gradlew clean
./gradlew assembleRelease

# 3. 验证签名 / Verify signature
jarsigner -verify app/build/outputs/apk/release/app-release.apk

# 4. 测试安装 / Test installation
adb install -r app/build/outputs/apk/release/app-release.apk
```

## 📞 获取帮助 / Get Help

- 查看 Issues: https://github.com/OMOCV/Android/issues
- 阅读文档: [SIGNING.md](SIGNING.md)
