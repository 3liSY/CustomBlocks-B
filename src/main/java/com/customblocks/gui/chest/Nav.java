/**
 * Nav.java
 *
 * Server-side, per-player navigation back-stack for the chest GUIs (Group 02, G02.6).
 * Each entry is a serializable MenuKey describing how to rebuild a menu, so the stack
 * survives the brief close/reopen that openHandledScreen performs between screens.
 */
package com.customblocks.gui.chest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Nav {

    private Nav() {} // static-only

    /** Every chest-menu destination the router can rebuild from a key. */
    public enum Dest { MAIN, BLOCK_LIST, EDITOR, REID, UNDO, REDO, HISTORY, MAGIC, MAGIC_EDIT, CONFIG, TEXTURE_SIZE, RETEXTURE_CONFIRM, RECOLOR_CONFIRM, HEX_RECOLOR_CONFIRM, HEX_COLORS, CUSTOM_COLOR, DIAG, SEARCH, HELP, OMNI, BULK_HUB, BULK_SELECT, BULK_ACTION, SHAPE_EDITOR, FACE_EDITOR, BACKUP_LIST, BACKUP_CONFIRM, CONFIG_CONFIRM, TRASH_LIST, TRASH_ENTRY, BROKEN_LIST, BROKEN_CONFIRM, SAFETY, BGSTUDIO, COLOR_VARIANTS, COLORS, PALETTE_LIST, GRADIENT_PICKER, COLOR_PICK, CATEGORY_LIST, CATEGORY_BROWSE, CATEGORY_BLOCK, CATEGORY_EDIT, CATEGORY_DELETE_CONFIRM, EXPORT_DASHBOARD, ARABIC, ARABIC_LIST, ARABIC_GROUP, ARABIC_CHOICE, ARABIC_COLOR, BULK_CONFIRM, ANIM_LIST }

    /** A rebuildable pointer to one menu: destination + optional argument + page index. */
    public record MenuKey(Dest dest, String arg, int page) {
        public static MenuKey of(Dest dest) { return new MenuKey(dest, "", 0); }
        public static MenuKey of(Dest dest, String arg) { return new MenuKey(dest, arg == null ? "" : arg, 0); }
        public MenuKey withPage(int p) { return new MenuKey(dest, arg, Math.max(0, p)); }
    }

    private static final Map<UUID, Deque<MenuKey>> STACKS = new ConcurrentHashMap<>();

    private static Deque<MenuKey> stack(UUID u) {
        return STACKS.computeIfAbsent(u, k -> new ArrayDeque<>());
    }

    /** Clear the stack and start a fresh session at the given root key. */
    public static synchronized void reset(UUID u, MenuKey root) {
        Deque<MenuKey> s = stack(u);
        s.clear();
        s.push(root);
    }

    /** Push a new menu as the current (top) screen. */
    public static synchronized void push(UUID u, MenuKey key) { stack(u).push(key); }

    /** Replace the current top (used when only the page changed). */
    public static synchronized void replaceTop(UUID u, MenuKey key) {
        Deque<MenuKey> s = stack(u);
        if (!s.isEmpty()) s.pop();
        s.push(key);
    }

    /** The current (top) menu, or null if the stack is empty. */
    public static synchronized MenuKey current(UUID u) { return stack(u).peek(); }

    /** Pop the current menu and return the one beneath it (now current), or null. */
    public static synchronized MenuKey popToPrevious(UUID u) {
        Deque<MenuKey> s = stack(u);
        if (!s.isEmpty()) s.pop();
        return s.peek();
    }

    /** Forget a player's navigation (safe to call on disconnect). */
    public static synchronized void clear(UUID u) { STACKS.remove(u); }
}
