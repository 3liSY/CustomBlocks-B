/**
 * SourceListCommands.java
 *
 * Responsibility: the /cb sourcelist command — a READ-ONLY survey that tells the owner which of their
 * blocks can be re-made sharper and how. It snapshots the block list on the server thread, then scans
 * each block's saved files OFF the server thread (~1000 small reads — same off-thread idiom as
 * retextureall), writes a plain-English report to config/customblocks/exports/, and reports the bucket
 * tallies in chat with a click-to-copy file path. Mutates nothing, downloads nothing.
 *
 * Why it exists: most old blocks look soft because their SOURCE images were small, not because of a
 * render bug or file damage (Group 14 handoff). The only way to sharpen a specific old block is to
 * re-bake it from a bigger source — so the owner first needs to SEE which blocks even have a source.
 *
 * Depends on: SlotManager, SourceAudit, Chat
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotManager;
import com.customblocks.core.SourceAudit;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;

public final class SourceListCommands {

    private SourceListCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb sourcelist — list every block + whether it can be re-made sharper (auto / link-only / needs a pic).
        root.then(CommandManager.literal("sourcelist")
                .executes(ctx -> sourceList(ctx.getSource())));
    }

    private static int sourceList(ServerCommandSource src) {
        // Snapshot the live block view on the server thread, then scan files off it (heavy ~1000 reads).
        var blocks = new ArrayList<>(SlotManager.assignedSlots());
        if (blocks.isEmpty()) { Chat.error(src, "You have no blocks yet — nothing to list."); return 0; }
        Chat.info(src, "Scanning " + blocks.size() + " block(s) for re-makeable sources…");
        MinecraftServer server = src.getServer();

        Thread worker = new Thread(() -> {
            SourceAudit.Result r = SourceAudit.writeReport(blocks);
            if (server == null) return;
            server.execute(() -> report(src, r));
        }, "CustomBlocks-SourceList");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }

    private static void report(ServerCommandSource src, SourceAudit.Result r) {
        if (r == null || r.file() == null) {
            Chat.error(src, "Couldn't write the report file — check the log.");
            return;
        }
        Chat.success(src, "Source report ready — " + CbFmt.OK + r.autoReady() + CbFmt.RESET + " auto-ready, " + CbFmt.VALUE + r.linkOnly()
                + CbFmt.RESET + " link-only, " + CbFmt.DIM + r.needsImage() + CbFmt.RESET + " need a picture from you (of " + CbFmt.BODY + r.total() + CbFmt.RESET + " total).");
        String rel = "config/customblocks/exports/" + r.file().getFileName();
        Chat.line(src, Chat.copyButton(CbFmt.VALUE + "[click to copy the file path]", rel,
                "Copy the report's location to your clipboard, then open it in Notepad:\n" + rel));
    }
}
