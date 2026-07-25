/**
 * PlacedMaskCommands.java — Group 30 (Guess Mode) · §H Placed Mask Mode. OP / level-2 (inherited).
 *
 * The {@code placed} leaf under {@code /cb guess} — one toggle, nothing else:
 *
 *   /cb guess placed   → ON:  every CustomBlock you place from now on shows as the bundled "?" to everyone
 *                             else, while you keep seeing your real build
 *                        OFF: every block you masked turns real for everyone, at once
 *
 * §H is the inverse of {@code /cb guess &lt;player&gt; on …}: that one blinds a single flagged holder while
 * watchers keep the truth, this one blinds every watcher while one runner keeps the truth. Two players may
 * run it at the same time and stay independent — each sees only their own placements as real.
 *
 * Op level decides who may RUN this, never who may SEE through it: a second operator watching still sees "?".
 * There is no look argument and no Guess Screen tab — §H is fixed to the bundled "?" by design.
 *
 * Depends on: PlacedMaskStore, PlacedMaskHooks, GuessSync, Chat, CbFmt.
 * Called by:  GuessCommands.register (attaches {@link #node()} under the guess literal).
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.PlacedMaskHooks;
import com.customblocks.core.PlacedMaskStore;
import com.customblocks.network.GuessSync;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class PlacedMaskCommands {

    private PlacedMaskCommands() {} // static-only

    /** The {@code placed} literal node, attached under {@code /cb guess} by GuessCommands (op-gated there). */
    public static LiteralArgumentBuilder<ServerCommandSource> node() {
        return CommandManager.literal("placed").executes(PlacedMaskCommands::toggle);
    }

    private static int toggle(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity runner)) {
            Chat.error(src, "Run /cb guess placed as a player — it hides the blocks YOU place.");
            return 0;
        }
        UUID id = runner.getUuid();
        int hidden = PlacedMaskStore.count(id);          // read BEFORE the toggle: off clears the set
        boolean on = PlacedMaskStore.toggle(id);
        if (!on) PlacedMaskHooks.forget(id);             // fresh session warns about the cap again

        // Everyone's picture changes at once: on = nothing masked yet, off = every masked block revealed.
        GuessSync.broadcastPlacedMask(src.getServer());

        if (on) {
            Chat.success(src, "Placed Mask ON — every custom block you place from now on shows as \"?\" to "
                    + "everyone else. You still see your real build. Run /cb guess placed again to reveal it.");
        } else if (hidden > 0) {
            Chat.success(src, "Placed Mask OFF — " + CbFmt.VALUE + hidden + CbFmt.BODY
                    + (hidden == 1 ? " block is" : " blocks are") + " now real for everyone.");
        } else {
            Chat.success(src, "Placed Mask OFF — you had not placed anything while it was on.");
        }
        return 1;
    }
}
