package com.customblocks.image;
import javax.imageio.ImageIO; import java.awt.Graphics2D; import java.awt.image.BufferedImage;
import java.io.File; import java.nio.file.Files;
public final class RimProbe {
  static final int[][] DIRS={{1,0},{-1,0},{0,1},{0,-1}};
  public static void main(String[] a) throws Exception {
    File f=new File(a[0]); byte[] raw=Files.readAllBytes(f.toPath());
    byte[] flat=CheckerboardDetector.flattenToBlack(raw);
    BufferedImage src=ImageIO.read(new java.io.ByteArrayInputStream(flat));
    int w=src.getWidth(),h=src.getHeight();
    BufferedImage img=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g=img.createGraphics(); g.drawImage(src,0,0,null); g.dispose();
    int[] px=img.getRGB(0,0,w,h,null,0,w);
    BgCascade.Result r=BgCascade.decide(flat,img,w,h,null);
    if(r.mask()==null){System.out.println("declined");return;}
    boolean[][] m=r.mask();
    double[][] plate=BgPlate.build(px,m,w,h);
    System.out.println("rung "+r.rung()+"  rimDepth-scale min(w,h)/200="+Math.min(w,h)/200);
    int n=0; double sum=0,mx=0,mn=999; int within=0;
    double bar=BgRungKey.JND*2.0;
    for(int y=0;y<h;y++) for(int x=0;x<w;x++){
      if(m[x][y]) continue;
      boolean t=false;
      for(int[] d:DIRS){int nx=x+d[0],ny=y+d[1]; if(nx>=0&&ny>=0&&nx<w&&ny<h&&m[nx][ny]){t=true;break;}}
      if(!t) continue;
      int i=y*w+x;
      double de=CieDe2000.of(BackgroundRemover.rgbToLab(px[i]|0xFF000000), BgPlate.labOf(plate[i]));
      n++; sum+=de; mx=Math.max(mx,de); mn=Math.min(mn,de); if(de<=bar) within++;
      if(n<=8) System.out.printf("  (%d,%d) rgb=%06x dPlate=%.1f%n",x,y,px[i]&0xFFFFFF,de);
    }
    System.out.printf("boundary px=%d  mean dPlate=%.1f  min=%.1f max=%.1f  within %.1f = %d%n",
      n,sum/Math.max(1,n),mn,mx,bar,within);
  }
}
