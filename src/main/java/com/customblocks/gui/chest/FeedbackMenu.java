/**
 * FeedbackMenu.java — chest board for the merged Feedback FX (Group 16, slice 5). One master tile
 * per FX category ({@link com.customblocks.CustomBlocksConfig#FX_CATEGORIES}). Left-click a master
 * = toggle BOTH the particle and the sound for that category. Shift-click = expand it into two
 * sub-tiles (Particle / Sound) for independent control; shift-click again collapses. Right-click =
 * preview both, ignoring the toggles. Reached by /cb feedback, /cb particles, /cb sounds and
 * IT-Chest Row 6 — all the same board. The expanded category is carried in the MenuKey arg
 * ("" = all collapsed).
 *
 * Depends on: CustomBlocksConfig (toggle maps), ParticleFx + SoundFx (preview), GuiRouter/Nav, Icons
 * Called by:  GuiRouter (Dest.PARTICLES)
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.core.ParticleFx;
import com.customblocks.core.SoundFx;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FeedbackMenu {

    private FeedbackMenu() {} // static-only

    // Parallel to CustomBlocksConfig.FX_CATEGORIES.
    private static final Item[] ITEMS = {
        Items.LIME_DYE, Items.RED_DYE, Items.ENCHANTED_BOOK, Items.END_ROD,
        Items.TOTEM_OF_UNDYING, Items.FIREWORK_ROCKET
    };
    private static final String[] LABELS = {
        "Success", "Error", "Menu Clicks", "Selection", "Bulk Complete", "Achievement"
    };
    private static final String[] PARTICLE_DESC = {
        "green ring", "angry puff", "enchant glyphs", "end-rod sparkle",
        "totem ring", "firework column"
    };
    private static final String[] SOUND_DESC = {
        "XP-orb pickup", "note-block bass", "amethyst chime", "amethyst chime",
        "beacon hum", "toast ding"
    };

    // Row 2 (slots 19-24), 6 tiles — vertically centred so the master + its 2 expansion rows
    // (sub-tiles at +9 / +18 → rows 3-4) sit as a balanced block between the header and footer.
    private static final int[] MASTER = {19, 20, 21, 22, 23, 24};

    public static ChestMenu build(ServerPlayerEntity player, String expanded) {
        ChestMenu m = new ChestMenu("Feedback FX", 6).fill();
        m.set(0, Icons.accent());
        m.set(8, Icons.accent());
        m.set(4, Icons.of(Items.NETHER_STAR, "§d§lFeedback FX",
                "§7Each category plays a §fparticle §7+ a §fsound§7.",
                " ",
                "§eLeft-click §7a tile: turn BOTH on/off.",
                "§eShift-click §7a tile: split it into",
                "§7  separate Particle / Sound switches.",
                "§eRight-click §7a tile: preview both."));

        String[] cats = CustomBlocksConfig.FX_CATEGORIES;
        for (int i = 0; i < cats.length && i < MASTER.length; i++) {
            final String cat = cats[i];
            boolean pOn = CustomBlocksConfig.particlesOn(cat);
            boolean sOn = CustomBlocksConfig.soundsOn(cat);
            boolean bothOn = pOn && sOn;
            boolean anyOn = pOn || sOn;
            boolean isExpanded = cat.equals(expanded);

            String head = (bothOn ? "§a§l" : anyOn ? "§e§l" : "§7§l") + LABELS[i];
            var stack = anyOn
                ? Icons.glint(ITEMS[i], head,
                    "§7Particle: " + onOff(pOn) + " §8(" + PARTICLE_DESC[i] + ")",
                    "§7Sound: " + onOff(sOn) + " §8(" + SOUND_DESC[i] + ")",
                    " ",
                    "§eLeft-click: toggle BOTH",
                    "§eShift-click: " + (isExpanded ? "collapse" : "split into 2 switches"),
                    "§8Right-click: preview both")
                : Icons.of(Items.GRAY_DYE, head,
                    "§8Particle: off §8(" + PARTICLE_DESC[i] + ")",
                    "§8Sound: off §8(" + SOUND_DESC[i] + ")",
                    " ",
                    "§eLeft-click: turn BOTH on",
                    "§eShift-click: " + (isExpanded ? "collapse" : "split into 2 switches"),
                    "§8Right-click: preview both");

            m.set(MASTER[i], stack, (p, b, a) -> {
                if (a == SlotActionType.QUICK_MOVE) {            // shift → expand/collapse
                    GuiRouter.repage(p, MenuKey.of(Dest.PARTICLES, cat.equals(expanded) ? "" : cat));
                    return;
                }
                if (b == 1) {                                    // right → preview both
                    ParticleFx.preview(p, cat);
                    SoundFx.preview(p, cat);
                    return;
                }
                boolean now = !(CustomBlocksConfig.particlesOn(cat) && CustomBlocksConfig.soundsOn(cat));
                CustomBlocksConfig.particlesEnabled.put(cat, now);
                CustomBlocksConfig.soundsEnabled.put(cat, now);
                CustomBlocksConfig.save();
                if (now) { ParticleFx.preview(p, cat); SoundFx.preview(p, cat); }
                GuiRouter.repage(p, MenuKey.of(Dest.PARTICLES, expanded));
            });

            if (isExpanded) {
                placeSub(m, MASTER[i] + 9, Items.FIREWORK_STAR, "Particle", PARTICLE_DESC[i], pOn, cat,
                        true, expanded);
                placeSub(m, MASTER[i] + 18, Items.NOTE_BLOCK, "Sound", SOUND_DESC[i], sOn, cat,
                        false, expanded);
            }
        }

        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** One channel sub-tile under an expanded master: left-click toggles just this channel. */
    private static void placeSub(ChestMenu m, int slot, Item onItem, String channel, String desc,
                                 boolean on, String cat, boolean particle, String expanded) {
        var stack = on
            ? Icons.glint(onItem, "§f" + channel + " §8· §aON",
                "§8" + desc, " ", "§eLeft-click: turn OFF", "§8Right-click: preview")
            : Icons.of(Items.GRAY_DYE, "§7" + channel + " §8· §7OFF",
                "§8" + desc, " ", "§eLeft-click: turn ON", "§8Right-click: preview");
        m.set(slot, stack, (p, b, a) -> {
            if (b == 1) {
                if (particle) ParticleFx.preview(p, cat); else SoundFx.preview(p, cat);
                return;
            }
            boolean now;
            if (particle) {
                now = !CustomBlocksConfig.particlesOn(cat);
                CustomBlocksConfig.particlesEnabled.put(cat, now);
            } else {
                now = !CustomBlocksConfig.soundsOn(cat);
                CustomBlocksConfig.soundsEnabled.put(cat, now);
            }
            CustomBlocksConfig.save();
            if (now) { if (particle) ParticleFx.preview(p, cat); else SoundFx.preview(p, cat); }
            GuiRouter.repage(p, MenuKey.of(Dest.PARTICLES, expanded));
        });
    }

    private static String onOff(boolean on) { return on ? "§aON" : "§7off"; }
}
