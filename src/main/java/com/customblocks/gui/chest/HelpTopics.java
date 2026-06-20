/**
 * HelpTopics.java
 *
 * Responsibility: The static command-reference data behind the /cb help chest GUI
 * (Group 04) — every player-facing /cb command, grouped by category, with the syntax
 * to pre-fill and a short description. Also the single source of the known-subcommand
 * dictionary used by DidYouMean, so help and typo correction can never drift apart.
 *
 * Depends on: Minecraft Items (category icons)
 * Called by: HelpMenu (renders), DidYouMean (firstTokens)
 */
package com.customblocks.gui.chest;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class HelpTopics {

    private HelpTopics() {} // static-only

    /** One command: GUI label, command text to pre-fill in chat, one-line description. */
    public record Topic(String label, String suggest, String desc) {}

    /** One help category: name, icon, blurb, and its commands. */
    public record Category(String name, Item icon, String desc, List<Topic> topics) {}

    public static final List<Category> CATEGORIES = List.of(
            new Category("Blocks", Items.GRASS_BLOCK, "Create, copy and remove blocks", List.of(
                    new Topic("create", "/cb create <id> <name> <imageUrl>", "Make a new block from an image URL"),
                    new Topic("give", "/cb give <id>", "Put a block's item in your inventory"),
                    new Topic("list", "/cb list", "List every custom block"),
                    new Topic("search", "/cb search <query>", "Find blocks by id, name or category"),
                    new Topic("rename", "/cb rename <id> <new name>", "Change a block's display name"),
                    new Topic("dupe", "/cb dupe <id> <newId>", "Duplicate a block under a new id"),
                    new Topic("delete", "/cb delete <id>", "Delete a block (undoable)"))),

            new Category("Looks & Behavior", Items.PAINTING, "Texture and block properties", List.of(
                    new Topic("retexture", "/cb retexture <id> <imageUrl>", "Replace a block's texture from a URL"),
                    new Topic("setglow", "/cb setglow <id> <0-15>", "Make a block emit light"),
                    new Topic("sethardness", "/cb sethardness <id> <value>", "How long it takes to break (or unbreakable)"),
                    new Topic("setsound", "/cb setsound <id> <type>", "Step/break sound type"),
                    new Topic("setcollision", "/cb setcollision <id> <solid|passable>", "Solid block or walk-through"),
                    new Topic("video", "/cb video extract <url>", "Extract frames from a video"))),

            new Category("Organize", Items.BOOKSHELF, "Categories, notes, locks, favorites", List.of(
                    new Topic("setcategory", "/cb setcategory <id> <category>", "Put a block in a category"),
                    new Topic("categories", "/cb categories", "Browse blocks by category (chest GUI)"),
                    new Topic("category", "/cb category", "Category tools (rename, merge, delete, color, desc, icon…)"),
                    new Topic("category edit", "/cb category edit <category>", "Open the category editor GUI"),
                    new Topic("category rename", "/cb category rename <old> <new>", "Rename a category"),
                    new Topic("category merge", "/cb category merge <source> <target>", "Merge two categories"),
                    new Topic("category color", "/cb category color <category> <color>", "Tint the category name"),
                    new Topic("category desc", "/cb category desc <category> <text>", "Set a category's description"),
                    new Topic("category give", "/cb category give <category>", "Get one of every block in a category"),
                    new Topic("note", "/cb note <id> <text>", "Attach a note to a block"),
                    new Topic("lock", "/cb lock <id>", "Protect a block from edits"),
                    new Topic("unlock", "/cb unlock <id>", "Remove a block's edit lock"),
                    new Topic("locked", "/cb locked", "List locked blocks"),
                    new Topic("fav", "/cb fav <id>", "Bookmark a block (per player)"),
                    new Topic("favs", "/cb favs", "List your favorites"),
                    new Topic("draft", "/cb draft <id>", "Hide a block as work-in-progress"),
                    new Topic("publish", "/cb publish <id>", "Publish a draft"),
                    new Topic("drafts", "/cb drafts", "List drafts"))),

            new Category("Templates & Macros", Items.PAPER, "Reusable settings and command sequences", List.of(
                    new Topic("template save", "/cb template save <name> <fromId>", "Save a block's settings as a template"),
                    new Topic("template apply", "/cb template apply <name> <toId>", "Apply a template to a block"),
                    new Topic("template list", "/cb template list", "List templates"),
                    new Topic("macro record", "/cb macro record <name>", "Record a command sequence"),
                    new Topic("macro play", "/cb macro play <name>", "Replay a recorded macro"),
                    new Topic("macro list", "/cb macro list", "List macros"))),

            new Category("History", Items.CLOCK, "Undo, redo and the edit log", List.of(
                    new Topic("undo", "/cb undo", "Undo your last block change"),
                    new Topic("redo", "/cb redo", "Redo an undone change"),
                    new Topic("undogui", "/cb undogui", "Visual undo browser"),
                    new Topic("redogui", "/cb redogui", "Visual redo browser"),
                    new Topic("history", "/cb history", "Who changed what, and when"))),

            new Category("GUIs & HUD", Items.CHEST, "Menus, the editor and the overlay", List.of(
                    new Topic("gui", "/cb gui", "Open the dashboard"),
                    new Topic("editor", "/cb editor <id>", "Open a block's editor menu"),
                    new Topic("listgui", "/cb listgui", "List all GUIs with open buttons"),
                    new Topic("magicitems", "/cb magicitems", "Special admin tools"),
                    new Topic("edithud", "/cb edithud", "Drag the block-info HUD anywhere"),
                    new Topic("config hud", "/cb config hud toggle", "Turn the HUD overlay on/off"))),

            new Category("Sharing", Items.ENDER_CHEST, "Export, import and integrations", List.of(
                    new Topic("export", "/cb export", "Export dashboard (opens chest GUI)"),
                    new Topic("category export", "/cb category export <category>", "ZIP a whole category (textures + JSON)"),
                    new Topic("category share", "/cb category share <category>", "Upload a category, get a share code"),
                    new Topic("category import", "/cb category import <code>", "Import a shared category by code"),
                    new Topic("importfolder", "/cb importfolder", "Import block JSONs from a folder"),
                    new Topic("vault", "/cb vault", "Cloud Block Vault"),
                    new Topic("discord", "/cb discord test", "Discord webhook notifications"))),

            new Category("Arabic", Items.BOOK, "Arabic letter & word blocks", List.of(
                    new Topic("arabic list", "/cb arabic list", "List Arabic letter blocks"),
                    new Topic("arabic import", "/cb arabic import", "Import the Arabic letter set"),
                    new Topic("arabic word", "/cb arabic word <word>", "Build a word from letter blocks"))),

            new Category("Tools", Items.BRUSH, "Hand tools you can hold and use", List.of(
                    new Topic("omni", "/cb omni", "Get the Omni-Tool (sneak+right-click to switch modes)"),
                    new Topic("brush", "/cb brush", "Get the Omni-Tool in Glow mode (cycle a block's glow)"),
                    new Topic("chisel", "/cb chisel", "Get the Omni-Tool in Hardness mode (cycle hardness)"),
                    new Topic("rectangle", "/cb rectangle", "Get the Rainbow Rectangle (mark an area's corners)"),
                    new Topic("deleter", "/cb deleter", "Get the Deleter (right-click to delete a block)"))),

            new Category("System", Items.COMPARATOR, "Config, diagnostics and maintenance", List.of(
                    new Topic("config", "/cb config", "Server configuration"),
                    new Topic("diag", "/cb diag", "Live system diagnostics"),
                    new Topic("incidents", "/cb incidents", "Review logged errors"),
                    new Topic("reload", "/cb reload", "Reload blocks from disk"),
                    new Topic("rp pause", "/cb rp pause", "Pause resource-pack rebuilds"),
                    new Topic("rp resume", "/cb rp resume", "Resume resource-pack rebuilds"),
                    new Topic("sync", "/cb sync", "Re-send the pack to everyone"),
                    new Topic("welcome", "/cb welcome", "Quick-start links for new users"),
                    new Topic("help", "/cb help", "This command browser"))));

    /** Look up a category by name (case-insensitive), or null. */
    public static Category byName(String name) {
        for (Category c : CATEGORIES) {
            if (c.name().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    /**
     * Every known first token after /cb (e.g. "create", "setglow"), lowercase — the
     * dictionary DidYouMean matches typos against. Includes the dashboard aliases that
     * have no help topic of their own.
     */
    public static Set<String> firstTokens() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (Category c : CATEGORIES) {
            for (Topic t : c.topics()) {
                // suggest is always "/cb <token> ..." — take the first word after /cb.
                String[] parts = t.suggest().split("\\s+");
                if (parts.length >= 2) set.add(parts[1].toLowerCase(Locale.ROOT));
            }
        }
        set.add("menu");
        set.add("dashboard");
        set.add("admingui");
        set.add("template");
        set.add("macro");
        set.add("unsuppress");
        return set;
    }
}
