/**
 * CaptureOverlayWindow.java - GROUP 29 (Shorts Framing Overlay). CLIENT-SIDE ONLY.
 *
 * Responsibility: own the separate transparent OS window used for the capture-invisible
 * guide in borderless/windowed mode.
 *
 * Depends on: Swing, CaptureOverlayNative, CaptureOverlayPanel.
 * Called by: CaptureOverlayManager.
 */
package com.customblocks.client.capture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.GraphicsEnvironment;

@Environment(EnvType.CLIENT)
final class CaptureOverlayWindow {

    private JWindow window;
    private CaptureOverlayPanel panel;
    private boolean failed;

    void showOrUpdate(long minecraftHwnd) {
        if (failed || minecraftHwnd == 0L || GraphicsEnvironment.isHeadless()) return;
        CaptureOverlayNative.Rect rect = CaptureOverlayNative.windowRect(minecraftHwnd);
        if (rect == null || rect.w() <= 0 || rect.h() <= 0) return;
        SwingUtilities.invokeLater(() -> {
            try {
                ensureWindow();
                window.setBounds(rect.x(), rect.y(), rect.w(), rect.h());
                if (!window.isVisible()) window.setVisible(true);
                window.repaint();
            } catch (Throwable t) {
                failed = true;
                close();
            }
        });
    }

    void close() {
        SwingUtilities.invokeLater(() -> {
            if (window != null) {
                window.setVisible(false);
                window.dispose();
                window = null;
                panel = null;
            }
        });
    }

    private void ensureWindow() {
        if (window != null) return;
        panel = new CaptureOverlayPanel();
        window = new JWindow();
        window.setName("CustomBlocks Shorts Overlay");
        window.setAlwaysOnTop(true);
        window.setFocusableWindowState(false);
        window.setAutoRequestFocus(false);
        window.setBackground(new Color(0, 0, 0, 0));
        window.setContentPane(panel);
        window.pack();
        window.addNotify();
        boolean affinity = CaptureOverlayNative.applyOverlayStyles(window);
        CaptureOverlayConfig.lastMode = affinity ? "borderless_capture_excluded" : "borderless_no_affinity";
        CaptureOverlayConfig.save();
    }
}
