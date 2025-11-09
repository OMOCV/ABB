# 签名应用构建检查清单 / Signed Application Build Checklist

## 📋 Before You Start / 开始之前

- [ ] Read [SIGNING.md](SIGNING.md) for complete documentation
      阅读 [SIGNING.md](SIGNING.md) 获取完整文档
      
- [ ] Understand the difference between demo and production keystores
      理解演示密钥库和生产密钥库的区别

- [ ] Review [DEMO_KEYSTORE_INFO.md](DEMO_KEYSTORE_INFO.md) for security warnings
      查看 [DEMO_KEYSTORE_INFO.md](DEMO_KEYSTORE_INFO.md) 了解安全警告

## 🔧 Initial Setup / 初始设置

### For Testing/Development / 测试/开发环境

- [ ] Copy demo configuration:
      复制演示配置：
      ```bash
      cp demo-keystore.properties keystore.properties
      ```

- [ ] Verify demo keystore exists:
      验证演示密钥库存在：
      ```bash
      ls -l abb-release-key.jks
      ```

### For Production / 生产环境

- [ ] Generate your own keystore:
      生成您自己的密钥库：
      ```bash
      ./generate-keystore.sh
      ```

- [ ] Use strong passwords (min 8 characters, mixed case, numbers, symbols)
      使用强密码（至少8个字符，大小写混合，数字，符号）

- [ ] Store keystore securely (encrypted storage, access control)
      安全存储密钥库（加密存储，访问控制）

- [ ] Create encrypted backups of keystore
      创建密钥库的加密备份

- [ ] Document keystore location and backup locations
      记录密钥库位置和备份位置

- [ ] Store passwords in password manager
      在密码管理器中存储密码

## 🔨 Building / 构建

### Build Debug APK / 构建 Debug APK

- [ ] Clean project:
      清理项目：
      ```bash
      ./gradlew clean
      ```

- [ ] Build debug APK:
      构建 debug APK：
      ```bash
      ./gradlew assembleDebug
      ```

- [ ] Verify output exists:
      验证输出存在：
      ```bash
      ls -lh app/build/outputs/apk/debug/app-debug.apk
      ```

### Build Signed Release APK / 构建签名 Release APK

- [ ] Verify keystore.properties exists
      验证 keystore.properties 存在

- [ ] Build release APK:
      构建 release APK：
      ```bash
      ./gradlew assembleRelease
      ```

- [ ] Verify output is signed (not unsigned):
      验证输出已签名（非未签名）：
      ```bash
      ls -lh app/build/outputs/apk/release/app-release.apk
      ```

- [ ] Verify signature:
      验证签名：
      ```bash
      jarsigner -verify app/build/outputs/apk/release/app-release.apk
      ```

- [ ] View certificate details:
      查看证书详情：
      ```bash
      keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
      ```

### Build Signed AAB / 构建签名 AAB

- [ ] Build release AAB:
      构建 release AAB：
      ```bash
      ./gradlew bundleRelease
      ```

- [ ] Verify output exists:
      验证输出存在：
      ```bash
      ls -lh app/build/outputs/bundle/release/app-release.aab
      ```

## ✅ Testing / 测试

### Local Testing / 本地测试

- [ ] Install APK on test device:
      在测试设备上安装 APK：
      ```bash
      adb install -r app/build/outputs/apk/release/app-release.apk
      ```

- [ ] Test all major features
      测试所有主要功能

- [ ] Test on multiple Android versions
      在多个 Android 版本上测试

- [ ] Test app updates (install over existing version)
      测试应用更新（安装覆盖现有版本）

### CI/CD Testing / CI/CD 测试

- [ ] Configure GitHub Secrets (see [SIGNING.md](SIGNING.md))
      配置 GitHub Secrets（参见 [SIGNING.md](SIGNING.md)）

- [ ] Test workflow by pushing to branch
      通过推送到分支测试工作流

- [ ] Verify artifacts are created
      验证产物已创建

- [ ] Test release workflow with tag
      使用标签测试发布工作流

## 🔐 Security / 安全

### Keystore Protection / 密钥库保护

- [ ] Verify keystore files are in .gitignore
      验证密钥库文件在 .gitignore 中

- [ ] Check that keystore.properties is not committed:
      检查 keystore.properties 未提交：
      ```bash
      git status --ignored | grep keystore.properties
      ```

- [ ] Set restrictive file permissions:
      设置限制性文件权限：
      ```bash
      chmod 600 keystore.properties
      chmod 600 *.jks
      ```

- [ ] Backup keystore to secure location
      将密钥库备份到安全位置

- [ ] Test keystore backup (verify it works)
      测试密钥库备份（验证其有效性）

### Password Security / 密码安全

- [ ] Never commit passwords to version control
      永远不要将密码提交到版本控制

- [ ] Don't share passwords via insecure channels (email, chat)
      不要通过不安全的渠道共享密码（电子邮件，聊天）

- [ ] Use password manager for storage
      使用密码管理器存储

- [ ] Don't hardcode passwords in scripts or code
      不要在脚本或代码中硬编码密码

## 📦 Release Preparation / 发布准备

### Version Management / 版本管理

- [ ] Update versionCode in app/build.gradle.kts
      在 app/build.gradle.kts 中更新 versionCode

- [ ] Update versionName in app/build.gradle.kts
      在 app/build.gradle.kts 中更新 versionName

- [ ] Update CHANGELOG.md with changes
      在 CHANGELOG.md 中更新变更

### Build Verification / 构建验证

- [ ] Build with production keystore
      使用生产密钥库构建

- [ ] Verify APK is signed with production key
      验证 APK 使用生产密钥签名

- [ ] Test APK on multiple devices
      在多个设备上测试 APK

- [ ] Check APK size is reasonable
      检查 APK 大小合理

- [ ] Verify all resources are included
      验证所有资源已包含

### Release Artifacts / 发布产物

- [ ] Generate release APK
      生成发布 APK

- [ ] Generate release AAB (for Play Store)
      生成发布 AAB（用于 Play Store）

- [ ] Generate checksums:
      生成校验和：
      ```bash
      sha256sum app-release.apk > checksums.txt
      ```

- [ ] Create release notes
      创建发布说明

- [ ] Tag release in git:
      在 git 中标记发布：
      ```bash
      git tag -a v1.0.0 -m "Release 1.0.0"
      git push origin v1.0.0
      ```

## 📱 Distribution / 分发

### GitHub Releases / GitHub 发布

- [ ] Verify GitHub Actions completed successfully
      验证 GitHub Actions 成功完成

- [ ] Check release was created automatically
      检查发布已自动创建

- [ ] Verify APK and AAB are uploaded
      验证 APK 和 AAB 已上传

- [ ] Test download links
      测试下载链接

### Google Play Store (if applicable) / Google Play Store（如适用）

- [ ] Upload AAB to Play Console
      将 AAB 上传到 Play Console

- [ ] Complete store listing
      完成商店列表

- [ ] Add screenshots
      添加截图

- [ ] Submit for review
      提交审核

## 📝 Post-Release / 发布后

- [ ] Monitor crash reports
      监控崩溃报告

- [ ] Check user feedback
      检查用户反馈

- [ ] Update documentation if needed
      如需要更新文档

- [ ] Plan next release
      计划下一个版本

## ❌ Common Mistakes to Avoid / 避免常见错误

- [ ] ❌ Using demo keystore for production
      ❌ 使用演示密钥库进行生产

- [ ] ❌ Committing production keystore to git
      ❌ 将生产密钥库提交到 git

- [ ] ❌ Using weak passwords
      ❌ 使用弱密码

- [ ] ❌ Not backing up keystore
      ❌ 不备份密钥库

- [ ] ❌ Losing keystore (can't update app!)
      ❌ 丢失密钥库（无法更新应用！）

- [ ] ❌ Sharing keystore insecurely
      ❌ 不安全地共享密钥库

- [ ] ❌ Not testing signed APK before release
      ❌ 发布前不测试签名 APK

- [ ] ❌ Forgetting to update version numbers
      ❌ 忘记更新版本号

## 🆘 Troubleshooting / 故障排查

If something goes wrong, check:
如果出现问题，请检查：

- [ ] [SIGNING.md](SIGNING.md) - Troubleshooting section
      [SIGNING.md](SIGNING.md) - 故障排查部分

- [ ] [SIGNING_QUICKREF.md](SIGNING_QUICKREF.md) - Common issues
      [SIGNING_QUICKREF.md](SIGNING_QUICKREF.md) - 常见问题

- [ ] Build logs: `./gradlew assembleRelease --stacktrace`
      构建日志：`./gradlew assembleRelease --stacktrace`

- [ ] GitHub Actions logs (if using CI/CD)
      GitHub Actions 日志（如果使用 CI/CD）

## 📚 Resources / 资源

- [ ] [SIGNING.md](SIGNING.md) - Complete signing guide
      完整签名指南

- [ ] [SIGNING_QUICKREF.md](SIGNING_QUICKREF.md) - Quick reference
      快速参考

- [ ] [DEMO_KEYSTORE_INFO.md](DEMO_KEYSTORE_INFO.md) - Demo keystore info
      演示密钥库信息

- [ ] [BUILDING.md](BUILDING.md) - Build guide
      构建指南

- [ ] [RELEASE.md](RELEASE.md) - Release guide
      发布指南

- [ ] `./generate-keystore.sh` - Keystore generation script
      密钥库生成脚本

- [ ] `./example-signed-build.sh` - Example workflow
      示例工作流

---

**Remember:** Losing your keystore means you can't update your app!
**记住：** 丢失密钥库意味着您无法更新应用！

**Always keep secure backups!**
**始终保留安全备份！**
