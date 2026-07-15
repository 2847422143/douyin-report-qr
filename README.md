# 抖音举报二维码生成器

这是一个小型 Web 工具：

- 粘贴抖音分享文本或短链。
- 自动解析 `v.douyin.com` 跳转。
- 支持视频作品 `/video/数字ID` 和图文作品 `/note/数字ID`。
- 生成打开抖音举报界面的二维码。
- 生成微博扫码后尽量直跳抖音举报界面的二维码。

## 腾讯 EdgeOne Pages 部署

1. 打开：

```text
https://pages.edgeone.ai/
```

2. 登录后选择新建项目，选择“导入 Git 仓库”。
3. 选择 GitHub 仓库：

```text
2847422143/douyin-report-qr
```

4. 构建配置建议如下：

```text
Framework Preset: Other
Build Command: 留空
Output Directory: .
Root Directory: /
```

如果页面要求“安装命令”，也留空。

这个项目是静态首页加 Edge Functions，不需要构建步骤。

5. 点击部署。部署完成后，EdgeOne Pages 会给一个公网 HTTPS 地址。

## 项目结构

```text
index.html                         EdgeOne Pages 首页
edge-functions/api/resolve.js       EdgeOne Pages 短链解析接口
cloud-functions/api/resolve.js      Cloud Functions 兼容接口
public/index.html                   Vercel 兼容首页
api/resolve.js                      Vercel 兼容接口
app/server.py                       本地 Python 服务器
android/douyin-report-android       Android 用户端 APK 源码
android/douyin-report-admin-android Android 管理端 APK 源码
release/                            最新 APK、源码包、Supabase 部署说明
```

## Android 双 APK 版本

仓库已包含 Android 原生 Java 版本：

- 用户端包名：`com.example.douyinreportqr`
- 管理端包名：`com.example.douyinreportadmin`
- 用户端 APK：`release/douyin-report-qr-android-debug.apk`
- 管理端 APK：`release/douyin-report-admin-android-debug.apk`
- 完整复刻说明：`release/douyin-report-apk-full-rebuild-guide.md`
- 完整源码压缩包：`release/douyin-report-apk-full-project-source.zip`

用户端功能：

- 输入抖音分享文本或链接。
- 自动识别视频作品和图文作品。
- 生成打开抖音举报界面的二维码。
- 支持复制、保存二维码、尝试打开抖音。
- 接入 Supabase 远程开关和设备登记。

管理端功能：

- 查看已经登记的设备。
- 全局开启/暂停生成。
- 单独控制某台设备。
- 一键全部暂停、一键全部开启、清空单独配置。
- 删除设备记录。
- 支持超过 10 台设备后，新设备默认禁用。

Android 构建示例：

```powershell
$env:JAVA_HOME='D:\JDK17'
$env:Path='D:\JDK17\bin;' + $env:Path
.\work\gradle-8.11.1\bin\gradle.bat -p .\android\douyin-report-android assembleDebug
.\work\gradle-8.11.1\bin\gradle.bat -p .\android\douyin-report-admin-android assembleDebug
```

如果没有仓库外部的 `work\gradle-8.11.1`，也可以用本机已安装的 Gradle/Android Studio 打开 `android/` 下两个项目分别构建。

## 本地测试

```powershell
npm test
```

## 本地运行 Python 版本

```powershell
python app/server.py
```

打开：

```text
http://127.0.0.1:8787/
```

## 注意

举报二维码只会打开抖音举报界面，不会自动提交举报。扫码后仍需要手动选择举报理由并提交。

微博直跳举报页二维码使用抖音举报 WebView 深链。微博是否直接放行跳转由微博客户端决定，如果被拦截，请换系统相机或抖音扫码。
