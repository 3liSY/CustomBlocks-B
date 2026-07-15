/**
 * BulkAction.java — Group 07 (Bulk Operations Hub — Console tab). CLIENT-ONLY.
 *
 * Builds and ships the {@link BulkActionPayload} an Execute produces, split out of {@link BulkWorkbenchScreen}
 * to hold the ≤500-line cap. The Screen never mutates blocks itself: it hands its current intent here, this
 * maps it to a typed payload, and the server runs the real handler and answers with a fresh snapshot.
 *
 * Re-ID is special — it sends an explicit {@code old=new,…} map of just the valid, hand-edited boxes rather
 * than a pattern, so each new id is exactly what the player typed (§G07-4 B9).
 *
 * Depends on: BulkWorkbenchScreen (getters), BulkOpSpec, BulkActionPayload, ClientPlayNetworking.
 * Called by:  BulkWorkbenchScreen (Execute confirm + PICK_DONE hand-off).
 */
package com.customblocks.client.gui;

import com.customblocks.network.payloads.BulkActionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;

@Environment(EnvType.CLIENT)
final class BulkAction {

    private BulkAction() {} // static-only

    /** Ship the op the player confirmed. */
    static void send(BulkWorkbenchScreen s) {
        if (s.opIndex() == BulkOpSpec.OP_REID) {
            // §G27.22b Re-ID by TEMPLATE: number the non-locked included blocks 1..k and send an explicit map
            // (matches BulkWorkbenchModel.reidPreview) — the server re-checks locked/invalid/taken/clash.
            StringBuilder map = new StringBuilder();
            int n = 0;
            for (String id : s.scopeIds()) {
                if (s.locked().contains(id)) continue;
                n++;
                if (map.length() > 0) map.append(',');
                map.append(id).append('=').append(BulkWorkbenchModel.numTokens(s.textA(), n));
            }
            ClientPlayNetworking.send(new BulkActionPayload(BulkActionPayload.REID_MAP, map.toString(), "", "", ""));
            return;
        }
        ClientPlayNetworking.send(new BulkActionPayload(BulkOpSpec.payloadOp(s.opIndex()), scopeArg(s), p1(s), p2(s), p3(s)));
    }

    /** The Execute scope: the included ids (ticked − excluded), with the 1-id comma guard. Server skips locked. */
    static String scopeArg(BulkWorkbenchScreen s) {
        List<String> ids = s.scopeIds();
        String joined = String.join(",", ids);
        return ids.size() == 1 ? joined + "," : joined;
    }

    /** The pick-mode (G12 hand-off) scope: the ticked-id list (with the 1-id comma guard). */
    static String scopeExpr(BulkWorkbenchScreen s) {
        String joined = String.join(",", s.ticked());
        return s.ticked().size() == 1 ? joined + "," : joined;
    }

    static String p1(BulkWorkbenchScreen s) {
        return switch (s.opIndex()) {
            case BulkOpSpec.OP_PROPERTY -> s.property();
            case BulkOpSpec.OP_RENAME, BulkOpSpec.OP_REID -> s.textMode();
            case BulkOpSpec.OP_CATEGORY -> s.textA();
            case BulkOpSpec.OP_EXPORT   -> s.exportFormat();
            case BulkOpSpec.OP_LOCK     -> s.lockMode();
            case BulkOpSpec.OP_FAVORITE -> s.favMode();
            case BulkOpSpec.OP_RECOLOR  -> String.valueOf(s.recolorHue());
            default -> "";
        };
    }

    static String p2(BulkWorkbenchScreen s) {
        return switch (s.opIndex()) {
            case BulkOpSpec.OP_PROPERTY -> s.value();
            case BulkOpSpec.OP_RENAME, BulkOpSpec.OP_REID -> s.textA();
            default -> "";
        };
    }

    static String p3(BulkWorkbenchScreen s) {
        return switch (s.opIndex()) {
            case BulkOpSpec.OP_RENAME, BulkOpSpec.OP_REID -> "replace".equals(s.textMode()) ? s.textB() : "";
            default -> "";
        };
    }
}
