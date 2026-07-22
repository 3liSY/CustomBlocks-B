/**
 * CbKick.java — GROUP 04 §G04-5 kick registry + cause factories. CLIENT-SIDE ONLY.
 *
 * Two jobs:
 *   1. A one-slot holder. A CustomBlocks kick that fires client-side (the registry desync) ARMS its
 *      {@link CbKickInfo} here right before it throws; the {@code DisconnectedScreenMixin} CONSUMES it
 *      when the disconnect screen opens and swaps in {@link CbKickScreen}. One value, armed then read
 *      once, so a later unrelated disconnect can't inherit it.
 *   2. The cause catalogue. Every known CustomBlocks kick reason has a factory here that builds its
 *      {@link CbKickInfo}. A new cause is one method — the Screen, the colours and the report never
 *      change. {@link #forCause} dispatches the hidden {@code /cb debug kick <cause>} dev command so
 *      every screen can be force-fired and tested without reproducing a real crash.
 *
 * Deep-search 2026-07-15: the codebase has exactly ONE real CustomBlocks kick — the registry /
 * max_blocks desync. The resource-pack and generic/mid-game shapes are covered by the framework (and
 * the dev command) so a future cause plugs in free; no CB code path fires them today.
 *
 * Depends on: CbKickInfo, FabricLoader, SharedConstants. Used by: RegistrySyncHealMixin (arm),
 * DisconnectedScreenMixin (consume), CustomBlocksClient (dev preview), CbKickInfo (versions).
 */
package com.customblocks.client.kick;

import com.customblocks.CustomBlocksMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class CbKick {

    private CbKick() {} // static-only

    private static volatile CbKickInfo pending;

    /** Arm the next disconnect screen with this kick's info (client-side kicks, just before they fire). */
    public static void arm(CbKickInfo info) { pending = info; }

    /** Take the armed kick, if any, and clear it — so it is applied to exactly one disconnect screen. */
    public static CbKickInfo consume() {
        CbKickInfo p = pending;
        pending = null;
        return p;
    }

    // ── Cause catalogue ────────────────────────────────────────────────────────

    /** The registry / max_blocks desync — the ONE real CustomBlocks kick (RegistrySyncHealMixin). */
    public static CbKickInfo registryDesync(int serverNeeds, int youWere) {
        return new CbKickInfo(
                "CustomBlocks could not connect",
                "Your client and this server have different CustomBlocks block limits.",
                "The server has custom blocks that your client does not have room for yet.",
                "Your client limit was raised to " + serverNeeds + " automatically. Fully restart "
                        + "Minecraft, then join the server again.",
                "CB-KICK-REGISTRY",
                "Received registry entries that are unknown to this client "
                        + "(customblocks:slot_" + youWere + "…" + Math.max(0, serverNeeds - 1) + ").",
                "Server max_blocks = " + serverNeeds + "; this client was registered for " + youWere
                        + ". The client block registry is frozen at launch, so the raised limit only "
                        + "takes effect after a full restart.",
                "Client CustomBlocks block limit lower than the server's.",
                "Client limit raised to " + serverNeeds + "; restart Minecraft and rejoin.",
                "Dedicated or integrated server",
                "Configuration (registry sync)",
                "Joining a server that hosts more custom blocks than your client was set up for.");
    }

    /** A CustomBlocks resource-pack that failed to apply (framework-covered; no CB path fires it today). */
    public static CbKickInfo resourcePackFailure(String detail) {
        return new CbKickInfo(
                "CustomBlocks pack could not be applied",
                "The CustomBlocks texture pack from this server did not finish applying.",
                "The pack may have failed to download, or your graphics settings rejected its size.",
                "Rejoin the server. If it keeps happening, lower the texture size with /cblowres 256 "
                        + "(or 128) and try again.",
                "CB-KICK-RESOURCEPACK",
                detail == null || detail.isBlank() ? "Resource-pack apply failed (no detail)." : detail,
                "The server-sent CustomBlocks pack failed at the resource-reload stage on the client.",
                "Resource-pack download or apply failure.",
                "Rejoin; if it persists, reduce the client texture size via /cblowres.",
                "Dedicated or integrated server",
                "Login (resource-pack apply)",
                "Receiving the CustomBlocks texture pack while joining.");
    }

    /** A generic CustomBlocks kick shape (framework fallback / mid-game shape — none fires today). */
    public static CbKickInfo generic(String detail) {
        return new CbKickInfo(
                "CustomBlocks disconnected you",
                "CustomBlocks ran into a problem and had to end your connection.",
                "Something CustomBlocks depends on was in an unexpected state.",
                "Rejoin the server. If it keeps happening, use /cb incidents to see the details, or send "
                        + "the report below to the server owner.",
                "CB-KICK-GENERIC",
                detail == null || detail.isBlank() ? "No raw error was supplied." : detail,
                "A CustomBlocks system requested a disconnect. See the raw error above for specifics.",
                "An unexpected CustomBlocks state during play.",
                "Rejoin; report if it recurs.",
                "Dedicated or integrated server",
                "Play",
                "Playing on a server running CustomBlocks.");
    }

    /** Dispatch for the hidden {@code /cb debug kick <cause>} dev command. */
    public static CbKickInfo forCause(String cause) {
        String c = cause == null ? "" : cause.trim().toLowerCase(Locale.ROOT);
        return switch (c) {
            case "registry", "maxslots", "slots" -> registryDesync(400, 300);
            case "resourcepack", "pack", "rp"    -> resourcePackFailure("Preview: resource-pack apply failed.");
            default                               -> generic("Preview: generic CustomBlocks kick.");
        };
    }

    /** The known cause keywords the dev command accepts (for tab-complete + docs). */
    public static java.util.List<String> causes() {
        return java.util.List.of("registry", "resourcepack", "generic");
    }

    // ── Environment (captured up front so Copy Report works while unable to join) ──

    public static String mcVersion() {
        try { return SharedConstants.getGameVersion().getName(); }
        catch (Throwable t) { return "unknown"; }
    }

    public static String cbVersion() {
        return FabricLoader.getInstance()
                .getModContainer(CustomBlocksMod.MOD_ID)
                .map(m -> m.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
