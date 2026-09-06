package com.vb769.desktoppet;
import org.junit.Test;
import static org.junit.Assert.*;
public class PetGeometryTest {
    @Test public void dragAcrossCenterChoosesNearestEdge() {
        assertFalse(PetGeometry.nearestRight(50, 80, 0, 400));
        assertTrue(PetGeometry.nearestRight(300, 80, 0, 400));
        assertTrue(PetGeometry.nearestRight(160, 80, 0, 400));
    }
    @Test public void resizingAndFoldingPreserveRightEdge() {
        for (int width : new int[]{120, 80, 56, 40}) {
            assertEquals(400, PetGeometry.edgeX(true, 0, 400, width) + width);
            assertEquals(0, PetGeometry.edgeX(false, 0, 400, width));
        }
    }
    @Test public void insetsRemainOutsidePetArea() {
        assertEquals(30, PetGeometry.edgeX(false, 30, 780, 80));
        assertEquals(700, PetGeometry.edgeX(true, 30, 780, 80));
        assertEquals(50, PetGeometry.clamp(-100, 50, 900));
        assertEquals(900, PetGeometry.clamp(1200, 50, 900));
    }
    @Test public void rotationClampsOldBottomPosition() {
        assertEquals(320, PetGeometry.clamp(900, 24, 400 - 80));
        assertEquals(24, PetGeometry.clamp(-10, 24, 320));
    }
    @Test public void tinyWindowNeverProducesNegativePosition() {
        assertEquals(20, PetGeometry.edgeX(true, 20, 60, 80));
        assertEquals(24, PetGeometry.clamp(200, 24, 10));
    }
    @Test public void sizeLimitsRejectOutOfRangeSavedValues() {
        assertEquals(56, PetGeometry.clamp(-1, 56, 120));
        assertEquals(120, PetGeometry.clamp(999, 56, 120));
        assertEquals(80, PetGeometry.clamp(80, 56, 120));
    }
    @Test public void centerIsPreservedUnlessSnapWasEnabled() {
        assertEquals(160, PetGeometry.restingX(160,80,0,400,false));
        assertEquals(320, PetGeometry.restingX(160,80,0,400,true));
        assertEquals(73, PetGeometry.restingX(73,80,0,400,false));
    }
    @Test public void resizeKeepsCenterAndOnlyClampsOutsideScreen() {
        assertEquals(140,PetGeometry.resizedOrigin(160,80,120));
        assertEquals(200,PetGeometry.resizedOrigin(160,80,120)+120/2);
        assertEquals(0,PetGeometry.restingX(-20,80,0,400,false));
        assertEquals(320,PetGeometry.restingX(390,80,0,400,false));
    }
    @Test public void speechFlipsBelowCatAtScreenTopAndStaysVisible() {
        assertEquals(250,PetGeometry.bubbleY(300,80,44,24,800,6));
        assertEquals(110,PetGeometry.bubbleY(24,80,44,24,800,6));
        assertEquals(650,PetGeometry.bubbleY(700,80,44,24,800,6));
    }
}

