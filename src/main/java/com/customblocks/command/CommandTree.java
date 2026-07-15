/**
 * CommandTree.java — the REAL registered /cb subcommands, captured from Brigadier (G04-2).
 *
 * The bug this exists to kill: DidYouMean and /cb help both fed off HelpTopics.CATEGORIES, a
 * hand-maintained list. The class header claimed "help and typo correction can never drift apart" —
 * true of each other, and false of the thing that actually matters. Both had drifted from the real
 * command tree: 67 of the 119 registered subcommands were missing, `animation` among them. So typing
 * `/cb anim` could not suggest `/cb animation` (not a candidate) and instead offered `/cb gui`.
 *
 * The fix is to stop maintaining a list. After {@code dispatcher.register(root)} the returned
 * {@link LiteralCommandNode} exposes {@link com.mojang.brigadier.tree.CommandNode#getChildren()} —
 * every literal child IS a subcommand, by construction. Capture that and hand it to DidYouMean and
 * /cb help. Drift is now structurally impossible: a command that exists is a candidate, full stop.
 *
 * Depends on: Brigadier's registered tree, HelpTopics (only to AUDIT coverage, never to source names)
 * Called by:  CommandRegistrar (capture), DidYouMean (candidates), HelpCommands (complete list)
 */
package com.customblocks.command;

import com.customblocks.gui.chest.HelpTopics;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class CommandTree {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks/Commands");

    /** Every literal registered under /cb, lowercase, sorted. Replaced once at registration. */
    private static volatile Set<String> LITERALS = Set.of();

    private CommandTree() {} // static-only

    /**
     * Snapshot the literal children of the registered /cb node. Called once, right after
     * {@code dispatcher.register(root)} — the ONE moment the authoritative tree exists.
     *
     * The greedy DidYouMean catch-all is an ArgumentCommandNode, not a literal, so it is skipped
     * for free and can never suggest itself.
     */
    public static void capture(LiteralCommandNode<ServerCommandSource> node) {
        Set<String> found = new TreeSet<>();
        for (CommandNode<ServerCommandSource> child : node.getChildren()) {
            if (child instanceof LiteralCommandNode<ServerCommandSource> lit) {
                found.add(lit.getName().toLowerCase(Locale.ROOT));
            }
        }
        LITERALS = Collections.unmodifiableSet(found);
        LOG.info("[CustomBlocks] Command tree captured: {} subcommands.", LITERALS.size());
        auditHelpCoverage();
    }

    /** Every real /cb subcommand. The candidate set for DidYouMean and the checklist for /cb help. */
    public static Set<String> literals() { return LITERALS; }

    /** True if {@code token} is an actual registered subcommand. */
    public static boolean isCommand(String token) {
        return token != null && LITERALS.contains(token.toLowerCase(Locale.ROOT));
    }

    /**
     * Registered subcommands that no one has written a help entry for — neither a {@code /cb help}
     * topic nor an explicit "internal, deliberately not in help" declaration.
     *
     * The {@code helpCoverageGate} build task fails on exactly this list, so it is normally empty.
     * This runtime copy is the backstop: the gate scans source text and can only see the common
     * registration shapes, whereas this sees the tree Brigadier actually built.
     */
    public static List<String> missingHelpEntries() {
        Set<String> documented = HelpTopics.documentedCommands();
        List<String> missing = new ArrayList<>();
        for (String lit : LITERALS) {
            if (!documented.contains(lit)) missing.add(lit);
        }
        return missing;
    }

    /** Log loudly if a command shipped with no help entry. Never throws — a live server must still boot. */
    private static void auditHelpCoverage() {
        List<String> missing = missingHelpEntries();
        if (missing.isEmpty()) return;
        LOG.error("[CustomBlocks] {} registered command(s) have NO help entry — they are invisible to "
                + "/cb help and to \"did you mean\": {}", missing.size(), String.join(", ", missing));
        LOG.error("[CustomBlocks] Add a Topic in HelpTopics, or declare it internal in HelpTopics.INTERNAL.");
    }
}
