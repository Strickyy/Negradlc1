package dev.negradlc.visuals;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AmbienceMathTest {
    @Test void tintUsesBoundedRgbBlendAndPreservesAlpha() {
        assertEquals(0xFF808080, AmbienceMath.blendRgb(0xFF000000, 0xFFFFFF, .5));
        assertEquals(0xFF123456, AmbienceMath.blendRgb(0xFF123456, 0xFFFFFF, -1));
        assertEquals(0xFFFFFFFF, AmbienceMath.blendRgb(0xFF123456, 0xFFFFFF, 10));
        assertEquals(0xFF123456, AmbienceMath.blendRgb(0xFF123456, 0xFFFFFF, Double.NaN));
    }
    @Test void displayTimeWrapsWithoutChangingTheGameClock() {
        assertEquals(6000, AmbienceMath.timeOfDay(30000));
        assertEquals(23999, AmbienceMath.timeOfDay(-1));
    }
    @Test void weatherPresetsMapToSeparateRainAndThunderOverlays() {
        assertEquals(0, AmbienceMath.rain("CLEAR"));
        assertEquals(1, AmbienceMath.rain("RAIN"));
        assertEquals(0, AmbienceMath.thunder("RAIN"));
        assertEquals(1, AmbienceMath.rain("THUNDER"));
        assertEquals(1, AmbienceMath.thunder("THUNDER"));
    }
    @Test void particleEmissionIsCappedAndCannotAccumulateInvalidRates() {
        assertEquals(0, AmbienceMath.particlesPerTick(Double.NaN));
        assertEquals(0, AmbienceMath.particlesPerTick(-1));
        assertEquals(.4, AmbienceMath.particlesPerTick(8));
        assertEquals(3, AmbienceMath.particlesPerTick(10000));
    }
}
