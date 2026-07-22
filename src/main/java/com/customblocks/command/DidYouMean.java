/**
 * DidYouMean.java
 *
 * Responsibility: Typo correction for unknown /cb subcommands (Group 04). A greedy
 * catch-all branch is appended AFTER every real literal; Brigadier always prefers
 * literals, so this only fires when nothing else matched. The closest known subcommand
 * is offered as a CLICKABLE CHIP — it never auto-executes.
 *
 * The pitfall fix: the fallback argument is named "subcommand" — never an internal
 * label like "unknown_cb_tail" — so vanilla usage errors can't leak a raw arg name.
 *
 * G04-2 rewrite (2026-07-14) — two bugs, two fixes:
 *
 *   1. WRONG CANDIDATES. Candidates came from HelpTopics, a hand-maintained list that had drifted
 *      from the real command tree (67 of 119 commands missing, including `animation`). They now come
 *      from {@link CommandTree}, i.e. Brigadier's actually-registered literals, so `/cb anim` can
 *      finally see `animation` — and no future command can go missing.
 *
 *   2. CONFIDENT GARBAGE. The old matcher scored `eff * 1000 + candidate.length()`, which is a
 *      tie-break that rewards SHORT words. With `animation` absent, `/cb anim` matched the nearest
 *      short word in range and cheerfully proposed `/cb gui` — a command with nothing in common.
 *      An absolute distance cap (≤3 in `always` mode) let unrelated words through: 3 edits is
 *      nothing between two 3-letter words and everything between two 12-letter ones. Distance is
 *      now NORMALISED by length, tiered (prefix beats substring beats fuzzy), and the shortest-word
 *      tie-break is gone. Nothing close → we say we don't know, rather than inventing an answer.
 *
 * Mode: always smart — confident hits only, never garbage. Not editable (owner-locked 2026-07-18);
 * the old smart/always/off config switch was removed, so there is one behaviour and no way to weaken it.
 *
 * Depends on: CommandTree (the real registered literals), Chat
 * Called by: CommandRegistrar (appendFallback, registered last)
 */
package com.customblocks.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DidYouMean {

    /**
     * Suggestion-only aliases for HARD-renamed commands (owner-locked 2026-07-15). The old name is
     * gone from the tree — it is NOT re-registered and never auto-runs — but an old habit still lands
     * on the new command as a clickable chip, with the full tail preserved. Two exist:
     *   • anim      → animation   (hard rename 2026-06-22, AnimCommands.java:54 — old /cb anim removed)
     *   • livecolor → recolor     (§G27.11 locked rename, ImageToolCommands.java:78 — NO live alias)
     * `anim` would also prefix-match `animation` on its own, but `livecolor`→`recolor` fails the fuzzy
     * gate (ratio 0.44 > smart 0.34), so the map is what makes that bridge exist at all.
     */
    private static final Map<String, String> RENAMED = Map.of(
            "anim", "animation",
            "livecolor", "recolor");

    /**
     * Fuzzy threshold as a FRACTION of word length, not an absolute edit count.
     * A third of the word may be wrong ("cretae"→"create" is 2/6 = 0.33). Anything looser starts
     * matching genuinely different words, so this is fixed — there is deliberately no looser mode.
     */
    private static final double SMART_MAX = 0.34;

    /** Below this many characters a typo is indistinguishable from a different word — don't guess. */
    private static final int MIN_FUZZY_LEN = 3;

    private DidYouMean() {} // static-only

    /**
     * Append the greedy catch-all as the LAST branch of the /cb tree. Always present so unknown
     * input gets a friendly suggestion (or an honest "I don't know") instead of a raw Brigadier
     * usage error.
     */
    public static void appendFallback(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.argument("subcommand", StringArgumentType.greedyString())
                .suggests((c, b) -> b.buildFuture()) // no tab-complete noise from the fallback
                .executes(ctx -> handleUnknown(ctx.getSource(),
                        StringArgumentType.getString(ctx, "subcommand"))));
    }

    private static int handleUnknown(ServerCommandSource src, String tail) {
        String typedRaw = tail == null ? "" : tail.trim();
        int sp = typedRaw.indexOf(' ');
        String first = (sp < 0 ? typedRaw : typedRaw.substring(0, sp)).toLowerCase(Locale.ROOT);
        String remainder = sp < 0 ? "" : typedRaw.substring(sp).trim();

        // A hard-renamed command is a KNOWN bridge, not a guess — offer it regardless of mode. It is a
        // clickable chip that never auto-runs and never revives the removed name (owner-locked 2026-07-15).
        String renamed = RENAMED.get(first);
        if (renamed != null) {
            String moved = "/cb " + renamed + (remainder.isEmpty() ? "" : " " + remainder);
            Chat.line(src, Text.literal(CbFmt.BAD + "\"" + first + "\" is now ")
                    .append(Chat.runButton(CbFmt.CLICK + "[" + moved + "]", moved))
                    .append(Text.literal(CbFmt.DIM + " — click to run it.")));
            return 1;
        }

        // Suggestion behaviour is always smart — confident hits only, never editable (owner-locked 2026-07-18).
        String best = first.isEmpty() ? null : pickBest(first);

        if (best == null) {
            // Nothing is genuinely close. Say so honestly — do NOT manufacture a suggestion.
            Chat.line(src, Text.literal(CbFmt.BAD + "I don't know \"" + first + "\" " + CbFmt.BAD + "✖ " + CbFmt.DIM + "— ")
                    .append(Chat.runButton(CbFmt.CLICK + "[/cb help]", "/cb help"))
                    .append(Text.literal(CbFmt.DIM + " for the full list.")));
            return 0;
        }

        // The full tail is preserved, so `/cb anim x ticks 5` offers `/cb animation x ticks 5`.
        // It is a chip: clicking RUNS it. It never auto-executes (G04-4).
        String full = "/cb " + best + (remainder.isEmpty() ? "" : " " + remainder);
        Chat.line(src, Text.literal(CbFmt.BAD + "I don't know \"" + first + "\" " + CbFmt.BAD + "✖ " + CbFmt.DIM + "— did you mean ")
                .append(Chat.runButton(CbFmt.CLICK + "[" + full + "]", full))
                .append(Text.literal(CbFmt.DIM + "?")));
        return 1;
    }

    /**
     * The closest registered subcommand, or null when nothing is close enough to be worth saying.
     *
     * Tiered, best tier wins outright:
     *   1. PREFIX   — what you typed starts the command ("anim" → "animation"). The strongest signal
     *                 there is; among several, the shortest completion is the least presumptuous.
     *   2. SUBSTRING— what you typed appears inside it ("glow" → "setglow").
     *   3. FUZZY    — edit distance normalised by length, gated by the fixed smart threshold. Real typos only.
     */
    private static String pickBest(String typed) {
        Set<String> candidates = CommandTree.literals();
        if (candidates.isEmpty()) return null;              // tree not captured yet (shouldn't happen)
        double maxRatio = SMART_MAX;

        String prefixBest = null, substrBest = null, fuzzyBest = null;
        double fuzzyBestRatio = Double.MAX_VALUE;

        for (String c : candidates) {
            if (typed.equals(c)) continue;                   // an exact match never reaches here

            if (typed.length() >= 2 && c.startsWith(typed)) {
                // shortest completion wins; ties broken alphabetically so the answer is deterministic
                if (prefixBest == null || c.length() < prefixBest.length()
                        || (c.length() == prefixBest.length() && c.compareTo(prefixBest) < 0)) {
                    prefixBest = c;
                }
                continue;
            }
            if (typed.length() >= 3 && c.contains(typed)) {
                if (substrBest == null || c.length() < substrBest.length()
                        || (c.length() == substrBest.length() && c.compareTo(substrBest) < 0)) {
                    substrBest = c;
                }
                continue;
            }
            if (typed.length() < MIN_FUZZY_LEN) continue;

            int d = levenshtein(typed, c);
            double ratio = (double) d / Math.max(typed.length(), c.length());
            if (ratio > maxRatio) continue;                  // not a typo — a different word
            if (ratio < fuzzyBestRatio || (ratio == fuzzyBestRatio && fuzzyBest != null && c.compareTo(fuzzyBest) < 0)) {
                fuzzyBest = c;
                fuzzyBestRatio = ratio;
            }
        }

        if (prefixBest != null) return prefixBest;
        if (substrBest != null) return substrBest;
        return fuzzyBest;
    }

    /** Classic two-row Levenshtein edit distance (recycled from the old project). */
    private static int levenshtein(String a, String b) {
        if (a.equals(b)) return 0;
        int n = a.length(), m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] dp = new int[m + 1];
        for (int j = 0; j <= m; j++) dp[j] = j;
        for (int i = 1; i <= n; i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= m; j++) {
                int tmp = dp[j];
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[j] = Math.min(Math.min(dp[j] + 1, dp[j - 1] + 1), prev + cost);
                prev = tmp;
            }
        }
        return dp[m];
    }
}
