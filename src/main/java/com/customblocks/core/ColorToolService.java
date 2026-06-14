/**
 * ColorToolService.java — Group 10 (Color & Image Tools), shared server-side flows.
 *
 * Responsibility: the three reusable texture operations the new Group 10 GUIs and commands all
 * drive, each following the project idiom (fast checks on the server thread, the pixel work on a
 * daemon worker, then back to the server thread for ONE pack rebuild + sync + undo record + chat):
 *
 *   applyBgRemoval — re-run background removal on an EXISTING block at a chosen mode/tolerance
 *                    (what /cb bgstudio + /cb tolerance commit). Re-renders from the saved source
 *                    when there is one, else operates on the baked PNG. Undoable (TEXTURE op).
 *   createVariant  — bake an HSL-shifted copy of a block into a NEW block (the Colour Variants
 *                    panel swatches). Copies the source's feel; one CREATE undo per click.
 *   applyRecolor   — commit an HSL shift onto a block IN PLACE (the live recolour slider's Apply).
 *                    Undoable (TEXTURE op).
 *
 * This is the only NEW image logic Group 10 needs server-side — bgstudio's three classic modes are
 * the existing BackgroundRemover; the variants/recolour maths are ColorMath. Distinct from the
 * Group-06 ColorVariantService (that one is the shape-tool triangle/square variant flow).
 *
 * Depends on: SlotManager, SlotData, TextureStore, LockManager, UndoManager, ImageProcessor,
 *             BackgroundRemover, ColorMath, ResourcePackServer, CustomBlocksConfig, IncidentRecorder, Chat.
 * Called by:  command/handlers/ImageToolCommands, gui/chest/BgStudioMenu, gui/chest/ColorVariantsMenu,
 *             CustomBlocksMod (RecolorApplyPayload receiver — the live slider's Apply).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ColorMath;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ColorToolService {

    private ColorToolService() {} // static-only

    // ── Colour Variants panel: the fixed set of algorithmic swatches ────────────────────────────

    /** One algorithmic variant: an id-suffix key, a label, and its HSL shift (hue°, sat×, light×). */
    public record Variant(String key, String label, double hueDeg, double satFactor, double lightFactor) {}

    /** The swatches shown in the Colour Variants panel, in display order. */
    public static final List<Variant> VARIANTS = List.of(
            new Variant("lighter",   "Lighter",            0,   1.00, 1.35),
            new Variant("darker",    "Darker",             0,   1.00, 0.65),
            new Variant("vivid",     "Vivid",              0,   1.55, 1.00),
            new Variant("muted",     "Muted",              0,   0.45, 1.00),
            new Variant("complement","Complementary",      180, 1.00, 1.00),
            new Variant("split1",    "Split-Complement A", 150, 1.00, 1.00),
            new Variant("split2",    "Split-Complement B", 210, 1.00, 1.00));

    public static Variant variant(String key) {
        for (Variant v : VARIANTS) if (v.key().equalsIgnoreCase(key)) return v;
        return null;
    }

    // ── Background removal on an existing block (bgstudio / tolerance) ───────────────────────────

    /**
     * Re-run background removal on {@code id} at {@code mode}/{@code tol} and commit it. Re-renders
     * from the stored source image when present (cleanest), else operates on the baked PNG at its
     * current resolution. {@code fillRgb} (0xRRGGBB) is the colour painted where the background was
     * removed (-1 = smart auto black/white). Undoable. All feedback goes to {@code player}.
     */
    public static void applyBgRemoval(ServerPlayerEntity player, String id, String mode, int tol, int fillRgb) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.tool(player, "§cThere's no block called \"" + id + "\"."); return; }
        if (LockManager.isLocked(id)) {
            Chat.tool(player, "§c\"" + id + "\" is locked. /cb unlock " + id + " to edit it."); return;
        }
        final byte[] before = TextureStore.load(d.index());
        final byte[] source = TextureStore.loadSource(d.index());
        if ((before == null || before.length == 0) && (source == null || source.length == 0)) {
            Chat.tool(player, "§c\"" + id + "\" has no texture yet — give it one with /cb retexture " + id + " <url>.");
            return;
        }
        final int index = d.index();
        final SlotData slot = d;
        final UUID who = player.getUuid();
        final String m = BackgroundRemover.normalize(mode);
        final int t = Math.max(0, Math.min(100, tol));
        final int fill = fillRgb;
        Chat.tool(player, "§7Applying §f" + BackgroundRemover.displayName(m) + " §7(" + t + "%) to \"" + id + "\"…");
        Thread worker = new Thread(() -> {
            try {
                byte[] png;
                if (source != null && source.length > 0) {
                    int size = before != null ? sizeOf(before, CustomBlocksConfig.textureSize) : CustomBlocksConfig.textureSize;
                    byte[] cleaned = fill >= 0
                            ? BackgroundRemover.apply(source, m, t, fill)
                            : BackgroundRemover.apply(source, m, t);
                    png = ImageProcessor.toBlockPng(cleaned, size);
                    png = fill >= 0
                            ? BackgroundRemover.snapBackgroundColor(png, m, t, fill)
                            : BackgroundRemover.snapBackgroundBlack(png, m, t);
                } else {
                    png = fill >= 0
                            ? BackgroundRemover.apply(before, m, t, fill)
                            : BackgroundRemover.apply(before, m, t);
                }
                final byte[] after = png;
                server.execute(() -> {
                    TextureStore.save(index, after);
                    ResourcePackServer.updatePack();
                    ResourcePackServer.syncToAll();
                    UndoManager.recordTexture(who, slot, before, after, "background");
                    Chat.tool(player, "§a\"" + id + "\" background updated §7(" + BackgroundRemover.displayName(m)
                            + "). /cb undo to revert.");
                });
            } catch (Exception e) {
                IncidentRecorder.record("bgstudio apply failed for \"" + id + "\" (" + m + " @ " + t + ")", e);
                server.execute(() -> Chat.tool(player, "§cCouldn't update that background — texture left unchanged."));
            }
        }, "CustomBlocks-BgStudio");
        worker.setDaemon(true);
        worker.start();
    }

    /** Convenience: smart fill (auto black/white). */
    public static void applyBgRemoval(ServerPlayerEntity player, String id, String mode, int tol) {
        applyBgRemoval(player, id, mode, tol, -1);
    }

    // ── Colour variant → a brand-new block (Colour Variants panel swatch click) ─────────────────

    /** Create a new block that is {@code srcId} shifted by the {@code variantKey} preset. */
    public static void createVariant(ServerPlayerEntity player, String srcId, String variantKey) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        Variant v = variant(variantKey);
        if (v == null) { Chat.tool(player, "§cUnknown variant \"" + variantKey + "\"."); return; }
        SlotData src = SlotManager.getById(srcId);
        if (src == null) { Chat.tool(player, "§cThere's no block called \"" + srcId + "\"."); return; }
        byte[] source = TextureStore.loadSource(src.index());
        final byte[] input = (source != null && source.length > 0) ? source : TextureStore.load(src.index());
        if (input == null || input.length == 0) {
            Chat.tool(player, "§c\"" + srcId + "\" has no texture to make a variant from.");
            return;
        }
        // Pick a free id: "<src>_<key>", then "_2", "_3"… so repeated clicks don't collide.
        String baseVid = srcId + "_" + v.key();
        String vid = baseVid;
        for (int n = 2; SlotManager.hasId(vid); n++) vid = baseVid + "_" + n;

        final boolean hadSource = source != null && source.length > 0;
        final String finalVid = vid;
        final int size = CustomBlocksConfig.textureSize;
        final UUID who = player.getUuid();
        Chat.tool(player, "§7Creating §f" + finalVid + " §7(" + v.label() + ")…");
        Thread worker = new Thread(() -> {
            try {
                byte[] shifted = ColorMath.hslShift(input, v.hueDeg(), v.satFactor(), v.lightFactor());
                byte[] png = ImageProcessor.toBlockPng(shifted, size);
                server.execute(() -> {
                    SlotData created = SlotManager.create(finalVid, src.displayName() + " (" + v.label() + ")");
                    if (created == null) { Chat.tool(player, "§cCouldn't create \"" + finalVid + "\" — every slot is in use."); return; }
                    SlotData copied = new SlotData(created.index(), created.customId(), created.displayName(),
                            src.glow(), src.hardness(), src.soundType(), src.noCollision(), src.category());
                    SlotManager.restoreSnapshot(copied);
                    TextureStore.save(copied.index(), png);
                    if (hadSource) TextureStore.saveSource(copied.index(), shifted); // stays re-renderable
                    UndoManager.recordCreate(who, copied);
                    ResourcePackServer.updatePack();
                    ResourcePackServer.syncToAll();
                    var item = SlotManager.itemAt(copied.index());
                    if (item != null) player.giveItemStack(new ItemStack(item));
                    Chat.tool(player, "§a\"" + finalVid + "\" created §7(" + v.label() + "). /cb undo removes it.");
                });
            } catch (Exception e) {
                IncidentRecorder.record("Colour variant failed for \"" + srcId + "\" (" + v.key() + ")", e);
                server.execute(() -> Chat.tool(player, "§cCouldn't make that variant."));
            }
        }, "CustomBlocks-ColorVariantImg");
        worker.setDaemon(true);
        worker.start();
    }

    // ── Live recolour commit (the slider's Apply) ───────────────────────────────────────────────

    /** Commit an HSL shift onto {@code id} in place (live recolour Apply). Undoable. */
    public static void applyRecolor(ServerPlayerEntity player, String id,
                                    double hueDeg, double satFactor, double lightFactor) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.tool(player, "§cThere's no block called \"" + id + "\"."); return; }
        if (LockManager.isLocked(id)) {
            Chat.tool(player, "§c\"" + id + "\" is locked. /cb unlock " + id + " to edit it."); return;
        }
        final byte[] before = TextureStore.load(d.index());
        if (before == null || before.length == 0) {
            Chat.tool(player, "§c\"" + id + "\" has no texture to recolour yet."); return;
        }
        final int index = d.index();
        final SlotData slot = d;
        final UUID who = player.getUuid();
        Thread worker = new Thread(() -> {
            try {
                byte[] after = ColorMath.hslShift(before, hueDeg, satFactor, lightFactor);
                server.execute(() -> {
                    TextureStore.save(index, after);
                    ResourcePackServer.updatePack();
                    ResourcePackServer.syncToAll();
                    UndoManager.recordTexture(who, slot, before, after, "recolour");
                    Chat.tool(player, "§aRecoloured \"" + id + "\". §7/cb undo to revert.");
                });
            } catch (Exception e) {
                IncidentRecorder.record("Live recolour failed for \"" + id + "\"", e);
                server.execute(() -> Chat.tool(player, "§cCouldn't recolour that texture."));
            }
        }, "CustomBlocks-Recolor");
        worker.setDaemon(true);
        worker.start();
    }

    /** The square edge length of a baked PNG, or {@code fallback} if it can't be read. */
    private static int sizeOf(byte[] png, int fallback) {
        try {
            var img = ImageIO.read(new ByteArrayInputStream(png));
            if (img != null) return Math.max(img.getWidth(), img.getHeight());
        } catch (Exception ignored) {}
        return fallback;
    }

    /** "lighter" → "Lighter" (display only). */
    public static String capitalize(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
