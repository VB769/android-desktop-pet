package com.vb769.desktoppet;

import android.graphics.*;

/** Original vector cat. All movements stay inside a fixed 160 x 160 canvas. */
final class CatRenderer {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final Shader fur=new LinearGradient(0, 44, 0, 142,
        new int[]{0xFFFFFCF6, 0xFFF4E7F4}, null, Shader.TileMode.CLAMP);
    private static final int OUTLINE=0xFF806B95, LAVENDER=0xFFC1ACED, INK=0xFF51425F;

    private void fill(int color) { p.setShader(null); p.setColor(color); p.setStyle(Paint.Style.FILL); }
    private void stroke(int color, float width) {
        fill(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(width);
        p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
    }
    private void furPath(Canvas c) {
        fill(Color.WHITE); p.setShader(fur); c.drawPath(path,p);
        stroke(OUTLINE,1.6f); c.drawPath(path,p);
    }
    private void oval(Canvas c,float l,float t,float r,float b,int color,boolean outline) {
        fill(color); c.drawOval(l,t,r,b,p);
        if(outline){stroke(OUTLINE,1.5f); c.drawOval(l,t,r,b,p);}
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
        oval(c,x-7,y-3,x+7,y+24,0xFFFFFAF4,true);
        oval(c,x-8,y+13,x+8,y+27,0xFFF9F1F5,true);
        if(pads) {
            oval(c,x-3.5f,y+20,x+3.5f,y+25,0xFFF1B7CA,false);
            fill(0xFFF1B7CA); c.drawCircle(x-4,y+18,1.6f,p); c.drawCircle(x,y+16,1.6f,p); c.drawCircle(x+4,y+18,1.6f,p);
        } else {
            stroke(0xFFC3ADCE,1); c.drawLine(x-2,y+21,x-2,y+25,p); c.drawLine(x+2,y+21,x+2,y+25,p);
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

        // Curved, independently swaying tail.
        path.reset(); path.moveTo(108,132);
        path.cubicTo(147,142,150,105+sway*7,130+sway*4,107);
        stroke(OUTLINE,15); c.drawPath(path,p);
        stroke(LAVENDER,12); c.drawPath(path,p);
        stroke(0xFFE2D5F8,5); c.drawPath(path,p);

        // Pear-shaped body with a light belly, then back paws.
        path.reset(); path.moveTo(66,91);
        path.cubicTo(50,100,46,132,59,143);
        path.cubicTo(69,151,101,151,113,141);
        path.cubicTo(122,129,109,98,98,93); path.close(); furPath(c);
        oval(c,66,111,103,145,0xFFFFFEF9,false);
        oval(c,49,138,76,151,0xFFF9F3FF,true);
        oval(c,95,138,121,151,0xFFF9F3FF,true);

        // Rounded ears, cheek tufts and forehead form one silhouette.
        c.save(); c.rotate(sleep*8 + (action==PetMotion.Action.WAVE ? wave*1.6f : 0),80,94);
        path.reset(); path.moveTo(39,64);
        path.cubicTo(36,52,34,29,41,28);
        path.cubicTo(48,28,61,41,66,43);
        path.cubicTo(75,39,90,40,97,43);
        path.cubicTo(105,36,116,25,121,30);
        path.cubicTo(126,36,123,55,124,63);
        path.cubicTo(137,77,134,85,139,87);
        path.lineTo(130,93); path.lineTo(134,98); path.lineTo(123,100);
        path.cubicTo(115,116,50,116,39,101);
        path.lineTo(28,98); path.lineTo(33,92); path.lineTo(26,87);
        path.cubicTo(29,83,29,74,39,64); path.close(); furPath(c);

        // Lavender cap and soft inner ears.
        path.reset(); path.moveTo(42,34); path.cubicTo(43,32,56,43,59,48);
        path.cubicTo(54,58,47,64,40,66); path.close();
        fill(LAVENDER); c.drawPath(path,p);
        path.reset(); path.moveTo(117,35); path.cubicTo(122,39,117,56,118,62);
        path.lineTo(103,47); path.close(); fill(LAVENDER); c.drawPath(path,p);
        path.reset(); path.moveTo(43,40); path.lineTo(53,49); path.lineTo(43,57); path.close();
        fill(0xFFF5B8C9); c.drawPath(path,p);
        path.reset(); path.moveTo(116,41); path.lineTo(108,49); path.lineTo(117,56); path.close(); c.drawPath(path,p);
        path.reset(); path.moveTo(70,43); path.cubicTo(74,52,76,51,78,44);
        path.moveTo(81,43); path.cubicTo(84,54,87,53,89,44);
        stroke(0xFFD7C7EF,3); c.drawPath(path,p);

        // Large violet eyes with iris reflections.
        boolean closed=sleep>.3f || happy>.15f || now%4600<150;
        for(int eyeX : new int[]{58,105}) {
            if(closed) {
                stroke(INK,2.7f);
                c.drawArc(eyeX-8,73,eyeX+8,83,delighted?200:20,140,false,p);
            } else {
                oval(c,eyeX-8,66,eyeX+8,85,INK,false);
                oval(c,eyeX-5,75,eyeX+5,83,0xFFB393D9,false);
                oval(c,eyeX-3,68,eyeX+3,81,0xFF51425F,false);
                fill(Color.WHITE); c.drawCircle(eyeX-3,70,3.1f,p); c.drawCircle(eyeX+3.5f,78.5f,1.5f,p);
            }
        }
        oval(c,40,86,57,94,0x88F2A6C1,false); oval(c,108,86,125,94,0x88F2A6C1,false);
        path.reset(); path.moveTo(77,85); path.quadTo(82,82,87,85); path.quadTo(86,88,82,90); path.close();
        fill(0xFFD18AAA); c.drawPath(path,p);
        stroke(INK,1.7f); c.drawLine(82,89,82,92,p);
        c.drawArc(73,88,82,97,0,155,false,p); c.drawArc(82,88,91,97,25,155,false,p);
        if(action==PetMotion.Action.JUMP) oval(c,79,95,86,101,0xFFE69BB7,false);
        stroke(0xFFB39EC2,1.3f);
        c.drawLine(35,84,45,87,p); c.drawLine(33,93,45,92,p);
        c.drawLine(120,87,131,84,p); c.drawLine(121,92,133,93,p);
        c.restore();

        // Small mint scarf and star charm.
        fill(0xFF9EDDD0); c.drawRoundRect(60,105,108,112,4,4,p);
        path.reset(); path.moveTo(96,108); path.lineTo(109,119); path.lineTo(99,121); path.lineTo(91,111); path.close();
        fill(0xFF82C8BE); c.drawPath(path,p);
        star(c,82,114,5,0xFFE9C46F);
        float leftAngle=action==PetMotion.Action.WAVE ? (95+wave*22)*envelope :
            stretch*140 + happy*18;
        float rightAngle=-stretch*140-happy*18;
        arm(c,59,114,leftAngle,action==PetMotion.Action.WAVE || stretch>.5f);
        arm(c,107,114,rightAngle,stretch>.5f);
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
