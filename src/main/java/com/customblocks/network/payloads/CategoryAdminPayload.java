/**
 * CategoryAdminPayload.java — Group 27 §G27.6 (Block Creation Studio, Category tab).
 *
 * Client→server packet for the lightweight category management done from the studio's Category tab:
 *   op   — "rename" | "delete" | "color" | "default"
 *   cat  — the target category name (lowercased server-side)
 *   arg  — op-specific: rename → new name · color → a §-colour tag (e.g. "§a") · default/delete → "".
 *
 * The SERVER applies it through the existing rails (SlotManager.setCategory, CategoryMetadataStore,
 * DefaultCategoryStore) in CategoryAdminBridge, then re-syncs the player's HUD cache so the studio's
 * chips refresh. Authoritative server (CLAUDE.md §5.8).
 *
 * Registered playC2S in CustomBlocksMod, sent by client/gui/StudioCategoryPanel.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CategoryAdminPayload(String op, String cat, String arg) implements CustomPayload {

    public static final CustomPayload.Id<CategoryAdminPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "category_admin"));

    public static final PacketCodec<PacketByteBuf, CategoryAdminPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, CategoryAdminPayload::op,
                    PacketCodecs.STRING, CategoryAdminPayload::cat,
                    PacketCodecs.STRING, CategoryAdminPayload::arg,
                    CategoryAdminPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
