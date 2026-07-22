import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Throwaway: composite the baked white label PNGs onto a dark background so shaping can be eyeballed. */
public final class PreviewLabels {
    public static void main(String[] args) throws Exception {
        String dir = "src/main/resources/assets/customblocks/textures/font/";
        BufferedImage t = ImageIO.read(new File(dir + "timer_label_target.png"));
        BufferedImage r = ImageIO.read(new File(dir + "timer_label_result.png"));
        int pad = 30, gap = 30;
        int w = Math.max(t.getWidth(), r.getWidth()) + 2 * pad;
        int h = t.getHeight() + r.getHeight() + gap + 2 * pad;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(new Color(0x101010));
        g.fillRect(0, 0, w, h);
        g.drawImage(t, pad, pad, null);
        g.drawImage(r, pad, pad + t.getHeight() + gap, null);
        g.dispose();
        ImageIO.write(out, "png", new File("tools/out/preview_labels.png"));
        System.out.println("wrote tools/out/preview_labels.png (" + w + "x" + h + ")");
    }
}
