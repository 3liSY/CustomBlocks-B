/**
 * ImportFolderCommands.java — /cb importfolder (Group 12 §B), the whole command surface.
 *
 *   /cb importfolder                     scan the import folder and show the preview
 *   /cb importfolder confirm             create the previewed blocks
 *   /cb importfolder cancel              drop the preview, change nothing
 *   /cb importfolder fix rename <file>   type a different name for one file (anvil)
 *   /cb importfolder fix delete <file>   delete that one file from the folder
 *   /cb importfolder fix ignore <file>   leave it alone and stop listing it
 *
 * It used to read *.json only, out of the EXPORTS folder, and never applied a texture — the whole feature
 * was a definition reader. It now reads pictures from a dedicated folder and makes blocks from them.
 *
 * Rules this file owns:
 *   1  · deliberate. There is no watcher; the owner runs it. ("a safe step I need" over automation)
 *   2  · nothing is created until a preview has been confirmed, and a cancel changes nothing on disk.
 *   3  · the three inline fixes above, each aimed at one named file.
 *   4  · a rename is re-validated the same way, so a fix can never overwrite an existing block either.
 *   16 · admin-only — it creates blocks in bulk.
 *
 * THREADING: a scan reads (and size-checks) every file in the folder, so it NEVER runs on the server
 * thread — {@link #withScan} does it on a worker and hops back before touching chat or game state. Only
 * the render and the commit run on the server thread.
 *
 * The free-form path argument is GONE on purpose: §B settled on one dedicated folder, and letting a
 * command name any directory on the host was a path-traversal surface with nothing to gain.
 *
 * Depends on: ImportScan, ImportService, ImportReport, ImportChat, AnvilPrompt (rename), Chat
 * Called by:  UtilityCommands.register
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.CbPaths;
import com.customblocks.core.ImportEntry;
import com.customblocks.core.ImportReport;
import com.customblocks.core.ImportScan;
import com.customblocks.core.ImportService;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.AnvilPrompt;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ImportFolderCommands {

    private ImportFolderCommands() {} // static-only

    /** How long a shown preview stays confirmable. Long enough to read a long list, short enough that a
     *  stale one can't be confirmed hours later against a folder that has since changed. */
    private static final long PREVIEW_WINDOW_MS = 300_000L;

    /** Stand-in key for the console / a command block, which has no player UUID. */
    private static final UUID CONSOLE = new UUID(0L, 0L);

    /**
     * One actor's shown preview.
     *
     * It holds the DECISIONS (which files to ignore, which to rename) rather than the scan itself: confirm
     * re-scans the folder and re-applies these, so a file added, removed or claimed between the preview and
     * the confirmation is seen as it really is instead of acted on from a stale list.
     */
    private record Preview(Set<String> ignored, Map<String, String> renamed, long expiresAt) {}

    private static final Map<UUID, Preview> PREVIEWS = new ConcurrentHashMap<>();

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // Rule 16: admin-only on the parent, which covers every branch below it.
        root.then(CommandManager.literal("importfolder")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(ImportFolderCommands::preview)
                .then(CommandManager.literal("confirm").executes(ImportFolderCommands::confirm))
                .then(CommandManager.literal("cancel").executes(ImportFolderCommands::cancel))
                .then(CommandManager.literal("fix")
                        .then(CommandManager.literal("rename")
                                .then(CommandManager.argument("file", StringArgumentType.greedyString())
                                        .executes(ctx -> fixRename(ctx, file(ctx)))))
                        .then(CommandManager.literal("delete")
                                .then(CommandManager.argument("file", StringArgumentType.greedyString())
                                        .executes(ctx -> fixDelete(ctx, file(ctx)))))
                        .then(CommandManager.literal("ignore")
                                .then(CommandManager.argument("file", StringArgumentType.greedyString())
                                        .executes(ctx -> fixIgnore(ctx, file(ctx)))))));
    }

    private static String file(CommandContext<ServerCommandSource> ctx) {
        return StringArgumentType.getString(ctx, "file").trim();
    }

    private static UUID key(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : CONSOLE;
    }

    /** The undo owner — null for console/command blocks, which are not undoable. */
    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }

    private static ServerPlayerEntity playerOrNull(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p : null;
    }

    /**
     * Scan the folder on a worker thread, then hand the result to {@code then} back ON the server thread.
     * A scan reads every file's bytes to tell a picture from junk, so doing it inline would hitch the tick
     * on exactly the big folders this feature exists for.
     */
    private static void withScan(ServerCommandSource src, Consumer<ImportScan.Scan> then) {
        MinecraftServer server = src.getServer();
        if (server == null) { Chat.error(src, "No server context."); return; }
        Thread worker = new Thread(() -> {
            ImportScan.Scan scan = ImportScan.scan();
            server.execute(() -> then.accept(scan));
        }, "CustomBlocks-ImportScan");
        worker.setDaemon(true);
        worker.start();
    }

    // ── the preview ──────────────────────────────────────────────────────────

    /** /cb importfolder — scan and show what would happen. Creates nothing (rule 2). */
    private static int preview(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Preview held = live(src);
        showPreview(src, ignoredOf(held), renamedOf(held));
        return 1;
    }

    /** Scan, apply this actor's decisions, render, and hold the decisions for a confirmation. */
    private static void showPreview(ServerCommandSource src, Set<String> ignored, Map<String, String> renamed) {
        withScan(src, raw -> {
            if (raw.created()) { ImportChat.firstRun(src); return; } // rule 15: first run just explains
            ImportScan.Scan scan = applyDecisions(raw, ignored, renamed);
            PREVIEWS.put(key(src), new Preview(ignored, renamed, System.currentTimeMillis() + PREVIEW_WINDOW_MS));
            if (scan.empty()) { ImportChat.empty(src); return; }
            ImportChat.preview(src, scan);
        });
    }

    /**
     * Fold an actor's decisions into a fresh scan: ignored files disappear from the problem list, and a
     * renamed file becomes ready under its new id — unless that id is ALSO taken, in which case it stays a
     * problem (rule 4: a fix can never overwrite either).
     *
     * Re-slices the free-slot cap afterwards, because a rename that turns a problem into a ready entry
     * changes how many slots the run wants.
     */
    private static ImportScan.Scan applyDecisions(ImportScan.Scan scan, Set<String> ignored,
                                                  Map<String, String> renamed) {
        List<ImportEntry> ready = new ArrayList<>(scan.ready());
        List<ImportEntry> problems = new ArrayList<>();
        Set<String> claimed = new HashSet<>();
        for (ImportEntry e : ready) claimed.add(e.id());

        for (ImportEntry e : scan.problems()) {
            if (ignored.contains(e.fileName())) continue;      // rule 3: the line closes, the file stays
            String newId = renamed.get(e.fileName());
            if (newId == null) { problems.add(e); continue; }
            String problem = renameProblem(newId, claimed);
            if (problem != null) { problems.add(e.withProblem(problem)); continue; }
            claimed.add(newId);
            ready.add(e.withId(newId, ImportScan.displayName(newId)));
        }

        List<ImportEntry> leftover = new ArrayList<>(scan.leftover());
        if (ready.size() > scan.freeSlots()) {
            leftover.addAll(0, ready.subList(scan.freeSlots(), ready.size()));
            ready = new ArrayList<>(ready.subList(0, scan.freeSlots()));
        }
        return new ImportScan.Scan(ready, leftover, problems, scan.freeSlots(), scan.unpacked(), false);
    }

    /** Why a typed replacement id is not usable, or null when it is fine. Same rules /cb create applies. */
    private static String renameProblem(String newId, Set<String> claimed) {
        String safe = ImportScan.safeId(newId);
        if (safe.isEmpty())          return "that name has no usable characters — try letters and numbers";
        if (!safe.equals(newId))     return "\"" + newId + "\" is not a safe id — try \"" + safe + "\"";
        if (SlotManager.hasId(safe)) return "a block called \"" + safe + "\" already exists";
        if (claimed.contains(safe))  return "another file in this run already claims \"" + safe + "\"";
        return null;
    }

    /** This actor's held preview if it hasn't expired, else null (and the stale one is dropped). */
    private static Preview live(ServerCommandSource src) {
        Preview p = PREVIEWS.get(key(src));
        if (p == null) return null;
        if (System.currentTimeMillis() > p.expiresAt()) { PREVIEWS.remove(key(src)); return null; }
        return p;
    }

    private static Set<String> ignoredOf(Preview p) {
        return p == null ? new HashSet<>() : new HashSet<>(p.ignored());
    }

    private static Map<String, String> renamedOf(Preview p) {
        return p == null ? new HashMap<>() : new HashMap<>(p.renamed());
    }

    // ── confirm / cancel ─────────────────────────────────────────────────────

    /** /cb importfolder confirm — create exactly what a FRESH scan still says is ready (rule 2). */
    private static int confirm(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Preview held = live(src);
        if (held == null) {
            Chat.info(src, "Nothing is waiting to be confirmed — run /cb importfolder first.");
            return 0;
        }
        withScan(src, raw -> {
            MinecraftServer server = src.getServer();
            if (server == null) { Chat.error(src, "No server context."); return; }
            ImportScan.Scan scan = applyDecisions(raw, held.ignored(), held.renamed());
            if (scan.ready().isEmpty()) {
                PREVIEWS.remove(key(src));
                Chat.info(src, "Nothing is ready to import any more — the folder changed. "
                        + "Run /cb importfolder again.");
                return;
            }
            PREVIEWS.remove(key(src)); // one confirmation per preview

            int count = scan.ready().size();
            Chat.info(src, count >= ImportService.BACKUP_THRESHOLD
                    ? "Backing up first, then importing " + count + " picture" + (count == 1 ? "" : "s") + "…"
                    : "Importing " + count + " picture" + (count == 1 ? "" : "s") + "…");

            List<String> leftoverNames = new ArrayList<>();
            for (ImportEntry e : scan.leftover()) leftoverNames.add(e.fileName());

            ImportService.commit(server, playerOrNull(src), src.getName(), actor(src), scan.ready(), outcome -> {
                List<String> left = new ArrayList<>(leftoverNames);
                left.addAll(outcome.leftover());
                ImportReport report = new ImportReport(System.currentTimeMillis(), src.getName(),
                        outcome.made(), outcome.missed(), left);
                report.keep();                // kept + written to disk, so §C can bring it back later
                ImportChat.result(src, report);
            });
        });
        return 1;
    }

    /** /cb importfolder cancel — forget the preview. Nothing on disk or in the world was touched (B4). */
    private static int cancel(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Preview p = PREVIEWS.remove(key(src));
        Chat.info(src, p == null
                ? "Nothing to cancel."
                : "Cancelled — nothing was created and every file is still in the import folder.");
        return 1;
    }

    // ── the three inline fixes (rule 3) ──────────────────────────────────────

    /**
     * /cb importfolder fix rename &lt;file&gt; — open the anvil pre-filled with the id this file would have
     * claimed, then re-show the preview with the typed id applied. Reuses {@link AnvilPrompt}, the same
     * typing box /cb reid uses, rather than building a second one.
     */
    private static int fixRename(CommandContext<ServerCommandSource> ctx, String fileName) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = playerOrNull(src);
        if (player == null) {
            // No anvil on a console. Say what to do instead of failing silently.
            Chat.info(src, "Renaming needs the typing box, which only works in-game. From console, rename the "
                    + "file in " + ImportChat.FOLDER_LABEL + " and run /cb importfolder again.");
            return 0;
        }
        withProblemFile(src, fileName, entry -> {
            String suggested = entry.id().isEmpty() ? "block" : entry.id();
            AnvilPrompt.open(player, "New name for " + fileName, new ItemStack(Items.NAME_TAG), suggested,
                    typed -> {
                        Preview held = live(src);
                        Map<String, String> renamed = renamedOf(held);
                        renamed.put(fileName, typed.trim());
                        showPreview(src, ignoredOf(held), renamed);
                    },
                    () -> Chat.info(src, "Rename cancelled — " + fileName + " is unchanged."));
        });
        return 1;
    }

    /**
     * /cb importfolder fix delete &lt;file&gt; — delete THAT file from the import folder and nothing else. No
     * block is touched (B8). The path is rebuilt from the folder plus the file NAME only, so the argument
     * cannot reach outside the import folder however it is written.
     */
    private static int fixDelete(CommandContext<ServerCommandSource> ctx, String fileName) {
        ServerCommandSource src = ctx.getSource();
        withProblemFile(src, fileName, entry -> {
            Path target = CbPaths.IMPORT.resolve(Path.of(fileName).getFileName().toString());
            try {
                if (!Files.isRegularFile(target)) {
                    Chat.error(src, "\"" + fileName + "\" is not in the import folder any more.");
                    return;
                }
                Files.delete(target);
            } catch (Exception e) {
                Chat.error(src, "Couldn't delete \"" + fileName + "\" — it may be open in another program.");
                return;
            }
            Chat.success(src, "Deleted \"" + fileName + "\" from the import folder. No block was touched.");
            Preview held = live(src);
            showPreview(src, ignoredOf(held), renamedOf(held));
        });
        return 1;
    }

    /** /cb importfolder fix ignore &lt;file&gt; — leave the file where it is and stop listing it (B9). */
    private static int fixIgnore(CommandContext<ServerCommandSource> ctx, String fileName) {
        ServerCommandSource src = ctx.getSource();
        withProblemFile(src, fileName, entry -> {
            Preview held = live(src);
            Set<String> ignored = ignoredOf(held);
            Map<String, String> renamed = renamedOf(held);
            ignored.add(fileName);
            renamed.remove(fileName);
            Chat.info(src, "Ignoring \"" + fileName + "\" — it stays in the folder, untouched.");
            showPreview(src, ignored, renamed);
        });
        return 1;
    }

    /**
     * Resolve a fix's file argument against the folder as it is NOW (scanned off-thread), then run
     * {@code then} on the server thread. Says so plainly when the file is no longer one of the problems,
     * rather than acting on a line the owner clicked after the folder moved on.
     */
    private static void withProblemFile(ServerCommandSource src, String fileName, Consumer<ImportEntry> then) {
        withScan(src, scan -> {
            for (ImportEntry e : scan.problems()) {
                if (e.fileName().equalsIgnoreCase(fileName)) { then.accept(e); return; }
            }
            Chat.info(src, "\"" + fileName + "\" is not one of the files needing a decision any more. "
                    + "Run /cb importfolder to see the current list.");
        });
    }
}
