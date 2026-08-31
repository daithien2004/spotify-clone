import { createHmac } from "node:crypto";
import { pathToFileURL } from "node:url";

// RFC 4648 base32 alphabet (uppercase). Input is case-insensitive; "=" padding
// and whitespace are ignored — QR secrets from enroll often come unpadded.
const BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

export function decodeBase32(input) {
  const clean = input.toUpperCase().replace(/[=\s]/g, "");
  const bytes = [];
  let buffer = 0;
  let bitsLeft = 0;
  for (const ch of clean) {
    const idx = BASE32.indexOf(ch);
    if (idx === -1) throw new Error(`invalid base32 character: ${ch}`);
    buffer = (buffer << 5) | idx;
    bitsLeft += 5;
    if (bitsLeft >= 8) {
      bytes.push((buffer >>> (bitsLeft - 8)) & 0xff);
      bitsLeft -= 8;
    }
  }
  return Buffer.from(bytes);
}

// RFC 4226 HOTP: HMAC-SHA1(counter), dynamic truncation, zero-padded digits.
export function hotp(secret, counter, digits = 6) {
  const key = decodeBase32(secret);
  const msg = Buffer.alloc(8);
  msg.writeBigUInt64BE(BigInt(counter));
  const hmac = createHmac("sha1", key).update(msg).digest();
  const offset = hmac[hmac.length - 1] & 0x0f;
  const binary =
    ((hmac[offset] & 0x7f) << 24) |
    ((hmac[offset + 1] & 0xff) << 16) |
    ((hmac[offset + 2] & 0xff) << 8) |
    (hmac[offset + 3] & 0xff);
  return (binary % 10 ** digits).toString().padStart(digits, "0");
}

// RFC 6238 TOTP. timestamp in ms (Date.now()); period in seconds.
export function totp(
  secret,
  { period = 30, digits = 6, timestamp = Date.now() } = {}
) {
  const counter = Math.floor(timestamp / 1000 / period);
  return hotp(secret, counter, digits);
}

// CLI: node scripts/totp.mjs <BASE32_SECRET> [period]
if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  const secret = process.argv[2];
  if (!secret) {
    console.error("usage: node scripts/totp.mjs <BASE32_SECRET> [period]");
    process.exit(1);
  }
  const period = Number(process.argv[3] || 30);
  console.log(totp(secret, { period }));
}