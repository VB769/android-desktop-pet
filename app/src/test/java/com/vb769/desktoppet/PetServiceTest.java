package com.vb769.desktoppet;

import android.content.*;
import android.view.*;
import java.lang.reflect.Field;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSettings;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w480dp-h800dp-mdpi")
public class PetServiceTest {
    ServiceController<PetService> controller;
    PetService service;
    SharedPreferences prefs;
    private Object field(String name) throws Exception {
        Field f=PetService.class.getDeclaredField(name);f.setAccessible(true);return f.get(service);
    }
    @Before public void setup() {
        prefs=RuntimeEnvironment.getApplication().getSharedPreferences("pet",0);
        prefs.edit().clear().putInt("freeX",100).putInt("freeY",200).putBoolean("autoActions",false).commit();
        ShadowSettings.setCanDrawOverlays(true);
        controller=Robolectric.buildService(PetService.class).create();service=controller.get();
        service.onStartCommand(new Intent(),0,1);
    }
    @After public void close(){controller.destroy();}
    @Test public void draggingIntoMiddleDoesNotSnapAndSurvivesRestart() throws Exception {
        View view=(View)field("cat");assertNotNull(view);
        WindowManager.LayoutParams p=(WindowManager.LayoutParams)field("params");
        int before=p.x;
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP}) {
            float x=action==MotionEvent.ACTION_DOWN?10:40;
            MotionEvent e=MotionEvent.obtain(0,0,action,x,10,0);view.onTouchEvent(e);e.recycle();
        }
        assertEquals(before+30,p.x);
        assertEquals(p.x,prefs.getInt("freeX",-1));
        int saved=p.x;
        controller.destroy();
        controller=Robolectric.buildService(PetService.class).create();service=controller.get();
        service.onStartCommand(new Intent(),0,1);
        assertEquals(saved,((WindowManager.LayoutParams)field("params")).x);
    }
    @Test public void foldingRestoresBothCoordinates() throws Exception {
        WindowManager.LayoutParams p=(WindowManager.LayoutParams)field("params");
        int x=p.x,y=p.y;
        prefs.edit().putBoolean("folded",true).commit();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(x,prefs.getInt("freeX",-1));
        prefs.edit().putBoolean("folded",false).commit();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(x,p.x);assertEquals(y,p.y);
    }
    @Test public void tapShowsWordsAndStartsActionInsteadOfMenu() throws Exception {
        ((View)field("cat")).performClick();
        assertNotNull(field("bubble"));assertNull(field("menu"));
        PetMotion m=(PetMotion)field("motion");
        assertNotEquals(PetMotion.Action.IDLE,m.current(android.os.SystemClock.uptimeMillis()));
    }
}
