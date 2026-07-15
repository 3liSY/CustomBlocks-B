# Group 12 — Export Dashboard & Marketplace

**🎯 Active:** B (0/8) · C (0/4) · A (0/4)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                                    |
| --------------- | -------------------------------------------------- |
| **Verdict**     | ❗ blocked — download-link fix not built            |
| **Progress**    | 🟡🟡🟡💔❗❗❗❗🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/16 passed)                  |
| **Last tested** | 2026-06-21                                          |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-10): Export Dashboard + Marketplace are Screen-based per the mod-wide Screen
> migration (`GROUP_12_EXPORT_MARKETPLACE.md` audit note). Dashboard is already built/confirmed as a chest
> GUI (A1) — owner confirmed 2026-07-10: convert to Screen (default applies, no exception). Real rework,
> scheduled as ⏳ planned. Marketplace (§D) is unbuilt and targets Screen directly.
> Screen spec moved 2026-07-12 → `GROUP_27_SCREENS.md` §G27.30.

## 🗺️ Sections

| § | What                                                | State      |
| --- | ----------------------------------------------------- | ---------- |
| B | Download-link delivery (cross-cutting IP/host leak)  | ❗ blocked |
| C | Vault share-code (G12.7)                             | 🟡 built, config-gated, untested |
| A | Export Dashboard core (G12.1, G12.6)                 | 🎯 (2 rows need testing) |
| D | Marketplace `/cb market` (G12.8)                     | ⏳ not built |
| E | Advanced export formats (litematic/schem/vanilla RP) | ⏳ not built |

🔗 **Related Doc:** [GROUP_12_EXPORT_MARKETPLACE.md](../groups/GROUP_12_EXPORT_MARKETPLACE.md)

---

# 🎯 Test now

### 🧰 Setup Required
* `/cb create g12a ExportTestBlock1 https://i.imgur.com/example.png`
* `/cb create g12b ExportTestBlock2`
* `/cb setcategory g12a exporttest` · `/cb setcategory g12b exporttest`

**B · Download-link delivery**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Export g12a → Download JSON | `[download]` link works, no host leaked | ❗ | ❌ (2026-06-21, `ERR_CONNECTION_REFUSED` + leaked `yoyoo.mcsh.io`) |
| B2 | Export g12a → Download PNG | same | ❗ | 🟡 (2026-06-21, export runs, link bad) |
| B3 | Export All → ZIP | same | ❗ | 🟡 (2026-06-21) |
| B4 | Export Category → ZIP | same | ❗ | 🟡 (2026-06-21) |

> Root cause confirmed in source: `ResourcePackServer.getPngUrl/getZipUrl/getExportUrl` still
> interpolate the configurable `httpHost` (default `127.0.0.1` — safe but useless for remote
> players; leaks the host if pointed at a public address, as this test server was). Fix decided
> 2026-06-25 (owner): host-local link stays `127.0.0.1`/`localhost`, remote share goes through the
> Cloud Vault worker URL instead. **Not built yet** — logged as `BUG-G12-001` below.

**C · Vault share-code**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | g12a export screen → Upload to Vault (`CloudCommands.uploadBlock`) | ZIP uploads via `VaultBlockCodec`, share code returned in chat | 🎯 | 🎯 |
| C2 | `/cb delete g12a` → `/cb importblock <code>` (`BlueprintCommands.importByCode` → `CloudCommands.downloadBlock`) | g12a re-created from the code; on id clash opens `VaultConflictScreen` | 🎯 | 🎯 |

> Code is complete (`CloudCommands.java`, `BlueprintCommands.java`) — never confirmed in-game.
> Blocked in practice by `vaultEndpoint` being empty by default (`CustomBlocksConfig.java:88`);
> `cloudReady()` refuses with "cloud vault isn't set up yet" until it's set. **Config/deployment
> gate, not a missing-code gate** — the group doc's "deferred, needs cloud vault deployed" framing
> undersold how much of this already works. Retest for real once `vaultEndpoint` points at a live
> `cb-cloud-vault` worker.

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

**A · Export Dashboard core**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb export` | Dashboard opens (currently a chest GUI; target = Screen): Per Block / Per Category / All Blocks / Bulk Choose | 🎯 | 🎯 (was ✅ 2026-06-21 — reverted, stale, needs retest; original note: confirmed in `ExportDashboardMenu.buildScopeSelection`; layout is hand-pick "Bulk Choose", not the per-slot-click the spec described — spec corrected) |
| A2 | g12a export screen → Generate Blueprint | Blueprint item given (`BlueprintCommands.exportBlock`), tooltip carries schema-v1 JSON | 🎯 | 🎯 (was ✅ 2026-06-21 — reverted, stale, needs retest; original note: works; hand-to-hand `/cb importblock` confirmed in `BlueprintCommands.importFromHand`; only useful same-server, rethink flagged, see ❔ below) |

</details>

<details><summary>⏳ <b>Planned / Not built</b> (click to open)</summary>

| § | What                                                                       | Test when built |
| --- | ----------------------------------------------------------------------- | ---------------- |
| D | `/cb market` Marketplace Screen — browse/import shared blocks from any player | grepped src/ for `market`/`marketplace` command — no match, needs writing from scratch (Screen-based), then cb-cloud-vault Worker deployed |
| E | `.litematic` / `.schem` / standalone vanilla resource pack export         | grepped src/ — zero code; heavy binary formats, deferred |
| — | NBT export format (spec'd, never implemented)                             | `BlockExporter.isSupported()` has no `nbt` case, no dashboard tile; real formats shipped are json/txt/csv/md/html/yaml/png/zip — add nbt or drop from spec |

</details>

<details><summary>❔ <b>A2 — Blueprint rethink</b> (click to open)</summary>

Blueprint generates and imports fine hand-to-hand, but only useful trading on the same server.
Needs a decision: cross-server trade? offline import? Revisit before further polish.

</details>

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID      | Test Row | Issue Description | Status |
| ----------- | -------- | ------------------ | ------ |
| B1–B4       | B1–B4    | Chat `[download]` links leak the server host (when `httpHost` is set to a public address) and hit `ERR_CONNECTION_REFUSED`. Fix decided 2026-06-25 (host-local link stays `127.0.0.1`, remote share via Cloud Vault worker URL) — design only, not built. | Open |

---

