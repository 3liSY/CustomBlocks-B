/**
 * CaptureOverlayPanel.java - GROUP 29 (Shorts Framing Overlay). CLIENT-SIDE ONLY.
 *
 * Responsibility: paint the full, capture-excluded 9:16 guide into the native
 * transparent overlay window.
 *
 * Depends on: CaptureOverlayConfig, ShortsGeometry.
 * Called by: CaptureOverlayWindow.
 */
package com.customblocks.client.capture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

@Environment(EnvType.CLIENT)
final class CaptureOverlayPanel extends JPanel {

    private static final Color FRAME = new Color(0x33, 0xDD, 0xFF);
    private static final Color SAFE = new Color(0xFF, 0xFF, 0xFF);
    private static final Color CAPTION = new Color(0xFF, 0xD0, 0x38);
    private static final Color SUBJECT = new Color(0x8D, 0xFF, 0x5B);
    private static final Color DIM = Color.BLACK;

    CaptureOverlayPanel() {
        setOpaque(false);
        setDoubleBuffered(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        if (w < 32 || h < 32) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ShortsGeometry s = ShortsGeometry.of(w, h);

        if (CaptureOverlayConfig.showDim) {
            g2.setComposite(AlphaComposite.SrcOver.derive(CaptureOverlayConfig.clamp01(CaptureOverlayConfig.dimOpacity)));
            g2.setColor(DIM);
            g2.fillRect(0, 0, s.frameX(), h);
            g2.fillRect(s.frameX() + s.frameW(), 0, w - (s.frameX() + s.frameW()), h);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        if (CaptureOverlayConfig.showFrame) {
            drawRect(g2, s.frameX(), s.frameY(), s.frameW(), s.frameH(), FRAME, 3f, null);
        }
        if (CaptureOverlayConfig.showSafe) {
            drawRect(g2, s.safeX(), s.safeY(), s.safeW(), s.safeH(), SAFE, 1.5f, new float[]{10f, 8f});
        }
        if (CaptureOverlayConfig.showSubject) {
            drawRect(g2, s.subjectX(), s.subjectY(), s.subjectW(), s.subjectH(), SUBJECT, 2f, new float[]{16f, 10f});
        }
        if (CaptureOverlayConfig.showCaption) {
            drawRect(g2, s.captionX(), s.captionY(), s.captionW(), s.captionH(), CAPTION, 2.5f, null);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, Math.round(h / 80f))));
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(s.captionX() + 8, s.captionY() - 26, 178, 20, 8, 8);
            g2.setColor(CAPTION);
            g2.drawString("CAPTIONS - KEEP CLEAR", s.captionX() + 15, s.captionY() - 11);
        }
        g2.dispose();
    }

    private static void drawRect(Graphics2D g, int x, int y, int w, int h, Color color, float stroke, float[] dash) {
        g.setColor(color);
        g.setStroke(dash == null
                ? new BasicStroke(stroke)
                : new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
        g.drawRect(x, y, Math.max(0, w - 1), Math.max(0, h - 1));
    }
}
