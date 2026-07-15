/**
 * GuessPosePayload.java — Group 30 (Guess Mode) · G30-4 pose editor.
 *
 * Client→server packet sent by the Guess Settings screen's Pose tab when a slider is released, a preset is
 * clicked, or Reset is pressed. Carries the WHOLE six-angle pose (both arms' up/down · in/out · twist) as one
 * compact comma-separated string of radians "rp,ry,rr,lp,ly,lr" — a single field keeps the codec trivial
 * (PacketCodec of one STRING) and matches the CreateStudioPayload "delimited string" convention.
 *
 * The server (see CustomBlocksMod receiver) re-checks op (level 2), writes all six into GuessPoseStore in one
 * atomic save, then re-broadcasts via GuessSync so every client's pose updates live. Client never mutates
 * server state (CLAUDE.md §5.8) — this replaces the old typeable `/cb guess pose …` command, which was removed
 * so the screen is the single control surface.
 *
 * Registered playC2S in CustomBlocksMod, sent by client/gui/GuessSettingsScreen.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GuessPosePayload(String pose) implements CustomPayload {

    public static final CustomPayload.Id<GuessPosePayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "guess_pose"));

    public static final PacketCodec<PacketByteBuf, GuessPosePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, GuessPosePayload::pose, GuessPosePayload::new);

    /** Build the payload from six radian angles in slider order [rp, ry, rr, lp, ly, lr]. */
    public static GuessPosePayload of(float rp, float ry, float rr, float lp, float ly, float lr) {
        return new GuessPosePayload(rp + "," + ry + "," + rr + "," + lp + "," + ly + "," + lr);
    }

    /** Parse the six radian angles, or null if the string is malformed (server ignores a null). */
    public float[] radians() {
        if (pose == null) return null;
        String[] p = pose.split(",");
        if (p.length != 6) return null;
        try {
            float[] v = new float[6];
            for (int i = 0; i < 6; i++) v[i] = Float.parseFloat(p[i].trim());
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
