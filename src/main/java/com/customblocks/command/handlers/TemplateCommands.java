/**
 * TemplateCommands.java
 *
 * Responsibility: /cb template subcommands — save, apply, list, delete.
 * Templates capture a block's attribute preset (glow, hardness, sound, collision, category)
 * so the same setup can be stamped onto other blocks or shared across worlds.
 * Apply is recorded by UndoManager so /cb undo can reverse it.
 *
 * Depends on: TemplateManager, SlotManager, UndoManager, Chat, BlockSuggestions
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TemplateManager;
import com.customblocks.core.UndoManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TemplateCommands {

    private TemplateCommands() {}

    /** Tab-complete saved template names for apply / delete arguments. */
    private static final SuggestionProvider<ServerCommandSource> TEMPLATE_NAMES = (ctx, builder) -> {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String name : TemplateManager.list()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(typed)) builder.suggest(name);
        }
        return builder.buildFuture();
    };

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("template")
                .executes(ctx -> listTemplates(ctx))
                .then(CommandManager.literal("list")
                        .executes(ctx -> listTemplates(ctx)))
                .then(CommandManager.literal("save")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .suggests(BlockSuggestions.IDS)
                                        .executes(ctx -> save(ctx,
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "id"))))))
                .then(CommandManager.literal("apply")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests(TEMPLATE_NAMES)
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .suggests(BlockSuggestions.IDS)
                                        .executes(ctx -> apply(ctx,
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "id"))))))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests(TEMPLATE_NAMES)
                                .executes(ctx -> delete(ctx,
                                        StringArgumentType.getString(ctx, "name"))))));
    }

    private static int listTemplates(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        List<String> names = TemplateManager.list();
        if (names.isEmpty()) {
            Chat.raw(src, CbFmt.DIM + "No templates yet.");
            Chat.raw(src, Text.literal(CbFmt.DIM + "Templates capture a block's " + CbFmt.BODY + "glow, hardness, sound, collision " + CbFmt.DIM + "and " + CbFmt.BODY + "category" + CbFmt.DIM + "."));
            Chat.raw(src, Text.literal(CbFmt.DIM + "Stamp them onto any block to apply the same style instantly."));
            Chat.raw(src, Text.literal(CbFmt.DIM + "Create one: " + CbFmt.BODY + "/cb template save <name> <id>"));
            return 1;
        }
        Chat.raw(src, CbFmt.VALUE + names.size() + " template(s) " + CbFmt.FAINT + "— glow · hardness · sound · collision · category");
        for (String n : names) {
            TemplateManager.Template t = TemplateManager.load(n);
            String attrs = t == null ? "?" : formatAttrs(t);
            MutableText line = Text.literal(CbFmt.DIM + " - " + CbFmt.BODY + n + " " + CbFmt.FAINT + "(" + attrs + ") ")
                    .append(Chat.suggestButton("[apply]", "/cb template apply " + n + " "))
                    .append(Text.literal(" "))
                    .append(Chat.runButton("[x]", "/cb template delete " + n));
            Chat.raw(src, line);
        }
        return 1;
    }

    private static int save(CommandContext<ServerCommandSource> ctx, String name, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData d = SlotManager.getById(id);
        if (d == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        if (!TemplateManager.save(name, d)) { Chat.error(src, "Failed to save template \"" + name + "\""); return 0; }
        Chat.success(src, "Saved " + CbFmt.BODY + "\"" + name + "\"" + CbFmt.RESET + " from " + CbFmt.VALUE + id + CbFmt.RESET + " " + CbFmt.FAINT + "(" + formatAttrs(d) + ")");
        return 1;
    }

    private static int apply(CommandContext<ServerCommandSource> ctx, String name, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData before = SlotManager.getById(id);
        if (before == null) { Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id."); return 0; }
        if (LockManager.isLocked(id)) {
            Chat.lockedError(src, id);
            return 0;
        }
        TemplateManager.Template t = TemplateManager.load(name);
        SlotData after = TemplateManager.apply(name, id);
        if (after == null) { Chat.error(src, "No template \"" + name + "\""); return 0; }
        UndoManager.recordModify(actor(src), before, after, "template:" + name);
        String attrs = t != null ? " " + CbFmt.FAINT + "(" + formatAttrs(t) + ")" : "";
        Chat.success(src, "Applied " + CbFmt.BODY + "\"" + name + "\"" + CbFmt.RESET + " → " + CbFmt.VALUE + id + attrs);
        return 1;
    }

    private static int delete(CommandContext<ServerCommandSource> ctx, String name) {
        ServerCommandSource src = ctx.getSource();
        if (!TemplateManager.exists(name)) { Chat.error(src, "No template \"" + name + "\""); return 0; }
        if (!TemplateManager.delete(name)) { Chat.error(src, "Failed to delete template \"" + name + "\""); return 0; }
        Chat.success(src, "Deleted template \"" + name + "\"");
        return 1;
    }

    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }

    // ── Format helpers ───────────────────────────────────────────────────────

    private static String formatAttrs(TemplateManager.Template t) {
        StringBuilder sb = new StringBuilder();
        sb.append("glow:").append(t.glow());
        sb.append(" hard:").append(t.hardness());
        sb.append(" ").append(t.soundType());
        if (t.noCollision()) sb.append(" passable");
        if (!t.category().isEmpty()) sb.append(" " + CbFmt.ITALIC).append(t.category()).append(CbFmt.FAINT);
        return sb.toString();
    }

    private static String formatAttrs(SlotData d) {
        StringBuilder sb = new StringBuilder();
        sb.append("glow:").append(d.glow());
        sb.append(" hard:").append(d.hardness());
        sb.append(" ").append(d.soundType());
        if (d.noCollision()) sb.append(" passable");
        if (!d.category().isEmpty()) sb.append(" " + CbFmt.ITALIC).append(d.category()).append(CbFmt.FAINT);
        return sb.toString();
    }

}
