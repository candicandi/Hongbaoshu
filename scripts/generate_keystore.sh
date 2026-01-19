#!/bin/bash

# ============================================================================
# Hongbaoshu 发布密钥库生成脚本
# ============================================================================

set -e

echo "🔐 Hongbaoshu 发布密钥库生成工具"
echo "================================"
echo ""

# 配置
KEYSTORE_NAME="hongbaoshu-release.jks"
KEY_ALIAS="hongbaoshu-release"
KEY_ALGORITHM="RSA"
KEY_SIZE="2048"
VALIDITY_DAYS="10000"  # 约27年

# 检查是否已存在密钥库
if [ -f "app/$KEYSTORE_NAME" ]; then
    echo "⚠️  警告: 密钥库文件已存在: app/$KEYSTORE_NAME"
    echo ""
    read -p "是否要覆盖现有密钥库? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        echo "❌ 操作已取消"
        exit 0
    fi
    echo ""
fi

echo "📝 请输入密钥库信息:"
echo ""

# 获取密钥库密码
while true; do
    read -s -p "密钥库密码 (至少8位,包含字母和数字): " STORE_PASSWORD
    echo ""
    read -s -p "确认密钥库密码: " STORE_PASSWORD_CONFIRM
    echo ""
    
    if [ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]; then
        echo "❌ 两次输入的密码不一致,请重新输入"
        echo ""
        continue
    fi
    
    if [ ${#STORE_PASSWORD} -lt 8 ]; then
        echo "❌ 密码长度至少为8位,请重新输入"
        echo ""
        continue
    fi
    
    break
done

# 获取密钥密码
echo ""
read -p "密钥密码是否与密钥库密码相同? (yes/no): " same_password
if [ "$same_password" = "yes" ]; then
    KEY_PASSWORD="$STORE_PASSWORD"
else
    while true; do
        read -s -p "密钥密码: " KEY_PASSWORD
        echo ""
        read -s -p "确认密钥密码: " KEY_PASSWORD_CONFIRM
        echo ""
        
        if [ "$KEY_PASSWORD" != "$KEY_PASSWORD_CONFIRM" ]; then
            echo "❌ 两次输入的密码不一致,请重新输入"
            echo ""
            continue
        fi
        
        break
    done
fi

# 获取证书信息
echo ""
echo "📋 证书信息 (可以使用默认值):"
read -p "姓名 [Hongbaoshu]: " CN
CN=${CN:-Hongbaoshu}

read -p "组织单位 [XuyuTech]: " OU
OU=${OU:-XuyuTech}

read -p "组织 [XuyuTech]: " O
O=${O:-XuyuTech}

read -p "城市 [Beijing]: " L
L=${L:-Beijing}

read -p "省份 [Beijing]: " ST
ST=${ST:-Beijing}

read -p "国家代码 [CN]: " C
C=${C:-CN}

DNAME="CN=$CN, OU=$OU, O=$O, L=$L, ST=$ST, C=$C"

echo ""
echo "🔨 正在生成密钥库..."
echo ""

# 生成密钥库
keytool -genkeypair -v \
    -keystore "app/$KEYSTORE_NAME" \
    -alias "$KEY_ALIAS" \
    -keyalg "$KEY_ALGORITHM" \
    -keysize "$KEY_SIZE" \
    -validity "$VALIDITY_DAYS" \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "$DNAME"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 密钥库生成成功!"
    echo ""
    
    # 更新 keystore.properties
    echo "📝 更新 keystore.properties..."
    cat > keystore.properties << EOF
storePassword=$STORE_PASSWORD
keyPassword=$KEY_PASSWORD
keyAlias=$KEY_ALIAS
storeFile=$KEYSTORE_NAME
EOF
    
    echo "✅ keystore.properties 已更新"
    echo ""
    
    # 显示密钥库信息
    echo "📋 密钥库信息:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    keytool -list -v -keystore "app/$KEYSTORE_NAME" -storepass "$STORE_PASSWORD" | grep -A 5 "证书指纹"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    # 安全提示
    echo "⚠️  重要提示:"
    echo "1. 请妥善保管密钥库文件和密码,丢失后无法恢复"
    echo "2. 不要将密钥库文件提交到 Git 仓库"
    echo "3. 建议将密钥库文件备份到安全的地方"
    echo "4. 使用新密钥库签名的应用无法覆盖安装旧版本"
    echo ""
    
    # 提示备份
    read -p "是否要将密钥库信息保存到安全位置? (yes/no): " backup
    if [ "$backup" = "yes" ]; then
        BACKUP_FILE="hongbaoshu-keystore-info-$(date +%Y%m%d).txt"
        cat > "$BACKUP_FILE" << EOF
Hongbaoshu 密钥库信息
生成时间: $(date)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

密钥库文件: app/$KEYSTORE_NAME
密钥别名: $KEY_ALIAS
证书信息: $DNAME

⚠️ 密码信息已省略,请单独记录保管

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
证书指纹:
$(keytool -list -v -keystore "app/$KEYSTORE_NAME" -storepass "$STORE_PASSWORD" | grep -A 5 "证书指纹")
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
EOF
        echo "✅ 密钥库信息已保存到: $BACKUP_FILE"
        echo "   (密码未包含在文件中,请单独记录)"
    fi
    
    echo ""
    echo "🎉 完成! 现在可以使用新密钥库构建 release 版本了"
    echo "   运行: ./gradlew assembleRelease"
    
else
    echo ""
    echo "❌ 密钥库生成失败"
    exit 1
fi
