/**
 * PanelSession.java — Group 31 (BuzzerGame) Phase 1 items 2-3-5-6.
 *
 * The single source of truth for one admin panel's round (design lock: one central session per panel,
 * NOT mutual UUID cross-refs). Owned + persisted by {@link AdminPanelBlockEntity}; linked buzzers store
 * only this session's {@code sessionId} and route their presses back here via {@link #onBuzz}.
 *
 * Responsibilities:
 *   - state machine: IDLE → COUNTDOWN → RUNNING → RESULTS → FINISHED (+ reset/stop).
 *   - round clock: {@link #tick()} runs the 3-2-1 countdown then counts up in ticks (20/s = hundredths).
 *   - Precision Stop (default): on buzz, freeze the buzzer's stopped time; winner = closest to target.
 *   - Reaction Race: 3-2-1-GO, first buzz after GO wins; a pre-GO buzz obeys the false-start rule.
 *   - reveal drama: the raw time is frozen on buzz but hidden until {@link #reveal()} (host-triggered).
 * All world/packet effects (titles, sounds, action-bar to nearby players) live in {@link RoundBroadcast};
 * this class stays world-free and unit-testable and only describes what happened via {@link RoundBeat}.
 *
 * Depends on: SessionState, GameMode, RoundConfig, BuzzResult, RoundBeat, FalseStartRule
 * Called by:  AdminPanelBlockEntity (owns + ticks + persists), BuzzerBlock (onBuzz), BuzzerGameCommands
 */
package com.customblocks.buzzergame;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PanelSession {

    /** Result of a control action: whether it happened, plus a message to show the host. */
    public record Result(boolean ok, String message) {}

    /** Extra ticks folded into a Reaction Race buzz when the player false-started under the PENALTY rule. */
    private static final int PENALTY_TICKS = 20; // +1.00s

    // --- identity / links / config (all persisted) ---
    private UUID sessionId;
    private SessionState state = SessionState.IDLE;
    private GameMode mode = GameMode.SOLO;
    private final Set<UUID> buzzers = new LinkedHashSet<>();
    private final Set<UUID> screens = new LinkedHashSet<>();
    private final RoundConfig config = new RoundConfig();

    // --- live round runtime (transient; results are persisted so a reload can still reveal) ---
    private int countdownTicksLeft = 0;
    private int lastCountShown = -1;
    private long elapsedTicks = 0;
    private final Set<UUID> falseStarted = new HashSet<>();
    private final LinkedHashMap<UUID, BuzzResult> results = new LinkedHashMap<>();
    private String revealTitle = "";
    private String revealSubtitle = "";
    private final List<String> revealLines = new ArrayList<>(); // one per rank + "Disqualified: …" (item 5)

    public PanelSession() {
        this.sessionId = UUID.randomUUID();
    }

    public UUID sessionId() { return sessionId; }
    public SessionState state() { return state; }
    public GameMode mode() { return mode; }
    public RoundConfig config() { return config; }
    public int buzzerCount() { return buzzers.size(); }
    public int screenCount() { return screens.size(); }
    public String revealTitle() { return revealTitle; }
    public String revealSubtitle() { return revealSubtitle; }
    public List<String> revealLines() { return revealLines; }

    /** Live enough to want the panel's server ticker running / re-broadcasting each tick. */
    public boolean isBroadcasting() {
        return state == SessionState.COUNTDOWN || state == SessionState.RUNNING || state == SessionState.RESULTS;
    }

    /** Accepting buzzer presses right now (running, or the countdown window for false-start detection). */
    public boolean isBuzzAccepting() {
        return state == SessionState.COUNTDOWN || state == SessionState.RUNNING;
    }

    // ------------------------------------------------------------------ linking (item 3)

    public void linkBuzzer(UUID id)   { if (id != null) buzzers.add(id); }
    public void unlinkBuzzer(UUID id) { buzzers.remove(id); }
    public void linkScreen(UUID id)   { if (id != null) screens.add(id); }
    public void unlinkScreen(UUID id) { screens.remove(id); }

    // ------------------------------------------------------------------ config verbs (IDLE only)

    /** Change mode — only allowed while IDLE, so a running round can't have its rules pulled out. */
    public Result setMode(GameMode newMode) {
        if (newMode == null) return new Result(false, "Unknown mode.");
        Result guard = idleGuard();
        if (guard != null) return guard;
        this.mode = newMode;
        return new Result(true, "Mode set to " + newMode.label() + " (needs " + newMode.minBuzzers() + "+ buzzers).");
    }

    public Result setGameFormat(boolean reaction) {
        Result guard = idleGuard();
        if (guard != null) return guard;
        config.reactionRace = reaction;
        return new Result(true, "Format: " + (reaction ? "Reaction Race" : "Precision Stop") + ".");
    }

    public Result setTargetSeconds(double seconds) {
        Result guard = idleGuard();
        if (guard != null) return guard;
        config.targetTicks = Math.max(1, (int) Math.round(seconds * 20.0));
        return new Result(true, "Target set to " + fmtTicks(config.targetTicks) + ".");
    }

    public Result setCountdown(int seconds) {
        Result guard = idleGuard();
        if (guard != null) return guard;
        if (seconds <= 0) {
            config.countdownEnabled = false;
            return new Result(true, "Countdown turned off.");
        }
        config.countdownEnabled = true;
        config.countdownSecs = Math.min(10, seconds);
        return new Result(true, "Countdown set to " + config.countdownSecs + "s.");
    }

    public Result setFalseStart(FalseStartRule rule) {
        if (rule == null) return new Result(false, "Unknown false-start rule.");
        Result guard = idleGuard();
        if (guard != null) return guard;
        config.falseStart = rule;
        return new Result(true, "False-start rule: " + rule.label() + ".");
    }

    private Result idleGuard() {
        if (state != SessionState.IDLE) {
            return new Result(false, "Change settings/mode only while Idle — reset the round first.");
        }
        return null;
    }

    // ------------------------------------------------------------------ GUI helpers (item 4)
    // The admin-Screen cycle buttons + snapshot live in PanelGui (kept out to hold this class under the
    // §9.3 500-line cap). Only the index-based unlink needs the private buzzer set, so it stays here.

    /**
     * Remove the idx-th (1-based) linked buzzer (IDLE only). The session is the source of truth, so the
     * buzzer immediately stops counting toward this panel; its block clears its own stale link on break.
     */
    public Result unlinkBuzzerByIndex(int idx) {
        Result guard = idleGuard();
        if (guard != null) return guard;
        if (idx < 1 || idx > buzzers.size()) return new Result(false, "No buzzer #" + idx + " linked.");
        UUID target = null;
        int i = 1;
        for (UUID u : buzzers) { if (i == idx) { target = u; break; } i++; }
        if (target != null) buzzers.remove(target);
        return new Result(true, "Unlinked buzzer #" + idx + " (" + buzzers.size() + " left).");
    }

    // ------------------------------------------------------------------ round lifecycle (items 5-6)

    /**
     * Validated round start. Must be IDLE with enough linked buzzers for the mode. Clears the previous
     * round, then either opens the 3-2-1 countdown or (countdown off) goes straight to RUNNING.
     */
    public Result tryStart() {
        if (state != SessionState.IDLE) {
            return new Result(false, "A round is already in progress (" + state.label() + ") — reset first.");
        }
        int need = mode.minBuzzers();
        int have = buzzers.size();
        if (have < need) {
            return new Result(false, "Can't start " + mode.label() + " — needs " + need
                    + " buzzer(s), " + have + " linked. Link buzzers with the wand first.");
        }
        clearRound();
        if (config.countdownEnabled) {
            state = SessionState.COUNTDOWN;
            countdownTicksLeft = config.countdownSecs * 20;
            lastCountShown = -1;
        } else {
            state = SessionState.RUNNING;
            elapsedTicks = 0;
        }
        String fmt = config.reactionRace ? "Reaction Race" : "Precision Stop";
        return new Result(true, "Round started — " + fmt
                + (config.reactionRace ? "" : " (target " + fmtTicks(config.targetTicks) + ")") + ".");
    }

    /**
     * Advance the round one tick. Runs the countdown then the count-up clock; returns the beat (title/sound)
     * for {@link RoundBroadcast} to show nearby players. No-op outside COUNTDOWN/RUNNING.
     */
    public RoundBeat tick() {
        switch (state) {
            case COUNTDOWN -> {
                countdownTicksLeft--;
                if (countdownTicksLeft <= 0) {
                    state = SessionState.RUNNING;
                    elapsedTicks = 0;
                    return RoundBeat.go();
                }
                int secLeft = (countdownTicksLeft + 19) / 20; // ceil to whole seconds
                if (secLeft != lastCountShown) {
                    lastCountShown = secLeft;
                    return RoundBeat.count(secLeft);
                }
                return RoundBeat.NONE;
            }
            case RUNNING -> {
                elapsedTicks++;
                return RoundBeat.NONE;
            }
            default -> {
                return RoundBeat.NONE;
            }
        }
    }

    /**
     * A linked buzzer was pressed. Records/rejects per format + state, then (once every buzzer has resolved)
     * freezes the round at RESULTS for the host to reveal. Never announces the winner here — that's reveal().
     */
    public Result onBuzz(UUID buzzerId, String playerName) {
        if (buzzerId == null || !buzzers.contains(buzzerId)) {
            return new Result(false, "This buzzer isn't linked to this panel.");
        }
        String name = playerName == null || playerName.isBlank() ? "Player" : playerName;

        if (state == SessionState.COUNTDOWN) {
            if (!config.reactionRace) {
                return new Result(false, "Wait for GO — the round starts after the countdown.");
            }
            return handleFalseStart(buzzerId, name);
        }
        if (state == SessionState.RUNNING) {
            BuzzResult existing = results.get(buzzerId);
            if (existing != null && existing.resolved()) {
                return new Result(false, "You already buzzed this round.");
            }
            boolean penalised = falseStarted.contains(buzzerId);
            long recorded = elapsedTicks + (config.reactionRace && penalised ? PENALTY_TICKS : 0L);
            results.put(buzzerId, BuzzResult.timed(name, recorded, penalised));
            maybeFinishBuzzing();
            String msg = "Buzzed! Time locked in" + (penalised ? " (+penalty)" : "") + " — waiting for the host to reveal.";
            return new Result(true, msg);
        }
        return new Result(false, "No round is running right now.");
    }

    private Result handleFalseStart(UUID buzzerId, String name) {
        return switch (config.falseStart) {
            case DISQUALIFY -> {
                results.put(buzzerId, BuzzResult.disqualified(name));
                maybeFinishBuzzing();
                yield new Result(false, "False start — disqualified for this round!");
            }
            case PENALTY -> {
                falseStarted.add(buzzerId);
                yield new Result(false, "Too early! A time penalty will apply — now wait for GO and buzz again.");
            }
            case IGNORE -> new Result(false, "Too early! Press ignored — wait for GO.");
        };
    }

    /** Once every linked buzzer has resolved, freeze the round at RESULTS (host reveals from there). */
    private void maybeFinishBuzzing() {
        int resolved = 0;
        for (BuzzResult r : results.values()) if (r.resolved()) resolved++;
        if (resolved >= buzzers.size() && !buzzers.isEmpty()) {
            state = SessionState.RESULTS;
        }
    }

    /**
     * Host-triggered reveal: pick the winner from the frozen results (closest-to-target for Precision Stop,
     * fastest reaction for Reaction Race), publish the reveal title/subtitle, and move to FINISHED. Only
     * valid from RESULTS.
     */
    public Result reveal() {
        if (state != SessionState.RESULTS) {
            return new Result(false, "Nothing to reveal yet — no round has finished buzzing.");
        }
        List<BuzzResult> ranked = new ArrayList<>();
        List<BuzzResult> disqualified = new ArrayList<>();
        for (BuzzResult r : results.values()) {
            if (r.dq()) disqualified.add(r);
            else if (r.recorded()) ranked.add(r);
        }
        ranked.sort(Comparator.comparingLong(this::metric));
        state = SessionState.FINISHED;
        revealLines.clear();

        if (ranked.isEmpty()) {
            revealTitle = "No valid result";
            revealSubtitle = disqualified.isEmpty() ? "No one buzzed" : "Everyone false-started";
            for (BuzzResult d : disqualified) revealLines.add("Disqualified: " + d.name());
            return new Result(true, "Revealed — no valid result.");
        }

        BuzzResult winner = ranked.get(0);
        revealTitle = winner.name() + " wins!";
        revealSubtitle = ranked.size() >= 2 ? "2nd: " + ranked.get(1).name() : soloDetail(winner);

        for (int i = 0; i < ranked.size(); i++) {
            revealLines.add(ordinal(i + 1) + ": " + ranked.get(i).name() + " — " + rankDetail(ranked.get(i)));
        }
        for (BuzzResult d : disqualified) revealLines.add("Disqualified: " + d.name());
        return new Result(true, revealTitle + " | " + revealSubtitle);
    }

    /** The sort key: closest-to-target (Precision) or fastest reaction (Reaction). Lower is better. */
    private long metric(BuzzResult r) {
        return config.reactionRace ? r.ticks() : Math.abs(r.ticks() - config.targetTicks);
    }

    /** Solo / single-entry subtitle: the full detail line (kept from the single-winner build). */
    private String soloDetail(BuzzResult w) {
        return config.reactionRace
                ? "Reaction " + fmtTicks(w.ticks()) + (w.falseStart() ? " (with penalty)" : "")
                : "Stopped " + fmtTicks(w.ticks()) + " - target " + fmtTicks(config.targetTicks)
                        + " - off by " + fmtTicks(Math.abs(w.ticks() - config.targetTicks));
    }

    /** Per-rank chat detail (Reaction time, or Precision time + how far off target). */
    private String rankDetail(BuzzResult r) {
        return config.reactionRace
                ? fmtTicks(r.ticks()) + (r.falseStart() ? " (+penalty)" : "")
                : fmtTicks(r.ticks()) + " (off " + fmtTicks(Math.abs(r.ticks() - config.targetTicks)) + ")";
    }

    /** 1 → "1st", 2 → "2nd", 3 → "3rd", 4 → "4th", … */
    private static String ordinal(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) return n + "th";
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }

    /** Debug stepper (kept from item 2): walk the state chain by hand, bypassing the buzzer check. */
    public SessionState advance() {
        state = switch (state) {
            case IDLE -> SessionState.COUNTDOWN;
            case COUNTDOWN -> SessionState.RUNNING;
            case RUNNING -> SessionState.RESULTS;
            case RESULTS -> SessionState.FINISHED;
            case FINISHED -> SessionState.FINISHED;
        };
        if (state == SessionState.RUNNING) elapsedTicks = 0;
        return state;
    }

    /** Stop an in-progress round → FINISHED (no-op if already IDLE/FINISHED). */
    public Result stop() {
        if (state == SessionState.IDLE || state == SessionState.FINISHED) {
            return new Result(false, "No round to stop (" + state.label() + ").");
        }
        state = SessionState.FINISHED;
        return new Result(true, "Round stopped — " + state.label() + ".");
    }

    /** Reset back to a clean IDLE session (keeps links + config; clears round progress). */
    public Result reset() {
        state = SessionState.IDLE;
        clearRound();
        return new Result(true, "Session reset — " + state.label() + ".");
    }

    private void clearRound() {
        countdownTicksLeft = 0;
        lastCountShown = -1;
        elapsedTicks = 0;
        falseStarted.clear();
        results.clear();
        revealTitle = "";
        revealSubtitle = "";
        revealLines.clear();
    }

    // ------------------------------------------------------------------ display

    /** The live action-bar line for nearby players (empty = nothing to show). */
    public String displayLine() {
        return switch (state) {
            case COUNTDOWN -> "Get ready... " + Math.max(1, (countdownTicksLeft + 19) / 20);
            case RUNNING -> config.reactionRace
                    ? "GO!  " + fmtTicks(elapsedTicks)
                    : "Time " + fmtTicks(elapsedTicks) + "   (target " + fmtTicks(config.targetTicks) + ")";
            case RESULTS -> "Buzzed! Host: /cb buzzergame reveal";
            default -> "";
        };
    }

    /** Ticks → seconds with hundredths, e.g. 100 → "5.00s". */
    public static String fmtTicks(long ticks) {
        return String.format(Locale.ROOT, "%.2fs", ticks / 20.0);
    }

    /** Bare number for the physical timer stand (no "s" suffix): live time while running, else "0.00". */
    public String screenText() {
        return switch (state) {
            case RUNNING, RESULTS, FINISHED -> String.format(Locale.ROOT, "%.2f", elapsedTicks / 20.0);
            default -> "0.00";
        };
    }

    // ------------------------------------------------------------------ NBT

    public void writeNbt(NbtCompound nbt) {
        nbt.putUuid("sessionId", sessionId);
        nbt.putString("state", state.name());
        nbt.putString("mode", mode.name());
        nbt.put("buzzers", uuidList(buzzers));
        nbt.put("screens", uuidList(screens));
        NbtCompound cfg = new NbtCompound();
        config.writeNbt(cfg);
        nbt.put("config", cfg);
        nbt.put("results", resultList());
    }

    public void readNbt(NbtCompound nbt) {
        if (nbt.containsUuid("sessionId")) sessionId = nbt.getUuid("sessionId");
        state = parseEnum(nbt.getString("state"), SessionState.values(), SessionState.IDLE);
        mode = parseEnum(nbt.getString("mode"), GameMode.values(), GameMode.SOLO);
        readUuidList(nbt, "buzzers", buzzers);
        readUuidList(nbt, "screens", screens);
        if (nbt.contains("config")) config.readNbt(nbt.getCompound("config"));
        readResults(nbt);
    }

    private static NbtList uuidList(Set<UUID> ids) {
        NbtList list = new NbtList();
        for (UUID id : ids) {
            NbtCompound c = new NbtCompound();
            c.putUuid("id", id);
            list.add(c);
        }
        return list;
    }

    private static void readUuidList(NbtCompound nbt, String key, Set<UUID> into) {
        into.clear();
        if (!nbt.contains(key, NbtElement.LIST_TYPE)) return;
        NbtList list = nbt.getList(key, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            if (c.containsUuid("id")) into.add(c.getUuid("id"));
        }
    }

    private NbtList resultList() {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, BuzzResult> e : results.entrySet()) {
            BuzzResult r = e.getValue();
            NbtCompound c = new NbtCompound();
            c.putUuid("id", e.getKey());
            c.putString("name", r.name());
            c.putLong("ticks", r.ticks());
            c.putBoolean("recorded", r.recorded());
            c.putBoolean("falseStart", r.falseStart());
            c.putBoolean("dq", r.dq());
            list.add(c);
        }
        return list;
    }

    private void readResults(NbtCompound nbt) {
        results.clear();
        if (!nbt.contains("results", NbtElement.LIST_TYPE)) return;
        NbtList list = nbt.getList("results", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            if (!c.containsUuid("id")) continue;
            results.put(c.getUuid("id"), new BuzzResult(
                    c.getString("name"), c.getLong("ticks"),
                    c.getBoolean("recorded"), c.getBoolean("falseStart"), c.getBoolean("dq")));
        }
    }

    private static <E extends Enum<E>> E parseEnum(String name, E[] values, E fallback) {
        if (name != null && !name.isBlank()) {
            for (E v : values) if (v.name().equals(name)) return v;
        }
        return fallback;
    }
}
