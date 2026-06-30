import assert from "node:assert/strict";

function buildReportDeepLink(objectId, objectType = "video", secOwnerId = "") {
  const reportMortiseId = "9879902d-907f-46f3-85e0-678c416f366a";
  const reportH5Base = `https://api.amemv.com/falcon/fe_douyin_security_react/mortise/${reportMortiseId}/`;
  if (!/^\d{16,22}$/.test(String(objectId))) throw new Error("ID must be 16 to 22 digits");
  if (!["video", "note"].includes(objectType)) throw new Error("type must be video or note");
  const reportParams = [
    ["report_type", objectType],
    ["object_id", objectId],
    ...(secOwnerId ? [["sec_owner_id", secOwnerId]] : []),
    ["enter_from", "aweme_reflow"],
    ["hide_nav_bar", "1"],
    ["should_full_screen", "1"],
  ];
  const reportH5 = `${reportH5Base}?${reportParams.map(([key, value]) => `${key}=${encodeURIComponent(value)}`).join("&")}`;
  const params = [
    ["refer", "web"], ["from", "webview"], ["from_ssr", "1"], ["from_aid", "1128"],
    ["app", "aweme"], ["scene_from", "share_reflow"], ["host", "www.iesdouyin.com"],
    ["browser_name", "safari"], ["is_edenx", "1"], ["forbid_pasteboard", "1"],
    ["gd_label", "click_schema_ug_filter_v1_click_schema_lhft_48148317a"],
    ["launch_h5_method", "click_wap_rf_video_report"], ["url", reportH5],
    ["hide_nav_bar", "1"], ["should_full_screen", "1"], ["enter_from", "aweme_reflow"],
  ];
  return `snssdk1128://webview?${params.map(([key, value]) => `${key}=${encodeURIComponent(value)}`).join("&")}`;
}

function buildWeiboReportDeepLink(objectId, objectType = "video", secOwnerId = "") {
  return buildReportDeepLink(objectId, objectType, secOwnerId);
}

function buildBridgeUrl(reportDeepLink, baseUrl = "https://douyin-report-qr.edgeone.dev/") {
  const url = new URL(baseUrl);
  url.searchParams.set("open_report", reportDeepLink);
  return url.toString();
}

function buildQrPayload(objectId, objectType, mode, secOwnerId = "") {
  if (mode === "weibo-jump") return buildWeiboReportDeepLink(objectId, objectType, secOwnerId);
  if (mode === "weibo-bridge") return buildBridgeUrl(buildReportDeepLink(objectId, objectType, secOwnerId));
  return buildReportDeepLink(objectId, objectType, secOwnerId);
}

assert.match(
  buildReportDeepLink("7654811955592898930", "note"),
  /report_type%3Dnote%26object_id%3D7654811955592898930/,
);
assert.match(
  buildReportDeepLink("7639583721263374026", "video", "MS4wLjABAAAArdo4ql4bGt7Wfdyvr1N_qtKw5ad0coSlSGuXznCaPjE"),
  /sec_owner_id%3DMS4wLjABAAAArdo4ql4bGt7Wfdyvr1N_qtKw5ad0coSlSGuXznCaPjE/,
);
const weiboReportLink = buildWeiboReportDeepLink("7654816343196392294", "video");
assert.match(weiboReportLink, /^snssdk1128:\/\/webview\?/);
assert.match(weiboReportLink, /report_type%3Dvideo%26object_id%3D7654816343196392294/);
assert.doesNotMatch(weiboReportLink, /aweme\/detail/);
const bridgeUrl = buildBridgeUrl(buildReportDeepLink("7639583721263374026", "video", "MS4wLjABAAAArdo4ql4bGt7Wfdyvr1N_qtKw5ad0coSlSGuXznCaPjE"));
assert.match(bridgeUrl, /^https:\/\/douyin-report-qr\.edgeone\.dev\/\?open_report=/);
assert.equal(new URL(bridgeUrl).searchParams.get("open_report"), buildReportDeepLink("7639583721263374026", "video", "MS4wLjABAAAArdo4ql4bGt7Wfdyvr1N_qtKw5ad0coSlSGuXznCaPjE"));
const bridgePayload = buildQrPayload("7639583721263374026", "video", "weibo-bridge", "MS4wLjABAAAArdo4ql4bGt7Wfdyvr1N_qtKw5ad0coSlSGuXznCaPjE");
assert.match(bridgePayload, /^https:\/\/douyin-report-qr\.edgeone\.dev\/\?open_report=/);
assert.equal(new URL(bridgePayload).searchParams.get("open_report"), buildReportDeepLink("7639583721263374026", "video", "MS4wLjABAAAArdo4ql4bGt7Wfdyvr1N_qtKw5ad0coSlSGuXznCaPjE"));

console.log("deeplink core tests passed");
