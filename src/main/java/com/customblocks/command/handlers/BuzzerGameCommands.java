/**
 * BuzzerGameCommands.java — Group 31 (BuzzerGame) Phase 1 items 1-2-3-5-6.
 *
 * The single {@code /cb buzzergame ...} handler, registered from
 * {@link com.customblocks.command.CommandRegistrar} like every other {@code *Commands.java}. Split
 * further if it nears the 400-line handler cap (§9.3). Leaves:
 *
 *   /cb buzzergame give buzzer|panel|wand|display → hand yourself the item (real Items; /give + tab also work)
 *   /cb buzzergame start|stop|reset        → drive the round on the admin panel you're looking at
 *                                            (reset on a live round asks for a second confirm within 5s)
 *   /cb buzzergame reveal                  → publish the frozen winner near the panel (RESULTS only)
 *   /cb buzzergame size small|medium|large|<scale> → resize the timer stand you're looking at
 *   /cb buzzergame help                    → list this command set
 *
 * Mode / format / target / countdown / false-start (and the old debug state/advance) moved into the admin
 * panel Screen (item 4) — right-click the panel. All control verbs act on the {@link AdminPanelBlockEntity}
 * the player's crosshair is on, and are operators-only (design lock). Link buzzers with the wand first.
 *
 * Depends on: BuzzerGameRegistry (Items), AdminPanelBlockEntity, PanelSession, SessionState, RoundBroadcast, Chat
 * Called by:  CommandRegistrar.register()
 */
package com.customblocks.command.handlers;

import com.customblocks.buzzergame.AdminPanelBlockEntity;
import com.customblocks.buzzergame.BuzzerGameRegistry;
import com.customblocks.buzzergame.PanelSession;
import com.customblocks.buzzergame.RoundBroadcast;
import com.customblocks.buzzergame.SessionState;
import com.customblocks.buzzergame.TimerDisplayBlockEntity;
import com.customblocks.buzzergame.TimerDisplayVisual;
import com.customblocks.command.Chat;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BuzzerGameCommands {

    private BuzzerGameCommands() {} // static-only

    /** How far the crosshair reaches to find the admin panel a control command targets. */
    private static final double PANEL_REACH = 6.0;

    /** Per-player pending reset time — a live-round reset needs a second command within this window. */
    private static final Map<UUID, Long> PENDING_RESET = new HashMap<>();
    private static final long RESET_CONFIRM_MS = 5000L;

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("buzzergame")
                .then(CommandManager.literal("give")
                        .then(CommandManager.literal("buzzer").executes(ctx -> giveBlock(ctx, BuzzerGameRegistry.ITEM, "Buzzer")))
                        .then(CommandManager.literal("panel").executes(ctx -> giveBlock(ctx, BuzzerGameRegistry.ADMIN_PANEL_ITEM, "Admin Panel")))
                        .then(CommandManager.literal("wand").executes(ctx -> giveBlock(ctx, BuzzerGameRegistry.WAND, "BuzzerGame Wand")))
                        .then(CommandManager.literal("display").executes(ctx -> giveBlock(ctx, BuzzerGameRegistry.TIMER_DISPLAY_ITEM, "Timer Stand"))))
                .then(CommandManager.literal("start").executes(BuzzerGameCommands::start))
                .then(CommandManager.literal("stop").executes(BuzzerGameCommands::stop))
                .then(CommandManager.literal("reset").executes(BuzzerGameCommands::reset))
                .then(CommandManager.literal("reveal").executes(BuzzerGameCommands::reveal))
                .then(CommandManager.literal("help").executes(BuzzerGameCommands::help))
                .then(CommandManager.literal("size")
                        .then(CommandManager.literal("small").executes(ctx -> setSize(ctx, TimerDisplayVisual.SCALE_SMALL)))
                        .then(CommandManager.literal("medium").executes(ctx -> setSize(ctx, TimerDisplayVisual.SCALE_MEDIUM)))
                        .then(CommandManager.literal("large").executes(ctx -> setSize(ctx, TimerDisplayVisual.SCALE_LARGE)))
                        .then(CommandManager.argument("scale", FloatArgumentType.floatArg(0.4f, 3.0f))
                                .executes(ctx -> setSize(ctx, FloatArgumentType.getFloat(ctx, "scale"))))));
    }

    // ------------------------------------------------------------------ give

    /** /cb buzzergame give buzzer|panel — one block to the caller. */
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

    // ------------------------------------------------------------------ control

    private static int start(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return control(ctx, panel -> panel.session().tryStart());
    }

    private static int stop(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return control(ctx, panel -> panel.session().stop());
    }

    /** /cb buzzergame reset — reset the targeted panel; a live round needs a second confirm within 5s. */
    private static int reset(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        if (!requireOp(src)) return 0;
        AdminPanelBlockEntity panel = lookedAtPanel(player);
        if (panel == null) { noPanel(src); return 0; }
        SessionState st = panel.session().state();
        boolean live = st == SessionState.COUNTDOWN || st == SessionState.RUNNING || st == SessionState.RESULTS;
        if (live) {
            long now = System.currentTimeMillis();
            Long prev = PENDING_RESET.get(player.getUuid());
            if (prev == null || now - prev > RESET_CONFIRM_MS) {
                PENDING_RESET.put(player.getUuid(), now);
                Chat.info(src, "Reset will end the live round — run /cb buzzergame reset again within 5s to confirm.");
                return 0;
            }
            PENDING_RESET.remove(player.getUuid());
        }
        PanelSession.Result r = panel.session().reset();
        panel.markDirty();
        if (r.ok()) Chat.success(src, r.message()); else Chat.error(src, r.message());
        return r.ok() ? 1 : 0;
    }

    /** /cb buzzergame reveal — publish the winner near the panel and finish the round (RESULTS only). */
    private static int reveal(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        if (!requireOp(src)) return 0;
        AdminPanelBlockEntity panel = lookedAtPanel(player);
        if (panel == null) { noPanel(src); return 0; }
        PanelSession.Result result = panel.session().reveal();
        panel.markDirty();
        if (result.ok()) {
            RoundBroadcast.announceReveal(player.getServerWorld(), panel.getPos(), panel.session());
            Chat.success(src, result.message());
            return 1;
        }
        Chat.error(src, result.message());
        return 0;
    }

    /** /cb buzzergame help — list the trimmed command set. */
    private static int help(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Chat.info(src, "BuzzerGame commands:");
        Chat.info(src, "  give buzzer|panel|wand|display — get the gear");
        Chat.info(src, "  start · stop · reset · reveal — run the round (aim at the panel)");
        Chat.info(src, "  size small|medium|large|<0.4-3.0> — resize the timer stand you're aiming at");
        Chat.info(src, "  Mode / format / target / countdown / false-start + links: right-click the panel (op).");
        return 1;
    }

    /**
     * Shared plumbing for the mutating verbs: require op, find the looked-at panel, run the action on its
     * session, persist (markDirty), and report the result message.
     */
    private static int control(CommandContext<ServerCommandSource> ctx, PanelAction action) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        if (!requireOp(src)) return 0;
        AdminPanelBlockEntity panel = lookedAtPanel(player);
        if (panel == null) { noPanel(src); return 0; }
        PanelSession.Result result = action.run(panel);
        panel.markDirty();
        if (result.ok()) Chat.success(src, result.message());
        else Chat.error(src, result.message());
        return result.ok() ? 1 : 0;
    }

    @FunctionalInterface
    private interface PanelAction {
        PanelSession.Result run(AdminPanelBlockEntity panel);
    }

    // ------------------------------------------------------------------ helpers

    private static boolean requireOp(ServerCommandSource src) {
        if (src.hasPermissionLevel(2)) return true;
        Chat.error(src, "Only operators can control the admin panel.");
        return false;
    }

    /** The admin panel the player's crosshair is on, or null if they aren't looking at one in reach. */
    private static AdminPanelBlockEntity lookedAtPanel(ServerPlayerEntity player) {
        HitResult hit = player.raycast(PANEL_REACH, 1.0f, false);
        if (hit instanceof BlockHitResult bhr) {
            BlockEntity be = player.getWorld().getBlockEntity(bhr.getBlockPos());
            if (be instanceof AdminPanelBlockEntity panel) return panel;
        }
        return null;
    }

    private static void noPanel(ServerCommandSource src) {
        Chat.error(src, "Look directly at an admin panel to control it (within " + (int) PANEL_REACH + " blocks).");
    }

    /** /cb buzzergame size small|medium|large|<scale> — resize the timer stand you're looking at (op-only). */
    private static int setSize(CommandContext<ServerCommandSource> ctx, float scale) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        if (!requireOp(src)) return 0;
        TimerDisplayBlockEntity display = lookedAtDisplay(player);
        if (display == null) {
            Chat.error(src, "Look directly at a timer stand to resize it (within " + (int) PANEL_REACH + " blocks).");
            return 0;
        }
        display.setScale(scale);
        Chat.success(src, String.format(java.util.Locale.ROOT, "Timer stand size set to %.2fx.", display.getScale()));
        return 1;
    }

    /** The timer stand the player's crosshair is on, or null if they aren't looking at one in reach. */
    private static TimerDisplayBlockEntity lookedAtDisplay(ServerPlayerEntity player) {
        HitResult hit = player.raycast(PANEL_REACH, 1.0f, false);
        if (hit instanceof BlockHitResult bhr
                && player.getWorld().getBlockEntity(bhr.getBlockPos()) instanceof TimerDisplayBlockEntity display) {
            return display;
        }
        return null;
    }
}
