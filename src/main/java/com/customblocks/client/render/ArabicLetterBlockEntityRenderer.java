/**
 * ArabicLetterBlockEntityRenderer.java — Group 13 / Pass 4 step 4b + O6. CLIENT-ONLY.
 *
 * Draws the joinable letter's glyph on ALL SIX faces of the block from a LIVE in-memory texture
 * (NativeImageBackedTexture registered straight into the TextureManager) — never the resource pack,
 * so a form change causes no pack rebuild / no reload (ADR-005). The letter reads like a solid letter
 * cube from any angle (dev request 2026-06-19); the FRONT face (the block's FACING) keeps the proven
 * word-axis orientation, so a row of connected letters still tiles seamlessly along the front.
 *
 * Front + the four sides + top + bottom all carry THIS block's own tile. The BACK face is special
 * (issue #6, C-full readable back): it carries the MIRROR PARTNER's tile (backLetter/backForm, set by
 * ArabicJoinFlow) so a connected word reads correctly from behind. The back face is the front quad turned
 * 180° about the vertical axis (like spinning a sign around) — NOT mirrored; the partner swap already
 * handles the left/right order. A lone / unset block (backLetter == 0) falls back to its own tile.
 *
 * Face quads sit at z = 0.999 (just INSIDE the cube) rather than poking out past it (issue #1): the old
 * z = 1.002 overhang made every face stick 0.002 past the block edge, drawing the "tiny long lines"
 * along edges and at the seams between adjacent letters. Inset removes the overhang with no visible gap.
 *
 * Texture per (letter, form, colour), built once and shared (cache keyed by all three):
 *   - ALL forms → engine-drawn via ArabicTileRenderer (arabtype). ISOLATED simply gets NO joining
 *     bars; initial/medial/final add the kashida hand. An isolated letter therefore matches its
 *     connected neighbours EXACTLY — same stroke, same shared size + baseline (where it extends below),
 *     same crispness and style (dev call 2026-06-19: "make it like the others, just without the joining
 *     hands"). The bundled hand-art is the SEPARATE 224 static block set (ArabicBlockRegistry), not this
 *     auto-join block — so the auto-join letters never mix a scaled raster beside a vector font tile.
 *
 * Brightness (v2 fix-pass): the glyph quads are drawn FULL-BRIGHT (max lightmap) on every face, so the
 * white letter never dims to grey or blacks out regardless of facing. This replaces the old O6 approach
 * (sampling pos.offset(facing)), which read dark whenever the block faced into a solid neighbour.
 *
 * The cube-draw is shared with the inventory item icon via {@link #drawGlyphCube} + {@link #textureFor}.
 *
 * Depends on: ArabicLetterBlockEntity, ArabicLetterBlock (FACING), ArabicTileRenderer
 * Called by:  CustomBlocksClient (BlockEntityRendererFactories.register + the item renderer)
 */
package com.customblocks.client.render;

import com.customblocks.CustomBlocksMod;
import com.customblocks.arabic.ArabicTileRenderer;
import com.customblocks.block.ArabicLetterBlock;
import com.customblocks.block.ArabicLetterBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class ArabicLetterBlockEntityRenderer implements BlockEntityRenderer<ArabicLetterBlockEntity> {

    /** (letter+form+colour) key → uploaded dynamic-texture id. Built + registered once, then reused. */
    private static final Map<String, Identifier> CACHE = new HashMap<>();
    /** Keys we already tried and failed to build, so we don't retry (and spam) every frame. */
    private static final Map<String, Boolean> FAILED = new HashMap<>();

    private static final int WHITE = 0xFFFFFFFF;

    /** Opaque tile bg per bundled colour — EXACTLY the bundled hand-art bg (sampled from the art
     *  PNGs) so a connected font tile sits seamlessly beside an isolated hand-art tile. */
    private static int bgArgb(String color) {
        return switch (color == null ? "black" : color) {
            case "red"    -> 0xFFFF0000; // (255,0,0)
            case "green"  -> 0xFF1E8C1E; // (30,140,30)
            case "yellow" -> 0xFFF0C814; // (240,200,20)
            default       -> 0xFF0A0A0A; // black (10,10,10)
        };
    }

    public ArabicLetterBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(ArabicLetterBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light, int overlay) {
        char letter = be.letter();
        if (letter == 0) return;
        int form = be.effectiveForm();
        Identifier tex = textureFor(letter, form, be.color());
        if (tex == null) return;

        // Back face shows the mirror partner's tile (C-full readable back); 0 = lone/unset → use own tile.
        char back = be.backLetter();
        Identifier backTex = (back != 0) ? textureFor(back, be.backForm(), be.color()) : tex;
        if (backTex == null) backTex = tex;

        Direction facing = be.getCachedState().contains(ArabicLetterBlock.FACING)
                ? be.getCachedState().get(ArabicLetterBlock.FACING) : Direction.NORTH;

        // Full-bright glyph always (v2 fix-pass): the white letter renders at full lightmap no matter which
        // way the block faces, so it never dims to grey or blacks out when it faces into a solid block.
        // Replaces the old O6 pos.offset(facing) light sample, which read dark off a solid neighbour.
        int faceLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        matrices.push();
        // Orient local +Z onto the block's FACING so the FRONT face keeps the proven word-axis layout
        // (rows tile seamlessly); the helpers then paint all six faces in this frame.
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
        matrices.translate(-0.5, -0.5, -0.5);
        // The buffer provider keeps only ONE RenderLayer "building" at a time — requesting a second
        // buffer ends the first. So draw ALL own-tile faces, THEN fetch the partner buffer and draw the
        // back face last; never switch back to the own buffer (doing so threw "Not building!" → crash).
        drawOwnFaces(matrices, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex)), faceLight);
        drawBackFace(matrices, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(backTex)), faceLight);
        matrices.pop();
    }

    /**
     * Single-tile cube (inventory item icon / lone block): all six faces use {@code vc}'s one tile (one
     * RenderLayer, so no buffer switch). The back face is still U-flipped so it reads from behind too.
     */
    public static void drawGlyphCube(MatrixStack matrices, VertexConsumer vc, int light) {
        drawOwnFaces(matrices, vc, light);
        drawBackFace(matrices, vc, light);
    }

    /**
     * Front + the four sides + top + bottom, all on {@code vc}'s tile (NOT the back face). The front face
     * is the canonical south quad; the others are that same quad rotated onto their face.
     */
    public static void drawOwnFaces(MatrixStack matrices, VertexConsumer vc, int light) {
        face(matrices, vc, light, RotationAxis.POSITIVE_Y, 0f,   false); // front  (+Z)
        face(matrices, vc, light, RotationAxis.POSITIVE_Y, 90f,  false); // left   (-X)
        face(matrices, vc, light, RotationAxis.POSITIVE_Y, -90f, false); // right  (+X)
        face(matrices, vc, light, RotationAxis.POSITIVE_X, -90f, false); // top    (+Y)
        face(matrices, vc, light, RotationAxis.POSITIVE_X, 90f,  false); // bottom (-Y)
    }

    /**
     * The BACK face only (-Z). The 180° Y-rotation alone turns the quad to face behind WHILE staying
     * readable — exactly like spinning a sign around to show someone behind you (a 180° turn about the
     * vertical axis keeps text readable; it does NOT mirror it). So NO U-flip: an earlier flipU=true added
     * a second mirror on top of the turn, which read back glyphs backwards (C-full garble, R3.6). The
     * left/right reading order is already handled by the partner-letter swap in ArabicJoinFlow; orientation
     * is just this turn. Its tile may be the mirror partner's (issue #6). Draw this LAST: the caller fetches
     * its buffer after the own-tile faces are done, so the RenderLayer never switches back to the own tile.
     */
    public static void drawBackFace(MatrixStack matrices, VertexConsumer vc, int light) {
        face(matrices, vc, light, RotationAxis.POSITIVE_Y, 180f, false); // back (-Z) — turned, NOT mirrored
    }

    /**
     * Rotate the canonical south quad onto one face (about the cube centre) and draw it. {@code flipU}
     * mirrors the texture horizontally (kept for completeness; all faces currently pass false).
     */
    private static void face(MatrixStack matrices, VertexConsumer vc, int light,
                             RotationAxis axis, float degrees, boolean flipU) {
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(axis.rotationDegrees(degrees));
        matrices.translate(-0.5, -0.5, -0.5);
        MatrixStack.Entry e = matrices.peek();
        Matrix4f m = e.getPositionMatrix();
        float z = 0.999f; // just INSIDE the cube surface (no overhang lines — issue #1)
        float u0 = flipU ? 1f : 0f;
        float u1 = flipU ? 0f : 1f;
        vert(vc, m, e, light, 0f, 1f, z, u0, 0f); // top-left
        vert(vc, m, e, light, 1f, 1f, z, u1, 0f); // top-right
        vert(vc, m, e, light, 1f, 0f, z, u1, 1f); // bottom-right
        vert(vc, m, e, light, 0f, 0f, z, u0, 1f); // bottom-left
        matrices.pop();
    }

    private static void vert(VertexConsumer vc, Matrix4f m, MatrixStack.Entry e, int light,
                             float x, float y, float z, float u, float v) {
        vc.vertex(m, x, y, z)
          .color(255, 255, 255, 255)
          .texture(u, v)
          .overlay(OverlayTexture.DEFAULT_UV)
          .light(light)
          .normal(e, 0f, 0f, 1f);
    }

    // ── texture cache ─────────────────────────────────────────────────────────

    /** Dynamic-texture id for one (letter, form, colour); built + uploaded once, then cached. */
    public static Identifier textureFor(char letter, int form, String color) {
        String key = ((int) letter) + "_" + form + "_" + (color == null ? "black" : color);
        Identifier cached = CACHE.get(key);
        if (cached != null) return cached;
        if (FAILED.containsKey(key)) return null;

        NativeImage img = build(letter, form, color);
        if (img == null) { FAILED.put(key, Boolean.TRUE); return null; }

        Identifier id = Identifier.of(CustomBlocksMod.MOD_ID, "arabic_dyn_" + key);
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(img));
        CACHE.put(key, id);
        return id;
    }

    /**
     * Build the glyph bitmap for one (letter, form, colour). EVERY form is engine-drawn by
     * ArabicTileRenderer (arabtype) so an isolated letter matches its connected neighbours exactly —
     * ISOLATED just gets no joining bars; initial/medial/final add the kashida hand. White glyph on the
     * colour's bg. (Bundled hand-art is the separate 224 static block set, not this auto-join block.)
     */
    private static NativeImage build(char letter, int form, String color) {
        try {
            String col = (color == null || color.isEmpty()) ? "black" : color;
            byte[] png = ArabicTileRenderer.render(letter, form, WHITE, bgArgb(col));
            if (png == null) return null;
            return NativeImage.read(new ByteArrayInputStream(png));
        } catch (Exception ex) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Arabic tile build failed for U+{} form {} colour {}: {}",
                    Integer.toHexString(letter), form, color, ex.toString());
            return null;
        }
    }
}
