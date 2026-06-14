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

import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class BulkChat {

    private BulkChat() {} // static-only

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
            if (i < ids.size() - 1) sb.append((i + 1) % 5 == 0 ? "\n§f" : "§7, §f");
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
                .append(Chat.hover("§e§n" + count + " block" + (count == 1 ? "" : "s") + "§r", hoverList))
                .append(Text.literal(trail))
                .append(Chat.runButton(okLabel, "/cb confirm", "§a/cb confirm"))
                .append(Text.literal("  "))
                .append(Chat.runButton(cancelLabel, "/cb cancel", "§c/cb cancel"))
                .append(Text.literal("  §8expires 60s"));
        Chat.line(src, msg);
    }
}
