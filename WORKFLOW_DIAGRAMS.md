# 工作流程图 / Workflow Diagrams

## CI/CD 流程概览 / CI/CD Process Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                     代码仓库 / Repository                          │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
                ┌───────────────────────────────┐
                │    Git 操作 / Git Operations   │
                └───────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐    ┌──────────────────┐    ┌──────────────────┐
│  Push/PR to   │    │  Manual Trigger  │    │   Push Tag       │
│  main/develop │    │  (workflow_      │    │   (v*.*.*)       │
│               │    │   dispatch)      │    │                  │
└───────────────┘    └──────────────────┘    └──────────────────┘
        │                       │                       │
        ▼                       ▼                       │
┌─────────────────────────────────────────┐            │
│   Build APK Workflow                     │            │
│   (.github/workflows/build-apk.yml)     │            │
└─────────────────────────────────────────┘            │
        │                                               │
        ├──► Build Debug APK                           │
        ├──► Build Release APK (optional)              │
        ├──► Rename with version                       │
        └──► Upload to Artifacts                       │
                                                        ▼
                                        ┌───────────────────────────┐
                                        │   Release Workflow        │
                                        │   (.github/workflows/     │
                                        │    release.yml)           │
                                        └───────────────────────────┘
                                                        │
                                        ├──► Build Debug APK
                                        ├──► Build Release APK
                                        ├──► Generate Checksums
                                        ├──► Create GitHub Release
                                        └──► Upload APK to Release
                                                        │
                                                        ▼
                                        ┌───────────────────────────┐
                                        │   GitHub Release          │
                                        │   - APK Files             │
                                        │   - Checksums             │
                                        │   - Release Notes         │
                                        └───────────────────────────┘
```

## 构建工作流详细流程 / Build Workflow Details

```
┌─────────────────────────────────────────────────────────────┐
│              Build APK Workflow                              │
│              (.github/workflows/build-apk.yml)              │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  1. Checkout Repository           │
        │     - fetch-depth: 0              │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  2. Setup JDK 17                  │
        │     - Temurin Distribution        │
        │     - Gradle Cache                │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  3. Get Version Information       │
        │     - From tag if available       │
        │     - From git describe           │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  4. Build Debug APK               │
        │     ./gradlew assembleDebug       │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  5. Build Release APK             │
        │     ./gradlew assembleRelease     │
        │     (continue-on-error: true)     │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  6. Rename APK Files              │
        │     - Add version to filename     │
        │     - Copy to dist/ folder        │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  7. Upload Artifacts              │
        │     - Debug APK                   │
        │     - Release APK (if available)  │
        └───────────────────────────────────┘
```

## 发布工作流详细流程 / Release Workflow Details

```
┌─────────────────────────────────────────────────────────────┐
│              Release Workflow                                │
│              (.github/workflows/release.yml)                │
│              Trigger: tags (v*.*.*)                         │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  1. Extract Version from Tag      │
        │     - v1.0.0 → 1.0.0             │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  2. Setup Build Environment       │
        │     - Checkout code               │
        │     - Setup JDK 17                │
        │     - Cache Gradle                │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  3. Build APKs                    │
        │     - Debug APK                   │
        │     - Release APK (unsigned)      │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  4. Prepare Release Artifacts     │
        │     - Rename APK files            │
        │     - Generate SHA256 checksums   │
        │     - List all artifacts          │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  5. Create GitHub Release         │
        │     - Upload APK files            │
        │     - Upload checksums            │
        │     - Generate release notes      │
        │     - Set pre-release flag        │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  6. Archive Build Artifacts       │
        │     - Retention: 90 days          │
        └───────────────────────────────────┘
```

## 触发条件矩阵 / Trigger Conditions Matrix

```
┌───────────────────┬──────────────┬───────────────┬────────────────┐
│ Event             │ Build APK    │ Release       │ Creates        │
│                   │ Workflow     │ Workflow      │ GitHub Release │
├───────────────────┼──────────────┼───────────────┼────────────────┤
│ Push to main      │      ✓       │       -       │       -        │
│ Push to develop   │      ✓       │       -       │       -        │
│ Pull Request      │      ✓       │       -       │       -        │
│ Manual Trigger    │      ✓       │       -       │       -        │
│ Tag (v*.*.*)      │      ✓       │       ✓       │       ✓        │
└───────────────────┴──────────────┴───────────────┴────────────────┘
```

## 产物生成流程 / Artifact Generation Flow

```
┌──────────────────────────────────────────────────────────────┐
│                  Source Code Repository                       │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
                  ┌─────────────────┐
                  │  Gradle Build   │
                  └─────────────────┘
                            │
        ┌───────────────────┴───────────────────┐
        │                                       │
        ▼                                       ▼
┌──────────────────┐                  ┌──────────────────┐
│   Debug Build    │                  │  Release Build   │
│   - No signing   │                  │  - Unsigned or   │
│   - No obfuscate │                  │    Signed        │
│   - Debug info   │                  │  - Obfuscated    │
└──────────────────┘                  │  - Optimized     │
        │                             └──────────────────┘
        │                                       │
        ▼                                       ▼
┌──────────────────┐                  ┌──────────────────┐
│  app-debug.apk   │                  │ app-release-     │
│                  │                  │  unsigned.apk    │
└──────────────────┘                  └──────────────────┘
        │                                       │
        └───────────────────┬───────────────────┘
                            │
                            ▼
                  ┌─────────────────┐
                  │ Rename & Copy   │
                  │  to dist/       │
                  └─────────────────┘
                            │
        ┌───────────────────┴───────────────────┐
        │                                       │
        ▼                                       ▼
┌────────────────────┐              ┌───────────────────────┐
│ ABB-{version}-     │              │ ABB-{version}-        │
│   debug.apk        │              │   release-unsigned.apk│
└────────────────────┘              └───────────────────────┘
        │                                       │
        └───────────────────┬───────────────────┘
                            │
                            ▼
                  ┌─────────────────┐
                  │ Generate SHA256 │
                  │   Checksums     │
                  └─────────────────┘
                            │
                            ▼
                  ┌─────────────────┐
                  │   checksums-    │
                  │    sha256.txt   │
                  └─────────────────┘
                            │
                            ▼
        ┌───────────────────┴───────────────────┐
        │                                       │
        ▼                                       ▼
┌────────────────────┐              ┌───────────────────────┐
│ Upload to GitHub   │              │ Upload to GitHub      │
│   Actions          │              │   Releases (tags only)│
│   Artifacts        │              │                       │
└────────────────────┘              └───────────────────────┘
```

## 版本发布时间线 / Release Timeline

```
Developer Actions                    GitHub Actions
─────────────────                   ─────────────────

1. Code changes                     
   └─► git add .
   └─► git commit -m "..."
   └─► git push origin main
                                    ├─► Trigger Build APK
                                    ├─► Run tests (if any)
                                    ├─► Build APKs
                                    └─► Upload artifacts

2. Prepare release
   └─► Update version in gradle
   └─► Update CHANGELOG.md
   └─► git commit -m "Prepare v1.0.0"
   └─► git push origin main

3. Create release tag
   └─► git tag -a v1.0.0 -m "Release"
   └─► git push origin v1.0.0
                                    ├─► Trigger Build APK
                                    ├─► Trigger Release Workflow
                                    │   ├─► Build APKs
                                    │   ├─► Generate checksums
                                    │   ├─► Create Release
                                    │   └─► Upload to Release
                                    └─► Workflow complete

4. Verify release
   └─► Check GitHub Releases page
   └─► Download and test APK
   └─► Verify checksums
```

## 权限和安全流程 / Permissions and Security Flow

```
┌─────────────────────────────────────────────────────────┐
│                GitHub Repository                         │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
        ┌───────────────────────────────┐
        │  Workflow Permissions          │
        │  - contents: write             │
        │  - GITHUB_TOKEN (automatic)    │
        └───────────────────────────────┘
                        │
        ┌───────────────┴───────────────┐
        │                               │
        ▼                               ▼
┌──────────────────┐        ┌──────────────────┐
│  Build Actions   │        │  Release Actions │
│  - Read code     │        │  - Read code     │
│  - Build APK     │        │  - Build APK     │
│  - Upload        │        │  - Create Release│
│    artifacts     │        │  - Upload assets │
└──────────────────┘        └──────────────────┘
                                       │
                        ┌──────────────┴──────────────┐
                        │                             │
                        ▼                             ▼
            ┌───────────────────┐        ┌────────────────────┐
            │  Optional Secrets │        │  Public Releases   │
            │  (for signing)    │        │  - APK files       │
            │  - KEYSTORE_FILE  │        │  - Checksums       │
            │  - KEYSTORE_PWD   │        │  - Release notes   │
            │  - KEY_ALIAS      │        └────────────────────┘
            │  - KEY_PASSWORD   │
            └───────────────────┘
```

## 故障恢复流程 / Failure Recovery Flow

```
Build Failure
      │
      ├─► Check logs in Actions
      │
      ├─► Identify issue:
      │   ├─► Dependency issue → Update dependencies
      │   ├─► Code error → Fix code
      │   ├─► Config error → Fix gradle config
      │   └─► Network issue → Retry build
      │
      └─► Fix and retry:
          ├─► git commit -m "Fix build issue"
          ├─► git push origin main
          └─► Re-run workflow or create new tag

Release Failure
      │
      ├─► Delete failed release (if created)
      │
      ├─► Delete tag locally and remotely:
      │   ├─► git tag -d v1.0.0
      │   └─► git push origin :refs/tags/v1.0.0
      │
      ├─► Fix the issue
      │
      └─► Create new tag:
          ├─► git tag -a v1.0.1 -m "Fixed release"
          └─► git push origin v1.0.1
```

## 使用场景示例 / Usage Scenarios

### 场景 1: 日常开发 / Scenario 1: Daily Development
```
Developer → Push code → Build APK Workflow → Artifacts available
```

### 场景 2: 创建发布 / Scenario 2: Create Release
```
Developer → Create tag → Release Workflow → GitHub Release created
```

### 场景 3: 手动构建 / Scenario 3: Manual Build
```
Developer → Trigger workflow manually → Build APK Workflow → Artifacts available
```

### 场景 4: 修复发布 / Scenario 4: Fix Release
```
Developer → Delete tag → Fix issues → Create new tag → New release created
```

---

**说明 / Notes**:
- ✓ = 执行 / Executed
- - = 不执行 / Not executed
- → = 流程方向 / Flow direction
- │ = 流程分支 / Branch
- ├ = 分支点 / Branch point
- └ = 流程结束 / End point
