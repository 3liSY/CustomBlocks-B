/**
 * ConfigMenu.java — full server configuration dashboard (finale fix; replaces the old
 * Yes/No CONFIG_WARNING confirm and mirrors the old buildConfigGui panel). Every live
 * config field is shown as a labelled item. Editable entries glow and act on click; the
 * rest are read-only (changed in config.json). History Mode toggles via the existing,
 * tested /cb config undomode command so no mutation logic is duplicated here.
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureNameMirror;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ConfigMenu {

    private ConfigMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("Server Configuration", 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 45; i < 54; i++) m.set(i, Icons.accent());

        m.set(4, Icons.glint(Items.LEVER, "§6§lServer Configuration",
                "§7Live settings for CustomBlocks.",
                "§aGlowing §7entries can be changed here;",
                "§7others are edited in §fconfig.json§7."));

        // ── Capacity & rendering (read-only) ──────────────────────────────
        m.set(19, Icons.of(Items.CHEST, "§e§lBlock Capacity §f= §a" + CustomBlocksConfig.maxSlots,
                "§7Slots used: §f" + SlotManager.usedSlots() + " §7/ §f" + CustomBlocksConfig.maxSlots,
                "§8config.json → maxSlots (restart required)"));
        m.set(20, Icons.glint(Items.PAINTING, "§a§lTexture Quality §f→ §e" + CustomBlocksConfig.textureSize + "px",
                "§7Resolution used for new textures.",
                "§7Higher = sharper, but a bigger pack.",
                "§aClick §7→ choose a size"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.TEXTURE_SIZE)));
        m.set(21, Icons.of(Items.BEACON, "§e§lTexture Server §f= §a"
                        + CustomBlocksConfig.httpHost + ":" + CustomBlocksConfig.httpPort,
                "§7Address the resource pack is served from",
                "§8config.json → httpHost / httpPort"));
        m.set(22, Icons.of(Items.CLOCK, "§e§lHistory Depth §f= §a" + CustomBlocksConfig.maxUndoDepth,
                "§7Undo / redo steps kept per stack",
                "§8config.json → maxUndoDepth"));

        // ── History Mode (editable: cycles via /cb config undomode) ───────
        boolean perPlayer = "per_player".equals(CustomBlocksConfig.undoMode);
        m.set(23, Icons.glint(Items.REPEATER, "§a§lHistory Mode §f→ §e"
                        + (perPlayer ? "per-player" : "server-wide"),
                "§7Whether undo history is shared across",
                "§7players or isolated per player.",
                "§aClick §7→ switch mode §8(clears history)"),
                (p, b, a) -> GuiRouter.runAndReopen(p, "config undomode", MenuKey.of(Dest.CONFIG)));

        // ── Did-you-mean (editable: cycles via /cb config didyoumean cycle) ──
        m.set(24, Icons.glint(Items.NAME_TAG, "§a§lTypo Correction §f→ §e" + CustomBlocksConfig.didYouMean,
                "§7Suggests the right command when a",
                "§7/cb subcommand is mistyped.",
                "§7smart §8= only confident hits §7· always §8= closest match §7· off",
                "§aClick §7→ cycle mode"),
                (p, b, a) -> GuiRouter.runAndReopen(p, "config didyoumean cycle", MenuKey.of(Dest.CONFIG)));

        // ── Silent pack (editable: toggles via /cb config silentpack) ──────
        m.set(25, Icons.glint(CustomBlocksConfig.silentPack ? Items.LIME_DYE : Items.GRAY_DYE,
                "§a§lSilent Pack §f= " + (CustomBlocksConfig.silentPack ? "§aON" : "§cOFF"),
                "§7Apply texture updates with no",
                "§7\"download resource pack?\" dialog.",
                "§aClick §7→ toggle"),
                (p, b, a) -> GuiRouter.runAndReopen(p, "config silentpack toggle", MenuKey.of(Dest.CONFIG)));

        // ── Background removal (editable: cycles via /cb config background) ──
        m.set(26, Icons.glint(Items.BLACK_DYE, "§a§lBackground Removal §f→ §e"
                        + BackgroundRemover.displayName(CustomBlocksConfig.backgroundMode),
                "§7Strip an image's background to black",
                "§7when a block is created or retextured.",
                "§7Strength: §f" + CustomBlocksConfig.backgroundTolerance + "§7/100 §8(/cb tolerance)",
                "§7Off §8· §7Removal Only §8· §7+ Closed Areas",
                "§aClick §7→ cycle mode"),
                (p, b, a) -> GuiRouter.runAndReopen(p,
                        "config background " + BackgroundRemover.commandArg(
                                BackgroundRemover.next(CustomBlocksConfig.backgroundMode)),
                        MenuKey.of(Dest.CONFIG)));

        // ── Variant colours (editable: sub-GUI, one dye per magic tool) ──────
        m.set(32, Icons.glint(Items.RED_DYE, "§a§lVariant Colours §f→ §c" + CustomBlocksConfig.triangleRedHex
                        + " §e" + CustomBlocksConfig.triangleYellowHex
                        + " §a" + CustomBlocksConfig.triangleGreenHex
                        + " §8" + CustomBlocksConfig.triangleBlackHex,
                "§7The hex each Square/Triangle tool uses.",
                "§7Edit them per-colour in an anvil.",
                "§aClick §7→ open the colour editor"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.HEX_COLORS)));

        // ── Auto-backup (editable: left = interval, right = keep count) ──────
        int autoIv = CustomBlocksConfig.autoBackupInterval;
        m.set(33, Icons.glint(Items.BARREL, "§a§lAuto-Backup §f→ "
                        + (autoIv <= 0 ? "§cOFF" : "§aevery " + autoIv + " min"),
                "§7The server saves a backup on a timer and",
                "§7prunes old auto-backups automatically.",
                "§7Keep newest: §f" + CustomBlocksConfig.autoBackupKeepCount + " §8auto-backup(s)",
                "§aLeft-click §7→ interval §8(off→5→15→30→60→120→360)",
                "§aRight-click §7→ how many to keep"),
                (p, b, a) -> {
                    if (b == 1) GuiRouter.runAndReopen(p, "config autobackup keep", MenuKey.of(Dest.CONFIG));
                    else GuiRouter.runAndReopen(p, "config autobackup interval", MenuKey.of(Dest.CONFIG));
                });

        // ── Named-texture mirror (editable: left = toggle, right = rebuild) ──
        boolean mirrorOn = CustomBlocksConfig.mirrorNamedTextures;
        m.set(34, Icons.glint(mirrorOn ? Items.FILLED_MAP : Items.MAP,
                        "§a§lNamed Textures §f= " + (mirrorOn ? "§aON" : "§cOFF"),
                        "§7A readable copy of your texture folder,",
                        "§7named by block §8(e.g. §fNeptune Red.png§8).",
                        "§7Files now: §f" + TextureNameMirror.fileCount(),
                        "§8Write-only — never touches a real block.",
                        "§aLeft-click §7→ turn " + (mirrorOn ? "§coff" : "§aon"),
                        "§aRight-click §7→ rebuild from scratch"),
                (p, b, a) -> {
                    if (b == 1) GuiRouter.runAndReopen(p, "config mirrornames rebuild", MenuKey.of(Dest.CONFIG));
                    else GuiRouter.runAndReopen(p, "config mirrornames "
                            + (CustomBlocksConfig.mirrorNamedTextures ? "off" : "on"), MenuKey.of(Dest.CONFIG));
                });

        // ── Feature flags (read-only) ─────────────────────────────────────
        m.set(28, Icons.of(CustomBlocksConfig.hudEnabled ? Items.GLOWSTONE : Items.GLOWSTONE_DUST,
                "§e§lBlock HUD §f= " + (CustomBlocksConfig.hudEnabled ? "§aON" : "§cOFF"),
                "§7Info overlay when looking at a block",
                "§8config.json → hudEnabled"));
        m.set(29, Icons.of(CustomBlocksConfig.aiTextureEnabled ? Items.AMETHYST_SHARD : Items.GRAY_DYE,
                "§e§lAI Textures §f= " + (CustomBlocksConfig.aiTextureEnabled ? "§aON" : "§cOFF"),
                "§7API key: §f" + (CustomBlocksConfig.aiApiKey.isEmpty() ? "§8(not set)" : "§a••••••••"),
                "§8config.json → aiTextureEnabled / aiApiKey"));
        m.set(30, Icons.of(CustomBlocksConfig.vaultEndpoint.isEmpty() ? Items.ENDER_PEARL : Items.ENDER_EYE,
                "§e§lCloud Vault §f= " + (CustomBlocksConfig.vaultEndpoint.isEmpty() ? "§8(not set)" : "§aconfigured"),
                "§7Endpoint: §f" + (CustomBlocksConfig.vaultEndpoint.isEmpty() ? "§8—" : CustomBlocksConfig.vaultEndpoint),
                "§8config.json → vaultEndpoint"));
        m.set(31, Icons.of(CustomBlocksConfig.discordWebhookUrl.isEmpty() ? Items.GRAY_DYE : Items.PAPER,
                "§e§lDiscord Webhook §f= " + (CustomBlocksConfig.discordWebhookUrl.isEmpty() ? "§8(not set)" : "§aconfigured"),
                "§7Block-event notifications",
                "§8config.json → discordWebhookUrl"));

        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
