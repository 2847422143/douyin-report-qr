# 抖音举报二维码生成器

这是一个小型 Web 工具：

- 粘贴抖音分享文本或短链。
- 自动解析 `v.douyin.com` 跳转。
- 支持视频作品 `/video/数字ID` 和图文作品 `/note/数字ID`。
- 生成打开抖音举报界面的二维码。
- 生成微博扫码后尽量直跳抖音作品页的二维码。

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
```

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

微博直跳二维码使用 `snssdk1128://aweme/detail/作品ID`。微博是否直接放行跳转由微博客户端决定，如果被拦截，请换系统相机或抖音扫码。
