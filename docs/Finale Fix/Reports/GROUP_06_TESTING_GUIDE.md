# Group 06 — Testing Guide

*One green build proves it compiles — nothing is **done** until you confirm it in-game.*

**Legend:**  🎯 test now  ·  ☑️ you confirmed  ·  🟡 works, polish later  ·  ⏳ not built yet

---

## Where Group 06 stands

| | What | Where |
|---|---|---|
| 🎯 **TEST NOW** | **Round-2 fixes + M4.** Red Triangle art redrawn clean · pack reload now waits while ANY CustomBlocks GUI is open · **M4 per-face URL paint** (Rainbow Rectangle). Build green, jar in `.minecraft\mods`, **never run in-game.** | **§0d** |
| ☑️ Passed 2026-06-11 | §0c Config-GUI hex editor + Color Studio (developer: "everything else passes") — except ④ timing, refixed in §0d② | §0c |
| ☑️ Passed 2026-06-11 | M3 Square swap + marker purge (8/8) · M2 Triangle variants (6/6) · Retexture-all + source store (6/6) | §0 · §1 · §2 |
| ☑️ Working | Omni-Tool · Deleter · Magic Items GUI · `[CB]` prefix · bg-removal smart fill + pixelation · Texture-Size picker GUI | §3 |
| 🟡 Polish later | **hotbar wording (PARTIAL)** · bg fringe + low-contrast (parked) · chat flooding · partial GUIs · lore · chat formatting | §4 |
| ⏳ Coming | eyedropper · despeckle · preview · per-face paint · `.dat` migration (parked) | §5 |

---

# 🎯 TEST NOW

## §0b · Hex setters (M3 hex)

**In one line:** `/cb config hex red #FF8800` → "red" now MEANS orange — for new variants,
the item art, the item names, and (if you say Yes) your existing red blocks.

> 🧰 **Before you start:** a placed `vart_red` (§1 setup made it), a `_green` variant for
> test ⑦, and the Squares/Triangles in your inventory (`/cb` → Magic Items).

---

### ① Status line
```
/cb config hex
```
**Expect:** all four on one line — red `#EE3333` · yellow `#F0C814` · green `#1E8C1E` · black `#0A0A0A`.
`/cb config hex red` shows just red.

### ② Change a colour → confirm pops
```
/cb config hex red #FF8800
```
**Expect:** chat "Red `#EE3333` → `#FF8800`…", then — because `_red` blocks exist — the
**Yes / Info / No** chest opens. Info shows block count, old → new, "design untouched" note.

### ③ Yes — repaint existing blocks
Click **Yes**.
**Expect:** chat "Recolouring N red variant(s)…" → "Recoloured N block(s)". After the pack
reload, the placed `vart_red` background is **orange** — design intact.

### ④ Item art + names follow
Look at the **Red Square** + **Red Triangle** in inventory.
**Expect:** names read `Red Square [#FF8800]` / `Red Triangle [#FF8800]`, and the item art
itself is **orange-tinted** now.

### ⑤ New variants use the new colour
Red Triangle on a different block.
**Expect:** the new variant's background is **orange** (`#FF8800`), not the old red.

### ⑥ Bad input bounces
```
/cb config hex red banana
/cb config hex purple #112233
```
**Expect:** "use #RRGGBB" error · "pick red/yellow/green/black" error. Nothing changes.

### ⑦ No — keep existing blocks
```
/cb config hex green #00FFAA
```
Click **No** in the confirm.
**Expect:** existing green blocks unchanged; new green variants + item art use `#00FFAA`.

### ⑧ Survives restart
Restart the client/server.
**Expect:** `/cb config hex` still shows your custom hexes (saved to `config.json`).

---

## §0c · Config-GUI hex editor + pack-rebuild timing + Color Studio

**In one line:** the hexes are now editable from the Config GUI (dyes → anvil), the pack
reload politely waits for the recolour prompt, and `/cb customcolor` is back — a Color
Studio that hands out magic tool pairs in 29 preset colours or any hex.

> 🧰 **Before you start:** at least one `*_red` variant placed (for the confirm test) and a
> textured custom block to try the custom tools on.

### ① Config GUI → Variant Colours
```
/cb config
```
**Expect:** a glowing **Variant Colours** entry (red dye) listing all four hexes. Click it →
a "Variant Colours" sub-GUI with four dyes — §cred §eyellow §agreen §8black — each showing
its current hex, its default, and how many `_colour` blocks exist.

### ② Edit a hex in the anvil
Click the **Red dye**.
**Expect:** an anvil opens titled "Set Red hex (#RRGGBB)" with the current hex pre-typed.
Type `#FF8800`, take the output item.
**Expect:** same flow as `/cb config hex red #FF8800` — chat confirm; with `_red` blocks
placed the Yes/Info/No prompt opens; with none you land back in the colour editor showing
the new hex.

### ③ Anvil bad input + cancel
Type `banana` and take the output → "use #RRGGBB" error, back to the editor, nothing changed.
Press ESC in the anvil → straight back to the editor, nothing changed.

### ④ Pack rebuild WAITS for the confirm (the §0b bug)
With `_red` blocks placed: `/cb config hex red #22AAFF`
**Expect:** the Yes/Info/No prompt opens **without** a resource-pack reload starting first.
The reload happens only AFTER you answer: **Yes** → one reload after the repaint batch ·
**No / close / ESC** → one reload right then (item art re-tints either way).

### ⑤ Color Studio opens
```
/cb customcolor
```
**Expect:** a 6-row "Color Studio" with 29 colours as dyes (Purple, Pink, Baby Blue…), each
showing its hex + RGB, plus a glowing **Custom Hex…** anvil button.

### ⑥ Preset → tool pair
Click **Purple**.
**Expect:** chat "Gave you the Purple Square + Triangle (#7700CC)" — two glinting tools whose
names are coloured purple and whose icons are purple-tinted. The studio stays open.

### ⑦ Custom hex → tool pair
Click **Custom Hex…**, type `#FF1493`, take the output.
**Expect:** a pair labelled `#FF1493` lands in your inventory; back in the studio. Typing a
name like `lavender` works too; `banana` errors and returns to the studio.

### ⑧ The custom tools actually work
Custom **Triangle** on a textured block `foo` → creates `foo_hex_ff1493` (background in that
colour, design intact). Custom **Square** on the placed original → swaps it to the variant.
Square with no matching variant → "create one with the #FF1493 Triangle first".

### ⑨ Direct command form
```
/cb customcolor pink
/cb customcolor #00CED1
/cb customcolor banana
```
**Expect:** pink pair · teal pair · error with "did you mean …" suggestions.

### ⑩ Survives restart
Restart with the custom tools in your inventory and `foo_hex_ff1493` placed.
**Expect:** tools keep their colour/name/tint and still work · the hex variant block is still
there with its texture · `/cb config hex` keeps your edited hexes.

---

## §0d · Round-2 fixes + M4 per-face paint

**In one line:** the Red Triangle art is redrawn in the clean yellow style, pack reloads now
wait politely while you're inside ANY CustomBlocks menu/anvil, and the Rainbow Rectangle paints
single faces from a URL.

### ① Red Triangle looks right
Look at the Red Triangle (and a changed-hex colour's tools) in the inventory.
**Expect:** the red art is a clean, symmetric triangle exactly like the yellow one — fill,
lighter highlight, dark outline. Tools whose hex you changed keep that style in the new colour
(highlight no longer washed out). Custom (Color Studio) pairs use the clean shape too.

### ② Reload waits for ANY open GUI (the §0c④ re-fix)
Open `/cb config` → Variant Colours → edit red's hex in the anvil — **even with NO `_red`
blocks placed** (this was the exact case that broke before: log showed the rebuild starting
the same second).
**Expect:** chat confirms the change, you land back in the colour editor, and **no resource
reload happens while any CustomBlocks menu or anvil is open**. Close the menu →
the reload arrives a moment later. With `_red` blocks placed, the confirm GUI shows first and
the reload still only comes after you answer AND close.

### ③ Rectangle paints one face
Hold the **Rainbow Rectangle**, right-click the **north** face of a textured custom block.
**Expect:** chat opens pre-filled `/cb paintface <id> north ` — paste an image URL, Enter.
"Painted the north face…" → after one pack reload, ONLY that face shows the new image; the
other five faces keep the base texture.

### ④ Paint more faces + direct command
Paint `up` with a different URL; also try typing `/cb paintface <id> east <url>` by hand.
**Expect:** each face holds its own image; the rest stay base.

### ⑤ Clear a face / all faces
```
/cb clearface <id> north
/cb clearface <id> all
```
**Expect:** north returns to the base texture · then every painted face resets. "No painted
faces" message when nothing's left.

### ⑥ Area selector still works (moved to sneak)
**Sneak + right-click** two custom blocks with the Rectangle.
**Expect:** corner 1 / corner 2 messages exactly as before (Omni-Tool Area mode unaffected).

### ⑦ Errors bounce cleanly
`/cb paintface <id> sideways <url>` → "pick a face" error · bad URL → "couldn't get a texture"
error, face unchanged · `/cb paintface nope north <url>` → "no block called" error.

### ⑧ Survives restart
Restart with painted faces present.
**Expect:** every painted face is still painted (stored as files in
`config/customblocks/textures/slot_N_<face>.png`); `/cb clearface` still works on them.

### ⑨ Deleting the block cleans up
`/cb delete <id>` (or Deleter) on a block with painted faces, then create a new block.
**Expect:** no leftover face art ever bleeds onto other blocks.

---

### Scorecard

| ✓ | # | Proves |
|---|---|---|
| ⬜ | ① | status lines read right |
| ⬜ | ② | set opens the Yes/Info/No confirm (blocks exist) |
| ⬜ | ③ | Yes repaints placed `_red` blocks, design intact |
| ⬜ | ④ | Square/Triangle names show hex + art re-tinted |
| ⬜ | ⑤ | new variants use the new colour |
| ⬜ | ⑥ | bad hex / bad colour → clean errors |
| ⬜ | ⑦ | No keeps existing blocks, new ones still change |
| ⬜ | ⑧ | hexes survive a restart |

> ↩️ **Put it back anytime:** `/cb config hex red #EE3333` · `/cb config hex green #1E8C1E`
> (default item art returns automatically).

---

# ☑️ PASSED — kept for re-test reference

---

## ☑️ §0 · Square swap (M3) + marker purge — ALL 8 TESTS PASSED 2026-06-11

> ✅ You confirmed every test below in-game on 2026-06-11. Kept for re-test reference.
> 🟡 One PARTIAL: **hotbar message wording** — works, reads rough; queued with the chat/GUI polish (§4).

> Squares changed jobs too. The old "tagged at (x, y, z)" message is **gone**. Now:
> **right-click a placed custom block with a Square → that block changes IN PLACE** to the
> Square's colour variant (the one a Triangle made earlier). No new item, no block to place —
> the block in the wall just becomes the other colour.
>
> **Squares never create.** No variant of that colour → hotbar error telling you to make one
> with the Triangle first. **Black Square special:** no `_black` variant → swaps back to the
> **original base block**. Swapping is its own undo — click with the original colour's Square
> to go back (`/cb undo` does NOT cover swaps; nothing in the block registry changes).

### Setup
Reuse `vart` from §1 (or remake it), plus its variants: use the **Red Triangle** and **Black
Triangle** on it so `vart_red` and `vart_black` exist. Place a `vart` block. Grab **Squares**
(all four colours) from `/cb` → Magic Items.

### Tests

**1 — Swap works** — right-click the placed `vart` with the **Red Square**.
→ The placed block instantly becomes `vart_red` (red background, same design). Chat:
"Swapped to `vart_red`". No new item appeared.
*Broken if:* old "tagged at (x, y, z)" message, nothing happens, or block breaks/drops.

**2 — Swap between colours** — right-click the same block (now red) with the **Black Square**.
→ Block becomes `vart_black`.

**3 — Missing variant = error, never creates** — right-click with the **Green Square**
(no `vart_green` exists).
→ Hotbar error: "No green variant of `vart` exists — create one with the Green Triangle first."
Check `/cb list`: **no** `vart_green` appeared.

**4 — Black Square fallback** — delete the black variant (`/cb delete vart_black`), then
right-click the placed block with the **Black Square**.
→ Block swaps back to the **base** `vart` (no `_black` exists → base fallback).

**5 — Already that colour** — right-click a `vart_red` block with the **Red Square**.
→ Chat: "This block is already `vart_red`." Nothing changes.

**6 — Glow rides along** — give one variant a glow (`/cb setglow vart_red 12`), then swap a
dark-area block to it with the Red Square.
→ The swapped block **lights up** at glow 12.

**7 — Lore** — hover a Square in inventory.
→ New text: "swap it to its <colour> variant… create the variants with the Triangles."
Old "tagging/mark it" lines gone. Black Square has the extra fallback line.

**8 — Marker wording purged** — open `/cb` → Magic Items.
→ The shapes section label now reads **"Squares & Triangles — Triangles create colour
variants, Squares swap them"** (was "Marker Shapes — Tag your custom blocks by colour").
No "marker"/"tagging" text anywhere in the mod any more.

| ✓ | Step |
|---|---|
| ☑️ | 1 — Red Square swaps placed block to `vart_red`, no tag message |
| ☑️ | 2 — Black Square swaps red → black |
| ☑️ | 3 — Green Square (no variant) → error, nothing created |
| ☑️ | 4 — Black Square with no `_black` → back to base `vart` |
| ☑️ | 5 — same colour twice → "already" message, no change |
| ☑️ | 6 — swapped block carries the variant's glow |
| ☑️ | 7 — Square lore rewritten (no "tagging") — 🟡 hotbar wording polish queued |
| ☑️ | 8 — Magic Items label = "Squares & Triangles" (no "Marker Shapes") |

---

## ☑️ §1 · Triangle colour variants (M2) — ALL 6 TESTS PASSED 2026-06-11

> ✅ You confirmed every test below in-game on 2026-06-11. Kept for re-test reference.

> Triangles changed jobs. They no longer just tag a block — **right-clicking a placed custom
> block with a Triangle creates a COPY of that block with its background recoloured** to the
> triangle's colour. The copy is a real new block (`<id>_red`, `<id>_black`, …) that lands in
> your inventory. The original block is untouched. (Squares SWAP between variants — §0.)
>
> Colours (changeable via `/cb config hex` — §0b): Red `#EE3333` · Yellow `#F0C814` ·
> Green `#1E8C1E` · Black `#0A0A0A`.
>
> Get triangles from `/cb` → **Magic Items** (all four colours are in the grid).

### Setup
```
/cb create vart "hi" <image-url>
```
Place `vart` somewhere visible. Grab a **Red Triangle** from `/cb` → Magic Items.

### Tests

**1 — Create a variant** — right-click placed `vart` with the Red Triangle.
→ Chat: "Creating …", then "created — background recoloured to `#EE3333`". A `vart_red`
item lands in your inventory. Place it next to the original: **same design, red background**,
original unchanged.
*Broken if:* no chat, no item, design destroyed, or the ORIGINAL block changed.

**2 — Confirm GUI** — **sneak**+right-click `vart` with a triangle.
→ A **Yes / Info / No** chest opens. Info shows the new id, the hex, and notes
(source-vs-baked, already-exists). **Yes** creates it (chat + item) · **No** or closing does nothing.

**3 — Already exists** — right-click `vart` with the **same** Red Triangle again.
→ Instant "already exists — here's one" + another `vart_red` item. No re-render, no duplicate block.

**4 — Suffix swaps, never stacks** — use a Red Triangle on the placed **`vart_black`** (make one
with the Black Triangle first).
→ You get **`vart_red`** — NOT `vart_black_red`.

**5 — Untextured block** — use a triangle on a custom block that has **no texture**.
→ Friendly error, nothing created — check `/cb list` to confirm no ghost block appeared.

**6 — Undo** — right after creating a variant, run `/cb undo`.
→ The variant block is removed again.

| ✓ | Step |
|---|---|
| ☑️ | 1 — variant created: same design, red background, original untouched |
| ☑️ | 2 — sneak = Yes/Info/No GUI; Yes creates, No does nothing |
| ☑️ | 3 — repeat = instant "already exists" + item |
| ☑️ | 4 — `vart_black` + red → `vart_red` (swapped, not stacked) |
| ☑️ | 5 — untextured block → error, nothing created |
| ☑️ | 6 — `/cb undo` removes the variant |

---

## ☑️ §2 · Retexture-all — confirm GUI + `/cb retextureall` + source store — ALL 6 TESTS PASSED 2026-06-11

> ✅ You confirmed every test below in-game on 2026-06-11. Kept for re-test reference.

> Until now, changing texture size only affected **new** blocks. Three pieces landed:
> - **Source store** — every block textured **from now on** keeps its original image on disk
>   (`config/customblocks/sources/slot_N.src`), so it can be **truly re-rendered sharper** at any
>   size. Blocks made before this update have no source → they can only be **upscaled** (no new detail).
> - **Confirm GUI** — picking a size in the Texture-Size menu now opens a **Yes / Info / No**
>   chest (only when you have ≥1 block): Yes = retexture all EXISTING blocks to the new size too.
> - **`/cb retextureall [16-512]`** — the same batch as a direct command.

### Tests

**1 — Confirm opens** — with ≥1 block existing: `/cb config texturesize` → pick a size.
→ The **Yes / Info / No** confirm opens. **Info** shows the block count, the estimated new pack
size, and the source-vs-upscale note.
*Broken if:* picking a size silently returns to Config even though blocks exist.

**2 — Yes runs the batch** — click **Yes**.
→ Chat: "Retexturing N…" then "complete — N re-rendered, N upscaled, N skipped". Placed blocks
visibly change resolution after the (single) pack reload.

**3 — No keeps blocks** — re-open, pick a size, click **No**.
→ Existing blocks unchanged, but the new size IS saved and applies to **new** textures.

**4 — Zero blocks skips it** — with no blocks at all, picking a size goes straight back to
Config (no confirm).

**5 — Sharpen vs upscale (the important one)** — make one block NOW (it stores its source),
keep one OLD block from before today. Go UP in size (e.g. 64 → 256) and run the batch:
→ the **new** block becomes genuinely **sharper** · the **old** block is only upscaled —
bigger pixels, no new detail. That difference is correct, not a bug.

**6 — Direct command** — `/cb retextureall 128` runs the same batch without the GUI.

| ✓ | Step |
|---|---|
| ☑️ | 1 — picking a size opens Yes/Info/No; Info numbers read sane |
| ☑️ | 2 — Yes → progress + "complete" chat, blocks change |
| ☑️ | 3 — No → blocks unchanged, size still set for new blocks |
| ☑️ | 4 — 0 blocks → no confirm |
| ☑️ | 5 — new block sharpens for real; old block only upscales |
| ☑️ | 6 — `/cb retextureall <px>` works directly |

> ⚠️ Known limits (documented, not bugs): pre-today blocks have no source · Arabic/video blocks
> never have a single source · undo of a delete restores the texture but not the source.

---

## ☑️ §3 · Already working — only re-test if something feels off

| Feature | Quick check |
|---|---|
| Omni-Tool (Glow/Hardness/Area) | `/cb omni` → right-click a block cycles the mode's effect |
| Sneak-click **in the air** | hold Omni-Tool, sneak+right-click nothing → config GUI opens |
| Deleter | `/cb deleter` → right-click wipes a block; `/cb undo` restores |
| **Squares** | swap a placed block to its colour variant — ✅ passed (§0) |
| Magic Items GUI | `/cb` → Magic Items → double chest, all items, colour grid |
| `[CB]` prefix | chat + hotbar popup look identical |
| Background Removal | smart black/white fill + tolerance cap — re-tested ✅ 2026-06-11 |
| Texture-Size picker GUI | Config → Texture Quality row OR bare `/cb config texturesize` → 6 sizes (16–512), hover MB, click sets ✅ |

---

## 🟡 §4 · Polishing later (known, not bugs)

- **Hotbar message wording** (Square swap errors, tool feedback) — works, but the wording is
  rough; **PARTIAL, queued** with the chat-formatting polish (flagged 2026-06-11).
- **Bg removal — fringe** (Test 2: thin light halo on hard edges) and **low-contrast eat**
  (Test 4 at high strength) — **parked** unless you reopen them; eyedropper/despeckle (§5) are the real fix.
- **Chat flooding** — still happening; tracked PARTIAL, fix later.
- **All "partial" GUIs** (Group 02 chest GUIs, Omni-Tool config GUI, …) — revisit + final polish pass.
- **Item lore** — general tone pass still queued ("marker"/"tagging" already purged 2026-06-11).
- **Chat formatting** — proper formatting polish queued.

---

## ⏳ §5 · Coming next (not built — nothing to test)

| | Feature | What it'll do |
|---|---|---|
| A | Eyedropper | click a block to strip that exact colour |
| B | Despeckle | auto-wipe leftover confetti specks |
| C | Preview | pick from a few strength results |
| M4 | Per-face paint | recolour one block face from a URL |
| — | Held-block glow | dynamic light on a held block (deferred) |
| — | Old `.dat` migration | 770 old high-res blocks → new scheme; **parked** (decisions logged 2026-06-11: keep native 256, delete the 20 junk test blocks) |

---

## If a test fails
Send: the step number, what happened vs expected (a screenshot helps), and the last ~20 lines of
`.minecraft/logs/latest.log`.

## Cleanup
```
/cb delete vart
/cb delete vart_red
/cb delete vart_black
```
