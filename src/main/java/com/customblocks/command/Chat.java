/**
 * Chat.java — the ONE place every CustomBlocks message is sent (G04-3 / §F).
 *
 * Two surfaces, one voice:
 *   • CHAT  — branded §0§l[§b§lCB§0§l]§r line + body + a status glyph (✔ / ✖). success / error /
 *             info / line carry the [CB] tag; raw / toPlayer send an UNBRANDED line (list bodies,
 *             welcome banners, incident dumps — one [CB] per block, not per line).
 *   • HOTBAR — the action bar, unbranded, one colour contract: green = worked, red = failed,
 *             light-gray §7 = a neutral fact, yellow = the value. The CALLER names the MEANING
 *             (toolSuccess / toolError / tool / lockedTool); this file picks the colour. A § or a
 *             CbFmt colour inside a hotbar body is a bug (hotbarRouteGate). Every hotbar line goes
 *             through {@link #hotbar}/{@link #toolRaw} — the one sanctioned sendMessage(…, true),
 *             anti-strobe-gated by {@link HotbarSpeaker}.
 *
 * Colours are never hand-picked here beyond the [CB] tag itself: they come from {@link CbFmt} by
 * name (OK / BAD / VALUE / DIM / TOOL_*). Chat.java is exempt from chatColourGate/chatRouteGate/
 * hotbarRouteGate because it IS the sanctioned way in.
 *
 * Used by: every command handler (command/handlers/*), the chest-GUI router, the item tools, and
 * the buzzer game.
 */
package com.customblocks.command;

import com.customblocks.core.IncidentRecorder;
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
     * The single [CB] tag — black brackets, aqua letters — used on branded CHAT lines
     * (success / error / info / line / successWith / incidentError). Hotbar lines and raw / toPlayer
     * are deliberately UNBRANDED.
     */
    public static final String PREFIX = "§0§l[§b§lCB§0§l]§r ";

    private Chat() {} // static-only

    // ══════════════════════════════════════════════════════════════════════════
    //  CHAT — branded lines ([CB] tag + glyph)
    // ══════════════════════════════════════════════════════════════════════════

    /** Green-checked success line: [CB] <body> ✔ — fires the generic "success" Feedback FX. */
    public static void success(ServerCommandSource src, String body) {
        src.sendFeedback(() -> Text.literal(PREFIX + CbFmt.BODY + body + " " + CbFmt.OK + "✔"), false);
        if (src.getEntity() instanceof ServerPlayerEntity p) { ParticleFx.play(p, "success"); SoundFx.play(p, "success"); }
    }

    /**
     * A success line that carries one or more clickable chips (Edit / Undo / Redo …): [CB] <body> ✔
     * followed by each chip. Null chips are skipped, so a caller can pass a chip that may not apply.
     */
    public static void successWith(ServerCommandSource src, String body, MutableText... chips) {
        MutableText line = Text.literal(PREFIX + CbFmt.BODY + body + " " + CbFmt.OK + "✔");
        for (MutableText chip : chips) {
            if (chip != null) line.append(Text.literal("  ")).append(chip);
        }
        final MutableText out = line;
        src.sendFeedback(() -> out, false);
        if (src.getEntity() instanceof ServerPlayerEntity p) { ParticleFx.play(p, "success"); SoundFx.play(p, "success"); }
    }

    /**
     * A success line whose BODY is a pre-built component — the same brand, glyph and FX as
     * {@link #success}, for a sentence that carries styling of its own (G11: a category name
     * rendered in that category's colour tag, clickable and hoverable). Chips ride the tail exactly
     * as in {@link #successWith}; null chips are skipped.
     */
    public static void successRich(ServerCommandSource src, MutableText body, MutableText... chips) {
        MutableText line = Text.literal(PREFIX).append(body).append(Text.literal(" " + CbFmt.OK + "✔"));
        for (MutableText chip : chips) {
            if (chip != null) line.append(Text.literal("  ")).append(chip);
        }
        final MutableText out = line;
        src.sendFeedback(() -> out, false);
        if (src.getEntity() instanceof ServerPlayerEntity p) { ParticleFx.play(p, "success"); SoundFx.play(p, "success"); }
    }

    /** The {@link #error} twin for a pre-built component body — [CB] <body> ✖. */
    public static void errorRich(ServerCommandSource src, MutableText body) {
        src.sendError(Text.literal(PREFIX).append(body).append(Text.literal(" " + CbFmt.BAD + "✖")));
        if (src.getEntity() instanceof ServerPlayerEntity p) { ParticleFx.play(p, "error"); SoundFx.play(p, "error"); }
    }

    /** Red-crossed error line: [CB] <body> ✖ */
    public static void error(ServerCommandSource src, String body) {
        src.sendError(Text.literal(PREFIX + CbFmt.BAD + body + " " + CbFmt.BAD + "✖"));
        if (src.getEntity() instanceof ServerPlayerEntity p) { ParticleFx.play(p, "error"); SoundFx.play(p, "error"); }
    }

    /**
     * The canonical locked-block chat error (G04-3): {@code "x" is locked. Use /cb unlock x to edit it.}
     * The hotbar twin is {@link #lockedTool}; both say the same sentence so the wording never drifts.
     */
    public static void lockedError(ServerCommandSource src, String id) {
        error(src, "\"" + id + "\" is locked. Use /cb unlock " + id + " to edit it.");
    }

    /**
     * A major-error line tied to an incident (G04-4): the friendly red sentence, then a dim line
     * carrying the pasteable code AND a clickable {@code /cb incidents <code>} jump to the full entry.
     * The caller has already logged the incident via {@link IncidentRecorder} and passes its code.
     */
    public static void incidentError(ServerCommandSource src, String message, String code) {
        error(src, message);
        if (code != null && !code.isBlank()) {
            MutableText detail = Text.literal(CbFmt.DIM + "Logged as " + CbFmt.VALUE + code + CbFmt.DIM + "  ")
                    .append(runButton(CbFmt.CLICK + "[⊙ Details]", "/cb incidents " + code, "Open incident " + code));
            src.sendFeedback(() -> detail, false);
        }
    }

    /** Neutral/info line (no glyph), branded: [CB] §7<body>. */
    public static void info(ServerCommandSource src, String body) {
        src.sendFeedback(() -> Text.literal(PREFIX + CbFmt.DIM + body), false);
    }

    /** Send a pre-built rich component as a branded [CB] line. */
    public static void line(ServerCommandSource src, MutableText body) {
        src.sendFeedback(() -> Text.literal(PREFIX).append(body), false);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CHAT — unbranded lines (no [CB] tag) — list bodies, banners, per-player sends
    // ══════════════════════════════════════════════════════════════════════════

    /** An UNBRANDED chat line via a command source — a body row under a branded header. */
    public static void raw(ServerCommandSource src, String body) {
        src.sendFeedback(() -> Text.literal(body), false);
    }

    /** An UNBRANDED chat line (pre-built component) via a command source. */
    public static void raw(ServerCommandSource src, Text body) {
        src.sendFeedback(() -> body, false);
    }

    /** Send an UNBRANDED line straight to a player (no command source) — networking / GUI / onboarding. */
    public static void toPlayer(ServerPlayerEntity player, String body) {
        player.sendMessage(Text.literal(body), false);
    }

    /** Send an UNBRANDED component straight to a player (no command source). */
    public static void toPlayer(ServerPlayerEntity player, Text body) {
        player.sendMessage(body, false);
    }

    /**
     * Explicitly-unbranded player send — same wire as {@link #toPlayer}, named at the call site to
     * document that the line carries its OWN brand (e.g. the buzzer game's "[BuzzerGame] …").
     */
    public static void toPlayerUnbranded(ServerPlayerEntity player, Text body) {
        player.sendMessage(body, false);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HOTBAR — the action bar. One colour contract, chosen HERE by meaning.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * The ONE sanctioned action-bar send: {@code sendMessage(text, true)}, gated by
     * {@link HotbarSpeaker} so a fast-swung tool can't strobe the same line. Every tool* helper
     * routes here. The formatted string carries its colours already (from CbFmt, chosen by the
     * helper) — a caller must never build one by hand.
     */
    public static void hotbar(ServerPlayerEntity player, String formatted) {
        if (HotbarSpeaker.allow(player, formatted)) {
            player.sendMessage(Text.literal(formatted), true);
        }
    }

    /**
     * Send a pre-built component to the action bar (the buzzer timer bar), still anti-strobe-gated on
     * its rendered text. Bypasses the colour contract on purpose — this is a live scoreboard-style
     * bar, not a tool message, so it is NOT one of the {@code tool*} meanings the hotbar gate checks.
     */
    public static void toolRaw(ServerPlayerEntity player, Text body) {
        if (HotbarSpeaker.allow(player, body.getString())) {
            player.sendMessage(body, true);
        }
    }

    /** A neutral hotbar fact ("Nothing selected") — light-gray §7 (F3, owner-locked 2026-07-15). */
    public static void tool(ServerPlayerEntity player, String label) {
        hotbar(player, CbFmt.TOOL_NEUTRAL + label);
    }

    /** A neutral hotbar fact with a reported value — §7 label + yellow value. */
    public static void tool(ServerPlayerEntity player, String label, String value) {
        hotbar(player, CbFmt.TOOL_NEUTRAL + label + " " + CbFmt.VALUE + value);
    }

    /** A hotbar success sentence — green (F1). */
    public static void toolSuccess(ServerPlayerEntity player, String sentence) {
        hotbar(player, CbFmt.TOOL_OK + sentence);
    }

    /** A hotbar success with a reported value — green label + yellow value (F1: {@code Glow set 12}). */
    public static void toolSuccess(ServerPlayerEntity player, String label, String value) {
        hotbar(player, CbFmt.TOOL_OK + label + " " + CbFmt.VALUE + value);
    }

    /** A hotbar error sentence — red. */
    public static void toolError(ServerPlayerEntity player, String sentence) {
        hotbar(player, CbFmt.TOOL_BAD + sentence);
    }

    /**
     * The hotbar locked-block error (F2): {@code Can't edit "x" — /cb unlock x} — red label, yellow
     * quoted id, light-gray §7 tail (owner-locked 2026-07-15). The chat twin is {@link #lockedError}.
     */
    public static void lockedTool(ServerPlayerEntity player, String id) {
        hotbar(player, CbFmt.TOOL_BAD + "Can't edit " + CbFmt.VALUE + "\"" + id + "\""
                + CbFmt.TOOL_NEUTRAL + " — /cb unlock " + id);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Clickable / hoverable chips (aqua = the vanilla "you can click this" colour)
    // ══════════════════════════════════════════════════════════════════════════

    /** A clickable [label] that RUNS {@code command} when clicked, showing {@code hover} on hover. */
    public static MutableText runButton(String label, String command, String hover) {
        return Text.literal(label).styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    /** A clickable [label] that RUNS {@code command}; the hover defaults to the command itself. */
    public static MutableText runButton(String label, String command) {
        return runButton(label, command, command);
    }

    /** A clickable [label] that COPIES {@code text} to the player's clipboard, with a hover tooltip. */
    public static MutableText copyButton(String label, String text, String hover) {
        return Text.literal(label).styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    /** A clickable [label] that COPIES {@code text}; hover defaults to a "Copy …" tooltip. */
    public static MutableText copyButton(String label, String text) {
        return copyButton(label, text, "Copy " + text);
    }

    /** A clickable [label] that PRE-FILLS {@code command} into the chat box, with a hover tooltip. */
    public static MutableText suggestButton(String label, String command, String hover) {
        return Text.literal(label).styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    /** A clickable [label] that PRE-FILLS {@code command}; hover defaults to the command itself. */
    public static MutableText suggestButton(String label, String command) {
        return suggestButton(label, command, command);
    }

    /** Plain {@code label} that reveals {@code hoverText} (which may contain "\n") on hover — no click. */
    public static MutableText hover(String label, String hoverText) {
        return Text.literal(label).styled(s -> s
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hoverText))));
    }

    // ── The curated chip set (G04-4) — every chip runs a REAL /cb command, aqua-coloured ──

    /** ↩ Undo — runs the real {@code /cb undo}. The single undo affordance across the whole mod. */
    public static MutableText undoButton() {
        return runButton(CbFmt.CLICK + "[↩ Undo]", "/cb undo", "Undo your last change");
    }

    /** ↪ Redo — runs the real {@code /cb redo}. Rides an undo's own result line so it round-trips. */
    public static MutableText redoButton() {
        return runButton(CbFmt.CLICK + "[↪ Redo]", "/cb redo", "Redo the change you just undid");
    }

    /** ✎ Edit — runs {@code /cb edit <id>}. The editor screen is coming later; the chip answers honestly. */
    public static MutableText editButton(String id) {
        return runButton(CbFmt.CLICK + "[✎ Edit]", "/cb edit " + id, "Edit \"" + id + "\" (editor screen coming later)");
    }

    /** ⇪ Copy — copies a Group 20 vault/share code to the clipboard. The only clipboard chip. */
    public static MutableText shareButton(String code) {
        return copyButton(CbFmt.CLICK + "[⇪ Copy]", code, "Copy share code " + code + " to your clipboard");
    }
}
