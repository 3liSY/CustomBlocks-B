/** Zoom.java — crop a window out of a PNG and scale it nearest-neighbour, so a 1 px rim is visible.
 *  Usage: java -cp out_lab Zoom <in.png> <out.png> <x> <y> <w> <h> <scale> */
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class Zoom {
    public static void main(String[] a) throws Exception {
        BufferedImage src = ImageIO.read(new File(a[0]));
        int x = Integer.parseInt(a[2]), y = Integer.parseInt(a[3]);
        int w = Integer.parseInt(a[4]), h = Integer.parseInt(a[5]);
        int s = Integer.parseInt(a[6]);
        w = Math.min(w, src.getWidth() - x);
        h = Math.min(h, src.getHeight() - y);
        BufferedImage out = new BufferedImage(w * s, h * s, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < h * s; j++) {
            for (int i = 0; i < w * s; i++) out.setRGB(i, j, src.getRGB(x + i / s, y + j / s));
        }
        ImageIO.write(out, "PNG", new File(a[1]));
    }
}
