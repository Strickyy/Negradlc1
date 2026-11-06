package dev.negradlc.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetDwellTest {
    @Test void zeroDelayPreservesImmediateTrigger() {
        var dwell = new TargetDwell();
        assertTrue(dwell.update(7, 100, 0));
        assertTrue(dwell.update(8, 101, 0));
    }

    @Test void requiresContinuousTicksOnSameEntityAndClearsWhenLost() {
        var dwell = new TargetDwell();
        assertFalse(dwell.update(7, 100, 2));
        assertFalse(dwell.update(7, 101, 2));
        assertTrue(dwell.update(7, 102, 2));
        assertFalse(dwell.update(-1, 103, 2));
        assertFalse(dwell.update(7, 104, 2));
        assertFalse(dwell.update(8, 105, 2));
        assertFalse(dwell.update(8, 106, 2));
        assertTrue(dwell.update(8, 107, 2));
    }

    @Test void resetAndWorldAgeRollbackCannotReuseOldDwell() {
        var dwell = new TargetDwell();
        assertFalse(dwell.update(7, 100, 2));
        assertTrue(dwell.update(7, 105, 2));
        assertFalse(dwell.update(7, 1, 2));
        dwell.reset();
        assertFalse(dwell.update(7, 5, 2));
    }
}
