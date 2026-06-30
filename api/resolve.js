const URL_RE = /https?:\/\/[^\s，。]+/;
const OBJECT_RE = /(?:douyin\.com\/(video|note)\/|iesdouyin\.com\/share\/(video|note)\/)(\d{16,22})/;

function extractFirstUrl(text) {
  const match = URL_RE.exec(text || "");
  return match ? match[0].replace(/[ :;,.，。]+$/, "") : null;
}

function extractObject(text) {
  const value = String(text || "").trim();
  if (/^\d{16,22}$/.test(value)) return { object_type: "video", object_id: value };
  const match = OBJECT_RE.exec(value);
  if (!match) return null;
  return { object_type: match[1] || match[2], object_id: match[3] };
}

function extractSecOwnerIdFromHtml(html) {
  const match = /"sec_uid"\s*:\s*"([^"]+)"/.exec(String(html || ""));
  return match ? match[1] : null;
}

async function fetchShareMetadata(url) {
  const response = await fetch(url, {
    redirect: "follow",
    headers: {
      "user-agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
      "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    },
  });
  const html = await response.text();
  return {
    resolved_url: response.url,
    object: extractObject(response.url) || extractObject(html),
    sec_owner_id: extractSecOwnerIdFromHtml(html),
  };
}

async function resolveObject(text) {
  const direct = extractObject(text);
  const firstUrl = extractFirstUrl(text);
  if (direct && !firstUrl) {
    return {
      ...direct,
      video_id: direct.object_type === "video" ? direct.object_id : null,
      resolved_url: text,
      sec_owner_id: null,
    };
  }

  const shortUrl = firstUrl;
  if (!shortUrl) throw new Error("没有找到链接。");
  const host = new URL(shortUrl).hostname;
  if (!host.endsWith("douyin.com") && !host.endsWith("iesdouyin.com")) {
    throw new Error("只支持抖音链接。");
  }

  const metadata = await fetchShareMetadata(shortUrl);
  const resolved = direct || metadata.object;
  if (!resolved) throw new Error("短链已打开，但没有在跳转地址里找到 video 或 note ID。");
  return {
    ...resolved,
    video_id: resolved.object_type === "video" ? resolved.object_id : null,
    resolved_url: metadata.resolved_url,
    sec_owner_id: metadata.sec_owner_id,
  };
}

export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.status(405).json({ ok: false, error: "Method not allowed" });
    return;
  }

  try {
    const result = await resolveObject(req.body?.text || "");
    res.status(200).json({ ok: true, ...result });
  } catch (error) {
    res.status(400).json({ ok: false, error: error.message || String(error) });
  }
}

export const __test__ = { extractFirstUrl, extractObject, extractSecOwnerIdFromHtml };
