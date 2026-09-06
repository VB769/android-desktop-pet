package com.vb769.desktoppet;

import android.graphics.*;

/** Original vector cat. All movements stay inside a fixed 160 x 160 canvas. */
final class CatRenderer {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final Shader fur=new LinearGradient(0, 44, 0, 142,
        new int[]{0xFFFFFCF6, 0xFFF6EAF2}, null, Shader.TileMode.CLAMP);
    private static final int OUTLINE=0xFF9B829F, LAVENDER=0xFFC1ACED, INK=0xFF51425F;

    private void fill(int color) { p.setShader(null); p.setColor(color); p.setStyle(Paint.Style.FILL); }
    private void stroke(int color, float width) {
        fill(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(width);
        p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
    }
    private void furPath(Canvas c) {
        fill(Color.WHITE); p.setShader(fur); c.drawPath(path,p);
        stroke(OUTLINE,2.0f); c.drawPath(path,p);
    }
    private void oval(Canvas c,float l,float t,float r,float b,int color,boolean outline) {
        fill(color); c.drawOval(l,t,r,b,p);
        if(outline){stroke(OUTLINE,1.8f); c.drawOval(l,t,r,b,p);}
    }
    private void star(Canvas c,float x,float y,float radius,int color) {
        path.reset(); path.moveTo(x,y-radius); path.lineTo(x+radius*.32f,y-radius*.3f);
        path.lineTo(x+radius,y); path.lineTo(x+radius*.32f,y+radius*.3f);
        path.lineTo(x,y+radius); path.lineTo(x-radius*.32f,y+radius*.3f);
        path.lineTo(x-radius,y); path.lineTo(x-radius*.32f,y-radius*.3f); path.close();
        fill(color); c.drawPath(path,p);
    }
    private void heart(Canvas c,float x,float y,float scale) {
        c.save(); c.translate(x,y); c.scale(scale,scale);
        path.reset(); path.moveTo(0,7); path.cubicTo(-15,-2,-7,-12,0,-5);
        path.cubicTo(7,-12,15,-2,0,7); fill(0xFFF18BAC); c.drawPath(path,p); c.restore();
    }
    private void arm(Canvas c,float x,float y,float angle,boolean pads) {
        c.save(); c.rotate(angle,x,y);
        // Short, mitten-shaped paws instead of long articulated legs.
        oval(c,x-8,y-3,x+8,y+12,0xFFFFFBF5,true);
        oval(c,x-9,y+4,x+9,y+18,0xFFFFF5F6,true);
        if(pads) {
            oval(c,x-4,y+10,x+4,y+15,0xFFF3B6C9,false);
            fill(0xFFF3B6C9); c.drawCircle(x-4,y+7,1.6f,p); c.drawCircle(x,y+5,1.6f,p); c.drawCircle(x+4,y+7,1.6f,p);
        } else {
            stroke(0xFFCFB8CC,1); c.drawLine(x-2,y+12,x-2,y+16,p); c.drawLine(x+2,y+12,x+2,y+16,p);
        }
        c.restore();
    }
    void draw(Canvas c,int width,int height,PetMotion.Action action,float progress,long now,boolean dragging) {
        c.save(); c.scale(width/160f,height/160f);
        float t=Math.max(0,Math.min(1,progress));
        float envelope=Math.min(1f,Math.min(t/0.18f,(1-t)/0.18f));
        float wave=(float)Math.sin(t*Math.PI*8);
        float breathe=(float)Math.sin(now/620.0);
        float jump=action==PetMotion.Action.JUMP ? (float)Math.abs(Math.sin(t*Math.PI*3))*18 : 0;
        float stretch=action==PetMotion.Action.STRETCH ? (float)Math.sin(t*Math.PI) : 0;
        float sleep=action==PetMotion.Action.SLEEP ? envelope : 0;
        boolean delighted=action==PetMotion.Action.HAPPY;
        float happy=delighted ? envelope : 0;
        float sway=(float)Math.sin(now/(delighted?120.0:650.0));
        oval(c,45+jump*.4f,147,121-jump*.4f,155,0x226E588A,false);
        c.save();
        c.translate(0,-jump + breathe*.6f);
        c.scale(1-stretch*.08f+sleep*.08f,1+stretch*.10f-sleep*.16f,80,145);
        if(dragging)c.rotate((float)Math.sin(now/130.0)*4,80,72);

        // A short curled tail behind the tiny body.
        path.reset(); path.moveTo(106,137);
        path.cubicTo(139,146,144,122+sway*5,128+sway*3,123);
        stroke(OUTLINE,16); c.drawPath(path,p);
        stroke(LAVENDER,13); c.drawPath(path,p);
        stroke(0xFFEAE0FA,5); c.drawPath(path,p);

        // Squat mochi body: the head is roughly three times its visible height.
        path.reset(); path.moveTo(64,106);
        path.cubicTo(48,115,48,138,60,146);
        path.cubicTo(70,153,101,153,112,144);
        path.cubicTo(119,136,115,115,98,106); path.close(); furPath(c);
        oval(c,66,119,102,147,0xFFFFFEFA,false);
        oval(c,49,139,76,151,0xFFFFF5F6,true);
        oval(c,94,139,121,151,0xFFFFF5F6,true);

        // Large round head, soft cheeks, small rounded triangular ears.
        c.save(); c.rotate(sleep*6 + (action==PetMotion.Action.WAVE ? wave*1.5f : 0),80,103);
        path.reset(); path.moveTo(33,62);
        path.cubicTo(31,51,31,35,38,35);
        path.cubicTo(45,35,52,45,57,48);
        path.cubicTo(69,43,94,43,106,48);
        path.cubicTo(112,43,120,33,126,36);
        path.cubicTo(132,41,130,54,129,63);
        path.cubicTo(141,73,145,85,141,99);
        path.cubicTo(137,117,117,124,82,125);
        path.cubicTo(47,125,24,117,21,99);
        path.cubicTo(18,85,23,72,33,62); path.close(); furPath(c);

        // Warm blush ears and small lavender forehead tufts.
        path.reset(); path.moveTo(38,42); path.quadTo(40,38,50,51);
        path.quadTo(45,57,37,59); path.close();
        fill(0xFFF4B9C9); c.drawPath(path,p);
        path.reset(); path.moveTo(123,43); path.quadTo(122,39,113,51);
        path.quadTo(119,58,126,59); path.close(); c.drawPath(path,p);
        path.reset(); path.moveTo(72,47); path.quadTo(72,59,78,56);
        path.quadTo(82,54,82,47);
        path.moveTo(85,47); path.quadTo(87,58,91,55); path.quadTo(94,53,92,48);
        stroke(0xFFD9C9EF,3.4f); c.drawPath(path,p);

        // Low-set round eyes, a tiny mouth and broad pink cheeks.
        boolean closed=sleep>.3f || happy>.15f || now%4600<150;
        for(int eyeX : new int[]{55,109}) {
            if(closed) {
                stroke(INK,3f);
                c.drawArc(eyeX-9,84,eyeX+9,95,delighted?200:20,140,false,p);
            } else {
                oval(c,eyeX-10,77,eyeX+10,99,INK,false);
                oval(c,eyeX-7,87,eyeX+7,97,0xFFB79AD8,false);
                oval(c,eyeX-4,79,eyeX+4,94,INK,false);
                fill(Color.WHITE); c.drawCircle(eyeX-3.5f,81.5f,4,p); c.drawCircle(eyeX+4.5f,92,1.9f,p);
            }
        }
        oval(c,30,99,52,109,0x99F3B4C7,false); oval(c,112,99,134,109,0x99F3B4C7,false);
        fill(0xAAFFFCF8);c.drawCircle(36,102,1.8f,p);c.drawCircle(118,102,1.8f,p);
        path.reset(); path.moveTo(78,99); path.quadTo(82,97,86,99); path.quadTo(85,102,82,103); path.close();
        fill(0xFFD493A9); c.drawPath(path,p);
        stroke(INK,1.8f); c.drawLine(82,102,82,105,p);
        c.drawArc(74,101,82,110,0,155,false,p); c.drawArc(82,101,90,110,25,155,false,p);
        if(action==PetMotion.Action.JUMP) oval(c,79,109,85,114,0xFFECA0B7,false);
        // Just two short whisker strokes, so the face stays soft at phone size.
        stroke(0xFFC5B0C9,1.2f);
        c.drawLine(26,95,35,97,p); c.drawLine(128,97,137,95,p);
        c.restore();

        // Tiny mint collar and golden bell peek out under the cheeks.
        fill(0xFFADE0D2); c.drawRoundRect(65,121,102,126,3,3,p);
        oval(c,77,123,87,133,0xFFF3D085,false);
        stroke(0xFFC29C5C,1);c.drawLine(82,128,82,131,p);
        float leftAngle=action==PetMotion.Action.WAVE ? (112+wave*24)*envelope :
            stretch*145 + happy*20;
        float rightAngle=-stretch*145-happy*20;
        arm(c,59,128,leftAngle,action==PetMotion.Action.WAVE || stretch>.5f);
        arm(c,106,128,rightAngle,stretch>.5f);
        c.restore();

        // Action accents stay within the same window.
        if(action==PetMotion.Action.WAVE) star(c,24,55+wave*3,4*envelope,0xFFE9C46F);
        if(action==PetMotion.Action.JUMP) {
            stroke(0xFFA0CFCA,2);
            c.drawLine(28,125-jump/2,28,131-jump/2,p); c.drawLine(134,131-jump/2,134,138-jump/2,p);
        }
        if(delighted) {
            heart(c,26,51-(t*18)%15,.6f*envelope);
            heart(c,137,38-(t*12)%10,.45f*envelope);
        }
        if(sleep>.2f) {
            fill(0xFF8C7AB7); p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            p.setTextSize(13); c.drawText("z",126,58-(t*12)%8,p);
            p.setTextSize(18); c.drawText("Z",137,42-(t*9)%8,p);
        }
        c.restore();
    }
}
