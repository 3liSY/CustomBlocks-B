/**
 * DiagReport.java
 *
 * Responsibility: Build a plain-text diagnostic report (server info, full health snapshot,
 * the last 100 incidents and the last 50 mutation-log entries) and write it atomically to
 * config/customblocks/data/diag_report.txt for download (Group 16, slice 3). Read-only over
 * the live managers — it never mutates state. Minecraft "§x" colour codes are stripped so the
 * file is clean text.
 *
 * Depends on: DiagnosticsHelper, IncidentRecorder, MutationLog
 * Called by: DiagnosticsCommands (/cb report) and the IT Chest "Generate Report" button.
 */
package com.customblocks.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class DiagReport {

    private DiagReport() {}

    private static final Path FILE = Path.of("config/customblocks/data", "diag_report.txt");
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_MUTATIONS = 50;

    /** Build the report and write it atomically. Returns the file path. */
    public static Path write(MinecraftServer server) throws IOException {
        String text = build(server);
        Files.createDirectories(FILE.getParent());
        Path tmp = FILE.resolveSibling("diag_report.txt.tmp");
        Files.writeString(tmp, text, StandardCharsets.UTF_8);
        Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
        return FILE;
    }

    private static String build(MinecraftServer server) {
        StringBuilder b = new StringBuilder();
        b.append("=== CustomBlocks Diagnostic Report ===\n");
        b.append("Generated: ").append(Instant.now()).append('\n');
        if (server != null) {
            b.append("Server: ").append(server.getVersion())
             .append(" | players ").append(server.getCurrentPlayerCount())
             .append('/').append(server.getMaxPlayerCount()).append('\n');
        }
        b.append('\n');

        b.append("--- Health snapshot ---\n");
        for (String line : DiagnosticsHelper.collect(server)) b.append(strip(line)).append('\n');
        b.append('\n');
        gauge(b, "TPS",            DiagnosticsHelper.tps(server));
        gauge(b, "Block Registry", DiagnosticsHelper.registry());
        gauge(b, "Network Sync",   DiagnosticsHelper.networkSync(server));
        gauge(b, "Pack Status",    DiagnosticsHelper.packStatus());
        gauge(b, "Memory",         DiagnosticsHelper.memory());
        b.append('\n');

        List<IncidentRecorder.Incident> incidents = IncidentRecorder.recent();
        b.append("--- Incidents (").append(incidents.size()).append(") ---\n");
        if (incidents.isEmpty()) b.append("(none)\n");
        for (IncidentRecorder.Incident in : incidents) {
            String time = in.time().length() >= 19 ? in.time().substring(0, 19).replace('T', ' ') : in.time();
            b.append('[').append(time).append("] ").append(in.severity())
             .append(" | ").append(in.player())
             .append(" | ").append(in.block() == null ? "—" : in.block())
             .append(" | ").append(strip(in.context()));
            if (in.error() != null) b.append(" | ").append(in.error());
            if (in.url() != null)   b.append(" | url=").append(in.url());
            b.append('\n');
        }
        b.append('\n');

        List<MutationLog.Entry> muts = MutationLog.recent();
        int shown = Math.min(MAX_MUTATIONS, muts.size());
        b.append("--- Mutations (last ").append(shown).append(" of ").append(muts.size()).append(") ---\n");
        if (muts.isEmpty()) b.append("(none)\n");
        for (int i = 0; i < shown; i++) {
            MutationLog.Entry e = muts.get(i);
            b.append('[').append(FMT.format(new Date(e.time()))).append("] ")
             .append(e.action()).append(' ').append(e.blockId())
             .append(" by ").append(name(server, e.actor())).append('\n');
        }
        return b.toString();
    }

    private static void gauge(StringBuilder b, String label, DiagnosticsHelper.Gauge g) {
        b.append(label).append(" [").append(g.health()).append("]: ");
        b.append(String.join("; ", g.hover().stream().map(DiagReport::strip).filter(s -> !s.isEmpty()).toList()));
        b.append('\n');
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("§.", "");
    }

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
