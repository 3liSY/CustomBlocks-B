/**
 * AttributeCommands.java
 *
 * Responsibility: Per-block attribute setters — currently `setglow` (light emission).
 * Registered into the /cb tree by CommandRegistrar. All mutations go through SlotManager
 * (the single source of truth). Stays under 400 lines (§9.3).
 *
 * Phase 6 (attributes): setglow + sethardness. setsound / shape commands land here next,
 * keeping CreationCommands focused on the block lifecycle.
 */
package com.customblocks.command.handlers;

import com.customblocks.block.SlotBlock;
import com.customblocks.block.SlotLighting;
import com.customblocks.command.Chat;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;
import java.util.UUID;

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
                                .suggests((c, b) -> {
                                    b.suggest("none");
                                    for (String s : SlotManager.categories()) b.suggest(s);
                                    return b.buildFuture();
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
            Chat.error(src, "'" + id + "' is locked — /cb unlock " + id + " first");
            return true;
        }
        return false;
    }

    private static int setGlow(CommandContext<ServerCommandSource> ctx, String id, int level) {
        ServerCommandSource src = ctx.getSource();
        int clamped = Math.max(0, Math.min(15, level)); // 15 is Minecraft's max block light
        SlotData before = SlotManager.getById(id);
        if (before == null) { Chat.error(src, "No block '" + id + "'"); return 0; }
        if (locked(src, id)) return 0;
        SlotData d = SlotManager.setGlow(id, clamped);
        if (d == null) {
            Chat.error(src, "No block '" + id + "'");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "glow");
        // New placements inherit it via getPlacementState; refresh already-placed ones nearby.
        SlotLighting.applyToPlaced(src.getServer(), d.index(), clamped);
        String note = level > 15 ? " §7(max 15)" : level < 0 ? " §7(min 0)" : "";
        Chat.success(src, "Glow " + id + " → " + clamped + note);
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
        if (before == null) { Chat.error(src, "No block '" + id + "'"); return 0; }
        if (locked(src, id)) return 0;
        SlotData d = SlotManager.setHardness(id, value);
        if (d == null) {
            Chat.error(src, "No block '" + id + "'");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "hardness");
        // Read live on every break attempt (SlotBlock.calcBlockBreakingDelta).
        if (value < 0)       Chat.success(src, id + " → unbreakable");
        else if (value == 0) Chat.success(src, id + " → instant break");
        else                 Chat.success(src, "Hardness " + id + " → " + trim(value));
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
            Chat.error(src, "Unknown sound '" + type + "'. Options: " + String.join(", ", SlotBlock.SOUND_TYPES));
            return 0;
        }
        SlotData before = SlotManager.getById(id);
        if (before == null) { Chat.error(src, "No block '" + id + "'"); return 0; }
        if (locked(src, id)) return 0;
        SlotData d = SlotManager.setSoundType(id, key);
        if (d == null) {
            Chat.error(src, "No block '" + id + "'");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "sound");
        // Read live in SlotBlock.getSoundGroup — placed blocks use the new sound immediately.
        Chat.success(src, "Sound " + id + " → " + key);
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
        if (before == null) { Chat.error(src, "No block '" + id + "'"); return 0; }
        if (locked(src, id)) return 0;
        SlotData d = SlotManager.setNoCollision(id, passable);
        if (d == null) {
            Chat.error(src, "No block '" + id + "'");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "collision");
        // Read live in SlotBlock.getCollisionShape — placed blocks update immediately.
        Chat.success(src, id + " → " + (passable ? "passable (walk-through)" : "solid"));
        return 1;
    }

    private static int setCategory(CommandContext<ServerCommandSource> ctx, String id, String raw) {
        ServerCommandSource src = ctx.getSource();
        String cat = raw.trim();
        if (cat.equalsIgnoreCase("none") || cat.equalsIgnoreCase("clear") || cat.equalsIgnoreCase("uncategorized")) {
            cat = ""; // clear
        } else {
            cat = cat.toLowerCase(Locale.ROOT);
        }
        SlotData before = SlotManager.getById(id);
        if (before == null) { Chat.error(src, "No block '" + id + "'"); return 0; }
        if (locked(src, id)) return 0;
        SlotData d = SlotManager.setCategory(id, cat);
        if (d == null) {
            Chat.error(src, "No block '" + id + "'");
            return 0;
        }
        UndoManager.recordModify(actor(src), before, d, "category");
        Chat.success(src, cat.isEmpty() ? id + " → uncategorized" : "Category " + id + " → " + cat);
        return 1;
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
