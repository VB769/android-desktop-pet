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
}
