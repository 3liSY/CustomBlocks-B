/**
 * Chat.java
 *
 * Responsibility: Branded, concise command feedback in the old project's house style —
 * one bold §0§l[§b§lCB§0§l]§r prefix + a short body + a status glyph. Keeps in-game
 * messages compact instead of multi-line explanations.
 *
 * Used by: the command handlers (CreationCommands, AttributeCommands, UtilityCommands).
 */
package com.customblocks.command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class Chat {

    /** Bold [CB] tag — black brackets, aqua letters (matches the old project). For chat. */
    public static final String PREFIX = "§0§l[§b§lCB§0§l]§r ";

    /**
     * HUD-friendly tag — aqua brackets. The chat PREFIX's §0 (black) brackets are nearly
     * invisible on the dark action-bar HUD, so tool feedback uses this instead.
     */
    public static final String HUD_PREFIX = "§b§l[CB]§r ";

    private Chat() {} // static-only

    /** Brief action-bar feedback for tool use (no chat spam). */
    public static void tool(ServerPlayerEntity player, String body) {
        player.sendMessage(Text.literal(HUD_PREFIX + "§f" + body), true);
    }

    /** Green-checked success line. */
    public static void success(ServerCommandSource src, String body) {
        src.sendFeedback(() -> Text.literal(PREFIX + "§f" + body + " §a✔"), false);
    }

    /** Red-crossed error line. */
    public static void error(ServerCommandSource src, String body) {
        src.sendError(Text.literal(PREFIX + "§c" + body + " §4✖"));
    }

    /** Neutral/info line (no glyph). */
    public static void info(ServerCommandSource src, String body) {
        src.sendFeedback(() -> Text.literal(PREFIX + "§7" + body), false);
    }
}
