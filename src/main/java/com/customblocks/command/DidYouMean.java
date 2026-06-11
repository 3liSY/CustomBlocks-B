/**
 * DidYouMean.java
 *
 * Responsibility: Typo correction for unknown /cb subcommands (Group 04). A greedy
 * catch-all branch is appended AFTER every real literal; Brigadier always prefers
 * literals, so this only fires when nothing else matched. The closest known subcommand
 * (Levenshtein distance + prefix boost, recycled from the old project) is suggested as
 * a clickable chat line.
 *
 * The pitfall fix: the fallback argument is named "subcommand" — never an internal
 * label like "unknown_cb_tail" — so vanilla usage errors can't leak a raw arg name.
 *
 * Modes (CustomBlocksConfig.didYouMean): smart (confident hits only, default),
 * always (closest match within distance 3), off (plain unknown-command message).
 *
 * Depends on: HelpTopics (command dictionary), CustomBlocksConfig, Chat
 * Called by: CommandRegistrar (appendFallback, registered last)
 */
package com.customblocks.command;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.gui.chest.HelpTopics;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Set;

public final class DidYouMean {

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
            // No confident match (or DYM off) — friendly message, clickable help.
            src.sendError(Text.literal(Chat.PREFIX + "§cI don't know the command \"" + first + "\". Try ")
                    .append(Text.literal("/cb help").styled(s -> s
                            .withColor(net.minecraft.util.Formatting.YELLOW)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cb help"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Open the command browser")))))
                    .append(Text.literal("§c for the full list. §c✖")));
            return 0;
        }

        String full = "/cb " + best + (remainder.isEmpty() ? "" : " " + remainder);
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§7I don't know \"" + first + "\" — did you mean ")
                .append(Text.literal("§e[" + full + "]").styled(s -> s
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, full))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Click to run " + full)))))
                .append(Text.literal("§7? Click it to run.")), false);
        return 1;
    }

    /**
     * The closest known subcommand, or null when nothing is close enough.
     * smart: distance ≤ 1, or ≤ 2 on longer words (≥ 5 chars), or a prefix match.
     * always: distance ≤ 3, or a prefix match.
     */
    private static String pickBest(String mode, String typed) {
        Set<String> candidates = HelpTopics.firstTokens();
        boolean smart = !"always".equals(mode);
        String best = null;
        int bestScore = Integer.MAX_VALUE;

        for (String c : candidates) {
            if (typed.equals(c)) continue; // exact match can't reach here, but be safe
            int d = levenshtein(typed, c);
            boolean prefix = typed.length() >= 2 && c.startsWith(typed);
            int maxDist = smart ? (typed.length() >= 5 ? 2 : 1) : 3;
            if (d > maxDist && !prefix) continue;

            int eff = prefix ? Math.min(d, 1) : d;
            int score = eff * 1000 + c.length();
            if (best == null || score < bestScore || (score == bestScore && c.compareTo(best) < 0)) {
                best = c;
                bestScore = score;
            }
        }
        return best;
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
