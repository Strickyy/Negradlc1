package dev.negradlc.visuals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScreenProjectionTest {
    @Test void centerAndScreenAxesUseHomogeneousDivision() {
        assertEquals(new ScreenProjection.Point(400, 300), ScreenProjection.projectClip(0, 0, 0, 2, 800, 600));
        assertEquals(new ScreenProjection.Point(800, 0), ScreenProjection.projectClip(2, 2, 0, 2, 800, 600));
        assertEquals(new ScreenProjection.Point(0, 600), ScreenProjection.projectClip(-2, -2, 0, 2, 800, 600));
    }

    @Test void behindCameraAndNearPlanePointsAreRejected() {
        assertNull(ScreenProjection.projectClip(0, 0, 0, -1, 800, 600));
        assertNull(ScreenProjection.projectClip(0, 0, -2, 1, 800, 600));
        assertNull(ScreenProjection.projectClip(0, 0, 2, 1, 800, 600));
        assertNull(ScreenProjection.projectClip(Double.NaN, 0, 0, 1, 800, 600));
        assertNull(ScreenProjection.projectClip(0, 0, 0, 1, 0, 600));
    }

    @Test void healthBarsClampOvershootAndInvalidEntityValues() {
        assertEquals(0.5, ScreenProjection.healthFraction(10, 20));
        assertEquals(1, ScreenProjection.healthFraction(40, 20));
        assertEquals(0, ScreenProjection.healthFraction(-5, 20));
        assertEquals(0, ScreenProjection.healthFraction(5, 0));
        assertEquals(0, ScreenProjection.healthFraction(Double.NaN, 20));
    }

    @Test void tracerEndpointsStayInsideViewportWithoutChangingTheirDirection() {
        assertEquals(new ScreenProjection.Point(600, 400), ScreenProjection.clipTracer(600, 400, 800, 600));
        assertEquals(new ScreenProjection.Point(400, 599), ScreenProjection.clipTracer(400, 100000, 800, 600));
        ScreenProjection.Point diagonal = ScreenProjection.clipTracer(4400, 2300, 800, 600);
        assertTrue(diagonal.x() >= 1 && diagonal.x() <= 799);
        assertTrue(diagonal.y() >= 1 && diagonal.y() <= 599);
        assertEquals(0.5, (diagonal.y() - 300) / (diagonal.x() - 400), 1e-9);
        assertEquals(new ScreenProjection.Point(400, 300), ScreenProjection.clipTracer(Double.NaN, 0, 800, 600));
    }
}
