/**
 * BackupActionPayload.java — Group 09 §G09-A4 (Backup Screen).
 *
 * Client→server packet sent when the player acts in the Backup Screen. The SERVER performs every
 * action through the exact same BackupCommands rail the chat commands use (validate → BackupManager →
 * pack-safe restore) and answers with a fresh OpenGuiPayload(BACKUP_SCREEN) so the screen re-renders
 * in place — the client only expresses intent; the server stays authoritative (CLAUDE.md §5.8).
 *
 *   action — one of the ACTION_* constants below.
 *   name   — the target backup's raw restore-by id (or the desired name when creating).
 *   arg    — the new name for a rename; "" otherwise.
 *
 * Registered playC2S in CustomBlocksMod (server receiver), sent by client/gui/BackupScreen.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BackupActionPayload(String action, String name, String arg) implements CustomPayload {

    public static final String ACTION_CREATE  = "create";   // name = desired name ("" → auto-named)
    public static final String ACTION_RESTORE = "restore";  // name = target
    public static final String ACTION_DELETE  = "delete";   // name = target
    public static final String ACTION_RENAME  = "rename";   // name = old, arg = new
    public static final String ACTION_PROTECT = "protect";  // name = target (toggles protect flag)

    public static final CustomPayload.Id<BackupActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "backup_action"));

    public static final PacketCodec<PacketByteBuf, BackupActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, BackupActionPayload::action,
                    PacketCodecs.STRING, BackupActionPayload::name,
                    PacketCodecs.STRING, BackupActionPayload::arg,
                    BackupActionPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
