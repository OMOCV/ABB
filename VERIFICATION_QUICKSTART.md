# 签名验证快速开始 / Signing Verification Quick Start

## 快速使用 / Quick Usage

在构建已签名的 APK/AAB 之前，运行验证脚本：

Before building signed APK/AAB, run the verification script:

```bash
./verify-signing-config.sh
```

## 预期输出 / Expected Output

### ✅ 成功 / Success
```
✓ All checks passed! / 所有检查通过！

You can now build signed APK/AAB with:
  ./gradlew assembleRelease    # Build signed APK
  ./gradlew bundleRelease      # Build signed AAB
```

### ❌ 失败 / Failure
如果验证失败，脚本会显示具体错误并提供修复建议。

If verification fails, the script will show specific errors and suggest fixes.

## 构建签名版本 / Build Signed Releases

### 构建 APK / Build APK
```bash
./verify-signing-config.sh && ./gradlew assembleRelease
```

### 构建 AAB / Build AAB
```bash
./verify-signing-config.sh && ./gradlew bundleRelease
```

## GitHub Actions 自动化 / GitHub Actions Automation

验证步骤已集成到 CI/CD 工作流中，会在构建前自动执行。

The verification step is integrated into CI/CD workflows and runs automatically before building.

## 详细文档 / Detailed Documentation

请参阅 [SIGNING_VERIFICATION.md](SIGNING_VERIFICATION.md) 获取完整文档。

See [SIGNING_VERIFICATION.md](SIGNING_VERIFICATION.md) for complete documentation.
