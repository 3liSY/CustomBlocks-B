/**
 * GuessShowcasePayload.java — Group 30 (Guess Mode) · G30-8b Showcase.
 *
 * Client→server packet from the Guess Settings screen's Showcase tab when a slider is released. Carries the
 * shared showcase tuning — core spin, size — as one comma-separated string "inner,size", matching the
 * single-STRING codec convention of {@link GuessPosePayload}. (The glow toggle was scrapped — G30 §S: S6.)
 *
 * The server (see CustomBlocksMod receiver) re-checks op (level 2), writes GuessShowcaseStore, then
 * re-broadcasts via GuessSync so every placed Showcase updates live. Client never mutates server state (§5.8).
 *
 * Registered playC2S in CustomBlocksMod, sent by client/gui/GuessSettingsScreen.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GuessShowcasePayload(String data) implements CustomPayload {

    public static final CustomPayload.Id<GuessShowcasePayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "guess_showcase"));

    public static final PacketCodec<PacketByteBuf, GuessShowcasePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, GuessShowcasePayload::data, GuessShowcasePayload::new);

    /** Build from the two tuning values. */
    public static GuessShowcasePayload of(float inner, float size) {
        return new GuessShowcasePayload(inner + "," + size);
    }

    /** Parsed values: {inner, size}, or null if malformed (server ignores null). */
    public Parsed parse() {
        if (data == null) return null;
        String[] p = data.split(",");
        if (p.length != 2) return null;
        try {
            return new Parsed(Float.parseFloat(p[0].trim()), Float.parseFloat(p[1].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record Parsed(float inner, float size) {}

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
