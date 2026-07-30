/**
 * ImportEntry.java — one candidate file in a folder-import run (Group 12 §B).
 *
 * The scan turns every file sitting in the import folder into exactly one of these, and the preview is
 * a rendering of the list. Nothing here touches the world: an entry is a DESCRIPTION of what would
 * happen, which is what makes "preview, then commit" (rule 2) possible — cancel simply drops the list.
 *
 * A problem entry ({@link #problem} non-null) is never created. It keeps its file name and reason so the
 * preview can offer the three fixes the owner asked for — rename, delete the file, ignore (rule 3) —
 * against a specific file rather than a vague "some files failed".
 *
 * Depends on: (nothing — a plain record)
 * Called by:  ImportScan (builds them), ImportService (commits them), ImportChat (renders them)
 */
package com.customblocks.core;

import java.nio.file.Path;

/**
 * @param image    the picture file to bake as the texture, or null for a data-file-only entry.
 * @param data     the block .json sitting beside it (B17: same-name pair), or null for an image-only entry.
 * @param id       the block id this file would claim — {@code /cb create} rules, derived from the file name.
 * @param name     the display name, capital on every word (rule 5).
 * @param problem  why this file cannot be imported as-is, or null when it is ready.
 * @param animated true when the picture is a moving GIF/WebP — parked for its own session (rule 13).
 */
public record ImportEntry(Path image, Path data, String id, String name, String problem, boolean animated) {

    /** True when nothing is wrong with this entry and a block would be created for it. */
    public boolean ready() {
        return problem == null;
    }

    /** The file name shown in the preview — the picture's if there is one, else the data file's. */
    public String fileName() {
        Path p = image != null ? image : data;
        return p == null ? id : p.getFileName().toString();
    }

    /** A ready entry re-pointed at a new id + name (what the anvil rename produces). */
    public ImportEntry withId(String newId, String newName) {
        return new ImportEntry(image, data, newId, newName, null, animated);
    }

    /** The same entry marked unusable for {@code reason}. */
    public ImportEntry withProblem(String reason) {
        return new ImportEntry(image, data, id, name, reason, animated);
    }
}
