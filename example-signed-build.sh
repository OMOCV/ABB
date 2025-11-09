#!/bin/bash

# Example workflow for building signed Android applications
# 构建签名 Android 应用的示例工作流

echo "======================================"
echo "ABB Android - Signed Build Example"
echo "ABB Android - 签名构建示例"
echo "======================================"
echo ""

# Check if running in project root
if [ ! -f "build.gradle.kts" ]; then
    echo "❌ Error: This script must be run from the project root directory"
    echo "❌ 错误：此脚本必须从项目根目录运行"
    exit 1
fi

echo "This script demonstrates how to build a signed Android application."
echo "此脚本演示如何构建签名的 Android 应用。"
echo ""

# Check if keystore.properties exists
if [ ! -f "keystore.properties" ]; then
    echo "⚠️  Notice: keystore.properties not found"
    echo "⚠️  提示：未找到 keystore.properties"
    echo ""
    echo "You have two options:"
    echo "您有两个选择："
    echo ""
    echo "1. Use demo keystore (for testing only):"
    echo "   使用演示密钥库（仅用于测试）："
    echo "   cp demo-keystore.properties keystore.properties"
    echo ""
    echo "2. Generate your own keystore (recommended for production):"
    echo "   生成您自己的密钥库（推荐用于生产）："
    echo "   ./generate-keystore.sh"
    echo ""
    
    read -p "Do you want to use demo keystore for this example? (yes/no): " USE_DEMO
    
    if [ "$USE_DEMO" = "yes" ]; then
        echo ""
        echo "Copying demo-keystore.properties to keystore.properties..."
        echo "正在将 demo-keystore.properties 复制到 keystore.properties..."
        cp demo-keystore.properties keystore.properties
        echo "✅ Done / 完成"
        echo ""
        echo "⚠️  Remember: Demo keystore is for testing only!"
        echo "⚠️  记住：演示密钥库仅用于测试！"
        echo ""
    else
        echo ""
        echo "Please set up signing configuration and run this script again."
        echo "请设置签名配置并再次运行此脚本。"
        exit 0
    fi
fi

echo "======================================"
echo "Step 1: Clean build / 第1步：清理构建"
echo "======================================"
echo ""

echo "Running: ./gradlew clean"
./gradlew clean --no-daemon 2>&1 | grep -E "(BUILD|Task|UP-TO-DATE|FAILED)" || true

echo ""
echo "======================================"
echo "Step 2: Build Debug APK / 第2步：构建 Debug APK"
echo "======================================"
echo ""

echo "Running: ./gradlew assembleDebug"
./gradlew assembleDebug --no-daemon 2>&1 | grep -E "(BUILD|Task|UP-TO-DATE|FAILED)" || true

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo ""
    echo "✅ Debug APK built successfully!"
    echo "✅ Debug APK 构建成功！"
    echo "Location / 位置: app/build/outputs/apk/debug/app-debug.apk"
    ls -lh app/build/outputs/apk/debug/app-debug.apk
else
    echo ""
    echo "⚠️  Debug APK not found / 未找到 Debug APK"
fi

echo ""
echo "======================================"
echo "Step 3: Build Signed Release APK / 第3步：构建签名 Release APK"
echo "======================================"
echo ""

echo "Running: ./gradlew assembleRelease"
./gradlew assembleRelease --no-daemon 2>&1 | grep -E "(BUILD|Task|UP-TO-DATE|FAILED)" || true

if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo ""
    echo "✅ Signed Release APK built successfully!"
    echo "✅ 签名 Release APK 构建成功！"
    echo "Location / 位置: app/build/outputs/apk/release/app-release.apk"
    ls -lh app/build/outputs/apk/release/app-release.apk
    
    echo ""
    echo "======================================"
    echo "Step 4: Verify Signature / 第4步：验证签名"
    echo "======================================"
    echo ""
    
    echo "Verifying APK signature..."
    echo "正在验证 APK 签名..."
    jarsigner -verify app/build/outputs/apk/release/app-release.apk 2>&1 | head -5
    
    echo ""
    echo "Certificate details / 证书详情:"
    keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk 2>&1 | head -10
    
elif [ -f "app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    echo ""
    echo "⚠️  Release APK is unsigned / Release APK 未签名"
    echo "Location / 位置: app/build/outputs/apk/release/app-release-unsigned.apk"
    ls -lh app/build/outputs/apk/release/app-release-unsigned.apk
else
    echo ""
    echo "❌ Release APK build failed / Release APK 构建失败"
fi

echo ""
echo "======================================"
echo "Step 5: Build Signed AAB (optional) / 第5步：构建签名 AAB（可选）"
echo "======================================"
echo ""

read -p "Do you want to build AAB? (yes/no): " BUILD_AAB

if [ "$BUILD_AAB" = "yes" ]; then
    echo ""
    echo "Running: ./gradlew bundleRelease"
    ./gradlew bundleRelease --no-daemon 2>&1 | grep -E "(BUILD|Task|UP-TO-DATE|FAILED)" || true
    
    if [ -f "app/build/outputs/bundle/release/app-release.aab" ]; then
        echo ""
        echo "✅ Signed Release AAB built successfully!"
        echo "✅ 签名 Release AAB 构建成功！"
        echo "Location / 位置: app/build/outputs/bundle/release/app-release.aab"
        ls -lh app/build/outputs/bundle/release/app-release.aab
    else
        echo ""
        echo "❌ Release AAB build failed / Release AAB 构建失败"
    fi
fi

echo ""
echo "======================================"
echo "Summary / 总结"
echo "======================================"
echo ""

echo "Build outputs / 构建输出:"
echo ""

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Debug APK: app/build/outputs/apk/debug/app-debug.apk"
fi

if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo "✅ Signed Release APK: app/build/outputs/apk/release/app-release.apk"
elif [ -f "app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    echo "⚠️  Unsigned Release APK: app/build/outputs/apk/release/app-release-unsigned.apk"
fi

if [ -f "app/build/outputs/bundle/release/app-release.aab" ]; then
    echo "✅ Signed Release AAB: app/build/outputs/bundle/release/app-release.aab"
fi

echo ""
echo "======================================"
echo "Next Steps / 下一步"
echo "======================================"
echo ""
echo "1. Install APK on device / 在设备上安装 APK:"
echo "   adb install -r app/build/outputs/apk/release/app-release.apk"
echo ""
echo "2. Test the application / 测试应用"
echo ""
echo "3. For production, generate your own keystore / 对于生产，生成您自己的密钥库:"
echo "   ./generate-keystore.sh"
echo ""
echo "4. Read more documentation / 阅读更多文档:"
echo "   - SIGNING.md - Complete signing guide / 完整签名指南"
echo "   - SIGNING_QUICKREF.md - Quick reference / 快速参考"
echo "   - BUILDING.md - Build guide / 构建指南"
echo ""
echo "======================================"
echo "Example completed! / 示例完成！"
echo "======================================"
