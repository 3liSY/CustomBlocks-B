/**
 * HelpTopics.java
 *
 * Responsibility: The static command-reference data behind the /cb help chest GUI
 * (Group 04) — every player-facing /cb command, grouped by category, with the syntax
 * to pre-fill and a short description.
 *
 * G04-2 (2026-07-14) — what changed, and why it matters:
 *
 * This file used to ALSO be the dictionary DidYouMean matched typos against, on the theory that
 * "help and typo correction can never drift apart". They didn't drift from each other — they drifted
 * together, away from the actual command tree. 67 of the 119 registered subcommands had no entry
 * here, `animation` among them, which is why `/cb anim` could never suggest `/cb animation`.
 *
 * So the dictionary role is GONE: DidYouMean now reads {@link com.customblocks.command.CommandTree},
 * i.e. the literals Brigadier really registered. This file's only job is human-readable help, and
 * the {@code helpCoverageGate} build task fails if a registered command has no entry here — so the
 * list can no longer fall behind in silence.
 *
 * Depends on: Minecraft Items (category icons)
 * Called by:  HelpMenu (renders), CommandTree (audits coverage), helpCoverageGate (build)
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
                    new Topic("list", "/cb list", "Browse every custom block in the Bulk Operations Hub"),
                    new Topic("search", "/cb search <query>", "Find blocks by id, name or category"),
                    new Topic("rename", "/cb rename <id> <new name>", "Change a block's display name"),
                    new Topic("reid", "/cb reid <id> <newId>", "Change a block's id, keeping everything else"),
                    new Topic("dupe", "/cb dupe <id> <newId>", "Duplicate a block under a new id"),
                    new Topic("delete", "/cb delete <id>", "Delete a block (undoable)"),
                    new Topic("trash", "/cb trash", "Open the trash — restore a deleted block"),
                    new Topic("deletedblocks", "/cb deletedblocks", "List blocks that are in the trash"))),

            new Category("Looks & Behavior", Items.PAINTING, "Texture, shape and block properties", List.of(
                    new Topic("retexture", "/cb retexture <id> <imageUrl>", "Replace a block's texture from a URL"),
                    new Topic("retextureall", "/cb retextureall <imageUrl>", "Retexture every block at once"),
                    new Topic("tempretexture", "/cb tempretexture <id> <imageUrl>", "Try a texture, then keep or revert it"),
                    new Topic("animation", "/cb animation <id>", "Tune an animated block's speed, loop and smoothing"),
                    new Topic("resize", "/cb resize <id> <px>", "Re-render a block's texture at a new size"),
                    new Topic("setglow", "/cb setglow <id> <0-15>", "Make a block emit light"),
                    new Topic("sethardness", "/cb sethardness <id> <value>", "How long it takes to break (or unbreakable)"),
                    new Topic("setsound", "/cb setsound <id> <type>", "Step/break sound type"),
                    new Topic("setcollision", "/cb setcollision <id> <solid|passable>", "Solid block or walk-through"),
                    new Topic("setshape", "/cb setshape <id> <shape>", "Give a block a shape (slab, stairs, …)"),
                    new Topic("clearshape", "/cb clearshape <id>", "Back to a full cube"),
                    new Topic("shapelist", "/cb shapelist", "List every available shape"),
                    new Topic("shapepreview", "/cb shapepreview <shape>", "Preview a shape before applying it"),
                    new Topic("shapeeditor", "/cb shapeeditor <id>", "Open the visual shape editor"))),

            new Category("Faces", Items.OAK_SIGN, "Paint each side of a block separately", List.of(
                    new Topic("setface", "/cb setface <id> <face> <imageUrl>", "Texture one face of a block"),
                    new Topic("clearface", "/cb clearface <id> <face>", "Reset one face to the base texture"),
                    new Topic("clearallfaces", "/cb clearallfaces <id>", "Reset every face to the base texture"),
                    new Topic("facechangegui", "/cb facechangegui <id>", "Open the per-face editor GUI"))),

            new Category("Colour & Image", Items.CYAN_DYE, "Recolour, gradients and picking colours", List.of(
                    new Topic("colors", "/cb colors <id>", "Open the colour studio for a block"),
                    new Topic("coloring", "/cb coloring <id>", "Colour-in mode — paint regions of a texture"),
                    new Topic("recolor", "/cb recolor <id> <color>", "Recolour a block's texture"),
                    new Topic("customcolor", "/cb customcolor <id> <hex>", "Recolour with an exact hex colour"),
                    new Topic("colorvariants", "/cb colorvariants <id>", "Make a whole colour family from one block"),
                    new Topic("variants", "/cb variants <id>", "Alias of /cb colorvariants"),
                    new Topic("recolorvariants", "/cb recolorvariants <id>", "Rebuild a block's colour family"),
                    new Topic("gradient", "/cb gradient <idA> <idB> <steps>", "Build a gradient between two blocks"),
                    new Topic("gradientpick", "/cb gradientpick", "Pick gradient endpoints in a GUI"),
                    new Topic("palette", "/cb palette", "Your working palette of saved colours"),
                    new Topic("eyedrop", "/cb eyedrop <id>", "Pull the colours out of a block's texture"),
                    new Topic("tolerance", "/cb tolerance <value>", "How closely colours must match to be replaced"),
                    new Topic("bgstudio", "/cb bgstudio <id>", "Remove or replace a texture's background"),
                    new Topic("exportpng", "/cb exportpng <id>", "Save a block's texture as a .png"))),

            new Category("Bulk", Items.HOPPER, "Do one thing to many blocks at once", List.of(
                    new Topic("bulk", "/cb bulk", "Open the Bulk Operations Hub"),
                    new Topic("bulkhub", "/cb bulkhub", "Open the Bulk Operations Hub"),
                    new Topic("bulkgui", "/cb bulkgui", "Open the Bulk Operations Hub"),
                    new Topic("bulkproperty", "/cb bulkproperty <ids...> <prop> <value>", "Set a property on many blocks"),
                    new Topic("bulkrename", "/cb bulkrename <ids...> <pattern>", "Rename many blocks at once"),
                    new Topic("bulkreid", "/cb bulkreid <ids...> <pattern>", "Re-id many blocks at once"),
                    new Topic("bulkdelete", "/cb bulkdelete <ids...>", "Delete many blocks (asks first)"),
                    new Topic("bulkduplicate", "/cb bulkduplicate <ids...> <suffix>", "Duplicate many blocks"),
                    new Topic("bulkexport", "/cb bulkexport <ids...>", "Export many blocks at once"),
                    new Topic("bulkcategory", "/cb bulkcategory <ids...> <category>", "Re-categorise many blocks"),
                    new Topic("bulkshape", "/cb bulkshape <ids...> <shape>", "Set the shape of many blocks"),
                    new Topic("setall", "/cb setall <setting> <value>", "Set one setting on EVERY block (auto-backup first)"))),

            new Category("Organize", Items.BOOKSHELF, "Categories, lore, locks, favorites", List.of(
                    new Topic("setcategory", "/cb setcategory <id> <category>", "Put a block in a category"),
                    new Topic("categories", "/cb categories", "Browse blocks by category (chest GUI)"),
                    new Topic("category", "/cb category", "Category tools (rename, merge, delete, color, desc, icon…)"),
                    new Topic("category edit", "/cb category edit <category>", "Open the category editor GUI"),
                    new Topic("category rename", "/cb category rename <old> <new>", "Rename a category"),
                    new Topic("category merge", "/cb category merge <source> <target>", "Merge two categories"),
                    new Topic("category color", "/cb category color <category> <color>", "Tint the category name"),
                    new Topic("category desc", "/cb category desc <category> <text>", "Set a category's description"),
                    new Topic("category give", "/cb category give <category>", "Get one of every block in a category"),
                    new Topic("lore", "/cb lore <id>", "Open a block's lore editor"),
                    new Topic("note", "/cb note <id>", "Alias of /cb lore"),
                    new Topic("lock", "/cb lock <id>", "Protect a block from edits"),
                    new Topic("unlock", "/cb unlock <id>", "Remove a block's edit lock"),
                    new Topic("locked", "/cb locked", "List locked blocks"),
                    new Topic("fav", "/cb fav <id>", "Bookmark a block (per player)"),
                    new Topic("favs", "/cb favs", "List your favorites"))),

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
                    new Topic("menu", "/cb menu", "Open the dashboard"),
                    new Topic("dashboard", "/cb dashboard", "Open the dashboard"),
                    new Topic("admingui", "/cb admingui", "Open the admin dashboard"),
                    new Topic("edit", "/cb edit <id>", "Open a block's editor Screen (what the ✎ Edit chip runs)"),
                    new Topic("editor", "/cb editor <id>", "Open a block's editor menu"),
                    new Topic("magicitems", "/cb magicitems", "Special admin tools"),
                    new Topic("editmagicitems", "/cb editmagicitems", "Configure the magic-item set"),
                    new Topic("edithud", "/cb edithud", "Drag the HUD widgets anywhere"),
                    new Topic("config hud", "/cb config hud toggle", "Turn the HUD overlay on/off"))),

            new Category("Sharing", Items.ENDER_CHEST, "Export, import and integrations", List.of(
                    new Topic("export", "/cb export", "Export dashboard (opens chest GUI)"),
                    new Topic("exportblock", "/cb exportblock <id>", "Make a tradeable Blueprint item"),
                    new Topic("importblock", "/cb importblock <code>", "Import a block from a Vault share code"),
                    new Topic("category export", "/cb category export <category>", "ZIP a whole category (textures + JSON)"),
                    new Topic("category share", "/cb category share <category>", "Upload a category, get a share code"),
                    new Topic("category import", "/cb category import <code>", "Import a shared category by code"),
                    new Topic("importfolder", "/cb importfolder", "Import block JSONs from a folder"),
                    new Topic("vault", "/cb vault", "Cloud Block Vault"),
                    new Topic("vault upload", "/cb vault upload <id>", "Upload one block, get a share code"),
                    new Topic("vault download", "/cb vault download <code>", "Download one shared block"),
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

            new Category("Games", Items.NOTE_BLOCK, "Party modes built on custom blocks", List.of(
                    new Topic("guess", "/cb guess <player>", "Guess Mode — disguise players as blocks"),
                    new Topic("buzzergame", "/cb buzzergame", "BuzzerGame — quiz buzzers, timer and panel"),
                    new Topic("tomato", "/cb tomato <targets> [amount]", "Hand out Explosive Tomatoes (sneak + throw to ride)"),
                    new Topic("wheel", "/cb wheel", "Wheel of Fortune - place the wheel, right-click it to spin"))),

            new Category("Safety & Backup", Items.SHIELD, "Backups and broken-block repair", List.of(
                    new Topic("backup", "/cb backup", "Save, list and restore backups"),
                    new Topic("backupgui", "/cb backupgui", "Open the backup browser"),
                    new Topic("safety", "/cb safety", "Repair blocks whose texture went missing"),
                    new Topic("showbrokenblocks", "/cb showbrokenblocks", "List blocks with a broken texture"),
                    new Topic("audit", "/cb audit", "Check every block for problems"))),

            new Category("System", Items.COMPARATOR, "Config, diagnostics and maintenance", List.of(
                    new Topic("config", "/cb config", "Server configuration"),
                    new Topic("diag", "/cb diag", "Live system diagnostics"),
                    new Topic("incidents", "/cb incidents", "Review logged errors (or /cb incidents <code>)"),
                    new Topic("report", "/cb report", "Write a diagnostics report to disk"),
                    new Topic("cache", "/cb cache", "Inspect and clear the texture cache"),
                    new Topic("reload", "/cb reload", "Reload blocks from disk"),
                    new Topic("rp pause", "/cb rp pause", "Pause resource-pack rebuilds"),
                    new Topic("rp resume", "/cb rp resume", "Resume resource-pack rebuilds"),
                    new Topic("sync", "/cb sync", "Re-send the pack to everyone"),
                    new Topic("particles", "/cb particles <cat> <on|off>", "Turn feedback particles on or off"),
                    new Topic("sounds", "/cb sounds <cat> <on|off>", "Turn feedback sounds on or off"),
                    new Topic("feedback", "/cb feedback <cat> <on|off>", "Turn feedback particles AND sounds on or off"),
                    new Topic("achievements", "/cb achievements", "Your CustomBlocks achievements"),
                    new Topic("sourcelist", "/cb sourcelist", "List the source image behind each block"),
                    new Topic("recordoverlay", "/cb recordoverlay", "Capture-invisible overlay for recording"),
                    new Topic("ai", "/cb ai <prompt>", "Describe what you want in plain English"),
                    new Topic("welcome", "/cb welcome", "Quick-start links for new users"),
                    new Topic("help", "/cb help", "This command browser"))));

    /**
     * Registered commands that deliberately have NO help topic, with the reason. The
     * {@code helpCoverageGate} accepts a command as "accounted for" if it is either a {@link Topic}
     * above or named here — so nothing can go missing by accident, but genuinely internal commands
     * don't have to clutter a player's help.
     *
     * Keep this list SHORT and justified. If you're tempted to add a command a player might type,
     * write it a Topic instead.
     */
    public static final Set<String> INTERNAL = Set.of(
            "confirm",      // the Yes half of a destructive confirm — only ever reached by clicking a chip
            "cancel",       // the No half of the same
            "unsuppress",   // internal: un-hide a GUI that was suppressed
            "spawnmarker",  // dev-only marker used to test block placement
            "sourcewall",   // Group 14 TEMPORARY re-source command; removed when that job is done
            "debug"         // §G04-5 hidden dev command: /cb debug kick <cause> (kick-screen preview)
    );

    /** Look up a category by name (case-insensitive), or null. */
    public static Category byName(String name) {
        for (Category c : CATEGORIES) {
            if (c.name().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    /**
     * Every /cb subcommand that is accounted for — has a help topic, or is declared {@link #INTERNAL}.
     *
     * This is the CHECKLIST the coverage audit compares the real command tree against. It is NOT a
     * command dictionary: nothing sources command names from here any more (that was the G04-2 bug).
     */
    public static Set<String> documentedCommands() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (Category c : CATEGORIES) {
            for (Topic t : c.topics()) {
                // suggest is always "/cb <token> ..." — take the first word after /cb.
                String[] parts = t.suggest().split("\\s+");
                if (parts.length >= 2) set.add(parts[1].toLowerCase(Locale.ROOT));
            }
        }
        set.addAll(INTERNAL);
        return set;
    }
}
