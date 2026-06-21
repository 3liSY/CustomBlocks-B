/**
 * DiagnosticsCommands.java
 *
 * Responsibility: /cb diag (system snapshot / IT Chest), /cb incidents (incident log),
 * /cb audit [player] (mutation log, optionally filtered), /cb cache (cache readout).
 * Stays under 400 lines (§9.3); heavy lifting lives in DiagnosticsHelper / MutationLog.
 *
 * Depends on: DiagnosticsHelper, IncidentRecorder, MutationLog, ResourcePackServer, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.DiagReport;
import com.customblocks.core.DiagnosticsHelper;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.MutationLog;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class DiagnosticsCommands {

    private DiagnosticsCommands() {}

    private static final SimpleDateFormat FMT = new SimpleDateFormat("MMM d, HH:mm");

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("diag")
                .executes(DiagnosticsCommands::diag));

        root.then(CommandManager.literal("incidents")
                .executes(DiagnosticsCommands::incidents)
                .then(CommandManager.literal("clear")
                        .executes(DiagnosticsCommands::clearIncidents)));

        root.then(CommandManager.literal("audit")
                .executes(ctx -> audit(ctx, null))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(PLAYER_NAMES)
                        .executes(ctx -> audit(ctx, StringArgumentType.getString(ctx, "player")))));

        root.then(CommandManager.literal("cache")
                .executes(DiagnosticsCommands::cache)
                .then(CommandManager.literal("clear")
                        .executes(DiagnosticsCommands::cacheClear)));

        root.then(CommandManager.literal("report")
                .executes(DiagnosticsCommands::reportEntry)
                .then(CommandManager.literal("generate")
                        .executes(DiagnosticsCommands::reportGenerate))
                .then(CommandManager.literal("link")
                        .executes(DiagnosticsCommands::reportLink)));
    }

    /** Tab-complete currently-online player names (for /cb audit <player>). */
    private static final SuggestionProvider<ServerCommandSource> PLAYER_NAMES = (ctx, b) -> {
        MinecraftServer s = ctx.getSource().getServer();
        if (s != null) {
            return CommandSource.suggestMatching(java.util.Arrays.asList(s.getPlayerManager().getPlayerNames()), b);
        }
        return b.buildFuture();
    };

    private static int diag(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.DIAG));
            return 1;
        }
        List<String> lines = DiagnosticsHelper.collect(src.getServer());
        for (String line : lines) src.sendFeedback(() -> Text.literal(line), false);
        return 1;
    }

    private static int incidents(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        // Players get the IT Chest (incidents are rows 2–4); console keeps the text log.
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.DIAG));
            return 1;
        }
        List<String> lines = IncidentRecorder.list();
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§eIncident log:"), false);
        for (String line : lines) src.sendFeedback(() -> Text.literal(line), false);
        return 1;
    }

    private static int clearIncidents(CommandContext<ServerCommandSource> ctx) {
        IncidentRecorder.clear();
        Chat.success(ctx.getSource(), "Incident log cleared.");
        return 1;
    }

    // ── /cb audit [player] — players get the GUI; console keeps the text log ──────────────────
    private static int audit(CommandContext<ServerCommandSource> ctx, String filter) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.AUDIT, filter));
            return 1;
        }
        MinecraftServer server = src.getServer();
        List<MutationLog.Entry> all = MutationLog.recent();
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§eMutation log"
                + (filter == null ? "" : " §7for §f" + filter) + "§e:"), false);
        int shown = 0;
        for (MutationLog.Entry e : all) {
            String who = name(server, e.actor());
            if (filter != null && !who.equalsIgnoreCase(filter)) continue;
            String line = "§7[" + FMT.format(new Date(e.time())) + "] §f" + e.action()
                    + " §7" + e.blockId() + " §8by " + who;
            src.sendFeedback(() -> Text.literal(line), false);
            if (++shown >= 50) break; // cap chat spam; full history lives in the GUI
        }
        if (shown == 0) src.sendFeedback(() -> Text.literal("§7No matching entries."), false);
        return 1;
    }

    // ── /cb cache — read-only cache + pack readout (no clearing here) ─────────────────────────
    private static int cache(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§eCache & pack:"), false);

        long[] tex  = dirStats(Path.of("config/customblocks/textures"));
        long[] srcF = dirStats(Path.of("config/customblocks/sources"));
        long[] exp  = dirStats(Path.of("config/customblocks/cloud_exports"));
        line(src, "§fLive textures: §e" + tex[0] + " file(s), " + bytes(tex[1]) + " §8(never cleared)");
        line(src, "§fSaved sources: §e" + srcF[0] + " file(s), " + bytes(srcF[1]));
        line(src, "§fExports/temp: §e" + exp[0] + " file(s), " + bytes(exp[1]) + " §8(cleared by /cb cache clear)");

        File pack = ResourcePackServer.getPackFile();
        if (pack != null && pack.exists()) {
            line(src, "§fResource pack: §e" + bytes(pack.length()) + " §7· built " + FMT.format(new Date(pack.lastModified())));
        } else {
            line(src, "§fResource pack: §cnot built yet");
        }
        line(src, "§fPending rebuild: " + (ResourcePackServer.isRebuilding() ? "§eyes" : "§ano"));
        return 1;
    }

    // ── /cb cache clear — remove disposable artifacts only (owner-approved scope) ─────────────
    // Deletes cloud_exports/* + stray *.tmp. Live textures, saved sources, the pack and backups
    // are never touched.
    private static int cacheClear(CommandContext<ServerCommandSource> ctx) {
        Path root = Path.of("config/customblocks");
        long[] a = deleteMatching(root.resolve("cloud_exports"), p -> true);
        long[] b = deleteMatching(root, p -> p.getFileName().toString().endsWith(".tmp"));
        long count = a[0] + b[0], freed = a[1] + b[1];
        Chat.success(ctx.getSource(), "Cache cleared — removed " + count + " file(s), freed " + bytes(freed)
                + ". Live textures, pack and backups untouched.");
        return 1;
    }

    // ── /cb report — players get the GUI; console writes + prints the link ────────────────────
    private static int reportEntry(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.REPORT));
            return 1;
        }
        return reportGenerate(ctx);
    }

    // ── /cb report generate — write the diagnostic report + post a [download] link ────────────
    private static int reportGenerate(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            Path p = DiagReport.write(src.getServer());
            Chat.success(src, "Report written: §f" + p.toAbsolutePath());
            sendDownloadLink(src);
        } catch (Exception e) {
            Chat.error(src, "Couldn't write the report: " + e.getMessage());
            IncidentRecorder.record("Diag report write failed", null, src.getName(), e);
        }
        return 1;
    }

    // ── /cb report link — re-post the link for an already-written report (no rewrite) ─────────
    private static int reportLink(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Path f = Path.of("config/customblocks/data", "diag_report.txt");
        if (!Files.exists(f)) {
            Chat.error(src, "No report yet — run §f/cb report generate §cfirst.");
            return 1;
        }
        Chat.success(src, "Report: §f" + f.toAbsolutePath());
        sendDownloadLink(src);
        return 1;
    }

    /** Post the clickable [download] link for the diagnostic report. */
    private static void sendDownloadLink(ServerCommandSource src) {
        String url = ResourcePackServer.getReportUrl();
        src.sendFeedback(() -> Text.literal("  ")
                .append(Text.literal("§b[download]").styled(st -> st
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(url))))), false);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────
    /** Delete every regular file under {@code dir} matching {@code match}; returns [count, bytes]. */
    private static long[] deleteMatching(Path dir, Predicate<Path> match) {
        if (!Files.isDirectory(dir)) return new long[]{0, 0};
        long count = 0, total = 0;
        try (Stream<Path> s = Files.walk(dir)) {
            List<Path> files = s.filter(Files::isRegularFile).filter(match).toList();
            for (Path p : files) {
                long len = p.toFile().length();
                try { Files.deleteIfExists(p); count++; total += len; } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return new long[]{count, total};
    }

    private static void line(ServerCommandSource src, String s) {
        src.sendFeedback(() -> Text.literal(s), false);
    }

    /** [fileCount, totalBytes] for a directory tree, or [0,0] if it doesn't exist. */
    private static long[] dirStats(Path dir) {
        if (!Files.isDirectory(dir)) return new long[]{0, 0};
        long count = 0, total = 0;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (Files.isRegularFile(p)) { count++; total += p.toFile().length(); }
            }
        } catch (Exception ignored) {}
        return new long[]{count, total};
    }

    private static String bytes(long b) {
        if (b <= 0) return "0 B";
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return (b / 1024) + " KB";
        return String.format("%.1f MB", b / (1024.0 * 1024));
    }

    /** Resolve a stored actor (UUID string or "console") to a display name. */
    private static String name(MinecraftServer server, String actor) {
        if (actor == null || actor.equals("console")) return "Console";
        try {
            UUID u = UUID.fromString(actor);
            if (server != null) {
                ServerPlayerEntity sp = server.getPlayerManager().getPlayer(u);
                if (sp != null) return sp.getName().getString();
            }
            return actor.substring(0, 8);
        } catch (IllegalArgumentException ex) {
            return actor;
        }
    }
}
