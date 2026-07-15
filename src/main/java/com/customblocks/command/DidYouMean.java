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
 * Modes (CustomBlocksConfig.didYouMean): smart (confident hits only, default),
 * always (a looser threshold — still normalised, still never garbage), off (plain message).
 *
 * Depends on: CommandTree (the real registered literals), CustomBlocksConfig, Chat
 * Called by: CommandRegistrar (appendFallback, registered last)
 */
package com.customblocks.command;

import com.customblocks.CustomBlocksConfig;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Set;

public final class DidYouMean {

    /**
     * Fuzzy threshold as a FRACTION of word length, not an absolute edit count.
     * smart: a third of the word may be wrong ("cretae"→"create" is 2/6 = 0.33).
     * always: half may be wrong. Even "always" refuses genuine nonsense — that is the point.
     */
    private static final double SMART_MAX  = 0.34;
    private static final double ALWAYS_MAX = 0.50;

    /** Below this many characters a typo is indistinguishable from a different word — don't guess. */
    private static final int MIN_FUZZY_LEN = 3;

    private DidYouMean() {} // static-only

    /**
     * Append the greedy catch-all as the LAST branch of the /cb tree. Always present
     * (even when the mode is off) so unknown input gets a friendly message instead of
     * a raw Brigadier usage error.
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

        String mode = CustomBlocksConfig.normalizeDidYouMean(CustomBlocksConfig.didYouMean);
        String best = "off".equals(mode) || first.isEmpty() ? null : pickBest(mode, first);

        if (best == null) {
            // Nothing is genuinely close. Say so honestly — do NOT manufacture a suggestion.
            Chat.raw(src, Text.literal(CbFmt.BAD + "I don't know \"" + first + "\". ")
                    .append(Chat.runButton("/cb help", "/cb help"))
                    .append(Text.literal(CbFmt.DIM + " for the full list.")));
            return 0;
        }

        // The full tail is preserved, so `/cb anim x ticks 5` offers `/cb animation x ticks 5`.
        // It is a chip: clicking RUNS it. It never auto-executes (G04-4).
        String full = "/cb " + best + (remainder.isEmpty() ? "" : " " + remainder);
        Chat.raw(src, Text.literal(CbFmt.DIM + "I don't know \"" + first + "\" — did you mean ")
                .append(Chat.runButton(full, full))
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
     *   3. FUZZY    — edit distance normalised by length, gated by mode. Real typos only.
     */
    private static String pickBest(String mode, String typed) {
        Set<String> candidates = CommandTree.literals();
        if (candidates.isEmpty()) return null;              // tree not captured yet (shouldn't happen)
        double maxRatio = "always".equals(mode) ? ALWAYS_MAX : SMART_MAX;

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
