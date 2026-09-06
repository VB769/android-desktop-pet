package com.vb769.desktoppet;

import android.content.res.Resources;
import android.graphics.*;

/** Reference-inspired chibi character. Chroma key is applied once on resource load. */
final class CatRenderer {
    private static Atlas shared;
    private final Atlas atlas;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF target=new RectF();
    CatRenderer(Resources resources) {
        synchronized(CatRenderer.class) {
            if(shared==null)shared=new Atlas(resources);
            atlas=shared;
        }
    }
    // The authored sprite resource has an opaque green matte. Only saturated
    // green is removed; neutral white hair and blue eyes retain their opacity.
    static int keyPixel(int pixel) {
        int r=Color.red(pixel),g=Color.green(pixel),b=Color.blue(pixel);
        int excess=g-Math.max(r,b);
        if(excess>=60)return Color.TRANSPARENT;
        if(excess<=8)return pixel;
        float alpha=(60-excess)/52f;
        int a=Math.round(Color.alpha(pixel)*alpha);
        return Color.argb(a,r,Math.min(g,Math.max(r,b)),b);
    }
    static int frame(PetMotion.Action action,float progress,long now) {
        switch(action) {
            case WAVE: return 2+((int)(progress*12)%2);
            case JUMP: return 4+((progress*3)%1f < .22f?0:1);
            case STRETCH: return (progress<.3f || progress>.82f)?6:7;
            case SLEEP: return 8+(int)((now/1100)%2);
            case HAPPY: return 10+(int)((now/350)%2);
            default: return now%4600<160?1:0;
        }
    }
    void draw(Canvas c,int width,int height,PetMotion.Action action,float progress,long now,boolean dragging) {
        int index=frame(action,Math.max(0,Math.min(.9999f,progress)),now);
        Rect source=atlas.frames[index];
        float scale=128f/atlas.largest;
        float w=source.width()*scale,h=source.height()*scale;
        float jump=action==PetMotion.Action.JUMP?(float)Math.abs(Math.sin(progress*Math.PI*3))*10:0;
        float bob=(float)Math.sin(now/650.0)*.6f;
        target.set(80-w/2,147-h-jump+bob,80+w/2,147-jump+bob);
        c.save();c.scale(width/160f,height/160f);
        if(dragging)c.rotate((float)Math.sin(now/140.0)*3,80,100);
        else if(action==PetMotion.Action.HAPPY)c.rotate((float)Math.sin(now/200.0)*1.2f,80,145);
        c.drawBitmap(atlas.bitmap,source,target,paint);
        c.restore();
    }
    private static final class Atlas {
        final Bitmap bitmap;
        final Rect[] frames=new Rect[12];
        int largest;
        Atlas(Resources resources) {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inScaled=false;
            Bitmap raw=BitmapFactory.decodeResource(resources,R.drawable.anime_cat_sheet,options);
            if(raw==null)throw new IllegalStateException("Missing anime character resource");
            int width=raw.getWidth(),height=raw.getHeight();
            int[] pixels=new int[width*height];raw.getPixels(pixels,0,width,0,0,width,height);raw.recycle();
            for(int i=0;i<pixels.length;i++)pixels[i]=keyPixel(pixels[i]);
            bitmap=Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888);
            // Separators sit in the authored transparent gutters. The top row's
            // boots reach below one third of the image, so equal thirds clip them.
            int[] rows={0,Math.round(height*377f/1086f),Math.round(height*724f/1086f),height};
            for(int i=0;i<12;i++) {
                int left=(i%4)*width/4,right=(i%4+1)*width/4;
                int top=rows[i/4],bottom=rows[i/4+1];
                int minX=right,minY=bottom,maxX=left,maxY=top;
                for(int y=top;y<bottom;y++)for(int x=left;x<right;x++) {
                    if(Color.alpha(pixels[y*width+x])>20) {
                        minX=Math.min(minX,x);minY=Math.min(minY,y);
                        maxX=Math.max(maxX,x+1);maxY=Math.max(maxY,y+1);
                    }
                }
                if(minX>=maxX || minY>=maxY)throw new IllegalStateException("Empty character frame "+i);
                frames[i]=new Rect(minX,minY,maxX,maxY);
                largest=Math.max(largest,Math.max(maxX-minX,maxY-minY));
            }
        }
    }
}
