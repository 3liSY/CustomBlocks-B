/**
 * Chat.java
 *
 * Responsibility: Branded command feedback — the bold §0§l[§b§lCB§0§l]§r tag + a clear
 * message body + a status glyph (✔ success · ✖ error). Group 04 keeps the brand and the
 * glyphs; what changed is the *wording*: bodies are now full, helpful sentences instead of
 * terse fragments. Every command handler routes through these four methods, so the brand
 * stays consistent everywhere.
 *
 * Used by: every command handler (command/handlers/*) and the chest GUI router.
 */
package com.customblocks.command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class Chat {

    /**
     * The single [CB] tag — black brackets, aqua letters — used everywhere: chat lines AND
     * the action-bar/hotbar tool popups. (The black brackets are dim on the dark hotbar bar;
     * this is the developer's chosen trade for one identical format in all contexts.)
     */
    public static final String PREFIX = "§0§l[§b§lCB§0§l]§r ";

    private Chat() {} // static-only

    /** Brief action-bar feedback for tool use (no chat spam). */
    public static void tool(ServerPlayerEntity player, String body) {
        player.sendMessage(Text.literal(PREFIX + "§f" + body), true);
    }

    /** Green-checked success line: [CB] <body> ✔ */
    public static void success(ServerCommandSource src, String body) {
        src.sendFeedback(() -> Text.literal(PREFIX + "§f" + body + " §a✔"), false);
    }

    /** Red-crossed error line: [CB] <body> ✖ */
    public static void error(ServerCommandSource src, String body) {
        src.sendError(Text.literal(PREFIX + "§c" + body + " §c✖"));
    }

    /** Neutral/info line (no glyph). */
    public static void info(ServerCommandSource src, String body) {
        src.sendFeedback(() -> Text.literal(PREFIX + "§7" + body), false);
    }
}
