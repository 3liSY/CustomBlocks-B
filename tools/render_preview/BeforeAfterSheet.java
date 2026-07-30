package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * BeforeAfterSheet — lay the same picture's OLD bake beside its NEW one at a readable size, so the
 * owner can judge the change without reading a number. Both bakes are produced by their own build and
 * handed to this as files; this only arranges them.
 *
 * Usage: java -cp <out> com.customblocks.image.BeforeAfterSheet <outFile> <label::before::after> ...
 */
public final class BeforeAfterSheet {
    private static final int CELL = 300;
    private static final int PAD = 16;
    private static final int HEAD = 34;

    public static void main(String[] args) throws Exception {
        int rows = args.length - 1;
        int w = PAD + 2 * (CELL + PAD);
        int h = HEAD + rows * (CELL + HEAD) + PAD;
        BufferedImage sheet = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x1E1E1E));
        g.fillRect(0, 0, w, h);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        g.drawString("BEFORE", PAD + CELL / 2 - 34, 24);
        g.drawString("AFTER", PAD + CELL + PAD + CELL / 2 - 28, 24);

        int y = HEAD;
        for (int i = 1; i < args.length; i++) {
            String[] parts = args[i].split("::");
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(0xC8C8C8));
            g.drawString(parts[0], PAD, y + 18);
            int top = y + HEAD - 8;
            draw(g, parts[1], PAD, top);
            draw(g, parts[2], PAD + CELL + PAD, top);
            y += CELL + HEAD;
        }
        g.dispose();
        ImageIO.write(sheet, "PNG", new File(args[0]));
        System.out.println("wrote " + args[0]);
    }

    private static void draw(Graphics2D g, String path, int x, int y) throws Exception {
        BufferedImage img = ImageIO.read(new File(path));
        double s = Math.min(CELL / (double) img.getWidth(), CELL / (double) img.getHeight());
        int dw = (int) (img.getWidth() * s), dh = (int) (img.getHeight() * s);
        g.setColor(Color.BLACK);
        g.fillRect(x, y, CELL, CELL);
        g.drawImage(img, x + (CELL - dw) / 2, y + (CELL - dh) / 2, dw, dh, null);
    }
}
