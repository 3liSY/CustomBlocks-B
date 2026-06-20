/**
 * BeforeAfterPreview.java — SIMPLE before/after of the isolated-letter fix on real words.
 * Before = current (floaty short letters). After = centroid-lowered (short letters sit grounded;
 * tailed/tall letters unchanged). Connected letters untouched. NOT shipped.
 *
 *   "$JAVA_HOME/bin/javac" -encoding UTF-8 -d tools/render_preview \
 *       src/main/java/com/customblocks/arabic/ArabicJoining.java \
 *       tools/render_preview/RenderPreview.java tools/render_preview/BeforeAfterPreview.java
 *   "$JAVA_HOME/bin/java" -cp tools/render_preview BeforeAfterPreview
 */
import com.customblocks.arabic.ArabicJoining;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class BeforeAfterPreview {
    static final char ZWJ='‍';
    static final int CELL=256, SS=4, H=CELL*SS, FG=0xFFFFFFFF;
    static final float RING=H*(12f/256f), WHITE=H*(3f/256f);
    static final double DROP=0.09;      // grounding nudge for SHORT no-descender floaters (dal/dhal/ta-marbuta…)
    static final double TALL_DROP=0.03; // tiny nudge for TALL no-descender letters (alef family)
    static final int[] BGS={0xFF0A0A0A,0xFFFF0000,0xFFF0C814,0xFF1E8C1E};
    static final String[] ALL28={"ا","ب","ت","ث","ج","ح","خ","د","ذ","ر","ز","س","ش","ص","ض","ط","ظ","ع","غ","ف","ق","ك","ل","م","ن","ه","و","ي"};
    static final String[][] WORDS={ {"داود","Dawud  (alef + dal float)"}, {"مكة","Makkah (ta-marbuta floats)"} };
    static Font BASE; static float FS, BASE_Y, DESC_F;

    public static void main(String[] a) throws Exception {
        System.setProperty("java.awt.headless","true");
        BASE=RenderPreview.ARABIC_FONT=RenderPreview.loadFont(RenderPreview.RES+"arabtype.ttf");
        new File("tools/render_preview/out").mkdirs();
        metrics();
        int D=150, PAD=18, TITLE=30, WL=18, RL=16, RGAP=22, WGAP=20;
        int maxLen=0; for(String[] w:WORDS) maxLen=Math.max(maxLen,w[0].length());
        int rowH=RL+D+10;
        int wblock=WL+2*rowH+WGAP;
        int W=PAD*2+maxLen*D, Ht=TITLE+WORDS.length*wblock+PAD;
        BufferedImage cv=new BufferedImage(W,Ht,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=cv.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x222222)); g.fillRect(0,0,W,Ht);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,15));
        g.drawString("BEFORE vs AFTER — short floaty letters sit grounded; tall/tailed unchanged. (read right-to-left)",PAD,20);
        int y=TITLE;
        for(String[] wd:WORDS){
            g.setColor(new Color(0xCFE8CF)); g.setFont(new Font("SansSerif",Font.BOLD,13));
            g.drawString(wd[1],PAD,y+14);
            String[] tags={"BEFORE","AFTER"};
            for(int v=0;v<2;v++){
                int yr=y+WL+v*rowH;
                g.setColor(v==0?new Color(0xE08A8A):new Color(0x8AE08A));
                g.setFont(new Font("SansSerif",Font.BOLD,12));
                g.drawString(tags[v],PAD,yr+12);
                char[] L=wd[0].toCharArray(); int n=L.length;
                for(int vis=0;vis<n;vis++){
                    int i=n-1-vis; char self=L[i];
                    char right=(i-1>=0)?L[i-1]:0, left=(i+1<n)?L[i+1]:0;
                    int form=ArabicJoining.form(self,right,left);
                    boolean cl=form==ArabicJoining.INITIAL||form==ArabicJoining.MEDIAL;
                    boolean cr=form==ArabicJoining.FINAL||form==ArabicJoining.MEDIAL;
                    boolean iso=form==ArabicJoining.ISOLATED;
                    boolean fix=(v==1)&&iso;
                    int x=PAD+vis*D, ty=yr+RL;
                    g.drawImage(tile(String.valueOf(self),cl,cr,BGS[vis%BGS.length],fix),x,ty,D,D,null);
                    g.setColor(new Color(0x555555)); g.drawRect(x,ty,D,D);
                }
            }
            y+=wblock;
        }
        g.dispose();
        ImageIO.write(cv,"png",new File("tools/render_preview/out/BEFORE_AFTER.png"));
        System.out.println("Wrote BEFORE_AFTER.png");
        edges();
    }

    /** Down-nudge (working px) for an ISOLATED glyph: short no-descender floaters get DROP, tall
     *  no-descender (alef family) gets a tiny TALL_DROP, anything with a real descender stays put. */
    static double isoDrop(Shape placed){
        Rectangle2D ob=placed.getBounds2D(); double desc=ob.getMaxY()-BASE_Y, ht=ob.getHeight();
        if(desc>H*0.02) return 0;                       // has a tail/descender → leave it
        return (ht<H*0.62) ? H*DROP : H*TALL_DROP;      // short → big nudge; tall (alef) → tiny nudge
    }

    /** One-glance grid of tricky ISOLATED letters, BEFORE (top) vs AFTER (bottom). */
    static void edges() throws Exception {
        String[][] E={ {"ا","alef  (tall→tiny)"},{"ء","hamza (floats)"},{"ى","maqsura (tail→stays)"},
                       {"ه","ha   (short→drop)"},{"ة","ta-marbuta (drop)"},{"ع","ain  (bowl→stays)"} };
        int D=150, PAD=18, TOP=46, LAB=22, ROW=24;
        int W=PAD*2+E.length*D, Ht=TOP+LAB+2*(ROW+D)+PAD;
        BufferedImage cv=new BufferedImage(W,Ht,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=cv.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x222222)); g.fillRect(0,0,W,Ht);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,15));
        g.drawString("EDGE CASES — isolated letters, BEFORE vs AFTER (each its own column)",PAD,22);
        g.setFont(new Font("SansSerif",Font.PLAIN,11)); g.setColor(new Color(0xBBBBBB));
        g.drawString("alef nudged a touch; short round letters drop; tailed/bowled letters do NOT move",PAD,40);
        for(int c=0;c<E.length;c++){
            int x=PAD+c*D;
            g.setColor(new Color(0xCFE8CF)); g.setFont(new Font("SansSerif",Font.BOLD,11));
            g.drawString(E[c][1],x+2,TOP+14);
        }
        String[] tags={"BEFORE","AFTER"};
        for(int v=0;v<2;v++){
            int yr=TOP+LAB+v*(ROW+D);
            g.setColor(v==0?new Color(0xE08A8A):new Color(0x8AE08A)); g.setFont(new Font("SansSerif",Font.BOLD,12));
            g.drawString(tags[v],PAD,yr+14);
            for(int c=0;c<E.length;c++){
                int x=PAD+c*D, ty=yr+ROW;
                g.drawImage(tile(E[c][0],false,false,BGS[c%BGS.length],v==1),x,ty,D,D,null);
                g.setColor(new Color(0x555555)); g.drawRect(x,ty,D,D);
            }
        }
        g.dispose();
        ImageIO.write(cv,"png",new File("tools/render_preview/out/EDGE_CASES.png"));
        System.out.println("Wrote EDGE_CASES.png");

        // Each edge letter shown inside a REAL word where it lands isolated (so the drop actually fires).
        renderWords(new String[][]{
            {"اسد","asad   — ALEF isolated (word start) → tiny drop"},
            {"ماء","maa    — HAMZA isolated (end)"},
            {"هدى","huda   — MAQSURA isolated (tail → stays)"},
            {"شاه","shah   — HA round isolated (short → drop)"},
            {"حياة","hayah  — TA-MARBUTA isolated (drop)"},
            {"ذراع","dhiraa — dhal+ra+alef+AIN all isolated (mixed, beside ain)"},
        }, "WORD_EDGES.png", "EDGE LETTERS IN WORDS — BEFORE vs AFTER (read right-to-left)");
    }

    /** Stacked BEFORE/AFTER word rows for an arbitrary word list → {@code out}. */
    static void renderWords(String[][] words, String out, String title) throws Exception {
        int D=150, PAD=18, TITLE=30, WL=18, RL=16, WGAP=20;
        int maxLen=0; for(String[] w:words) maxLen=Math.max(maxLen,w[0].length());
        int rowH=RL+D+10, wblock=WL+2*rowH+WGAP;
        int W=PAD*2+maxLen*D, Ht=TITLE+words.length*wblock+PAD;
        BufferedImage cv=new BufferedImage(W,Ht,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=cv.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x222222)); g.fillRect(0,0,W,Ht);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,15));
        g.drawString(title,PAD,20);
        int y=TITLE;
        for(String[] wd:words){
            g.setColor(new Color(0xCFE8CF)); g.setFont(new Font("SansSerif",Font.BOLD,13));
            g.drawString(wd[1],PAD,y+14);
            String[] tags={"BEFORE","AFTER"};
            for(int v=0;v<2;v++){
                int yr=y+WL+v*rowH;
                g.setColor(v==0?new Color(0xE08A8A):new Color(0x8AE08A));
                g.setFont(new Font("SansSerif",Font.BOLD,12)); g.drawString(tags[v],PAD,yr+12);
                char[] L=wd[0].toCharArray(); int n=L.length;
                for(int vis=0;vis<n;vis++){
                    int i=n-1-vis; char self=L[i];
                    char right=(i-1>=0)?L[i-1]:0, left=(i+1<n)?L[i+1]:0;
                    int form=ArabicJoining.form(self,right,left);
                    boolean cl=form==ArabicJoining.INITIAL||form==ArabicJoining.MEDIAL;
                    boolean cr=form==ArabicJoining.FINAL||form==ArabicJoining.MEDIAL;
                    boolean iso=form==ArabicJoining.ISOLATED;
                    int x=PAD+vis*D, ty=yr+RL;
                    g.drawImage(tile(String.valueOf(self),cl,cr,BGS[vis%BGS.length],(v==1)&&iso),x,ty,D,D,null);
                    g.setColor(new Color(0x555555)); g.drawRect(x,ty,D,D);
                }
            }
            y+=wblock;
        }
        g.dispose();
        ImageIO.write(cv,"png",new File("tools/render_preview/out/"+out));
        System.out.println("Wrote "+out);
    }

    static BufferedImage tile(String ch, boolean cl, boolean cr, int bg, boolean fix){
        String text=(cr?""+ZWJ:"")+ch+(cl?""+ZWJ:"");
        FontRenderContext frc=new FontRenderContext(null,true,true);
        Color fg=new Color(FG,true);
        Shape raw=RenderPreview.layout(text,BASE.deriveFont(FS),TextAttribute.RUN_DIRECTION_RTL,fg,frc,0f).getOutline(null);
        Rectangle2D b=raw.getBounds2D();
        double tx=H/2.0-(b.getX()+b.getWidth()/2);
        Shape placed=AffineTransform.getTranslateInstance(tx,BASE_Y).createTransformedShape(raw);
        if(fix){ double dy=isoDrop(placed);
                 if(dy>0) placed=AffineTransform.getTranslateInstance(0,dy).createTransformedShape(placed); }
        Area shape=new Area(placed);
        double[] tb=tatweel(frc); double yTop=BASE_Y+tb[0], barH=tb[1]-tb[0];
        Area inBar=new Area(placed); inBar.intersect(new Area(new Rectangle2D.Double(-RING,yTop,H+2*RING,barH)));
        Rectangle2D gb=inBar.getBounds2D();
        double bl=gb.isEmpty()?H/2.0:gb.getMinX(), br=gb.isEmpty()?H/2.0:gb.getMaxX();
        if(cl) shape.add(new Area(new Rectangle2D.Double(-RING,yTop,bl+RING,barH)));
        if(cr) shape.add(new Area(new Rectangle2D.Double(br,yTop,(H-br)+RING,barH)));
        BufferedImage img=new BufferedImage(H,H,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
        g.setColor(new Color(0xFF000000,true)); g.setStroke(new BasicStroke(2f*RING,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.fill(shape); g.draw(shape);
        g.setColor(fg); g.setStroke(new BasicStroke(2f*WHITE,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.fill(shape); g.draw(shape); g.dispose();
        BufferedImage tile=new BufferedImage(CELL,CELL,BufferedImage.TYPE_INT_ARGB);
        Graphics2D gt=tile.createGraphics();
        gt.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        gt.setColor(new Color(bg,true)); gt.fillRect(0,0,CELL,CELL);
        gt.drawImage(img,0,0,CELL,CELL,null); gt.dispose();
        return tile;
    }
    static double centroidY(Shape o){
        BufferedImage m=new BufferedImage(H,H,BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g=m.createGraphics(); g.setColor(Color.BLACK); g.fillRect(0,0,H,H);
        g.setColor(Color.WHITE); g.fill(o); g.dispose();
        long s=0,c=0; for(int y=0;y<H;y+=4)for(int x=0;x<H;x+=4) if((m.getRGB(x,y)&0xFF)>127){s+=y;c++;}
        return c==0?H/2.0:(double)s/c;
    }
    static double[] tatweel(FontRenderContext frc){
        Rectangle2D r=RenderPreview.layout("ـ",BASE.deriveFont(FS),TextAttribute.RUN_DIRECTION_RTL,Color.WHITE,frc,0f).getOutline(null).getBounds2D();
        return new double[]{r.getMinY(),r.getMaxY()};
    }
    static void metrics(){
        FontRenderContext frc=new FontRenderContext(null,true,true);
        float probe=H*0.5f; double asc=0,desc=0,ms=0;
        for(String ch:ALL28) for(String sh:new String[]{ch,ch+ZWJ,ZWJ+ch+ZWJ,ZWJ+ch}){
            Rectangle2D r=RenderPreview.layout(sh,BASE.deriveFont(probe),TextAttribute.RUN_DIRECTION_RTL,Color.WHITE,frc,0f).getOutline(null).getBounds2D();
            if(r.isEmpty()) continue;
            asc=Math.max(asc,-r.getMinY()); desc=Math.max(desc,r.getMaxY()); ms=Math.max(ms,-r.getMinY()+r.getMaxY());
        }
        double f=(H*0.90-2*RING)/ms; double aF=asc*f,dF=desc*f,t=aF+dF+2*RING;
        if(t>H*0.98){f*=(H*0.98)/t; aF=asc*f; dF=desc*f;}
        FS=(float)(probe*f); BASE_Y=(float)((H-(aF+dF+2*RING))/2+aF+RING); DESC_F=(float)dF;
    }
}
