/**
 * ArabicBreakPredictor.java — GROUP 13 §C: the missing HALF of the CP3b join-flow prediction.
 * CLIENT-SIDE.
 *
 * Responsibility: run {@link com.customblocks.arabic.ArabicSlotJoinFlow#onBreak} on the CLIENT the
 * tick the local player breaks a letter, so the neighbouring letters re-shape at click speed
 * instead of at network speed.
 *
 * WHY THIS FILE EXISTS (the §C regression, root-caused 2026-07-30):
 * {@code ArabicSlotJoinFlow} was written to run on both sides — the server authoritatively, the
 * client as prediction — and {@code SlotBlock.onPlaced} / {@code SlotBlock.onStateReplaced} are its
 * two entry points. Placement predicts correctly, because a locally-placed block DOES reach
 * {@code onPlaced} on the client. Breaking never did, and not because of a gate in this project:
 * vanilla {@code WorldChunk.setBlockState} calls {@code onStateReplaced} only when
 * {@code !world.isClient} — on the client it takes the other branch and merely drops the block
 * entity. Verified in the mapped 1.21.1 jar: the {@code isClient} test at offset 313 jumps past the
 * {@code onStateReplaced} invoke at 328. So the break hook was server-only by construction and no
 * amount of work inside the flow could have made it fire. The owner's report — "reflow works but
 * feels server-paced after break" — is exactly that: correct result, one round-trip late.
 *
 * Fabric's {@code ClientPlayerBlockBreakEvents.AFTER} is the client-side counterpart vanilla does
 * not give us. It fires after the client has removed the block, which matches the server's ordering
 * (the server also re-flows once the block is already gone), so both sides walk an identical world
 * and compute an identical answer — the authoritative packets then reconcile with no visible change.
 *
 * Deliberately narrow: only the LOCAL player's break predicts. A letter removed by anything else
 * (another player, the Deleter sweep, an explosion) still arrives as a server update, because the
 * client has nothing to predict from until it hears about it.
 *
 * Depends on: ArabicSlotJoinFlow (the flow + its own IN_FLOW / remote-session guards),
 *             SlotBlock (slot identity).
 * Called by:  CustomBlocksClient.onInitializeClient (register()), beside ClientSwapPredictor.
 *
 * See: GROUP_13_ARABIC.md §G13-25 CP3b, ADR-009 (the same predict-then-reconcile pattern).
 */
package com.customblocks.client;

import com.customblocks.arabic.ArabicSlotJoinFlow;
import com.customblocks.block.SlotBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;

@Environment(EnvType.CLIENT)
public final class ArabicBreakPredictor {

    private ArabicBreakPredictor() {} // static-only

    public static void register() {
        ClientPlayerBlockBreakEvents.AFTER.register((world, player, pos, state) -> {
            // Data-gated exactly like SlotBlock.onStateReplaced: a normal slot block or a number
            // carries no letter meta and must not start a word walk.
            if (!(state.getBlock() instanceof SlotBlock sb)) return;
            if (!ArabicSlotJoinFlow.isArabicSlot(world, sb.getSlotIndex(), sb.getSlotKey())) return;
            // The flow self-guards: IN_FLOW makes its own nested swaps no-ops, and canRun bails on a
            // remote session whose ClientView seam is not installed yet.
            ArabicSlotJoinFlow.onBreak(world, pos);
        });
    }
}
