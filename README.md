# 抖音举报二维码 Android 双 APK

这是一个 Android 原生 Java 双 APK 项目：

- **用户端 APK**：输入抖音分享文本或链接，生成打开抖音举报界面的二维码。
- **管理端 APK**：查看已安装设备，远程控制全局开关或单台设备开关。

旧的 HTML / EdgeOne / Vercel / Python 网页方案已经移除，本仓库只保留 Android APK 方案。

## APK 下载

最新 debug APK 位于：

```text
release/douyin-report-qr-android-debug.apk
release/douyin-report-admin-android-debug.apk
```

完整源码压缩包：

```text
release/douyin-report-apk-full-project-source.zip
```

完整复刻说明：

```text
release/douyin-report-apk-full-rebuild-guide.md
```

Supabase 云端说明：

```text
release/supabase-device-control-usage.md
```

## 项目结构

```text
android/
  douyin-report-android/             用户端 Android 项目
  douyin-report-admin-android/       管理端 Android 项目

release/
  douyin-report-qr-android-debug.apk
  douyin-report-admin-android-debug.apk
  douyin-report-apk-full-project-source.zip
  douyin-report-apk-full-rebuild-guide.md
  supabase-device-control-usage.md
```

## 用户端

包名：

```text
com.example.douyinreportqr
```

功能：

- 输入抖音分享文本或链接。
- 自动识别视频作品和图文作品。
- 生成打开抖音举报界面的二维码。
- 支持复制 deeplink。
- 支持保存二维码图片。
- 支持尝试直接打开抖音举报页。
- 自动登记设备到 Supabase。
- 每 5 秒读取远程配置。
- 如果云端禁用当前设备，点击生成时显示提示语，不生成二维码。

源码：

```text
android/douyin-report-android
```

## 管理端

包名：

```text
com.example.douyinreportadmin
```

功能：

- 查看已登记设备。
- 全局开启或暂停生成。
- 设置全局提示语。
- 单独控制某台设备。
- 单独设置某台设备提示语。
- 一键全部暂停。
- 一键全部开启。
- 清空所有单独配置。
- 删除设备记录。
- 超过 10 台设备后，新设备默认禁用。

源码：

```text
android/douyin-report-admin-android
```

## APK 使用的 Supabase

当前 Android 用户端和管理端 APK 都连接同一个 Supabase 项目：

```text
Project URL: https://wnaqpyoxpgzvcfupoloe.supabase.co
REST API: https://wnaqpyoxpgzvcfupoloe.supabase.co/rest/v1
Publishable key: sb_publishable_QqLauvy5EGcEPCkEnYqL0Q_gL2YZAZ5
```

使用的表：

- `devices`：登记已安装并打开过用户端的设备。
- `device_configs`：保存单台设备的单独开关和提示语。
- `global_config`：保存全局开关和全局提示语。

使用的云端函数和触发器：

- `admin_delete_device(target_device_id text, admin_secret text)`：管理端删除设备记录。
- `trg_limit_new_devices`：新设备超过 10 台后，自动写入禁用配置。
- 限制提示语：`设备数量受限，暂不能使用`。

完整 SQL 在：

```text
release/supabase-device-control-usage.md
release/douyin-report-apk-full-rebuild-guide.md
```

## 构建方式

要求：

- JDK 17
- Android SDK
- Gradle 或 Android Studio
- compileSdk 36
- minSdk 23
- targetSdk 36

使用 Gradle 构建示例：

```powershell
gradle -p .\android\douyin-report-android assembleDebug
gradle -p .\android\douyin-report-admin-android assembleDebug
```

如果使用 Android Studio，分别打开：

```text
android/douyin-report-android
android/douyin-report-admin-android
```

然后执行 `Build APK(s)`。

## 重要说明

- APK 只能安装在 Android 手机上，iPhone 不能安装 `.apk`。
- 举报二维码只会打开抖音举报界面，不会自动提交举报。
- 扫码后仍需要用户手动选择举报理由并提交。
- 抖音 deeplink 和举报 H5 地址属于外部平台行为，抖音未来改规则后可能需要更新。
- Supabase publishable key 写在 APK 内，用于前端访问 Supabase REST。
- 管理端删除设备使用 RPC 口令，适合当前轻量项目；如果商业化，建议改成登录鉴权 + Edge Function。
