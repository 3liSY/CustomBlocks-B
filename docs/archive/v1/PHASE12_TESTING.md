# Phase 12 Testing Tutorial — Arabic System

> **Prerequisite:** Phase 10 ✅ confirmed. Creative world open, cheats ON.
>
> **Goal:** Verify Arabic letter/word block creation, browser GUI, and give work correctly.
>
> **Font note:** `arabtype.ttf` is NOT bundled with the mod. The renderer will fall back
> to the system `SansSerif` font, which may lack full Arabic shaping. Letters will be
> rendered but might look like boxes or unshaped glyphs. To get proper Arabic text, place
> `arabtype.ttf` (or any Arabic TTF) at:
> `config/customblocks/arabtype.ttf`
> This is a known gap — logged as Phase 17 if it becomes a priority.

---

## What changed from old CustomBlocks → new

| Area | Old CustomBlocks | New CustomBlocks-B |
|---|---|---|
| Arabic letter import | `/cb arabic import` — 28 letter blocks, chest GUI browser | `/cb arabic import` — same. Browser via `/cb gui arabic`. |
| Font | `arabtype.ttf` bundled in JAR resources | Not bundled — user must place TTF in `config/customblocks/arabtype.ttf` or `config/customblocks/fonts/`. System fallback used otherwise. |
| Auto-joining | Place letters next to each other → forms auto-swap based on neighbors | **Not built.** Letters render in isolated form regardless of neighbors. (Phase 17 scope) |
| Word blocks | Supported | `/cb arabic word <text> <id> <displayName>` — creates a custom block with the word rendered as texture |
| GUI | Chest-based browser | `ArabicBrowserScreen.java` — screen-based (Phase 17 replacement in 17.3) |

---

## What Phase 12 covers (built)

| Feature | Command |
|---|---|
| Import all 28 letters | `/cb arabic import` |
| Import one letter | `/cb arabic letter <name>` |
| Create word block | `/cb arabic word <text> <id> <displayName>` |
| List imported letters | `/cb arabic list` |
| Browse in GUI | `/cb gui arabic` |
| Give a letter | `/cb give arabic_alef` (uses existing give command) |

**Not built from Bible spec:**
- `arabtype.ttf` font bundled in JAR
- Auto-joining (east/west neighbor detection + form swapping)

---

## Setup

No setup required. Letters use their own ID scheme (`arabic_alef`, `arabic_ba`, etc.) so they won't conflict with test blocks from other phases.

---

## Test 12.1 — Import all 28 letters

```
/cb arabic import
```

**Expected:**
```
Created 28 block(s).
```
(Or "Skipped N (already exist)" if previously imported — that's fine.)

**Pass:** 28 blocks created without error. Count in chat matches.
**Fail:** Error message, crash, or 0 created when letters didn't exist before.

---

## Test 12.2 — List shows all 28

```
/cb arabic list
```

**Expected:** `Arabic: 28/28 letters imported` header, then 28 lines — each with the English name, Arabic script, and `[give]` tag.

**Pass:** 28/28 shown, all have `[give]` tag.
**Fail:** Less than 28, or some show `[not imported]`.

---

## Test 12.3 — Give a letter and place it

Click `[give]` next to `alef` in the list, or run:

```
/cb give arabic_alef
```

Place the block in the world.

**Expected:** Block placed. It renders with the letter texture (Arabic ا — may look like a box if no Arabic font installed, that's expected).

**Pass:** Block places and renders something. No crash.
**Fail:** Give fails, or placed block is invisible/broken.

---

## Test 12.4 — Arabic browser GUI

```
/cb gui arabic
```

**Expected:** Arabic browser screen opens showing the imported letters.

Press Esc — screen closes.

**Pass:** Screen opens and closes cleanly.
**Fail:** Crash or nothing.

---

## Test 12.5 — Import single letter

First, delete one letter to test single import:

```
/cb delete arabic_ba
```

Then import just that letter:

```
/cb arabic letter ba
```

**Expected:** `Letter ba ready. Give: /cb give arabic_ba`

**Pass:** Single letter re-created successfully.
**Fail:** "Unknown letter" error for a valid name, or "Failed to import".

---

## Test 12.6 — Word block creation

```
/cb arabic word مرحبا p12word HelloArabic
```

**Expected:** `Arabic word block p12word created. Give: /cb give p12word`

Then:

```
/cb give p12word
```

Place it — block renders the word as a texture.

**Pass:** Word block created, placed, renders something.
**Fail:** Error creating the word block, or give fails.

---

## Test 12.7 — Tab-complete for letter names

Type (don't press Enter):
```
/cb arabic letter 
```
Press **Tab** after the space.

**Expected:** Suggestions include letter names: `alef`, `ba`, `ta`, etc.

**Pass:** Tab-complete shows letter names.
**Fail:** No suggestions.

---

## Phase 12 Verdict

| Test | Description | Result |
|---|---|---|
| 12.1 | Import all 28 letters | ✅ |
| 12.2 | List shows 28/28 | ✅ |
| 12.3 | Give letter and place it | ✅ |
| 12.4 | Arabic browser GUI opens | ✅ |
| 12.5 | Import single letter | ✅ |
| 12.6 | Word block creation | ❌ BROKEN — Minecraft chat can't input Arabic Unicode; Brigadier rejects it |
| 12.7 | Tab-complete for letter names | ✅ |

**Phase 12 — PARTIAL PASS. Core letter system works. Known issues deferred to Phase 17:**

| Issue | Phase 17 ref |
|---|---|
| Can't type Arabic in Minecraft chat → `/cb arabic word` command unusable as-is | 17.18 |
| `arabtype.ttf` not bundled — letters render via system font fallback | 17.19 |
| Auto-joining not built | 17.20 |
| English font for word blocks — needs developer decision | 17.21 (interview) |

If anything shows ❌ — paste:
1. The exact command typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure

---

## Cleanup after testing

```
/cb arabic import
```
(Re-import if you deleted `arabic_ba` in test 12.5 and want the full set back)

```
/cb delete p12word
```
