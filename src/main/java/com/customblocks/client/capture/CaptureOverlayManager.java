/**
 * CaptureOverlayManager.java - GROUP 29 (Shorts Framing Overlay). CLIENT-SIDE ONLY.
 *
 * Responsibility: lifecycle for the Shorts framing helper: load settings, poll the
 * Minecraft window, open/close the native overlay, and expose keybind actions.
 *
 * Depends on: CaptureOverlayConfig, CaptureOverlayWindow, CaptureOverlayNative.
 * Called by: CustomBlocksClient, CbKeybinds, HudRenderMixin fallback.
 */
package com.customblocks.client.capture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class CaptureOverlayManager {

    private static final CaptureOverlayWindow OVERLAY = new CaptureOverlayWindow();

    private CaptureOverlayManager() {}

    public static void init() {
        CaptureOverlayConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(CaptureOverlayManager::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> OVERLAY.close());
    }

    public static void toggle(MinecraftClient client) {
        setEnabled(client, !CaptureOverlayConfig.enabled);
    }

    public static void setEnabled(MinecraftClient client, boolean enabled) {
        CaptureOverlayConfig.enabled = enabled;
        if (!enabled) OVERLAY.close();
        CaptureOverlayConfig.save();
        actionBar(client, "Shorts overlay " + (enabled ? "on" : "off"));
    }

    public static void toggleBorderless(MinecraftClient client) {
        if (client == null || client.getWindow() == null) return;
        if (!CaptureOverlayNative.isWindows()) {
            actionBar(client, "Shorts borderless needs Windows");
            return;
        }
        Window window = client.getWindow();
        if (window.isFullscreen()) {
            window.toggleFullscreen();
        }
        long hwnd = CaptureOverlayNative.hwndFromGlfw(window.getHandle());
        if (hwnd == 0L) {
            CaptureOverlayConfig.lastMode = "native_window_missing";
            actionBar(client, "Shorts borderless could not find the Minecraft window");
            return;
        }
        boolean ok = CaptureOverlayNative.toggleBorderless(hwnd);
        actionBar(client, ok
                ? "Shorts borderless " + (CaptureOverlayNative.borderlessActive() ? "on" : "off")
                : "Shorts borderless failed");
    }

    public static void toggleObsMode(MinecraftClient client) {
        setObsMode(client, !CaptureOverlayConfig.obsMode);
    }

    public static void setObsMode(MinecraftClient client, boolean enabled) {
        if (enabled && !CaptureOverlayNative.isWindows()) {
            CaptureOverlayConfig.obsMode = false;
            CaptureOverlayConfig.save();
            actionBar(client, "Shorts OBS mode needs Windows");
            return;
        }
        CaptureOverlayConfig.obsMode = enabled;
        if (enabled) {
            CaptureOverlayConfig.enabled = true;
            if (client != null && client.getWindow() != null && !CaptureOverlayNative.borderlessActive()) {
                Window window = client.getWindow();
                if (window.isFullscreen()) window.toggleFullscreen();
                long hwnd = CaptureOverlayNative.hwndFromGlfw(window.getHandle());
                if (hwnd == 0L || !CaptureOverlayNative.toggleBorderless(hwnd)) {
                    CaptureOverlayConfig.lastMode = "native_window_missing";
                    CaptureOverlayConfig.save();
                    actionBar(client, "Shorts OBS mode could not find the Minecraft window");
                    return;
                }
            }
        } else if (client != null && client.getWindow() != null && CaptureOverlayNative.borderlessActive()) {
            long hwnd = CaptureOverlayNative.hwndFromGlfw(client.getWindow().getHandle());
            if (hwnd != 0L) CaptureOverlayNative.toggleBorderless(hwnd);
        }
        CaptureOverlayConfig.save();
        actionBar(client, "Shorts OBS mode " + (enabled ? "on" : "off"));
    }

    public static boolean borderlessActive() {
        return CaptureOverlayNative.borderlessActive();
    }

    public static boolean exclusiveFallbackActive(MinecraftClient client) {
        if (!CaptureOverlayConfig.enabled || client == null || client.getWindow() == null) return false;
        Window w = client.getWindow();
        return w.isFullscreen() && w.getFullscreenVideoMode().isPresent();
    }

    private static void tick(MinecraftClient client) {
        if (!CaptureOverlayConfig.enabled || client == null || client.getWindow() == null) {
            OVERLAY.close();
            return;
        }
        if (!CaptureOverlayNative.isWindows()) {
            CaptureOverlayConfig.lastMode = "unsupported_os";
            return;
        }
        if (exclusiveFallbackActive(client)) {
            CaptureOverlayConfig.lastMode = "exclusive_fallback";
            OVERLAY.close();
            return;
        }
        long hwnd = CaptureOverlayNative.hwndFromGlfw(client.getWindow().getHandle());
        if (hwnd == 0L) {
            CaptureOverlayConfig.lastMode = "native_window_missing";
            OVERLAY.close();
            return;
        }
        CaptureOverlayConfig.lastMode = "borderless";
        OVERLAY.showOrUpdate(hwnd);
    }

    private static void actionBar(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("CustomBlocks: " + message), true);
        }
    }
}
