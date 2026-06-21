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

import com.customblocks.core.ParticleFx;
import com.customblocks.core.SoundFx;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class Chat {

    /**
     * The single [CB] tag — black brackets, aqua letters — used on CHAT lines (success / error /
     * info / line). The action-bar/hotbar tool popups are deliberately UNBRANDED for a cleaner
     * hotbar (dev call 2026-06-20); see {@link #tool}.
     */
    public static final String PREFIX = "§0§l[§b§lCB§0§l]§r ";

    private Chat() {} // static-only

    /** Brief action-bar feedback for tool use (no chat spam). Unbranded — no [CB] tag on the hotbar. */
    public static void tool(ServerPlayerEntity player, String body) {
        player.sendMessage(Text.literal("§f" + body), true);
    }

    /** Green-checked success line: [CB] <body> ✔ — fires the generic "success" Feedback FX. */
    public static void success(ServerCommandSource src, String body) {
        src.sendFeedback(() -> Text.literal(PREFIX + "§f" + body + " §a✔"), false);
        if (src.getEntity() instanceof ServerPlayerEntity p) { ParticleFx.play(p, "success"); SoundFx.play(p, "success"); }
    }

    /** Red-crossed error line: [CB] <body> ✖ */
    public static void error(ServerCommandSource src, String body) {
        src.sendError(Text.literal(PREFIX + "§c" + body + " §c✖"));
        if (src.getEntity() instanceof ServerPlayerEntity p) { ParticleFx.play(p, "error"); SoundFx.play(p, "error"); }
    }

    /** Neutral/info line (no glyph). */
    public static void info(ServerCommandSource src, String body) {
        src.sendFeedback(() -> Text.literal(PREFIX + "§7" + body), false);
    }

    // ── Rich chat: clickable + hoverable components (QOL — less flooding, more interactive) ──

    /** Send a pre-built rich component as a branded [CB] line. */
    public static void line(ServerCommandSource src, MutableText body) {
        src.sendFeedback(() -> Text.literal(PREFIX).append(body), false);
    }

    /** A clickable [label] that RUNS {@code command} when clicked, showing {@code hover} on hover. */
    public static MutableText runButton(String label, String command, String hover) {
        return Text.literal(label).styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    /** A clickable [label] that COPIES {@code text} to the player's clipboard, with a hover tooltip. */
    public static MutableText copyButton(String label, String text, String hover) {
        return Text.literal(label).styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    /** A clickable [label] that PRE-FILLS {@code command} into the chat box, with a hover tooltip. */
    public static MutableText suggestButton(String label, String command, String hover) {
        return Text.literal(label).styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    /** Plain {@code label} that reveals {@code hoverText} (which may contain "\n") on hover — no click. */
    public static MutableText hover(String label, String hoverText) {
        return Text.literal(label).styled(s -> s
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hoverText))));
    }
}
