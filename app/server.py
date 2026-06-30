import json
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, urlparse
from urllib.request import Request, urlopen


APP_DIR = Path(__file__).resolve().parent
HTML_PATH = APP_DIR / "douyin-report-qr.html"
URL_RE = re.compile(r"https?://[^\s，。]+")
OBJECT_RE = re.compile(r"(?:douyin\.com/(video|note)/|iesdouyin\.com/share/(video|note)/)(\d{16,22})")


def extract_first_url(text):
    match = URL_RE.search(text or "")
    if not match:
        return None
    return match.group(0).rstrip(" :;,.，。")


def extract_video_id(text):
    obj = extract_object(text)
    if obj and obj["object_type"] == "video":
        return obj["object_id"]
    return None


def extract_object(text):
    value = str(text or "").strip()
    if re.fullmatch(r"\d{16,22}", value):
        return {"object_type": "video", "object_id": value}
    match = OBJECT_RE.search(value)
    if not match:
        return None
    object_type = match.group(1) or match.group(2)
    return {"object_type": object_type, "object_id": match.group(3)}


def resolve_video_id(text, timeout=12):
    direct = extract_object(text)
    if direct:
        return {
            "object_type": direct["object_type"],
            "object_id": direct["object_id"],
            "video_id": direct["object_id"] if direct["object_type"] == "video" else None,
            "resolved_url": text,
        }

    url = extract_first_url(text)
    if not url:
        raise ValueError("没有找到链接。")

    if not urlparse(url).netloc.endswith(("douyin.com", "iesdouyin.com")):
        raise ValueError("只支持抖音链接。")

    request = Request(
        url,
        headers={
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        },
    )
    with urlopen(request, timeout=timeout) as response:
        resolved_url = response.geturl()

    resolved = extract_object(resolved_url)
    if not resolved:
        raise ValueError("短链已打开，但没有在跳转地址里找到 video 或 note ID。")

    return {
        "object_type": resolved["object_type"],
        "object_id": resolved["object_id"],
        "video_id": resolved["object_id"] if resolved["object_type"] == "video" else None,
        "resolved_url": resolved_url,
    }


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path in ("/", "/index.html"):
            self._send_file(HTML_PATH, "text/html; charset=utf-8")
            return
        if parsed.path == "/health":
            self._send_json({"ok": True})
            return
        self.send_error(404)

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path != "/api/resolve":
            self.send_error(404)
            return

        try:
            length = int(self.headers.get("Content-Length", "0"))
            payload = json.loads(self.rfile.read(length).decode("utf-8") or "{}")
            result = resolve_video_id(payload.get("text", ""))
            self._send_json({"ok": True, **result})
        except (ValueError, HTTPError, URLError, TimeoutError) as exc:
            self._send_json({"ok": False, "error": str(exc)}, status=400)
        except Exception as exc:
            self._send_json({"ok": False, "error": f"解析失败：{exc}"}, status=500)

    def log_message(self, format, *args):
        print("%s - %s" % (self.address_string(), format % args))

    def _send_file(self, path, content_type):
        data = path.read_bytes()
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _send_json(self, payload, status=200):
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def run(host="0.0.0.0", port=None):
    port = port or int(__import__("os").environ.get("PORT", "8787"))
    server = ThreadingHTTPServer((host, port), Handler)
    print(f"Douyin report QR app: http://{host}:{port}/")
    server.serve_forever()


if __name__ == "__main__":
    run()
