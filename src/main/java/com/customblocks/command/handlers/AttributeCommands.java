/**
 * AttributeCommands.java
 *
 * Responsibility: Per-block attribute setters — currently `setglow` (light emission).
 * Registered into the /cb tree by CommandRegistrar. All mutations go through SlotManager
 * (the single source of truth). Stays under 400 lines (§9.3).
 *
 * Phase 6 (attributes): setglow + sethardness. setsound / shape commands land here next,
 * keeping CreationCommands focused on the block lifecycle.
 *
 * NO-REJOIN (see HudSync's NO-REJOIN PRINCIPLE banner; owner 2026-06-27): block BEHAVIOUR is live
 * (glow via SlotLighting.applyToPlaced; hardness/sound/collision read live in SlotBlock) AND each
 * setter now broadcasts the slot cache so the look-HUD's DISPLAYED values (glow/hard/sound/pass/cat)
 * refresh live for every player too — no rejoin (fixed 2026-06-27).
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.block.SlotBlock;
import com.customblocks.block.SlotLighting;
import com.customblocks.command.Chat;
import com.customblocks.core.CategoryMembershipStore;
import com.customblocks.core.CategoryService;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.core.onboarding.FirstUseHints;
import com.customblocks.network.HudSync;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AttributeCommands {

    private AttributeCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb setglow <id> <level>. Accept ANY integer (not bounded 0-15) so an over-range
        // value gives a friendly "capped at 15" message instead of a red Brigadier rejection.
        root.then(CommandManager.literal("setglow")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("level", IntegerArgumentType.integer())
                                .executes(ctx -> setGlow(ctx,
                                        StringArgumentType.getString(ctx, "id"),
                                        IntegerArgumentType.getInteger(ctx, "level"))))));

        // /cb sethardness <id> <value>. Value is a word so it accepts both numbers AND
        // friendly keywords: "unbreakable", "instant", "stone" (parsed in setHardness).
        root.then(CommandManager.literal("sethardness")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("value", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    for (String s : new String[]{"unbreakable", "instant", "stone", "5", "20", "50"}) {
                                        b.suggest(s);
                                    }
                                    return b.buildFuture();
                                })
                                .executes(ctx -> setHardness(ctx,
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "value"))))));

        // /cb setsound <id> <type>  (stone, wood, metal, glass, sand, ...)
        root.then(CommandManager.literal("setsound")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("type", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    for (String s : SlotBlock.SOUND_TYPES) b.suggest(s);
                                    return b.buildFuture();
                                })
                                .executes(ctx -> setSound(ctx,
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "type"))))));

        // /cb setcollision <id> <solid|passable>  — passable = entities walk through it.
        root.then(CommandManager.literal("setcollision")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("mode", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    for (String s : new String[]{"solid", "passable"}) b.suggest(s);
                                    return b.buildFuture();
                                })
                                .executes(ctx -> setCollision(ctx,
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "mode"))))));

        // /cb setcategory <id> <name>  — organize blocks; "none" clears it. Used by /cb search.
        root.then(CommandManager.literal("setcategory")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("category", StringArgumentType.greedyString())
                                // TG11 A9: suggestions come from the real category records (shared with
                                // /cb category), so an empty category still completes and a typo can't
                                // quietly invent one.
                                .suggests((c, b) -> {
                                    // Prefix-filter "none" like every other suggestion here (TG11 A1):
                                    // b.suggest() adds unconditionally, so it used to keep offering
                                    // itself after unrelated text was already typed.
                                    if ("none".startsWith(b.getRemaining().toLowerCase(Locale.ROOT))) b.suggest("none");
                                    return CategorySuggest.categories(c, b);
                                })
                                .executes(ctx -> setCategory(ctx,
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "category"))))));
    }

    /** The acting player's UUID, or null for console/command-block (those aren't undoable). */
    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }

    /** Returns true (and prints an error) if the block is locked. Call before any mutation. */
    private static boolean locked(ServerCommandSource src, String id) {
        if (LockManager.isLocked(id)) {
            Chat.lockedError(src, id);
            return true;
        }
        return false;
    }

    private static int setGlow(CommandContext<ServerCommandSource> ctx, String id, int level) {
        ServerCommandSource src = ctx.getSource();
        int clamped = Math.max(0, Math.min(15, level)); // 15 is Minecraft's max block light
        SlotData before = SlotManager.getById(id);
        if (before == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        if (locked(src, id)) return 0;
        SlotData d = SlotManager.setGlow(id, clamped);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "glow");
        // New placements inherit it via getPlacementState; refresh already-placed ones nearby.
        SlotLighting.applyToPlaced(src.getServer(), d.index(), clamped);
        HudSync.broadcast(src.getServer()); // NO-REJOIN: HUD glow value updates live for all players
        String note = level > 15 ? " " + CbFmt.DIM + "(15 is the maximum)" : level < 0 ? " " + CbFmt.DIM + "(0 is the minimum)" : "";
        Chat.success(src, "Set \"" + id + "\" glow to " + clamped + "." + note);
        if (src.getEntity() instanceof ServerPlayerEntity p) FirstUseHints.onFirstSetglow(p); // Group 23: one-time hint
        return 1;
    }

    private static int setHardness(CommandContext<ServerCommandSource> ctx, String id, String raw) {
        ServerCommandSource src = ctx.getSource();
        Float parsed = parseHardness(raw);
        if (parsed == null) {
            Chat.error(src, "Hardness must be a number or: unbreakable, instant, stone");
            return 0;
        }
        float value = Math.max(-1.0f, Math.min(100.0f, parsed)); // clamp to a sane range
        SlotData before = SlotManager.getById(id);
        if (before == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        if (locked(src, id)) return 0;
        SlotData d = SlotManager.setHardness(id, value);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "hardness");
        HudSync.broadcast(src.getServer()); // NO-REJOIN: HUD hardness updates live for all players
        // Read live on every break attempt (SlotBlock.calcBlockBreakingDelta).
        if (value < 0)       Chat.success(src, "\"" + id + "\" is now unbreakable.");
        else if (value == 0) Chat.success(src, "\"" + id + "\" now breaks instantly.");
        else                 Chat.success(src, "Set \"" + id + "\" hardness to " + trim(value) + ".");
        return 1;
    }

    /** Parse a hardness value: a number, or a friendly keyword. Returns null if unrecognized. */
    private static Float parseHardness(String raw) {
        switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "unbreakable": case "unbreak": case "indestructible": case "bedrock":
                return -1.0f;
            case "instant": case "instabreak": case "instant-break": case "soft":
                return 0.0f;
            case "stone": case "default": case "normal":
                return 1.5f;
            default:
                try { return Float.parseFloat(raw.trim()); }
                catch (NumberFormatException e) { return null; }
        }
    }

    /** Drop a trailing ".0" so 50.0 shows as "50" but 1.5 stays "1.5". */
    private static String trim(float v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    private static int setSound(CommandContext<ServerCommandSource> ctx, String id, String type) {
        ServerCommandSource src = ctx.getSource();
        String key = type.trim().toLowerCase(Locale.ROOT);
        boolean valid = false;
        for (String s : SlotBlock.SOUND_TYPES) {
            if (s.equals(key)) { valid = true; break; }
        }
        if (!valid) {
            Chat.error(src, "Unknown sound \"" + type + "\". Options: " + String.join(", ", SlotBlock.SOUND_TYPES));
            return 0;
        }
        SlotData before = SlotManager.getById(id);
        if (before == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        if (locked(src, id)) return 0;
        SlotData d = SlotManager.setSoundType(id, key);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "sound");
        HudSync.broadcast(src.getServer()); // NO-REJOIN: HUD sound updates live for all players
        // Read live in SlotBlock.getSoundGroup — placed blocks use the new sound immediately.
        Chat.success(src, "Set \"" + id + "\" sound to " + key + ".");
        return 1;
    }

    private static int setCollision(CommandContext<ServerCommandSource> ctx, String id, String raw) {
        ServerCommandSource src = ctx.getSource();
        Boolean passable = parseCollision(raw);
        if (passable == null) {
            Chat.error(src, "Use: solid or passable");
            return 0;
        }
        SlotData before = SlotManager.getById(id);
        if (before == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        if (locked(src, id)) return 0;
        SlotData d = SlotManager.setNoCollision(id, passable);
        if (d == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "collision");
        HudSync.broadcast(src.getServer()); // NO-REJOIN: HUD passable/solid updates live for all players
        // Read live in SlotBlock.getCollisionShape — placed blocks update immediately.
        Chat.success(src, "\"" + id + "\" is now " + (passable ? "passable — players walk through it." : "solid."));
        return 1;
    }

    /**
     * /cb setcategory &lt;id&gt; &lt;category&gt;… — G11: this ADDS memberships now, it does not replace them.
     *
     * Assigning a block somewhere new never silently removes it from where it already is (G11
     * Locked Decisions); clearing needs {@code none} or {@code /cb category remove}. Several
     * categories can be named at once, and a multi-word name is quoted — otherwise "Arabic Letters"
     * would read as two separate categories.
     */
    private static int setCategory(CommandContext<ServerCommandSource> ctx, String id, String raw) {
        ServerCommandSource src = ctx.getSource();
        SlotData before = SlotManager.getById(id);
        if (before == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        if (locked(src, id)) return 0;

        List<String> names = splitCategories(raw);
        if (names.isEmpty()) { Chat.error(src, "Name at least one category (or 'none' to clear)."); return 0; }

        // "none" is still the clear escape hatch — it drops every membership, so the block falls
        // back to Uncategorized rather than adding a category literally named "none".
        String first = names.get(0);
        if (names.size() == 1 && (first.equalsIgnoreCase("none") || first.equalsIgnoreCase("clear")
                || first.equalsIgnoreCase("uncategorized"))) {
            List<String> was = new ArrayList<>(CategoryMembershipStore.of(before.customId()));
            SlotManager.setCategory(id, "");
            UndoManager.recordMembership(actor(src), before.customId(), was,
                    CategoryMembershipStore.of(before.customId()), "category");
            HudSync.broadcast(src.getServer());
            Chat.success(src, "Removed \"" + id + "\" from every category — it's Uncategorized now.");
            return 1;
        }

        List<String> was = new ArrayList<>(CategoryMembershipStore.of(before.customId()));
        List<String> added = new ArrayList<>();
        for (String name : names) {
            CategoryService.Outcome o = CategoryService.addMembership(null, id, name);
            if (!o.ok()) { Chat.error(src, o.msg()); continue; }
            added.add(name);
        }
        if (added.isEmpty()) return 0;
        // One undo step for the whole command line, not one per name (addMembership was passed a
        // null actor above precisely so it wouldn't push a step of its own).
        UndoManager.recordMembership(actor(src), before.customId(), was,
                CategoryMembershipStore.of(before.customId()), "category");
        HudSync.broadcast(src.getServer()); // NO-REJOIN: HUD category updates live for all players
        // G11 C13: the success line carries the obvious next step — open the category it landed in.
        Chat.successRich(src, CategoryChat.decorate("\"" + id + "\" is now in " + String.join(", ", added)
                        + " (" + CategoryMembershipStore.of(before.customId()).size() + " total)."),
                CategoryChat.viewButton(added.get(0)));
        return 1;
    }

    /** Split a category argument into names: bare words, or "quoted phrases" for multi-word names. */
    private static List<String> splitCategories(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        Matcher m = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(raw.trim());
        while (m.find()) {
            String s = (m.group(1) != null ? m.group(1) : m.group(2)).trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    /** Parse a collision mode. Returns true (passable), false (solid), or null if unrecognized. */
    private static Boolean parseCollision(String raw) {
        switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "passable": case "through": case "walkthrough": case "walk": case "none":
            case "off": case "no": case "false": case "ghost": case "decor":
                return Boolean.TRUE;
            case "solid": case "on": case "yes": case "true": case "normal": case "block":
                return Boolean.FALSE;
            default:
                return null;
        }
    }
}
