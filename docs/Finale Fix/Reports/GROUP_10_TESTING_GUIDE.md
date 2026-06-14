# 🧪 Group 10 — Color & Image Tools — Testing

> 🟢 Build green = it compiles + all gates pass.   ✋ Only you, in-game, can say it works.
> 📦 Jar: `build/libs/customblocks-1.0.0.jar` (copy to your server's mods folder yourself)
> ✅ **`/cb resize` + `/cb exportpng` confirmed in-game (§1).** The rest is the **Coloring redesign** — built,
> build green, but NOT yet in-game tested.

**Legend:**  🎯 test now · ✅ confirmed · 🟡 polish later

---

## 🔁 What changed in the redesign (read first)

- **`/cb colors` is now `/cb coloring`** (old name still works). It's a polished hub for everything; there's
  also a **Coloring** tile on the `/cb` dashboard.
- **`/cb bgstudio` with no id opens a block picker** — pick a block, then the studio. `/cb bgstudio <id>`
  still goes straight in. Same picker backs the hub's Background Studio / Colour Variants / Live Recolour.
- **`/cb tolerance <value> [id]`** — value comes first now. **No id = global default.** **With an id = just that
  block** (saved per-block; global stays). e.g. `/cb tolerance 40` vs `/cb tolerance 40 myblock`.
- **Palette is now useful** — its swatches appear as one-click quick-picks in the BgStudio **fill** picker,
  the **Gradient** endpoints, and **Custom Colour**.
- **`/cb gradient` with no args opens the Gradient Builder GUI** (was an error before). Gradient stays.

---

## 🗺️ At a glance

| | What | Where |
|:--:|---|:--:|
| ✅ | **`/cb resize`** · **`/cb exportpng`** (with **[download]** link) — *confirmed in-game* | §1 |
| 🎯 | **`/cb gradient`** (no args → GUI) · **Gradient Builder** + palette endpoints | §2 |
| 🎯 | **`/cb bgstudio`** (no id → picker) · clearer modes · palette fills · ↩ Undo · **`/cb tolerance`** | §3 |
| 🎯 | **Colour Variants** panel (editor or hub picker) | §4 |
| 🎯 | **`/cb palette`** as a shared colour source · **`/cb coloring`** hub | §5 |
| 🎯 | **Live recolour slider** · **screen eyedrop** *(client-side)* | §6 |

> ⚠️ `/cb dress` was removed earlier (redundant with Colour Variants + live recolour).

---

# 🎯 Test now

## §1 · Resize + Export PNG  *(Slice 1)*

> 💡 **What it does:** `/cb resize` changes a block's texture resolution (re-rendering from its saved image
> when possible, so it stays crisp). `/cb exportpng` saves a block's texture out to a PNG file you can grab.

> 🧰 **Before you start:** have a URL-made block, e.g. `/cb create g10a <image-url>`.

**Try these:**

- **① Resize up** — `/cb resize g10a 256`
  - ✅ **Pass:** chat says `"g10a" texture resized to 256×256px (re-rendered from its saved image).` The
    block looks sharper. Accept the pack prompt if asked.
- **② Resize down** — `/cb resize g10a 64`
  - ✅ **Pass:** resizes to 64×64 (chunkier). No errors.
- **③ Refuses a textureless / locked block** — `/cb create g10empty` (no URL), then `/cb resize g10empty 128`;
  also `/cb lock g10a` → `/cb resize g10a 128`.
  - ✅ **Pass:** clear messages ("no texture to resize" / "is locked") — no crash. (Unlock with `/cb unlock g10a`.)
- **④ Export PNG** — `/cb exportpng g10a`
  - ✅ **Pass:** chat says `Exported "g10a" texture → config/customblocks/cloud_exports/g10a.png` with the
    full path beneath, and a clickable **[download]** link.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | resize up re-renders from the saved image |
| ✅ | ② | resize down works |
| ✅ | ③ | textureless / locked blocks are refused cleanly |
| ✅ | ④ | exportpng writes the PNG file + [download] link |

---

## §2 · Gradient (command + GUI)

> 💡 **What it does:** `/cb gradient` creates a row of solid-colour blocks stepping smoothly between two
> endpoints (perceptual CIE-Lab interpolation). The new **Gradient Builder** GUI lets you pick the
> two colours visually (type a hex, pick from a block, or grab from your palette) and preview the
> swatches before creating them.

> 🧰 **Before you start:** two URL-made blocks, e.g. `/cb create g10a <url1>` and `/cb create g10b <url2>`
> with different-coloured images.

**Try these:**

- **① Gradient command** — `/cb gradient g10a g10b 4`
  - ✅ **Pass:** chat says `Created 4 gradient block(s): gradient_1, gradient_2, gradient_3, gradient_4.`
    Place them in a row — each is a smooth colour step from g10a's colour toward g10b's.
- **② Gradient is one undo** — `/cb undo`
  - ✅ **Pass:** all four `gradient_*` blocks vanish in a single undo (chat: `Undid gradient (4 blocks)`).
- **③ Gradient Builder GUI** — `/cb gradient` (no args) *(or `/cb coloring` → Gradient Builder)*
  - ✅ **Pass:** a 5-row chest opens with Colour A / Colour B tiles, a Steps ±, preview swatches, palette
    quick-pick swatches (left-click → A, right-click → B), and a green **Create gradient** button.
- **④ Pick colours in the GUI** — Left-click "Colour A" → type `red` in the anvil. Left-click "Colour B" →
  type `#0000FF`. Set steps to 3.
  - ✅ **Pass:** preview swatches appear between A and B as nearest-wool tiles (purple-ish in this case).
    The Create button shows `3 block(s) from #FF0000 → #0000FF`.
- **⑤ Pick from a block** — Right-click "Colour A" → chat prompts `/cb gradientpick a <blockId>` → type
  `g10a`. The GUI reopens with Colour A set to g10a's average colour.
  - ✅ **Pass:** Colour A shows the hex and "from block: g10a".
- **⑥ Create from the GUI** — Click **Create gradient**.
  - ✅ **Pass:** blocks are created. One `/cb undo` removes the whole batch.
- **⑦ Dress is gone** — `/cb dress g10a red 0.5`
  - ✅ **Pass:** unknown-command error (dress no longer exists).

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | gradient command creates N interpolated blocks |
| ✅ | ② | the whole gradient batch reverts in one undo |
| ✅ | ③ | Gradient Builder GUI opens from Colour Tools hub |
| ✅ | ④ | typing hex/names via anvil sets both endpoints + preview shows |
| ✅ | ⑤ | right-click picks a block's average colour |
| ✅ | ⑥ | Create from GUI actually bakes the blocks |
| ✅ | ⑦ | /cb dress is gone |

---

## §3 · Background Studio + fill colour + tolerance

> 💡 **What it does:** re-run background removal on an **existing** block, on demand, picking the mode +
> strength + **fill colour** yourself. The fill colour determines what the removed background becomes
> (defaults to black). The four modes are: No removal / Background only / Background + enclosed / Smart.

> 🧰 **Before you start:** a URL-made block with a clear subject on a plain-ish background, e.g.
> `/cb create g10a <image-url>`.

**Try these:**

- **⓪ No-arg picker** — `/cb bgstudio` (no id).
  - ✅ **Pass:** a "Pick a block — Background Studio" list opens; clicking a block opens its studio.
- **① Open it** — `/cb bgstudio g10a` *(or `/cb editor g10a` → "Background Studio", or the Coloring hub)*
  - ✅ **Pass:** a 5-row chest opens with four mode tiles (**Keep background / Remove background /
    Remove background + gaps / ★ Smart auto**), a strength −/＋, a **fill colour** tile (black, "#000000"),
    palette quick-fill swatches, an **↩ Undo last change** tile, and a green **Apply**. The header shows
    whether the strength is "(this block)" or "(global default)".
- **② Background only** — click "Background only", set strength ~30%, click **Apply**.
  - ✅ **Pass:** chat `"g10a" background updated`; the flat background is replaced with black. `/cb undo` restores.
- **③ Change fill colour** — Left-click the fill-colour tile → type `red` (or `#FF0000`) in the anvil.
  - ✅ **Pass:** the tile updates to red wool, hex shows `#FF0000`. Apply again → background is now RED.
- **④ Right-click resets fill** — Right-click the fill-colour tile.
  - ✅ **Pass:** resets to black wool + `#000000`.
- **⑤ Smart (offline)** — click "Smart", **Apply**.
  - ✅ **Pass:** only the main subject survives; loose background clutter is gone. Undoable.
- **⑥ `/cb tolerance` (value first now)** — global: `/cb tolerance 60` → chat confirms the **global** default
  changed (no block touched). Per-block: `/cb tolerance 60 g10a` → re-applies g10a at 60% immediately and
  saves it as g10a's own strength; chat notes the global default stays. Reopen `/cb bgstudio g10a` → header
  shows 60% "(this block)". Undoable.
  - ✅ **Pass:** both forms behave as above; the per-block value sticks across a reopen.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | BgStudio opens with fill-colour tile |
| ✅ | ② | background-only cuts + undoes (default black fill) |
| ✅ | ③ | fill colour changes via anvil (e.g. red) |
| ✅ | ④ | right-click resets fill to black |
| ✅ | ⑤ | smart isolates subject |
| ✅ | ⑥ | tolerance applies immediately |

---

## §4 · Colour Variants panel

- **① Open** — `/cb editor g10a` → **Colour Variants**.
  - ✅ **Pass:** a row of swatches (Lighter, Darker, Vivid, Muted, Complementary, two Split-Complements),
    each lore previewing the rough resulting colour.
- **② Make one** — click **Lighter**.
  - ✅ **Pass:** chat `"g10a_lighter" created`; you're handed the new block; it's a lighter version of g10a.
    `/cb undo` removes it. Clicking the same swatch again makes `g10a_lighter_2` (no clash).

**📋 Scorecard** — ✅ ① panel shows swatches ✅ ② clicking creates a colour-shifted block (undoable)

---

## §5 · Palette (anvil "Add colour") + Colours hub

> 💡 **What changed:** the Palette is now a **shared colour source**. Its working swatches appear as one-click
> quick-picks in the **Background Studio fill** picker, the **Gradient Builder** endpoints, and **Custom Colour**.
> "＋ Add colour" opens an anvil; the layout is actions on row 2, working swatches on row 3, saved palettes
> on rows 4-5.

- **⓪ Shared source** — add a couple of colours (e.g. `red`, `#00AAFF`). Then open `/cb bgstudio <id>`,
  `/cb gradient`, and `/cb coloring` → Custom Colour.
  - ✅ **Pass:** those same colours show up as clickable swatches in all three (fill / endpoints / colour pair).

- **① Add via anvil** — `/cb colors` → **Palette** → click **＋ Add colour** → type `red` in the anvil →
  take the output.
  - ✅ **Pass:** the palette reopens with a red swatch in your working set.
- **② Add via hex** — click **＋ Add colour** → type `#00AAFF` → take it.
  - ✅ **Pass:** `#00AAFF` appears as a light-blue wool swatch.
- **③ Remove swatch** — Left-click the red swatch.
  - ✅ **Pass:** red is removed from the working set.
- **④ Save + load** — click **Save current as…** → type `mytheme` → take it. Then click **Clear working set**.
  Load it back by left-clicking `mytheme` in the saved list.
  - ✅ **Pass:** save confirms; clear empties; load restores.
- **⑤ Delete saved palette** — Right-click `mytheme`.
  - ✅ **Pass:** palette deleted.
- **⑥ Coloring hub layout** — `/cb coloring` *(or `/cb colors`, or the dashboard's Coloring tile)*
  - ✅ **Pass:** titled "Coloring"; shows Background Studio, Palette, Gradient Builder, Custom Colour (top row)
    and Colour Variants, Live Recolour, Screen Eyedrop (lower row). Block tools open a picker first.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | "Add colour" opens an anvil (not chat) |
| ✅ | ② | hex input works via anvil |
| ✅ | ③ | left-click removes a swatch |
| ✅ | ④ | save as… / clear / load via the GUI |
| ✅ | ⑤ | right-click deletes a saved palette |
| ✅ | ⑥ | Colours hub has Gradient Builder tile |

---

## §6 · Live recolour slider + screen eyedrop  *(client-side — needs the mod installed on your client)*

> ⚠️ These two are **client screens**. They only work if you're playing with the CustomBlocks mod on your own
> client (which you are). On a vanilla client nothing opens — that's expected.

- **① Live recolour** — `/cb editor g10a` → **Live Recolour** *(or `/cb livecolor g10a`)*.
  - ✅ **Pass:** a screen opens showing a preview of g10a's texture with three drag bars (Hue / Saturation /
    Lightness). **Dragging updates the preview live.** Click **Apply** → the real block recolours in-world; chat
    `Recoloured "g10a"`. `/cb undo` reverts. *(If the preview says "unavailable", Apply still works.)*
- **② Screen eyedrop** — `/cb eyedrop` *(or Colours hub → Screen Eyedrop)*.
  - ✅ **Pass:** the world stays visible with a crosshair; click any pixel → your chat opens pre-filled with
    `/cb palette add #RRGGBB` for that pixel's colour. Press Enter to add it to your palette.

**📋 Scorecard** — ✅ ① slider previews live + Apply recolours (undoable) ✅ ② eyedrop grabs the clicked pixel's colour

---

## 🆘 If a test fails, send me
- 🔢 the step number · 👀 what happened vs expected (📸 screenshot helps) · 📄 last ~20 lines of `latest.log`

## 🧹 Cleanup
`/cb delete g10a` · `/cb delete g10b` · `/cb delete g10empty` · `/cb delete gradient_1` … `gradient_4`
*(or just `/cb undo` after each gradient while testing)*
