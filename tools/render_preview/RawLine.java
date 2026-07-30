/** RawLine.java — raw source pixels along a line. Usage: java RawLine <img> <x> <y> <dx> <dy> <n> */
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class RawLine {
    public static void main(String[] a) throws Exception {
        BufferedImage img = ImageIO.read(new File(a[0]));
        int x = Integer.parseInt(a[1]), y = Integer.parseInt(a[2]);
        int dx = Integer.parseInt(a[3]), dy = Integer.parseInt(a[4]), n = Integer.parseInt(a[5]);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++, x += dx, y += dy) {
            if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) break;
            sb.append(String.format("(%d,%d)%08X ", x, y, img.getRGB(x, y)));
            if (i % 6 == 5) sb.append('\n');
        }
        System.out.println(sb);
    }
}
