import assert from "node:assert/strict";
import { __test__ } from "./api/resolve.js";

assert.equal(
  __test__.extractFirstUrl("复制打开抖音 https://v.douyin.com/M4Hy4uVUAKc/ :2pm"),
  "https://v.douyin.com/M4Hy4uVUAKc/",
);
assert.deepEqual(
  __test__.extractObject("https://www.douyin.com/video/7654816343196392294?previous_page=app_code_link"),
  { object_type: "video", object_id: "7654816343196392294" },
);
assert.deepEqual(
  __test__.extractObject("https://www.douyin.com/note/7654811955592898930?previous_page=app_code_link"),
  { object_type: "note", object_id: "7654811955592898930" },
);
console.log("vercel tests passed");
