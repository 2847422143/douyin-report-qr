# 抖音举报二维码生成器

这是一个可部署到 Vercel 的小型 Web 工具。

功能：

- 粘贴抖音分享文本或短链。
- 自动解析 `v.douyin.com` 跳转。
- 支持视频作品 `/video/数字ID` 和图文作品 `/note/数字ID`。
- 生成打开举报界面的二维码。

## Vercel 部署

1. 把本项目推送到 GitHub。
2. 打开：

```text
https://vercel.com/new
```

3. 选择 GitHub 仓库 `2847422143/douyin-report-qr`。
4. Framework Preset 选择 `Other`。
5. 直接 Deploy。

部署成功后，Vercel 会给一个固定链接，例如：

```text
https://douyin-report-qr.vercel.app
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

二维码只会打开抖音举报界面，不会自动提交举报。扫码后仍需要手动选择举报理由并提交。
