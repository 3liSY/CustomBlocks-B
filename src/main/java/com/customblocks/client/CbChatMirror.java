/**
 * CbChatMirror.java — client-side chat shadow copy, so /cb clearlogs can remove ONLY [CB] lines
 * (Group 04 §G04-4).
 *
 * The problem: vanilla's ChatHud can be cleared wholesale ({@code clear()}) or appended to
 * ({@code addMessage}), and that is the entire public surface. It exposes no way to read back or
 * selectively remove what is already on screen. But the requirement is explicitly surgical: wipe the
 * admin's own [CB] spam and leave real player chat alone.
 *
 * The approach: keep our own copy. Fabric fires an event for every message the client receives, so we
 * mirror each one into a bounded deque as it arrives. When /cb clearlogs runs we clear the real chat
 * and replay the mirror minus the [CB] lines. No mixin, no accessor into vanilla internals — which
 * matters, because a ChatHud mixin would be a THIRD client mixin poking at a load-bearing vanilla
 * system, and Group 04's whole architecture rule is "never rewrite vanilla chat".
 *
 * Honest limits, and why they're acceptable:
 *   • Replayed lines are re-appended, so they lose their original timestamps and any signed-message
 *     indicator. They are text on a screen the player asked us to tidy — not a security boundary.
 *   • Only messages received since the client started are mirrored (MAX below). Older scrollback is
 *     dropped by the clear, not preserved. Chat scrolls away on its own anyway.
 *
 * Depends on: Fabric ClientReceiveMessageEvents, MinecraftClient.inGameHud.getChatHud()
 * Called by:  CustomBlocksClient (registers the mirror + handles ClearLogsPayload)
 */
package com.customblocks.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class CbChatMirror {

    /** How much scrollback we can faithfully rebuild. Vanilla itself keeps 100 visible lines. */
    private static final int MAX = 200;

    /** The brand every CustomBlocks chat line carries, once the §-codes are stripped off. */
    private static final String BRAND = "[CB]";

    private static final Deque<Text> MIRROR = new ArrayDeque<>();

    private CbChatMirror() {} // static-only

    /** Start shadowing incoming chat. Call once, from client init. */
    public static void register() {
        // GAME = system messages, which is what every Chat.* line is. overlay==true means it went to
        // the action bar rather than the chat box, so it was never in chat and must not be replayed.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) remember(message);
        });
        // CHAT = signed player chat. Mirrored so /cb clearlogs can put it BACK — these are exactly the
        // lines the command promises to leave untouched.
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> remember(message));
    }

    private static synchronized void remember(Text message) {
        if (message == null) return;
        MIRROR.addLast(message);
        while (MIRROR.size() > MAX) MIRROR.removeFirst();
    }

    /**
     * Wipe every [CB] line from this client's chat, keeping everything else.
     *
     * Rebuild rather than delete: clear the chat, then replay the mirror minus our own lines. Must run
     * on the client thread — {@link CustomBlocksClient} calls this inside {@code client.execute(...)}.
     *
     * @return how many [CB] lines were removed
     */
    public static synchronized int clearCbLines() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null) return 0;
        ChatHud hud = mc.inGameHud.getChatHud();

        List<Text> keep = new ArrayList<>();
        int removed = 0;
        for (Text t : MIRROR) {
            if (isCbLine(t)) removed++;
            else keep.add(t);
        }

        // Clear the history too, otherwise the up-arrow sent-message buffer keeps growing while the
        // visible list is rebuilt underneath it.
        hud.clear(true);
        MIRROR.clear();
        for (Text t : keep) {
            MIRROR.addLast(t);
            hud.addMessage(t);
        }
        return removed;
    }

    /** True if this line came from CustomBlocks — i.e. it carries the [CB] tag once colours are stripped. */
    private static boolean isCbLine(Text t) {
        return strip(t.getString()).startsWith(BRAND);
    }

    /**
     * Drop §-formatting codes. Chat.PREFIX is "§0§l[§b§lCB§0§l]§r ", so the raw string does NOT plainly
     * start with "[CB]" — the codes are interleaved right through the tag and must come out first.
     */
    private static String strip(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) { i++; continue; }   // skip § and the code after it
            out.append(c);
        }
        return out.toString().trim();
    }
}
