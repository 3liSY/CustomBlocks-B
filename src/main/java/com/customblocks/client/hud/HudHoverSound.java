/**
 * HudHoverSound.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: the look-at feedback sound. A sound plays ONCE when the crosshair newly
 * lands on a qualifying block (edge-triggered, not every frame), per the configured trigger
 * (None / Custom blocks only / Any block), sound choice (17 vanilla options) and volume.
 * Also exposes preview() for the editor's "preview on pick". State (trigger/sound/volume)
 * lives in HudConfig.
 *
 * Sound note (NFR-12): only BLOCK_NOTE_BLOCK_* constants are RegistryEntry and need .value();
 * every other SoundEvents constant here is a bare SoundEvent. The verifySound gate enforces it.
 *
 * Depends on: HudConfig, SlotBlock, vanilla SoundEvents.
 * Called by: CustomBlocksClient (client tick → tick()), HudEditorScreen (sound dropdown → preview()).
 */
package com.customblocks.client.hud;

import com.customblocks.block.SlotBlock;
import com.customblocks.client.HudConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class HudHoverSound {

    private HudHoverSound() {}

    /** Trigger modes (stored in HudConfig.hoverTrigger). */
    public static final int TRIGGER_NONE = 0, TRIGGER_CUSTOM = 1, TRIGGER_ANY = 2;

    /** Stable keys (persisted) — index-aligned with LABELS. */
    public static final String[] KEYS = {
            "none", "bamboo", "stone", "wood", "amethyst", "note_pling", "glass", "bell",
            "wool", "copper", "sand", "grass", "nether", "froglight", "sculk", "lodestone", "anvil"
    };
    public static final String[] LABELS = {
            "None", "Bamboo", "Stone", "Wood", "Amethyst", "Note Pling", "Glass", "Bell",
            "Wool", "Copper", "Sand", "Grass", "Nether", "Froglight", "Sculk", "Lodestone", "Anvil"
    };

    private static BlockPos last = null;   // last qualifying block (edge-trigger state)

    /** Map a key to its vanilla SoundEvent (or null for "none" / unknown). */
    private static SoundEvent eventFor(String key) {
        return switch (key == null ? "none" : key) {
            case "bamboo"     -> SoundEvents.BLOCK_BAMBOO_PLACE;
            case "stone"      -> SoundEvents.BLOCK_STONE_HIT;
            case "wood"       -> SoundEvents.BLOCK_WOOD_HIT;
            case "amethyst"   -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
            case "note_pling" -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();  // RegistryEntry → .value()
            case "glass"      -> SoundEvents.BLOCK_GLASS_HIT;
            case "bell"       -> SoundEvents.BLOCK_BELL_USE;
            case "wool"       -> SoundEvents.BLOCK_WOOL_HIT;
            case "copper"     -> SoundEvents.BLOCK_COPPER_HIT;
            case "sand"       -> SoundEvents.BLOCK_SAND_HIT;
            case "grass"      -> SoundEvents.BLOCK_GRASS_HIT;
            case "nether"     -> SoundEvents.BLOCK_NETHERRACK_HIT;
            case "froglight"  -> SoundEvents.BLOCK_FROGLIGHT_HIT;
            case "sculk"      -> SoundEvents.BLOCK_SCULK_HIT;
            case "lodestone"  -> SoundEvents.BLOCK_LODESTONE_HIT;
            case "anvil"      -> SoundEvents.BLOCK_ANVIL_LAND;
            default           -> null;   // "none"
        };
    }

    /** Per-tick edge check: play once when the crosshair newly lands on a qualifying block. */
    public static void tick(MinecraftClient client) {
        if (HudConfig.hoverTrigger == TRIGGER_NONE || client == null || client.world == null || client.player == null) {
            last = null;
            return;
        }
        BlockPos pos = null;
        boolean custom = false;
        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            pos = bh.getBlockPos();
            custom = client.world.getBlockState(pos).getBlock() instanceof SlotBlock;
        }
        boolean qualifies = pos != null
                && (HudConfig.hoverTrigger == TRIGGER_ANY || (HudConfig.hoverTrigger == TRIGGER_CUSTOM && custom));

        if (qualifies) {
            if (!pos.equals(last)) { playCurrent(client); last = pos.toImmutable(); }
        } else {
            last = null;
        }
    }

    /** Play the configured sound once at the configured volume (used by tick + preview). */
    private static void playCurrent(MinecraftClient client) {
        SoundEvent ev = eventFor(HudConfig.hoverSound);
        float vol = HudConfig.hoverVolume / 100f;
        if (ev == null || vol <= 0f || client == null) return;
        client.getSoundManager().play(PositionedSoundInstance.master(ev, 1.0f, vol));
    }

    /** Preview-on-pick: play the currently selected sound once, ignoring the trigger mode. */
    public static void preview() {
        playCurrent(MinecraftClient.getInstance());
    }

    // ── Editor helpers ─────────────────────────────────────────────────────
    public static String triggerLabel(int t) {
        return switch (t) { case TRIGGER_CUSTOM -> "Custom"; case TRIGGER_ANY -> "Any"; default -> "None"; };
    }

    public static int soundIndex(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return i;
        return 0;
    }

    public static String soundLabel(String key) { return LABELS[soundIndex(key)]; }

    /** Next sound key (wraps), for the editor's dropdown cycle. */
    public static String cycleSound(String key, int dir) {
        int i = (soundIndex(key) + dir) % KEYS.length;
        if (i < 0) i += KEYS.length;
        return KEYS[i];
    }
}
