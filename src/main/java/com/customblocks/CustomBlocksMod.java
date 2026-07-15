/**
 * CustomBlocksMod.java
 *
 * Responsibility: Main mod entrypoint (Fabric ModInitializer). Registers the slot pool,
 * loads saved blocks, registers the /cb command tree, runs the resource-pack HTTP server,
 * and pushes the pack to clients.
 *
 * Phase 4 (textures): the HTTP pack server starts with the world, rebuilds on every
 * retexture, and is sent to each player on join.
 *
 * Depends on: CustomBlocksConfig, SlotManager, SlotBlock, CommandRegistrar, ResourcePackServer
 * Called by:  Fabric loader via the "main" entrypoint in fabric.mod.json
 */
package com.customblocks;

import com.customblocks.block.SlotBlock;
import com.customblocks.command.CommandRegistrar;
import com.customblocks.core.AutoBackup;
import com.customblocks.core.OnboardingManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.item.ToolItems;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.customblocks.network.payloads.ArabicLabelsPayload;
import com.customblocks.network.payloads.SilentPackPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomBlocksMod implements ModInitializer {

    /** Mod id — matches fabric.mod.json and the assets/data namespace. */
    public static final String MOD_ID = "customblocks";

    /** Shared logger. All log lines are prefixed "[CustomBlocks]" per NFR-09. */
    public static final Logger LOGGER = LoggerFactory.getLogger("CustomBlocks");

    /** The mod's creative inventory tab for created blocks. */
    public static final RegistryKey<ItemGroup> CUSTOM_BLOCKS_TAB =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "blocks"));

    /** The mod's creative inventory tab for the hand tools (Phase 7). */
    public static final RegistryKey<ItemGroup> CUSTOM_TOOLS_TAB =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "tools"));

    /** Group 13 / G13-25 CP5: the dedicated Arabic tab, now listing the REAL letter/number slot
     *  blocks (the old customblocks:arabic_letter NBT system was removed entirely). */
    public static final RegistryKey<ItemGroup> ARABIC_JOIN_TAB =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "arabic_join"));

    @Override
    public void onInitialize() {
        LOGGER.info("[CustomBlocks] Initializing CustomBlocks v1.0.0 (Phase 10+)");

        CustomBlocksConfig.load();
        registerImageIOPlugins(); // Group 14 — let ImageIO find the bundled WebP plugin under Fabric's classloader
        // Group 13 / O6: seed the live Arabic form labels from config so server-side naming + the
        // join-send below are correct before any client connects.
        com.customblocks.arabic.ArabicLabels.set(CustomBlocksConfig.arabicFormIni,
                CustomBlocksConfig.arabicFormMid, CustomBlocksConfig.arabicFormFin);
        // Group 13 / Pass 1: extract JAR-bundled fonts (arabtype + Rockwell) before any text render.
        com.customblocks.arabic.FontAssets.extractAll();
        // Ensure a raise-only pool floor: an older config.json capped at 800 is lifted to
        // REQUIRED_MAX_SLOTS once, on disk (a hand-set HIGHER value — e.g. 2000 — is kept).
        // Registration hasn't run yet, so the new size takes effect THIS boot; no extra restart.
        if (CustomBlocksConfig.maxSlots < com.customblocks.core.SlotPools.REQUIRED_MAX_SLOTS) {
            LOGGER.info("[CustomBlocks] Raising maxSlots {} -> {} (pool floor).",
                    CustomBlocksConfig.maxSlots, com.customblocks.core.SlotPools.REQUIRED_MAX_SLOTS);
            CustomBlocksConfig.maxSlots = com.customblocks.core.SlotPools.REQUIRED_MAX_SLOTS;
            CustomBlocksConfigStore.save();
        }
        int maxSlots = CustomBlocksConfig.maxSlots;

        // Register server→client/client→server payloads + their receivers (Phase 10/11 onward).
        // Moved into PayloadRegistrar to keep this file under the §9.3 500-line cap.
        PayloadRegistrar.registerAll();

        SlotManager.registerAll(maxSlots);
        SlotManager.loadAll();
        // G06-2/G06-3 (improved Opt-2, 2026-06-26): migrate any old FreedSlots reservations into the new
        // permanent DeletedSlots set so previously-deleted indices are never reused and their placements
        // get swept to (Removed) too. Idempotent (a no-op once freed_slots.json is empty/gone).
        java.util.List<Integer> freedLegacy = com.customblocks.core.FreedSlots.all();
        if (!freedLegacy.isEmpty()) {
            com.customblocks.core.DeletedSlots.addAll(freedLegacy);
            LOGGER.info("[CustomBlocks] Migrated {} legacy freed slot(s) into the permanent deleted set.", freedLegacy.size());
        }
        // Group 26 / FIX A: clean legacy display names (underscores -> spaces) once on boot.
        // Idempotent — a no-op once every name is already clean.
        int cleaned = SlotManager.migrateDisplayNames();
        if (cleaned > 0) LOGGER.info("[CustomBlocks] Cleaned {} legacy display name(s) (underscores -> spaces).", cleaned);
        // G06-5: de-bracket colour-variant names ("Vart (Green)" -> "Vart Green"), turn "#hex" names
        // into their nearest preset ("A4 Black (#FF1493)" -> "A4 Magenta"), and de-hex legacy ids
        // ("a4_hex_ff1493" -> "a4_magenta"). Idempotent; Arabic/non-colour names are left alone.
        com.customblocks.core.ColorVariantNameMigration.Result cv = com.customblocks.core.ColorVariantNameMigration.migrate();
        if (cv.any()) LOGGER.info("[CustomBlocks] G06-5: cleaned {} colour-variant name(s) + {} legacy hex id(s).", cv.names(), cv.ids());
        // Group 13 / G13-25 CP5: retire ALL old static Arabic art blocks (letters AND numbers —
        // the real Arabic slot blocks below are the only letter/number system now), reclaim their
        // slots, and air-clean any placed copies as chunks load. Idempotent -- a no-op once gone.
        com.customblocks.arabic.ArabicLetterRetirement.init();
        // Group 13 / G13-25 CP1+CP4: ensure the 656 base Arabic letter/number slots (all four
        // colours, exact config triangle hexes) exist as REAL slot blocks (data-only ArabicMeta
        // flag, normal pool allocation, idempotent by stable id). First boot creates + bakes;
        // later boots skip; a config-hex change re-bakes that coloured set once. No pack rebuild
        // here — the SERVER_STARTED handler below builds the pack once with them included.
        com.customblocks.arabic.ArabicSlotBootstrap.ensure();
        ToolItems.registerAll();
        // Group 14 / Phase 1b — attach a BlockEntity to every slot block so the own-texture world
        // renderer can draw placed animated blocks off-atlas (crisp, no mipmap muffle). Must run
        // AFTER SlotManager.registerAll() (the blocks must exist to build the BlockEntityType).
        com.customblocks.block.AnimSlotRegistry.register();
        // G06-2/G06-3 (improved Opt-2): the shared (Removed) placeholder a deleted block's placements
        // become, + the chunk sweeper that swaps them live (no rejoin) and on chunk load.
        com.customblocks.block.RemovedBlock.register();
        com.customblocks.block.DeletedPlacementSweeper.init();
        // G06-14 (Unified Recycle-Bin deletion) slice 1 — the identity-carrying Deleted marker block
        // (BlockEntity holds the deleted block's id + name). Replaces the (Removed) system above, built
        // in slices; the old system is removed in slice 5.
        com.customblocks.block.DeletedMarkerRegistry.register();
        // Group 31 (BuzzerGame) Phase 1 — the buzzer block + BlockEntity + BlockItem.
        com.customblocks.buzzergame.BuzzerGameRegistry.register();
        // Group 30 · G30-8b — the Guess-mode Showcase display block + BlockEntity (op-spawned, no item).
        com.customblocks.block.GuessShowcaseRegistry.register();
        // Group 32 Phase A — the Explosive Tomato: the mod's FIRST custom entity type, its item, and the
        // dispenser behaviour. Its renderer is registered in CustomBlocksClient — an entity type with no
        // renderer crashes the client the moment one spawns, so the two must stay in step.
        com.customblocks.tomato.TomatoRegistry.register();
        // Group 32 §B — the 3 splat sounds (random pick per blast) + the in-memory crater-restore sweep.
        com.customblocks.tomato.TomatoSounds.register();
        com.customblocks.tomato.TomatoCraterManager.init();
        // Group 30 · G30 §R (R2) — the "?"-textured break/dig debris particle (shown to a flagged holder
        // instead of the real block particles, which would leak the answer). Client factory in CustomBlocksClient.
        com.customblocks.particle.MysteryParticles.register();
        // Group 30 · G30-8b (§S: S8) — a Showcase is removed ONLY by an op shift-right-click or
        // /cb guess showcase delete. Cancel any left-click / mining break (op or not) so it can't be
        // destroyed by accident. (world.removeBlock in onUse + the delete command bypass this event.)
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register(
                (world, player, pos, state, blockEntity) ->
                        !(state.getBlock() instanceof com.customblocks.block.GuessShowcaseBlock));
        registerCreativeTab();
        registerToolsTab();
        registerArabicJoinTab();
        com.customblocks.buzzergame.BuzzerGameRegistry.registerTab(); // Group 31 — dedicated BuzzerGame tab
        CommandRegistrar.register();

        // Resource-pack HTTP server: start with the world, rebuild the pack, stop on shutdown.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ResourcePackServer.setServer(server);
            ResourcePackServer.start();
            ResourcePackServer.updatePack();
            AutoBackup.start(server); // Group 09 / Slice 3 — timed auto-backups + prune
        });
        // Stop the auto-backup timer first, THEN flush slots, so no auto-backup fires mid-shutdown.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { AutoBackup.stop(); SlotManager.saveAll(); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ResourcePackServer.stop());

        // On player join: tell the client our silent-pack preference FIRST (so the pack
        // push below is auto-accepted with no dialog), then send pack, HUD index, welcome.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    handler.player, new SilentPackPayload(CustomBlocksConfig.silentPack));
            // Group 14 Phase 1c Step 2b: tell the client whether off-atlas blocks use a black or transparent bg.
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.player,
                    new com.customblocks.network.payloads.TransparentBgPayload(CustomBlocksConfig.transparentBackground));
            // Group 06 / M3 hex (G06-C): push the live variant hexes so the Square/Triangle tool names
            // (rendered client-side) show THIS server's colours, not the client's own defaults.
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.player,
                    new com.customblocks.network.payloads.ColorHexSyncPayload(
                            CustomBlocksConfig.triangleRedHex, CustomBlocksConfig.triangleYellowHex,
                            CustomBlocksConfig.triangleGreenHex, CustomBlocksConfig.triangleBlackHex));
            // Group 13 / O6: push the live Arabic form labels so join-block names match this server.
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.player,
                    new ArabicLabelsPayload(CustomBlocksConfig.arabicFormIni,
                            CustomBlocksConfig.arabicFormMid, CustomBlocksConfig.arabicFormFin));
            ResourcePackServer.sendToPlayer(handler.player);
            // Group 20 §K — tell the joining client THIS server's version + where to grab the jar, so an
            // older client can auto-update (or, when autoUpdateEnabled is off, just warn). Empty download
            // fields when the server has no packaged jar (dev run) → client shows a toast only.
            {
                String dl = com.customblocks.update.ServerJarInfo.available()
                        ? ResourcePackServer.getDownloadUrl() : "";
                String sha = com.customblocks.update.ServerJarInfo.available()
                        ? com.customblocks.update.ServerJarInfo.sha256() : "";
                String ver = com.customblocks.update.ServerJarInfo.version();
                if (ver != null) {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.player,
                            new com.customblocks.network.payloads.VersionHandshakePayload(
                                    ver, dl, sha, CustomBlocksConfig.autoUpdateEnabled));
                }
            }
            // Dedicated server + modded client: stream the pack files (no-op on integrated host /
            // for vanilla clients — beginSync self-gates on isDedicated + canSend). Group 05 remote fix.
            com.customblocks.network.packsync.PackSyncService.beginSync(handler.player);
            HudSync.sendTo(handler.player);
            // Group 30 — tell the joining client the current guess-mode set (their own blinding + everyone's pose).
            com.customblocks.network.GuessSync.sendTo(handler.player);
            OnboardingManager.onPlayerJoin(handler.player);
            com.customblocks.core.WidgetSync.push(handler.player);   // G03: seed the HUD widgets
        });
        // Drop the player's pack-send history so a later rejoin gets exactly one prompt again.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ResourcePackServer.forget(handler.player.getUuid());
            com.customblocks.network.packsync.PackSyncService.forget(handler.player.getUuid());
        });
        // Group 05 remote fix: drive the throttled pack-file stream once per server tick.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(
                com.customblocks.network.packsync.PackSyncService::tick);

        LOGGER.info("[CustomBlocks] Registered {} slot blocks (slot_0 to slot_{}).",
                maxSlots, maxSlots - 1);
        LOGGER.info("[CustomBlocks] Loaded {} saved custom block(s).", SlotManager.usedSlots());
        LOGGER.info("[CustomBlocks] Hello World — mod loaded successfully.");
    }

    /**
     * Group 14 — ImageIO discovers its reader plugins (incl. the bundled TwelveMonkeys WebP reader)
     * by scanning the THREAD CONTEXT classloader. Under Fabric/Knot that isn't the mod's classloader,
     * so the JiJ'd WebP SPI wouldn't be found at first use. We scan once with the mod classloader set,
     * then restore the previous one. The registered SPIs persist in the global IIORegistry.
     */
    private static void registerImageIOPlugins() {
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(CustomBlocksMod.class.getClassLoader());
            javax.imageio.ImageIO.scanForPlugins();
            LOGGER.info("[CustomBlocks] ImageIO readers available: {}",
                    java.util.Arrays.toString(javax.imageio.ImageIO.getReaderFormatNames()));
        } catch (Throwable t) {
            LOGGER.warn("[CustomBlocks] ImageIO plugin scan failed (WebP may be unavailable).", t);
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }

    /** Register the creative tab, listing only the blocks that have actually been created. */
    private static void registerCreativeTab() {
        Registry.register(Registries.ITEM_GROUP, CUSTOM_BLOCKS_TAB,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.customblocks.blocks"))
                        .icon(() -> new ItemStack(Items.BOOKSHELF))
                        .entries((displayContext, entries) -> {
                            for (SlotData d : SlotManager.assignedSlots()) {
                                SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
                                if (item != null) entries.add(item);
                            }
                        })
                        .build());
    }

    /**
     * Register the tools tab listing the mod's hand tools. Registered AFTER the blocks tab
     * (see onInitialize order) so it appears right after the CustomBlocks blocks tab — custom
     * creative groups render in registration order.
     */
    private static void registerToolsTab() {
        Registry.register(Registries.ITEM_GROUP, CUSTOM_TOOLS_TAB,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.customblocks.tools"))
                        .icon(() -> new ItemStack(ToolItems.OMNI_TOOL))
                        .entries((displayContext, entries) -> {
                            // Group 06: the unified Omni-Tool replaces the separate Brush + Chisel.
                            entries.add(ToolItems.OMNI_TOOL);
                            entries.add(ToolItems.RAINBOW_RECTANGLE);
                            entries.add(ToolItems.DELETER);
                            // Group 32 Phase A — the Explosive Tomato (not craftable; command + this tab only).
                            entries.add(com.customblocks.tomato.TomatoRegistry.ITEM);
                            // The eight colour/shape tools (Squares + Triangles).
                            for (var shape : ToolItems.SHAPES) entries.add(shape);
                        })
                        .build());
    }

    /**
     * Group 13 / G13-25: the dedicated Arabic tab. Lists every REAL Arabic slot block (SlotData
     * carries ArabicMeta) — letters in all four contextual forms plus numbers, every pre-baked
     * colour. Same items as the main blocks tab / creative search, gathered in one place. Icon =
     * the isolated black Jeem slot (falls back to a bookshelf if it hasn't been created yet).
     */
    private static void registerArabicJoinTab() {
        Registry.register(Registries.ITEM_GROUP, ARABIC_JOIN_TAB,
                FabricItemGroup.builder()
                        .displayName(Text.literal("Arabic Letters"))
                        .icon(() -> {
                            SlotData jeem = SlotManager.getById("arabic_jeem_iso");
                            SlotBlock.SlotItem item = (jeem == null) ? null : SlotManager.itemAt(jeem.index());
                            return item != null ? new ItemStack(item) : new ItemStack(Items.BOOKSHELF);
                        })
                        .entries((displayContext, entries) -> {
                            for (SlotData d : SlotManager.assignedSlots()) {
                                if (!d.isArabic()) continue;
                                SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
                                if (item != null) entries.add(item);
                            }
                        })
                        .build());
    }
}
