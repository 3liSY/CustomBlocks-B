/**
 * AchievementCommands.java
 *
 * Responsibility: /cb achievements — print the player's achievement list in chat: which are
 * unlocked, which are still locked, and live "X/target" progress for the counter milestones.
 * This is the text view the unlock toast's [View] link runs; the richer gallery is a Group 27
 * screen (G27.16) that will reuse the same engine data.
 *
 * Reads only through AchievementManager (the engine facade) — never AchievementStore directly.
 *
 * Depends on: Achievement, Achievements, AchievementManager, Chat
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.onboarding.Achievement;
import com.customblocks.core.onboarding.Achievement.Counter;
import com.customblocks.core.onboarding.AchievementManager;
import com.customblocks.core.onboarding.Achievements;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public final class AchievementCommands {

    private AchievementCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("achievements")
                .executes(AchievementCommands::show));
    }

    // /cb achievements — list every achievement with unlocked/locked state and progress.
    private static int show(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        UUID uuid = (src.getEntity() instanceof ServerPlayerEntity p) ? p.getUuid() : null;

        int unlocked = (uuid == null) ? 0 : AchievementManager.unlockedCount(uuid);
        int total = AchievementManager.total();

        Chat.raw(src, CbFmt.HEAD + CbFmt.BOLD + "Your Achievements");
        Chat.raw(src, Text.literal(CbFmt.DIM + unlocked + " of " + total + " unlocked"));

        for (Achievement a : Achievements.all()) {
            Chat.raw(src, Text.literal(line(uuid, a)));
        }
        return 1;
    }

    /** One formatted list row for an achievement, given the viewer (or null for console). */
    private static String line(UUID uuid, Achievement a) {
        boolean done = uuid != null && AchievementManager.isUnlocked(uuid, a.key());
        if (done) {
            return CbFmt.OK + "[x] " + CbFmt.BODY + a.name() + " " + CbFmt.DIM + "- " + a.description();
        }
        // Active counter milestone that is not yet unlocked: show live progress, e.g. [3/10].
        if (a.active() && a.counter() != Counter.NONE) {
            int have = (uuid == null) ? 0 : AchievementManager.counterValue(uuid, a.counter());
            int need = a.target();
            int shown = Math.min(have, need);
            return CbFmt.VALUE + "[" + shown + "/" + need + "] " + CbFmt.BODY + a.name() + " " + CbFmt.DIM + "- " + a.description();
        }
        // Feature-gated achievement still locked.
        return CbFmt.FAINT + "[ ] " + CbFmt.DIM + a.name() + " " + CbFmt.FAINT + "- " + a.description();
    }
}
