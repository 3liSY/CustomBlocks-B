/**
 * CloudCommands.java
 *
 * Responsibility: /cb vault and /cb discord subcommands.
 *   /cb vault                     — status / how-to
 *   /cb vault upload <id>         — OP-only: share one block to the cloud, get a code (Group 20 S1)
 *   /cb vault download <code>     — any player: restore a shared block from a code (Group 20 S1)
 *   /cb discord [test|status]     — Phase 14 stubs (full Discord wiring lands in G20 S5/S6)
 *
 * Block share reuses the proven /category vault route (D3): the block is packed into a one-block
 * ZIP by VaultBlockCodec and POSTed via CloudVaultClient.uploadCategory; download GETs the ZIP and
 * VaultBlockCodec restores the block (definition + textures + animation). Network calls run off the
 * server thread; results post back via server.execute. Stays under the 400-line handler gate (§9.3).
 *
 * Depends on: CloudVaultClient, VaultBlockCodec, SlotManager, ResourcePackServer, SoundFx, ParticleFx,
 *             DiscordWebhook, Chat, CustomBlocksConfig
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.core.IncidentRecorder;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.cloud.CloudVaultClient;
import com.customblocks.cloud.VaultBlockCodec;
import com.customblocks.cloud.VaultHistory;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockExporter.ImportResult;
import com.customblocks.core.ParticleFx;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.SoundFx;
import com.customblocks.discord.DiscordWebhook;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class CloudCommands {

    private CloudCommands() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb vault [upload <id> | download <code>]
        var vault = CommandManager.literal("vault");
        vault.executes(ctx -> vaultInfo(ctx));
        vault.then(CommandManager.literal("upload")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> uploadBlock(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id")))));
        vault.then(CommandManager.literal("download")
                .then(CommandManager.argument("code", StringArgumentType.word())
                        .executes(ctx -> downloadBlock(ctx.getSource(),
                                StringArgumentType.getString(ctx, "code")))));
        vault.then(CommandManager.literal("codes").executes(ctx -> vaultCodes(ctx)));
        root.then(vault);

        // /cb discord [test | status]
        var discord = CommandManager.literal("discord");
        discord.executes(ctx -> discordStatus(ctx));
        discord.then(CommandManager.literal("test").executes(ctx -> discordTest(ctx)));
        root.then(discord);
    }

    private static int vaultInfo(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!CustomBlocksConfig.cloudShareEnabled) {
            Chat.info(src, "Cloud sharing is disabled. Enable it in " + CbFmt.VALUE + "/cb config" + CbFmt.RESET + " (cloudShareEnabled).");
        } else if (!CloudVaultClient.isConfigured()) {
            Chat.info(src, "Block Vault is not set up. Put your worker URL in config.json as " + CbFmt.VALUE + "vaultEndpoint" + CbFmt.RESET + ", then " + CbFmt.VALUE + "/cb reload" + CbFmt.RESET + ".");
        } else {
            Chat.info(src, "Vault ready. " + CbFmt.VALUE + "/cb vault upload <id>" + CbFmt.RESET + " (OP) to share · " + CbFmt.VALUE + "/cb vault download <code>" + CbFmt.RESET + " to restore.");
        }
        return 1;
    }

    // ── /cb vault codes  (server-wide list of share codes, newest first) ─────────

    private static int vaultCodes(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        java.util.List<VaultHistory.Entry> all = VaultHistory.all();
        if (all.isEmpty()) {
            Chat.info(src, "No share codes yet. They're saved here when you " + CbFmt.VALUE + "/cb vault upload" + CbFmt.RESET + ", " + CbFmt.VALUE + "/cb category share" + CbFmt.RESET + ", or share lore.");
            return 1;
        }
        int show = Math.min(all.size(), 20);
        Chat.info(src, CbFmt.VALUE + all.size() + CbFmt.DIM + " share code(s) — newest first:");
        for (int i = 0; i < show; i++) {
            VaultHistory.Entry e = all.get(i);
            MutableText line = Text.literal(CbFmt.DIM + "• " + CbFmt.VALUE + e.code() + " " + CbFmt.FAINT + "[" + e.kind() + "] " + CbFmt.BODY + e.label()
                            + (e.by().isEmpty() ? "" : " " + CbFmt.FAINT + "by " + e.by()) + "  ")
                    .append(Chat.copyButton("[copy]", e.code()));
            Chat.raw(src, line);
        }
        if (all.size() > show) Chat.info(src, CbFmt.FAINT + "…and " + (all.size() - show) + " more.");
        return 1;
    }

    // ── /cb vault upload <id>  (OP-only, off-thread) ─────────────────────────────

    public static int uploadBlock(ServerCommandSource src, String id) {
        if (!cloudReady(src)) return 0;
        if (!src.hasPermissionLevel(2)) {
            Chat.error(src, "Only operators can upload blocks to the vault.");
            return 0;
        }
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "No block \"" + id + "\". See /cb list.");
            return 0;
        }
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        ServerPlayerEntity player = (src.getEntity() instanceof ServerPlayerEntity p) ? p : null;
        Chat.info(src, "Uploading \"" + id + "\" to the vault…");
        new Thread(() -> {
            try {
                byte[] zip = VaultBlockCodec.pack(d);
                if (zip == null) {
                    String reason = VaultBlockCodec.lastPackError();
                    server.execute(() -> Chat.error(src, "Couldn't package \"" + id + "\" — " + reason));
                    return;
                }
                String code = CloudVaultClient.uploadCategory(id, zip);
                server.execute(() -> {
                    if (code == null) {
                        Chat.error(src, "Upload failed — check vaultEndpoint and that the worker is reachable.");
                    } else {
                        VaultHistory.record("block", code, id, src);
                        MutableText msg = Text.literal(CbFmt.OK + "✔ Shared " + CbFmt.VALUE + id + CbFmt.OK + " — code: " + CbFmt.VALUE + code + "  ")
                                .append(Chat.shareButton(code));
                        Chat.line(src, msg);
                        shareJuice(player, code);
                    }
                });
            } catch (Exception ex) {
                String incidentCode = IncidentRecorder.record("Vault share failed for \"" + id + "\"", id, src.getName(), ex);
                server.execute(() -> Chat.incidentError(src, "Couldn't share that block — the vault didn't answer. Check your connection and try again.", incidentCode));
            }
        }, "cb-vault-block-share").start();
        return 1;
    }

    // ── /cb vault download <code>  (any player, off-thread) ──────────────────────

    public static int downloadBlock(ServerCommandSource src, String code) {
        if (!cloudReady(src)) return 0;
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        ServerPlayerEntity player = (src.getEntity() instanceof ServerPlayerEntity p) ? p : null;
        Chat.info(src, "Downloading code \"" + code + "\"…");
        new Thread(() -> {
            try {
                byte[] zip = CloudVaultClient.downloadCategory(code);
                if (zip == null) {
                    server.execute(() -> Chat.error(src, "Download failed — bad code, or the vault is unreachable."));
                    return;
                }
                ImportResult r = VaultBlockCodec.unpack(zip);
                server.execute(() -> {
                    if (!r.created().isEmpty()) {
                        ResourcePackServer.updatePack();
                        String id = r.created().get(0);
                        Chat.success(src, "Downloaded " + CbFmt.VALUE + id + CbFmt.OK + " — use " + CbFmt.BODY + "/cb give " + id + CbFmt.OK + " to get it.");
                    }
                    if (!r.skipped().isEmpty()) {
                        String clashId = r.skipped().get(0);
                        if (player != null) {
                            // S2: the id already exists → open the conflict screen instead of refusing.
                            VaultConflict.open(player, code, zip, clashId);
                        } else {
                            Chat.info(src, "Already have \"" + String.join(", ", r.skipped())
                                    + "\" — delete it first to download this block (the conflict screen is player-only).");
                        }
                    }
                    if (!r.failed().isEmpty()) {
                        Chat.error(src, "Couldn't import: " + String.join(", ", r.failed()));
                    }
                    if (r.created().isEmpty() && r.skipped().isEmpty() && r.failed().isEmpty()) {
                        Chat.info(src, "Nothing to import from that code.");
                    }
                });
            } catch (Exception ex) {
                String incidentCode = IncidentRecorder.record("Vault download failed (code: " + code + ")", null, src.getName(), ex);
                server.execute(() -> Chat.incidentError(src, "Couldn't download that code — it may be wrong, expired, or the vault is unreachable.", incidentCode));
            }
        }, "cb-vault-block-download").start();
        return 1;
    }

    /** Shared upload/download gate: cloud master switch on AND a vaultEndpoint configured. */
    private static boolean cloudReady(ServerCommandSource src) {
        if (!CustomBlocksConfig.cloudShareEnabled) {
            Chat.error(src, "Cloud sharing is disabled. Enable it in /cb config (cloudShareEnabled).");
            return false;
        }
        if (!CloudVaultClient.isConfigured()) {
            Chat.error(src, "The cloud vault isn't set up yet. Put your worker URL in config.json as "
                    + "\"vaultEndpoint\", then /cb reload.");
            return false;
        }
        return true;
    }

    /** Share juice (U10a): success sound + particle burst + an on-screen title with the code. */
    private static void shareJuice(ServerPlayerEntity p, String code) {
        if (p == null) return;
        SoundFx.play(p, "success");
        ParticleFx.play(p, "success");
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(CbFmt.VALUE + "☁ Uploaded!")));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(CbFmt.VALUE + code)));
    }


    // ── Discord (Phase 14 stubs — full wiring in G20 S5/S6) ──────────────────────

    private static int discordStatus(CommandContext<ServerCommandSource> ctx) {
        if (DiscordWebhook.isConfigured())
            Chat.success(ctx.getSource(), "Discord webhook configured. Use /cb discord test to verify.");
        else
            Chat.info(ctx.getSource(), "Discord not configured. Set " + CbFmt.VALUE + "discordWebhookUrl" + CbFmt.RESET + " in config.json.");
        return 1;
    }

    private static int discordTest(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!DiscordWebhook.isConfigured()) { Chat.error(src, "Discord not configured."); return 0; }
        DiscordWebhook.post("[CustomBlocks] Test message from " + src.getName() + " — Discord active!");
        Chat.success(src, "Test message sent to Discord.");
        return 1;
    }
}
