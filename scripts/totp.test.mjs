import { test } from "node:test";
import assert from "node:assert/strict";
import { decodeBase32, hotp, totp } from "./totp.mjs";

// RFC 6238 Appendix B secret: ASCII "12345678901234567890" (20 bytes).
// HOTP keys the byte string directly, so the tests feed its base32 encoding
// (same bytes round-trip through decodeBase32).
const RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

test("RFC 6238 test secret decodes back to the ASCII key bytes", () => {
  assert.equal(decodeBase32(RFC_SECRET).toString(), "12345678901234567890");
});

test("decodeBase32 decodes RFC 4648 'foobar' vector", () => {
  const buf = decodeBase32("MZXW6YTBOI");
  assert.deepEqual([...buf], [...Buffer.from("foobar")]);
});

test("decodeBase32 tolerates lowercase and padding", () => {
  const buf = decodeBase32("mzxw6ytboi====");
  assert.deepEqual([...buf], [...Buffer.from("foobar")]);
});

test("HOTP matches RFC 4226 draft vectors (6 digits)", () => {
  const expected = ["755224", "287082", "359152", "969429", "338314", "254676"];
  for (let counter = 0; counter < expected.length; counter++) {
    assert.equal(hotp(RFC_SECRET, counter), expected[counter]);
  }
});

test("TOTP matches RFC 6238 SHA1 vectors (8 digits, period=30)", () => {
  // [timestamp(sec), totp]
  const vectors = [
    [59, "94287082"],
    [1111111109, "07081804"],
    [1111111111, "14050471"],
    [1234567890, "89005924"],
    [2000000000, "69279037"],
    [20000000000, "65353130"],
  ];
  for (const [sec, expected] of vectors) {
    assert.equal(
      totp(RFC_SECRET, { timestamp: sec * 1000, digits: 8 }),
      expected,
      `T=${sec}`
    );
  }
});

test("TOTP defaults to 6 digits and 30s period", () => {
  // 59s / 30s -> counter 1 -> HOTP 287082.
  assert.equal(totp(RFC_SECRET, { timestamp: 59 * 1000 }), "287082");
});