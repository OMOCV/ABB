# 签名配置快速参考 / Signing Configuration Quick Reference

## 快速设置步骤 / Quick Setup Steps

### 1️⃣ 生成密钥库 / Generate Keystore

```bash
keytool -genkey -v -keystore abb-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias abb-key
```

### 2️⃣ 本地配置 / Local Configuration

创建 `keystore.properties`:

```properties
KEYSTORE_FILE=abb-release-key.jks
KEYSTORE_PASSWORD=your_password
KEY_ALIAS=abb-key
KEY_PASSWORD=your_key_password
```

### 3️⃣ GitHub Secrets 配置 / GitHub Secrets Setup

```bash
# 生成 Base64
base64 abb-release-key.jks | tr -d '\n' > keystore.b64
```

在 GitHub 仓库添加 4 个 Secrets:
- `KEYSTORE_BASE64` → keystore.b64 的内容
- `KEYSTORE_PASSWORD` → 密钥库密码
- `KEY_ALIAS` → `abb-key`
- `KEY_PASSWORD` → 密钥密码

### 4️⃣ 构建 / Build

```bash
# 本地构建 / Local build
./gradlew assembleRelease

# 或推送 tag 触发 CI / Or push tag to trigger CI
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

## 重要提示 / Important Notes

⚠️ **不要提交 / DO NOT COMMIT:**
- `abb-release-key.jks`
- `keystore.properties`
- `keystore.b64`

✅ **已自动配置 / Already Configured:**
- `app/build.gradle.kts` - 签名配置
- `.github/workflows/*.yml` - CI/CD 签名支持
- `.gitignore` - 排除敏感文件

## 验证签名 / Verify Signing

```bash
# 验证 APK 签名
apksigner verify --verbose app-release.apk

# 查看签名信息
jarsigner -verify -verbose -certs app-release.apk
```

## 命令速查 / Command Cheatsheet

| 操作 / Action | 命令 / Command |
|--------------|----------------|
| 生成密钥 | `keytool -genkey -v -keystore abb-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias abb-key` |
| Base64 编码 (Linux/Mac) | `base64 abb-release-key.jks \| tr -d '\n' > keystore.b64` |
| Base64 编码 (Windows PS) | `[Convert]::ToBase64String([IO.File]::ReadAllBytes("abb-release-key.jks")) > keystore.b64` |
| 构建签名 APK | `./gradlew assembleRelease` |
| 构建签名 AAB | `./gradlew bundleRelease` |
| 验证签名 | `apksigner verify --verbose app-release.apk` |
| 清理构建 | `./gradlew clean` |

## 文件位置 / File Locations

| 文件 / File | 位置 / Location |
|------------|-----------------|
| 签名的 APK | `app/build/outputs/apk/release/app-release.apk` |
| 未签名的 APK | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| 签名的 AAB | `app/build/outputs/bundle/release/app-release.aab` |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |

## 故障排查 / Quick Troubleshooting

| 问题 / Issue | 解决方案 / Solution |
|-------------|-------------------|
| Keystore not found | 检查文件路径，确认 `KEYSTORE_FILE` 正确 |
| Incorrect password | 验证 `KEYSTORE_PASSWORD` 和 `KEY_PASSWORD` |
| Base64 decode error | 确保编码时使用 `tr -d '\n'` 移除换行符 |
| 未签名的 APK | 确认签名配置正确，使用 `assembleRelease` |
| CI 构建失败 | 检查所有 4 个 GitHub Secrets 是否已添加 |

## 详细文档 / Detailed Documentation

📚 完整指南 / Full Guide: [docs/SIGNING_SETUP.md](./SIGNING_SETUP.md)  
🏗️ 构建指南 / Build Guide: [BUILDING.md](../BUILDING.md)  
🚀 发布指南 / Release Guide: [RELEASE.md](../RELEASE.md)

---

**提示 / Tip:** 妥善保管密钥库文件和密码，丢失后无法恢复！  
**Tip:** Keep your keystore file and passwords safe - they cannot be recovered if lost!
