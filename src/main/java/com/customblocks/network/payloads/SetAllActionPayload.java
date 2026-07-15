/**
 * SetAllActionPayload.java — Group 07 (Set All Screen).
 *
 * Client→server: the Set All Screen's "Apply to all" fired. Carries the chosen setting and its value; the
 * server ({@link com.customblocks.command.handlers.SetAllCommands}) runs the SAME validated auto-backup +
 * apply-to-all rail the `/cb setall <setting> <value>` chat command uses, so the screen only expresses intent
 * and the server stays authoritative (CLAUDE.md §5.8). No refresh is pushed back — the apply is async (a backup
 * runs off-thread first) and reports through chat.
 *
 * Registered playC2S in PayloadRegistrar, sent by client/gui/SetAllScreen.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetAllActionPayload(String setting, String value) implements CustomPayload {

    public static final CustomPayload.Id<SetAllActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "setall_action"));

    public static final PacketCodec<PacketByteBuf, SetAllActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, SetAllActionPayload::setting,
                    PacketCodecs.STRING, SetAllActionPayload::value,
                    SetAllActionPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
