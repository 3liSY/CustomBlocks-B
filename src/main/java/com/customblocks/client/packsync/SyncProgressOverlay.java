/**
 * SyncProgressOverlay.java — Group 05 §E12. CLIENT-ONLY.
 *
 * The on-screen readout for the dedicated-server pack sync (§G in-band transport). On a bad connection the
 * first join can stream for minutes; without this the player sees a normal-looking world with missing/half
 * textures and no sign anything is happening — indistinguishable from a frozen join. This draws a small
 * top-center panel while {@link ClientPackReceiver} has a transfer in flight, showing the honest state:
 * received / total bytes, percent, current rate, ETA, retry round, and the resumed offset (bytes already on
 * disk from a prior interrupted session that we did NOT re-download — §G's `.part`/commit checkpoint). It
 * flips to "Applying…" during the reload and a brief lime "Textures updated" on success, then hides itself.
 *
 * Purely a VIEW over counters {@link ClientPackReceiver} pushes in — it owns no transport logic and never
 * touches disk or the network, so it cannot affect the §G/§I path it reports on. All state lives on the
 * client main thread (receivers hop there via {@code client.execute}, and the HUD render runs there too).
 * Cancel is the ordinary Esc → Disconnect, which fires {@code ClientPackReceiver.reset()} and cleanly closes
 * the open `.part` + session; this overlay simply hides on reset — it does not look like a frozen join.
 *
 * Depends on: DrawContext, CbTheme (locked red+black palette), Util (wall-clock).
 * Called by:  ClientPackReceiver (state pushes), HudRenderMixin (render), CustomBlocksClient (hide on disconnect).
 */
package com.customblocks.client.packsync;

import com.customblocks.client.gui.CbTheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class SyncProgressOverlay {

    private SyncProgressOverlay() {}

    /**
     * IMPORTING is Group 12's addition (§B rule 18): the owner chose to REUSE this panel for a folder-import
     * run rather than grow a second progress look. It is a sibling phase, not a change to the pack-sync
     * ones — it counts FILES instead of bytes, so it fills the bar from {@link #importDone}/{@link #importTotal}
     * and writes its own stats line, and it never touches the transport counters above it.
     */
    private enum Phase { IDLE, SYNCING, APPLYING, DONE, ERROR, IMPORTING }

    private static final int PANEL_W = 224;
    private static final int PANEL_H = 42;
    private static final int TOP_MARGIN = 6;
    /** How long the terminal DONE / ERROR panel lingers before hiding itself. */
    private static final long DONE_LINGER_MS = 3_000L;
    private static final long ERROR_LINGER_MS = 7_000L;

    private static Phase phase = Phase.IDLE;
    private static long totalBytes;      // bytes this sync must transfer (missing/changed only)
    private static long receivedBytes;   // bytes streamed so far this sync
    private static long resumedBytes;    // already-present bytes we skipped (the resumed offset)
    private static int retryRound;       // >0 once a verification retry round has started
    private static long rateEmaBps;      // smoothed transfer rate
    private static long lastSampleMs;
    private static long lastSampleBytes;
    private static long lingerUntilMs;   // wall-clock at which a DONE/ERROR panel hides
    private static String message = "";  // terminal reason line (DONE/ERROR)
    private static int importDone;        // G12: files finished this import run
    private static int importTotal;       // G12: files this import run will handle

    // ── state pushed in by ClientPackReceiver (client main thread) ─────────────────

    /** A real transfer is starting: {@code total} bytes to fetch, {@code resumed} already on disk. */
    public static void beginSync(long total, long resumed) {
        phase = Phase.SYNCING;
        totalBytes = Math.max(0, total);
        receivedBytes = 0;
        resumedBytes = Math.max(0, resumed);
        retryRound = 0;
        rateEmaBps = 0;
        lastSampleMs = Util.getMeasuringTimeMs();
        lastSampleBytes = 0;
        message = "";
    }

    /** One accepted chunk landed — advance the byte counter and re-estimate the rate. */
    public static void onChunk(int bytes) {
        if (phase != Phase.SYNCING || bytes <= 0) return;
        receivedBytes += bytes;
        long now = Util.getMeasuringTimeMs();
        long dt = now - lastSampleMs;
        if (dt >= 250) { // resample ~4×/s so the rate reads steady, not jumpy
            long inst = (receivedBytes - lastSampleBytes) * 1000L / Math.max(1, dt);
            rateEmaBps = rateEmaBps == 0 ? inst : (long) (rateEmaBps * 0.7 + inst * 0.3);
            lastSampleMs = now;
            lastSampleBytes = receivedBytes;
        }
    }

    /** A verification retry round began (a file failed SHA and is being re-fetched) — §G7. */
    public static void onRetryRound(int round) {
        if (phase == Phase.SYNCING) retryRound = Math.max(retryRound, round);
    }

    /** All bytes in — the one silent resource reload is running. */
    public static void applying() {
        if (phase == Phase.SYNCING) phase = Phase.APPLYING;
    }

    /** Sync finished and the pack applied — show a brief success, then auto-hide. No-op for a silent
     *  up-to-date join where no panel was ever shown (only fires from an active SYNCING/APPLYING panel). */
    public static void done() {
        if (phase != Phase.SYNCING && phase != Phase.APPLYING) return;
        phase = Phase.DONE;
        message = "Textures updated";
        lingerUntilMs = Util.getMeasuringTimeMs() + DONE_LINGER_MS;
    }

    /** Sync stopped for a real reason (kept the previous pack) — show it, then auto-hide. */
    public static void error(String reason) {
        phase = Phase.ERROR;
        message = reason == null ? "Texture sync failed" : reason;
        lingerUntilMs = Util.getMeasuringTimeMs() + ERROR_LINGER_MS;
    }

    /** Disconnect / session reset — drop the panel immediately (a lost connection shows the vanilla screen). */
    public static void hide() {
        phase = Phase.IDLE;
    }

    // ── state pushed in by a Group 12 folder-import run (client main thread) ───────

    /**
     * A folder import is reporting itself (G12 §B rule 18). {@code total <= 0} means the run is over →
     * a short closing panel, then it hides.
     *
     * Deliberately yields to a live pack sync: textures streaming in is the one thing the player cannot
     * do without, so an import never steals the panel from it.
     */
    public static void importProgress(int done, int total, String label) {
        if (phase == Phase.SYNCING || phase == Phase.APPLYING) return; // a real transfer owns the panel
        if (total <= 0) {
            if (phase == Phase.IMPORTING) phase = Phase.IDLE;
            return;
        }
        importTotal = total;
        importDone = Math.max(0, Math.min(done, total));
        message = label == null ? "" : label;
        if (importDone >= importTotal) {           // finished — linger briefly like a completed sync
            phase = Phase.DONE;
            lingerUntilMs = Util.getMeasuringTimeMs() + DONE_LINGER_MS;
        } else {
            phase = Phase.IMPORTING;
        }
    }

    // ── render (HUD main thread) ───────────────────────────────────────────────────

    public static void render(DrawContext ctx) {
        if (phase == Phase.IDLE) return;
        if ((phase == Phase.DONE || phase == Phase.ERROR) && Util.getMeasuringTimeMs() > lingerUntilMs) {
            phase = Phase.IDLE;
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

        int x = (ctx.getScaledWindowWidth() - PANEL_W) / 2;
        int y = TOP_MARGIN;
        int edge = phase == Phase.DONE ? CbTheme.LIME : phase == Phase.ERROR ? CbTheme.ACCENT : CbTheme.ACCENT;

        // Black card + accent frame + thicker left edge (matches CbToast's locked look).
        ctx.fill(x, y, x + PANEL_W, y + PANEL_H, CbTheme.PANEL_BG);
        ctx.fill(x, y, x + PANEL_W, y + 1, edge);
        ctx.fill(x, y + PANEL_H - 1, x + PANEL_W, y + PANEL_H, edge);
        ctx.fill(x + PANEL_W - 1, y, x + PANEL_W, y + PANEL_H, edge);
        ctx.fill(x, y, x + 2, y + PANEL_H, edge);

        // Title: bold red "CustomBlocks" + white phase word.
        ctx.drawText(mc.textRenderer, CbTheme.red("CustomBlocks"), x + 7, y + 6, CbTheme.ACCENT, false);
        ctx.drawText(mc.textRenderer, Text.literal(titleWord()), x + 7 + mc.textRenderer.getWidth("CustomBlocks") + 5,
                y + 6, CbTheme.TEXT, false);

        // Progress bar.
        int barX = x + 7, barY = y + 18, barW = PANEL_W - 14, barH = 5;
        ctx.fill(barX, barY, barX + barW, barY + barH, CbTheme.CARD);
        float frac = phase == Phase.DONE ? 1f
                : phase == Phase.IMPORTING ? (importTotal > 0 ? importDone / (float) importTotal : 0f)
                : totalBytes > 0 ? Math.min(1f, receivedBytes / (float) totalBytes)
                : phase == Phase.APPLYING ? 1f : 0f;
        int fillW = (int) (barW * frac);
        if (fillW > 0) ctx.fill(barX, barY, barX + fillW, barY + barH, phase == Phase.DONE ? CbTheme.LIME : CbTheme.ACCENT);

        // Stats / message line.
        ctx.drawText(mc.textRenderer, Text.literal(statsLine()), x + 7, y + 29, CbTheme.TEXT_DIM, false);
    }

    private static String titleWord() {
        return switch (phase) {
            case APPLYING -> "· applying";
            case DONE -> "· done";
            case ERROR -> "· problem";
            case IMPORTING -> "· importing";
            default -> "· syncing textures";
        };
    }

    private static String statsLine() {
        if (phase == Phase.DONE || phase == Phase.ERROR) return message;
        if (phase == Phase.IMPORTING) {
            int pct = importTotal > 0 ? (100 * importDone / importTotal) : 0;
            return importDone + " / " + importTotal + "  " + pct + "%"
                    + (message.isEmpty() ? "" : "  " + message);
        }
        if (phase == Phase.APPLYING) return "Applying textures — one moment…";
        int pct = totalBytes > 0 ? (int) (100 * Math.min(1f, receivedBytes / (float) totalBytes)) : 0;
        StringBuilder sb = new StringBuilder();
        sb.append(mb(receivedBytes)).append(" / ").append(mb(totalBytes))
                .append("  ").append(pct).append('%');
        if (rateEmaBps > 0) {
            sb.append("  ").append(mb(rateEmaBps)).append("/s");
            long remain = totalBytes - receivedBytes;
            if (remain > 0) sb.append("  ETA ").append(secs(remain / Math.max(1, rateEmaBps)));
        }
        if (resumedBytes > 0) sb.append("  +").append(mb(resumedBytes)).append(" resumed");
        if (retryRound > 0) sb.append("  retry ").append(retryRound);
        return sb.toString();
    }

    private static String mb(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return (b / 1024) + " KB";
        return String.format("%.1f MB", b / (1024.0 * 1024));
    }

    private static String secs(long s) {
        if (s < 60) return s + "s";
        return (s / 60) + "m" + String.format("%02ds", s % 60);
    }
}
