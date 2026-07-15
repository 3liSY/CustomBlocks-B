/**
 * VersionCompare.java
 *
 * Responsibility: Pure semver-ish comparison for §K auto-update (GROUP_20 §CS3 AU2). Compares two
 * dot-separated version strings ("1.5.0" vs "1.3.0") the way a human expects: numeric parts compared
 * as numbers, missing trailing parts treated as 0, a leading "v" and any "+build"/"-suffix" tail
 * ignored. No dependency on Minecraft or Fabric — trivially unit-testable.
 *
 * Called by: UpdateController (client) to decide older / same / newer.
 */
package com.customblocks.update;

public final class VersionCompare {

    private VersionCompare() {} // static-only

    /** &lt;0 when {@code a} is older than {@code b}, 0 when equal, &gt;0 when {@code a} is newer. */
    public static int compare(String a, String b) {
        int[] pa = parse(a);
        int[] pb = parse(b);
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int va = i < pa.length ? pa[i] : 0;
            int vb = i < pb.length ? pb[i] : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    /** True when {@code client} is strictly older than {@code server} — the auto-update trigger. */
    public static boolean isOlder(String client, String server) {
        return compare(client, server) < 0;
    }

    /** Split "v1.5.0+1.21.1" → [1,5,0]. Strips a leading v/V and everything from the first '+' or '-';
     *  each remaining dot-part contributes its leading digits (non-numeric parts count as 0). */
    private static int[] parse(String v) {
        if (v == null) return new int[0];
        String s = v.trim();
        if (!s.isEmpty() && (s.charAt(0) == 'v' || s.charAt(0) == 'V')) s = s.substring(1);
        int cut = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '+' || c == '-') { cut = i; break; }
        }
        s = s.substring(0, cut);
        if (s.isEmpty()) return new int[0];
        String[] parts = s.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = leadingInt(parts[i]);
        return out;
    }

    /** Leading run of digits in a part as an int ("3a" → 3, "beta" → 0), overflow-safe. */
    private static int leadingInt(String part) {
        long val = 0;
        boolean any = false;
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (c < '0' || c > '9') break;
            any = true;
            val = val * 10 + (c - '0');
            if (val > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return any ? (int) val : 0;
    }
}
