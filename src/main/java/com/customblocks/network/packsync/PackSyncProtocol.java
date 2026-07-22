/**
 * PackSyncProtocol.java — GROUP 05 §G (bounded, resumable, verified dedicated sync). SHARED server+client.
 *
 * Single home for every HARD BOUND and the two SAFETY primitives the dedicated pack sync relies on, so
 * server ({@link PackSyncService}) and client ({@link com.customblocks.client.packsync.ClientPackReceiver})
 * enforce the exact same numbers and can never drift:
 *
 *   • flow control — {@link #INITIAL_CREDIT} starting send window, replenished by client acks, so the
 *     server's outstanding (sent-but-unacked) bytes stay bounded and never exceed {@link #MAX_INFLIGHT};
 *   • allocation guards (§G8) — caps on file count, path length, chunk size, per-file size, whole-pack
 *     size, and the compressed / inflated manifest blob, all checked BEFORE any large allocation or
 *     decompression so a hostile or corrupt peer can neither OOM us nor zip-bomb us;
 *   • path safety (§G9) — {@link #safeRelPath} + {@link #resolveInside} reject absolute, traversal, and
 *     normalisation-escape paths so nothing can ever be written outside {@code resourcepacks/CustomBlocks};
 *   • disk preflight (§G14) — {@link #DISK_MARGIN} headroom the client requires before a bulk transfer.
 *
 * Pure constants + static helpers, no state. Never throws from a validator (returns false / null); the
 * bounded inflate throws a checked-style {@link IllegalStateException} the caller catches and treats as a
 * rejected message.
 *
 * Depends on: nothing (JDK only).
 * Called by:  PackSyncService, ClientPackReceiver, PackManifest.
 */
package com.customblocks.network.packsync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.zip.GZIPInputStream;

public final class PackSyncProtocol {

    private PackSyncProtocol() {} // static-only

    /** Bumped when the wire shape changes; lets a future §I handshake refuse an incompatible client. */
    public static final int PROTOCOL_VERSION = 2;

    // ── flow control (§G1/§G4) ────────────────────────────────────────────────────
    /** One PackFilePayload carries at most this many bytes (well under the custom-payload cap). */
    public static final int CHUNK_BYTES = 128 * 1024;
    /** Server's starting send credit — it may put this many bytes on the wire before the first ack. */
    public static final int INITIAL_CREDIT = 256 * 1024;
    /** Hard ceiling on outstanding (sent-but-unacked) bytes; credit is capped here so in-flight can't exceed it. */
    public static final int MAX_INFLIGHT = 1024 * 1024;
    /** Max bytes streamed to ONE player in ONE server tick (coarse rate cap layered on top of credit). */
    public static final int BUDGET_PER_TICK = 256 * 1024;

    // ── allocation guards (§G8) ───────────────────────────────────────────────────
    /** Reject an incoming chunk whose payload exceeds this (a peer inflating {@code count}/size). */
    public static final int MAX_CHUNK_BYTES = CHUNK_BYTES;
    /** No single pack file may exceed this — declared size over it is rejected before any write. */
    public static final long MAX_FILE_BYTES = 64L * 1024 * 1024;
    /** No manifest / request may name more paths than this. */
    public static final int MAX_FILES = 200_000;
    /** No pack-relative path may be longer than this. */
    public static final int MAX_PATH_LEN = 512;
    /** Whole selected pack may not declare more than this many bytes (disk-preflight ceiling). */
    public static final long MAX_TOTAL_BYTES = 512L * 1024 * 1024;
    /** A compressed manifest / request blob larger than this is rejected before inflating. */
    public static final int MAX_GZ_BYTES = 16 * 1024 * 1024;
    /** Bounded inflate stops here — guards against a zip-bomb manifest. */
    public static final int MAX_INFLATED_BYTES = 64 * 1024 * 1024;

    // ── retry / disk (§G7/§G14) ───────────────────────────────────────────────────
    /** How many times the client re-requests a file that failed SHA-256 before giving up. */
    public static final int MAX_FILE_RETRIES = 3;
    /** Free space the client demands ON TOP of the needed bytes before starting a bulk transfer. */
    public static final long DISK_MARGIN = 32L * 1024 * 1024;

    /**
     * True when {@code rel} is a safe pack-relative path: non-empty, within the length cap, forward-slash
     * only, not absolute, no drive letter, and no empty / {@code .} / {@code ..} segment. Rejects control
     * chars. This is the first gate; {@link #resolveInside} additionally proves the resolved file stays
     * under the pack root.
     */
    public static boolean safeRelPath(String rel) {
        if (rel == null) return false;
        int n = rel.length();
        if (n == 0 || n > MAX_PATH_LEN) return false;
        if (rel.indexOf('\\') >= 0) return false;         // backslash — could escape on some FS
        if (rel.charAt(0) == '/') return false;           // absolute
        if (rel.indexOf(':') >= 0) return false;          // drive letter / scheme
        for (int i = 0; i < n; i++) if (rel.charAt(i) < 0x20) return false; // control char
        for (String seg : rel.split("/", -1)) {
            if (seg.isEmpty() || seg.equals(".") || seg.equals("..")) return false;
        }
        return true;
    }

    /**
     * Resolve {@code rel} under {@code root} and return the target file ONLY if it is provably inside the
     * root's canonical tree (§G9); otherwise null. Combines {@link #safeRelPath} with a canonical-prefix
     * check so a symlink or clever normalisation can't escape. Never throws.
     */
    public static File resolveInside(File root, String rel) {
        if (root == null || !safeRelPath(rel)) return null;
        try {
            File dest = new File(root, rel);
            String base = root.getCanonicalPath() + File.separator;
            String got = dest.getCanonicalPath();
            return (got.equals(root.getCanonicalPath()) || got.startsWith(base)) ? dest : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * GZIP-inflate {@code gz} into raw bytes, refusing the input if it is bigger than {@link #MAX_GZ_BYTES}
     * and aborting mid-inflate if the output would exceed {@code maxOut} (a zip-bomb guard, §G8). Throws
     * {@link IllegalStateException} on any limit breach or decode error — callers catch it and reject the
     * message without having allocated an unbounded buffer.
     */
    public static byte[] gunzipBounded(byte[] gz, int maxOut) {
        if (gz == null || gz.length == 0) return new byte[0];
        if (gz.length > MAX_GZ_BYTES) throw new IllegalStateException("compressed blob too large: " + gz.length);
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxOut, 1 << 16));
        byte[] buf = new byte[8192];
        int total = 0;
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
            int r;
            while ((r = in.read(buf)) > 0) {
                total += r;
                if (total > maxOut) throw new IllegalStateException("inflated blob exceeds " + maxOut);
                out.write(buf, 0, r);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("gunzip failed", e);
        }
        return out.toByteArray();
    }
}
