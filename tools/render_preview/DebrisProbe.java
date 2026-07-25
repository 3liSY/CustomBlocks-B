/**
 * DebrisProbe.java — one-shot diagnostic for the G10 §H debris sweep gate. For every foreground
 * island that survives the current pipeline stages 1-2, print its area, max depth into the
 * background, and the min/mean/max ΔE00 of its pixels to the background colour — so the sweep's
 * colour bound is chosen from measured debris vs measured legitimate content, not guessed.
 */
import com.customblocks.image.CieDe2000;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public final class DebrisProbe {
    static final int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}};

    public static void main(String[] args) throws Exception {
        File f = new File(args[0]);
        double tol = 30.0 / 100.0 * 14.2, weak = tol * 2.0;
        BufferedImage src = ImageIO.read(f);
        BufferedImage img = toArgb(src);
        int w = img.getWidth(), h = img.getHeight();
        int bg = corner(img, w, h);
        if (((bg >>> 24) & 0xFF) < 128) { System.out.println("transparent bg — n/a"); return; }
        double[] bgLab = lab(bg);
        HashMap<Integer, Double> memo = new HashMap<>();

        // Stage 1: hysteresis flood (same rules as the real pipeline).
        boolean[][] isBg = new boolean[w][h];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int x = 0; x < w; x++) for (int y : new int[]{0, h-1})
            if (!isBg[x][y] && de(img, x, y, bgLab, memo) <= tol) { isBg[x][y]=true; q.add(new int[]{x,y}); }
        for (int y = 1; y < h-1; y++) for (int x : new int[]{0, w-1})
            if (!isBg[x][y] && de(img, x, y, bgLab, memo) <= tol) { isBg[x][y]=true; q.add(new int[]{x,y}); }
        while (!q.isEmpty()) {
            int[] p = q.poll();
            for (int[] d : DIRS) {
                int nx=p[0]+d[0], ny=p[1]+d[1];
                if (nx>=0&&nx<w&&ny>=0&&ny<h&&!isBg[nx][ny]&&de(img,nx,ny,bgLab,memo)<=weak) {
                    isBg[nx][ny]=true; q.add(new int[]{nx,ny});
                }
            }
        }

        // Depth transform: city-block distance of fg pixels to nearest bg.
        int INF = 1<<28;
        int[][] dist = new int[w][h];
        for (int x=0;x<w;x++) for (int y=0;y<h;y++) {
            if (isBg[x][y]) { dist[x][y]=0; continue; }
            int up = y>0?dist[x][y-1]:INF, left = x>0?dist[x-1][y]:INF;
            dist[x][y] = Math.min(INF, Math.min(up,left)+1);
        }
        for (int x=w-1;x>=0;x--) for (int y=h-1;y>=0;y--) {
            if (isBg[x][y]) continue;
            int down = y<h-1?dist[x][y+1]:INF, right = x<w-1?dist[x+1][y]:INF;
            dist[x][y] = Math.min(dist[x][y], Math.min(INF, Math.min(down,right)+1));
        }

        // Islands + stats. Print the ones a sweep would even look at (small-ish), plus the biggest.
        boolean[][] seen = new boolean[w][h];
        record Isle(int area, int maxDepth, double dMin, double dMean, double dMax) {}
        List<Isle> isles = new ArrayList<>();
        for (int x0=0;x0<w;x0++) for (int y0=0;y0<h;y0++) {
            if (isBg[x0][y0]||seen[x0][y0]) continue;
            ArrayDeque<int[]> bq = new ArrayDeque<>(); seen[x0][y0]=true; bq.add(new int[]{x0,y0});
            int area=0, maxD=0; double dmin=1e9, dmax=0, dsum=0;
            while (!bq.isEmpty()) {
                int[] p = bq.poll(); area++;
                maxD = Math.max(maxD, dist[p[0]][p[1]]);
                double d = de(img, p[0], p[1], bgLab, memo);
                dmin=Math.min(dmin,d); dmax=Math.max(dmax,d); dsum+=d;
                for (int[] d2 : DIRS) {
                    int nx=p[0]+d2[0], ny=p[1]+d2[1];
                    if (nx>=0&&nx<w&&ny>=0&&ny<h&&!isBg[nx][ny]&&!seen[nx][ny]) { seen[nx][ny]=true; bq.add(new int[]{nx,ny}); }
                }
            }
            isles.add(new Isle(area, maxD, dmin, dsum/area, dmax));
        }
        if (args.length >= 5) { // region dump mode: '.'=bg, digit=fg dE00/10 (9 = >=90)
            int rx0=Integer.parseInt(args[1]), ry0=Integer.parseInt(args[2]),
                rx1=Integer.parseInt(args[3]), ry1=Integer.parseInt(args[4]);
            for (int y=ry0; y<=ry1 && y<h; y++) {
                StringBuilder sb = new StringBuilder();
                for (int x=rx0; x<=rx1 && x<w; x++) {
                    if (isBg[x][y]) sb.append('.');
                    else sb.append((char)('0'+Math.min(9,(int)(de(img,x,y,bgLab,memo)/10))));
                }
                System.out.println(sb);
            }
            return;
        }
        isles.sort((a,b)->Integer.compare(b.area(), a.area()));
        System.out.println(f.getName()+"  "+w+"x"+h+"  islands="+isles.size());
        System.out.println("area     maxDepth  dE00 min/mean/max");
        for (int i=0;i<isles.size();i++) {
            Isle il = isles.get(i);
            if (i<5 || il.maxDepth()<=8) // biggest 5, plus every shallow island
                System.out.printf("%-8d %-9d %.1f / %.1f / %.1f%n", il.area(), il.maxDepth(), il.dMin(), il.dMean(), il.dMax());
            if (i>60) { System.out.println("…"); break; }
        }
    }

    static double de(BufferedImage img, int x, int y, double[] bgLab, HashMap<Integer,Double> memo) {
        int argb = img.getRGB(x,y);
        int a = (argb>>>24)&0xFF;
        if (a<128) return 0; // transparent counts as bg
        int key = argb|0xFF000000;
        Double v = memo.get(key);
        if (v==null) { v = CieDe2000.of(lab(key), bgLab); if (memo.size()<(1<<17)) memo.put(key,v); }
        return v;
    }
    static int corner(BufferedImage img,int w,int h){
        List<Integer> s=new ArrayList<>();
        int[][] cs={{0,0},{Math.max(0,w-3),0},{0,Math.max(0,h-3)},{Math.max(0,w-3),Math.max(0,h-3)}};
        for(int[] c:cs) for(int dx=0;dx<3&&c[0]+dx<w;dx++) for(int dy=0;dy<3&&c[1]+dy<h;dy++) s.add(img.getRGB(c[0]+dx,c[1]+dy));
        Collections.sort(s); return s.get(s.size()/2);
    }
    static BufferedImage toArgb(BufferedImage s){
        if (s.getType()==BufferedImage.TYPE_INT_ARGB) return s;
        BufferedImage o=new BufferedImage(s.getWidth(),s.getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=o.createGraphics(); g.drawImage(s,0,0,null); g.dispose(); return o;
    }
    static double[] lab(int argb){
        double r=((argb>>16)&0xFF)/255.0,g=((argb>>8)&0xFF)/255.0,b=(argb&0xFF)/255.0;
        r=r>0.04045?Math.pow((r+0.055)/1.055,2.4):r/12.92;
        g=g>0.04045?Math.pow((g+0.055)/1.055,2.4):g/12.92;
        b=b>0.04045?Math.pow((b+0.055)/1.055,2.4):b/12.92;
        r*=100;g*=100;b*=100;
        double x=r*0.4124+g*0.3576+b*0.1805, y=r*0.2126+g*0.7152+b*0.0722, z=r*0.0193+g*0.1192+b*0.9505;
        x/=95.047;y/=100.0;z/=108.883;
        x=x>0.008856?Math.cbrt(x):7.787*x+16.0/116.0;
        y=y>0.008856?Math.cbrt(y):7.787*y+16.0/116.0;
        z=z>0.008856?Math.cbrt(z):7.787*z+16.0/116.0;
        return new double[]{116*y-16,500*(x-y),200*(y-z)};
    }
}
