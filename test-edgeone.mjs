import assert from "node:assert/strict";
import { onRequest, __test__ } from "./cloud-functions/api/resolve.js";

assert.deepEqual(
  __test__.extractObject("https://www.douyin.com/note/7654811955592898930?previous_page=app_code_link"),
  { object_type: "note", object_id: "7654811955592898930" },
);
assert.equal(
  __test__.extractSecOwnerIdFromHtml('"author":{"sec_uid":"MS4wLjABAAAArdo4ql4bGt7Wfdyvr1N_qtKw5ad0coSlSGuXznCaPjE"}'),
  "MS4wLjABAAAArdo4ql4bGt7Wfdyvr1N_qtKw5ad0coSlSGuXznCaPjE",
);

const directRequest = new Request("https://example.com/api/resolve", {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({ text: "https://www.douyin.com/video/7654816343196392294" }),
});
const directResponse = await onRequest({ request: directRequest });
const directPayload = await directResponse.json();

assert.equal(directResponse.status, 200);
assert.equal(directPayload.ok, true);
assert.equal(directPayload.object_type, "video");
assert.equal(directPayload.object_id, "7654816343196392294");

const getResponse = await onRequest({
  request: new Request("https://example.com/api/resolve", { method: "GET" }),
});
const getPayload = await getResponse.json();

assert.equal(getResponse.status, 405);
assert.equal(getPayload.ok, false);

console.log("edgeone tests passed");
