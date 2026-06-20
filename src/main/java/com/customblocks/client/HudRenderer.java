/**
 * HudRenderer.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: render the brick-based HUD overlay. Builds a look-at context once per
 * frame (id/name/slot of the aimed custom block + world coords/light/distance/facing),
 * then draws each visible HudField at its anchor-resolved position with its own scale,
 * colour, effect, and background (per-brick override or the global default). Block-info
 * bricks render only while aiming at a custom block; world bricks render whenever they
 * resolve a value. Shared by the live mixin and the editor preview.
 *
 * Depends on: ClientSlotCache, HudConfig, HudField, HudFieldType, SlotBlock.
 * Called by: HudRenderMixin (live, after InGameHud.render), HudEditorScreen (preview + hit-test).
 */
package com.customblocks.client;

import com.customblocks.block.SlotBlock;
import com.customblocks.client.hud.HudField;
import com.customblocks.client.hud.HudFieldType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class HudRenderer {

    private HudRenderer() {}

    private static final int PAD       = 3;
    private static final int DIVIDER_W = 64;   // unscaled width of a Divider brick's rule line

    // ── Live entry (HudRenderMixin) ──────────────────────────────────────────
    public static void render(DrawContext ctx) {
        if (!HudConfig.visible) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        drawAll(ctx, client, buildContext(client));
    }

    /** Draw every visible brick honouring the family-visibility rule. */
    public static void drawAll(DrawContext ctx, MinecraftClient client, HudFieldType.Ctx c) {
        for (HudField f : HudConfig.fields) {
            if (!f.visible) continue;
            if (f.type.family() == HudFieldType.Family.BLOCK_INFO && !c.hasBlock()) continue;
            String value = f.type.resolve(c, f.text);
            if (value == null) continue;            // world brick with nothing to show this frame
            drawField(ctx, client, f, value, c);
        }
    }

    // ── Look-at context ───────────────────────────────────────────────────────
    /** Build the per-frame resolver context from the player's crosshair target. */
    public static HudFieldType.Ctx buildContext(MinecraftClient client) {
        if (client.world == null || client.player == null) return HudFieldType.Ctx.empty();
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK)
            return HudFieldType.Ctx.empty();

        BlockPos pos = bh.getBlockPos();
        int light = client.world.getLightLevel(pos);
        double dist = client.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
        String facing = bh.getSide().asString();

        BlockState state = client.world.getBlockState(pos);
        if (state.getBlock() instanceof SlotBlock slot) {
            int idx = slot.getSlotIndex();
            ClientSlotCache.Entry e = ClientSlotCache.getEntry(idx);
            if (e != null) {
                return new HudFieldType.Ctx(true, e.id(), e.name(), idx,
                        e.category(), e.glow(), e.hardness(), e.sound(), e.shape(), e.passable(),
                        true, pos.getX(), pos.getY(), pos.getZ(), light, dist, facing);
            }
            // Custom block we have no sync data for yet — show id/name blank, defaults elsewhere.
            return new HudFieldType.Ctx(true, "", "", idx,
                    "", 0, com.customblocks.core.SlotData.DEFAULT_HARDNESS, "stone", "full", false,
                    true, pos.getX(), pos.getY(), pos.getZ(), light, dist, facing);
        }
        // Group 13 / O6 — a placed joinable Arabic letter: show its LIVE name + virtual id (computed
        // from the synced BlockEntity, so it tracks neighbours and form-label edits with no reload).
        if (state.getBlock() instanceof com.customblocks.block.ArabicLetterBlock
                && client.world.getBlockEntity(pos) instanceof com.customblocks.block.ArabicLetterBlockEntity be
                && be.letter() != 0) {
            int form = be.effectiveForm();
            String name = com.customblocks.arabic.ArabicNaming.displayName(be.letter(), be.color(), form);
            String vid  = com.customblocks.arabic.ArabicNaming.virtualId(be.letter(), be.color(), form);
            return new HudFieldType.Ctx(true, vid, name, -1,
                    "Arabic", 0, 1.0f, "stone", "full", false,
                    true, pos.getX(), pos.getY(), pos.getZ(), light, dist, facing);
        }
        // Aiming at a non-custom block: world bricks still resolve, block-info bricks do not.
        return new HudFieldType.Ctx(false, "", "", -1,
                "", 0, 0f, "stone", "full", false,
                true, pos.getX(), pos.getY(), pos.getZ(), light, dist, facing);
    }

    // ── Per-brick draw ─────────────────────────────────────────────────────────
    private static void drawField(DrawContext ctx, MinecraftClient client, HudField f, String value,
                                  HudFieldType.Ctx c) {
        TextRenderer tr = client.textRenderer;
        boolean divider = f.type.isDivider();
        String display = divider ? "" : f.prefix + value;

        int   unW = divider ? DIVIDER_W : tr.getWidth(styled(display, f.bold));
        int   unH = tr.fontHeight;
        int   boxW = unW + PAD * 2;
        int   boxH = unH + PAD * 2;
        float scale = effectiveScale(f);

        int sw = MathHelper.ceil(boxW * scale);
        int sh = MathHelper.ceil(boxH * scale);
        int[] p = resolvePos(f, sw, sh, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(p[0], p[1], 0);
        m.scale(scale, scale, 1.0f);

        drawBackground(ctx, f, boxW, boxH);

        if (divider) {
            int y = boxH / 2;
            ctx.fill(PAD, y, boxW - PAD, y + 1, 0xFF000000 | (f.color & 0xFFFFFF));
        } else {
            drawText(ctx, tr, display, f, c);
        }
        m.pop();
    }

    /** Background pad behind a brick: per-brick override, or the global default. */
    private static void drawBackground(DrawContext ctx, HudField f, int boxW, int boxH) {
        int color; float opacity;
        if (f.bgOverride) {
            if (f.bgOff) return;
            color = f.bgColor; opacity = f.bgOpacity;
        } else {
            color = HudConfig.bgColor; opacity = HudConfig.bgOpacity;
        }
        if (opacity <= 0f) return;
        int alpha = Math.round(HudConfig.clamp01(opacity) * 255f);
        ctx.fill(0, 0, boxW, boxH, (alpha << 24) | (color & 0xFFFFFF));
    }

    /** Draw the brick's text at local (PAD,PAD) honouring colour, bold, shadow + effect. */
    private static void drawText(DrawContext ctx, TextRenderer tr, String s, HudField f, HudFieldType.Ctx c) {
        int base = 0xFF000000 | (f.color & 0xFFFFFF);
        long t = System.currentTimeMillis();
        switch (f.effect) {
            case NONE  -> ctx.drawText(tr, styled(s, f.bold), PAD, PAD, base, f.shadow);
            case PULSE -> {
                float k = 0.6f + 0.4f * (float) (0.5 * (1 + Math.sin(t * 0.005)));
                ctx.drawText(tr, styled(s, f.bold), PAD, PAD, scaleRgb(base, k), f.shadow);
            }
            case RAINBOW  -> drawPerChar(ctx, tr, s, f, t, true);
            case GRADIENT -> drawPerChar(ctx, tr, s, f, t, false);
        }
    }

    private static void drawPerChar(DrawContext ctx, TextRenderer tr, String s, HudField f, long t, boolean rainbow) {
        int x = PAD;
        int n = Math.max(1, s.length());
        float baseHue = rainbow ? (t % 4000L) / 4000f : hueOf(f.color);
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            float hue = rainbow ? (baseHue + i * 0.06f) % 1f : (baseHue + (i / (float) n) * 0.30f) % 1f;
            int col = 0xFF000000 | MathHelper.hsvToRgb(hue, 0.85f, 1.0f);
            ctx.drawText(tr, styled(ch, f.bold), x, PAD, col, f.shadow);
            x += tr.getWidth(styled(ch, f.bold));
        }
    }

    // ── Editor support ──────────────────────────────────────────────────────
    /** Screen-space {x, y, w, h} of a brick at current settings, or null if not shown. */
    public static int[] brickBounds(MinecraftClient client, HudField f, HudFieldType.Ctx c, int screenW, int screenH) {
        if (f.type.family() == HudFieldType.Family.BLOCK_INFO && !c.hasBlock()) return null;
        String value = f.type.resolve(c, f.text);
        if (value == null) return null;
        TextRenderer tr = client.textRenderer;
        boolean divider = f.type.isDivider();
        int unW = divider ? DIVIDER_W : tr.getWidth(styled(f.prefix + value, f.bold));
        int boxW = unW + PAD * 2, boxH = tr.fontHeight + PAD * 2;
        float scale = effectiveScale(f);
        int sw = MathHelper.ceil(boxW * scale), sh = MathHelper.ceil(boxH * scale);
        int[] p = resolvePos(f, sw, sh, screenW, screenH);
        return new int[]{ p[0], p[1], sw, sh };
    }

    // ── Geometry / colour helpers ────────────────────────────────────────────
    public static float effectiveScale(HudField f) {
        return HudConfig.clampScale(f.size) * HudConfig.clampScale(HudConfig.masterScale);
    }

    /** Top-left screen position from the brick's anchor + offset and its scaled size. */
    private static int[] resolvePos(HudField f, int w, int h, int screenW, int screenH) {
        int x, y;
        switch (f.anchor) {
            case TR     -> { x = screenW - f.offsetX - w; y = f.offsetY; }
            case BL     -> { x = f.offsetX;               y = screenH - f.offsetY - h; }
            case BR     -> { x = screenW - f.offsetX - w; y = screenH - f.offsetY - h; }
            case CENTER -> { x = screenW / 2 + f.offsetX - w / 2; y = screenH / 2 + f.offsetY - h / 2; }
            default     -> { x = f.offsetX;               y = f.offsetY; } // TL
        }
        return new int[]{ x, y };
    }

    private static Text styled(String s, boolean bold) {
        return Text.literal(s).setStyle(Style.EMPTY.withBold(bold));
    }

    private static int scaleRgb(int argb, float k) {
        int r = Math.round(((argb >> 16) & 0xFF) * k);
        int g = Math.round(((argb >> 8) & 0xFF) * k);
        int b = Math.round((argb & 0xFF) * k);
        return (argb & 0xFF000000) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    private static float hueOf(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        if (d == 0f) return 0f;
        float h;
        if (max == r)      h = ((g - b) / d) % 6f;
        else if (max == g) h = (b - r) / d + 2f;
        else               h = (r - g) / d + 4f;
        h /= 6f;
        return h < 0 ? h + 1f : h;
    }
}
