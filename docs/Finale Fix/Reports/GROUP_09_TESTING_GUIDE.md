# 🧪 Group 09 — Backup & Data Safety — Testing

> 🟢 Build green = it compiles.   ✋ Only you, in-game, can say it works.
> 📦 Jar: `build/libs/customblocks-1.0.0.jar` (copy to your server's mods folder yourself)
> ⚠️ This group is being built in **careful slices** — backups first (safe, read-only), then the
> riskier restore/migration pieces. Only the slices below are built; the rest are ⏳ not yet.

**Legend:**  🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🗺️ At a glance

| | What | Where |
|:--:|---|:--:|
| ✅ **CONFIRMED** | **load** (S2) · **auto-backup** (S3) · **trash** (S4) · **broken-blocks + safety** (S5) · **config gate + tile** | §2–§5, §CFG |
| 🟡 **PARTIAL** | **Advanced backup GUI** (`/cb backup` no args) — works, being polished | §GUI |
| 🎯 **TEST NOW** | **Broken-blocks bulk actions** (select / fix-selected / delete-selected) · **`/cb safety` dashboard GUI** | §5 |
| ⏳ Coming | first-boot migration + move data to `data/` | §6 |

> 🔁 **Renamed:** `/cb backup restore <name>` is now **`/cb backup load <name>`** ✅. The old `restore` word
> still works as a hidden alias, so nothing breaks if you type it out of habit.

---

# 🎯 Test now

## §1 · Backup save + list  *(Slice 1)*

> 💡 **What it does:** `/cb backup save` copies your current blocks (slot data + textures + sources +
> config) into `config/customblocks/backups/<name>/`. It only **reads** your live data — it can't
> change or break anything. `/cb backup list` shows what you've saved.

> 🧰 **Before you start:**
> - `/cb create g09a BackupTest1`
> - `/cb setglow g09a 8`
> - `/cb create g09b BackupTest2`

**Try these:**

- **① Save a named backup** — `/cb backup save pre-test`
  - ✅ **Pass:** chat says `Backup "pre-test" saved. (N block(s), config, textures)`.
  - Check the folder `config/customblocks/backups/pre-test/` exists, with `slots.json`, `manifest.json`,
    a `textures/` folder, etc.
  - ❌ **Broken if:** error, or no folder created.
- **② Save with no name** — `/cb backup save`
  - ✅ **Pass:** a backup named `backup-YYYYMMDD-HHMMSS` is created.
- **③ Duplicate name is refused** — `/cb backup save pre-test` again
  - ✅ **Pass:** it refuses with "already exists" — it does **not** overwrite the first one.
- **④ List backups** — `/cb backup list`
  - ✅ **Pass:** both backups show, newest first, with a timestamp + block count.
- **⑤ Bad name is refused** — `/cb backup save my/bad name`
  - ✅ **Pass:** refused with a "use letters, numbers, - or _" message (no folder made).

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ① | named backup saves, folder + manifest written |
| ⬜ | ② | auto-naming works |
| ⬜ | ③ | won't clobber an existing backup |
| ⬜ | ④ | list shows metadata, newest first |
| ⬜ | ⑤ | invalid names rejected (no path tricks) |

> ↩️ **Undo the test:** delete the test folders under `config/customblocks/backups/` by hand for now
> (`/cb backup delete` arrives in the next slice).

---

## §2 · Load · delete · panic · recover  *(Slice 2 — 🔴 these change live data)*

> 💡 **What it does:** `/cb backup load <name>` replaces your current blocks with a saved backup.
> **Before it overwrites anything it auto-saves your current state** as a `pre-restore-…` backup, so a
> restore is itself undoable. It asks for `/cb confirm` first. `panic` skips the confirm for emergencies;
> `/cb recover` restores the newest backup (with confirm); `delete` removes a backup (never touches live).

> 🧰 **Before you start:** have the `pre-test` backup from §1, and `g09a` (glow 8) + `g09b` placed.

**Try these:**

- **⑥ Load needs confirm** — `/cb delete g09a`, then `/cb backup load pre-test`
  - ✅ **Pass:** chat warns it will replace current blocks + save a safety copy, and waits.
- **⑦ Confirm it** — `/cb confirm`
  - ✅ **Pass:** `Restored from "pre-test" — N block(s) now…`; `g09a` is back (glow 8); accept the pack prompt.
  It also names a `pre-restore-…` safety backup.
- **⑧ Safety copy exists** — `/cb backup list`
  - ✅ **Pass:** a `pre-restore-YYYYMMDD-HHMMSS §8(auto)` backup is listed (your pre-restore state).
- **⑨ Cancel works** — `/cb backup load pre-test`, then `/cb cancel`
  - ✅ **Pass:** nothing is restored.
- **⑩ Recover (newest)** — `/cb recover` → `/cb confirm`
  - ✅ **Pass:** restores the newest backup (asks to confirm first).
- **⑪ Panic (no confirm)** — `/cb backup panic`
  - ✅ **Pass:** immediately restores the newest backup, no confirm step. (Saves a safety copy first.)
- **⑫ Delete a backup** — `/cb backup delete pre-test`
  - ✅ **Pass:** `Deleted backup "pre-test"`; it's gone from `/cb backup list`. Your live blocks are untouched.
- **⑬ 🔧 Survives restart** — `/cb backup save restart-test`, `/stop`, restart, `/cb delete g09b`,
  `/cb backup load restart-test` → `/cb confirm`
  - ✅ **Pass:** `g09b` comes back after a full restart.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ⑥ | restore is confirm-gated |
| ⬜ | ⑦ | restore brings the block back with its attributes |
| ⬜ | ⑧ | a safety copy is auto-saved before overwriting |
| ⬜ | ⑨ | cancel aborts a restore |
| ⬜ | ⑩ | recover restores the newest (with confirm) |
| ⬜ | ⑪ | panic restores immediately (no confirm) |
| ⬜ | ⑫ | delete removes a backup, live data untouched |
| ⬜ | ⑬ | backups survive a server restart |

> ↩️ **Undo the test:** the safety `pre-restore-…` backups let you roll back any load. Clean up extra
> backups with `/cb backup delete <name>`.

---

## §GUI · Advanced backup GUI  *(🟡 PARTIAL — works, being polished)*  *(`/cb backup` with no args)*

> 🟡 **Status:** the developer has this working but wants it polished further — treat it as partial, not
> signed off. Re-test after the next polish pass.

> 💡 **What it does:** running `/cb backup` with nothing after it now opens a chest GUI instead of just
> printing text. Each backup is a tile (newest first). **Left-click ticks** a backup for bulk delete;
> **right-click loads** it (with a Yes/No confirm). The bottom row has Create, Select-all, and Delete-
> selected. Everything reuses the same tested save/load/delete as the chat commands.

> 🧰 **Before you start:** have a couple of backups saved (e.g. `/cb backup save gtest1`, `/cb backup save gtest2`).

**Try these:**

- **G① Open the GUI** — `/cb backup` (no args)
  - ✅ **Pass:** a chest titled `Backups · N saved` opens, listing your backups newest-first. (Console still
    prints the text list.)
- **G② Create from the GUI** — click **Create new backup** → keep or edit the suggested name → take the
  renamed tag out of the anvil.
  - ✅ **Pass:** chat says `Backup "…" saved`, and the GUI reopens with the new backup now in the list.
- **G③ Load from the GUI** — **right-click** a backup → on the confirm screen click **Yes — load it**.
  - ✅ **Pass:** it loads that backup (same as `/cb backup load`), saves a `pre-restore-…` safety copy, and
    rebuilds the pack. **No.** on that screen changes nothing.
- **G④ Bulk delete** — **left-click** two or three backups (they get a ✔ + glow), then click **Delete N
  selected** → **Yes**.
  - ✅ **Pass:** only the ticked backups are gone from the list; everything else (and your live blocks) is
    untouched. **Select all backups** then clearing the selection also works.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | G① | `/cb backup` opens the GUI (console still gets text) |
| ⬜ | G② | Create button saves a backup + the list refreshes |
| ⬜ | G③ | Right-click → load works with a confirm |
| ⬜ | G④ | Tick + Delete-selected removes just those, live data safe |

---

## §3 · Auto-backup + prune  *(Slice 3)*

> 💡 **What it does:** the server quietly saves an `auto-YYYYMMDD-HHMMSS` backup every
> `autoBackupInterval` minutes (default **30**), and keeps only the newest `autoBackupKeepCount` (default
> **10**) auto-backups — older `auto-…` ones are deleted automatically. Your manual saves and the
> `pre-restore-…` safety copies are **never** pruned.

**Try these:**

- **⑭ Auto-backup fires** — edit `config/customblocks/config.json`, set `"autoBackupInterval": 2`, restart
  the server, then wait ~2 minutes and run `/cb backup list` (or open `/cb backup`).
  - ✅ **Pass:** an `auto-YYYYMMDD-HHMMSS §8(auto)` backup appears with no command from you. The log shows
    `[CustomBlocks] Auto-backup "auto-…" saved (N block(s)); pruned X old auto-backup(s).`
  - ↩️ Set `autoBackupInterval` back to `30` (or `0` to turn it off) when done.
- **⑮ Prune keeps only the newest N** *(optional, faster with a small keep count)* — set
  `"autoBackupKeepCount": 3` and `"autoBackupInterval": 1`, restart, wait ~4–5 min, `/cb backup list`.
  - ✅ **Pass:** never more than **3** `auto-…` backups exist; your named saves stay put.
- **⑯ Disable works** — set `"autoBackupInterval": 0`, restart, wait.
  - ✅ **Pass:** no new `auto-…` backups are ever created.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ⑭ | timed auto-backup fires with no command |
| ⬜ | ⑮ | old auto-backups are pruned to the keep count (manual saves untouched) |
| ⬜ | ⑯ | `autoBackupInterval: 0` disables it |

---

## §CFG · Config gate + auto-backup tile  *(new)*

> 💡 **What changed:** opening the config screen now asks "are you sure" first, and auto-backup is
> adjustable right in the config GUI (no more editing `config.json` by hand for it).

**Try these:**

- **C① Config asks first** — `/cb config` (or the dashboard → Config button).
  - ✅ **Pass:** a Yes/No "Open server config?" screen appears. **Yes** opens the config screen; **No** backs out.
- **C② Auto-backup tile** — in the config screen, find **Auto-Backup** (barrel icon).
  - ✅ **Pass:** **left-click** cycles the interval (off→5→15→30→60→120→360 min); **right-click** cycles how
    many to keep (3→5→10→20→50). The tile text updates each click and chat confirms.
  - ↩️ Set it back to every 30 min (or off) when done.

---

## §4 · Deleted-block trash  *(Slice 4 — 🟡 restore recreates live blocks)*

> 💡 **What it does:** deleting a custom block now drops a copy into a **trash** (`/cb deletedblocks`, alias
> `/cb trash`) so you can bring it back. Each entry can be **Restored**, **Pinned** (never auto-deleted), or
> **Deleted forever**. Old unpinned entries auto-clear after `trashRetentionDays` (default 30; 0 = keep forever).

> 🧰 **Before you start:** `/cb create g09t TrashTest` · `/cb setglow g09t 10` (give it a texture too if you like).

**Try these:**

- **⑰ Delete lands in the trash** — `/cb delete g09t`, then `/cb deletedblocks`.
  - ✅ **Pass:** a GUI opens with `g09t` listed (name, deleted time, "texture: saved/none").
- **⑱ Restore** — click `g09t` → **Restore this block**.
  - ✅ **Pass:** chat says restored; `/cb list` shows `g09t` again **with its glow 10 + texture**; it's gone
    from the trash. Accept the pack prompt if asked.
- **⑲ Restore is refused if the id exists** — `/cb create g09t Dupe`, `/cb delete g09t`, recreate `/cb create
  g09t Again`, then try to restore the trashed one.
  - ✅ **Pass:** a clear "a block named g09t already exists" message — no crash, nothing overwritten.
- **⑳ Pin protects from pruning** — open an entry → **Pin**. (It shows a ★ and "never auto-pruned".)
  - ✅ **Pass:** the entry shows pinned; it won't be removed by the retention prune.
- **㉑ Delete forever** — open an entry → **Delete forever** → **Yes**.
  - ✅ **Pass:** it's gone from the trash for good; your live blocks are untouched.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ⑰ | deleting a block captures it to the trash |
| ⬜ | ⑱ | restore brings the block back with texture + attributes |
| ⬜ | ⑲ | restore refuses cleanly when the id is taken |
| ⬜ | ⑳ | pin marks an entry as never-auto-pruned |
| ⬜ | ㉑ | delete-forever removes it permanently, live data safe |

> ⚠️ **Known limit:** per-face textures (from the face editor) are **not** captured — a restored face-edited
> block comes back with its main texture only. Tell me if you need faces preserved too.

---

## §5 · Broken blocks + safety check  *(Slice 5 — 🟢 read-only, fix re-bakes from a saved image)*

> 💡 **What it does:** `/cb showbrokenblocks` finds blocks whose texture file is missing (they'd show up
> purple) and offers a one-click fix — rebuilding the texture from the block's **saved image** (no internet
> needed). `/cb safety` prints a quick health summary of your data.

> 🧰 **Before you start:** have a URL-made block (so it has a saved image), e.g. `/cb create g09b <url>` —
> note its slot number from `/cb list` or the editor.

**Try these:**

- **㉒ Safety summary** — `/cb safety`
  - ✅ **Pass:** chat lists blocks used/max, backup count + newest, auto-backup status, trash count, and a
    broken-block count (§a0 ✔ when none).
- **㉓ Detect a broken block** — close the game/server, delete the file
  `config/customblocks/textures/slot_<N>.png` for that block, restart, then `/cb showbrokenblocks`.
  - ✅ **Pass:** a GUI opens with that block as a **red wool** tile saying "Missing baked texture", "Saved
    image: yes".
- **㉔ One-click fix** — click the red tile.
  - ✅ **Pass:** chat says it rebuilt the texture from the saved image; the block looks right again; the
    report empties. (Accept the pack prompt if asked.)
- **㉕ No-source block** *(optional)* — for a block with no saved image (e.g. an Arabic/text block),
  **right-click** the tile — it pre-fills `/cb retexture <id>` in chat instead of rebuilding.
  - ✅ **Pass:** right-click offers the retexture command rather than a rebuild.

> 🆕 **Now with bulk actions + a dashboard:**
> - **㉖ Safety dashboard** — `/cb safety` opens a **GUI** (not chat) with a health line and clickable tiles
>   (Blocks · Backups · Auto-Backup · Trash · Broken Blocks) + a "Save a backup now" button. Each tile jumps
>   to that screen. ✅ **Pass:** the dashboard opens and the tiles navigate correctly.
> - **㉗ Multi-fix** — in `/cb showbrokenblocks`, **left-click** several tiles to tick them, then **Fix
>   selected** → ✅ they all rebuild from their saved images in one go.
> - **㉘ Bulk delete** — tick some, **Delete selected** → confirm → ✅ they're removed and show up in
>   `/cb deletedblocks` (recoverable). **Right-click** still fixes a single block.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ㉒ | `/cb safety` opens the dashboard GUI (console gets the summary) |
| ⬜ | ㉓ | a missing-texture block is detected and listed |
| ⬜ | ㉔ | right-click rebuild from the saved image fixes one |
| ⬜ | ㉕ | no-source blocks route to /cb retexture instead |
| ⬜ | ㉖ | dashboard tiles navigate to each screen |
| ⬜ | ㉗ | tick + Fix-selected batch-rebuilds many |
| ⬜ | ㉘ | tick + Delete-selected removes many (to trash) |

---

# ⏳ Coming next  *(not built — nothing to test yet)*

| Slice | Feature | Risk |
|:--:|---|:--:|
| 6 | first-boot migration + move data to `data/` | 🔴 touches live persistence |

---

## 🆘 If a test fails, send me
- 🔢 the step number
- 👀 what happened vs what you expected (📸 a screenshot helps)
- 📄 the last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup
`/cb delete g09a` · `/cb delete g09b` · then delete the test backup folders by hand.
