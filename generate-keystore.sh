#!/bin/bash

# Script to generate a keystore for signing Android applications
# 生成用于签名 Android 应用的密钥库的脚本

set -e

echo "======================================"
echo "ABB Android Keystore Generator"
echo "ABB Android 密钥库生成器"
echo "======================================"
echo ""

# Default values
DEFAULT_KEYSTORE_FILE="abb-release-key.jks"
DEFAULT_KEY_ALIAS="abb-key"
DEFAULT_VALIDITY=10000

# Prompt for keystore file name
echo "Enter keystore file name (default: $DEFAULT_KEYSTORE_FILE):"
echo "输入密钥库文件名 (默认: $DEFAULT_KEYSTORE_FILE):"
read -r KEYSTORE_FILE
KEYSTORE_FILE=${KEYSTORE_FILE:-$DEFAULT_KEYSTORE_FILE}

# Check if file already exists
if [ -f "$KEYSTORE_FILE" ]; then
    echo ""
    echo "⚠️  Warning: File $KEYSTORE_FILE already exists!"
    echo "⚠️  警告：文件 $KEYSTORE_FILE 已存在！"
    echo "Do you want to overwrite it? (yes/no)"
    echo "是否要覆盖它？(yes/no)"
    read -r OVERWRITE
    if [ "$OVERWRITE" != "yes" ]; then
        echo "Aborted. / 已取消。"
        exit 1
    fi
    rm -f "$KEYSTORE_FILE"
fi

# Prompt for key alias
echo ""
echo "Enter key alias (default: $DEFAULT_KEY_ALIAS):"
echo "输入密钥别名 (默认: $DEFAULT_KEY_ALIAS):"
read -r KEY_ALIAS
KEY_ALIAS=${KEY_ALIAS:-$DEFAULT_KEY_ALIAS}

# Prompt for passwords
echo ""
echo "Enter keystore password:"
echo "输入密钥库密码:"
read -rs KEYSTORE_PASSWORD
echo ""
echo "Confirm keystore password:"
echo "确认密钥库密码:"
read -rs KEYSTORE_PASSWORD_CONFIRM

if [ "$KEYSTORE_PASSWORD" != "$KEYSTORE_PASSWORD_CONFIRM" ]; then
    echo ""
    echo "❌ Error: Passwords do not match!"
    echo "❌ 错误：密码不匹配！"
    exit 1
fi

echo ""
echo "Enter key password (press Enter to use the same as keystore password):"
echo "输入密钥密码（按 Enter 使用与密钥库密码相同的密码）:"
read -rs KEY_PASSWORD
KEY_PASSWORD=${KEY_PASSWORD:-$KEYSTORE_PASSWORD}

# Prompt for certificate information
echo ""
echo "Enter your name (CN):"
echo "输入您的名字 (CN):"
read -r CN

echo "Enter organizational unit (OU, optional):"
echo "输入组织单位 (OU，可选):"
read -r OU

echo "Enter organization (O, optional):"
echo "输入组织 (O，可选):"
read -r O

echo "Enter city (L, optional):"
echo "输入城市 (L，可选):"
read -r L

echo "Enter state/province (ST, optional):"
echo "输入州/省 (ST，可选):"
read -r ST

echo "Enter country code (C, e.g., CN, US, optional):"
echo "输入国家代码 (C，例如：CN、US，可选):"
read -r C

# Build dname string
DNAME="CN=$CN"
[ -n "$OU" ] && DNAME="$DNAME, OU=$OU"
[ -n "$O" ] && DNAME="$DNAME, O=$O"
[ -n "$L" ] && DNAME="$DNAME, L=$L"
[ -n "$ST" ] && DNAME="$DNAME, ST=$ST"
[ -n "$C" ] && DNAME="$DNAME, C=$C"

# Generate keystore
echo ""
echo "Generating keystore..."
echo "正在生成密钥库..."
echo ""

keytool -genkey -v \
    -keystore "$KEYSTORE_FILE" \
    -keyalg RSA \
    -keysize 2048 \
    -validity $DEFAULT_VALIDITY \
    -alias "$KEY_ALIAS" \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "$DNAME"

echo ""
echo "✅ Keystore generated successfully!"
echo "✅ 密钥库生成成功！"
echo ""
echo "Keystore file: $KEYSTORE_FILE"
echo "Key alias: $KEY_ALIAS"
echo ""

# Create or update keystore.properties
PROPERTIES_FILE="keystore.properties"
echo "Creating $PROPERTIES_FILE..."
echo "正在创建 $PROPERTIES_FILE..."

cat > "$PROPERTIES_FILE" << EOF
KEYSTORE_FILE=$KEYSTORE_FILE
KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD
KEY_ALIAS=$KEY_ALIAS
KEY_PASSWORD=$KEY_PASSWORD
EOF

echo ""
echo "✅ $PROPERTIES_FILE created successfully!"
echo "✅ $PROPERTIES_FILE 创建成功！"
echo ""
echo "======================================"
echo "⚠️  IMPORTANT / 重要提示"
echo "======================================"
echo ""
echo "1. Keep your keystore file and passwords secure!"
echo "   妥善保管您的密钥库文件和密码！"
echo ""
echo "2. DO NOT commit these files to version control:"
echo "   不要将这些文件提交到版本控制："
echo "   - $KEYSTORE_FILE"
echo "   - $PROPERTIES_FILE"
echo ""
echo "3. Back up your keystore file in a secure location."
echo "   在安全的位置备份您的密钥库文件。"
echo ""
echo "4. If you lose your keystore, you cannot update your app!"
echo "   如果丢失密钥库，您将无法更新您的应用！"
echo ""
echo "======================================"
echo "You can now build a signed release APK using:"
echo "您现在可以使用以下命令构建签名的发布 APK："
echo ""
echo "  ./gradlew assembleRelease"
echo ""
echo "Or build a signed release AAB using:"
echo "或使用以下命令构建签名的发布 AAB："
echo ""
echo "  ./gradlew bundleRelease"
echo ""
echo "======================================"
