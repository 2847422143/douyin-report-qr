const URL_RE = /https?:\/\/[^\s，。]+/;
const OBJECT_RE = /(?:douyin\.com\/(video|note)\/|iesdouyin\.com\/share\/(video|note)\/)(\d{16,22})/;

function json(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
    },
  });
}

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

async function resolveObject(text) {
  const direct = extractObject(text);
  if (direct) {
    return {
      ...direct,
      video_id: direct.object_type === "video" ? direct.object_id : null,
      resolved_url: text,
    };
  }

  const shortUrl = extractFirstUrl(text);
  if (!shortUrl) throw new Error("没有找到链接。");

  const host = new URL(shortUrl).hostname;
  if (!host.endsWith("douyin.com") && !host.endsWith("iesdouyin.com")) {
    throw new Error("只支持抖音链接。");
  }

  const response = await fetch(shortUrl, {
    redirect: "follow",
    headers: {
      "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
      "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    },
  });
  const resolvedUrl = response.url;
  const resolved = extractObject(resolvedUrl);
  if (!resolved) throw new Error("短链已打开，但没有在跳转地址里找到 video 或 note ID。");

  return {
    ...resolved,
    video_id: resolved.object_type === "video" ? resolved.object_id : null,
    resolved_url: resolvedUrl,
  };
}

async function onRequest(context) {
  const { request } = context;

  if (request.method !== "POST") {
    return json({ ok: false, error: "Method not allowed" }, 405);
  }

  try {
    const body = await request.json();
    const result = await resolveObject(body?.text || "");
    return json({ ok: true, ...result });
  } catch (error) {
    return json({ ok: false, error: error.message || String(error) }, 400);
  }
}

export default onRequest;
export { onRequest };
export const __test__ = { extractFirstUrl, extractObject, resolveObject };
