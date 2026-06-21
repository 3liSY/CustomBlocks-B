/**
 * DebugLog.java
 *
 * Reads the server's running log file ({@code logs/latest.log}, relative to the run dir like every
 * other path in the mod) and returns the mod's own lines — those carrying the manual
 * {@code [CustomBlocks]} prefix — newest first, capped. Backs the IT Chest "Debug Log" viewer
 * (Group 16, later slice): a read-only, in-game window onto what the mod logged this session, so
 * the owner doesn't have to alt-tab to a text file.
 *
 * Depends on: nothing (plain file read)
 * Called by:  DebugLogMenu
 */
package com.customblocks.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DebugLog {

    private DebugLog() {} // static-only

    /** The marker the mod stamps on every one of its log lines (see CustomBlocksMod.LOGGER usage). */
    private static final String MARKER = "[CustomBlocks]";
    /** Most lines to keep (newest). Filtered to mod lines only, so this is plenty for a session. */
    private static final int MAX = 200;

    public enum Level { ERROR, WARN, INFO }

    /** One mod log line: its severity (parsed from the vanilla {@code .../LEVEL]} field) + raw text. */
    public record Line(Level level, String raw) {

        /** The message after the {@code [CustomBlocks]} marker (timestamp/thread stripped). */
        public String message() {
            int i = raw.indexOf(MARKER);
            return i < 0 ? raw : raw.substring(i + MARKER.length()).trim();
        }
    }

    /** Viewer severity filter, carried in the menu's MenuKey arg. Cycled by the footer button. */
    public enum Filter {
        ALL("all", "All"), ISSUES("issues", "Issues only"),
        ERRORS("error", "Errors"), WARNS("warn", "Warnings"), INFOS("info", "Info");

        public final String token;
        public final String label;
        Filter(String token, String label) { this.token = token; this.label = label; }

        /** The next filter in the cycle (wraps), for the one-button cycle control. */
        public Filter next() { Filter[] v = values(); return v[(ordinal() + 1) % v.length]; }

        /** Resolve a stored arg token back to a filter (unknown / "" → {@link #ALL}). */
        public static Filter of(String token) {
            if (token != null) for (Filter f : values()) if (f.token.equals(token)) return f;
            return ALL;
        }

        /** Whether a line of {@code lv} severity is shown under this filter. */
        public boolean accepts(Level lv) {
            return switch (this) {
                case ALL    -> true;
                case ISSUES -> lv == Level.ERROR || lv == Level.WARN;
                case ERRORS -> lv == Level.ERROR;
                case WARNS  -> lv == Level.WARN;
                case INFOS  -> lv == Level.INFO;
            };
        }
    }

    /** Per-severity tally over a line list (drives the summary tile + filter counts). */
    public record Counts(int error, int warn, int info) {
        public int total()  { return error + warn + info; }
        public int issues() { return error + warn; }
    }

    /** Count {@code lines} by severity. */
    public static Counts counts(List<Line> lines) {
        int e = 0, w = 0, i = 0;
        for (Line l : lines) switch (l.level()) { case ERROR -> e++; case WARN -> w++; case INFO -> i++; }
        return new Counts(e, w, i);
    }

    /** The mod's log lines from {@code logs/latest.log}, newest first, capped at {@link #MAX}. */
    public static List<Line> recent() {
        Path log = Path.of("logs", "latest.log");
        if (!Files.exists(log)) return List.of();
        List<Line> hits = new ArrayList<>();
        try {
            for (String l : Files.readAllLines(log, StandardCharsets.UTF_8)) {
                if (l.contains(MARKER)) hits.add(new Line(levelOf(l), l));
            }
        } catch (IOException e) {
            return hits; // partial / empty is fine for a best-effort viewer
        }
        Collections.reverse(hits); // newest first
        return hits.size() > MAX ? new ArrayList<>(hits.subList(0, MAX)) : hits;
    }

    /** Vanilla log lines look like {@code [HH:MM:SS] [Server thread/INFO] ...} — read that LEVEL. */
    private static Level levelOf(String line) {
        if (line.contains("/ERROR]") || line.contains("/FATAL]")) return Level.ERROR;
        if (line.contains("/WARN]")) return Level.WARN;
        return Level.INFO;
    }
}
