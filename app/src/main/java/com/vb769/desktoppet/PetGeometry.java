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
    static int restingX(int x, int width, int left, int right, boolean snap) {
        return snap ? edgeX(nearestRight(x, width, left, right), left, right, width)
                    : clamp(x, left, right - width);
    }
    static int resizedOrigin(int origin, int oldSize, int newSize) {
        return origin + (oldSize-newSize)/2;
    }
    static int bubbleY(int petTop, int petHeight, int bubbleHeight, int top, int bottom, int gap) {
        int above=petTop-bubbleHeight-gap;
        return clamp(above >= top ? above : petTop+petHeight+gap, top, bottom-bubbleHeight);
    }
}

