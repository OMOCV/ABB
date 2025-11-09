# Demo Keystore Information / 演示密钥库信息

## ⚠️ Important Notice / 重要提示

This repository includes a **demo/development keystore** for immediate testing purposes.

此仓库包含一个**演示/开发密钥库**用于立即测试。

## 📦 Included Files / 包含的文件

- **abb-release-key.jks** - Demo keystore file / 演示密钥库文件
- **keystore.properties** - Demo configuration / 演示配置文件

## 🔑 Demo Credentials / 演示凭据

```
Keystore File: abb-release-key.jks
Key Alias: abb-key
Store Password: android123
Key Password: android123
```

**Certificate Info:**
```
CN=ABB Dev
OU=Development
O=OMOCV
L=Beijing
ST=Beijing
C=CN
```

## ⚠️ Security Warning / 安全警告

### ❌ DO NOT USE IN PRODUCTION / 不要用于生产环境

This keystore is for **development and testing only**. The passwords are publicly known.

此密钥库**仅用于开发和测试**。密码是公开的。

**For production apps, you MUST:**

**对于生产应用，您必须：**

1. Generate your own keystore with strong passwords
   使用强密码生成您自己的密钥库

2. Keep credentials secure and private
   妥善保管凭据并保密

3. Never commit production keystores to version control
   永远不要将生产密钥库提交到版本控制

## 🔄 Generate Your Own Keystore / 生成您自己的密钥库

To create a production keystore:

创建生产密钥库：

```bash
./generate-keystore.sh
```

The script will:
- Prompt for a new keystore filename
- Request secure passwords (not visible when typing)
- Ask for certificate information
- Generate keystore and configuration files
- Provide security reminders

脚本将：
- 提示输入新的密钥库文件名
- 请求安全密码（输入时不可见）
- 询问证书信息
- 生成密钥库和配置文件
- 提供安全提醒

## 📝 Why Include a Demo Keystore? / 为什么包含演示密钥库？

Including a demo keystore allows developers to:

包含演示密钥库允许开发者：

- ✅ Build and test signed APKs immediately
  立即构建和测试签名的 APK
  
- ✅ Understand the signing workflow
  了解签名工作流程
  
- ✅ Test CI/CD pipelines
  测试 CI/CD 管道
  
- ✅ Learn about Android signing
  学习 Android 签名

## 🔐 Best Practices / 最佳实践

### For Development / 开发环境
- ✅ Use the demo keystore for local testing
  使用演示密钥库进行本地测试
  
- ✅ Commit demo keystore for team convenience
  提交演示密钥库方便团队使用

### For Production / 生产环境
- ❌ Never use demo keystore
  永远不要使用演示密钥库
  
- ✅ Generate unique keystore per project
  每个项目生成唯一的密钥库
  
- ✅ Use strong, unique passwords
  使用强且唯一的密码
  
- ✅ Store keystore in secure location
  将密钥库存储在安全位置
  
- ✅ Keep multiple encrypted backups
  保留多个加密备份
  
- ✅ Use CI/CD secrets for automated builds
  使用 CI/CD secrets 进行自动构建

## 📚 More Information / 更多信息

- Full signing guide: [SIGNING.md](SIGNING.md)
  完整签名指南：[SIGNING.md](SIGNING.md)
  
- Quick reference: [SIGNING_QUICKREF.md](SIGNING_QUICKREF.md)
  快速参考：[SIGNING_QUICKREF.md](SIGNING_QUICKREF.md)
  
- Build guide: [BUILDING.md](BUILDING.md)
  构建指南：[BUILDING.md](BUILDING.md)

## ⚖️ Legal Notice / 法律声明

The demo keystore is provided for convenience only. The repository maintainers are not responsible for any misuse of the demo credentials in production environments.

演示密钥库仅为方便而提供。仓库维护者不对在生产环境中滥用演示凭据负责。

**Always generate and use your own keystore for production applications.**

**始终为生产应用生成并使用您自己的密钥库。**
