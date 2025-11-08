# 签名工作流程图 / Signing Workflow Diagram

## 整体架构 / Overall Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Android APK 签名系统                          │
│                  Android APK Signing System                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ├─────────────┬────────────────┐
                              │             │                │
                    ┌─────────▼──────┐ ┌───▼─────────┐ ┌───▼──────────┐
                    │  本地开发环境   │ │  GitHub     │ │  其他 CI/CD  │
                    │  Local Dev Env │ │  Actions    │ │  Other CI/CD │
                    └────────────────┘ └─────────────┘ └──────────────┘
```

## 本地开发流程 / Local Development Flow

```
┌──────────────────────────────────────────────────────────────────┐
│ 步骤 1: 生成密钥库 / Step 1: Generate Keystore                    │
└──────────────────────────────────────────────────────────────────┘
                              │
                              │ keytool -genkey
                              ▼
                    ┌──────────────────┐
                    │ abb-release-key  │
                    │     .jks         │
                    └──────────────────┘
                              │
                              │
┌──────────────────────────────────────────────────────────────────┐
│ 步骤 2: 创建配置文件 / Step 2: Create Config File                  │
└──────────────────────────────────────────────────────────────────┘
                              │
                              │ 手动创建 / Manual Create
                              ▼
                    ┌──────────────────┐
                    │ keystore         │
                    │ .properties      │
                    │                  │
                    │ KEYSTORE_FILE=   │
                    │ KEYSTORE_PASSWORD│
                    │ KEY_ALIAS=       │
                    │ KEY_PASSWORD=    │
                    └──────────────────┘
                              │
                              │
┌──────────────────────────────────────────────────────────────────┐
│ 步骤 3: 构建签名版本 / Step 3: Build Signed Release               │
└──────────────────────────────────────────────────────────────────┘
                              │
                              │ ./gradlew assembleRelease
                              ▼
                    ┌──────────────────┐
                    │  app-release.apk │
                    │    (signed)      │
                    └──────────────────┘
```

## GitHub Actions 流程 / GitHub Actions Flow

```
┌──────────────────────────────────────────────────────────────────┐
│ 步骤 1: 准备密钥库 / Step 1: Prepare Keystore                     │
└──────────────────────────────────────────────────────────────────┘
         本地 / Local                      GitHub 仓库 / GitHub Repo
    ┌──────────────────┐                 ┌───────────────────────┐
    │ abb-release-key  │                 │                       │
    │     .jks         │   Base64        │   GitHub Secrets:     │
    │                  │────编码──────────▶│                       │
    │  (Binary File)   │   Encode        │   KEYSTORE_FILE       │
    │                  │                 │   KEYSTORE_PASSWORD   │
    └──────────────────┘                 │   KEY_ALIAS           │
                                         │   KEY_PASSWORD        │
                                         └───────────────────────┘
                                                    │
                                                    │
┌──────────────────────────────────────────────────────────────────┐
│ 步骤 2: 触发工作流 / Step 2: Trigger Workflow                     │
└──────────────────────────────────────────────────────────────────┘
                                                    │
                    ┌───────────────────────────────┼──────────────┐
                    │                               │              │
            ┌───────▼────────┐              ┌──────▼──────┐  ┌───▼─────┐
            │ git push       │              │ git tag     │  │ Manual  │
            │ to main/develop│              │ v1.0.0      │  │ Trigger │
            └────────────────┘              └─────────────┘  └─────────┘
                    │                               │              │
                    └───────────────────────────────┴──────────────┘
                                                    │
                                                    │
┌──────────────────────────────────────────────────────────────────┐
│ 步骤 3: GitHub Actions 工作流 / Step 3: GitHub Actions Workflow  │
└──────────────────────────────────────────────────────────────────┘
                                                    │
                              ┌─────────────────────▼─────────────────┐
                              │  Checkout Code                         │
                              └─────────────────────┬─────────────────┘
                                                    │
                              ┌─────────────────────▼─────────────────┐
                              │  Setup JDK 17                          │
                              └─────────────────────┬─────────────────┘
                                                    │
                              ┌─────────────────────▼─────────────────┐
                              │  Decode Keystore (if configured)      │
                              │                                        │
                              │  IF secrets.KEYSTORE_FILE != '':      │
                              │    1. Base64 decode keystore          │
                              │    2. Save to $HOME/keystore.jks      │
                              │    3. Export environment variables:   │
                              │       - KEYSTORE_FILE                 │
                              │       - KEYSTORE_PASSWORD             │
                              │       - KEY_ALIAS                     │
                              │       - KEY_PASSWORD                  │
                              └─────────────────────┬─────────────────┘
                                                    │
                              ┌─────────────────────▼─────────────────┐
                              │  Build Debug APK & AAB                 │
                              └─────────────────────┬─────────────────┘
                                                    │
                              ┌─────────────────────▼─────────────────┐
                              │  Build Release APK & AAB               │
                              │                                        │
                              │  ./gradlew assembleRelease             │
                              │  ./gradlew bundleRelease               │
                              │                                        │
                              │  continue-on-error:                    │
                              │    ${{ secrets.KEYSTORE_FILE == '' }}  │
                              └─────────────────────┬─────────────────┘
                                                    │
                                    ┌───────────────┴──────────────┐
                                    │                              │
                        ┌───────────▼──────────┐      ┌───────────▼──────────┐
                        │  签名成功 / Signed    │      │  未配置签名 / No Sign │
                        │                      │      │                      │
                        │  app-release.apk     │      │  app-release-        │
                        │  (signed)            │      │  unsigned.apk        │
                        └──────────────────────┘      └──────────────────────┘
                                    │                              │
                                    └───────────────┬──────────────┘
                                                    │
                              ┌─────────────────────▼─────────────────┐
                              │  Upload Artifacts                      │
                              └─────────────────────┬─────────────────┘
                                                    │
                              ┌─────────────────────▼─────────────────┐
                              │  Create Release (if tag push)          │
                              └────────────────────────────────────────┘
```

## 签名配置读取流程 / Signing Configuration Flow

```
┌────────────────────────────────────────────────────────────────┐
│         Gradle 构建过程 / Gradle Build Process                  │
└────────────────────────────────────────────────────────────────┘
                              │
                              │ Start Build
                              ▼
                    ┌──────────────────┐
                    │  Read Signing    │
                    │  Configuration   │
                    └──────────────────┘
                              │
                              ├────────────┬─────────────┐
                              │            │             │
                    ┌─────────▼─────┐ ┌───▼────────┐ ┌─▼──────────┐
                    │ Environment   │ │ Project    │ │ No Config  │
                    │ Variables     │ │ Properties │ │            │
                    │               │ │            │ │            │
                    │ KEYSTORE_FILE │ │ keystore   │ │            │
                    │ (CI/CD)       │ │ .properties│ │            │
                    │               │ │ (Local)    │ │            │
                    └───────┬───────┘ └────┬───────┘ └─┬──────────┘
                            │              │           │
                            │   Priority   │           │
                            │   Order      │           │
                            │      1───────┘           │
                            │              2───────────┘
                            │                          3
                            │
                            ▼
                    ┌──────────────────┐
                    │  Validate Config │
                    │                  │
                    │  All Required    │
                    │  Values Present? │
                    └──────┬───────────┘
                           │
                    ┌──────┴──────┐
                    │             │
              ┌─────▼────┐   ┌────▼─────┐
              │   Yes    │   │    No    │
              └─────┬────┘   └────┬─────┘
                    │             │
        ┌───────────▼──────┐  ┌───▼─────────────┐
        │  Apply Signing   │  │  Build Unsigned │
        │  Configuration   │  │  APK            │
        └───────────┬──────┘  └───┬─────────────┘
                    │             │
                    │             │
        ┌───────────▼──────┐  ┌───▼─────────────┐
        │  Sign APK/AAB    │  │  Output Unsigned│
        │  with Key        │  │  APK            │
        └───────────┬──────┘  └───┬─────────────┘
                    │             │
                    └──────┬──────┘
                           │
                           ▼
                    ┌──────────────┐
                    │    Output    │
                    └──────────────┘
```

## 安全流程 / Security Flow

```
┌────────────────────────────────────────────────────────────────┐
│           安全措施层次 / Security Measures Layers               │
└────────────────────────────────────────────────────────────────┘

Level 1: 源文件保护 / Source File Protection
┌────────────────────────────────────────────────────────────────┐
│  .gitignore                                                    │
│  ├── *.jks              ✓ 密钥库文件 / Keystore files         │
│  ├── *.keystore         ✓ 密钥库文件 / Keystore files         │
│  └── keystore.properties ✓ 配置文件 / Config file             │
└────────────────────────────────────────────────────────────────┘
                              │ Protected
                              ▼

Level 2: 密钥传输保护 / Key Transmission Protection
┌────────────────────────────────────────────────────────────────┐
│  Base64 Encoding                                               │
│  ├── Binary → Text      ✓ GitHub Secrets 支持 / Compatible    │
│  ├── No Line Breaks     ✓ 避免传输错误 / Avoid errors          │
│  └── Reversible         ✓ 可解码使用 / Can be decoded          │
└────────────────────────────────────────────────────────────────┘
                              │ Encoded
                              ▼

Level 3: GitHub Secrets 保护 / GitHub Secrets Protection
┌────────────────────────────────────────────────────────────────┐
│  GitHub Repository Secrets                                     │
│  ├── Encrypted Storage  ✓ 加密存储 / Encrypted                │
│  ├── Access Control     ✓ 权限控制 / Access controlled         │
│  ├── Audit Logging      ✓ 审计日志 / Audit logged             │
│  └── Not in Logs        ✓ 不在日志中显示 / Hidden from logs    │
└────────────────────────────────────────────────────────────────┘
                              │ Secured
                              ▼

Level 4: 运行时保护 / Runtime Protection
┌────────────────────────────────────────────────────────────────┐
│  GitHub Actions Runner                                         │
│  ├── Temporary File     ✓ 临时文件 / Temporary only           │
│  ├── Isolated VM        ✓ 隔离虚拟机 / Isolated                │
│  ├── Auto Cleanup       ✓ 自动清理 / Auto cleaned             │
│  └── No Persistence     ✓ 不持久化 / Not persisted            │
└────────────────────────────────────────────────────────────────┘
                              │ Protected
                              ▼

Level 5: 输出保护 / Output Protection
┌────────────────────────────────────────────────────────────────┐
│  Built Artifacts                                               │
│  ├── Signed APK         ✓ 签名的 APK / Signed APK             │
│  ├── Secure Distribution✓ 安全分发 / Secure distribution       │
│  └── Verifiable         ✓ 可验证 / Verifiable                 │
└────────────────────────────────────────────────────────────────┘
```

## 文件依赖关系图 / File Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                    项目文件结构 / Project Structure              │
└─────────────────────────────────────────────────────────────────┘

Android/
├── app/
│   └── build.gradle.kts ◀─────┐ 读取签名配置
│       (Signing Config)       │ Read signing config
│                               │
├── .github/                    │
│   └── workflows/              │
│       ├── build-apk.yml ◀─────┤ 设置环境变量
│       │   (Decode keystore)   │ Set env vars
│       └── release.yml ◀───────┘
│           (Decode keystore)
│
├── keystore.properties ────────▶ 本地开发使用
│   (Local Development)           Local dev only
│
├── abb-release-key.jks ────────▶ 本地密钥库
│   (Local Keystore)              Local keystore
│
├── .gitignore ─────────────────▶ 保护敏感文件
│   (Protect secrets)             Protect secrets
│
└── docs/
    ├── SIGNING_SETUP.md ───────▶ 完整配置指南
    │   (Complete guide)          Complete guide
    │
    ├── SIGNING_QUICK_REF.md ───▶ 快速参考
    │   (Quick reference)         Quick reference
    │
    └── SIGNING_IMPLEMENTATION──▶ 技术实现
        .md (Implementation)      Technical details

GitHub Repository Secrets:
├── KEYSTORE_FILE ──────────────▶ Base64 编码的密钥库
│   (Base64 keystore)             Base64 keystore
├── KEYSTORE_PASSWORD ──────────▶ 密钥库密码
│   (Keystore password)           Keystore password
├── KEY_ALIAS ──────────────────▶ 密钥别名
│   (Key alias)                   Key alias
└── KEY_PASSWORD ───────────────▶ 密钥密码
    (Key password)                Key password
```

## 决策流程图 / Decision Flow

```
                    开始构建 / Start Build
                            │
                            ▼
                ┌────────────────────────┐
                │  检查签名配置           │
                │  Check Signing Config  │
                └───────────┬────────────┘
                            │
                    ┌───────┴───────┐
                    │               │
            ┌───────▼──────┐  ┌────▼─────────┐
            │ 配置存在      │  │ 配置不存在    │
            │ Config Exists│  │ No Config    │
            └───────┬──────┘  └────┬─────────┘
                    │              │
        ┌───────────▼─────┐  ┌────▼─────────────┐
        │ 验证配置完整性   │  │ 继续构建         │
        │ Validate Config │  │ Continue Build   │
        └───────────┬─────┘  │ (continue-on-    │
                    │        │  error: true)    │
            ┌───────┴──────┐ └────┬─────────────┘
            │              │      │
      ┌─────▼────┐   ┌─────▼────┐ │
      │ 完整     │   │ 不完整    │ │
      │ Complete │   │ Incomplete│ │
      └─────┬────┘   └─────┬────┘ │
            │              │      │
            │              └──────┴────────┐
            │                              │
  ┌─────────▼────────┐          ┌─────────▼──────────┐
  │ 构建签名版本      │          │ 构建未签名版本      │
  │ Build Signed     │          │ Build Unsigned     │
  │                  │          │                    │
  │ ✓ app-release   │          │ ⚠ app-release-    │
  │   .apk (signed) │          │   unsigned.apk    │
  │ ✓ app-release   │          │ ⚠ Warning in log  │
  │   .aab (signed) │          │                    │
  └──────────────────┘          └────────────────────┘
```

---

**说明 / Notes:**
- ✓ = 成功状态 / Success state
- ⚠ = 警告状态 / Warning state
- ◀ = 依赖关系 / Dependency
- │ = 流程方向 / Flow direction
- ┌─┐ = 决策点 / Decision point
