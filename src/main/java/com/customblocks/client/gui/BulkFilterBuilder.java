/**
 * BulkFilterBuilder.java — Group 07 (Bulk Operations Hub — Console tab). CLIENT-ONLY.
 *
 * The multi-condition filter state (§G07-4 AND/OR/NOT), split out of {@link BulkWorkbenchScreen} to keep it
 * under the ≤500-line cap. Owns the parallel condition lists and every edit to them; the Screen holds one of
 * these and the views read it via {@link BulkWorkbenchScreen#fb()}. Index 0 is the base condition;
 * {@code conn.get(i)} is the connector (AND/OR) BEFORE condition i+1, so it has one fewer entry than the rest.
 *
 * Each mutator plays its click sound and then calls {@code onChange} — argument 1 means "the set of value
 * fields changed, rebuild the widgets", 0 means "state-only, just refresh" — so the Screen resets its escalate
 * flag and rebuilds exactly when needed without this class importing Screen internals.
 *
 * Depends on: BulkWorkbenchModel (Cond + eval), BulkOpSpec (cycle), CbUiSounds.
 * Called by:  BulkWorkbenchScreen (state + mutators), BulkOpsView (draw).
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

@Environment(EnvType.CLIENT)
final class BulkFilterBuilder {

    private final List<String> kind = new ArrayList<>(List.of("all"));
    private final List<String> val  = new ArrayList<>(List.of(""));
    private final List<Boolean> neg = new ArrayList<>(List.of(Boolean.FALSE));
    private final List<String> conn = new ArrayList<>();
    private final IntConsumer onChange;

    BulkFilterBuilder(IntConsumer onChange) { this.onChange = onChange; }

    // ── read ──────────────────────────────────────────────────────────────────
    int count() { return kind.size(); }
    String kind(int i) { return kind.get(i); }
    String val(int i) { return val.get(i); }
    boolean neg(int i) { return neg.get(i); }
    /** The AND/OR connector shown before condition {@code i} (i ≥ 1). */
    String conn(int i) { return i >= 1 && i - 1 < conn.size() ? conn.get(i - 1) : "AND"; }
    boolean needsValue(int i) { return BulkWorkbenchModel.kindNeedsValue(kind.get(i)); }

    List<BulkWorkbenchModel.Cond> conds() {
        List<BulkWorkbenchModel.Cond> out = new ArrayList<>();
        for (int i = 0; i < kind.size(); i++) out.add(new BulkWorkbenchModel.Cond(kind.get(i), val.get(i), neg.get(i)));
        return out;
    }
    List<String> conns() { return conn; }
    boolean anyActive() { return BulkWorkbenchModel.anyActive(conds()); }
    /** The BulkScope expression this filter sends on escalate, and its readable label on the targets line. */
    String expr() { return BulkWorkbenchModel.exprOf(conds(), conn); }

    // ── edit ──────────────────────────────────────────────────────────────────
    /** A live value field wrote a new value for condition {@code i} (no sound, no rebuild). */
    void setValue(int i, String v) { val.set(i, v); }

    /** Reset to a SINGLE condition — used by the NL command bar to drop a parsed filter in. Rebuilds widgets. */
    void setSingle(String k, String v, boolean negate) {
        kind.clear(); val.clear(); neg.clear(); conn.clear();
        kind.add(k == null || k.isBlank() ? "all" : k);
        val.add(v == null ? "" : v);
        neg.add(negate);
        onChange.accept(1);
    }

    void cycleKind(int i, int dir) {
        String k = BulkOpSpec.cycle(BulkWorkbenchModel.FILTER_KINDS, kind.get(i), dir);
        kind.set(i, k);
        val.set(i, (k.equals("favorite") || k.equals("locked")) ? "yes" : "");  // yes/no kinds start active
        CbUiSounds.click();
        onChange.accept(1);
    }
    void toggleNeg(int i) { neg.set(i, !neg.get(i)); CbUiSounds.tick(); onChange.accept(0); }
    void toggleVal(int i) { val.set(i, val.get(i).equalsIgnoreCase("no") ? "yes" : "no"); CbUiSounds.tick(); onChange.accept(0); }
    void toggleConn(int i) {
        int g = i - 1;
        if (g >= 0 && g < conn.size()) conn.set(g, conn.get(g).equals("AND") ? "OR" : "AND");
        CbUiSounds.click(); onChange.accept(0);
    }
    void add(String connector) {
        if (kind.size() >= BulkWorkbenchScreen.MAX_CONDS) return;
        kind.add("category"); val.add(""); neg.add(Boolean.FALSE); conn.add(connector);
        CbUiSounds.click(); onChange.accept(1);
    }
    void remove(int i) {
        if (i <= 0 || i >= kind.size()) return;
        kind.remove(i); val.remove(i); neg.remove(i); conn.remove(i - 1);
        CbUiSounds.click(); onChange.accept(1);
    }
}
