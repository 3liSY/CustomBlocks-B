/**
 * BulkChat.java
 *
 * Shared rich-chat helpers for bulk operations (Group 07): the clickable confirm prompt and the
 * hover-friendly id list. Split out of BulkCommands so each new op (property / delete / rename / …)
 * reuses one confirm style and the handler stays under the 400-line gate (§9.3).
 *
 * Called by: BulkCommands.
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class BulkChat {

    private BulkChat() {} // static-only

    /**
     * The ONE sentence that tells a player how to pick blocks for a bulk op. Every bulk usage/help line
     * prints this and nothing else (Group 04 §A7, owner 2026-07-14).
     *
     * It exists as a constant because the thing it replaced did not: five handlers each carried their own
     * copy of "Filters: all · category:<name> · id:<prefix> · name:<text> · favorite:yes|no · locked:yes|no",
     * and when Group 07 deleted that filter language from BulkScope on 2026-07-13 the five copies stayed
     * behind — advertising a syntax the server had stopped resolving. One constant, one place to change.
     */
    public static final String SCOPE_HELP =
            "Pick blocks by id: id1 id2 id3  ·  or 'all' for every block  (commas and \"quotes\" work too)";

    /** The ids of a block list, in order. */
    public static List<String> ids(List<SlotData> blocks) {
        List<String> out = new ArrayList<>(blocks.size());
        for (SlotData d : blocks) out.add(d.customId());
        return out;
    }

    /** Lay ids out 5-per-row for a readable hover tooltip (long lists don't run off screen). */
    public static String columns(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) sb.append((i + 1) % 5 == 0 ? "\n" + CbFmt.BODY : CbFmt.DIM + ", " + CbFmt.BODY);
        }
        return sb.toString();
    }

    /**
     * A clickable confirm line: {@code lead} + the count (hover shows {@code hoverList}) + {@code trail}
     * + [ok] / [cancel] buttons + a 60s note. The buttons run /cb confirm and /cb cancel.
     */
    public static void confirm(ServerCommandSource src, String lead, String trail, int count,
                               String hoverList, String okLabel, String cancelLabel) {
        MutableText msg = Text.literal(lead)
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + count + " block" + (count == 1 ? "" : "s") + CbFmt.RESET, hoverList))
                .append(Text.literal(trail))
                .append(Chat.runButton(okLabel, "/cb confirm", CbFmt.OK + "/cb confirm"))
                .append(Text.literal("  "))
                .append(Chat.runButton(cancelLabel, "/cb cancel", CbFmt.BAD + "/cb cancel"))
                .append(Text.literal("  " + CbFmt.FAINT + "expires 60s"));
        Chat.line(src, msg);
    }
}
