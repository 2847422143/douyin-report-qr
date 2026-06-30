# 抖音举报二维码生成器

这是一个可部署到 Render 的小型 Web 工具。

功能：

- 粘贴抖音分享文本或短链。
- 自动解析 `v.douyin.com` 跳转。
- 支持视频作品 `/video/数字ID` 和图文作品 `/note/数字ID`。
- 生成打开举报界面的二维码。

## 本地运行

```powershell
python app/server.py
```

打开：

```text
http://127.0.0.1:8787/
```

## 部署到 Render

1. 把本项目上传到 GitHub 仓库。
2. 打开 Render：

```text
https://dashboard.render.com/web/new
```

3. 选择 GitHub 仓库。
4. Render 会读取 `render.yaml`。
5. 如果需要手动填写：
   - Runtime: `Python`
   - Build Command: 留空
   - Start Command: `python app/server.py`
6. 创建 Web Service。

部署成功后，Render 会给一个固定链接，例如：

```text
https://douyin-report-qr.onrender.com
```

## 注意

Render 免费服务一段时间没人访问后可能会休眠，首次打开可能需要等待几十秒。

二维码只会打开抖音举报界面，不会自动提交举报。扫码后仍需要手动选择举报理由并提交。
