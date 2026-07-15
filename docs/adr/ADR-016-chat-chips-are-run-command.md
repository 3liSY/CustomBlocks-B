# ADR-016 — Chat chips are `RUN_COMMAND` of real commands, not a token store

**Status:** Accepted (2026-07-14)
**Groups:** 04 (§G04-4)
**Supersedes:** the token-store / 7-day-TTL / disk-persisted click-target design drafted the same morning

---

## Context

Group 04 makes chat interactive again: a message can carry chips — `▶ Run`, `✎ Edit`, `↩ Undo`, `⊙ View`,
`⇪ Share`, and an inline `✔ Yes / ✖ No` confirm.

The obvious design is a **token store**: give each chip an opaque token, keep a server-side map of
token → {action, target, actor}, and have the chip run a hidden `/cb click <token>`. That is what we
first specified, and it dragged three more decisions behind it:

- tokens must **expire** (a 7-day TTL was chosen),
- tokens must be **persisted to disk**, or every chip in scrollback dies on restart,
- tokens must be **bound to the actor**, so only the person who triggered the line can click it.

## Decision

**A chip is nothing but `ClickEvent.RUN_COMMAND` of a real `/cb` command with a plain argument.**

| Chip | Runs |
|---|---|
| ▶ Run | the command on that line |
| ✎ Edit | `/cb edit <id>` → opens the block editor Screen |
| ↩ Undo | `/cb undo` — the existing command, the existing stack |
| ⊙ View | the item's existing screen command |
| ⇪ Share | `COPY_TO_CLIPBOARD` of a Group 20 vault code (the one clipboard chip) |
| ✔ Yes | the destructive command itself, immediately |

There is no token store, no `/cb click`, no TTL, and no persistence.

Vanilla `ClickEvent` cannot open a custom `Screen` directly — but it doesn't need to. The screen-opening
commands already ride the existing server→client `OpenGuiPayload`, so `/cb edit <id>` opens the editor
exactly the same way typing it does.

## Consequences

Deleting the token store deleted the three problems it created:

- **Scrollback never expires.** A chip clicked a month later still works, because it was only ever a
  command. There is nothing to persist and nothing to garbage-collect.
- **Staleness resolves itself.** The chip does not need to know whether its target still exists — the
  command validates on click and answers honestly ("Nothing to undo", "There's no block called …").
  Chips never need to grey themselves out, so we don't need live polling to grey them.
- **"Actor-only" becomes "runs-as-clicker".** The command runs as whoever clicks it, against *their own*
  per-player state. Player B clicking `↩ Undo` on player A's line undoes **B's** last action. That needs
  no per-line UUID binding — and is the behaviour you actually want.

The honest cost: **`⇪ Share` cannot play a sound cue.** `COPY_TO_CLIPBOARD` runs no command, so the server
never learns the chip was clicked. The other four chips fire their cue from the command that does the
work — which also means clicking `↩ Undo` and typing `/cb undo` sound identical, as they should.

A second real constraint stays visible: a chip's target is a plain command argument, so anything a chip
wants to do must be expressible as a command. That is a feature — it means every chip is also a thing a
player can type, and there is no hidden second API to keep in sync.
