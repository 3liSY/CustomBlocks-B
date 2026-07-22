/**
 * BuzzerSession.java — Group 31 (BuzzerGame) item 1 (wand-owned session rebuild, 2026-07-18).
 *
 * The single source of truth for one host's round. Replaces the scrapped panel-block {@code PanelSession}:
 * the session now belongs to a host PLAYER (via the wand) and lives in memory, owned by
 * {@link BuzzerSessionManager} and keyed by the host's UUID — it is NOT persisted and dies when the host
 * logs off (design lock 2026-07-18). Linked buzzers / timer stands store only this session's
 * {@code sessionId} and route their presses / digit reads back through the manager.
 *
 * Responsibilities (round logic ported unchanged from the old panel build — "assumed sound"):
 *   - state machine: IDLE → COUNTDOWN → RUNNING → RESULTS → FINISHED (+ reset/stop).
 *   - round clock: {@link #tick()} runs the 3-2-1 countdown then counts up in ticks (20/s = hundredths).
 *   - Precision Stop (default): on buzz, freeze the buzzer's stopped time; winner = closest to target.
 *   - Reaction Race: 3-2-1-GO, first buzz after GO wins; a pre-GO buzz obeys the false-start rule.
 *   - reveal drama: the raw time is frozen on buzz but hidden until {@link #reveal()} (host-triggered).
 * Links are stored as {id → BlockPos} so logout/break cleanup and the near-game broadcast can reach the
 * real blocks (the old panel knew none of its buzzers' positions). All world/packet effects live in
 * {@link RoundBroadcast}; this class stays world-free and unit-testable and only describes what happened
 * via {@link RoundBeat}.
 *
 * Depends on: SessionState, GameMode, RoundConfig, BuzzResult, RoundBeat, FalseStartRule
 * Called by:  BuzzerSessionManager (owns + ticks), BuzzerBlock (onBuzz), BuzzerGameWand (link),
 *             BuzzerGameCommands (control), TimerDisplayBlockEntity (screenText)
 */
package com.customblocks.buzzergame;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class BuzzerSession {

    /** Result of a control action: whether it happened, plus a message to show the host. */
    public record Result(boolean ok, String message) {}

    /** Which beat of the solo press cycle a buzzer press produced (drives the §G press FX + feedback). */
    public enum BuzzPhase { START, STOP, RESET, IGNORED }

    /** The outcome of a solo buzzer press: the cycle phase + a hotbar message for the presser. */
    public record BuzzOutcome(BuzzPhase phase, String message) {}

    /** Extra ticks folded into a Reaction Race buzz when the player false-started under the PENALTY rule. */
    private static final int PENALTY_TICKS = 20; // +1.00s

    // --- identity / owner / links / config ---
    private final UUID sessionId;
    private final UUID hostId;
    private SessionState state = SessionState.IDLE;
    private GameMode mode = GameMode.SOLO;
    private final LinkedHashMap<UUID, BlockPos> buzzers = new LinkedHashMap<>();  // buzzerId → block pos
    private final LinkedHashMap<UUID, BlockPos> displays = new LinkedHashMap<>(); // displayId → block pos
    private final RoundConfig config = new RoundConfig();

    // --- live round runtime (all in-memory; the session itself never persists) ---
    private int countdownTicksLeft = 0;
    private int lastCountShown = -1;
    private long elapsedTicks = 0;
    /** True while the live run was started by a bare buzzer press (no هدف armed) — a plain stopwatch (I3).
     *  Drives {@link #hasTarget()} (no target line) and the press-3 landing state (IDLE, not ARMED). */
    private boolean targetless = false;
    private final Set<UUID> falseStarted = new HashSet<>();
    private final LinkedHashMap<UUID, BuzzResult> results = new LinkedHashMap<>();
    private String revealTitle = "";
    private String revealSubtitle = "";
    private final List<String> revealLines = new ArrayList<>(); // one per rank + "Disqualified: …"

    public BuzzerSession(UUID hostId) {
        this.hostId = hostId;
        this.sessionId = UUID.randomUUID();
    }

    public UUID sessionId() { return sessionId; }
    public UUID hostId() { return hostId; }
    public SessionState state() { return state; }
    public GameMode mode() { return mode; }
    public RoundConfig config() { return config; }
    public int buzzerCount() { return buzzers.size(); }
    public int displayCount() { return displays.size(); }
    public String revealTitle() { return revealTitle; }
    public String revealSubtitle() { return revealSubtitle; }
    public List<String> revealLines() { return revealLines; }

    /** Solo: the clock only advances while RUNNING, so that is the only state the manager needs to tick. */
    public boolean isBroadcasting() {
        return state == SessionState.RUNNING;
    }

    /** Solo: a buzzer press is meaningful from a bare IDLE (target-less start, I3) through the whole
     *  ARMED → RUNNING → frozen(RESULTS) cycle. */
    public boolean isBuzzAccepting() {
        return state == SessionState.IDLE || state == SessionState.ARMED
                || state == SessionState.RUNNING || state == SessionState.RESULTS;
    }

    // ------------------------------------------------------------------ linking (item 1)

    public void linkBuzzer(UUID id, BlockPos pos)   { if (id != null) buzzers.put(id, pos == null ? null : pos.toImmutable()); }
    public void unlinkBuzzer(UUID id)               { buzzers.remove(id); }
    public void linkDisplay(UUID id, BlockPos pos)  { if (id != null) displays.put(id, pos == null ? null : pos.toImmutable()); }
    public void unlinkDisplay(UUID id)              { displays.remove(id); }

    public boolean hasBuzzer(UUID id)  { return buzzers.containsKey(id); }
    public boolean hasDisplay(UUID id) { return displays.containsKey(id); }

    /** Every linked buzzer + display position (for the near-game broadcast + logout cleanup). */
    public List<BlockPos> linkedPositions() {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : buzzers.values())  if (p != null) out.add(p);
        for (BlockPos p : displays.values()) if (p != null) out.add(p);
        return out;
    }

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

    // ------------------------------------------------------------------ solo stopwatch (item D)

    /**
     * Arm a solo target (seconds already validated to 0.5–60s by the command). Moves to ARMED: the screen
     * shows الهدف + a resting النتيجة "0.00", the clock idle. Re-arming from any state is allowed (a fresh
     * target simply resets the clock). The buzzer press cycle drives it from here — see {@link #onBuzz}.
     */
    public Result armTarget(double seconds) {
        config.targetTicks = Math.max(1, (int) Math.round(seconds * 20.0));
        mode = GameMode.SOLO;
        state = SessionState.ARMED;
        elapsedTicks = 0;
        targetless = false; // an armed run shows the هدف line + returns to ARMED on press 3
        results.clear();
        return new Result(true, "Armed — target " + fmtTicks(config.targetTicks) + ". Press the buzzer to start.");
    }

    /**
     * Solo buzzer press cycle, instant (no countdown, no auto-stop). Two entry points share one cycle:
     *   IDLE    → press 1: START — target-less plain stopwatch (no هدف line), النتيجة climbs from 0.00 (I3).
     *   ARMED   → press 1: START — a هدف is armed via {@code start}; النتيجة climbs from 0.00.
     *   RUNNING → press 2: STOP  — النتيجة freezes at the stopped value (state RESULTS).
     *   RESULTS → press 3: RESET — النتيجة clears to 0.00; re-arms to ARMED (armed run) or back to IDLE
     *             (target-less run — no هدف to keep).
     * Returns the phase so {@link BuzzerBlock} can fire the matching §G sound/particle combo.
     */
    public BuzzOutcome onBuzz(UUID buzzerId, String playerName) {
        if (buzzerId == null || !buzzers.containsKey(buzzerId)) {
            return new BuzzOutcome(BuzzPhase.IGNORED, "This buzzer isn't linked to a session.");
        }
        switch (state) {
            case IDLE -> {
                state = SessionState.RUNNING;
                elapsedTicks = 0;
                targetless = true; // bare press → plain stopwatch, press 3 returns to IDLE
                return new BuzzOutcome(BuzzPhase.START, "Go!");
            }
            case ARMED -> {
                state = SessionState.RUNNING;
                elapsedTicks = 0;
                return new BuzzOutcome(BuzzPhase.START, "Go!");
            }
            case RUNNING -> {
                state = SessionState.RESULTS; // frozen — the clock stops advancing here
                return new BuzzOutcome(BuzzPhase.STOP, "Stopped at " + fmtTicks(elapsedTicks) + ".");
            }
            case RESULTS -> {
                elapsedTicks = 0;
                if (targetless) {
                    state = SessionState.IDLE; // no هدف armed → back to a single resting 0.00
                    return new BuzzOutcome(BuzzPhase.RESET, "Reset — press to start again.");
                }
                state = SessionState.ARMED;    // keep the armed هدف for another attempt
                return new BuzzOutcome(BuzzPhase.RESET, "Reset — press to start again.");
            }
            default -> {
                return new BuzzOutcome(BuzzPhase.IGNORED, "Arm a target first: /cb buzzergame start <seconds>.");
            }
        }
    }

    // ------------------------------------------------------------------ round lifecycle (parked multiplayer)

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
     * PARKED (multiplayer) — a linked buzzer was pressed under the ranked-reveal formats. Records/rejects per
     * format + state, then (once every buzzer has resolved) freezes the round at RESULTS for the host to
     * reveal. Unreachable in the solo flow (the solo cycle is {@link #onBuzz}); kept for the deferred H pass.
     */
    public Result onBuzzMultiplayer(UUID buzzerId, String playerName) {
        if (buzzerId == null || !buzzers.containsKey(buzzerId)) {
            return new Result(false, "This buzzer isn't linked to a session.");
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
        targetless = false;
        falseStarted.clear();
        results.clear();
        revealTitle = "";
        revealSubtitle = "";
        revealLines.clear();
    }

    // ------------------------------------------------------------------ display (solo stopwatch screen)

    /** Ticks → seconds with hundredths + an "s" suffix, e.g. 100 → "5.00s" (chat/hotbar messages). */
    public static String fmtTicks(long ticks) {
        return String.format(Locale.ROOT, "%.2fs", ticks / 20.0);
    }

    /** Whether a هدف is armed (الهدف shown). A target-less bare-press run (I3) and IDLE/FINISHED show none —
     *  just a single resting النتيجة 0.00 above the climbing number. */
    public boolean hasTarget() {
        if (targetless) return false;
        return state == SessionState.ARMED || state == SessionState.RUNNING || state == SessionState.RESULTS;
    }

    /** The الهدف target number for the screen (bare, no "s"). */
    public String targetText() {
        return fmtNum(config.targetTicks);
    }

    /** The النتيجة result number for the screen: climbing while RUNNING, frozen at RESULTS, else 0.00. */
    public String resultText() {
        return switch (state) {
            case RUNNING, RESULTS -> fmtNum(elapsedTicks);
            default -> "0.00";
        };
    }

    /** Bare stopwatch number: hundredths, one integer digit under 10s ("0.00"–"9.99"), two at 10s+
     *  ("10.00"…) — the design's 0.00 → 00.00 switch, which %.2f already produces. Dot, never colon. */
    private static String fmtNum(long ticks) {
        return String.format(Locale.ROOT, "%.2f", ticks / 20.0);
    }
}
