# Phase 4 Testing Tutorial — Texture Pipeline

> **Prerequisite:** Phase 3 ✅ confirmed. Creative world, cheats ON.
>
> **Goal:** Verify that `/cb retexture <id> <url>` downloads an image, converts it to
> a block texture, and displays it on the block for everyone on the server.
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What Phase 4 covers

| Feature | Status |
|---|---|
| `/cb retexture <id> <url>` | ✅ Built |
| `/cb create <id> <name> <url>` (one-shot texture) | ✅ Built |
| Image download from HTTP/HTTPS URL | ✅ Built — `ImageDownloader.java` |
| Image → PNG conversion (resize, crop, convert formats) | ✅ Built — `ImageProcessor.java` |
| Resource pack generation + HTTP server | ✅ Built (from Phase 1) |
| Texture persistence (survives restart) | ✅ Built — `TextureStore.java` |

Tests below cover all these.

---

## Setup — Get a direct image URL

You need a **direct link to a PNG or JPG image** (not a webpage).

**Easy sources:**
- Right-click any image in your browser → **Copy Image Address**
- Wikimedia Commons: https://commons.wikimedia.org → search for image → right-click → Copy Image Address
- Try these test URLs:
  - Simple: `https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png`
  - Cat: `https://upload.wikimedia.org/wikipedia/commons/thumb/b/b1/VAN_CAT.png/450px-VAN_CAT.png`

**Bad URLs (will fail, test in 4.5):**
- `data:image/png;base64,AAAA` (data URL, not web link)
- `https://example.com/image.webp` (WebP not supported yet)
- Any `http://` or `https://` link to a page (not a direct image file)

---

## Test 4.1 — One-shot create + texture

```
/cb create p4cat Cat https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png
```

**Expected sequence:**
1. Green message: `Created p4cat (slot X)`
2. "Downloading texture for p4cat…"
3. A **resource-pack prompt** appears in-game (vanilla Minecraft resource-pack accept/decline screen)
   - Button says something like "CustomBlocks textures" or "Download Pack"
   - Click **Yes** (or **Proceed** / **Download** depending on your MC version)
4. Green message: `Textured p4cat — accept the pack prompt`

**Also check:**
5. `/cb give p4cat` → item appears
6. Place the block → **it shows the image** (not purple/black missing texture)

**Pass:** Block placed and shows the image, pack prompt accepted.  
**Fail:** "Downloading" never completes, no pack prompt, block stays purple, or error message.

---

## Test 4.2 — Retexture an existing block

1. First, create an untextured block:
   ```
   /cb create p4untex Untextured
   ```
2. Then retexture it:
   ```
   /cb retexture p4untex https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png
   ```

**Expected:**
1. "Downloading texture for p4untex…"
2. Resource-pack prompt appears → click Yes
3. Green message: `Textured p4untex — accept the pack prompt`

**Also check:**
4. `/cb give p4untex` → place block → shows the image

**Pass:** Block retextured and displays correctly.  
**Fail:** Download hangs, pack prompt never appears, or block stays purple.

---

## Test 4.3 — Multiple blocks, different textures

Create and texture two blocks with different images:

```
/cb create p4img1 Image1 https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png
/cb create p4img2 Image2 https://upload.wikimedia.org/wikipedia/commons/thumb/b/b1/VAN_CAT.png/450px-VAN_CAT.png
```

**Expected:** Both blocks texture successfully with their own images (each gets one pack prompt).

Place both in the world — **they show different images**, not the same texture.

**Pass:** Two distinct textures visible on two blocks.  
**Fail:** Blocks show the same image, or one didn't texture.

---

## Test 4.4 — Texture survives restart

1. Create and texture a block:
   ```
   /cb create p4persist PersistTest https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png
   ```
2. Place it in the world.
3. **Fully close Minecraft** — quit to desktop.
4. Relaunch and re-enter the world.
5. Run `/cb give p4persist` → place the block.

**Expected:** Block **still shows the image** after restart. Accept the pack prompt if asked.

**Pass:** Texture persisted across restart.  
**Fail:** Block is purple after restart (texture lost).

---

## Test 4.5 — Bad URL is handled cleanly

**Test 4.5a — data: URL (not a web link):**
```
/cb create p4bad BadURL data:image/png;base64,AAAA
```
**Expected red error:**
```
Not a web link — use a direct http/https image URL. Not created
```
**Also check:** `/cb list` does NOT show `p4bad` (block was never created).

**Test 4.5b — WebP (not supported yet):**
```
/cb retexture p4untex https://example.com/image.webp
```
(Use any real WebP URL, or a fake HTTPS link)
**Expected:** Error message explaining the format is not supported.

**Pass:** Both give clear errors, blocks not corrupted, no crashes.  
**Fail:** Crash, cryptic error, or block created despite error.

---

## Test 4.6 — Pack prompt caching / repeated textured blocks

1. Create block:
   ```
   /cb create p4repeat Repeat https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png
   ```
   Accept the pack prompt.

2. Retexture the **same** block with a **different** image:
   ```
   /cb retexture p4repeat https://upload.wikimedia.org/wikipedia/commons/thumb/b/b1/VAN_CAT.png/450px-VAN_CAT.png
   ```

**Expected:** Another pack prompt appears. Accept it. The block now shows the new (cat) image.

**Pass:** Retexture triggers a new pack prompt, texture updates.  
**Fail:** No prompt, or texture doesn't change.

---

## Test 4.7 — Image formats (PNG, JPG, GIF)

Test that different input formats all work:

**PNG:**
```
/cb create p4png PngTest https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png
```

**JPG:**
```
/cb create p4jpg JpgTest https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Camponotus_flavomarginatus_ant.jpg/320px-Camponotus_flavomarginatus_ant.jpg
```

(Use any freely available JPG on Wikimedia Commons or similar.)

**Expected:** Both texture successfully.

**Pass:** PNG and JPG both work.  
**Fail:** One format fails with an error.

> **Note:** GIF support comes in a later phase. If you try a GIF now, expect an error.

---

## Test 4.8 — Resource pack served on correct port

From the startup log, confirm:
```
[CustomBlocks] Resource-pack HTTP server live on port 8123
```

The pack is being served at:
```
http://127.0.0.1:8123/pack.zip
```

(Or whatever fallback port was printed in the log — if 8123 is in use, it tries others like 8124, 8081, etc.)

**Pass:** Log shows the HTTP server started.  
**Fail:** "Could not open an HTTP port" error in log.

---

## Test 4.9 — Texture directory structure

After textured a block, check:
```
C:\Users\66664\AppData\Roaming\.minecraft\config\customblocks\textures\
```

You should see a file named something like `slot_5.png` (the slot number where your block is).
This is the raw texture PNG stored on disk.

**Pass:** Texture file exists at correct path.  
**Fail:** No texture file, or file is corrupted (0 bytes).

---

## Phase 4 Verdict

| Test | Description | Result |
|---|---|---|
| 4.1 | One-shot create + texture with URL | ⬜ |
| 4.2 | Retexture an existing block | ⬜ |
| 4.3 | Multiple blocks with different textures | ⬜ |
| 4.4 | Texture survives full restart | ⬜ |
| 4.5 | Bad URL (data: and WebP) errors cleanly | ⬜ |
| 4.6 | Retexture triggers new pack prompt | ⬜ |
| 4.7 | PNG and JPG both work | ⬜ |
| 4.8 | HTTP server running on correct port | ⬜ |
| 4.9 | Texture files saved to disk | ⬜ |

**When all 9 show ✅ — tell me "Phase 4 passes" and we move to Phase 5.**

If anything shows ❌ — paste:
1. The exact command you typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure
4. Whether pack prompt appeared (if relevant)

---

## Cleanup after testing

```
/cb delete p4cat
/cb delete p4untex
/cb delete p4img1
/cb delete p4img2
/cb delete p4persist
/cb delete p4png
/cb delete p4jpg
/cb delete p4repeat
```

---

## What's not tested (Phase 4 stretch features, not built)

- GIF support (converts GIF to static PNG)
- Animated textures (Phase 13)
- Custom texture size (beyond the default 64×64)
