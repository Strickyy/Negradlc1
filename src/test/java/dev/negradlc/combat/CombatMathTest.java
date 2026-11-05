package dev.negradlc.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatMathTest {
    @Test
    void turnsAcrossTheYawSeamAlongTheShorterArc() {
        assertEquals(181.0f, CombatMath.turnToward(179.0f, -179.0f, 6.0), 0.0001);
        assertEquals(-181.0f, CombatMath.turnToward(-179.0f, 179.0f, 6.0), 0.0001);
        assertEquals(6.0f, CombatMath.turnToward(0.0f, 90.0f, 6.0), 0.0001);
    }

    @Test
    void coneUsesFullWidthAndHandlesWrappedAngles() {
        assertTrue(CombatMath.insideFov(179.0f, -179.0f, 10));
        assertTrue(CombatMath.insideFov(0.0f, 45.0f, 90));
        assertFalse(CombatMath.insideFov(0.0f, 46.0f, 90));
    }

    @Test
    void lookAtMatchesMinecraftCardinalDirectionsAndVerticalLimits() {
        assertEquals(0.0f, CombatMath.lookAt(0, 0, 1).yaw(), 0.001);
        assertEquals(-90.0f, CombatMath.lookAt(1, 0, 0).yaw(), 0.001);
        assertEquals(-90.0f, CombatMath.lookAt(0, 1, 0).pitch(), 0.001);
        assertEquals(90.0f, CombatMath.lookAt(0, -1, 0).pitch(), 0.001);
    }

    @Test
    void criticalOnlyRequiresFallingAndRejectsEveryVanillaDisqualifier() {
        assertTrue(CombatMath.canCriticalHit(0.2f, false, false, false, false, false, false));
        assertFalse(CombatMath.canCriticalHit(0, false, false, false, false, false, false));
        assertFalse(CombatMath.canCriticalHit(0.2f, true, false, false, false, false, false));
        assertFalse(CombatMath.canCriticalHit(0.2f, false, true, false, false, false, false));
        assertFalse(CombatMath.canCriticalHit(0.2f, false, false, true, false, false, false));
        assertFalse(CombatMath.canCriticalHit(0.2f, false, false, false, true, false, false));
        assertFalse(CombatMath.canCriticalHit(0.2f, false, false, false, false, true, false));
        assertFalse(CombatMath.canCriticalHit(0.2f, false, false, false, false, false, true));
    }

    @Test
    void smoothAimUsesShortestArcAndHonorsIndependentSpeedCeilings() {
        assertEquals(180, CombatMath.smoothYaw(179, -179, 10, 0.5), 0.0001);
        assertEquals(6, CombatMath.smoothYaw(0, 90, 6, 0.5), 0.0001);
        assertEquals(2, CombatMath.smoothPitch(0, 60, 2, 0.5), 0.0001);
        assertEquals(-2, CombatMath.smoothPitch(0, -60, 2, 0.5), 0.0001);
    }

    @Test
    void smoothAimConvergesWithoutOvershootAndSnapsTinyResiduals() {
        float yaw = 0;
        for (int i = 0; i < 100; i++) {
            float next = CombatMath.smoothYaw(yaw, 42, 6, 0.35);
            assertTrue(next >= yaw && next <= 42);
            yaw = next;
        }
        assertEquals(42, yaw, 0.0001);
        assertEquals(20.01, CombatMath.smoothYaw(20, 20.01f, 6, 0.35), 0.0001);
    }

    @Test
    void pitchNeverLeavesVanillaLimitsAndZeroSpeedDoesNotMove() {
        assertEquals(90, CombatMath.smoothPitch(85, 120, 20, 1), 0.0001);
        assertEquals(-90, CombatMath.smoothPitch(-85, -120, 20, 1), 0.0001);
        assertEquals(20, CombatMath.smoothYaw(20, 90, 0, 1), 0.0001);
        assertEquals(20, CombatMath.smoothPitch(20, 90, 0, 1), 0.0001);
    }
}
