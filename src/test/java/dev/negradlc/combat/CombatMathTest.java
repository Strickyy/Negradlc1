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
}
