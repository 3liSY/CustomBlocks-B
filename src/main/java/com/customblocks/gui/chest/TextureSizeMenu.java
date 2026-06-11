/**
 * TextureSizeMenu.java — chest sub-menu for picking the block texture resolution.
 *
 * Responsibility: list every supported size (16/32/64/128/256/512) as a clickable item whose
 * hover lore shows the average per-texture size AND the worst-case whole-pack size (per-texture
 * × the live slot capacity), so the pack-size cost is visible before choosing. The current size
 * glows. Clicking delegates to the tested `/cb config texturesize <px>` command (no mutation
 * logic duplicated here) and returns to the Config dashboard.
 *
 * Depends on: ChestMenu, Icons, GuiRouter, Nav, CustomBlocksConfig.
 * Called by: GuiRouter.build (Dest.TEXTURE_SIZE) — opened from the Config menu's Texture Quality
 * row and from `/cb config texturesize` with no argument.
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;

public final class TextureSizeMenu {

    private TextureSizeMenu() {} // static-only

    /** Supported sizes (low → high) and the matching middle slot positions in a 3-row chest. */
    private static final int[] SIZES = {16, 32, 64, 128, 256, 512};
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15};

    /** Average bytes for ONE exported texture at each size — measured from real exports, not guessed.
     *  Real images vary with detail (a flat logo packs larger than a smooth photo); these are typical
     *  mid-values, validated against the on-disk 64px textures (~3–11 KB). */
    static long avgBytes(int px) {
        return switch (px) {
            case 16  -> 1_024L;     // ~1 KB
            case 32  -> 2_048L;     // ~2 KB
            case 64  -> 6_144L;     // ~6 KB
            case 128 -> 20_480L;    // ~20 KB
            case 256 -> 81_920L;    // ~80 KB
            case 512 -> 322_560L;   // ~0.3 MB
            default  -> 6_144L;
        };
    }

    private static String look(int px) {
        return switch (px) {
            case 16  -> "very blocky";
            case 32  -> "blocky";
            case 64  -> "soft (old default)";
            case 128 -> "sharp (current default)";
            case 256 -> "very sharp";
            case 512 -> "sharpest — heavy pack";
            default  -> "";
        };
    }

    public static ChestMenu build(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("Texture Quality", 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());

        int current = CustomBlocksConfig.textureSize;
        int capacity = CustomBlocksConfig.maxSlots;

        m.set(4, Icons.glint(Items.PAINTING, "§6§lTexture Quality §f= §a" + current + "px",
                "§7Resolution for NEW textures.",
                "§7Higher = sharper, but a bigger pack.",
                "§8Existing blocks keep their size until retextured."));

        for (int i = 0; i < SIZES.length; i++) {
            int px = SIZES[i];
            boolean selected = px == current;
            long per = avgBytes(px);
            long pack = per * capacity;

            String name = (selected ? "§a§l" : "§e§l") + px + "px"
                    + (selected ? " §a(current)" : "");

            // Set the size first (so NEW blocks use it). If blocks already exist, offer to
            // retexture them (RetextureConfirmMenu); otherwise just return to Config.
            ChestMenu.Click click = (p, b, a) -> {
                if (SlotManager.usedSlots() > 0) {
                    GuiRouter.runThenNavigate(p, "config texturesize " + px,
                            MenuKey.of(Dest.RETEXTURE_CONFIRM, String.valueOf(px)));
                } else {
                    GuiRouter.runAndReopen(p, "config texturesize " + px, MenuKey.of(Dest.CONFIG));
                }
            };

            // Selected size glows; the rest are plain and clickable.
            var icon = selected
                    ? Icons.glint(Items.PAINTING, name,
                        "§7" + look(px),
                        "§7~" + human(per) + " §8per texture",
                        "§7~" + human(pack) + " §8pack §8(if all " + capacity + " textured)",
                        "§aSelected")
                    : Icons.of(Items.PAINTING, name,
                        "§7" + look(px),
                        "§7~" + human(per) + " §8per texture",
                        "§7~" + human(pack) + " §8pack §8(if all " + capacity + " textured)",
                        "§eClick §7→ use this size");
            m.set(SLOTS[i], icon, click);
        }

        m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** Bytes → a short human string: B, KB (no decimals), or MB (one decimal). */
    static String human(long bytes) {
        if (bytes >= 1024L * 1024L) return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
        if (bytes >= 1024L) return String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0);
        return bytes + " B";
    }
}
