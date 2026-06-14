# CustomBlocks GUI Design Guide

How to build a chest GUI that makes people smile. Every rule here is live in the
Bulk Dashboard (`BulkPropertyMenu` / `BulkFilterMenu`) — open `/cb bulkgui` next to this
file to see each one. New menus must follow this guide; consistency IS the polish.

The whole game in one line: a screen must answer three questions in under a second —
**Where am I? What can I do? What will happen?** Everything below serves those three.

---

## 1. Colour is a language, not decoration

One colour = one meaning, everywhere, forever. The player learns it once and never
reads a warning label again — the red frame already said it.

| Colour | Meaning | Frame (edge pane) | Text |
|---|---|---|---|
| Blue / light blue | calm · info · browsing · default | `LIGHT_BLUE_STAINED_GLASS_PANE` | `§b` |
| Red | DANGER · deletes · irreversible-feeling | `RED_STAINED_GLASS_PANE` | `§c` |
| Yellow | caution · changes things · renames | `YELLOW_STAINED_GLASS_PANE` | `§e` |
| Green / lime | go · confirm · success | `LIME_STAINED_GLASS_PANE` | `§a` |
| Purple / magenta | special · magic · rare | `PURPLE_STAINED_GLASS_PANE` | `§d` |
| Gray | disabled · neutral · background | `GRAY_STAINED_GLASS_PANE` | `§7`/`§8` |

Rules:
- **The frame announces the mode.** Switching the Bulk Dashboard to Delete turns the
  whole frame red before the player reads a single word. That instant is the design.
- Never mix meanings (no red frame on a safe screen "because it looks cool").
- The action button matches the frame: red frame → `TNT`/red text apply; calm frame →
  `LIME_DYE` apply. Green is always safe to click.

## 2. Two-tone "picture frame"

A flat border is fine. A border whose **four corners are a darker shade of the same
hue** reads as a real frame — it's one line of code and the single cheapest "expensive
look" we have. Use `ChestMenu.frame(edge, corner)`:

| Mode | Edge | Corner |
|---|---|---|
| calm / edit | light blue | blue |
| danger | red | black |
| caution / rename | yellow | orange |

The chest **title** gets the darker shade of the same hue (`§3`/`§4`/`§6`) — the title
bar sits on light parchment, dark text reads crisp there.

## 3. The grid (6-row chest, 54 slots)

```
 0  .  .  .  HDR .  .  .  8     ← frame + header at slot 4 (glinted, titled, explains the screen)
 9  .  .  .  .   .  .  . 17     ← frame sides, breathing room
18 ① »  ②  »  ③  »  ④  . 26    ← STEP ROW: choices at 19/21/23/25, » connectors between
27  .  ⑤  .  PRV .  MTC . 35    ← results row: preview 31, matched list 33 (overflow ⑤ at 29)
36  .  .  .  GO! .  .  . 44     ← ACTION ROW: the one big button at 40, alone, centre
45 BCK .  .  TWIN .  .  CLS 53  ← nav: Back 45 · extras 48-50 · Close 53
```

- **One tile = one job.** Never two actions on one tile (left/right-click *cycling* the
  same choice is fine — that's one job).
- **Empty space is a feature.** A tile with no neighbours gets looked at. Crowded rows
  get skimmed. The Apply button sits alone on its row for exactly this reason.
- **Steps go left→right on one row**, numbered `① ② ③ ④` in the tile names, with quiet
  `§8»` connector panes between them walking the eye forward.

## 4. Tiles that explain themselves

- **Header (slot 4):** glinted, icon = the screen's job, name = screen title, lore =
  one-line purpose + live count + the step hint (`§8① op ② filter ③ details ✔ apply`).
- **Choice tiles show every option**, radio-style, current one marked:
  ```
  §e▸ §f§lglow        ← current: arrow + white bold
  §8• hardness        ← others: dark gray + bullet
  ```
  Long lists (the 17 sounds) collapse to one line: `§8wool §7← §e▸ §f§lgravel §7→ §8snow`
  plus `§8choice 9 of 17`. Players should never wonder "what happens if I keep clicking".
- **Disabled tiles exist and say why.** Never hide a button — show `GRAY_DYE`, gray
  name, and the reason (`§8No blocks match` / `§7Change the filter first`). Clicking it
  plays the deny sound. A vanished button is a mystery; a gray one is an instruction.
- **Danger buttons are dressed as danger:** `TNT`, `§4§l⚠`, the count in the name
  (`⚠ Delete 12 block(s)`), the escape hatch in the lore (`/cb undo restores them`).
- **Glint = "this one is selected/important".** One or two glints per screen, max.
  Glint everything and you've glinted nothing.

## 5. Icon vocabulary

Same concept → same item, on every screen:

| Item | Means |
|---|---|
| `HOPPER` | filter / funnel down |
| `SPYGLASS` | inspect / preview the result set |
| `TNT` | delete / destructive |
| `NAME_TAG` | rename / naming |
| `COMPARATOR` | settings / modes |
| `PAPER` | summary / preview text |
| `WRITABLE_BOOK` | "click to type" (anvil input) |
| `LIME_DYE` / `GRAY_DYE` | go / disabled |
| `ARROW` / `BARRIER` | back / close |
| `BOOKSHELF` | "everything" |
| `NETHER_STAR` | favourites |
| `CHAIN` | locked |
| coloured `WOOL` (cycled) | categories — each one a different colour at a glance |
| `CHAIN_COMMAND_BLOCK` | the command twin (see §8) |

## 6. Writing on tiles

- **Names:** colour-coded bold for interactive (`§e§l① Operation`), the live value in
  white (`§f`). Gray non-bold for inert tiles.
- **Lore:** `§7` body — one short line of what it does. `§8` fine print — limits, tips.
  `§e` action hint — ALWAYS the last line: `§eClick §7to choose`,
  `§eLeft§7/§eright-click §7to cycle`. Blank lore lines between blocks of meaning.
- Glyphs that carry weight: `✔ ✖ ⚠ ▸ • » ↩ …` — use them; they're free emotion.
- Counts everywhere: `(3)`, `Matches now: §e12`, `…and 240 more`. Numbers feel alive;
  vagueness feels broken.

## 7. Sound — every click answers

Silent GUIs feel dead. The palette (in `GuiFx`, recycled from the old mod):

| Moment | Sound | Vol / pitch | Feel |
|---|---|---|---|
| menu opens from a command | book page turn | 0.7 / 1.0 | "we're in" |
| click / cycle | amethyst chime | 0.6 / 1.25 | bright tap |
| picked something | amethyst chime | 0.7 / 1.0 | lower = "set" |
| apply (safe) | XP orb | 0.8 / 1.05 | small win |
| apply (destructive) | note-block bass | 1.0 / 0.55 | gut warning |
| click a disabled tile | note-block bass | 0.8 / 0.8 | short "nope" |

Rules: high pitch = small/positive, low pitch = serious/negative. Destructive actions
must NEVER share the success sound. Keep volumes under 1.0 except danger.
(`BLOCK_NOTE_BLOCK_*` needs `.value()` — NFR-12; everything else is bare.)

## 8. Show consequences before commitment

The happiest players are the ones who were never surprised:
- **Live match count** on the header and filter tile, recomputed every click.
- **Matched tile** (spyglass): the first 8 actual ids + `…and N more`. Proof, not promise.
- **Preview tile** (paper): the change in plain words — `Will set glow = 8 on 12 block(s)`.
- **Command twin** (chain command block, slot 49): the exact chat command this screen
  equals. Teaches the fast path for free, builds trust that the GUI hides nothing.
- Big/destructive batches still confirm in chat with clickable `[✔] [✖]` — the GUI
  never lowers the safety bar the commands set.

## 9. Navigation contract

- **Back (45, arrow) goes back — never closes.** No history (screen opened straight
  from a command)? It goes **home to the Main Menu**. Dead-end Back buttons feel broken.
- **Close (53, barrier) closes. Always.** Both corners, every screen, same two items.
- Page changes and value cycles re-render **in place** (same title + size = no cursor
  snap). Different screens open fresh.
- ESC must never lose work — selections live in a per-player session (`BulkSession`),
  not in the screen.

## 10. New-menu checklist

```
[ ] Frame: two-tone, colour = the screen's meaning (§1, §2)
[ ] Title: darker shade of the frame colour
[ ] Header at 4: glint, icon, purpose line, live count
[ ] Steps numbered ①②③, left→right, » connectors
[ ] Every choice tile lists its options with ▸ on the current one
[ ] Action button alone on its row, dressed in its colour, count in the name
[ ] Disabled state: gray dye + reason + deny sound (never hidden)
[ ] Live counts + matched sample + preview before any commit
[ ] GuiFx on every interaction (open/click/select/apply/danger/deny)
[ ] Back at 45 (never closes), Close at 53
[ ] Icons from §5, lore in §6's voice, ≤2 glints
```

*Files: `gui/chest/Icons.java` (stacks) · `ChestMenu.frame` (frames) · `GuiFx` (sounds) ·
`BulkPropertyMenu` (the reference implementation).*
