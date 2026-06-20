/**
 * InGameMock2.java — HYBRID: isolated letters use the bundled hand-art PNG;
 * connected letters (initial/medial/final) are font-rendered with the joining bar,
 * stroke bumped to match the bundled art weight. Composited into a Minecraft scene.
 * NOT shipped. Run from project root (after RenderPreview is compiled):
 *   javac -encoding UTF-8 -d tools/render_preview tools/render_preview/RenderPreview.java tools/render_preview/InGameMock2.java
 *   java  -cp tools/render_preview InGameMock2
 * Output: tools/render_preview/out/INGAME2_*.png
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class InGameMock2 {

    static final char ZWJ  = '‍';
    static final int  CELL = 256, SS = 2, H = CELL * SS;
    static final int  FG   = 0xFFFFFFFF;
    // Bumped to match the bundled art recipe (generate_arabic_letters: ~18 black / ~7 white @256).
    static final float RING  = H * (18f / 256f);
    static final float WHITE = H * (7f  / 256f);

    static Font  BASE;
    static float FS;
    static float BASE_Y;

    static final String ART = "src/main/resources/assets/customblocks/arabic_art/";

    static final Set<Character> NON_CONN = new HashSet<>();
    static { for (char c : "اأإآدذرزوؤةىءٱ".toCharArray()) NON_CONN.add(c); }

    // Arabic char -> bundled art basename (only need isolated-form letters; covers our names + showcase).
    static final Map<Character,String> ART_NAME = new HashMap<>();
    static {
        ART_NAME.put('ا',"alef"); ART_NAME.put('أ',"alef_hamza_above"); ART_NAME.put('إ',"alef_hamza_below");
        ART_NAME.put('آ',"alef_madda"); ART_NAME.put('ى',"alef_maqsura");
        ART_NAME.put('ب',"ba"); ART_NAME.put('ت',"ta"); ART_NAME.put('ث',"tha"); ART_NAME.put('ج',"jeem");
        ART_NAME.put('ح',"ha2"); ART_NAME.put('خ',"kha"); ART_NAME.put('د',"dal"); ART_NAME.put('ذ',"thal");
        ART_NAME.put('ر',"ra"); ART_NAME.put('ز',"zay"); ART_NAME.put('س',"seen"); ART_NAME.put('ش',"sheen");
        ART_NAME.put('ص',"sad"); ART_NAME.put('ض',"dad"); ART_NAME.put('ط',"ta2"); ART_NAME.put('ظ',"tha2");
        ART_NAME.put('ع',"ain"); ART_NAME.put('غ',"ghain"); ART_NAME.put('ف',"fa"); ART_NAME.put('ق',"qaf");
        ART_NAME.put('ك',"kaf"); ART_NAME.put('ل',"lam"); ART_NAME.put('م',"meem"); ART_NAME.put('ن',"noon");
        ART_NAME.put('ه',"ha"); ART_NAME.put('و',"waw"); ART_NAME.put('ي',"ya"); ART_NAME.put('ة',"ta_marbuta");
        ART_NAME.put('ء',"hamza"); ART_NAME.put('ؤ',"waw_hamza"); ART_NAME.put('ئ',"ya_hamza");
    }

    static final String[] COLORS = { "black", "red", "green", "yellow" };
    static final Map<String,Integer> BG = new HashMap<>();          // sampled bg per colour
    static final Map<String,BufferedImage> ARTCACHE = new HashMap<>();

    static final String[][] WORDS = {
        { "علي","Ali" }, { "عبدالله","Abdullah" }, { "محمد","Muhammad" },
        { "خالد","Khalid" }, { "مصطفى","Mustafa" }, { "لؤي","Luay" },
    };

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        BASE = RenderPreview.ARABIC_FONT = RenderPreview.loadFont(RenderPreview.RES + "arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();

        // Sample each colour's bg from a bundled tile corner so font tiles match exactly.
        for (String c : COLORS) {
            BufferedImage a = art("alef", c);
            BG.put(c, a == null ? 0xFF0A0A0A : a.getRGB(4, 4));
        }
        for (String[] w : WORDS) renderScene(w[0], w[1]);
        showcaseIsolated();
        System.out.println("Done. See tools/render_preview/out/INGAME2_*.png");
    }

    static BufferedImage art(String base, String color) {
        String key = base + "_" + color;
        if (ARTCACHE.containsKey(key)) return ARTCACHE.get(key);
        File f = new File(ART + color + "/" + base + "_" + color + ".png");
        BufferedImage img = null;
        try { if (f.exists()) img = ImageIO.read(f); } catch (Exception e) { /* missing */ }
        ARTCACHE.put(key, img);
        return img;
    }

    static void renderScene(String word, String roman) throws Exception {
        char[] L = word.toCharArray();
        int n = L.length;
        boolean[] joinsPrev = new boolean[n], joinsNext = new boolean[n];
        for (int i = 0; i < n; i++) {
            joinsPrev[i] = i > 0 && !NON_CONN.contains(L[i - 1]);
            joinsNext[i] = !NON_CONN.contains(L[i]) && i < n - 1;
        }
        Set<String> chars = new HashSet<>();
        for (char c : L) chars.add(String.valueOf(c));
        computeMetrics(chars.toArray(new String[0]));

        int blk = Math.min(170, 1000 / Math.max(1, n));
        int rowW = n * blk, margin = 90;
        int W = rowW + margin * 2, top = 150, Hc = top + blk + 230;
        BufferedImage canvas = new BufferedImage(W, Hc, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        hints(g);
        scene(g, W, Hc);
        int x0 = margin, y0 = top;
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(x0 - 24, y0 + blk - 6, rowW + 48, 70);

        for (int img = 0; img < n; img++) {
            int li = n - 1 - img;
            boolean cl = joinsNext[li], cr = joinsPrev[li];
            String color = COLORS[img % COLORS.length];
            boolean isolated = !cl && !cr;
            BufferedImage tile = isolated
                    ? isolatedTile(L[li], color)
                    : letterTile(String.valueOf(L[li]), cl, cr, BG.get(color));
            int x = x0 + img * blk;
            g.drawImage(tile, x, y0, blk, blk, null);
            cubeEdges(g, x, y0, blk);
            if (isolated) tag(g, x, y0, blk);   // mark which blocks used the bundled art
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.setColor(Color.WHITE);
        g.drawString(word + "   —   " + roman + "   (" + n + " blocks · gold dot = bundled art)", margin, Hc - 34);
        g.dispose();
        write(canvas, "INGAME2_" + roman + ".png", n);
    }

    static void showcaseIsolated() throws Exception {
        char[] letters = { 'ء', 'ا', 'ز', 'ع', 'ؤ', 'ه' };
        String[] cols  = { "black","red","green","yellow","red","green" };
        int n = letters.length, blk = 150, margin = 90;
        int W = n * blk + margin * 2, top = 150, Hc = top + blk + 230;
        BufferedImage canvas = new BufferedImage(W, Hc, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics(); hints(g); scene(g, W, Hc);
        g.setColor(new Color(0,0,0,70)); g.fillOval(margin-24, top+blk-6, n*blk+48, 70);
        for (int i = 0; i < n; i++) {
            BufferedImage tile = isolatedTile(letters[i], cols[i]);
            int x = margin + i * blk;
            g.drawImage(tile, x, top, blk, blk, null);
            cubeEdges(g, x, top, blk);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 26)); g.setColor(Color.WHITE);
        g.drawString("Isolated single-letter blocks — straight from your bundled art (hamza, alef, zay, ain, waw-hamza, heh)", margin, Hc-34);
        g.dispose();
        write(canvas, "INGAME2_ZZ_isolated_showcase.png", n);
    }

    static BufferedImage isolatedTile(char ch, String color) {
        String base = ART_NAME.get(ch);
        BufferedImage a = base == null ? null : art(base, color);
        if (a != null) return a;
        // fallback: font isolated if no art (shouldn't happen for our set)
        return letterTile(String.valueOf(ch), false, false, BG.getOrDefault(color, 0xFF0A0A0A));
    }

    /* ---------- scene + chrome ---------- */
    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    static void scene(Graphics2D g, int W, int Hc) {
        g.setPaint(new GradientPaint(0,0,new Color(0x6CA6E0),0,Hc,new Color(0xBFE0F2))); g.fillRect(0,0,W,Hc);
        g.setColor(new Color(255,255,255,220));
        cloud(g,(int)(W*0.12),46,30); cloud(g,(int)(W*0.62),78,24);
        int gy = Hc-150;
        g.setColor(new Color(0x6B4A2B)); g.fillRect(0,gy,W,150);
        g.setColor(new Color(0x5A8B36)); g.fillRect(0,gy,W,26);
        g.setColor(new Color(0x4C7A2E)); for (int x=0;x<W;x+=16) g.fillRect(x,gy+26,8,8);
    }
    static void cloud(Graphics2D g,int x,int y,int s){ g.fillRect(x,y,s*4,s); g.fillRect(x+s,y-s,s*2,s); }
    static void cubeEdges(Graphics2D g,int x,int y,int s){
        g.setColor(new Color(255,255,255,26)); g.fillRect(x,y,s,3);
        g.setColor(new Color(0,0,0,55));       g.fillRect(x,y+s-3,s,3);
        g.setColor(new Color(0,0,0,30));        g.drawRect(x,y,s-1,s-1);
    }
    static void tag(Graphics2D g,int x,int y,int s){ g.setColor(new Color(0xFFD24A)); g.fillOval(x+8,y+8,12,12); }
    static void write(BufferedImage c,String name,int n) throws Exception {
        File out = new File("tools/render_preview/out/"+name);
        ImageIO.write(c,"png",out); System.out.println("Wrote "+name+"  ("+n+" blocks)");
    }

    /* ---------- font tile (connected forms) ---------- */
    static void computeMetrics(String[] chars) {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float probe = H * 0.5f; double maxAsc=0,maxDesc=0,maxW=0;
        for (String c : chars) {
            String[] forms = { c, c+ZWJ, ""+ZWJ+c, ""+ZWJ+c+ZWJ };
            for (String form : forms) {
                Rectangle2D r = RenderPreview.layout(form, BASE.deriveFont(probe),
                        TextAttribute.RUN_DIRECTION_RTL, Color.WHITE, frc, 0f).getOutline(null).getBounds2D();
                if (r.isEmpty()) continue;
                maxAsc=Math.max(maxAsc,-r.getMinY()); maxDesc=Math.max(maxDesc,r.getMaxY()); maxW=Math.max(maxW,r.getWidth());
            }
        }
        double fV=(H*0.94-2*RING)/(maxAsc+maxDesc), fH=(H*0.92-2*RING)/maxW, fac=Math.min(fV,fH);
        FS=(float)(probe*fac);
        double ascF=maxAsc*fac, descF=maxDesc*fac;
        BASE_Y=(float)((H-(ascF+descF+2*RING))/2+ascF+RING);
    }
    static BufferedImage letterTile(String ch, boolean connectLeft, boolean connectRight, int bg) {
        String text=(connectRight?""+ZWJ:"")+ch+(connectLeft?""+ZWJ:"");
        BufferedImage img=new BufferedImage(H,H,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        Boolean dir=TextAttribute.RUN_DIRECTION_RTL; Color fg=new Color(FG,true);
        FontRenderContext frc=g.getFontRenderContext();
        TextLayout tl=RenderPreview.layout(text,BASE.deriveFont(FS),dir,fg,frc,0f);
        Shape raw=tl.getOutline(null); Rectangle2D b=raw.getBounds2D();
        double tx=H/2.0-(b.getX()+b.getWidth()/2), ty=BASE_Y;
        Shape outline=AffineTransform.getTranslateInstance(tx,ty).createTransformedShape(raw);
        Area shape=new Area(outline);
        double[] tb=tatweelBand(frc); double yTop=ty+tb[0], barH=tb[1]-tb[0];
        Area gib=new Area(outline);
        gib.intersect(new Area(new Rectangle2D.Double(-RING,yTop,H+2*RING,barH)));
        Rectangle2D gb=gib.getBounds2D();
        double bl=gb.isEmpty()?H/2.0:gb.getMinX(), br=gb.isEmpty()?H/2.0:gb.getMaxX();
        if (connectLeft)  shape.add(new Area(new Rectangle2D.Double(-RING,yTop,bl+RING,barH)));
        if (connectRight) shape.add(new Area(new Rectangle2D.Double(br,yTop,(H-br)+RING,barH)));
        g.setColor(new Color(0xFF000000,true));
        g.setStroke(new BasicStroke(2f*RING,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.fill(shape); g.draw(shape);
        g.setColor(fg);
        g.setStroke(new BasicStroke(2f*WHITE,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.fill(shape); g.draw(shape); g.dispose();
        BufferedImage tile=new BufferedImage(CELL,CELL,BufferedImage.TYPE_INT_ARGB);
        Graphics2D gt=tile.createGraphics();
        gt.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        gt.setColor(new Color(bg,true)); gt.fillRect(0,0,CELL,CELL);
        gt.drawImage(img,0,0,CELL,CELL,null); gt.dispose();
        return tile;
    }
    static double[] tatweelBand(FontRenderContext frc) {
        Rectangle2D r=RenderPreview.layout("ـ",BASE.deriveFont(FS),
                TextAttribute.RUN_DIRECTION_RTL,Color.WHITE,frc,0f).getOutline(null).getBounds2D();
        return new double[]{ r.getMinY(), r.getMaxY() };
    }
}
