/**
 * DefaultCategoryStore.java — the one "default" category (Group 27 §G27.6, Category tab).
 *
 * A single persisted category name that the Block Creation Studio pre-fills for a new block and
 * highlights in its Category tab. Kept tiny and separate from CategoryMetadataStore because it is a
 * global pointer, not per-category metadata. "" = no default.
 *
 * Persists to config/customblocks/data/default_category.txt via atomic write (NFR-13).
 *
 * Depends on: (none). Called by: CategoryAdminBridge (set/clear), HudSync (sync to client).
 */
package com.customblocks.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class DefaultCategoryStore {

    private static final String FILE = "config/customblocks/data/default_category.txt";
    private static volatile String value = load();

    private DefaultCategoryStore() {} // static-only

    /** The default category name, or "" when none is set. */
    public static String get() { return value; }

    /** Set (or, with blank, clear) the default category. */
    public static synchronized void set(String category) {
        value = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        save();
    }

    /** Clear the default if it points at {@code category} (used when that category is deleted/renamed). */
    public static synchronized void onCategoryGone(String category, String renamedTo) {
        String c = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        if (value.equals(c)) set(renamedTo == null ? "" : renamedTo);
    }

    private static String load() {
        try {
            Path p = Path.of(FILE);
            return Files.exists(p) ? Files.readString(p, StandardCharsets.UTF_8).trim() : "";
        } catch (Exception e) { return ""; }
    }

    private static void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("default_category.txt.tmp");
            Files.writeString(tmp, value, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
