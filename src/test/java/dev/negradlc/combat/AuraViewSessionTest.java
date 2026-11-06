package dev.negradlc.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraViewSessionTest {
    @Test void repeatedAcquisitionAndHitsDoNotStartPerHitReturnOrReplaceOriginalView() {
        var session = new AuraViewSession();
        session.acquire(0, 5, true);
        for (int hit = 0; hit < 20; hit++) {
            session.applied(60, 15);
            session.observe(60, 15);
            session.acquire(60, 15, true);
            assertFalse(session.returning());
            assertEquals(0, session.yaw());
            assertEquals(5, session.pitch());
        }
        session.release(true);
        assertTrue(session.returning());
        assertEquals(0, session.yaw());
    }

    @Test void reacquisitionCancelsReturnInsteadOfReturningBetweenTargets() {
        var session = new AuraViewSession();
        session.acquire(10, 0, true);
        session.release(true);
        assertTrue(session.returning());
        session.acquire(40, 10, true);
        assertFalse(session.returning());
        assertEquals(10, session.yaw());
    }

    @Test void keepsManualInputDuringTrackingButReleasesControlDuringReturn() {
        var session = new AuraViewSession();
        session.acquire(170, 10, true);
        session.applied(179, 20);
        session.observe(-179, 23);
        assertEquals(172, session.yaw());
        assertEquals(13, session.pitch());
        session.release(true);
        session.applied(150, 15);
        session.observe(151, 15);
        assertFalse(session.returning());
    }

    @Test void disabledReturnAndResetCannotLeaveAStaleCameraSnapshot() {
        var session = new AuraViewSession();
        session.acquire(30, 5, false);
        session.release(true);
        assertFalse(session.returning());
        session.acquire(30, 5, true);
        session.release(false);
        assertFalse(session.returning());
        session.acquire(70, 20, true);
        assertEquals(70, session.yaw());
        session.reset();
        session.release(true);
        assertFalse(session.returning());
    }
}
