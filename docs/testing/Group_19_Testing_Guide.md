# Group 19 — Showcase & Hologram Display

**🎯 Active:** None  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                        |
| --------------- | -------------------------------------- |
| **Verdict**     | ⏳ not built — design locked 2026-06-21 |
| **Progress**    | — · 0 / 0                              |
| **Last tested** | —                                      |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-10): showcase config UI (§B) is Screen-based, not chest GUI, per the mod-wide
> Screen migration (`GROUP_19_DISPLAY.md` audit note). Nothing in G19 is built yet, so this is a clean
> Screen target with no legacy chest code to migrate.

## 🗺️ Sections

| § | What                                                   | State     |
| --- | ------------------------------------------------------ | --------- |
| A | Showcase core — place · spin · persist · remove        | ⏳ planned |
| B | Config GUI — Appearance / Motion / Display tabs        | ⏳ planned |
| C | Multi-block · vanilla · animated · placer              | ⏳ planned |
| D | Presets + cap                                          | ⏳ planned |
| E | Management — list · teleport · locate · rename · clone | ⏳ planned |
| F | Bulk / group — bulk apply · hide/show · lock · groups  | ⏳ planned |
| G | Hologram preview (`/cb preview`)                       | ⏳ planned |
| H | Offhand projection                                     | ⏳ planned |

🔗 **Related Doc:** [GROUP_19_DISPLAY.md](../groups/GROUP_19_DISPLAY.md)

---

# 🎯 Test now

*(All active tests passed or parked for owner build)*

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

> Build order is A → H. Each section goes 🎯 → ✅ only after owner confirms in-game.

**A · Showcase core (S1)**
| # | Action                                      | Expected Result                                   | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb showcase g19a`                         | `g19a` floats above a pedestal, spinning smoothly | 🎯 | 🎯 |
| A2 | Config → type = floating                    | block spins in mid-air, no base                   | 🎯 | 🎯 |
| A3 | Look up at sky → `/cb showcase g19a`        | spawns ~2 blocks in front of head; no error       | 🎯 | 🎯 |
| A4 | `/cb showcase remove` (looking at showcase) | showcase gone                                     | 🎯 | 🎯 |
| A5 | Restart server                              | showcase still there, still spinning              | 🎯 | 🎯 |

**B · Config GUI (S2)**
| # | Action                                             | Expected Result                           | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Shift-right-click showcase                         | multi-tab Screen opens                    | 🎯 | 🎯 |
| B2 | Motion tab → static                                | block stops spinning                      | 🎯 | 🎯 |
| B3 | Appearance → glow / hover bob / giant scale / aura | each visibly applies                      | 🎯 | 🎯 |
| B4 | Enable label, edit it                              | name floats above; edit shows custom text | 🎯 | 🎯 |

**C · Multi-block / vanilla / animated / placer (S3)**
| # | Action                                        | Expected Result                    | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | `/cb showcase diamond_sword`                  | diamond sword displays (item form) | 🎯 | 🎯 |
| C2 | Add `g19a` + `g19b`, right-click showcase     | cycles between them                | 🎯 | 🎯 |
| C3 | Set auto-cycle 0.5s then 0.1s                 | flips on its own; faster at 0.1s   | 🎯 | 🎯 |
| C4 | Content = "all custom blocks"                 | cycles all; a new block joins      | 🎯 | 🎯 |
| C5 | `/cb showcase item` (or Tools tab) → place it | becomes a showcase                 | 🎯 | 🎯 |

**D · Presets + cap (S4)**
| # | Action                                    | Expected Result   | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Save a look → apply elsewhere             | styles match      | 🎯 | 🎯 |
| D2 | Set default for new showcases; exceed cap | cap warning fires | 🎯 | 🎯 |

**E · Management (S5)**
| # | Action                            | Expected Result                      | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | `/cb showcase list` → click entry | teleported to that showcase          | 🎯 | 🎯 |
| E2 | Locate command                    | nearby showcases glow-highlight      | 🎯 | 🎯 |
| E3 | Rename + clone                    | clone matches config                 | 🎯 | 🎯 |
| E4 | Move + `/cb showcase editnearest` | moves one; edit-nearest hits closest | 🎯 | 🎯 |

**F · Bulk / group (S6)**
| # | Action                                                   | Expected Result                | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Bulk apply preset / bulk remove in radius                | applies / removes all in range | 🎯 | 🎯 |
| F2 | Hide → still saved; show → reappears; lock blocks config | each works                     | 🎯 | 🎯 |
| F3 | Assign group → change group setting                      | all in group update            | 🎯 | 🎯 |

**G · Hologram preview (S7)**
| # | Action                           | Expected Result                      | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | `/cb preview g19a`               | spinning preview for 10s, then gone  | 🎯 | 🎯 |
| G2 | `/cb preview <url>`              | works; bad url = clean error         | 🎯 | 🎯 |
| G3 | Walk >5 blocks away from preview | early despawn                        | 🎯 | 🎯 |
| G4 | `pin` · `unpin`                  | pin stays; unpin removes             | 🎯 | 🎯 |
| G5 | `/cb preview g19a @<player>`     | player sees it; side-by-side compare | 🎯 | 🎯 |

**H · Offhand projection (S8)**
| # | Action                 | Expected Result                                  | SP | MP |
| --- | --- | --- | --- | --- |
| H1 | Hold `g19a` in offhand | hologram floats above head; other players see it | 🎯 | 🎯 |
| H2 | Empty offhand          | hologram gone instantly                          | 🎯 | 🎯 |

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

*(No sections fully passed yet)*

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

