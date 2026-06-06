/**
 * BlockSuggestions.java
 *
 * Responsibility: Tab-completion for command args that take an EXISTING block id.
 * Suggests the ids the player has actually created (from SlotManager), filtered by
 * what they've typed so far. Shared by the create/utility handlers.
 *
 * Called by: CreationCommands (delete/rename/dupe/retexture), UtilityCommands (give).
 */
package com.customblocks.command.handlers;

import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Locale;

public final class BlockSuggestions {

    private BlockSuggestions() {} // static-only

    /** Suggests existing block ids, prefix-matched against the current input. */
    public static final SuggestionProvider<ServerCommandSource> IDS = (ctx, builder) -> {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (SlotData d : SlotManager.assignedSlots()) {
            if (d.customId().toLowerCase(Locale.ROOT).startsWith(typed)) {
                builder.suggest(d.customId());
            }
        }
        return builder.buildFuture();
    };
}
