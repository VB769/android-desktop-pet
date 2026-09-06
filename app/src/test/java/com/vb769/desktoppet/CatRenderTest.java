package com.vb769.desktoppet;

import android.graphics.*;
import java.io.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CatRenderTest {
    @Test public void actionFramesStayInsideCanvasAndRenderDifferently() throws Exception {
        File dir=new File("build/reports/pet-preview");dir.mkdirs();
        Bitmap sheet=Bitmap.createBitmap(960,960,Bitmap.Config.ARGB_8888);
        Canvas board=new Canvas(sheet);board.drawColor(0xFFF4F0F8);
        Paint label=new Paint(Paint.ANTI_ALIAS_FLAG);label.setColor(0xFF51425F);label.setTextSize(18);
        int i=0;
        for(PetMotion.Action action:PetMotion.Action.values()) {
            Bitmap previous=null;
            float[][] phases={{.28f,.28f},{.02f,.12f},{.02f,.17f},{.15f,.5f},{.28f,.28f},{.28f,.28f}};
            long[][] times={{1750,50},{1750,1750},{1750,1750},{1750,1750},{2500,1750},{800,1150}};
            for(int pose=0;pose<2;pose++) {
                float phase=phases[action.ordinal()][pose];
                long now=times[action.ordinal()][pose];
                assertEquals(action.ordinal()*2+pose,CatRenderer.frame(action,phase,now));
                Bitmap b=Bitmap.createBitmap(320,320,Bitmap.Config.ARGB_8888);
                new CatRenderer(org.robolectric.RuntimeEnvironment.getApplication().getResources()).draw(new Canvas(b),320,320,action,phase,now,false);
                int filled=0;
                for(int y=0;y<320;y++)for(int x=0;x<320;x++) {
                    int alpha=Color.alpha(b.getPixel(x,y));
                    if(alpha>0)filled++;
                    if(x==0||x==319||y==0||y==319)assertEquals("Clipped: "+action,0,alpha);
                }
                assertTrue("Cat must render visible pixels",filled>1000);
                assertTrue("Background must remain transparent",filled<320*320*.70);
                for(int y=0;y<320;y++)for(int x=0;x<320;x++) {
                    int pixel=b.getPixel(x,y);
                    if(Color.alpha(pixel)>100)
                        assertTrue("Green matte leaked into rendered character",
                            Color.green(pixel)-Math.max(Color.red(pixel),Color.blue(pixel))<60);
                }
                // Twelve cells, four columns. Draw at actual 80dp and enlarged 160dp.
                int cx=(i%4)*240,cy=(i/4)*320;
                board.drawBitmap(b,null,new Rect(cx+20,cy+30,cx+220,cy+230),null);
                board.drawBitmap(b,null,new Rect(cx+80,cy+225,cx+160,cy+305),null);
                board.drawText(action.name()+" "+pose,cx+12,cy+24,label);
                try(FileOutputStream out=new FileOutputStream(new File(dir,action.name()+"-"+pose+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}
                if(previous!=null) {
                    assertFalse("Both poses must differ: "+action,previous.sameAs(b));
                    previous.recycle();
                }
                previous=b;i++;
            }
            if(previous!=null)previous.recycle();
        }
        try(FileOutputStream out=new FileOutputStream(new File(dir,"action-sheet.png"))){sheet.compress(Bitmap.CompressFormat.PNG,100,out);}
        sheet.recycle();
    }
}
