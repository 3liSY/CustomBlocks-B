// CustomBlocks Cloud Vault — worker (Group 20)
// ---------------------------------------------------------------------------
// Two storage paths:
//
//   • category / note  →  Cloudflare KV (small, permanent shared library).
//       POST /category?name=<id>   body = zip   -> 200, body = the share code (text)
//       GET  /category/<code>                   -> 200, body = the zip
//       POST /note                 body = json  -> 200, body = the share code (text)
//       GET  /note/<code>                       -> 200, body = the json
//
//   • backup           →  Cloudflare R2 via a one-time presigned URL.
//       Backups are 40–150+ MB — far past KV's 25 MB cap AND past the worker's
//       own 100 MB request-body limit. So the worker never touches the bytes:
//       it hands the mod a signed R2 link and the mod uploads STRAIGHT to R2.
//       POST /backup?name=<name>   -> 200, JSON { "code": "...", "url": "<presigned PUT>" }
//       GET  /backup/<code>        -> 302 redirect to a presigned GET (download/restore)
//
//   • GET /                         -> 200, "CustomBlocks vault OK" (health check)
//
// Bindings / vars required:
//   KV namespace bound as            VAULT                 (category + note)
//   Vars/secrets for R2 (backup):    R2_ACCOUNT_ID
//                                    R2_BUCKET             e.g. cb-backups
//                                    R2_ACCESS_KEY_ID      (R2 API token)
//                                    R2_SECRET_ACCESS_KEY  (R2 API token, secret)
//
// Backup expiry (3 months) is set on the R2 BUCKET as a lifecycle rule in the
// dashboard, not here. Shared categories/notes never expire (permanent library).
//
// NOTE (security): minimal test worker — anyone with the URL can upload/download.
// Hardening (request signing, rate limit, WAF) is Group 20 S4. Keep the URL private.
// ---------------------------------------------------------------------------

// KV-stored kinds (category + note). Small, permanent.
const KV_MAX_BYTES = {
  category: 5 * 1024 * 1024,
  note:     5 * 1024 * 1024,
};
const KV_CONTENT_TYPE = {
  category: "application/zip",
  note:     "application/json",
};

const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no look-alikes (no 0/O, 1/I)

function makeCode(len = 6) {
  const bytes = new Uint8Array(len);
  crypto.getRandomValues(bytes);
  let out = "";
  for (let i = 0; i < len; i++) out += CODE_ALPHABET[bytes[i] % CODE_ALPHABET.length];
  return out;
}

function text(body, status = 200) {
  return new Response(body, { status, headers: { "content-type": "text/plain; charset=utf-8" } });
}
function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), { status, headers: { "content-type": "application/json; charset=utf-8" } });
}

// ── AWS SigV4 presigner for R2 (S3-compatible) ──────────────────────────────
// Produces a URL the mod can PUT/GET directly — bytes never pass through the
// worker, so the 100 MB worker body limit does not apply.

function awsUriEncode(str, encodeSlash = true) {
  let out = "";
  for (const ch of str) {
    if (/[A-Za-z0-9\-._~]/.test(ch)) { out += ch; continue; }
    if (ch === "/" && !encodeSlash) { out += "/"; continue; }
    for (const b of new TextEncoder().encode(ch)) {
      out += "%" + b.toString(16).toUpperCase().padStart(2, "0");
    }
  }
  return out;
}

async function sha256hex(input) {
  const data = typeof input === "string" ? new TextEncoder().encode(input) : input;
  const buf = await crypto.subtle.digest("SHA-256", data);
  return [...new Uint8Array(buf)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

async function hmac(key, data) {
  const k = typeof key === "string" ? new TextEncoder().encode(key) : key;
  const ck = await crypto.subtle.importKey("raw", k, { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  return new Uint8Array(await crypto.subtle.sign("HMAC", ck, new TextEncoder().encode(data)));
}

// Presign an R2 object request. method = "PUT" | "GET". Returns a full URL.
async function presignR2(env, method, key, expiresSeconds) {
  const host = env.R2_ACCOUNT_ID + ".r2.cloudflarestorage.com";
  const region = "auto";
  const service = "s3";

  const amzdate = new Date().toISOString().replace(/[:-]|\.\d{3}/g, ""); // YYYYMMDDTHHMMSSZ
  const datestamp = amzdate.slice(0, 8);
  const scope = `${datestamp}/${region}/${service}/aws4_request`;

  const canonicalUri = "/" + awsUriEncode(env.R2_BUCKET, false) + "/" + awsUriEncode(key, false);

  const params = {
    "X-Amz-Algorithm": "AWS4-HMAC-SHA256",
    "X-Amz-Credential": `${env.R2_ACCESS_KEY_ID}/${scope}`,
    "X-Amz-Date": amzdate,
    "X-Amz-Expires": String(expiresSeconds),
    "X-Amz-SignedHeaders": "host",
  };
  const canonicalQuery = Object.keys(params)
    .sort()
    .map((k) => awsUriEncode(k) + "=" + awsUriEncode(params[k]))
    .join("&");

  const canonicalRequest = [
    method,
    canonicalUri,
    canonicalQuery,
    `host:${host}\n`,
    "host",
    "UNSIGNED-PAYLOAD",
  ].join("\n");

  const stringToSign = [
    "AWS4-HMAC-SHA256",
    amzdate,
    scope,
    await sha256hex(canonicalRequest),
  ].join("\n");

  const kDate = await hmac("AWS4" + env.R2_SECRET_ACCESS_KEY, datestamp);
  const kRegion = await hmac(kDate, region);
  const kService = await hmac(kRegion, service);
  const kSigning = await hmac(kService, "aws4_request");
  const sig = [...(await hmac(kSigning, stringToSign))].map((b) => b.toString(16).padStart(2, "0")).join("");

  return `https://${host}${canonicalUri}?${canonicalQuery}&X-Amz-Signature=${sig}`;
}

function r2Configured(env) {
  return env.R2_ACCOUNT_ID && env.R2_BUCKET && env.R2_ACCESS_KEY_ID && env.R2_SECRET_ACCESS_KEY;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const parts = url.pathname.split("/").filter(Boolean); // "/category/ABC123" -> ["category","ABC123"]
    const kind = parts[0] || "";

    // Health check — open this URL in a browser to confirm the worker is live.
    if (request.method === "GET" && parts.length === 0) return text("CustomBlocks vault OK");

    // ── backup → R2 (presigned, bytes bypass the worker) ────────────────────
    if (kind === "backup") {
      if (!r2Configured(env)) return text("Worker misconfigured: missing R2_* vars for backups", 500);
      const key = (code) => "backups/" + code + ".zip";

      // Hand out a one-time upload link + code (the mod PUTs the zip itself).
      if (request.method === "POST" && parts.length === 1) {
        const code = makeCode();
        const putUrl = await presignR2(env, "PUT", key(code), 3600); // valid 1h
        return json({ code, url: putUrl });
      }
      // Download/restore by code → redirect to a presigned GET.
      if (request.method === "GET" && parts.length === 2) {
        const getUrl = await presignR2(env, "GET", key(parts[1]), 3600);
        return Response.redirect(getUrl, 302);
      }
      return text("Bad request", 400);
    }

    // ── category / note → KV ────────────────────────────────────────────────
    if (!(kind in KV_MAX_BYTES)) return text("Not found", 404);
    if (!env.VAULT) return text("Worker misconfigured: no VAULT KV binding", 500);

    // Store:  POST /category   or   POST /note
    if (request.method === "POST" && parts.length === 1) {
      const buf = await request.arrayBuffer();
      if (buf.byteLength === 0) return text("Empty upload", 400);
      if (buf.byteLength > KV_MAX_BYTES[kind]) return text("Payload too large", 413);
      const code = makeCode();
      await env.VAULT.put(kind + ":" + code, buf); // no ttl => permanent library
      return text(code);
    }

    // Fetch:  GET /category/<code>   or   GET /note/<code>
    if (request.method === "GET" && parts.length === 2) {
      const data = await env.VAULT.get(kind + ":" + parts[1], { type: "arrayBuffer" });
      if (!data) return text("Unknown or expired code", 404);
      return new Response(data, { status: 200, headers: { "content-type": KV_CONTENT_TYPE[kind] } });
    }

    return text("Bad request", 400);
  },
};
