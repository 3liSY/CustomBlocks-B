/**
 * ArabicSlotBootstrap.java — Group 13 / G13-25 CP1+CP4: pre-bake the 656 base Arabic slots.
 *
 * Responsibility: guarantee that every letter form and number exists as a REAL, normal slot
 * block, in ALL FOUR colours, from the moment the server boots:
 *     36 letters x 4 contextual forms (isolated/initial/medial/final) = 144
 *   + 10 Eastern numbers (a0-a9) + 10 Western numbers (e0-e9), always isolated = 20
 *   = 164 per colour x 4 colours (black + red + green + yellow) = 656 slots — each a plain
 * SlotBlock with real SlotData and a real baked PNG, flagged Arabic only by ArabicMeta on its
 * SlotData (data-only design: NO subclass, NO FACING blockstate, NO index partition). Allocation
 * goes through the same free pool as any /cb create, so existing blocks are never touched — the
 * lesson of the reverted CP1 partition (see SlotPools header).
 *
 * Idempotent by stable customId "arabic_<glyph>_<iso|ini|mid|fin>" (numbers always _iso); the
 * three coloured sets append the standard ColorVariantService suffix ("_red"/"_green"/"_yellow"
 * — black IS the base, no suffix), so Square swaps and the join flow's sibling lookup find them
 * with zero special-casing. The form tokens are FIXED — deliberately NOT ArabicNaming.virtualId,
 * whose form words track the live config labels and would break the skip-if-exists check whenever
 * a label changes. Deleting one with the Deleter permanently retires its index (DeletedSlots); the
 * next boot re-creates the block at a fresh index — the base set is complete every boot by design.
 *
 * Colours (CP4, owner 2026-07-03): the coloured sets use the EXACT config triangle hexes
 * (triangleRedHex/GreenHex/YellowHex via ColorVariantService.rgbFor), NOT the old bundled-art
 * colours. The sidecar remembers which hex each set was baked with; changing a config hex
 * re-bakes that whole coloured set once on the next boot.
 *
 * Textures (exactly the confirmed old-system look): ALL FOUR letter forms — isolated included —
 * bake through ArabicTileRenderer.render (white glyph on the colour's bg; black = the confirmed
 * 0xFF0A0A0A). The bundled hand-art PNG is the bake source ONLY for numbers: black uses it as-is
 * (the confirmed static look), coloured numbers recolour the black art's background to the config
 * hex through the SAME BackgroundRemover/ImageProcessor path a Triangle colour-variant uses.
 * BAKE_VERSION re-bakes existing slots' textures once whenever the recipe changes.
 *
 * Depends on: ArabicArt (catalog + bundled PNGs), ArabicGlyphs (name→char), ArabicTileRenderer,
 *             ArabicNaming (display names), ArabicJoining (form constants), ColorVariantService
 *             (config hexes), BackgroundRemover/ImageProcessor (number recolour),
 *             SlotManager.createArabicNoSave, TextureStore
 * Called by:  CustomBlocksMod.onInitialize (after ArabicLetterRetirement.init(), before the
 *             SERVER_STARTED pack build — new textures ride that same single pack rebuild)
 */
package com.customblocks.arabic;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.core.ArabicMeta;
import com.customblocks.core.ColorVariantService;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageProcessor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class ArabicSlotBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks/Arabic");

    /** Same fg/bg the confirmed join renderer drew with: white glyph on the near-black
     *  background — NOT pure 0x000000. */
    private static final int FG_WHITE = 0xFFFFFFFF;
    private static final int BG_BLACK = 0xFF0A0A0A;

    /** The three coloured sets on top of the black base (id suffix = ColorVariantService key). */
    private static final String[] VARIANT_COLOURS = {"red", "green", "yellow"};

    /** Bump when the bake recipe changes; existing slots' textures are re-baked ONCE on the next
     *  boot. v1 = bundled-art isolated letters (wrong — the join look is font-drawn); v2 = font. */
    private static final int BAKE_VERSION = 2;
    private static final Path VERSION_FILE = Path.of("config/customblocks/arabic_bake_version.json");

    private ArabicSlotBootstrap() {} // static-only

    private enum Result { CREATED, SKIPPED, REBAKED, FAILED }

    /** Ensure all 656 base slots exist (+ re-bake stale textures once after a recipe bump or a
     *  config-hex change). Saves slots.json once when anything new was created. */
    public static void ensure() {
        long t0 = System.currentTimeMillis();
        JsonObject sidecar = readSidecar();
        boolean rebakeBase = storedBakeVersion(sidecar) < BAKE_VERSION;
        int created = 0, skipped = 0, rebaked = 0;
        int failedBase = 0;
        // ── black base set (CP1) ─────────────────────────────────────────────
        for (ArabicArt.Glyph g : ArabicArt.ALL) {
            for (int form : formsOf(g)) {
                Character ch = charOf(g);
                if (g.group() == ArabicArt.Group.LETTER && ch == null) {
                    failedBase++; // catalog/glyph-map drift — never expected for the 36 letters
                    LOG.warn("[CustomBlocks/Arabic] No letter char for art base '{}' — skipped.", g.idBase());
                    continue;
                }
                switch (ensureOne(g, ch, form, "black", rebakeBase)) {
                    case CREATED -> created++;
                    case SKIPPED -> skipped++;
                    case REBAKED -> rebaked++;
                    case FAILED  -> failedBase++;
                }
            }
        }
        // ── the three coloured sets (CP4) — exact config triangle hexes ─────
        int failedColours = 0;
        for (String colour : VARIANT_COLOURS) {
            String liveHex = ColorVariantService.hexFor(colour);
            boolean rebakeColour = rebakeBase
                    || !liveHex.equalsIgnoreCase(storedColourHex(sidecar, colour));
            int failedThis = 0;
            for (ArabicArt.Glyph g : ArabicArt.ALL) {
                for (int form : formsOf(g)) {
                    Character ch = charOf(g);
                    if (g.group() == ArabicArt.Group.LETTER && ch == null) { failedThis++; continue; }
                    switch (ensureOne(g, ch, form, colour, rebakeColour)) {
                        case CREATED -> created++;
                        case SKIPPED -> skipped++;
                        case REBAKED -> rebaked++;
                        case FAILED  -> failedThis++;
                    }
                }
            }
            if (failedThis == 0) sidecar.addProperty(colour, liveHex); // retry next boot otherwise
            failedColours += failedThis;
        }
        if (created > 0) SlotManager.saveAll();
        if (!rebakeBase || failedBase == 0) sidecar.addProperty("version", BAKE_VERSION);
        writeSidecar(sidecar);
        LOG.info("[CustomBlocks/Arabic] G13-25 base-slot bootstrap (656): {} created, {} skipped, "
                        + "{} re-baked, {} failed ({} ms).",
                created, skipped, rebaked, failedBase + failedColours, System.currentTimeMillis() - t0);
    }

    /** Letters bake all 4 contextual forms; numbers never join → single isolated slot each. */
    private static int[] formsOf(ArabicArt.Glyph g) {
        return g.group() == ArabicArt.Group.LETTER
                ? new int[]{ArabicJoining.ISOLATED, ArabicJoining.INITIAL, ArabicJoining.MEDIAL, ArabicJoining.FINAL}
                : new int[]{ArabicJoining.ISOLATED};
    }

    /** The glyph's letter char, or null for numbers / unknown art bases. */
    private static Character charOf(ArabicArt.Glyph g) {
        if (g.group() != ArabicArt.Group.LETTER) return null;
        return ArabicGlyphs.charForName(g.idBase()).orElse(null);
    }

    /** Ensure one (glyph, form, colour) slot exists; re-bake its texture when asked.
     *  {@code ch} is null for numbers. */
    private static Result ensureOne(ArabicArt.Glyph g, Character ch, int form, String colour, boolean rebake) {
        String id = slotId(g.idBase(), form) + ("black".equals(colour) ? "" : "_" + colour);
        SlotData existing = SlotManager.getById(id);
        if (existing != null) {
            if (!existing.isArabic()) {
                LOG.warn("[CustomBlocks/Arabic] Id '{}' is already taken by a non-Arabic block — left alone.", id);
                return Result.SKIPPED;
            }
            if (!rebake) return Result.SKIPPED;
            byte[] png = bake(g, ch, form, colour);
            if (png == null) {
                LOG.warn("[CustomBlocks/Arabic] Re-bake failed for '{}' — old texture kept.", id);
                return Result.FAILED;
            }
            TextureStore.save(existing.index(), png);
            return Result.REBAKED;
        }
        byte[] png = bake(g, ch, form, colour);
        if (png == null) {
            LOG.warn("[CustomBlocks/Arabic] Texture bake failed for '{}'.", id);
            return Result.FAILED;
        }
        String display = (ch != null)
                ? ArabicNaming.displayName(ch, colour, form)    // "Jeem Black" / "Jeem Red Mid"
                : ArabicArt.displayName(g, colour);             // "A0 Black" / "E5 Yellow"
        SlotData d = SlotManager.createArabicNoSave(id, display, ArabicArt.category(g),
                new ArabicMeta(g.idBase(), form, colour));
        if (d == null) {
            LOG.warn("[CustomBlocks/Arabic] No free slot for '{}' — pool full?", id);
            return Result.FAILED;
        }
        TextureStore.save(d.index(), png);
        return Result.CREATED;
    }

    /** Stable base-slot id, e.g. "arabic_jeem_mid", "arabic_a0_iso". Colour variants append the
     *  standard ColorVariantService "_&lt;colour&gt;" suffix to this base. */
    public static String slotId(String glyphId, int form) {
        return "arabic_" + glyphId + "_" + formToken(form);
    }

    /** Fixed id token per form — never the live ArabicLabels words (see header). */
    private static String formToken(int form) {
        return switch (form) {
            case ArabicJoining.INITIAL -> "ini";
            case ArabicJoining.MEDIAL  -> "mid";
            case ArabicJoining.FINAL   -> "fin";
            default                    -> "iso";
        };
    }

    /**
     * PNG for one (glyph, form, colour). Letters are font-baked in EVERY form (the confirmed join
     * look) on the colour's background. Numbers keep the bundled hand-art: black as-is; coloured
     * = the black art's background recoloured to the config hex through the SAME
     * BackgroundRemover/ImageProcessor path a Triangle variant uses (corner-sampled fill — the
     * digit design survives, exactly like any block's colour variant).
     */
    private static byte[] bake(ArabicArt.Glyph g, Character ch, int form, String colour) {
        boolean black = "black".equals(colour);
        if (ch != null) {
            int bg = black ? BG_BLACK : 0xFF000000 | ColorVariantService.rgbFor(colour);
            return ArabicTileRenderer.render(ch, form, FG_WHITE, bg);
        }
        byte[] art = readResource(ArabicArt.resource(g, "black"));
        if (black || art == null) return art;
        try {
            int rgb = ColorVariantService.rgbFor(colour);
            String mode = CustomBlocksConfig.backgroundMode; // none → edges inside recolorBackground
            int tol = CustomBlocksConfig.backgroundTolerance > 0
                    ? CustomBlocksConfig.backgroundTolerance : 30;
            byte[] recoloured = BackgroundRemover.recolorBackground(art, mode, tol, rgb);
            byte[] png = ImageProcessor.toBlockPng(recoloured, CustomBlocksConfig.textureSize);
            return ImageProcessor.fillBackground(png, rgb);
        } catch (Exception e) {
            LOG.warn("[CustomBlocks/Arabic] Number recolour failed for '{}' ({}): {}",
                    g.idBase(), colour, e.getMessage());
            return null;
        }
    }

    // ── bake sidecar (version + the hex each coloured set was baked with) ─────

    /** The whole sidecar object ({} when missing/unreadable — everything re-bakes, harmless). */
    private static JsonObject readSidecar() {
        try {
            if (!Files.exists(VERSION_FILE)) return new JsonObject();
            return JsonParser.parseString(Files.readString(VERSION_FILE, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static int storedBakeVersion(JsonObject sidecar) {
        return sidecar.has("version") ? sidecar.get("version").getAsInt() : 1;
    }

    /** The hex {@code colour}'s set was last baked with, or "" (→ bake now). */
    private static String storedColourHex(JsonObject sidecar, String colour) {
        String key = colour.toLowerCase(Locale.ROOT);
        return sidecar.has(key) ? sidecar.get(key).getAsString() : "";
    }

    private static void writeSidecar(JsonObject root) {
        try {
            Files.createDirectories(VERSION_FILE.getParent());
            Path tmp = VERSION_FILE.resolveSibling("arabic_bake_version.json.tmp");
            Files.writeString(tmp, root.toString(), StandardCharsets.UTF_8);
            Files.move(tmp, VERSION_FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOG.warn("[CustomBlocks/Arabic] Could not store bake sidecar: {}", e.getMessage());
        }
    }

    /** Read a bundled JAR resource fully, or null if absent. */
    private static byte[] readResource(String path) {
        try (InputStream in = ArabicSlotBootstrap.class.getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        } catch (Exception e) {
            LOG.warn("[CustomBlocks/Arabic] Failed reading bundled art {}: {}", path, e.getMessage());
            return null;
        }
    }
}
