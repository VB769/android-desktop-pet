package com.vb769.desktoppet;
import org.junit.Test;
import static org.junit.Assert.*;
public class PetMotionTest {
    @Test public void everyActionEndsAndReturnsToIdle() {
        PetMotion m=new PetMotion();
        for(PetMotion.Action a:PetMotion.Action.values()) {
            if(a==PetMotion.Action.IDLE)continue;
            m.start(a,10000);
            assertEquals(a,m.current(10000+a.duration-1));
            assertEquals(PetMotion.Action.IDLE,m.current(10000+a.duration));
            assertEquals(0f,m.progress(10000+a.duration),0f);
        }
    }
    @Test public void repeatedTapsReachAllFiveActionsThenRepeat() {
        PetMotion m=new PetMotion();
        for(int i=0;i<10;i++)assertEquals(PetMotion.Action.values()[1+i%5],m.nextAction(1000+i));
    }
    @Test public void interruptedActionRestartsNewTimeline() {
        PetMotion m=new PetMotion();m.start(PetMotion.Action.SLEEP,100);
        m.start(PetMotion.Action.JUMP,500);
        assertEquals(0f,m.progress(500),0f);
        assertEquals(.5f,m.progress(1800),.001f);
        m.reset();assertEquals(PetMotion.Action.IDLE,m.current(1900));
    }
}
