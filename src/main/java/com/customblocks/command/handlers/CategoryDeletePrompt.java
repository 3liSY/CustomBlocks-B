/**
 * CategoryDeletePrompt.java — the one-question delete prompt for /cb category delete (G11 §C).
 *
 * Delete has ONE form and asks in chat (2026-07-26). Running it never destroys anything by itself:
 *
 *   • nothing is exclusive to the category → it is dropped straight away, blocks kept, no prompt
 *     (C4: asking a question with only one safe answer is noise);
 *   • something IS exclusive → one [CB] line naming BOTH counts, then two buttons —
 *     [✔ Keep Blocks] and the danger button, whose hover lists every id that would die (C5).
 *
 * Clicking the danger button IS the confirmation: no /cb confirm, no second question. The two undo
 * entries it leaves (blocks newest, category under it) are what covers a mistake — see
 * {@link CategoryService#deleteExclusiveBlocks}.
 *
 * The buttons run a TOKEN, not the category name: a token is single-use and expires, so a stale
 * chat line scrolled back to an hour later cannot wipe a category that has changed underneath it,
 * and there is no typeable "destroy" command sitting in the tree next to `delete`.
 *
 * Depends on: CategoryService, CategoryChat, Chat, CbFmt
 * Called by:  CategoryMemberCommands (delete / keep / wipe)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.CategoryMembershipStore;
import com.customblocks.core.CategoryMetadataStore;
import com.customblocks.core.CategoryService;
import com.customblocks.core.SlotData;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CategoryDeletePrompt {

    private CategoryDeletePrompt() {} // static-only

    /** How long a posted prompt stays clickable. Past this, the buttons say so instead of firing. */
    private static final long TTL_MS = 2 * 60 * 1000L;

    private static final UUID CONSOLE = new UUID(0L, 0L);

    private record Pending(String token, String cat, List<String> exclusive, long expiresAt) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    /** `/cb category delete <name>` — delete outright when nothing is at risk, otherwise ask. */
    static int delete(ServerCommandSource src, String typedName) {
        String cat = CategoryMembershipStore.key(typedName);
        List<SlotData> exclusive = CategoryService.exclusiveBlocksIn(cat);
        if (exclusive.isEmpty()) {
            // C4: nothing can be destroyed, so there is nothing to ask about.
            return CategoryChat.report(src, CategoryService.deleteCategoryOnly(BulkConfirm.actor(src), typedName));
        }
        List<String> ids = new ArrayList<>();
        for (SlotData d : exclusive) ids.add(d.customId());

        String token = Long.toHexString(System.nanoTime() & 0xFFFFFFL);
        PENDING.put(key(src), new Pending(token, cat, ids, System.currentTimeMillis() + TTL_MS));
        Chat.line(src, promptLine(cat, CategoryMembershipStore.blocksIn(cat).size(), ids, token, src));
        return 1;
    }

    /** The single [CB] question line: both counts, then the two choices. */
    private static MutableText promptLine(String cat, int inside, List<String> ids, String token,
                                          ServerCommandSource src) {
        MutableText line = Text.literal(CbFmt.BODY + "Delete ")
                .append(CategoryChat.name(cat))
                .append(Text.literal(CbFmt.BODY + "? " + CbFmt.VALUE + inside + CbFmt.BODY
                        + " block(s) are in it, " + CbFmt.VALUE + ids.size() + CbFmt.BODY
                        + " live nowhere else.  "));
        if (src.getEntity() instanceof ServerPlayerEntity) {
            line.append(Chat.runButton(CbFmt.OK + "[✔ Keep Blocks]", "/cb category keep " + token,
                            "Drop the category only — every block survives"))
                .append(Text.literal("  "))
                .append(Chat.runButton(CbFmt.DANGER + "[✖ Delete " + ids.size() + " Block"
                                + (ids.size() == 1 ? "" : "s") + "]",
                        "/cb category wipe " + token, dangerHover(ids)));
            return line;
        }
        // Console can't click. Same two choices, typed — the token keeps them unambiguous.
        return line.append(Text.literal(CbFmt.DIM + "Run " + CbFmt.BODY + "/cb category keep " + token
                + CbFmt.DIM + " or " + CbFmt.BODY + "/cb category wipe " + token));
    }

    /** The danger button's hover: every id that would be destroyed, named before the click. */
    private static String dangerHover(List<String> ids) {
        StringBuilder sb = new StringBuilder(CbFmt.DANGER + "Destroys " + ids.size() + " block"
                + (ids.size() == 1 ? "" : "s") + " — no /cb confirm, it fires on click:");
        for (String id : ids) sb.append("\n").append(CbFmt.BODY).append(" • ").append(id);
        sb.append("\n").append(CbFmt.DIM).append("/cb undo brings them back.");
        return sb.toString();
    }

    /** `[✔ Keep Blocks]` — drop the category only; every block survives (C6). */
    static int keep(ServerCommandSource src, String token) {
        Pending p = claim(src, token);
        if (p == null) return 0;
        return CategoryChat.report(src, CategoryService.deleteCategoryOnly(BulkConfirm.actor(src), p.cat()));
    }

    /** The danger button — destroys the exclusive blocks, then drops the category (C7). */
    static int wipe(ServerCommandSource src, String token) {
        Pending p = claim(src, token);
        if (p == null) return 0;
        return CategoryChat.report(src, CategoryService.deleteExclusiveBlocks(
                src.getServer(), BulkConfirm.actor(src), p.cat()));
    }

    /**
     * Take the caller's pending prompt if {@code token} is the live one, or explain why not.
     * Single-use: the entry is dropped whether the click destroys anything or not, so a chat line
     * cannot be clicked twice.
     */
    private static Pending claim(ServerCommandSource src, String token) {
        Pending p = PENDING.get(key(src));
        if (p == null || !p.token().equals(token)) {
            Chat.error(src, "That delete question isn't open any more. Run /cb category delete again.");
            return null;
        }
        PENDING.remove(key(src));
        if (System.currentTimeMillis() > p.expiresAt()) {
            Chat.error(src, "That delete question expired. Run /cb category delete "
                    + CategoryMetadataStore.getDisplayName(p.cat()) + " again.");
            return null;
        }
        return p;
    }

    private static UUID key(ServerCommandSource src) {
        UUID u = BulkConfirm.actor(src);
        return u != null ? u : CONSOLE;
    }
}
