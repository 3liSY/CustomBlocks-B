/**
 * BulkReidCommands.java
 *
 * Bulk re-id (Group 07 §D): change the custom id of many blocks at once by a pattern transform —
 * the id-counterpart of /cb bulkrename (which transforms the display name). Each matched block keeps
 * its slot index (so textures + placed blocks are untouched, no pack rebuild), exactly like the
 * single /cb reid. Modes mirror bulkrename:
 *
 *   /cb bulkreid <filter> prefix <text>          newId = text + oldId
 *   /cb bulkreid <filter> suffix <text>          newId = oldId + text
 *   /cb bulkreid <filter> replace <old> <new>    newId = oldId.replace(old, new)
 *
 * Per block we SKIP (and report): locked blocks, no-op transforms (newId == oldId), invalid ids
 * (must be the same charset as /cb create), and collisions — a newId already taken by a block, or
 * already claimed by an earlier block in this same batch (this also rules out unsafe id swaps).
 * The whole batch records ONE undo entry (REID children), so a single /cb undo re-ids them all back.
 * Big/"all" batches are held for /cb confirm, like the other bulk ops.
 *
 * Depends on: BulkScope, SlotManager (reId/hasId), LockManager, UndoManager, BulkConfirm, BulkChat,
 *             HudSync, Chat
 * Called by:  CommandRegistrar, BulkActionMenu (applyReidFromGui — once the reid GUI lands)
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.BulkScope;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.network.HudSync;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class BulkReidCommands {

    private BulkReidCommands() {} // static-only

    /** Same id charset as /cb create / /cb reid (Brigadier word()): letters, digits, _ - . + */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_.+-]+");

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("bulkreid")
                .executes(ctx -> { usage(ctx.getSource()); return 0; })
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.RENAME_ARGS)
                        .executes(ctx -> bulkReid(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));
    }

    /** Run a bulk re-id assembled in the GUI (prefix/suffix use text A; replace uses A → B). */
    public static void applyReidFromGui(ServerPlayerEntity player, String filter, String mode, String a, String b) {
        net.minecraft.server.MinecraftServer s = player.getServer();
        if (s == null) return;
        String args = "replace".equals(mode)
                ? filter + " replace " + a + " " + b
                : filter + " " + mode + " " + a;
        s.execute(() -> {
            player.closeHandledScreen();
            bulkReid(player.getCommandSource(), args.trim());
        });
    }

    private static int bulkReid(ServerCommandSource src, String args) {
        String[] t = args.trim().split("\\s+");
        if (t.length < 3) { usage(src); return 0; }
        String filter = t[0];
        String mode = t[1].toLowerCase(Locale.ROOT);
        String a;
        String b = "";
        switch (mode) {
            case "prefix", "suffix" -> a = String.join(" ", Arrays.copyOfRange(t, 2, t.length));
            case "replace" -> {
                if (t.length < 4) { usage(src); return 0; }
                a = t[2];
                b = t[3];
            }
            default -> { usage(src); return 0; }
        }

        List<SlotData> blocks = BulkScope.resolve(filter, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched filter: " + filter); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(filter) || blocks.size() > threshold;

        final String fmode = mode, fa = a, fb = b;
        Runnable action = () -> applyReid(src, blocks, fmode, fa, fb);
        if (needConfirm) {
            BulkConfirm.request(src, action, "re-id " + blocks.size() + " block(s)");
            String hoverList = "§7" + reidWhat(mode, a, b) + " on:\n§f" + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, "§fRe-id ", "§f (" + reidWhat(mode, a, b) + ")?  ", blocks.size(), hoverList,
                    "§a§l[✔ Confirm]", "§c§l[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    /** Apply the id transform to every eligible matched block, as one undo batch. */
    private static void applyReid(ServerCommandSource src, List<SlotData> blocks, String mode, String a, String b) {
        // Start from every currently-assigned id; a newId is a collision if it's taken by anything
        // other than the block being moved (this also blocks swaps within the batch).
        Set<String> taken = new HashSet<>();
        for (SlotData d : SlotManager.assignedSlots()) taken.add(d.customId());

        List<UndoManager.Op> children = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        int locked = 0, collided = 0, invalid = 0, unchanged = 0;

        for (SlotData target : blocks) {
            String oldId = target.customId();
            if (LockManager.isLocked(oldId)) { locked++; continue; }
            String newId = switch (mode) {
                case "prefix"  -> a + oldId;
                case "suffix"  -> oldId + a;
                case "replace" -> oldId.replace(a, b);
                default        -> oldId;
            };
            if (newId.equals(oldId)) { unchanged++; continue; }
            if (!VALID_ID.matcher(newId).matches()) { invalid++; continue; }
            if (taken.contains(newId)) { collided++; continue; }

            SlotData before = SlotManager.getById(oldId);
            if (before == null) continue;
            SlotData after = SlotManager.reId(oldId, newId);
            if (after == null) continue;
            children.add(new UndoManager.Op(UndoManager.Kind.REID, before, after, null, "bulk reid"));
            changed.add(oldId + " §7→§f " + newId);
            taken.remove(oldId);
            taken.add(newId);
        }

        if (changed.isEmpty()) {
            Chat.error(src, "No ids changed" + skipSuffix(locked, collided, invalid, unchanged) + ".");
            return;
        }
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-reid (" + changed.size() + ")");

        String hoverList = "§7Re-id'd " + changed.size() + " block(s):\n§f" + BulkChat.columns(changed)
                + skipHover(locked, collided, invalid, unchanged);
        MutableText msg = Text.literal("§aRe-id'd ")
                .append(Chat.hover("§e§n" + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + "§r", hoverList))
                .append(Text.literal("  "))
                .append(Chat.runButton("§e[↩ Undo]", "/cb undo", "§7Revert this whole batch §8(/cb undo)"))
                .append(Text.literal(" §a✔"));
        int skipped = locked + collided + invalid + unchanged;
        if (skipped > 0) msg.append(Text.literal("  §8" + skipped + " skipped"));
        Chat.line(src, msg);
        if (src.getEntity() instanceof ServerPlayerEntity p) HudSync.sendTo(p);
    }

    private static String reidWhat(String mode, String a, String b) {
        return switch (mode) {
            case "prefix" -> "add id-prefix \"" + a + "\"";
            case "suffix" -> "add id-suffix \"" + a + "\"";
            default       -> "replace \"" + a + "\" → \"" + b + "\" in ids";
        };
    }

    /** Short reason tail for the "nothing changed" error. */
    private static String skipSuffix(int locked, int collided, int invalid, int unchanged) {
        List<String> parts = new ArrayList<>();
        if (locked > 0)    parts.add(locked + " locked");
        if (collided > 0)  parts.add(collided + " would collide");
        if (invalid > 0)   parts.add(invalid + " invalid id");
        if (unchanged > 0) parts.add(unchanged + " unchanged");
        return parts.isEmpty() ? "" : " — " + String.join(", ", parts);
    }

    /** Hover footnote listing what was skipped, when at least one block did change. */
    private static String skipHover(int locked, int collided, int invalid, int unchanged) {
        String tail = skipSuffix(locked, collided, invalid, unchanged);
        return tail.isEmpty() ? "" : "\n\n§c" + tail.substring(3) + " — skipped";
    }

    private static void usage(ServerCommandSource src) {
        Chat.error(src, "Usage: /cb bulkreid <filter> prefix <text> | suffix <text> | replace <old> <new>");
        Chat.info(src, "Changes many block IDs by a pattern (keeps slots). Filters: all · category:<name> · id:<prefix> · name:<text> · favorite:yes|no · locked:yes|no");
    }
}
