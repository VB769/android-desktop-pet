package com.vb769.desktoppet;
import org.junit.Test;
import static org.junit.Assert.*;
public class SpriteFrameTest {
    @Test public void everyActionStaysWithinItsOwnPair() {
        for(PetMotion.Action a:PetMotion.Action.values())
            for(int step=0;step<1000;step++) {
                int frame=CatRenderer.frame(a,step/1000f,step*33L);
                assertTrue(frame>=a.ordinal()*2);
                assertTrue(frame<a.ordinal()*2+2);
            }
    }
    @Test public void crouchNeverFloatsAndEveryJumpReturnsToGround() {
        for(int step=0;step<=1000;step++) {
            float progress=step/1000f;
            float height=CatRenderer.jumpOffset(progress);
            assertTrue(height>=0 && height<=10.001f);
            if(CatRenderer.frame(PetMotion.Action.JUMP,progress,0)==4)
                assertEquals("Crouching feet must stay on the ground",0,height,0.001f);
        }
        assertEquals(0,CatRenderer.jumpOffset(0),0.001f);
        assertEquals(0,CatRenderer.jumpOffset(1),0.001f);
        assertTrue(CatRenderer.jumpOffset(.18f)>9);
    }
}
