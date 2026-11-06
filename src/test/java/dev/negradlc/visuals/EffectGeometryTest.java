package dev.negradlc.visuals;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EffectGeometryTest {
    @Test void ringsStayOnHorizontalPlaneAndRespectSegmentBudget() {
        var ring = EffectGeometry.ring(1.4, .1, 10000, .7);
        assertEquals(64, ring.size());
        for (var p : ring) {
            assertEquals(1.4, Math.hypot(p.x(), p.z()), 1e-9);
            assertEquals(.1, p.y());
        }
        assertEquals(8, EffectGeometry.ring(1, 0, 0, 0).size());
    }
    @Test void cubeHasTwelveEdgesAndExpectedBounds() {
        var cube = EffectGeometry.cube(.5, 2);
        assertEquals(12, cube.size());
        for (var edge : cube) for (var p : java.util.List.of(edge.from(), edge.to())) {
            assertTrue(Math.abs(p.x()) <= .5);
            assertTrue(Math.abs(p.z()) <= .5);
            assertTrue(p.y() == 0 || p.y() == 2);
        }
    }
    @Test void fadingAndExpansionAreFiniteMonotoneAndEndCleanly() {
        double previousRadius = 0, previousAlpha = 1;
        for (int i = 0; i <= 20; i++) {
            double age = i * .05;
            double radius = EffectGeometry.jumpRadius(1.4, age, 1);
            double alpha = EffectGeometry.fade(age, 1);
            assertTrue(radius >= previousRadius && radius <= 1.4);
            assertTrue(alpha <= previousAlpha && alpha >= 0);
            previousRadius = radius; previousAlpha = alpha;
        }
        assertEquals(0, EffectGeometry.fade(2, 1));
        assertEquals(0, EffectGeometry.fade(Double.NaN, 1));
        assertEquals(0, EffectGeometry.jumpRadius(1, 1, 0));
    }
    @Test void jumpDetectorRejectsLedgesTeleportsAndRepeatedAirSamples() {
        var detector = new EffectGeometry.JumpDetector();
        assertNull(detector.sample(0, 64, 0, true, 0));
        assertEquals(new EffectGeometry.Point(0, 64, 0), detector.sample(0, 64.42, 0, false, .33));
        assertNull(detector.sample(0, 64.75, 0, false, .24));
        detector.sample(0, 64, 0, true, 0);
        assertNull(detector.sample(0, 63.9, 0, false, -.1));
        detector.sample(0, 64, 0, true, 0);
        assertNull(detector.sample(100, 65, 0, false, .3));
        detector.reset();
        assertNull(detector.sample(0, 65, 0, false, .3));
    }
}
