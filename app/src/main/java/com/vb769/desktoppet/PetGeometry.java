package com.vb769.desktoppet;

/** Coordinates use the usable screen rectangle; kept independent of Android for tests. */
final class PetGeometry {
    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, Math.max(min, max)));
    }
    static int edgeX(boolean right, int left, int screenRight, int width) {
        return right ? Math.max(left, screenRight - width) : left;
    }
    static boolean nearestRight(int x, int width, int left, int right) {
        return (long)x * 2 + width >= (long)left + right;
    }
}
