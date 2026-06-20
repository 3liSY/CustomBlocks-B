/**
 * ArabicMaker.java — the Arabic word maker flow (Group 13 Area 2).
 *
 * Drives the all-in-GUI build with NO chat handoffs:
 *   GUI:     Name → ID → Text → [Single block → Color Studio] | [Place letter blocks]
 *   Command: /cb arabic word <id> <name> → Text → (same choice/colors)
 *
 * The per-step state lives in ArabicWordSession (a MenuKey only holds one arg). The actual block
 * creation reuses ArabicBlockRegistry.importWord, so there is no duplicated render/slot logic. The
 * "Render preview" makes a single reusable throwaway block (one slot per player), cleaned up when
 * the player Creates or returns to the hub.
 *
 * Depends on: ArabicBlockRegistry, ArabicLetterMap, SlotManager, HudSync, ResourcePackServer,
 *             AnvilPrompt, GuiRouter/Nav, ArabicWordSession, CustomBlocksConfig, Chat
 * Called by:  ArabicCommands, ArabicHubMenu, WordChoiceMenu, ColorStudioMenu
 */
package com.customblocks.arabic;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.block.ArabicLetterBlock;
import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.AnvilPrompt;
import com.customblocks.gui.chest.ArabicWordSession;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.gui.GuiMode;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.customblocks.network.payloads.OpenGuiPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;

public final class ArabicMaker {

    private ArabicMaker() {}

    // ── colour helpers (#RRGGBB ↔ opaque ARGB) ────────────────────────────────
    public static int argb(String hex) {
        String h = CustomBlocksConfig.normalizeHexColor(hex, "#000000").substring(1);
        return 0xFF000000 | Integer.parseInt(h, 16);
    }
    public static String hex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    // ── GUI maker: Name → ID → Text ───────────────────────────────────────────
    public static void startGuiMaker(ServerPlayerEntity player) {
        ArabicWordSession s = ArabicWordSession.start(player.getUuid());
        s.defaultsMode = false;
        s.bgArgb     = argb(CustomBlocksConfig.arabicDefaultBgHex);
        s.letterArgb = argb(CustomBlocksConfig.arabicDefaultLetterHex);
        askName(player, s);
    }

    private static void askName(ServerPlayerEntity player, ArabicWordSession s) {
        AnvilPrompt.open(player, "Block name", new ItemStack(Items.NAME_TAG), "",
                name -> { s.name = name.trim(); askId(player, s); },
                () -> cancel(player));
    }

    private static void askId(ServerPlayerEntity player, ArabicWordSession s) {
        AnvilPrompt.open(player, "Block id", new ItemStack(Items.PAPER), suggestId(s.name),
                raw -> {
                    String id = cleanId(raw, s.name);
                    if (SlotManager.hasId(id)) {
                        Chat.error(player.getCommandSource(), "Id '§e" + id + "§r' is taken — pick another.");
                        askId(player, s);
                        return;
                    }
                    s.id = id;
                    askText(player, s);
                },
                () -> cancel(player));
    }

    /** Command path: id + name already validated; jump straight to the text anvil. */
    public static void startFromCommand(ServerPlayerEntity player, String id, String name) {
        ArabicWordSession s = ArabicWordSession.start(player.getUuid());
        s.defaultsMode = false;
        s.id = id;
        s.name = name;
        s.bgArgb     = argb(CustomBlocksConfig.arabicDefaultBgHex);
        s.letterArgb = argb(CustomBlocksConfig.arabicDefaultLetterHex);
        askText(player, s);
    }

    private static void askText(ServerPlayerEntity player, ArabicWordSession s) {
        AnvilPrompt.open(player, "Type the text", new ItemStack(Items.WRITABLE_BOOK), "",
                text -> {
                    if (text == null || text.isBlank()) { cancel(player); return; }
                    s.text = text;
                    GuiRouter.navigate(player, MenuKey.of(Dest.ARABIC_CHOICE));
                },
                () -> cancel(player));
    }

    // ── choice outcomes ───────────────────────────────────────────────────────
    /** "Single texture block" → into the Color Studio (colours, preview, create). */
    public static void chooseSingle(ServerPlayerEntity player) {
        if (ArabicWordSession.get(player.getUuid()) == null) { GuiRouter.openFresh(player, MenuKey.of(Dest.ARABIC)); return; }
        GuiRouter.navigate(player, MenuKey.of(Dest.ARABIC_COLOR, "word"));
    }

    /** "Place letter blocks" → hand over one bundled letter block per character. */
    public static void chooseLetters(ServerPlayerEntity player) {
        ArabicWordSession s = ArabicWordSession.get(player.getUuid());
        if (s == null) { GuiRouter.openFresh(player, MenuKey.of(Dest.ARABIC)); return; }
        String text = s.text;
        player.closeHandledScreen();
        giveLetterBlocks(player, text);
        ArabicWordSession.clear(player.getUuid());
    }

    // ── create + preview ──────────────────────────────────────────────────────
    /** Create the real word block with the chosen colours, give it, return to the hub. */
    public static void finalizeWord(ServerPlayerEntity player) {
        ArabicWordSession s = ArabicWordSession.get(player.getUuid());
        if (s == null || s.text == null) { GuiRouter.openFresh(player, MenuKey.of(Dest.ARABIC)); return; }
        clearPreview(player);
        if (SlotManager.hasId(s.id)) { Chat.error(player.getCommandSource(), "Id '§e" + s.id + "§r' got taken — open the maker again."); return; }
        String err = ArabicBlockRegistry.importWord(s.text, s.id, s.name, s.letterArgb, s.bgArgb);
        if (err != null) {
            Chat.error(player.getCommandSource(), "Couldn't make the block: " + err);
            GuiRouter.render(player, MenuKey.of(Dest.ARABIC_COLOR, "word"));
            return;
        }
        giveById(player, s.id);
        HudSync.sendTo(player);
        Chat.success(player.getCommandSource(), "Made §e" + s.name + "§r §7(id " + s.id + ", "
                + hex(s.letterArgb) + " on " + hex(s.bgArgb) + "). Gave you one.");
        ArabicWordSession.clear(player.getUuid());
        GuiRouter.openFresh(player, MenuKey.of(Dest.ARABIC));
    }

    /**
     * Render the current colours to a single reusable throwaway slot (NO resource-pack rebuild) and
     * open the client-side live preview screen on it. Pack-free: the texture is served over the mod's
     * /tex/&lt;id&gt; endpoint straight from TextureStore, so there is no pack prompt and no placed block.
     * Back on that screen reopens the Color Studio (Nav.current); Create makes the real block.
     */
    public static void renderPreview(ServerPlayerEntity player) {
        ArabicWordSession s = ArabicWordSession.get(player.getUuid());
        if (s == null || s.text == null) return;
        String pid = previewId(player);
        String err = ArabicBlockRegistry.importWord(s.text, pid, "Preview", s.letterArgb, s.bgArgb, false);
        if (err != null) { Chat.error(player.getCommandSource(), "Preview failed: " + err); return; }
        s.previewActive = true;
        openPreviewScreen(player, pid, s);
    }

    /**
     * Re-render the throwaway preview slot after an on-screen colour change and refresh the open
     * preview screen. Pack-free, same as renderPreview — called by the ArabicPreviewPayload handler.
     */
    public static void updatePreviewColours(ServerPlayerEntity player, int letterArgb, int bgArgb) {
        ArabicWordSession s = ArabicWordSession.get(player.getUuid());
        if (s == null || s.text == null) return;
        s.letterArgb = letterArgb;
        s.bgArgb = bgArgb;
        String pid = previewId(player);
        String err = ArabicBlockRegistry.importWord(s.text, pid, "Preview", s.letterArgb, s.bgArgb, false);
        if (err != null) { Chat.error(player.getCommandSource(), "Preview failed: " + err); return; }
        s.previewActive = true;
        openPreviewScreen(player, pid, s);
    }

    /** Send the client the OpenGuiPayload that opens/refreshes the live preview screen. */
    private static void openPreviewScreen(ServerPlayerEntity player, String pid, ArabicWordSession s) {
        // data = "<id>|<texUrl>|<letterHex>|<bgHex>|<text>" (split client-side on '|').
        String data = pid + "|" + ResourcePackServer.getTexUrl(pid)
                + "|" + hex(s.letterArgb) + "|" + hex(s.bgArgb) + "|" + s.text;
        ServerPlayNetworking.send(player, new OpenGuiPayload(GuiMode.ARABIC_PREVIEW.id, data));
    }

    /**
     * Make the real word block from the live preview screen (its Create button): clears the throwaway
     * preview slot, then runs the normal create path (which gives the block + returns to the hub).
     */
    public static void finalizeFromPreview(ServerPlayerEntity player) {
        finalizeWord(player);
    }

    /** Remove the throwaway preview slot (called on Create, Back, and Cancel). Pack-free — the slot was
     *  never written into the pack, so no rebuild is needed. */
    public static void clearPreview(ServerPlayerEntity player) {
        ArabicWordSession s = ArabicWordSession.get(player.getUuid());
        if (s == null || !s.previewActive) return;
        SlotManager.removeSilently(previewId(player));
        s.previewActive = false;
    }

    private static String previewId(ServerPlayerEntity player) {
        String n = player.getGameProfile().getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        return "arabic_preview_" + (n.isEmpty() ? "p" : n);
    }

    private static void cancel(ServerPlayerEntity player) {
        clearPreview(player);
        ArabicWordSession.clear(player.getUuid());
        Chat.info(player.getCommandSource(), "Cancelled — no block made.");
    }

    // ── id helpers ────────────────────────────────────────────────────────────
    private static String suggestId(String name) {
        String base = (name == null ? "" : name).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return base.isEmpty() ? "myword" : base;
    }
    private static String cleanId(String raw, String name) {
        String id = (raw == null ? "" : raw).trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
        return id.isEmpty() ? suggestId(name) : id;
    }

    // ── letter blocks ─────────────────────────────────────────────────────────
    /**
     * Give one JOINABLE letter block (the auto-join {@link ArabicLetterBlock}, black) per Arabic letter
     * in {@code text}. Non-letters (spaces, numbers, punctuation) are skipped. Placed right-to-left these
     * auto-shape and connect into the word — they are the "(Join)" blocks, not the old static bundled art.
     */
    public static void giveLetterBlocks(ServerPlayerEntity player, String text) {
        int given = 0, skipped = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (ArabicLetterMap.byCodePoint(c).isEmpty()) { skipped++; continue; }
            player.getInventory().insertStack(ArabicLetterBlock.stackFor(c, 1));
            given++;
        }
        if (given == 0) Chat.error(player.getCommandSource(), "No Arabic letters in that text — nothing to give.");
        else Chat.success(player.getCommandSource(), "Gave you §e" + given + "§r joinable letter block(s)"
                + (skipped > 0 ? " §7(" + skipped + " skipped)" : "") + ". Place them right-to-left — they auto-join.");
    }

    private static boolean giveById(ServerPlayerEntity player, String id) {
        SlotData d = SlotManager.getById(id);
        if (d == null) return false;
        SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
        if (item == null) return false;
        player.getInventory().insertStack(new ItemStack(item));
        return true;
    }
}
