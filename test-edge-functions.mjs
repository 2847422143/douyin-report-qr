import assert from "node:assert/strict";
import edgeOnRequest, { __test__ } from "./edge-functions/api/resolve.js";

assert.deepEqual(
  __test__.extractObject("https://www.douyin.com/note/7654811955592898930?previous_page=app_code_link"),
  { object_type: "note", object_id: "7654811955592898930" },
);

const response = await edgeOnRequest({
  request: new Request("https://example.com/api/resolve", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ text: "https://www.douyin.com/video/7654816343196392294" }),
  }),
});
const payload = await response.json();

assert.equal(response.status, 200);
assert.equal(payload.ok, true);
assert.equal(payload.object_type, "video");
assert.equal(payload.object_id, "7654816343196392294");

console.log("edge functions tests passed");
