/**
 * BuzzerPanelActionPayload.java — Group 31 item 4 (admin panel Screen).
 *
 * Client→server: a button on the BuzzerGame admin Screen was clicked. Carries the panel's block position
 * (packed long), an action code (see the constants), and a numeric argument used only by TARGET_SET (the
 * exact target seconds) and UNLINK (the 1-based buzzer index). The server validates op + finds the panel,
 * runs the action on its {@link com.customblocks.buzzergame.PanelSession}, then pushes a fresh snapshot back
 * so the open Screen refreshes.
 *
 * Registered + received in CustomBlocksMod (server), sent by the client BuzzerPanelScreen.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  BuzzerPanelScreen (sends), CustomBlocksMod / BuzzerPanelNet (receives)
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BuzzerPanelActionPayload(long panelPos, int action, double arg) implements CustomPayload {

    // action codes
    public static final int START = 0;
    public static final int STOP = 1;
    public static final int RESET = 2;
    public static final int REVEAL = 3;
    public static final int MODE = 4;          // cycle Solo → Duel → Party
    public static final int FORMAT = 5;        // toggle Precision ↔ Reaction
    public static final int COUNTDOWN = 6;     // cycle Off → 1 → 2 → 3 → 5 → 10
    public static final int FALSESTART = 7;    // cycle DQ → Penalty → Ignore
    public static final int TARGET_PRESET = 8; // cycle 3 → 5 → 10 → 15 → 30 → 60s
    public static final int TARGET_SET = 9;    // arg = exact seconds
    public static final int UNLINK = 10;       // arg = 1-based buzzer index
    public static final int REFRESH = 11;      // no-op, just re-send the snapshot

    public static final CustomPayload.Id<BuzzerPanelActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "buzzer_panel_action"));

    public static final PacketCodec<PacketByteBuf, BuzzerPanelActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_LONG, BuzzerPanelActionPayload::panelPos,
                    PacketCodecs.INTEGER,  BuzzerPanelActionPayload::action,
                    PacketCodecs.DOUBLE,   BuzzerPanelActionPayload::arg,
                    BuzzerPanelActionPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
