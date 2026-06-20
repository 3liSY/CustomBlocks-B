/**
 * CreationStudioBridge.java — Group 27 §G27.6 (Block Creation Studio) server-side glue.
 *
 * Bridges the studio screen to the existing block-creation + attribute rails, kept out of
 * CreationCommands so that handler stays under the 400-line gate (§9.3):
 *   - openStudio       — bare /cb create (no args) opens the studio for a player.
 *   - createFromStudio — the screen's "Create & Publish" packet lands here. It normalises the id,
 *                        parses the attrs string, then creates the block through the SAME
 *                        CreationCommands.doCreate rail the CLI uses and applies shape/glow/hardness/
 *                        sound/collision/category through the existing SlotManager setters. A solid
 *                        base colour (no URL) is baked into a flat texture here. Server-authoritative.
 *
 * Depends on: CreationCommands.doCreate, SlotManager + setters, TextureStore, ImageProcessor,
 *             BlockShapes, SlotBlock.SOUND_TYPES, ResourcePackServer, HudSync, Chat.
 * Called by:  CreationCommands.register (no-arg /cb create), CustomBlocksMod (CreateStudioPayload receiver).
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.block.BlockShapes;
import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import com.customblocks.core.AnimData;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.gui.GuiMode;
import com.customblocks.image.ImageDownloader;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.customblocks.network.payloads.StudioEditPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class CreationStudioBridge {

    private CreationStudioBridge() {} // static-only

    /** Bare /cb create (no args) — open the Block Creation Studio screen for a player. */
    public static int openStudio(ServerCommandSource src) {
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run /cb create as a player to open the Block Creation Studio, "
                    + "or use /cb create <id> [name] [url] from the console.");
            return 0;
        }
        ServerPlayNetworking.send(player, new OpenGuiPayload(GuiMode.CREATE_STUDIO.id, ""));
        return 1;
    }

    /**
     * /cb anim &lt;id&gt; — open the studio on an EXISTING block (edit mode, Animation tab). Sends the
     * block's current state so the screen loads it; the client reads the frame-strip back from the pack.
     * Group 14 Phase 2.
     */
    public static int openStudioEdit(ServerCommandSource src, String id) {
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run /cb anim " + id + " as a player to open the studio.");
            return 0;
        }
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (!d.isAnimated()) {
            Chat.info(src, "\"" + id + "\" isn't animated yet — opened the studio on the Texture tab. "
                    + "Paste a GIF/WebP and hit Load texture to animate it.");
        }
        ServerPlayNetworking.send(player, new StudioEditPayload(d.index(), d.customId(), d.displayName(), editAttrs(d)));
        return 1;
    }

    /** Build the "key=value;…" state string the studio loads in edit mode (incl. the animation numbers). */
    private static String editAttrs(SlotData d) {
        StringBuilder sb = new StringBuilder();
        sb.append("shape=").append(d.shape())
          .append(";glow=").append(d.glow())
          .append(";hardness=").append(d.hardness())
          .append(";sound=").append(d.soundType())
          .append(";passable=").append(d.noCollision() ? 1 : 0)
          .append(";color=none") // texture/colour isn't re-baked in edit mode, so the bg isn't pre-filled
          .append(";category=").append(d.category() == null ? "" : d.category());
        AnimData a = d.anim();
        if (a != null && a.isAnimated()) {
            sb.append(";anim=").append(a.frameCount()).append(',').append(a.uniformTicks()).append(',')
              .append(a.loopMode()).append(',').append(a.interpolate() ? 1 : 0).append(',')
              .append(a.trimStart()).append(',').append(a.trimEnd());
            List<Integer> ft = a.frameTimes();
            if (ft != null && !ft.isEmpty()) {
                sb.append(";animtimes=");
                for (int i = 0; i < ft.size(); i++) { if (i > 0) sb.append('_'); sb.append(ft.get(i)); }
            }
        }
        return sb.toString();
    }

    /**
     * The studio's "Save changes" (edit mode) — a FULL editor, per the "edit everything" design. It can:
     *   • RENAME the id ({@code origId} → {@code newId}) via the safe {@link SlotManager#reId} (placed blocks are
     *     the registry slot_N, so a rename never orphans them; reId also migrates locks/favs/notes/drafts);
     *   • change name + shape/glow/hardness/sound/collision/category through the existing setters;
     *   • SWAP the picture/GIF (a non-blank {@code url} → {@link StudioReskin} re-bakes the slot);
     *   • when NO new url: merge the animation knobs onto the EXISTING AnimData (per-frame source timing kept),
     *     and re-fill a static block's background if a colour was chosen.
     * One pack rebuild at the end (the url path rebuilds inside StudioReskin). Server-authoritative.
     */
    public static void saveFromStudio(ServerPlayerEntity player, String origId, String newId, String name,
                                      String url, String attrs) {
        ServerCommandSource src = player.getCommandSource();
        SlotData before = SlotManager.getById(origId);
        if (before == null) {
            Chat.error(src, "There's no block called \"" + origId + "\" to save. It may have been deleted.");
            return;
        }
        if (LockManager.isLocked(origId)) {
            Chat.error(src, "\"" + origId + "\" is locked. Use /cb unlock " + origId + " to edit it.");
            return;
        }
        Map<String, String> a = parse(attrs);

        // 1) Rename the id first (safe — placed blocks are slot_N, not the id). If it fails (taken/blank), keep origId.
        String id = origId;
        boolean renamed = false;
        String wantId = newId == null ? "" : newId.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
        if (!wantId.isEmpty() && !wantId.equals(origId)) {
            if (SlotManager.reId(origId, wantId) != null) { id = wantId; renamed = true; }
            else Chat.error(src, "Couldn't rename to \"" + wantId + "\" — that id may already be taken. Kept \"" + origId + "\".");
        }

        // 2) Name + the property setters.
        SlotData cur = SlotManager.getById(id);
        if (name != null && !name.isBlank() && cur != null && !name.trim().equals(cur.displayName())) SlotManager.rename(id, name.trim());

        String shape = a.getOrDefault("shape", before.shape());
        if (BlockShapes.isValid(shape)) SlotManager.setShape(id, shape);
        SlotManager.setGlow(id, Math.max(0, Math.min(15, parseInt(a.get("glow"), before.glow()))));
        String hardness = a.get("hardness");
        if (hardness != null && !hardness.isBlank()) {
            try { SlotManager.setHardness(id, Math.max(-1f, Math.min(100f, Float.parseFloat(hardness)))); }
            catch (NumberFormatException ignored) {}
        }
        String sound = a.get("sound");
        if (isKnownSound(sound)) SlotManager.setSoundType(id, sound);
        SlotManager.setNoCollision(id, "1".equals(a.get("passable")));
        String cat = a.getOrDefault("category", "");
        SlotManager.setCategory(id, cat.equalsIgnoreCase("none") ? "" : cat.toLowerCase(Locale.ROOT));

        // Background colour (used to fill behind the image, both for a re-skin and a static re-fill).
        Integer bgArgb = null;
        String color = a.getOrDefault("color", "none");
        if (!color.equals("none")) {
            try { bgArgb = 0xFF000000 | (Integer.parseInt(color) & 0xFFFFFF); } catch (NumberFormatException ignored) {}
        }

        // 3) New picture? Re-skin the existing slot (async; StudioReskin does its own rebuild + message).
        String cleanUrl = url == null ? "" : url.trim();
        if (!cleanUrl.isEmpty()) {
            if (ImageDownloader.isHttpUrl(cleanUrl)) {
                StudioReskin.apply(player, src, id, before.index(), cleanUrl, bgArgb);
                return; // settings already saved above; the re-skin rebuilds the pack with them
            }
            Chat.error(src, "That image link isn't a valid http/https url — settings were saved, the picture is unchanged.");
        }

        // 4) No new picture: merge the animation knobs onto the EXISTING anim (keeps frameCount + source timing).
        SlotData mid = SlotManager.getById(id);
        if (mid != null && mid.isAnimated()) {
            String csv = a.get("anim");
            if (csv != null && !csv.isBlank()) {
                String[] p = csv.split(",");
                if (p.length >= 6) {
                    AnimData ad = mid.anim();
                    int ut = parseInt(p[1], ad.uniformTicks());
                    boolean interp = "1".equals(p[3]);
                    int ts = parseInt(p[4], ad.trimStart());
                    int te = parseInt(p[5], ad.trimEnd());
                    AnimData next = ad.withLoopMode(p[2]).withInterpolate(interp).withTrim(ts, te);
                    next = ut > 0 ? next.withUniform(ut) : next.withMatchOriginal();
                    SlotManager.setAnim(id, next);
                }
            }
        } else if (bgArgb != null && mid != null) {
            // Static block, no new image, but a background colour was chosen → re-fill the existing texture.
            byte[] tex = TextureStore.load(mid.index());
            if (tex != null) {
                try { TextureStore.save(mid.index(), ImageProcessor.fillBackground(tex, bgArgb)); }
                catch (Exception ignored) {}
            }
        }

        // No-rename edits are undoable as one modify; a rename owns its own (reId records none) so skip undo then.
        SlotData after = SlotManager.getById(id);
        if (!renamed && after != null) UndoManager.recordModify(player.getUuid(), before, after, "studio-edit");
        ResourcePackServer.updatePack();
        Chat.success(src, "Saved changes to \"" + id + "\""
                + (CustomBlocksConfig.silentPack ? "." : " — accept the pack prompt to see them."));
        HudSync.sendTo(player);
    }

    /**
     * Create a block from the studio's "Create & Publish". Normalises the id, parses {@code attrs},
     * and routes: a solid base colour (no URL) bakes a flat texture here; otherwise it runs the shared
     * {@link CreationCommands#doCreate} rail (URL or untextured) with a post-create callback that sets
     * the shape + attributes. The server still has the final say on id-taken / slots-full / bad-URL.
     */
    public static void createFromStudio(ServerPlayerEntity player, String id, String name, String url, String attrs) {
        ServerCommandSource src = player.getCommandSource();
        String cleanId = id == null ? "" : id.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
        if (cleanId.isEmpty()) {
            Chat.error(src, "That block needs an id (letters, numbers or underscores) before it can be created.");
            return;
        }
        String cleanName = (name == null || name.isBlank()) ? null : name.trim();
        String cleanUrl  = (url  == null || url.isBlank())  ? null : url.trim();

        Map<String, String> a = parse(attrs);
        Consumer<SlotData> apply = applyAttrs(cleanId, a);

        // Background colour. With no URL it's the whole block (bake a flat texture). With a URL it fills
        // BEHIND the image's transparent pixels (passed down the shared create rail as bgArgb).
        String color = a.getOrDefault("color", "none");
        Integer bgArgb = null;
        if (!color.equals("none")) {
            try { bgArgb = 0xFF000000 | (Integer.parseInt(color) & 0xFFFFFF); } catch (NumberFormatException ignored) {}
        }
        if (cleanUrl == null && bgArgb != null) {
            createSolidColour(player, src, cleanId, cleanName, color, apply);
            return;
        }
        CreationCommands.doCreate(src, cleanId, cleanName, cleanUrl, apply, bgArgb);
    }

    /** Build the post-create callback that applies shape + attributes through the existing setters. */
    private static Consumer<SlotData> applyAttrs(String id, Map<String, String> a) {
        return d -> {
            String shape = a.getOrDefault("shape", "full");
            if (BlockShapes.isValid(shape) && !shape.equals("full")) SlotManager.setShape(id, shape);

            int glow = parseInt(a.get("glow"), 0);
            if (glow > 0) SlotManager.setGlow(id, Math.min(15, glow));

            String hardness = a.getOrDefault("hardness", "");
            if (!hardness.isBlank()) {
                try { SlotManager.setHardness(id, Math.max(-1f, Math.min(100f, Float.parseFloat(hardness)))); }
                catch (NumberFormatException ignored) {}
            }

            String sound = a.getOrDefault("sound", "");
            if (isKnownSound(sound)) SlotManager.setSoundType(id, sound);

            if ("1".equals(a.get("passable"))) SlotManager.setNoCollision(id, true);

            String cat = a.getOrDefault("category", "");
            if (!cat.isBlank() && !cat.equalsIgnoreCase("none")) SlotManager.setCategory(id, cat.toLowerCase(Locale.ROOT));
        };
    }

    /** Create an untextured block, paint it a flat colour, then apply the attrs + rebuild once. */
    private static void createSolidColour(ServerPlayerEntity player, ServerCommandSource src, String id,
                                          String name, String color, Consumer<SlotData> apply) {
        int argb;
        try { argb = Integer.parseInt(color); }
        catch (NumberFormatException e) { Chat.error(src, "That colour wasn't understood — nothing was created."); return; }

        byte[] png;
        try { png = solidPng(argb, CustomBlocksConfig.textureSize); }
        catch (Exception e) { Chat.error(src, "Couldn't build the colour texture, so the block was not created."); return; }

        SlotData d = SlotManager.create(id, name);
        if (d == null) {
            Chat.error(src, "Couldn't create \"" + id + "\" — that id is already taken or every slot is in use.");
            return;
        }
        TextureStore.save(d.index(), png);
        UndoManager.recordCreate(player.getUuid(), d);
        apply.accept(d);
        ResourcePackServer.updatePack();
        Chat.success(src, "Block \"" + id + "\"" + (name == null ? "" : " (\"" + name + "\")")
                + " created" + (CustomBlocksConfig.silentPack ? " — it'll show in a moment."
                : " — accept the resource pack prompt to see it."));
        HudSync.sendTo(player);
    }

    /** A flat solid-colour block texture (16×16 → run through the normal block-png pipeline at {@code size}). */
    private static byte[] solidPng(int argb, int size) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(argb, true));
        g.fillRect(0, 0, 16, 16);
        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return ImageProcessor.toBlockPng(bos.toByteArray(), size);
    }

    /** Parse a "key=value;key=value" attrs string into a map (empty values kept). */
    private static Map<String, String> parse(String attrs) {
        Map<String, String> m = new HashMap<>();
        if (attrs == null || attrs.isBlank()) return m;
        for (String pair : attrs.split(";")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            m.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
        }
        return m;
    }

    private static int parseInt(String s, int def) {
        try { return s == null ? def : Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private static boolean isKnownSound(String s) {
        if (s == null || s.isBlank()) return false;
        for (String t : SlotBlock.SOUND_TYPES) if (t.equals(s)) return true;
        return false;
    }
}
