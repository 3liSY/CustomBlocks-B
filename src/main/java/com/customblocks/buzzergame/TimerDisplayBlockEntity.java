/**
 * TimerDisplayBlockEntity.java — Group 31 (BuzzerGame) item 1 (physical stand display, static version).
 *
 * The logic/anchor behind an (invisible) timer-stand block. It owns nothing visual itself — instead it
 * spawns and drives three server-side display entities via {@link TimerDisplayVisual} (stand model + two
 * digit panels). Like a buzzer, it stores only its link back to an admin panel ({@code panelPos} +
 * {@code sessionId}) and pulls everything else through that panel's {@link PanelSession} (design lock:
 * the session is the single source of truth; children never cross-reference each other).
 *
 * Each server tick it: makes sure its display entities exist (spawns them once, just after placement),
 * reads the current digit string from its linked panel (or the idle "0.00" when unlinked), and re-pushes
 * the digits to both faces only when they change. Its three entity UUIDs are persisted, so a reload
 * re-attaches to the same entities (they live in this same chunk) instead of duplicating them.
 *
 * Depends on: BuzzerGameRegistry (BlockEntityType), TimerDisplayVisual, AdminPanelBlockEntity, PanelSession
 * Called by:  TimerDisplayBlock (createBlockEntity, ticker, place/break), BuzzerGameWand (link/resize), NBT
 */
package com.customblocks.buzzergame;

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

    private static final TimerDisplayVisual.Handles NO_VISUAL = new TimerDisplayVisual.Handles(null, null, null);
    private static final String IDLE_DIGITS = "0.00";

    /** Stable id for this display, assigned on placement. Links key off it (session screen set). */
    private UUID displayId = UUID.randomUUID();

    /** Link back to an admin panel (null when unlinked); sessionId guards a replaced panel. */
    private @Nullable BlockPos panelPos;
    private @Nullable UUID sessionId;

    /** UUIDs of the three spawned display entities (persisted so a reload re-attaches, not duplicates). */
    private TimerDisplayVisual.Handles visual = NO_VISUAL;

    /** Size multiplier (item 2 resize); 1.0 = the default stand. Persisted; drives visuals + hitbox. */
    private float scale = TimerDisplayVisual.SCALE_MEDIUM;

    /** Facing angle in degrees (0-360). NaN = not yet set → lazily seeded from the placed block facing on
     *  the first tick. The wand's Rotate mode nudges it ±5°. Persisted; drives the model + both digit faces. */
    private float yaw = Float.NaN;

    /** Digit look: true = custom 7-segment LED font (default, item 3), false = plain text. Persisted. */
    private boolean ledDigits = true;

    // transient: skip redundant pushes
    private String lastDigits = "";
    private int lastColor = 0;
    private float lastScale = -1f;
    private float lastYaw = Float.NaN;
    private boolean lastLed = true;

    public TimerDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(BuzzerGameRegistry.TIMER_DISPLAY_ENTITY, pos, state);
    }

    public UUID getDisplayId() {
        return displayId;
    }

    public @Nullable BlockPos getPanelPos() {
        return panelPos;
    }

    public @Nullable UUID getSessionId() {
        return sessionId;
    }

    public boolean isLinked() {
        return panelPos != null && sessionId != null;
    }

    /** Bind this display to a panel's session (called by the link wand). */
    public void link(BlockPos panelPos, UUID sessionId) {
        this.panelPos = panelPos == null ? null : panelPos.toImmutable();
        this.sessionId = sessionId;
        markDirty();
    }

    /** Forget the panel link (called when a link goes stale). */
    public void clearLink() {
        this.panelPos = null;
        this.sessionId = null;
        markDirty();
    }

    public float getScale() {
        return scale;
    }

    /** Set the size multiplier (clamped); the next server tick re-scales the visuals + hitbox. */
    public void setScale(float newScale) {
        this.scale = TimerDisplayVisual.clampScale(newScale);
        markDirty();
    }

    /** Current facing angle in degrees (0-360); 0 until the first tick seeds it from the placed facing. */
    public float getYaw() {
        return Float.isNaN(yaw) ? 0f : yaw;
    }

    /** Nudge the facing angle by delta degrees (wraps 0-360); the next server tick re-points model + digits. */
    public void rotate(float deltaDeg) {
        float base = Float.isNaN(yaw) ? 0f : yaw;
        yaw = wrapDeg(base + deltaDeg);
        markDirty();
    }

    private static float wrapDeg(float d) {
        d %= 360f;
        return d < 0f ? d + 360f : d;
    }

    /** True = LED 7-segment digits, false = plain text. */
    public boolean isLed() {
        return ledDigits;
    }

    /** Flip between the LED 7-segment and plain-text digit look; next tick re-pushes both faces. */
    public void toggleDigitStyle() {
        ledDigits = !ledDigits;
        markDirty();
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
        AdminPanelBlockEntity panel = be.linkedPanel(world);
        String digits = panel == null ? IDLE_DIGITS : panel.session().screenText();
        int color = colorFor(panel);

        if (!be.visual.complete()) {
            be.visual = TimerDisplayVisual.spawnAll(serverWorld, pos, be.yaw, digits, color, be.scale, be.ledDigits);
            be.lastDigits = digits;
            be.lastColor = color;
            be.lastScale = be.scale;
            be.lastYaw = be.yaw;
            be.lastLed = be.ledDigits;
            be.markDirty();
            return;
        }
        if (be.scale != be.lastScale || be.yaw != be.lastYaw) {
            TimerDisplayVisual.updateStand(serverWorld, be.visual.stand(), pos, be.yaw, be.scale);
            TimerDisplayVisual.updateDigits(serverWorld, be.visual, pos, be.yaw, digits, color, be.scale, be.ledDigits);
            be.lastScale = be.scale;
            be.lastYaw = be.yaw;
            be.lastDigits = digits;
            be.lastColor = color;
            be.lastLed = be.ledDigits;
            return;
        }
        if (!digits.equals(be.lastDigits) || color != be.lastColor || be.ledDigits != be.lastLed) {
            TimerDisplayVisual.updateDigits(serverWorld, be.visual, pos, be.yaw, digits, color, be.scale, be.ledDigits);
            be.lastDigits = digits;
            be.lastColor = color;
            be.lastLed = be.ledDigits;
        }
    }

    /** Bright green while a round is timing/finished; dim green while idle or counting down. */
    private static int colorFor(@Nullable AdminPanelBlockEntity panel) {
        if (panel == null) return TimerDisplayVisual.IDLE_DIGIT_COLOR;
        SessionState s = panel.session().state();
        boolean live = s == SessionState.RUNNING || s == SessionState.RESULTS || s == SessionState.FINISHED;
        return live ? TimerDisplayVisual.DIGIT_COLOR : TimerDisplayVisual.IDLE_DIGIT_COLOR;
    }

    /** The admin panel this display is linked to, only if the link is still valid (matching session id). */
    private @Nullable AdminPanelBlockEntity linkedPanel(World world) {
        if (panelPos == null || sessionId == null) return null;
        if (world.getBlockEntity(panelPos) instanceof AdminPanelBlockEntity panel
                && sessionId.equals(panel.session().sessionId())) {
            return panel;
        }
        return null;
    }

    /** Kill this display's entities (called when the block is broken). */
    public void despawnVisual(ServerWorld world) {
        if (visual.complete()) TimerDisplayVisual.despawn(world, visual);
        visual = NO_VISUAL;
        lastDigits = "";
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putUuid("displayId", displayId);
        if (panelPos != null) nbt.putLong("panelPos", panelPos.asLong());
        if (sessionId != null) nbt.putUuid("sessionId", sessionId);
        if (visual.stand() != null) nbt.putUuid("vStand", visual.stand());
        if (visual.front() != null) nbt.putUuid("vFront", visual.front());
        if (visual.back() != null) nbt.putUuid("vBack", visual.back());
        nbt.putFloat("scale", scale);
        if (!Float.isNaN(yaw)) nbt.putFloat("yaw", yaw);
        nbt.putBoolean("led", ledDigits);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        if (nbt.containsUuid("displayId")) displayId = nbt.getUuid("displayId");
        panelPos = nbt.contains("panelPos") ? BlockPos.fromLong(nbt.getLong("panelPos")) : null;
        sessionId = nbt.containsUuid("sessionId") ? nbt.getUuid("sessionId") : null;
        UUID stand = nbt.containsUuid("vStand") ? nbt.getUuid("vStand") : null;
        UUID front = nbt.containsUuid("vFront") ? nbt.getUuid("vFront") : null;
        UUID back = nbt.containsUuid("vBack") ? nbt.getUuid("vBack") : null;
        visual = new TimerDisplayVisual.Handles(stand, front, back);
        scale = nbt.contains("scale")
                ? TimerDisplayVisual.clampScale(nbt.getFloat("scale")) : TimerDisplayVisual.SCALE_MEDIUM;
        yaw = nbt.contains("yaw") ? nbt.getFloat("yaw") : Float.NaN; // absent (old stands) → reseed from facing
        ledDigits = !nbt.contains("led") || nbt.getBoolean("led");   // absent (old stands) → default LED
        lastDigits = "";  // force a refresh on the next tick after load
        lastScale = -1f;  // force a re-scale push on the next tick after load
        lastYaw = Float.NaN; // force a re-point push on the next tick after load
        lastLed = ledDigits;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }
}
