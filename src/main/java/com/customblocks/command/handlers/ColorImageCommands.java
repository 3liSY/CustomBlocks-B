/**
 * ColorImageCommands.java — Group 10 (Color & Image Tools), Slices 1–2.
 *
 *   /cb resize <id> <16-512>          — resample a block's texture to a new resolution. Re-renders from the
 *                                       saved SOURCE image when there is one (lossless), else the baked PNG.
 *   /cb exportpng <id>                — write a block's current texture to config/customblocks/cloud_exports/<id>.png.
 *   /cb gradient <id1> <id2> <steps>  — create <steps> solid blocks Lab-interpolated between the two blocks'
 *                                       average colours; the whole batch is ONE /cb undo.
 *   /cb gradientpick <a|b> <id>       — pick a block whose average colour becomes endpoint A or B in the
 *                                       Gradient Builder GUI.
 *
 * /cb dress has been removed (redundant with Colour Variants).
 *
 * All reuse the tested image pipeline + the off-thread-bake / on-server-thread-pack-rebuild idiom from
 * /cb retexture — colour maths live in ColorMath. The rest of Group 10 (bgstudio, palette, variants,
 * AI/live/eyedrop) lands in later slices.
 *
 * Depends on: SlotManager, SlotData, TextureStore, ImageProcessor, BackgroundRemover, ColorMath, ColorLibrary,
 *             UndoManager, LockManager, ResourcePackServer, CustomBlocksConfig, IncidentRecorder, Chat,
 *             BlockSuggestions, GradientSession.
 * Called by:  CommandRegistrar.
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.gui.chest.GradientSession;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ColorMath;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ColorImageCommands {

    private ColorImageCommands() {} // static-only

    private static final Path EXPORT_DIR = Path.of("config/customblocks/cloud_exports");
    private static final Pattern UNSAFE = Pattern.compile("[^A-Za-z0-9_-]");

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb resize <id> <px> — px restricted to a sane range; 64/128/256 suggested.
        root.then(CommandManager.literal("resize")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("px", IntegerArgumentType.integer(16, 512))
                                .suggests((c, b) -> { b.suggest(64); b.suggest(128); b.suggest(256); return b.buildFuture(); })
                                .executes(ctx -> resize(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id"),
                                        IntegerArgumentType.getInteger(ctx, "px"))))));

        // /cb exportpng <id>
        root.then(CommandManager.literal("exportpng")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> exportPng(ctx.getSource(), StringArgumentType.getString(ctx, "id")))));

        // /cb gradient — no args opens the Gradient Builder GUI; the 3-arg form stays for power users.
        root.then(CommandManager.literal("gradient")
                .executes(ctx -> openGradient(ctx.getSource()))
                .then(CommandManager.argument("id1", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("id2", StringArgumentType.word())
                                .suggests(BlockSuggestions.IDS)
                                .then(CommandManager.argument("steps", IntegerArgumentType.integer(1, 32))
                                        .suggests((c, b) -> { b.suggest(4); b.suggest(6); b.suggest(8); return b.buildFuture(); })
                                        .executes(ctx -> gradient(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id1"),
                                                StringArgumentType.getString(ctx, "id2"),
                                                IntegerArgumentType.getInteger(ctx, "steps")))))));

        // /cb gradientpick <a|b> <id> — set gradient endpoint from a block's average colour.
        root.then(CommandManager.literal("gradientpick")
                .then(CommandManager.literal("a")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests(BlockSuggestions.IDS)
                                .executes(ctx -> gradientPick(ctx.getSource(), "a",
                                        StringArgumentType.getString(ctx, "id")))))
                .then(CommandManager.literal("b")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests(BlockSuggestions.IDS)
                                .executes(ctx -> gradientPick(ctx.getSource(), "b",
                                        StringArgumentType.getString(ctx, "id"))))));
    }

    /** The acting player's UUID, or null for console/command-block (those aren't undoable). */
    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }

    // ── /cb gradient (no args) → open the Gradient Builder GUI ──────────────────
    private static int openGradient(ServerCommandSource src) {
        ServerPlayerEntity p = src.getEntity() instanceof ServerPlayerEntity sp ? sp : null;
        if (p == null) { Chat.error(src, "Only a player can open the Gradient Builder."); return 0; }
        GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.GRADIENT_PICKER));
        return 1;
    }

    // ── /cb resize ────────────────────────────────────────────────────────────
    private static int resize(ServerCommandSource src, String id, int px) {
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.error(src, "\"" + id + "\" is locked. Use /cb unlock " + id + " to edit it.");
            return 0;
        }
        if (!TextureStore.has(d.index()) && TextureStore.loadSource(d.index()) == null) {
            Chat.error(src, "\"" + id + "\" has no texture to resize. Give it one with /cb retexture " + id + " <url>.");
            return 0;
        }
        final int index = d.index();
        final String mode = CustomBlocksConfig.backgroundMode;
        final int tol = CustomBlocksConfig.backgroundTolerance;
        Chat.info(src, "Resizing \"" + id + "\" to §e" + px + "×" + px + "px§r…");
        Thread worker = new Thread(() -> {
            try {
                byte[] source = TextureStore.loadSource(index);
                byte[] png;
                if (source != null && source.length > 0) {
                    byte[] cleaned = BackgroundRemover.apply(source, mode, tol);
                    png = ImageProcessor.toBlockPng(cleaned, px);
                    png = BackgroundRemover.snapBackgroundBlack(png, mode, tol);
                } else {
                    byte[] baked = TextureStore.load(index); // no source → resample the baked pixels
                    png = ImageProcessor.toBlockPng(baked, px);
                }
                TextureStore.save(index, png);
                server.execute(() -> {
                    ResourcePackServer.updatePack();
                    ResourcePackServer.syncToAll();
                    Chat.success(src, "\"" + id + "\" texture resized to §e" + px + "×" + px + "px§r"
                            + (source != null ? " §7(re-rendered from its saved image)." : "."));
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                IncidentRecorder.record("Resize failed for \"" + id + "\" to " + px + "px", e);
                server.execute(() -> Chat.error(src, "Couldn't resize that texture. " + msg));
            }
        }, "CustomBlocks-Resize");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }

    // ── /cb exportpng ─────────────────────────────────────────────────────────
    private static int exportPng(ServerCommandSource src, String id) {
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        byte[] tex = TextureStore.load(d.index());
        if (tex == null || tex.length == 0) {
            Chat.error(src, "\"" + id + "\" has no texture to export yet.");
            return 0;
        }
        try {
            Files.createDirectories(EXPORT_DIR);
            String file = UNSAFE.matcher(id).replaceAll("_") + ".png";
            Path out = EXPORT_DIR.resolve(file);
            Path tmp = EXPORT_DIR.resolve(file + ".tmp");
            Files.write(tmp, tex);
            Files.move(tmp, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Path abs = out.toAbsolutePath();
            Chat.success(src, "Exported \"" + id + "\" texture → §f" + EXPORT_DIR + "/" + file);
            src.sendFeedback(() -> Text.literal("  §8" + abs), false);
            // Clickable [download] link — opens the PNG over the mod's localhost HTTP server.
            String url = ResourcePackServer.getPngUrl(UNSAFE.matcher(id).replaceAll("_"));
            src.sendFeedback(() -> Text.literal("  ")
                    .append(Text.literal("§b[download]").styled(st -> st
                            .withClickEvent(new net.minecraft.text.ClickEvent(
                                    net.minecraft.text.ClickEvent.Action.OPEN_URL, url))
                            .withHoverEvent(new net.minecraft.text.HoverEvent(
                                    net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.literal(url))))), false);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            IncidentRecorder.record("Export PNG failed for \"" + id + "\"", e);
            Chat.error(src, "Couldn't export that texture. " + msg);
            return 0;
        }
        return 1;
    }

    // ── /cb gradient ──────────────────────────────────────────────────────────
    private static int gradient(ServerCommandSource src, String id1, String id2, int steps) {
        MinecraftServer server = src.getServer();
        if (server == null) return 0;
        SlotData a = SlotManager.getById(id1);
        if (a == null) {
            Chat.error(src, "There's no block called \"" + id1 + "\". Check /cb list for the right id.");
            return 0;
        }
        SlotData b = SlotManager.getById(id2);
        if (b == null) {
            Chat.error(src, "There's no block called \"" + id2 + "\". Check /cb list for the right id.");
            return 0;
        }
        final byte[] texA = TextureStore.load(a.index());
        final byte[] texB = TextureStore.load(b.index());
        if (texA == null || texA.length == 0) {
            Chat.error(src, "\"" + id1 + "\" has no texture — give it one before making a gradient.");
            return 0;
        }
        if (texB == null || texB.length == 0) {
            Chat.error(src, "\"" + id2 + "\" has no texture — give it one before making a gradient.");
            return 0;
        }
        final UUID who = actor(src);
        final int size = CustomBlocksConfig.textureSize;
        final int n = steps;
        Chat.info(src, "Generating §e" + n + "§r gradient block(s) between \"" + id1 + "\" and \"" + id2 + "\"…");
        Thread worker = new Thread(() -> {
            try {
                int rgbA = ColorMath.averageColor(texA);
                int rgbB = ColorMath.averageColor(texB);
                // Bake the swatches off-thread; the colours sit strictly BETWEEN the endpoints.
                List<byte[]> pngs = new ArrayList<>();
                for (int k = 1; k <= n; k++) {
                    double tStep = (double) k / (n + 1);
                    pngs.add(ColorMath.solidPng(ColorMath.labLerp(rgbA, rgbB, tStep), size));
                }
                server.execute(() -> {
                    List<UndoManager.Op> children = new ArrayList<>();
                    List<String> made = new ArrayList<>();
                    int next = 1;
                    for (byte[] png : pngs) {
                        String gid;
                        do { gid = "gradient_" + (next++); } while (SlotManager.hasId(gid));
                        SlotData created = SlotManager.create(gid, "Gradient " + gid.substring("gradient_".length()));
                        if (created == null) break; // slot pool exhausted
                        TextureStore.save(created.index(), png);
                        children.add(new UndoManager.Op(UndoManager.Kind.CREATE, null, created, null, "create"));
                        made.add(gid);
                    }
                    ResourcePackServer.updatePack();
                    ResourcePackServer.syncToAll();
                    if (children.isEmpty()) {
                        Chat.error(src, "Couldn't create gradient blocks — every slot is in use.");
                        return;
                    }
                    UndoManager.recordBatch(who, children, "gradient (" + children.size() + ")");
                    Chat.success(src, "Created §a" + made.size() + "§r gradient block(s): §f"
                            + String.join(", ", made) + "§r. §7One /cb undo removes them all.");
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                IncidentRecorder.record("Gradient failed for \"" + id1 + "\" → \"" + id2 + "\" (" + n + ")", e);
                server.execute(() -> Chat.error(src, "Couldn't build that gradient. " + msg));
            }
        }, "CustomBlocks-Gradient");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }

    // ── /cb gradientpick ──────────────────────────────────────────────────────
    private static int gradientPick(ServerCommandSource src, String endpoint, String id) {
        ServerPlayerEntity p = src.getEntity() instanceof ServerPlayerEntity sp ? sp : null;
        if (p == null) { Chat.error(src, "Only a player can pick gradient endpoints."); return 0; }
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        byte[] tex = TextureStore.load(d.index());
        if (tex == null || tex.length == 0) {
            Chat.error(src, "\"" + id + "\" has no texture yet.");
            return 0;
        }
        try {
            int avg = ColorMath.averageColor(tex);
            String hex = String.format(Locale.ROOT, "#%06X", avg & 0xFFFFFF);
            if ("a".equalsIgnoreCase(endpoint)) {
                GradientSession.setColorA(p.getUuid(), hex, id);
            } else {
                GradientSession.setColorB(p.getUuid(), hex, id);
            }
            Chat.success(src, "Set gradient colour " + endpoint.toUpperCase(Locale.ROOT)
                    + " to §f" + hex + "§r (from \"" + id + "\").");
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.GRADIENT_PICKER));
        } catch (Exception e) {
            Chat.error(src, "Couldn't read that block's colour.");
        }
        return 1;
    }
}
