/**
 * BulkActionPayload.java — Group 07 §G07-3 (Bulk Workbench Screen).
 *
 * Client→server: the Bulk Workbench's Apply (or a Browse-tab action) fired. Carries an op code, the
 * BulkScope expression the op runs over, and up to three string params whose meaning depends on the op
 * (see the constants). The server ({@link com.customblocks.command.handlers.BulkNet}) validates the op,
 * hops to the server thread, calls the existing bulk handler directly — NO command-string assembly, so a
 * category name with a space can never become a parse failure — then pushes a fresh snapshot back via
 * OpenGuiPayload so the open Screen refreshes in place.
 *
 * {@code scope} uses a widened 64 KiB string codec: a hand-ticked selection is sent as an explicit
 * comma-separated id list, and 1028 ids can exceed the 32767-char default of PacketCodecs.STRING.
 *
 * Registered + received in CustomBlocksMod (server), sent by the client BulkWorkbenchScreen.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  BulkWorkbenchScreen (sends), CustomBlocksMod / BulkNet (receives)
 */
package com.customblocks.network.payloads;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BulkActionPayload(int op, String scope, String p1, String p2, String p3) implements CustomPayload {

    // op codes — p1/p2/p3 usage noted per op ("—" = unused, send "")
    /** Re-send the snapshot; changes nothing. p1 = requested tab ("browse" / "bulk" / "pick"). */
    public static final int REFRESH = 0;
    /** Edit a setting. p1 = property (glow/hardness/sound/collision), p2 = value. */
    public static final int PROPERTY = 1;
    /** Rename display names. p1 = prefix|suffix|replace, p2 = text A, p3 = text B (replace only). */
    public static final int RENAME = 2;
    /** Move to a category. p1 = category name ("none" clears). */
    public static final int CATEGORY = 3;
    /** Duplicate each matched block into "&lt;id&gt;_copy". — no params. */
    public static final int DUPLICATE = 4;
    /** Export the matched blocks. p1 = json|txt|csv|md|html|yaml|png. */
    public static final int EXPORT = 5;
    /** Flip a flag. p1 = lock|unlock|favorite|unfavorite. */
    public static final int FLAG = 6;
    /** Re-id blocks. p1 = prefix|suffix|replace, p2 = text A, p3 = text B (replace only). */
    public static final int REID = 7;
    /** Delete the matched blocks. — no params. */
    public static final int DELETE = 8;
    /** Browse tab: open the full block editor. p1 = block id; scope unused. */
    public static final int OPEN_EDITOR = 9;
    /** Pick mode (G12 hand-off): return the ticked ids in {@code scope} to the caller. */
    public static final int PICK_DONE = 10;
    /**
     * Re-ID by an EXPLICIT per-block map (B9 hybrid editor): {@code scope} = "old1=new1,old2=new2,…", one pair
     * per hand-edited box. p1/p2/p3 unused. Unlike {@link #REID} (a pattern transform), each new id is chosen
     * individually, so the server re-ids exactly these pairs (re-checking locked/invalid/taken/clash at apply).
     */
    public static final int REID_MAP = 11;
    /** In-screen Undo (§G07-4): undo N steps (p1 = count, blank = 1) on the player's stack, then refresh the Hub. */
    public static final int UNDO = 12;
    /** In-screen Redo (§G07-4): redo N steps (p1 = count, blank = 1) on the player's stack, then refresh the Hub. */
    public static final int REDO = 13;
    /** Recolor (§G27.22b — net-new 10th op): batch hue-shift the matched blocks. p1 = hue degrees (0-360).
     *  Batch recolor is always forced to EDGE mode server-side (never player "full" mode — KNOWN_PITFALLS). */
    public static final int RECOLOR = 14;

    public static final CustomPayload.Id<BulkActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "bulk_action"));

    /** A ticked selection travels as an explicit id list — too long for the 32767-char default. */
    private static final PacketCodec<ByteBuf, String> SCOPE = PacketCodecs.string(65536);

    public static final PacketCodec<PacketByteBuf, BulkActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, BulkActionPayload::op,
                    SCOPE,                BulkActionPayload::scope,
                    PacketCodecs.STRING,  BulkActionPayload::p1,
                    PacketCodecs.STRING,  BulkActionPayload::p2,
                    PacketCodecs.STRING,  BulkActionPayload::p3,
                    BulkActionPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
