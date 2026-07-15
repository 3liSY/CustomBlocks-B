/**
 * RegistrySyncHealMixin.java
 *
 * Group 21 / §9 (D13) — Phase A client self-heal net for the `max_blocks` (maxSlots) desync.
 *
 * Intercepts Fabric's CONFIGURATION-phase registry remap check — the exact place a client too low
 * on custom-block slots is kicked with the cryptic "Received N registry entries that are unknown
 * to this client". The remap map carries the FULL server registry snapshot, so we read the
 * server's highest `customblocks:slot_N` straight from it, compare it against what is ACTUALLY
 * registered in this client's live block registry, raise the local config to match (via the
 * shared MaxSlotsHealer), and throw a friendly RemapException whose Text Fabric surfaces on the
 * disconnect screen (FabricRegistryClientInit#getText -> RemapException#getText). One unavoidable
 * restart later, the client joins clean.
 *
 * IMPORTANT (the 2026-06-27 fix): we compare the server count against the LIVE registry, NOT
 * against CustomBlocksConfig.maxSlots. The heal raises that config field in memory, but the block
 * registry is frozen at launch — so on a same-session retry the config would falsely read "already
 * big enough" and let Fabric's raw kick leak. The registry is the only honest source of the count.
 *
 * Targets a Fabric API impl class (not Minecraft) -> remap = false. No server change needed: this
 * heals the owner AND every friend against any server, even one we cannot otherwise query.
 *
 * Registered as a client mixin in customblocks.mixins.json.
 */
package com.customblocks.mixin;

import com.customblocks.CustomBlocksMod;
import com.customblocks.core.MaxSlotsHealer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.fabricmc.fabric.impl.registry.sync.RemapException;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = RegistrySyncManager.class, remap = false)
public class RegistrySyncHealMixin {

    private static final String SLOT_PREFIX = "slot_";

    /**
     * HEAD of checkRemoteRemap(Map&lt;Identifier, Object2IntMap&lt;Identifier&gt;&gt;). The map holds
     * the full server registry snapshot, so customblocks:slot_0..slot_{serverMax-1} are all present.
     * We take the server's slot count (highest + 1) and compare it to the count LOCALLY REGISTERED
     * in this client's block registry. If the server needs more than this client physically has,
     * a kick is certain this session — so we heal the config (raise-only, for the next launch) and
     * throw the friendly screen BEFORE Fabric's raw error.
     */
    @Inject(method = "checkRemoteRemap", at = @At("HEAD"))
    private static void customblocks$healMaxSlots(Map<Identifier, Object2IntMap<Identifier>> map,
                                                  CallbackInfo ci) throws RemapException {
        if (map == null || map.isEmpty()) return;

        int serverMax = highestSlot(map) + 1;   // server's max_blocks (slot count)
        if (serverMax <= 0) return;              // server sent no customblocks slots -> nothing we own

        int localMax = localRegisteredSlots();   // what THIS client physically has (frozen at launch)
        if (serverMax <= localMax) return;       // local registry already covers the server -> no kick

        MaxSlotsHealer.ensureAtLeast(serverMax);                                       // single write path
        throw new RemapException(MaxSlotsHealer.restartScreenText(serverMax, localMax)); // friendly screen
    }

    /** Highest customblocks:slot_N index across the server's remap map; -1 if none. */
    private static int highestSlot(Map<Identifier, Object2IntMap<Identifier>> map) {
        int highest = -1;
        for (Object2IntMap<Identifier> entries : map.values()) {
            if (entries == null) continue;
            for (Identifier id : entries.keySet()) {
                int n = slotIndex(id);
                if (n > highest) highest = n;
            }
        }
        return highest;
    }

    /** Count of customblocks slots actually registered in this client right now (frozen at launch). */
    private static int localRegisteredSlots() {
        int max = 0;
        for (Identifier id : Registries.BLOCK.getIds()) {
            int n = slotIndex(id);
            if (n + 1 > max) max = n + 1;
        }
        return max;
    }

    /** Slot index for a `customblocks:slot_N` id; -1 if it is not one. */
    private static int slotIndex(Identifier id) {
        if (id == null || !CustomBlocksMod.MOD_ID.equals(id.getNamespace())) return -1;
        String path = id.getPath();
        if (!path.startsWith(SLOT_PREFIX)) return -1;
        try {
            return Integer.parseInt(path.substring(SLOT_PREFIX.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
