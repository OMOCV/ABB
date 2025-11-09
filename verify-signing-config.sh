#!/bin/bash

# Script to verify signing configuration before building signed APK/AAB
# 在构建已签名的 APK/AAB 之前验证签名配置的脚本

set -e

echo "======================================"
echo "ABB Signing Configuration Verifier"
echo "ABB 签名配置验证器"
echo "======================================"
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Exit code
EXIT_CODE=0

# Function to print success message
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print error message
print_error() {
    echo -e "${RED}✗ $1${NC}"
    EXIT_CODE=1
}

# Function to print warning message
print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

echo "=== Step 1: Checking Signing Configuration ==="
echo "=== 步骤 1: 检查签名配置 ==="
echo ""

# Check for keystore.properties file
KEYSTORE_PROPS_FILE="keystore.properties"
DEMO_KEYSTORE_PROPS_FILE="demo-keystore.properties"

if [ -f "$KEYSTORE_PROPS_FILE" ]; then
    print_success "Found keystore.properties file / 找到 keystore.properties 文件"
    PROPS_FILE="$KEYSTORE_PROPS_FILE"
elif [ -f "$DEMO_KEYSTORE_PROPS_FILE" ]; then
    print_warning "Using demo-keystore.properties (NOT for production!) / 使用 demo-keystore.properties（不适用于生产环境！）"
    PROPS_FILE="$DEMO_KEYSTORE_PROPS_FILE"
else
    print_error "No keystore properties file found / 未找到密钥库属性文件"
    echo "  Please create keystore.properties or use demo-keystore.properties"
    echo "  请创建 keystore.properties 或使用 demo-keystore.properties"
    exit 1
fi

# Check for environment variables (used in CI/CD)
if [ -n "$KEYSTORE_FILE" ] && [ -n "$KEYSTORE_PASSWORD" ] && [ -n "$KEY_ALIAS" ] && [ -n "$KEY_PASSWORD" ]; then
    print_success "Found signing configuration in environment variables / 在环境变量中找到签名配置"
    USE_ENV_VARS=true
else
    USE_ENV_VARS=false
fi

echo ""
echo "=== Step 2: Validating Signing Parameters ==="
echo "=== 步骤 2: 验证签名参数 ==="
echo ""

# Read properties file
if [ "$USE_ENV_VARS" = false ]; then
    # Source the properties file
    source "$PROPS_FILE"
    
    # Validate required parameters
    REQUIRED_PARAMS=("KEYSTORE_FILE" "KEYSTORE_PASSWORD" "KEY_ALIAS" "KEY_PASSWORD")
    for param in "${REQUIRED_PARAMS[@]}"; do
        if [ -z "${!param}" ]; then
            print_error "Missing required parameter: $param / 缺少必需参数: $param"
        else
            print_success "Parameter $param is set / 参数 $param 已设置"
        fi
    done
fi

echo ""
echo "=== Step 3: Checking Keystore File ==="
echo "=== 步骤 3: 检查密钥库文件 ==="
echo ""

if [ "$USE_ENV_VARS" = false ]; then
    if [ -f "$KEYSTORE_FILE" ]; then
        print_success "Keystore file exists: $KEYSTORE_FILE / 密钥库文件存在: $KEYSTORE_FILE"
        
        # Check if keytool is available
        if command -v keytool &> /dev/null; then
            # Verify keystore (this will fail if password is wrong)
            if keytool -list -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD" -alias "$KEY_ALIAS" &> /dev/null; then
                print_success "Keystore is valid and accessible / 密钥库有效且可访问"
            else
                print_error "Cannot access keystore with provided credentials / 无法使用提供的凭据访问密钥库"
                echo "  Please check your passwords in $PROPS_FILE"
                echo "  请检查 $PROPS_FILE 中的密码"
            fi
        else
            print_warning "keytool not found, skipping keystore validation / 未找到 keytool，跳过密钥库验证"
        fi
    else
        print_error "Keystore file not found: $KEYSTORE_FILE / 未找到密钥库文件: $KEYSTORE_FILE"
        echo "  Please generate a keystore using: ./generate-keystore.sh"
        echo "  请使用以下命令生成密钥库: ./generate-keystore.sh"
    fi
else
    print_success "Using keystore from environment (CI/CD mode) / 使用环境中的密钥库（CI/CD 模式）"
fi

echo ""
echo "=== Step 4: Checking Build Configuration ==="
echo "=== 步骤 4: 检查构建配置 ==="
echo ""

# Check if build.gradle.kts exists and contains signing configuration
BUILD_GRADLE="app/build.gradle.kts"
if [ -f "$BUILD_GRADLE" ]; then
    print_success "Found build.gradle.kts / 找到 build.gradle.kts"
    
    # Check for signingConfigs block
    if grep -q "signingConfigs" "$BUILD_GRADLE"; then
        print_success "Build file contains signingConfigs / 构建文件包含 signingConfigs"
    else
        print_error "Build file missing signingConfigs / 构建文件缺少 signingConfigs"
    fi
    
    # Check for release signing config assignment
    if grep -q "signingConfig = signingConfigs" "$BUILD_GRADLE"; then
        print_success "Release build type uses signing config / Release 构建类型使用签名配置"
    else
        print_warning "Release build type may not use signing config / Release 构建类型可能不使用签名配置"
    fi
else
    print_error "Build file not found: $BUILD_GRADLE / 未找到构建文件: $BUILD_GRADLE"
fi

echo ""
echo "=== Step 5: Checking Workflow Files ==="
echo "=== 步骤 5: 检查工作流文件 ==="
echo ""

# Check workflow files
WORKFLOW_FILES=(".github/workflows/build-apk.yml" ".github/workflows/release.yml")
for workflow in "${WORKFLOW_FILES[@]}"; do
    if [ -f "$workflow" ]; then
        print_success "Found workflow: $workflow / 找到工作流: $workflow"
        
        # Check if workflow has keystore decode step
        if grep -q "Decode Keystore" "$workflow"; then
            print_success "Workflow has keystore decode step / 工作流包含密钥库解码步骤"
        else
            print_warning "Workflow missing keystore decode step / 工作流缺少密钥库解码步骤"
        fi
        
        # Check for signing secrets
        if grep -q "KEYSTORE_BASE64" "$workflow"; then
            print_success "Workflow references signing secrets / 工作流引用签名密钥"
        else
            print_warning "Workflow may not use signing secrets / 工作流可能不使用签名密钥"
        fi
    else
        print_warning "Workflow not found: $workflow / 未找到工作流: $workflow"
    fi
done

echo ""
echo "=== Step 6: Checking Gradle Wrapper ==="
echo "=== 步骤 6: 检查 Gradle Wrapper ==="
echo ""

if [ -f "gradlew" ]; then
    print_success "Gradle wrapper found / 找到 Gradle wrapper"
    
    if [ -x "gradlew" ]; then
        print_success "Gradle wrapper is executable / Gradle wrapper 可执行"
    else
        print_warning "Gradle wrapper is not executable / Gradle wrapper 不可执行"
        echo "  Run: chmod +x gradlew"
        echo "  运行: chmod +x gradlew"
    fi
else
    print_error "Gradle wrapper not found / 未找到 Gradle wrapper"
fi

echo ""
echo "======================================"
echo "=== Verification Summary ==="
echo "=== 验证摘要 ==="
echo "======================================"
echo ""

if [ $EXIT_CODE -eq 0 ]; then
    print_success "All checks passed! / 所有检查通过！"
    echo ""
    echo "You can now build signed APK/AAB with:"
    echo "现在可以使用以下命令构建已签名的 APK/AAB:"
    echo ""
    echo "  ./gradlew assembleRelease    # Build signed APK"
    echo "  ./gradlew bundleRelease      # Build signed AAB"
    echo ""
else
    print_error "Some checks failed! / 某些检查失败！"
    echo ""
    echo "Please fix the issues above before building signed releases."
    echo "请在构建已签名的发布版本之前修复上述问题。"
    echo ""
    echo "For help, see:"
    echo "获取帮助，请参阅:"
    echo "  - SIGNING.md"
    echo "  - BUILDING.md"
    echo ""
fi

exit $EXIT_CODE
