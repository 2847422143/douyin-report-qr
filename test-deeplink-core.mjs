import assert from "node:assert/strict";

function buildReportDeepLink(objectId, objectType = "video") {
  const reportMortiseId = "9879902d-907f-46f3-85e0-678c416f366a";
  const reportH5Base = `https://api.amemv.com/falcon/fe_douyin_security_react/mortise/${reportMortiseId}/`;
  if (!/^\d{16,22}$/.test(String(objectId))) throw new Error("ID must be 16 to 22 digits");
  if (!["video", "note"].includes(objectType)) throw new Error("type must be video or note");
  const reportH5 = `${reportH5Base}?report_type=${objectType}&object_id=${objectId}&enter_from=aweme_reflow&hide_nav_bar=1&should_full_screen=1`;
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

function buildWeiboJumpDeepLink(objectId) {
  if (!/^\d{16,22}$/.test(String(objectId))) throw new Error("ID must be 16 to 22 digits");
  return `snssdk1128://aweme/detail/${objectId}`;
}

assert.equal(
  buildWeiboJumpDeepLink("7654816343196392294"),
  "snssdk1128://aweme/detail/7654816343196392294",
);
assert.match(
  buildReportDeepLink("7654811955592898930", "note"),
  /report_type%3Dnote%26object_id%3D7654811955592898930/,
);

console.log("deeplink core tests passed");
