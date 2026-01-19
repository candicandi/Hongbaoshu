# Release 说明模板

> 💡 每次发布新版本时,复制此模板到 GitHub Release 页面

---

## 版本 vX.X.X

**发布日期**: YYYY-MM-DD

### ✨ 新功能

- 功能描述 1
- 功能描述 2

### 🔧 改进优化

- 改进描述 1
- 改进描述 2

### 🐛 问题修复

- 修复描述 1
- 修复描述 2

---

## 📥 下载安装

### 下载文件

- **APK 文件**: [app-release.apk](链接)
- **文件大小**: XX MB
- **最低系统要求**: Android 9.0 (API 28)

### 安装说明

如果您在安装时遇到安全警告,**这是正常现象**。请查看详细的 [安装指南](https://github.com/ceasarXuu/Hongbaoshu/blob/main/docs/安装指南.md) 了解如何处理。

**快速说明**:
1. 下载 APK 文件
2. 允许安装未知来源应用
3. 如遇安全软件警告,选择"继续安装"或"信任此应用"

---

## 🔒 安全信息

### 应用签名指纹

```
SHA-256: 86:37:19:42:70:E3:80:14:48:1B:3E:0B:BF:42:EA:D8:81:88:AB:EE:55:5F:19:A8:F8:F3:70:27:2D:58:70:71
```

### VirusTotal 扫描结果

- **扫描链接**: [VirusTotal 扫描结果](扫描链接)
- **检测率**: 0/XX (理想情况)
- **扫描时间**: YYYY-MM-DD HH:MM

### 为什么会有安全警告?

本应用通过 GitHub 直接分发,未经过 Google Play 等官方应用商店的验证,因此部分安全软件可能会发出警告。**这是正常现象**。

**安全保证**:
- ✅ **开源透明** - 所有代码公开可审计
- ✅ **无数据收集** - 不收集任何用户信息
- ✅ **无网络请求** - 应用不需要网络权限
- ✅ **无广告** - 完全免费,无任何广告
- ✅ **本地存储** - 所有数据仅存储在本地设备

### 验证应用完整性

您可以使用以下命令验证下载的 APK 是否为官方发布:

```bash
# 查看签名指纹
keytool -printcert -jarfile app-release.apk

# 或使用 apksigner
apksigner verify --print-certs app-release.apk
```

将输出的 SHA-256 指纹与上方公布的官方指纹对比,一致即为官方发布。

---

## 📝 更新说明

### 从旧版本升级

- **如果使用相同签名**: 直接安装即可覆盖旧版本,数据会保留
- **如果签名不同**: 需要先卸载旧版本,再安装新版本(数据会丢失)

### 已知问题

- 问题描述 1 (如有)
- 问题描述 2 (如有)

---

## 🙏 致谢

感谢所有用户的支持和反馈!

如果您在使用过程中遇到问题,欢迎:
- 提交 [Issue](https://github.com/ceasarXuu/Hongbaoshu/issues)
- 查看 [文档](https://github.com/ceasarXuu/Hongbaoshu/tree/main/docs)
- 参与 [讨论](https://github.com/ceasarXuu/Hongbaoshu/discussions)

---

## ⚖️ 免责声明

本项目仅供技术研究与学习交流使用。项目中的所有内容版权归原作者所有,请在下载后 24 小时内删除。详见 [README](https://github.com/ceasarXuu/Hongbaoshu/blob/main/README.md) 中的免责声明。
