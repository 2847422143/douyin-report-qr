import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

for (const file of ["package.json", "vercel.json"]) {
  const bytes = readFileSync(file);
  assert.notDeepEqual(
    [...bytes.slice(0, 3)],
    [0xef, 0xbb, 0xbf],
    `${file} must be UTF-8 without BOM`,
  );
  JSON.parse(readFileSync(file, "utf8"));
}

console.log("json encoding tests passed");
