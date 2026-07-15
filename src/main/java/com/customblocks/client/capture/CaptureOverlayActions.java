/**
 * CaptureOverlayActions.java - GROUP 29 (Record Overlay Studio). CLIENT-SIDE ONLY.
 *
 * Responsibility: execute local overlay actions requested by keybinds or the /cb
 * recordoverlay command route.
 */
package com.customblocks.client.capture;

import com.customblocks.client.gui.RecordOverlayStudioScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class CaptureOverlayActions {

    private CaptureOverlayActions() {}

    public static void handle(MinecraftClient client, String rawAction) {
        String action = rawAction == null ? "" : rawAction.trim();
        if (action.isEmpty() || action.equals("studio")) {
            if (client != null) client.setScreen(new RecordOverlayStudioScreen());
            return;
        }

        if (action.equals("on")) {
            CaptureOverlayManager.setEnabled(client, true);
            refresh(client);
        } else if (action.equals("off")) {
            CaptureOverlayManager.setEnabled(client, false);
            refresh(client);
        } else if (action.equals("toggle")) {
            CaptureOverlayManager.toggle(client);
            refresh(client);
        } else if (action.equals("borderless")) {
            CaptureOverlayManager.toggleBorderless(client);
            refresh(client);
        } else if (action.equals("obs_on")) {
            CaptureOverlayManager.setObsMode(client, true);
            refresh(client);
        } else if (action.equals("obs_off")) {
            CaptureOverlayManager.setObsMode(client, false);
            refresh(client);
        } else if (action.equals("obs_toggle")) {
            CaptureOverlayManager.toggleObsMode(client);
            refresh(client);
        } else if (action.startsWith("layout_save:")) {
            String name = CaptureOverlayLayouts.saveCurrent(action.substring("layout_save:".length()));
            bar(client, name.isEmpty() ? "Layout name was empty" : "Saved layout " + name);
        } else if (action.startsWith("layout_load:")) {
            String name = action.substring("layout_load:".length());
            if (CaptureOverlayLayouts.load(name)) {
                bar(client, "Loaded layout " + name);
                refresh(client);
            } else {
                bar(client, "Layout not found: " + name);
            }
        } else if (action.startsWith("layout_delete:")) {
            String name = action.substring("layout_delete:".length());
            bar(client, CaptureOverlayLayouts.delete(name) ? "Deleted layout " + name : "Layout not found: " + name);
        } else if (action.equals("layout_list")) {
            List<String> names = CaptureOverlayLayouts.listNames();
            chat(client, names.isEmpty()
                    ? "Record overlay layouts: none saved yet."
                    : "Record overlay layouts: " + String.join(", ", names));
        } else if (action.startsWith("layout_export:")) {
            exportLayout(client, action.substring("layout_export:".length()));
        } else if (action.equals("layout_import")) {
            importLayout(client);
        } else if (action.equals("push_everyone") || action.equals("push_off")) {
            chat(client, "Record overlay push is reserved for the next multiplayer sync slice.");
        }
    }

    private static void exportLayout(MinecraftClient client, String name) {
        String json = CaptureOverlayLayouts.exportLayout(name);
        if (json.isEmpty()) {
            bar(client, "Layout not found: " + name);
            return;
        }
        if (client != null) client.keyboard.setClipboard(json);
        chat(client, "Copied record overlay layout '" + name + "' to clipboard.");
    }

    private static void importLayout(MinecraftClient client) {
        if (client == null) return;
        String name = CaptureOverlayLayouts.importLayout(client.keyboard.getClipboard());
        if (name.isEmpty()) {
            bar(client, "Clipboard did not contain a layout");
        } else {
            chat(client, "Imported record overlay layout '" + name + "'.");
        }
    }

    private static void refresh(MinecraftClient client) {
        CaptureOverlayConfig.save();
        if (client != null && client.currentScreen instanceof RecordOverlayStudioScreen screen) {
            screen.refreshFromConfig();
        }
    }

    private static void bar(MinecraftClient client, String msg) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("CustomBlocks: " + msg), true);
        }
    }

    private static void chat(MinecraftClient client, String msg) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("CustomBlocks: " + msg), false);
        }
    }
}
