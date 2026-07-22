/**
 * BuzzerGameCommands.java — Group 31 (BuzzerGame) item D (solo stopwatch, 2026-07-18).
 *
 * The single {@code /cb buzzergame ...} handler, registered from
 * {@link com.customblocks.command.CommandRegistrar} like every other {@code *Commands.java}. The control
 * verbs act on the CALLER'S OWN wand session ({@link BuzzerSessionManager}) — there is no admin panel and
 * no op gate (anyone can host, design lock 2026-07-18). Leaves:
 *
 *   /cb buzzergame give buzzer|wand|display        → hand yourself the item (real Items; /give + tab also work)
 *   /cb buzzergame start <value>                   → arm YOUR solo target (0.5–60s; 5, 5s, 5.5, 5.5s)
 *   /cb buzzergame reset                           → clear the target, screen back to a single idle 0.00
 *   /cb buzzergame size small|medium|large|<scale> → resize the whole timer stand you're looking at
 *   /cb buzzergame textcolor <#hex>                → recolor all screen text (default #15FF00)
 *   /cb buzzergame textsize <n>                    → scale the screen text (up to 30×)
 *   /cb buzzergame help                            → list this command set
 *
 * The buzzer press cycle (start → freeze → reset) drives the on-screen result; the old {@code stop} and
 * {@code reveal} commands and the action-bar timer were removed with the solo rework. Link a buzzer + one
 * timer stand with the wand first (right-click them in Link mode).
 *
 * Depends on: BuzzerGameRegistry (Items), BuzzerSession, BuzzerSessionManager, TimerDisplayBlockEntity,
 *             TimerDisplayVisual, Chat
 * Called by:  CommandRegistrar.register()
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.buzzergame.BuzzerGameRegistry;
import com.customblocks.buzzergame.BuzzerSession;
import com.customblocks.buzzergame.BuzzerSessionManager;
import com.customblocks.buzzergame.TimerDisplayBlockEntity;
import com.customblocks.buzzergame.TimerDisplayVisual;
import com.customblocks.command.Chat;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

public final class BuzzerGameCommands {

    private BuzzerGameCommands() {} // static-only

    /** How far the crosshair reaches to find the timer stand a stand-targeting command acts on. */
    private static final double STAND_REACH = 6.0;

    /** Solo target bounds (seconds), design lock 2026-07-18. */
    private static final double MIN_TARGET = 0.5;
    private static final double MAX_TARGET = 60.0;

    /** Text-size clamp for {@code textsize <n>} (design lock: up to 30×). */
    private static final float TEXT_SIZE_MIN = 0.2f;
    private static final float TEXT_SIZE_MAX = 30.0f;

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("buzzergame")
                .then(CommandManager.literal("give")
                        .then(CommandManager.literal("buzzer").executes(ctx -> giveBlock(ctx, BuzzerGameRegistry.ITEM, "Buzzer")))
                        .then(CommandManager.literal("wand").executes(ctx -> giveBlock(ctx, BuzzerGameRegistry.WAND, "BuzzerGame Wand")))
                        .then(CommandManager.literal("display").executes(ctx -> giveBlock(ctx, BuzzerGameRegistry.TIMER_DISPLAY_ITEM, "Timer Stand"))))
                .then(CommandManager.literal("start")
                        .executes(BuzzerGameCommands::startNoValue)
                        .then(CommandManager.argument("value", StringArgumentType.word())
                                .executes(BuzzerGameCommands::start)))
                .then(CommandManager.literal("reset").executes(BuzzerGameCommands::reset))
                .then(CommandManager.literal("help").executes(BuzzerGameCommands::help))
                .then(CommandManager.literal("textcolor")
                        .then(CommandManager.argument("hex", StringArgumentType.word())
                                .executes(BuzzerGameCommands::textColor)))
                .then(CommandManager.literal("textsize")
                        .then(CommandManager.argument("n", FloatArgumentType.floatArg(TEXT_SIZE_MIN, TEXT_SIZE_MAX))
                                .executes(ctx -> setTextSize(ctx, FloatArgumentType.getFloat(ctx, "n")))))
                .then(CommandManager.literal("size")
                        .then(CommandManager.literal("small").executes(ctx -> setSize(ctx, TimerDisplayVisual.SCALE_SMALL)))
                        .then(CommandManager.literal("medium").executes(ctx -> setSize(ctx, TimerDisplayVisual.SCALE_MEDIUM)))
                        .then(CommandManager.literal("large").executes(ctx -> setSize(ctx, TimerDisplayVisual.SCALE_LARGE)))
                        .then(CommandManager.argument("scale", FloatArgumentType.floatArg(0.1f, 5.0f))
                                .executes(ctx -> setSize(ctx, FloatArgumentType.getFloat(ctx, "scale")))
                                .then(CommandManager.literal("setdefault")
                                        .executes(ctx -> setSizeDefault(ctx, FloatArgumentType.getFloat(ctx, "scale")))))));
    }

    // ------------------------------------------------------------------ give

    /** /cb buzzergame give buzzer|wand|display — one item to the caller. */
    private static int giveBlock(CommandContext<ServerCommandSource> ctx, Item item, String name) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        ItemStack stack = new ItemStack(item);
        boolean inserted = player.getInventory().insertStack(stack);
        if (!inserted && stack.getCount() > 0) {
            Chat.error(src, "Couldn't give you a " + name + " — your inventory is full.");
            return 0;
        }
        Chat.success(src, "Gave you 1 × " + name + ".");
        return 1;
    }

    // ------------------------------------------------------------------ solo stopwatch control

    /** Bare {@code /cb buzzergame start} — a target value is required. */
    private static int startNoValue(CommandContext<ServerCommandSource> ctx) {
        Chat.error(ctx.getSource(), "start needs a target time — e.g. /cb buzzergame start 5 (or 5s, 5.5, 5.5s).");
        return 0;
    }

    /** /cb buzzergame start <value> — arm YOUR solo target (0.5–60s). */
    private static int start(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        BuzzerSession session = ownSession(src, player);
        if (session == null) return 0;

        String raw = StringArgumentType.getString(ctx, "value");
        Double secs = parseSeconds(raw);
        if (secs == null) {
            Chat.error(src, "\"" + raw + "\" isn't a valid time — use seconds like 5, 5s, or 5.5.");
            return 0;
        }
        if (secs < MIN_TARGET || secs > MAX_TARGET) {
            Chat.error(src, String.format(Locale.ROOT,
                    "Target must be between %.1fs and %.0fs (you gave %s).", MIN_TARGET, MAX_TARGET, trimNum(secs)));
            return 0;
        }
        BuzzerSession.Result r = session.armTarget(secs);
        if (r.ok()) Chat.success(src, r.message()); else Chat.error(src, r.message());
        return r.ok() ? 1 : 0;
    }

    /** /cb buzzergame reset — clear the target; the screen returns to a single idle 0.00. */
    private static int reset(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        BuzzerSession session = ownSession(src, player);
        if (session == null) return 0;
        BuzzerSession.Result r = session.reset();
        if (r.ok()) Chat.success(src, "Session reset — screen back to idle 0.00. Run start again to arm.");
        else Chat.error(src, r.message());
        return r.ok() ? 1 : 0;
    }

    /** /cb buzzergame help — list the command set. */
    private static int help(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Chat.info(src, "BuzzerGame commands:");
        Chat.info(src, "  give buzzer|wand|display — get the gear");
        Chat.info(src, "  Right-click the wand to start your session, then right-click a buzzer + a timer stand to link them.");
        Chat.info(src, "  start <5|5s|5.5> — arm a target (0.5–60s); press the buzzer to start/freeze/reset");
        Chat.info(src, "  reset — clear the target, screen back to idle 0.00");
        Chat.info(src, "  size small|medium|large|<0.1-5.0> [setdefault] — resize the stand you're aiming at (setdefault also saves it as the spawn size)");
        Chat.info(src, "  textcolor <#hex> · textsize <n up to 30> — restyle the stand's text");
        return 1;
    }

    // ------------------------------------------------------------------ stand-targeting (textcolor / textsize / size)

    /** /cb buzzergame textcolor <#hex> — recolor ALL screen text on the stand you're aiming at. */
    private static int textColor(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        Integer rgb = parseHex(StringArgumentType.getString(ctx, "hex"));
        if (rgb == null) {
            Chat.error(src, "Give a colour as a 6-digit hex, e.g. #15FF00 or ff0000.");
            return 0;
        }
        TimerDisplayBlockEntity display = lookedAtDisplay(player);
        if (display == null) {
            Chat.error(src, "Look directly at a timer stand to recolor it (within " + (int) STAND_REACH + " blocks).");
            return 0;
        }
        display.setTextColor(rgb);
        Chat.success(src, String.format(Locale.ROOT, "Timer text colour set to #%06X.", rgb & 0xFFFFFF));
        return 1;
    }

    /** /cb buzzergame textsize <n> — scale the stand's text (up to 30×). */
    private static int setTextSize(CommandContext<ServerCommandSource> ctx, float n) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        TimerDisplayBlockEntity display = lookedAtDisplay(player);
        if (display == null) {
            Chat.error(src, "Look directly at a timer stand to resize its text (within " + (int) STAND_REACH + " blocks).");
            return 0;
        }
        display.setTextScale(n);
        Chat.success(src, String.format(Locale.ROOT, "Timer text size set to %.2f×.", display.getTextScale()));
        return 1;
    }

    /** /cb buzzergame size small|medium|large|<scale> — resize the whole timer stand you're looking at. */
    private static int setSize(CommandContext<ServerCommandSource> ctx, float scale) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        TimerDisplayBlockEntity display = lookedAtDisplay(player);
        if (display == null) {
            Chat.error(src, "Look directly at a timer stand to resize it (within " + (int) STAND_REACH + " blocks).");
            return 0;
        }
        display.setScale(scale);
        Chat.success(src, String.format(Locale.ROOT, "Timer stand size set to %.2f×.", display.getScale()));
        return 1;
    }

    /** /cb buzzergame size <scale> setdefault — persist the spawn default for NEW stands; also apply it to the
     *  stand you're aiming at, if any (design lock 2026-07-19). Works with no stand in view — it just saves. */
    private static int setSizeDefault(CommandContext<ServerCommandSource> ctx, float scale) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        float clamped = TimerDisplayVisual.clampScale(scale);
        CustomBlocksConfig.timerDefaultScale = clamped;
        CustomBlocksConfig.save();
        TimerDisplayBlockEntity display = lookedAtDisplay(player);
        if (display != null) display.setScale(clamped);
        Chat.success(src, String.format(Locale.ROOT,
                "Spawn default size set to %.2f× (saved)%s.", clamped,
                display != null ? " and applied to the stand you're aiming at" : ""));
        return 1;
    }

    // ------------------------------------------------------------------ helpers

    /** The caller's own wand session, or null (with a nudge) if they haven't started one yet. */
    private static BuzzerSession ownSession(ServerCommandSource src, ServerPlayerEntity player) {
        BuzzerSession session = BuzzerSessionManager.get(player.getUuid());
        if (session == null) {
            Chat.error(src, "No BuzzerGame session yet — right-click the BuzzerGame wand to start one.");
        }
        return session;
    }

    /** The timer stand the player's crosshair is on, or null if they aren't looking at one in reach. The stand
     *  block has no outline (I4), so we raycast its part INTERACTION entities and map the hit back to the block. */
    private static TimerDisplayBlockEntity lookedAtDisplay(ServerPlayerEntity player) {
        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(STAND_REACH));
        Box box = player.getBoundingBox().stretch(dir.multiply(STAND_REACH)).expand(1.0);
        EntityHitResult ehr = ProjectileUtil.raycast(player, start, end, box,
                com.customblocks.buzzergame.TimerHitbox::isHitEntity, STAND_REACH * STAND_REACH);
        if (ehr != null) {
            BlockPos pos = com.customblocks.buzzergame.TimerHitbox.hitPos(ehr.getEntity());
            if (pos != null && player.getWorld().getBlockEntity(pos) instanceof TimerDisplayBlockEntity display) {
                return display;
            }
        }
        return null;
    }

    /** Parse a target like "5", "5s", "5.5", "5.5s" to seconds, or null if it isn't a number. */
    private static Double parseSeconds(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        if (s.endsWith("s") || s.endsWith("S")) s = s.substring(0, s.length() - 1);
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parse a 6-digit hex colour ("#RRGGBB" or "RRGGBB") to 0xRRGGBB, or null if malformed. */
    private static Integer parseHex(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6) return null;
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Trim a whole-number double to "5" but keep "5.5" — for echoing the user's input back. */
    private static String trimNum(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
