/**
 * TimerDisplayBlockEntity.java — Group 31 (BuzzerGame) items E + F (Arabic screen + per-part resize).
 *
 * The logic/anchor behind an (invisible) timer-stand block. It owns nothing visual itself — instead it
 * spawns and drives fourteen server-side display entities via {@link TimerDisplayVisual} (base / leg / screen
 * ITEM_DISPLAY parts + a target and a result TEXT_DISPLAY number line on each face + an Arabic-word quad per
 * line per face + three INTERACTION hitboxes). Like a buzzer it stores only its link to a host's wand session
 * ({@code sessionId}) and pulls the target/result numbers through that {@link BuzzerSession} (via {@link BuzzerSessionManager}).
 *
 * It also persists the look/geometry the wand + commands set: the whole-stand scale, the three per-part
 * scales (item F), the one text colour, the text size (item E), and the facing yaw (LED is the only digit
 * style now). Each server tick it makes sure its entities exist (spawned once, on placement), builds the current
 * {@link TimerDisplayVisual.Render}, and re-pushes only what changed (parts when geometry moves, text when
 * the numbers/colour/size change). The fourteen entity UUIDs are persisted so a reload re-attaches, not
 * duplicates.
 *
 * Depends on: BuzzerGameRegistry (BlockEntityType), TimerDisplayVisual, TimerPart, BuzzerSession(Manager)
 * Called by:  TimerDisplayBlock (createBlockEntity, ticker, place/break), BuzzerGameWand (link/resize/rotate/style),
 *             BuzzerGameCommands (size/textcolor/textsize), NBT
 */
package com.customblocks.buzzergame;

import com.customblocks.CustomBlocksConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TimerDisplayBlockEntity extends BlockEntity {

    private static final TimerDisplayVisual.Handles NO_VISUAL =
            new TimerDisplayVisual.Handles(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    private static final String IDLE_NUM = "0.00";

    /** Stable id for this display, assigned on placement. Links key off it (session screen set). */
    private UUID displayId = UUID.randomUUID();

    /** The host session this stand is linked to, or null if unlinked (resolved via BuzzerSessionManager). */
    private @Nullable UUID sessionId;

    /** UUIDs of the seven spawned display entities (persisted so a reload re-attaches, not duplicates). */
    private TimerDisplayVisual.Handles visual = NO_VISUAL;

    /** Whole-stand size multiplier. Persisted; drives visuals + hitbox. A freshly placed stand spawns at the
     *  persisted spawn default (×1.3 shipped; owner-tunable via {@code size <n> setdefault}). */
    private float scale = CustomBlocksConfig.timerDefaultScale;

    /** Per-part size multipliers on top of the whole scale (item F); 1.0 = the original slice. */
    private float baseScale = 1.0f;
    private float legScale = 1.0f;
    private float screenScale = 1.0f;

    /** One colour for ALL screen text (item E). Default brand green. */
    private int textColor = TimerDisplayVisual.DEFAULT_TEXT_COLOR;

    /** Text size multiplier (textsize command, item E); 1.0 = default, up to 30×. */
    private float textScale = 1.0f;

    /** Facing angle in degrees (0-360). NaN = not yet set → seeded from the placed facing. Rotate mode nudges it. */
    private float yaw = Float.NaN;

    /** Last state pushed to the entities — used to skip redundant re-pushes. */
    private @Nullable TimerDisplayVisual.Render lastRender;

    public TimerDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(BuzzerGameRegistry.TIMER_DISPLAY_ENTITY, pos, state);
    }

    public UUID getDisplayId() { return displayId; }
    public @Nullable UUID getSessionId() { return sessionId; }
    public boolean isLinked() { return sessionId != null; }

    /** Bind this display to a host's wand session (called by the link wand). */
    public void link(UUID sessionId) { this.sessionId = sessionId; markDirty(); }

    /** Forget the session link (called on host logout, session reset, or break). */
    public void clearLink() { this.sessionId = null; markDirty(); }

    public float getScale() { return scale; }

    /** Set the whole-stand size multiplier (clamped); the next tick re-scales the visuals + hitbox. */
    public void setScale(float newScale) { this.scale = TimerDisplayVisual.clampScale(newScale); markDirty(); }

    public int getTextColor() { return textColor; }
    public void setTextColor(int rgb) { this.textColor = rgb & 0xFFFFFF; markDirty(); }

    public float getTextScale() { return textScale; }
    public void setTextScale(float s) { this.textScale = TimerDisplayVisual.clampTextScale(s); markDirty(); }

    /** The current scale of one part (WHOLE = the whole-stand scale). */
    public float partScale(TimerPart p) {
        return switch (p) {
            case WHOLE -> scale;
            case BASE -> baseScale;
            case LEG -> legScale;
            case SCREEN -> screenScale;
        };
    }

    /** Step one part's scale one notch (clamped); returns the new scale. Growing a lower part lifts those above. */
    public float stepPart(TimerPart p, boolean grow) {
        float step = grow ? TimerDisplayVisual.SCALE_STEP : -TimerDisplayVisual.SCALE_STEP;
        switch (p) {
            case WHOLE -> scale = TimerDisplayVisual.clampScale(scale + step);
            case BASE -> baseScale = TimerDisplayVisual.clampScale(baseScale + step);
            case LEG -> legScale = TimerDisplayVisual.clampScale(legScale + step);
            case SCREEN -> screenScale = TimerDisplayVisual.clampScale(screenScale + step);
        }
        markDirty();
        return partScale(p);
    }

    /** Current facing angle in degrees (0-360); 0 until the first tick seeds it from the placed facing. */
    public float getYaw() { return Float.isNaN(yaw) ? 0f : yaw; }

    /** Nudge the facing angle by delta degrees (wraps 0-360); the next tick re-points model + text. */
    public void rotate(float deltaDeg) {
        float base = Float.isNaN(yaw) ? 0f : yaw;
        yaw = wrapDeg(base + deltaDeg);
        markDirty();
    }

    private static float wrapDeg(float d) {
        d %= 360f;
        return d < 0f ? d + 360f : d;
    }

    // ------------------------------------------------------------------ ticking

    /** Server ticker (wired by {@link TimerDisplayBlock#getTicker}): keep the visual spawned + current. */
    public static void serverTick(World world, BlockPos pos, BlockState state, TimerDisplayBlockEntity be) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (Float.isNaN(be.yaw)) { // seed the angle from the placed block facing, once
            Direction facing = state.contains(TimerDisplayBlock.FACING)
                    ? state.get(TimerDisplayBlock.FACING) : Direction.NORTH;
            be.yaw = facing.asRotation();
            be.markDirty();
        }
        TimerDisplayVisual.Render cur = be.buildRender();
        if (!be.visual.complete()) {
            // Clear any partial/legacy entities first (e.g. a stand placed before the interaction hitboxes
            // existed persists only 7 of the 10 handles) so the respawn can't orphan duplicate parts/text.
            TimerDisplayVisual.despawn(serverWorld, be.visual);
            be.visual = TimerDisplayVisual.spawnAll(serverWorld, pos, cur);
            be.lastRender = cur;
            be.markDirty();
            return;
        }
        if (be.lastRender == null || geometryChanged(be.lastRender, cur)) {
            TimerDisplayVisual.updateStand(serverWorld, be.visual, pos, cur);
            TimerDisplayVisual.updateText(serverWorld, be.visual, pos, cur);
            be.lastRender = cur;
        } else if (textChanged(be.lastRender, cur)) {
            TimerDisplayVisual.updateText(serverWorld, be.visual, pos, cur);
            be.lastRender = cur;
        }
    }

    /** Build the current render state from this stand's fields + its linked session's numbers. */
    private TimerDisplayVisual.Render buildRender() {
        BuzzerSession session = BuzzerSessionManager.bySession(sessionId);
        boolean hasTarget = session != null && session.hasTarget();
        String target = session != null ? session.targetText() : IDLE_NUM;
        String result = session != null ? session.resultText() : IDLE_NUM;
        // ~1 Hz colon blink (Step 4): on for the first half of each second, off for the second half.
        boolean blink = getWorld() != null && (getWorld().getTime() % 20) < 10;
        return new TimerDisplayVisual.Render(getYaw(), scale, baseScale, legScale, screenScale,
                hasTarget, target, result, textColor, textScale, blink);
    }

    /** True if anything affecting the parts' position/scale/angle changed (also moves the glued text). */
    private static boolean geometryChanged(TimerDisplayVisual.Render a, TimerDisplayVisual.Render b) {
        return a.yaw() != b.yaw() || a.wholeScale() != b.wholeScale()
                || a.baseScale() != b.baseScale() || a.legScale() != b.legScale() || a.screenScale() != b.screenScale();
    }

    /** True if any text field (numbers / colour / size / target visibility / colon blink) changed. */
    private static boolean textChanged(TimerDisplayVisual.Render a, TimerDisplayVisual.Render b) {
        return a.hasTarget() != b.hasTarget() || !a.targetNum().equals(b.targetNum())
                || !a.resultNum().equals(b.resultNum()) || a.color() != b.color()
                || a.textScale() != b.textScale() || a.blink() != b.blink();
    }

    /**
     * Spawn the display entities immediately on placement (from {@link TimerDisplayBlock#onPlaced}) so the
     * stand + screen appear the instant the block is placed, with no one-tick flicker. Seeds the facing yaw
     * from the placed state; no-op if already spawned.
     */
    public void ensureSpawnedOnPlace(ServerWorld world, BlockState state) {
        if (visual.complete()) return;
        if (Float.isNaN(yaw)) {
            Direction facing = state.contains(TimerDisplayBlock.FACING)
                    ? state.get(TimerDisplayBlock.FACING) : Direction.NORTH;
            yaw = facing.asRotation();
        }
        TimerDisplayVisual.Render cur = buildRender();
        visual = TimerDisplayVisual.spawnAll(world, pos, cur);
        lastRender = cur;
        markDirty();
    }

    /** Kill this display's entities (called when the block is broken). */
    public void despawnVisual(ServerWorld world) {
        if (visual.complete()) TimerDisplayVisual.despawn(world, visual);
        visual = NO_VISUAL;
        lastRender = null;
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putUuid("displayId", displayId);
        if (sessionId != null) nbt.putUuid("sessionId", sessionId);
        putUuid(nbt, "vBase", visual.base());
        putUuid(nbt, "vLeg", visual.leg());
        putUuid(nbt, "vScreen", visual.screen());
        putUuid(nbt, "vFrontTarget", visual.frontTarget());
        putUuid(nbt, "vFrontResult", visual.frontResult());
        putUuid(nbt, "vBackTarget", visual.backTarget());
        putUuid(nbt, "vBackResult", visual.backResult());
        putUuid(nbt, "vFrontLabelTarget", visual.frontLabelTarget());
        putUuid(nbt, "vFrontLabelResult", visual.frontLabelResult());
        putUuid(nbt, "vBackLabelTarget", visual.backLabelTarget());
        putUuid(nbt, "vBackLabelResult", visual.backLabelResult());
        putUuid(nbt, "vHitBase", visual.hitBase());
        putUuid(nbt, "vHitLeg", visual.hitLeg());
        putUuid(nbt, "vHitScreen", visual.hitScreen());
        nbt.putFloat("scale", scale);
        nbt.putFloat("baseScale", baseScale);
        nbt.putFloat("legScale", legScale);
        nbt.putFloat("screenScale", screenScale);
        nbt.putInt("textColor", textColor);
        nbt.putFloat("textScale", textScale);
        if (!Float.isNaN(yaw)) nbt.putFloat("yaw", yaw);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        if (nbt.containsUuid("displayId")) displayId = nbt.getUuid("displayId");
        sessionId = nbt.containsUuid("sessionId") ? nbt.getUuid("sessionId") : null;
        visual = new TimerDisplayVisual.Handles(
                getUuid(nbt, "vBase"), getUuid(nbt, "vLeg"), getUuid(nbt, "vScreen"),
                getUuid(nbt, "vFrontTarget"), getUuid(nbt, "vFrontResult"),
                getUuid(nbt, "vBackTarget"), getUuid(nbt, "vBackResult"),
                getUuid(nbt, "vFrontLabelTarget"), getUuid(nbt, "vFrontLabelResult"),
                getUuid(nbt, "vBackLabelTarget"), getUuid(nbt, "vBackLabelResult"),
                getUuid(nbt, "vHitBase"), getUuid(nbt, "vHitLeg"), getUuid(nbt, "vHitScreen"));
        scale = nbt.contains("scale")
                ? TimerDisplayVisual.clampScale(nbt.getFloat("scale")) : CustomBlocksConfig.timerDefaultScale;
        baseScale = nbt.contains("baseScale") ? TimerDisplayVisual.clampScale(nbt.getFloat("baseScale")) : 1.0f;
        legScale = nbt.contains("legScale") ? TimerDisplayVisual.clampScale(nbt.getFloat("legScale")) : 1.0f;
        screenScale = nbt.contains("screenScale") ? TimerDisplayVisual.clampScale(nbt.getFloat("screenScale")) : 1.0f;
        textColor = nbt.contains("textColor") ? (nbt.getInt("textColor") & 0xFFFFFF) : TimerDisplayVisual.DEFAULT_TEXT_COLOR;
        textScale = nbt.contains("textScale") ? TimerDisplayVisual.clampTextScale(nbt.getFloat("textScale")) : 1.0f;
        yaw = nbt.contains("yaw") ? nbt.getFloat("yaw") : Float.NaN; // absent (old stands) → reseed from facing
        lastRender = null; // force a refresh on the next tick after load
    }

    private static void putUuid(NbtCompound nbt, String key, @Nullable UUID id) {
        if (id != null) nbt.putUuid(key, id);
    }

    private static @Nullable UUID getUuid(NbtCompound nbt, String key) {
        return nbt.containsUuid(key) ? nbt.getUuid(key) : null;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }
}
