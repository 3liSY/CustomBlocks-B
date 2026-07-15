/**
 * GuessPoseStore.java — Group 30 (Guess Mode) · G30-4 pose editor, slice 1. SERVER source of truth.
 *
 * Responsibility: the ONE shared "guess-mode pose" — the centered two-handed hold every flagged player is
 * shown in (seen by everyone). Slice 1 covers the two ARMS only: an independent pitch/yaw/roll per arm
 * (owner-facing "up/down · in/out · twist"), stored in radians. Global (not per-player), persisted atomically
 * to config/customblocks/guesspose.json, and broadcast to all clients by {@link com.customblocks.network.GuessSync}
 * so a change takes effect live (NO-REJOIN). Defaults reproduce the previous hardcoded pose from
 * BipedArmPoseMixin, so nothing moves until the owner tunes it. Later slices (1b/1c) add block-placement +
 * animation + flair knobs here (§3 pose editor: G30-4).
 *
 * Depends on: Gson.
 * Called by:  the GuessPosePayload receiver (mutations from the settings screen), GuessSync (read for broadcast).
 */
package com.customblocks.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class GuessPoseStore {

    private static final String FILE = "config/customblocks/guesspose.json";

    // Defaults = the previous hardcoded centered two-handed pose (BipedArmPoseMixin, radians): arms straight
    // forward + horizontal at chest (pitch -90°), each hand toed IN to meet at centre (yaw ∓0.5 rad), no twist.
    public static final float DEF_ARM_PITCH = -1.5708f; // -90°
    public static final float DEF_RIGHT_YAW = -0.50f;   // right hand toes in toward centre
    public static final float DEF_LEFT_YAW  =  0.50f;   // left hand toes in (mirror of the right)

    // Live values (radians). One shared pose applied to every flagged player.
    private static float rightPitch = DEF_ARM_PITCH, rightYaw = DEF_RIGHT_YAW, rightRoll = 0f;
    private static float leftPitch  = DEF_ARM_PITCH, leftYaw  = DEF_LEFT_YAW,  leftRoll  = 0f;

    static { load(); }

    private GuessPoseStore() {} // static-only

    /** Owner-facing arm axes: up/down (pitch), in/out (yaw), twist (roll). */
    public enum Axis { PITCH, YAW, ROLL }

    public static synchronized float rightPitch() { return rightPitch; }
    public static synchronized float rightYaw()   { return rightYaw;   }
    public static synchronized float rightRoll()  { return rightRoll;  }
    public static synchronized float leftPitch()  { return leftPitch;  }
    public static synchronized float leftYaw()    { return leftYaw;    }
    public static synchronized float leftRoll()   { return leftRoll;   }

    /** Set one arm's one axis (radians) and persist. {@code rightArm=false} → left arm. */
    public static synchronized void set(boolean rightArm, Axis axis, float radians) {
        if (rightArm) {
            switch (axis) {
                case PITCH -> rightPitch = radians;
                case YAW   -> rightYaw   = radians;
                case ROLL  -> rightRoll  = radians;
            }
        } else {
            switch (axis) {
                case PITCH -> leftPitch = radians;
                case YAW   -> leftYaw   = radians;
                case ROLL  -> leftRoll  = radians;
            }
        }
        save();
    }

    /** Set ALL six arm angles (radians) at once and persist in a single write. Slider order: right p/y/r, left p/y/r.
     *  Used by the Guess Settings screen (via GuessPosePayload) so one slider release / preset / reset is one save. */
    public static synchronized void setAll(float rp, float ry, float rr, float lp, float ly, float lr) {
        rightPitch = rp; rightYaw = ry; rightRoll = rr;
        leftPitch  = lp; leftYaw  = ly; leftRoll  = lr;
        save();
    }

    /** Reset every arm knob to the default centered pose and persist. */
    public static synchronized void reset() {
        rightPitch = DEF_ARM_PITCH; rightYaw = DEF_RIGHT_YAW; rightRoll = 0f;
        leftPitch  = DEF_ARM_PITCH; leftYaw  = DEF_LEFT_YAW;  leftRoll  = 0f;
        save();
    }

    // ── Presets (G30-4) ───────────────────────────────────────────────────────
    // A few ready-made arm poses the settings screen's preset buttons jump to. Each is six
    // DEGREES [rUpDown, rInOut, rTwist, lUpDown, lInOut, lTwist]. Shared by the command (applies them) and the
    // screen (sets its sliders), so the two never drift.

    public static String[] presetNames() { return new String[]{"cradle", "present", "low"}; }

    /** The six arm angles (degrees) for a named preset, or null if the name is unknown. */
    public static float[] presetDegrees(String name) {
        return switch (name == null ? "" : name.toLowerCase()) {
            case "cradle"  -> new float[]{-90f, -28.6f, 0f, -90f,  28.6f, 0f}; // default centered two-handed hold
            case "present" -> new float[]{-115f, -25f, 0f, -115f,  25f,  0f}; // raised, offering the block up
            case "low"     -> new float[]{-70f, -30f, 0f, -70f,   30f,  0f}; // lowered, cradled sneaky
            default -> null;
        };
    }

    /** Apply a named preset and persist. Returns false (no change) if the name is unknown. */
    public static synchronized boolean applyPreset(String name) {
        float[] d = presetDegrees(name);
        if (d == null) return false;
        rightPitch = (float) Math.toRadians(d[0]); rightYaw = (float) Math.toRadians(d[1]); rightRoll = (float) Math.toRadians(d[2]);
        leftPitch  = (float) Math.toRadians(d[3]); leftYaw  = (float) Math.toRadians(d[4]); leftRoll  = (float) Math.toRadians(d[5]);
        save();
        return true;
    }

    /** Serialize the pose for the client feed (GuessSync): a compact radians object. */
    public static synchronized JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("rp", rightPitch); o.addProperty("ry", rightYaw); o.addProperty("rr", rightRoll);
        o.addProperty("lp", leftPitch);  o.addProperty("ly", leftYaw);  o.addProperty("lr", leftRoll);
        return o;
    }

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
            rightPitch = f(o, "rp", DEF_ARM_PITCH); rightYaw = f(o, "ry", DEF_RIGHT_YAW); rightRoll = f(o, "rr", 0f);
            leftPitch  = f(o, "lp", DEF_ARM_PITCH); leftYaw  = f(o, "ly", DEF_LEFT_YAW);  leftRoll  = f(o, "lr", 0f);
        } catch (Exception ignored) {}
    }

    private static float f(JsonObject o, String k, float def) {
        return (o.has(k) && o.get(k).isJsonPrimitive()) ? o.get(k).getAsFloat() : def;
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("guesspose.json.tmp");
            Files.writeString(tmp, toJson().toString(), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
