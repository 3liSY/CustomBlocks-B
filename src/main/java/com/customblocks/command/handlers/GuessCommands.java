/**
 * GuessCommands.java — Group 30 (Guess Mode). v2 command redesign (2026-07-05). OP / level-2 only.
 *
 * The /cb guess command tree — flag a target player so that, while they hold/see a flagged custom block,
 * THEIR client shows a blank "???" name + the bundled "?" mystery cube, and everyone sees them hold it with
 * a centered two-handed pose (a party "guess the block" game). All state lives in {@link GuessModeStore}
 * (persistent, per UUID); every change re-broadcasts the set via {@link GuessSync} so it takes effect live
 * (NO-REJOIN).
 *
 *   /cb guess &lt;player&gt;                            → show the player's current guess-mode settings
 *   /cb guess &lt;player&gt; on &lt;blockid&gt;                → blind that one block type for the player (ids STACK)
 *   /cb guess &lt;player&gt; on &lt;blockid&gt; look &lt;lookid&gt;  → blind it AND disguise it as &lt;lookid&gt;'s look (this round)
 *   /cb guess &lt;player&gt; on all                      → blind EVERY custom block the player sees
 *   /cb guess &lt;player&gt; off &lt;blockid&gt;               → stop blinding just that one block type
 *   /cb guess &lt;player&gt; off all                     → turn all-mode off (keeps any specific flagged ids)
 *   /cb guess &lt;player&gt; off                         → clear EVERYTHING (all-mode + every flagged id)
 *   /cb guess defaultblock &lt;id&gt; | clear            → set/clear the GLOBAL default disguise look (v3 Phase 1)
 *   /cb guess settings                             → open the Guess Settings screen (Pose tab) — the ONLY way to
 *                                                     tune the shared pose now (the old /cb guess pose command was removed)
 *   /cb guess placed                               → §H Placed Mask Mode (PlacedMaskCommands): the INVERSE toggle —
 *                                                     the runner's own placements show as "?" to everyone else
 *
 * The blank name is hardcoded "???". The disguise LOOK (v3 Phase 1) resolves per-round override (look &lt;id&gt;)
 * → global default (defaultblock) → bundled "?" cube. Look ids are stored here; GuessSync maps them to slots.
 * Both the default look and the per-round look are EXISTING block ids only (pick from /cb list); there is no
 * paste-a-URL path (v3 Phase 2 "link" was reverted — links removed from guess mode entirely).
 *
 * Depends on: GuessModeStore, GuessSync, SlotManager, SlotData, Chat, BlockSuggestions
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.GuessModeStore;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.GuiMode;
import com.customblocks.network.GuessSync;
import com.customblocks.network.payloads.OpenGuiPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

public final class GuessCommands {

    private GuessCommands() {} // static-only

    /** Sentinel id meaning "all custom blocks" — not a real block id, so it can't collide with the id arg. */
    private static final String ALL = "all";

    /** Tab-complete online player names for the &lt;player&gt; argument. */
    private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS = (c, b) -> {
        MinecraftServer s = c.getSource().getServer();
        if (s != null) for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList())
            b.suggest(p.getName().getString());
        return b.buildFuture();
    };

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("guess")
                .requires(src -> src.hasPermissionLevel(2))
                // /cb guess defaultblock [<id> | clear] — the global default disguise look (v3 Phase 1).
                // A literal sibling of the <player> argument; Brigadier tries the literal first, so a player
                // can never be named "defaultblock" and shadow it.
                .then(CommandManager.literal("defaultblock")
                        .executes(GuessCommands::defaultLookShow)
                        .then(CommandManager.literal("clear")
                                .executes(GuessCommands::defaultLookClear))
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests(BlockSuggestions.IDS)
                                .executes(ctx -> defaultLookSet(ctx, StringArgumentType.getString(ctx, "id")))))
                // /cb guess settings — open the shared Guess Settings screen (Pose tab). Op opens it for
                // themselves via the proven OpenGuiPayload S2C path (mirrors /cb, category hub, studio…).
                .then(CommandManager.literal("settings")
                        .executes(GuessCommands::openSettings))
                // /cb guess showcase spawn [id] | delete — the floating end-crystal display (G30-8b).
                .then(GuessShowcaseCommands.node())
                // /cb guess placed — §H Placed Mask Mode: hide the blocks YOU place from everyone else.
                .then(PlacedMaskCommands.node())
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYERS)
                        .executes(ctx -> status(ctx, player(ctx)))
                        // /cb guess <player> on all | on <blockid> [look <lookid>]
                        .then(CommandManager.literal("on")
                                .then(CommandManager.literal("all")
                                        .executes(ctx -> onAll(ctx, player(ctx))))
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .suggests(BlockSuggestions.IDS)
                                        .executes(ctx -> onId(ctx, player(ctx), StringArgumentType.getString(ctx, "id"), null))
                                        .then(CommandManager.literal("look")
                                                .then(CommandManager.argument("lookid", StringArgumentType.word())
                                                        .suggests(BlockSuggestions.IDS)
                                                        .executes(ctx -> onId(ctx, player(ctx),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "lookid")))))))
                        // /cb guess <player> off | off all | off <blockid>
                        .then(CommandManager.literal("off")
                                .executes(ctx -> offAllClear(ctx, player(ctx)))
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .suggests(BlockSuggestions.IDS)
                                        .executes(ctx -> offId(ctx, player(ctx), StringArgumentType.getString(ctx, "id")))))));
    }

    private static String player(CommandContext<ServerCommandSource> ctx) {
        return StringArgumentType.getString(ctx, "player");
    }

    /** /cb guess settings — open the shared Guess Settings screen (Pose tab) for the running op. */
    private static int openSettings(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            ServerPlayNetworking.send(p, new OpenGuiPayload(GuiMode.GUESS_SETTINGS.id, ""));
            return 1;
        }
        Chat.error(src, "Run /cb guess settings as a player — it opens a screen.");
        return 0;
    }

    // ── Handlers ────────────────────────────────────────────────────────────

    private static int onAll(CommandContext<ServerCommandSource> ctx, String name) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity target = resolve(src, name);
        if (target == null) return 0;
        GuessModeStore.setAll(target.getUuid(), true);
        broadcast(src);
        Chat.success(src, "Guess mode ON for " + name + " — blinding EVERY custom block. " + describe(target.getUuid()));
        return 1;
    }

    /** /cb guess &lt;player&gt; on &lt;id&gt; [look &lt;lookId&gt;]. lookId null = flag only (use the default look). */
    private static int onId(CommandContext<ServerCommandSource> ctx, String name, String id, String lookId) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity target = resolve(src, name);
        if (target == null) return 0;
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        String lookMsg = "";
        if (lookId == null) {
            GuessModeStore.addId(target.getUuid(), d.customId());
        } else {
            SlotData look = SlotManager.getById(lookId);
            if (look == null) {
                Chat.error(src, "There's no look block called \"" + lookId + "\". Check /cb list for the right id.");
                return 0;
            }
            GuessModeStore.setLook(target.getUuid(), d.customId(), look.customId());
            lookMsg = " disguised as \"" + look.displayName() + "\" (" + look.customId() + ")";
        }
        broadcast(src);
        Chat.success(src, "Guess mode ON for " + name + " — blinding \"" + d.displayName() + "\" (" + d.customId()
                + ")" + lookMsg + ". " + describe(target.getUuid()));
        return 1;
    }

    /** Bare /cb guess <player> off — clear everything. */
    private static int offAllClear(CommandContext<ServerCommandSource> ctx, String name) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity target = resolve(src, name);
        if (target == null) return 0;
        GuessModeStore.clear(target.getUuid());
        broadcast(src);
        Chat.success(src, "Guess mode OFF for " + name + " — cleared all blocks and all-mode.");
        return 1;
    }

    /** /cb guess <player> off <blockid> (or "all" → turn all-mode off but keep specific ids). */
    private static int offId(CommandContext<ServerCommandSource> ctx, String name, String id) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity target = resolve(src, name);
        if (target == null) return 0;
        UUID uuid = target.getUuid();
        if (ALL.equalsIgnoreCase(id)) {
            GuessModeStore.setAll(uuid, false);
            broadcast(src);
            Chat.success(src, "All-mode OFF for " + name + " (specific flagged blocks kept). " + describe(uuid));
            return 1;
        }
        GuessModeStore.removeId(uuid, id);
        broadcast(src);
        Chat.success(src, "Stopped blinding \"" + id + "\" for " + name + ". " + describe(uuid));
        return 1;
    }

    private static int status(CommandContext<ServerCommandSource> ctx, String name) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity target = resolve(src, name);
        if (target == null) return 0;
        Chat.info(src, "Guess mode for " + name + ": " + describe(target.getUuid()));
        return 1;
    }

    // ── Global default disguise look (v3 Phase 1) ─────────────────────────────

    private static int defaultLookShow(CommandContext<ServerCommandSource> ctx) {
        Chat.info(ctx.getSource(), "Default disguise look: " + defaultLookLabel());
        return 1;
    }

    private static int defaultLookSet(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        GuessModeStore.setDefaultLook(d.customId());
        broadcast(src);
        Chat.success(src, "Default disguise look set to \"" + d.displayName() + "\" (" + d.customId()
                + "). Any flagged block without its own look now shows this.");
        return 1;
    }

    private static int defaultLookClear(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        GuessModeStore.setDefaultLook(null);
        broadcast(src);
        Chat.success(src, "Default disguise look cleared — flagged blocks fall back to the bundled \"?\" cube.");
        return 1;
    }

    /** Human label for the global default look (id + name if it still resolves), or "(none → bundled ?)". */
    private static String defaultLookLabel() {
        String id = GuessModeStore.defaultLook();
        if (id == null) return "(none → bundled \"?\")";
        SlotData d = SlotManager.getById(id);
        return d == null ? id + " " + CbFmt.BAD + "(deleted → bundled \"?\")" + CbFmt.DIM : "\"" + d.displayName() + "\" (" + id + ")";
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Resolve an online player by name, or send the standard "must be online" error and return null. */
    private static ServerPlayerEntity resolve(ServerCommandSource src, String name) {
        MinecraftServer s = src.getServer();
        ServerPlayerEntity p = s == null ? null : s.getPlayerManager().getPlayer(name);
        if (p == null) Chat.error(src, "No online player named \"" + name + "\". They must be online to set guess mode.");
        return p;
    }

    /** A one-line human summary of a player's current guess-mode settings. */
    private static String describe(UUID uuid) {
        boolean active = GuessModeStore.isActive(uuid);
        boolean all = GuessModeStore.isAll(uuid);
        List<String> ids = GuessModeStore.ids(uuid);
        String idPart;
        if (ids.isEmpty()) {
            idPart = "(none)";
        } else {
            List<String> parts = new java.util.ArrayList<>();
            for (String id : ids) {
                String look = GuessModeStore.lookFor(uuid, id);
                parts.add(look == null ? id : id + "→" + look);   // id→lookOverride when set
            }
            idPart = String.join(", ", parts);
        }
        // §H rides the same readout so one command answers both directions of the disguise.
        String placed = com.customblocks.core.PlacedMaskStore.isRunning(uuid)
                ? CbFmt.OK + "ON" + CbFmt.DIM + " (" + com.customblocks.core.PlacedMaskStore.count(uuid) + " hidden)"
                : "off (" + com.customblocks.core.PlacedMaskStore.count(uuid) + " hidden)";
        return (active ? CbFmt.OK + "ON" + CbFmt.DIM : CbFmt.BAD + "OFF" + CbFmt.DIM)
                + " · all=" + (all ? CbFmt.OK + "yes" + CbFmt.DIM : "no")
                + " · blocks=[" + idPart + "]"
                + " · default look=" + defaultLookLabel()
                + " · placed mask=" + placed;
    }

    private static void broadcast(ServerCommandSource src) {
        GuessSync.broadcast(src.getServer());
    }
}
