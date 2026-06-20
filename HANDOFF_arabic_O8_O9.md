# Handoff — Group 13 Arabic: keep designing O8 (Hide) + O9 (auto-build), then build

**Date:** 2026-06-19. **Repo:** `Coding/CustomBlocks-B/` (active, git repo on `main`). Build = JDK 21.

## YOUR FIRST MOVE: KEEP QUESTIONING (don't code yet)
The dev is actively designing two features by answering small multiple-choice questions, and explicitly
said **"keep asking."** Continue the brainstorm the SAME way: use the **AskUserQuestion** tool in SMALL
batches (2–4 concrete, plain-language options each), one topic at a time. Lock the open design questions
below FIRST, then propose a build. Do not write code until the design is locked and the dev says go.

## Dev profile (read this — it drives everything)
- Non-coder, server owner, aesthetics-obsessed. **Overwhelmed FAST** — said "overwhelmation" twice.
- SHORT, plain replies. Concrete clickable options >> open questions. When they show overwhelm: REDUCE
  scope, reassure ("nothing's being built, you can stop anytime"), don't pile on.
- They re-energize when handed cool concrete choices. Keep small calm batches but keep momentum.
- Stop firing the question tool the moment they ask you to write things down / hand off.

## What shipped this session (DONE — confirmed in-game + documented)
- **Isolated floater fix**: short isolated Arabic letters drew too high → two-tier downward nudge in
  `ArabicTileRenderer.render()` (ISOLATED form only). Short no-descender floaters drop `ISO_DROP = 9%`;
  tall alef drops `ISO_TALL_DROP = 3%`; tailed/bowled + all connected letters untouched. Shape-driven
  detector (descent≈0 → floater; height<62% → short). Confirmed in-game. TESTING GUIDE **§12**. Also
  **resolves §11** (stuck-isolated) — no separate hand-art shrink needed.
- **Whole Group 13 🎯 queue confirmed**: §1 preview · §5 auto-join+direction · §6 names/labels/HUD ·
  §7 6-face · §9 full-bright · §10 flash+all-colours join · §12 floaters. **No built-but-unconfirmed
  items remain.**
- Jar built + deployed: `customblocks-1.0.0.jar` (7.82 MB) → `.minecraft/mods/`.
- **NOT committed** (dev's call). Tree is dirty on `main` (dozens of unrelated files). The float-fix
  touched: `arabic/ArabicTileRenderer.java`, `client/render/ArabicLetterItemRenderer.java`,
  `tools/render_preview/BeforeAfterPreview.java`, + 3 untracked files
  (`block/ArabicLetterBlockEntity.java`, `client/render/ArabicLetterBlockEntityRenderer.java`,
  `client/render/ArabicLetterItemRenderer.java`), + the 3 docs. Dev wants a clean commit of ONLY these
  later, maybe on a new branch — **ASK before committing.**

## The two features to finish designing (NOT built — decisions locked so far)

### O8 — Hide / manage the 224 bundled letters (Hide, NOT hard delete)
LOCKED: per-block **Hide** button in the browser · **Hide all on page** (current search/filter) · master
**Hide all** · hidden letters move to a **Hidden tab** to reopen · un-hide **one-by-one** AND **un-hide
all** · always recoverable (hide = visibility only; jar masters never destroyed) · scope = one colour
variant (e.g. `Jeem Red` independent of other colours). DEFERRED: hard delete, custom-colour recolor.
**Open qs to ask next:** browser layout (where the Hide button sits; how the Hidden tab opens) ·
confirm-prompt on master "Hide all"? · does Hide also apply to the auto-join blocks or only the 224
bundled? · should hidden letters also drop out of `/cb arabic give`/search, or just the browser?

### O9 — Type-a-word, auto-build
LOCKED: trigger from **both** a GUI text box (open screen → type → Build) AND a command
(`/cb arabic build <word>`) · blocks **placed in a row in front of the dev**, correct auto-join forms,
**right-to-left** (reuses O3 joining + facing) · dev **picks a colour before building** (4 set + custom
via Color Studio path). **Open qs to ask next:** spaces/numbers/non-letters in a word (gap vs skip) ·
collision with blocks already at the target row (stop / overwrite / shift) · how far in front + which
direction it builds · whole-word undo · max word length.

Full design: `docs/Finale Fix/GROUP_13_ARABIC.md` → **O8** + **O9**. Status rows: TESTING GUIDE **§13/§14**.

## Build + deploy (the Java trap — important)
PATH `java` is **Java 8**, and a trailing-backslash `JAVA_HOME` breaks the bash `./gradlew` (it silently
falls back to Java 8). Build via **PowerShell + gradlew.bat** with a clean JAVA_HOME:
```
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"; & "C:\Users\66664\OneDrive\Desktop\Coding\CustomBlocks-B\gradlew.bat" -p "C:\Users\66664\OneDrive\Desktop\Coding\CustomBlocks-B" build -x test --no-daemon
```
Deploy:
```
Copy-Item -Force "C:\Users\66664\OneDrive\Desktop\Coding\CustomBlocks-B\build\libs\customblocks-1.0.0.jar" "C:\Users\66664\AppData\Roaming\.minecraft\mods\customblocks-1.0.0.jar"
```

## Preview harness (decide visuals BEFORE building — dev rule)
`tools/render_preview/` — standalone, JDK 21, mirrors runtime math. `BeforeAfterPreview.java` has
`isoDrop()` + emits BEFORE_AFTER / EDGE_CASES / WORD_EDGES PNGs. Compile/run:
```
"$JAVA_HOME/bin/javac" -encoding UTF-8 -d tools/render_preview src/main/java/com/customblocks/arabic/ArabicJoining.java tools/render_preview/RenderPreview.java tools/render_preview/BeforeAfterPreview.java
"$JAVA_HOME/bin/java" -cp tools/render_preview BeforeAfterPreview
```
Show PNGs via `Start-Process`; ONE simple image, dev approves before any jar.

## Key files
- `arabic/ArabicTileRenderer.java` — tile draw + the float nudge (`ISO_DROP`/`ISO_TALL_DROP`)
- `arabic/ArabicJoining.java` — joining brain (isolated/initial/medial/final)
- `arabic/ArabicBlockRegistry.java` — the 224 bundled letters (slots → `config/customblocks/arabic_registry.json`) — **O8 touches this**
- `client/render/ArabicLetterBlockEntityRenderer.java` + `ArabicLetterItemRenderer.java` — 6-face glyph cube + item icon
- `gui/screens/ArabicBrowserScreen.java`, `gui/chest/ArabicListMenu.java` / `ArabicGroupMenu.java` / `ArabicHubMenu.java` — the browser — **O8 Hide UI goes here**
- `command/handlers/ArabicCommands.java` — `/cb arabic …` — **O9 `build` command here**
- Docs: `GROUP_13_ARABIC.md` (design, O8/O9), `Reports/GROUP_13_TESTING_GUIDE.md` (status), `PROGRESS_LOG.md`

## Standing rules (dev's)
- Every jar build → update TESTING GUIDE (status/checkmarks) + PROGRESS_LOG (why/how). Group spec stays
  clean (design only). Testing guide is LEAN, ordered 🎯→✅→🟡→⏳.
- Discuss-before-build for design items. Preview visuals first. Hand off green; nothing is DONE until the
  dev confirms in-game.
