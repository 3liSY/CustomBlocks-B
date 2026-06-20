/**
 * AnimCommands.java
 *
 * Responsibility: The animated-block commands (Group 14 Part A).
 *   • {@link #maybeCreateAnimated} — the create-animated rail: when /cb create's downloaded image is
 *     a multi-frame GIF/WebP, decode it to a vertical strip, store it, and mark the slot animated.
 *     Returns false (→ caller falls back to the static path) if the data isn't really animated.
 *   • /cb anim <id> — prints a clean, CLICKABLE control card (speed presets, loop, smoothing, trim).
 *   • /cb anim <id> ticks|fps|original|loop|smoothing|trim … — edit the animation's PLAIN NUMBERS,
 *     then regenerate the .mcmeta + do ONE pack rebuild. Never re-downloads (the strip is stored).
 *
 * All decode work runs OFF the server thread (the caller is already on a daemon worker); only the
 * slot mutation + pack rebuild hop back onto the server thread. Stays under the 400-line cap (§9.3).
 *
 * Depends on: AnimationDecoder, AnimData, SlotManager, TextureStore, ResourcePackServer, Chat.
 * Called by:  CommandRegistrar (register), CreationCommands.createWithTexture (maybeCreateAnimated).
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.AnimData;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.customblocks.image.AnimationDecoder;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;
import java.util.UUID;

public final class AnimCommands {

    private AnimCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // Group 14 Phase 2: /cb anim <id> opens the studio's Animation tab (edit mode); /cb anim (no id)
        // opens the block list to pick one. The typed shortcuts below stay for scripting.
        root.then(CommandManager.literal("anim")
                .executes(ctx -> openList(ctx.getSource()))
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> CreationStudioBridge.openStudioEdit(ctx.getSource(), id(ctx)))
                        .then(CommandManager.literal("ticks")
                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> setTicks(ctx.getSource(), id(ctx),
                                                IntegerArgumentType.getInteger(ctx, "ticks")))))
                        .then(CommandManager.literal("fps")
                                .then(CommandManager.argument("fps", IntegerArgumentType.integer(1, 60))
                                        .executes(ctx -> setFps(ctx.getSource(), id(ctx),
                                                IntegerArgumentType.getInteger(ctx, "fps")))))
                        .then(CommandManager.literal("original")
                                .executes(ctx -> setOriginal(ctx.getSource(), id(ctx))))
                        .then(CommandManager.literal("loop")
                                .then(CommandManager.argument("mode", StringArgumentType.word())
                                        .suggests((c, b) -> { b.suggest("loop"); b.suggest("bounce"); b.suggest("reverse"); return b.buildFuture(); })
                                        .executes(ctx -> setLoop(ctx.getSource(), id(ctx),
                                                StringArgumentType.getString(ctx, "mode")))))
                        .then(CommandManager.literal("smoothing")
                                .then(CommandManager.argument("onoff", StringArgumentType.word())
                                        .suggests((c, b) -> { b.suggest("on"); b.suggest("off"); return b.buildFuture(); })
                                        .executes(ctx -> setSmoothing(ctx.getSource(), id(ctx),
                                                StringArgumentType.getString(ctx, "onoff")))))
                        .then(CommandManager.literal("trim")
                                .then(CommandManager.argument("start", IntegerArgumentType.integer(0))
                                        .then(CommandManager.argument("end", IntegerArgumentType.integer(0))
                                                .executes(ctx -> setTrim(ctx.getSource(), id(ctx),
                                                        IntegerArgumentType.getInteger(ctx, "start"),
                                                        IntegerArgumentType.getInteger(ctx, "end"))))))));
    }

    private static String id(CommandContext<ServerCommandSource> ctx) {
        return StringArgumentType.getString(ctx, "id");
    }

    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }

    private static void syncHud(ServerCommandSource src) {
        if (src.getEntity() instanceof ServerPlayerEntity p) HudSync.sendTo(p);
    }

    // ── create-animated rail (called from CreationCommands' download worker) ───

    /**
     * If {@code raw} is a real multi-frame image, decode it to a strip, create the block, mark it
     * animated, and rebuild the pack — then return true. Returns false when the data isn't actually
     * animated (or decodes to a single frame), so the caller can take the normal static path.
     * Runs on the caller's daemon worker thread; the slot mutation hops back to the server thread.
     */
    public static boolean maybeCreateAnimated(ServerCommandSource src, String id, String name,
                                              byte[] raw, MinecraftServer server) {
        if (!AnimationDecoder.isAnimated(raw)) return false;
        // Atlas strip → cap at 256 so a 512 config can't overflow the block atlas (the muffle); see ADR-007/008.
        int atlasSize = Math.min(AnimationDecoder.ATLAS_MAX_SIZE, CustomBlocksConfig.textureSize);
        AnimationDecoder.Decoded dec = AnimationDecoder.decode(raw, atlasSize);
        if (dec == null || dec.frameCount() <= 1) return false; // not usably animated → static fallback

        server.execute(() -> {
            // Re-check on the server thread — the id could have been taken while we were decoding.
            SlotData created = SlotManager.create(id, name);
            if (created == null) {
                Chat.error(src, "Couldn't create \"" + id + "\" — the id was taken or every slot is "
                        + "in use. Nothing was created.");
                return;
            }
            TextureStore.save(created.index(), dec.stripPng());
            TextureStore.saveSource(created.index(), raw); // keep the original GIF/WebP for later re-decode (Part B)
            AnimData anim = AnimData.ofDecoded(dec.frameCount(), dec.frameTimes(), dec.transparency());
            SlotManager.setAnim(id, anim);
            SlotData full = SlotManager.getById(id);
            UndoManager.recordCreate(actor(src), full != null ? full : created);
            ResourcePackServer.updatePack();
            Chat.success(src, "Animated block \"" + id + "\" created (" + dec.frameCount() + " frames)"
                    + (CustomBlocksConfig.silentPack ? " — it'll play in a moment."
                    : " — accept the resource pack prompt to see it."));
            if (dec.warning() != null) Chat.info(src, dec.warning());
            syncHud(src);
        });
        return true;
    }

    // ── /cb anim — open the GUI (Group 14 Phase 2 replaced the chat card) ──────

    /** /cb anim (no id) — open the ANIMATED-ONLY block list; clicking one opens the studio's Animation
     *  tab for that block (Group 14 Phase 2 open fix — was the full block list with the wrong click). */
    private static int openList(ServerCommandSource src) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "Run /cb anim <id> to edit a block's animation in the studio.");
            return 0;
        }
        GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.ANIM_LIST));
        return 1;
    }

    // ── /cb anim <id> … editors (no re-download — strip already stored) ───────

    private static int setTicks(ServerCommandSource src, String id, int ticks) {
        return edit(src, id, a -> a.withUniform(ticks),
                "Speed set to §e" + ticks + "§r ticks/frame (" + fmt(20.0 / ticks) + " fps).");
    }

    private static int setFps(ServerCommandSource src, String id, int fps) {
        int ticks = Math.max(1, Math.round(20f / fps));
        return edit(src, id, a -> a.withUniform(ticks),
                "Speed set to §e" + fps + "§r fps (" + ticks + " ticks/frame).");
    }

    private static int setOriginal(ServerCommandSource src, String id) {
        return edit(src, id, AnimData::withMatchOriginal, "Speed back to the clip's §eoriginal§r timing.");
    }

    private static int setLoop(ServerCommandSource src, String id, String mode) {
        String m = AnimData.normalizeLoop(mode);
        return edit(src, id, a -> a.withLoopMode(m), "Loop mode set to §e" + m + "§r.");
    }

    private static int setSmoothing(ServerCommandSource src, String id, String onoff) {
        boolean on = "on".equalsIgnoreCase(onoff) || "true".equalsIgnoreCase(onoff);
        return edit(src, id, a -> a.withInterpolate(on), "Smoothing turned §e" + (on ? "on" : "off") + "§r.");
    }

    private static int setTrim(ServerCommandSource src, String id, int start, int end) {
        SlotData d = SlotManager.getById(id);
        if (d == null) { notFound(src, id); return 0; }
        if (!d.isAnimated()) { notAnimated(src, id); return 0; }
        int last = d.anim().frameCount() - 1;
        if (start > last || end > last) {
            Chat.error(src, "This block has frames 0–" + last + ". Pick a range inside that.");
            return 0;
        }
        return edit(src, id, a -> a.withTrim(start, end),
                "Trimmed to frames §e" + Math.min(start, end) + "–" + Math.max(start, end) + "§r.");
    }

    /** Shared edit rail: validate, apply the immutable change, rebuild the pack ONCE, report. */
    private static int edit(ServerCommandSource src, String id,
                            java.util.function.UnaryOperator<AnimData> change, String okMsg) {
        SlotData d = SlotManager.getById(id);
        if (d == null) { notFound(src, id); return 0; }
        if (!d.isAnimated()) { notAnimated(src, id); return 0; }
        if (LockManager.isLocked(id)) {
            Chat.error(src, "\"" + id + "\" is locked. Use /cb unlock " + id + " to edit it.");
            return 0;
        }
        SlotData before = d;
        AnimData updated = change.apply(d.anim());
        SlotData after = SlotManager.setAnim(id, updated);
        if (after == null) { notFound(src, id); return 0; }
        UndoManager.recordModify(actor(src), before, after, "anim");
        ResourcePackServer.updatePack();
        Chat.success(src, okMsg + (CustomBlocksConfig.silentPack ? "" : " Accept the pack prompt to see it."));
        return 1;
    }

    /** Format an fps value: whole numbers plain, otherwise one decimal. */
    private static String fmt(double fps) {
        return fps == Math.rint(fps) ? String.valueOf((int) fps)
                : String.format(Locale.ROOT, "%.1f", fps);
    }

    private static void notFound(ServerCommandSource src, String id) {
        Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
    }

    private static void notAnimated(ServerCommandSource src, String id) {
        Chat.error(src, "\"" + id + "\" isn't an animated block. Make one with /cb create <id> <name> <gif-url>.");
    }
}
