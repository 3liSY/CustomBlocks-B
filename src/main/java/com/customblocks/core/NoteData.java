/**
 * NoteData.java
 *
 * Responsibility: Immutable snapshot of one block's Lore (Group 18 REVAMP v2) — a list of short text
 * lines plus a single on/off switch:
 *   • lines   — the lore lines shown under the block item's name on hover (gray italic, & colours apply)
 *   • enabled — whether those lines are actually shown on the item (text stays saved when off)
 *
 * Replaces the old 3-field model (Lore string / To-Do list / Tooltip+enabled): To-Do and the separate
 * tooltip are gone — "Lore" now IS the hover text (vanilla's own word for it). Each line is capped at the
 * anvil limit ({@link #MAX_LINE_LEN}); the list is capped at {@link #MAX_LINES}.
 *
 * Like SlotData, every change returns a NEW snapshot via a with*()/op method — never mutated in place
 * (Bible §3 immutability). Stored/loaded by BlockNotesManager; rendered by NotesMenu and on the item.
 *
 * Depends on: (none)
 * Called by:  BlockNotesManager, NotesMenu, NoteCommands
 */
package com.customblocks.core;

import java.util.ArrayList;
import java.util.List;

public record NoteData(List<String> lines, boolean enabled) {

    /** Anvil rename-box cap (AnvilPrompt.MAX_LENGTH) — the longest a single line can be. */
    public static final int MAX_LINE_LEN = 50;
    /** How many lore lines a block may have (fits the menu's content grid). */
    public static final int MAX_LINES = 28;

    public static final NoteData EMPTY = new NoteData(List.of(), false);

    /** Canonical constructor: null-safe, clamps each line, caps the list — the record is always tidy. */
    public NoteData {
        List<String> clean = new ArrayList<>();
        if (lines != null) {
            for (String s : lines) {
                if (s == null) continue;
                clean.add(clamp(s));
                if (clean.size() >= MAX_LINES) break;
            }
        }
        lines = List.copyOf(clean);
    }

    /** True when nothing is set — BlockNotesManager drops these so notes.json stays clean. */
    public boolean isEmpty() {
        return lines.isEmpty() && !enabled;
    }

    public NoteData withEnabled(boolean on) {
        return new NoteData(lines, on);
    }

    /** Add one line (capped at MAX_LINES). Returns a new snapshot; unchanged if full or blank. */
    public NoteData addLine(String text) {
        if (text == null || text.isBlank() || lines.size() >= MAX_LINES) return this;
        List<String> list = new ArrayList<>(lines);
        list.add(clamp(text.trim()));
        return new NoteData(list, enabled);
    }

    /** Replace line #index. Blank text deletes it instead. No-op if out of range. */
    public NoteData editLine(int index, String text) {
        if (index < 0 || index >= lines.size()) return this;
        if (text == null || text.isBlank()) return removeLine(index);
        List<String> list = new ArrayList<>(lines);
        list.set(index, clamp(text.trim()));
        return new NoteData(list, enabled);
    }

    /** Remove line #index. No-op if out of range. */
    public NoteData removeLine(int index) {
        if (index < 0 || index >= lines.size()) return this;
        List<String> list = new ArrayList<>(lines);
        list.remove(index);
        return new NoteData(list, enabled);
    }

    private static String clamp(String v) {
        if (v == null) return "";
        return v.length() > MAX_LINE_LEN ? v.substring(0, MAX_LINE_LEN) : v;
    }
}
