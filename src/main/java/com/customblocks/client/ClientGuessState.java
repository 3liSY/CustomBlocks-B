/**
 * ClientGuessState.java — Group 30 (Guess Mode). v2 command redesign. CLIENT-SIDE ONLY.
 *
 * Client cache of the guess-mode set, populated from {@link com.customblocks.network.payloads.GuessModePayload}
 * on join + after any /cb guess change. Each guessing player carries an all-mode flag + a map of flagged slot
 * → optional look slot. One shared predicate decides everything: does player P have a given slot flagged?
 *   • {@link #disguisesSlot(UUID,int)} — for ANY player's model (the centered pose): P holds a block flagged
 *     for P → pose. Read while rendering any player, on every client.
 *   • {@link #localDisguisesSlot(int)} — for the LOCAL player only: blank the name (→ {@link #BLANK_NAME}) +
 *     swap in the disguise cube for a flagged block, on this client only, so watchers still see the real block.
 *   • {@link #localLookSlot(int)} — for the LOCAL player only: which slot's picture to draw as the disguise
 *     (v3 Phase 1). Resolves per-slot override → global default → -1 (client draws the bundled "?" for -1).
 *
 * The blank NAME ("???") is hardcoded. The disguise LOOK (v3 Phase 1) is a slot index sent per flagged slot
 * (with a global default); -1 means "use the bundled '?' cube", which stays hardcoded client-side.
 *
 * Thread safety: populated on the network thread, read on the render thread → volatile map swap
 * (same pattern as {@link ClientSlotCache}).
 *
 * Depends on: Gson, MinecraftClient (local player uuid).
 * Called by:  CustomBlocksClient (populate/clear), BipedArmPoseMixin, GuessDisguise, HudRenderer, SlotBlock seam.
 */
package com.customblocks.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class ClientGuessState {

    /** The hardcoded blank name a flagged holder sees instead of the real block name (v2 redesign). */
    public static final String BLANK_NAME = "???";

    /** "No look" sentinel — draw the bundled "?" cube (v3 Phase 1). */
    public static final int NO_LOOK = -1;

    /**
     * One guessing player's settings. {@code all} = blind every custom block; {@code looks} maps each flagged
     * slot → its per-round look slot ({@link #NO_LOOK} = use the default). A key's presence = that slot flagged.
     */
    public record Rec(boolean all, Map<Integer, Integer> looks) {
        boolean covers(int slot) { return all || looks.containsKey(slot); }
    }

    private static volatile Map<UUID, Rec> STATE = Collections.emptyMap();
    /** Global default disguise-look slot (v3 Phase 1), or {@link #NO_LOOK} when unset. */
    private static volatile int defaultLookSlot = NO_LOOK;

    // G30-4 slice 1 — the ONE shared centered guess pose (radians), synced from GuessPoseStore. Defaults
    // reproduce the previous hardcoded pose so the arms don't move until the owner tunes it. Read on the
    // render thread by BipedArmPoseMixin; written on the network thread → each is volatile.
    private static volatile float poseRightPitch = -1.5708f, poseRightYaw = -0.50f, poseRightRoll = 0f;
    private static volatile float poseLeftPitch  = -1.5708f, poseLeftYaw  =  0.50f, poseLeftRoll  = 0f;

    // G30-4 pose editor — LIVE PREVIEW override. While GuessSettingsScreen is open, the dummy (the local
    // player rendered in the GUI) shows the screen's UNSAVED working pose instead of the synced one, updated
    // on every slider move and cleared on close. BipedArmPoseMixin applies it ONLY to the preview entity
    // (the local player), at full strength (no transition), so the drag feels instant.
    private static volatile boolean posePreview = false;
    private static volatile UUID posePreviewUuid = null;
    private static volatile float pvRP, pvRY, pvRR, pvLP, pvLY, pvLR; // radians

    // G30-8b Showcase — the ONE shared display tuning (from GuessShowcaseStore), read by GuessShowcaseBER.
    // Defaults match GuessShowcaseStore so a fresh client looks right before the first sync.
    private static volatile float scInner = 30f, scSize = 1.0f;

    private ClientGuessState() {}

    /** Replace the state from the server JSON ({ "def": int, "p": { uuid: { all, looks:{slot:lookSlot} } } }). */
    public static void populate(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            defaultLookSlot = (root.has("def") && root.get("def").isJsonPrimitive()) ? root.get("def").getAsInt() : NO_LOOK;
            parsePose(root);                       // G30-4 slice 1 — the shared centered guess pose
            parseShowcase(root);                   // G30-8b — the shared showcase display tuning
            Map<UUID, Rec> map = new HashMap<>();
            JsonObject players = (root.has("p") && root.get("p").isJsonObject()) ? root.getAsJsonObject("p") : new JsonObject();
            for (var e : players.entrySet()) {
                UUID id;
                try { id = UUID.fromString(e.getKey()); } catch (IllegalArgumentException bad) { continue; }
                if (!e.getValue().isJsonObject()) continue;
                JsonObject o = e.getValue().getAsJsonObject();
                boolean all = o.has("all") && !o.get("all").isJsonNull() && o.get("all").getAsBoolean();
                Map<Integer, Integer> looks = new HashMap<>();
                if (o.has("looks") && o.get("looks").isJsonObject())
                    for (var le : o.getAsJsonObject("looks").entrySet())
                        try { looks.put(Integer.parseInt(le.getKey()),
                                le.getValue().isJsonPrimitive() ? le.getValue().getAsInt() : NO_LOOK); }
                        catch (NumberFormatException bad) {}
                map.put(id, new Rec(all, looks));
            }
            STATE = Collections.unmodifiableMap(map);
        } catch (Exception ignored) {
            STATE = Collections.emptyMap();
            defaultLookSlot = NO_LOOK;
            resetPose();
            resetShowcase();
        }
    }

    /** Read the shared showcase tuning from the feed's "showcase" object; missing/bad → defaults. */
    private static void parseShowcase(JsonObject root) {
        if (!root.has("showcase") || !root.get("showcase").isJsonObject()) { resetShowcase(); return; }
        JsonObject s = root.getAsJsonObject("showcase");
        scInner = pf(s, "in", 30f); scSize = pf(s, "sz", 1.0f);
    }

    private static void resetShowcase() {
        scInner = 30f; scSize = 1.0f;
    }

    // G30-8b — the shared showcase display tuning, read by GuessShowcaseBER.
    public static float showcaseInner()      { return scInner; }
    public static float showcaseSize()       { return scSize;  }

    /** Read the shared guess pose (radians) from the feed's "pose" object; missing/bad → defaults. */
    private static void parsePose(JsonObject root) {
        if (!root.has("pose") || !root.get("pose").isJsonObject()) { resetPose(); return; }
        JsonObject p = root.getAsJsonObject("pose");
        poseRightPitch = pf(p, "rp", -1.5708f); poseRightYaw = pf(p, "ry", -0.50f); poseRightRoll = pf(p, "rr", 0f);
        poseLeftPitch  = pf(p, "lp", -1.5708f); poseLeftYaw  = pf(p, "ly",  0.50f); poseLeftRoll  = pf(p, "lr", 0f);
    }

    private static float pf(JsonObject o, String k, float def) {
        return (o.has(k) && o.get(k).isJsonPrimitive()) ? o.get(k).getAsFloat() : def;
    }

    private static void resetPose() {
        poseRightPitch = -1.5708f; poseRightYaw = -0.50f; poseRightRoll = 0f;
        poseLeftPitch  = -1.5708f; poseLeftYaw  =  0.50f; poseLeftRoll  = 0f;
    }

    // G30-4 slice 1 — the shared guess pose, read by BipedArmPoseMixin (radians).
    public static float poseRightPitch() { return poseRightPitch; }
    public static float poseRightYaw()   { return poseRightYaw;   }
    public static float poseRightRoll()  { return poseRightRoll;  }
    public static float poseLeftPitch()  { return poseLeftPitch;  }
    public static float poseLeftYaw()    { return poseLeftYaw;    }
    public static float poseLeftRoll()   { return poseLeftRoll;   }

    // G30-4 pose editor — live preview override (radians). Set by GuessSettingsScreen while it's open.
    public static void setPosePreview(UUID uuid, float rp, float ry, float rr, float lp, float ly, float lr) {
        posePreviewUuid = uuid; pvRP = rp; pvRY = ry; pvRR = rr; pvLP = lp; pvLY = ly; pvLR = lr; posePreview = true;
    }
    public static void clearPosePreview() { posePreview = false; posePreviewUuid = null; }
    /** True while the pose editor is previewing THIS entity (the local player rendered as the dummy). */
    public static boolean isPosePreview(UUID u) { return posePreview && u != null && u.equals(posePreviewUuid); }
    public static float pvRightPitch() { return pvRP; }
    public static float pvRightYaw()   { return pvRY; }
    public static float pvRightRoll()  { return pvRR; }
    public static float pvLeftPitch()  { return pvLP; }
    public static float pvLeftYaw()    { return pvLY; }
    public static float pvLeftRoll()   { return pvLR; }

    /** Drop all guess state (on disconnect) so another server's session never bleeds through. */
    public static void clear() { STATE = Collections.emptyMap(); defaultLookSlot = NO_LOOK; resetPose(); resetShowcase(); clearPosePreview(); }

    /** Does this player have {@code slot} flagged (specific id or all-mode)? Any client can ask (body pose). */
    public static boolean disguisesSlot(UUID player, int slot) {
        if (player == null) return false;
        Rec r = STATE.get(player);
        return r != null && r.covers(slot);
    }

    /** The local player's own guess record, or null if not in guess mode. */
    private static Rec local() {
        if (STATE.isEmpty()) return null;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return null;
        return STATE.get(mc.player.getUuid());
    }

    /** Is the LOCAL player in guess mode (so their own flagged blocks are blinded)? */
    public static boolean localActive() { return local() != null; }

    /** Does the LOCAL player have {@code slot} flagged (so it's blinded on this client only)? */
    public static boolean localDisguisesSlot(int slot) {
        Rec r = local();
        return r != null && r.covers(slot);
    }

    /**
     * Which slot's picture to draw as the disguise for the LOCAL holder's flagged {@code slot} (v3 Phase 1):
     * per-slot override → global default → {@link #NO_LOOK} (caller draws the bundled "?"). Only meaningful
     * when {@link #localDisguisesSlot(int)} is true.
     */
    public static int localLookSlot(int slot) {
        Rec r = local();
        if (r != null) {
            Integer override = r.looks().get(slot);
            if (override != null && override >= 0) return override;
        }
        return defaultLookSlot;   // NO_LOOK when unset → caller falls back to the bundled "?"
    }
}
