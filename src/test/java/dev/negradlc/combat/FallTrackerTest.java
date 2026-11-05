package dev.negradlc.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FallTrackerTest {
    @Test void realDescentAllowsCriticalEvenWhenVanillaClientFallDistanceStaysZero() {
        var tracker = new FallTracker();
        assertEquals(0, tracker.sample(100, true));
        assertEquals(0, tracker.sample(100.42, false));
        assertEquals(0, tracker.sample(100.7, false));
        float falling = tracker.sample(100.6, false);
        assertEquals(0.1, falling, 0.0001);
        assertTrue(CombatMath.canCriticalHit(falling, false, false, false, false, false, false));
        assertEquals(0.3, tracker.sample(100.4, false), 0.0001);
        assertEquals(0, tracker.sample(100, true));
    }

    @Test void firstSampleRisingTeleportAndResetNeverFabricateFall() {
        var tracker = new FallTracker();
        assertEquals(0, tracker.sample(100, false));
        assertEquals(0.2, tracker.sample(99.8, false), 0.0001);
        assertEquals(0, tracker.sample(100.1, false));
        assertEquals(0, tracker.sample(50, false));
        tracker.reset();
        assertEquals(0, tracker.sample(49.9, false));
    }
}
