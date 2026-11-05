package dev.negradlc.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovementMathTest {
    @Test void steeringMatchesMinecraftYaw() {
        assertArrayEquals(new double[]{0, 0.08}, MovementMath.steer(0, 0, 0, 1, 0, 0.08), 1e-9);
        assertArrayEquals(new double[]{-0.08, 0}, MovementMath.steer(0, 0, 90, 1, 0, 0.08), 1e-9);
        assertArrayEquals(new double[]{0.08, 0}, MovementMath.steer(0, 0, 0, 0, 1, 0.08), 1e-9);
    }

    @Test void diagonalInputDoesNotMultiplyAcceleration() {
        double[] result = MovementMath.steer(0, 0, 0, 1, 1, 0.08);
        assertEquals(0.08, Math.hypot(result[0], result[1]), 1e-9);
    }

    @Test void repeatedSteeringCapsSpeedAndDoesNotIncreaseExistingHighMomentum() {
        double x = 0, z = 0;
        for (int i = 0; i < 100; i++) {
            double[] result = MovementMath.steer(x, z, 0, 1, 0, 0.08);
            x = result[0]; z = result[1];
        }
        assertEquals(0.35, Math.hypot(x, z), 1e-9);
        double[] fast = MovementMath.steer(0, 0.7, 0, 1, 0, 0.08);
        assertEquals(0.7, Math.hypot(fast[0], fast[1]), 1e-9);
    }

    @Test void noInputPreservesMomentum() {
        assertArrayEquals(new double[]{0.2, -0.1}, MovementMath.steer(0.2, -0.1, 0, 0, 0, 0.08));
    }
}
