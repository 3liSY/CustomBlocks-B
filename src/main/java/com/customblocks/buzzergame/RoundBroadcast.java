/**
 * RoundBroadcast.java — Group 31 (BuzzerGame) Phase 1 items 5-6.
 *
 * The world/packet side of a round: turns the state-only {@link PanelSession}/{@link RoundBeat} into what
 * players actually see and hear — an action-bar timer, big 3-2-1-GO titles, the reveal, and sound cues.
 * Everything is broadcast **near the panel only** (design: near-game broadcast by default, not server-wide),
 * so two games in the same world don't bleed into each other. Kept separate from PanelSession so the state
 * machine stays world-free and testable.
 *
 * Depends on: PanelSession, RoundBeat, SoundEvents (BLOCK_NOTE_BLOCK_* use .value())
 * Called by:  AdminPanelBlockEntity.serverTick (per-tick render), BuzzerGameCommands (reveal), BuzzerBlock (warn)
 */
package com.customblocks.buzzergame;

import com.customblocks.command.Chat;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class RoundBroadcast {

    private RoundBroadcast() {} // static-only

    /** How far from the panel counts as "near the game" for announcements. */
    private static final double RADIUS = 48.0;
    private static final double RADIUS_SQ = RADIUS * RADIUS;

    /** CB-B brand: lime = winner/success, red = disqualified (reveal rebrand, item 5). */
    private static final int LIME = 0x40FF00;
    private static final int RED = 0xFF0000;

    /** Per-tick render: refresh the action-bar timer for nearby players, then fire this tick's beat. */
    public static void render(ServerWorld world, BlockPos panelPos, PanelSession session, RoundBeat beat) {
        List<ServerPlayerEntity> near = nearby(world, panelPos);
        if (near.isEmpty()) return;

        String line = session.displayLine();
        if (!line.isEmpty()) {
            Text bar = Text.literal(line).formatted(Formatting.YELLOW);
            for (ServerPlayerEntity p : near) Chat.toolRaw(p, bar);
        }
        if (beat.title() != null) {
            sendTitle(near, Text.literal(beat.title()).formatted(Formatting.GOLD),
                    beat.subtitle() == null ? null : Text.literal(beat.subtitle()), 2, 18, 6);
        }
        playCue(world, panelPos, beat.sound());
    }

    /**
     * The reveal moment: a big lime winner title + one chat line per rank (winner lime, DQ red) + fanfare +
     * a particle pop, near the panel. Duel/Party ranking comes from {@link PanelSession#revealLines()}.
     */
    public static void announceReveal(ServerWorld world, BlockPos panelPos, PanelSession session) {
        List<ServerPlayerEntity> near = nearby(world, panelPos);
        Text title = Text.literal(session.revealTitle())
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(LIME)).withBold(true));
        Text subtitle = Text.literal(session.revealSubtitle()).formatted(Formatting.WHITE);
        sendTitle(near, title, subtitle, 4, 60, 10);
        for (ServerPlayerEntity p : near) {
            Chat.toPlayerUnbranded(p, Text.literal("[BuzzerGame] " + session.revealTitle())
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(LIME)).withBold(true)));
            for (String line : session.revealLines()) {
                int color = line.startsWith("1st") ? LIME : line.startsWith("Disqualified") ? RED : 0xFFFFFF;
                Chat.toPlayerUnbranded(p, Text.literal("  " + line).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
            }
        }
        playCue(world, panelPos, RoundBeat.Sound.WIN);
        Vec3d c = Vec3d.ofCenter(panelPos).add(0, 1.0, 0);
        world.spawnParticles(ParticleTypes.FIREWORK, c.x, c.y, c.z, 40, 0.6, 0.6, 0.6, 0.05);
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y, c.z, 20, 0.5, 0.5, 0.5, 0.0);
    }

    /** Warn everyone near a panel (e.g. a linked buzzer was broken mid-setup). */
    public static void warnNear(ServerWorld world, BlockPos panelPos, String message) {
        for (ServerPlayerEntity p : nearby(world, panelPos)) {
            Chat.toPlayerUnbranded(p, Text.literal("[BuzzerGame] " + message).formatted(Formatting.YELLOW));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static List<ServerPlayerEntity> nearby(ServerWorld world, BlockPos panelPos) {
        Vec3d center = Vec3d.ofCenter(panelPos);
        List<ServerPlayerEntity> out = new ArrayList<>();
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.getPos().squaredDistanceTo(center) <= RADIUS_SQ) out.add(p);
        }
        return out;
    }

    private static void sendTitle(List<ServerPlayerEntity> players, Text title, Text subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        for (ServerPlayerEntity p : players) {
            p.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
            if (subtitle != null) p.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
            p.networkHandler.sendPacket(new TitleS2CPacket(title));
        }
    }

    private static void playCue(ServerWorld world, BlockPos pos, RoundBeat.Sound sound) {
        SoundEvent event;
        float pitch;
        switch (sound) {
            case TICK -> { event = SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(); pitch = 1.2f; }
            case GO -> { event = SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(); pitch = 1.5f; }
            case WIN -> { event = SoundEvents.UI_TOAST_CHALLENGE_COMPLETE; pitch = 1.0f; }
            case REJECT -> { event = SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(); pitch = 0.8f; }
            default -> { return; }
        }
        world.playSound(null, pos, event, SoundCategory.BLOCKS, 1.0f, pitch);
    }
}
