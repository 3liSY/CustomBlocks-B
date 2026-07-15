/**
 * BipedArmPoseMixin.java — Group 30 (Guess Mode) — third-person centered pose. CLIENT-SIDE ONLY.
 *
 * Forces both arms into a centered "holding a block with both hands" pose whenever a PLAYER is holding a
 * custom block FLAGGED for them in guess mode (the synced set in {@link ClientGuessState}). This is the everyone-
 * sees-it, camera-facing piece of guess mode — a watcher (or a stream) sees the guesser cradle the mystery
 * block, and the held item follows the arms to centre. Reads the SYNCED guess set (not a local flag), so it
 * applies to any player's model on every client; the holder's own name-blank + disguise stay local (no sync).
 *
 * Targets BipedEntityModel.setAngles (where the arm ModelParts live), injected at TAIL — i.e. AFTER the
 * vanilla arm angles are computed. PlayerEntityModel.setAngles calls this via {@code super.setAngles(...)}
 * and THEN copies the sleeve overlay from the arms, so re-posing the arms here makes the sleeves follow for
 * free (no sleeve shadow needed). The guard is player-only + holding-a-custom-block, so other bipeds
 * (zombies, armour stands) are never touched — their uuid is never in the guess set anyway.
 *
 * {@code require = 0}: the group doc (GROUP_30_GUESS_MODE.md) flags the third-person pose as the highest-
 * uncertainty piece needing in-game confirmation; if the mapped target ever shifts, guess mode degrades to
 * "no pose" instead of crashing the client at load.
 *
 * v3 slice 2 — SMOOTH transition (was an instant snap). A per-player progress eases 0→1 while the player
 * holds a flagged block and 1→0 when they stop, over ~{@link #TRANSITION_TICKS} ticks, and the arm angles
 * are LERPed from the vanilla pose toward the centered pose by that progress — so the block glides into and
 * out of the two-handed hold instead of popping. The clock is the model's own animationProgress (age in
 * ticks + tickDelta), so it's frame-rate independent and needs no world/tick hook.
 *
 * G30-4 (pose editor) — the target pose is no longer hardcoded here: the six arm angles come from the shared,
 * owner-tunable {@link ClientGuessState} pose (server {@code GuessPoseStore}, set from the Guess Settings
 * screen's Pose tab). The transition/lerp math is unchanged; only the target angles are now dynamic.
 *
 * Registered as a client mixin in customblocks.mixins.json.
 */
package com.customblocks.mixin;

import com.customblocks.block.SlotBlock;
import com.customblocks.client.ClientGuessState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
@Mixin(BipedEntityModel.class)
public abstract class BipedArmPoseMixin {

    // Declared on BipedEntityModel — only their pitch/yaw/roll are mutated, never the field reassigned.
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightArm;

    /** v3 slice 2 — per-player transition state: {@code [progress 0..1, lastClockTicks]}. Keyed by player UUID,
     *  created lazily only for players that actually enter guess mode, removed once fully back to normal. */
    private static final Map<UUID, float[]> CB_GUESS_POSE = new ConcurrentHashMap<>();

    /** Transition length in ticks (~0.3s at 20 tps). Placeholder feel — tune after watching it in-game. */
    private static final float TRANSITION_TICKS = 6f;

    // The target arm angles now live in GuessPoseStore (server) → ClientGuessState (synced), owner-tunable per
    // arm from the Guess Settings screen's Pose tab (G30-4). The DEFAULTS there reproduce the pose this mixin
    // used to hardcode, so nothing moves until the owner tunes it. Kept rationale for those defaults:
    //   • Pitch -π/2 (−90°): arms straight forward + horizontal so the block sits at chest, not tipped down.
    //   • Yaw ∓0.5 rad: toe each hand IN to MEET at centre. Magnitude ≈ asin(shoulderOffset/armLen) ≈
    //     asin(5/10) ≈ 0.52 reaches the midline. The sign is negated for the right arm because ModelPart
    //     applies rotations X(pitch) THEN Y(yaw): pitching forward first inverts the naive yaw direction, so a
    //     positive right-arm yaw would splay the hand OUTWARD — negating it brings both hands in.

    // require = 0: the group doc flags the third-person pose as the highest-uncertainty piece; if the mapped
    // target ever shifts, guess mode degrades to "no pose" instead of crashing the client at load.
    @Inject(method = "setAngles", at = @At("TAIL"), require = 0)
    private void customblocks$guessPose(LivingEntity entity, float limbAngle, float limbDistance,
                                        float animationProgress, float headYaw, float headPitch,
                                        CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity)) return;

        UUID id = entity.getUuid();

        // G30-4 pose editor — while the settings screen is open, force the WORKING pose onto the preview
        // entity (the local player drawn as the dummy) at full strength, no transition, so dragging a slider
        // moves the arms instantly. Bypasses the holding/flag gate; other players are unaffected.
        if (ClientGuessState.isPosePreview(id)) {
            rightArm.pitch = ClientGuessState.pvRightPitch();
            rightArm.yaw   = ClientGuessState.pvRightYaw();
            rightArm.roll  = ClientGuessState.pvRightRoll();
            leftArm.pitch  = ClientGuessState.pvLeftPitch();
            leftArm.yaw    = ClientGuessState.pvLeftYaw();
            leftArm.roll   = ClientGuessState.pvLeftRoll();
            return;
        }

        boolean holding = holdsFlaggedBlock(entity);

        // Touch the state map only for players in/entering the pose — normal players cost nothing.
        float[] st = CB_GUESS_POSE.get(id);
        if (st == null) {
            if (!holding) return;                      // ordinary player, nothing to do
            st = new float[]{0f, animationProgress};   // just started holding → ease up from 0
            CB_GUESS_POSE.put(id, st);
        }

        // Advance the eased progress toward the target (1 = holding, 0 = not) using the model's own clock.
        float dt = animationProgress - st[1];
        st[1] = animationProgress;
        if (dt < 0f) dt = 0f;          // clock reset/rollover → no step this frame
        if (dt > 4f) dt = 4f;          // frame hitch → cap the jump so it still visibly glides
        float step = dt / TRANSITION_TICKS;
        st[0] = holding ? Math.min(1f, st[0] + step) : Math.max(0f, st[0] - step);
        float t = st[0];

        if (t <= 0f) {                 // fully back to the normal pose
            if (!holding) CB_GUESS_POSE.remove(id);
            return;                    // leave the vanilla angles untouched
        }

        // Smoothstep for a softer ease, then blend the vanilla angles (already set at TAIL) toward the pose.
        // G30-4: the target angles are the shared, owner-tunable pose synced in ClientGuessState (per arm,
        // independent pitch/yaw/roll) — no longer the two hardcoded constants. Defaults reproduce the old
        // pose, so nothing moves until the owner tunes it in the Guess Settings screen.
        float e = t * t * (3f - 2f * t);
        rightArm.pitch = lerp(rightArm.pitch, ClientGuessState.poseRightPitch(), e);
        rightArm.yaw   = lerp(rightArm.yaw,   ClientGuessState.poseRightYaw(),   e);
        rightArm.roll  = lerp(rightArm.roll,  ClientGuessState.poseRightRoll(),  e);
        leftArm.pitch  = lerp(leftArm.pitch,  ClientGuessState.poseLeftPitch(),  e);
        leftArm.yaw    = lerp(leftArm.yaw,    ClientGuessState.poseLeftYaw(),    e);
        leftArm.roll   = lerp(leftArm.roll,   ClientGuessState.poseLeftRoll(),   e);
        // The sleeve overlay is copied from these arms by PlayerEntityModel.setAngles (our caller), so it follows.
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** True when this player holds (either hand) a custom block that is FLAGGED for guess mode for them. */
    private static boolean holdsFlaggedBlock(LivingEntity entity) {
        UUID id = entity.getUuid();
        return flagged(entity.getMainHandStack(), id) || flagged(entity.getOffHandStack(), id);
    }

    private static boolean flagged(ItemStack stack, UUID id) {
        if (stack == null || !(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof SlotBlock sb))
            return false;
        return ClientGuessState.disguisesSlot(id, sb.getSlotIndex());
    }
}
