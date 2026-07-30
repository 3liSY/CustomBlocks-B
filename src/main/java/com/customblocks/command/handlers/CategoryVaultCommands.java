/**
 * CategoryVaultCommands.java — the cloud half of /cb category (Group 11 §C).
 *
 * Split out of CategoryCommands so both stay under the 400-line handler gate (§9.3); it hangs its
 * sub-verbs onto the SAME `category` literal node, so `/cb category …` is still one command tree.
 *
 *   share  <category>  — ZIP the category and upload it to the vault, reporting a share code
 *   import <code>      — download a share code and create the blocks it carries
 *
 * Both are the only category verbs that touch the network, so both do their work on their own
 * thread and hop back through {@code server.execute(…)} before saying anything or touching game
 * state. Neither is reachable until cloud sharing is enabled AND a vaultEndpoint is configured.
 *
 * Depends on: CloudVaultClient, VaultHistory, BlockExporter, CategoryMembershipStore,
 *             ResourcePackServer, IncidentRecorder, Chat
 * Called by:  CategoryCommands.register
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.cloud.CloudVaultClient;
import com.customblocks.cloud.VaultHistory;
import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockExporter;
import com.customblocks.core.CategoryMembershipStore;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.SlotData;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.List;

public final class CategoryVaultCommands {

    private CategoryVaultCommands() {} // static-only

    private static String str(CommandContext<ServerCommandSource> c, String n) {
        return StringArgumentType.getString(c, n);
    }

    /** Hang the vault sub-verbs onto the shared `category` node. */
    public static void register(LiteralArgumentBuilder<ServerCommandSource> cat) {
        cat.then(CommandManager.literal("share")
                .then(CommandManager.argument("cat", StringArgumentType.greedyString())
                        .suggests(CategorySuggest::categories)
                        .executes(ctx -> shareCategory(ctx, str(ctx, "cat")))))

           .then(CommandManager.literal("import")
                .then(CommandManager.argument("code", StringArgumentType.word())
                        .executes(ctx -> importCategory(ctx, str(ctx, "code")))));
    }

    /** True (and an error printed) when the vault isn't usable — the shared gate for both verbs. */
    private static boolean vaultUnavailable(ServerCommandSource src) {
        if (!CustomBlocksConfig.cloudShareEnabled) {
            Chat.error(src, "Cloud sharing is disabled. Enable it in /cb config (cloudShareEnabled).");
            return true;
        }
        if (!CloudVaultClient.isConfigured()) {
            Chat.error(src, "The cloud vault isn't set up yet. Put your worker URL in config.json as "
                    + "\"vaultEndpoint\", then /cb reload.");
            return true;
        }
        return false;
    }

    // ── /cb category share <cat>  (vault upload — off-thread) ────────────────────

    private static int shareCategory(CommandContext<ServerCommandSource> ctx, String category) {
        ServerCommandSource src = ctx.getSource();
        String cat = CategoryMembershipStore.key(category);
        if (vaultUnavailable(src)) return 0;
        List<SlotData> blocks = CategoryMembershipStore.blocksIn(cat);
        if (blocks.isEmpty()) {
            Chat.error(src, "No blocks in category " + CategoryCommands.shown(cat) + ". See /cb categories.");
            return 0;
        }
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        Chat.info(src, "Uploading category " + CategoryCommands.shown(cat) + " to the vault…");
        new Thread(() -> {
            try {
                Path zip = BlockExporter.exportCategoryZip(cat, blocks);
                if (zip == null) { server.execute(() -> Chat.error(src, "Couldn't build the category ZIP.")); return; }
                byte[] data = java.nio.file.Files.readAllBytes(zip);
                String code = CloudVaultClient.uploadCategory(cat, data);
                server.execute(() -> {
                    if (code == null) {
                        Chat.error(src, "Upload failed — check vaultEndpoint and that the worker is reachable.");
                    } else {
                        VaultHistory.record("category", code, cat, src);
                        MutableText msg = Text.literal(CbFmt.OK + "Shared " + CbFmt.VALUE + CategoryCommands.shown(cat)
                                        + CbFmt.OK + " — code: " + CbFmt.VALUE + code + "  ")
                                .append(Chat.shareButton(code))
                                .append(Text.literal("  " + CbFmt.DIM + "Import with " + CbFmt.BODY + "/cb category import " + code));
                        Chat.line(src, msg);
                    }
                });
            } catch (Exception ex) {
                String code = IncidentRecorder.record("Category vault share failed for " + cat, null, src.getName(), ex);
                server.execute(() -> Chat.incidentError(src, "Couldn't share that category — the vault didn't answer. Check your connection and try again.", code));
            }
        }, "cb-vault-share").start();
        return 1;
    }

    // ── /cb category import <code>  (vault download — off-thread) ────────────────

    private static int importCategory(CommandContext<ServerCommandSource> ctx, String code) {
        ServerCommandSource src = ctx.getSource();
        if (vaultUnavailable(src)) return 0;
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        Chat.info(src, "Downloading category code \"" + code + "\"…");
        new Thread(() -> {
            try {
                byte[] zip = CloudVaultClient.downloadCategory(code);
                if (zip == null) { server.execute(() -> Chat.error(src, "Download failed — bad code, or the vault is unreachable.")); return; }
                BlockExporter.ImportResult r = BlockExporter.importCategoryZip(zip);
                server.execute(() -> {
                    int c = r.created().size(), s = r.skipped().size(), f = r.failed().size();
                    if (c > 0) {
                        ResourcePackServer.updatePack();
                        Chat.success(src, "Imported " + c + " block(s): " + String.join(", ", r.created()) + ".");
                    }
                    if (s > 0) Chat.info(src, "Skipped " + s + " already-present: " + String.join(", ", r.skipped()));
                    if (f > 0) Chat.error(src, f + " couldn't import: " + String.join(", ", r.failed()));
                    if (c == 0 && s == 0 && f == 0) Chat.info(src, "Nothing to import from that code.");
                });
            } catch (Exception ex) {
                String incidentCode = IncidentRecorder.record("Category vault import failed (code: " + code + ")", null, src.getName(), ex);
                server.execute(() -> Chat.incidentError(src, "Couldn't import that code — it may be wrong, expired, or the vault is unreachable.", incidentCode));
            }
        }, "cb-vault-import").start();
        return 1;
    }
}
