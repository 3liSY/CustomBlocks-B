# Group 22 — Permissions System

**🎯 Active:** None  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                    |
| --------------- | ------------------ |
| **Verdict**     | 🧊 parked — no code |
| **Progress**    | — · 0 / 0          |
| **Last tested** | —                  |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)

## 🗺️ Sections

| § | What                                                                         | State    |
| --- | ---------------------------------------------------------------------------- | -------- |
| A | Permissions system — LuckPerms + Fabric Permissions API, vanilla OP fallback | 🧊 parked |
| B | Migrated admin commands (permission gates on `/cb rp`, `/cb sync`)           | ⏳ planned |

🔗 **Related Doc:** [GROUP_22_PERMISSIONS.md](../groups/GROUP_22_PERMISSIONS.md)

---

# 🎯 Test now

*(All active tests passed or parked for owner build)*

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

**A · Permissions system (Deferred)**

| § | What it'll do                                                                                       | Spec                    |
| --- | --------------------------------------------------------------------------------------------------- | ----------------------- |
| A | LuckPerms + Fabric Permissions API; vanilla OP fallback; per-tier command gates; 8 tier rulings TBD | GROUP_22_PERMISSIONS.md |

*Test rows written after build.*

**B · Migrated admin commands (Planned)**

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | `/cb rp pause` as non-OP | permission denied | ➖ | ⏳ |
| B2 | `/cb rp pause` as OP | pack regeneration pauses | ⏳ | ⏳ |
| B3 | `/cb sync` as OP | a 3-second on-screen lag warning appears before the pack is pushed | ⏳ | ⏳ |

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

*(none)*

</details>

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---
