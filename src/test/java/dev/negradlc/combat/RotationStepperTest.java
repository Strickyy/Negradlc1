package dev.negradlc.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RotationStepperTest {
    @Test void shortestWrappedArcAvoidsSpinningAcrossTheYawSeam() {
        var result = new RotationStepper().step(179, 0, -179, 0, 20, 20, .5, 4, 0);
        assertEquals(180, result.yaw(), 1e-5);
        assertEquals(0, result.pitch());
    }

    @Test void independentSpeedLimitsAndAccelerationApplyDuringTracking() {
        var stepper = new RotationStepper();
        float yaw = 0, pitch = 0;
        double previousYawStep = 0, previousPitchStep = 0;
        for (int i = 0; i < 5; i++) {
            var result = stepper.step(yaw, pitch, 160, 80, 12, 5, .6, 2, 0);
            double yawStep = result.yaw() - yaw, pitchStep = result.pitch() - pitch;
            assertTrue(yawStep > 0 && yawStep <= 12);
            assertTrue(pitchStep > 0 && pitchStep <= 5);
            assertTrue(Math.abs(yawStep - previousYawStep) <= 2.00001);
            assertTrue(Math.abs(pitchStep - previousPitchStep) <= 2.00001);
            previousYawStep = yawStep;
            previousPitchStep = pitchStep;
            yaw = result.yaw(); pitch = result.pitch();
        }
    }

    @Test void convergesWithoutOvershootAndDoesNotDriftAtArrival() {
        var stepper = new RotationStepper();
        float yaw = 0, pitch = 0;
        for (int i = 0; i < 200; i++) {
            var next = stepper.step(yaw, pitch, 42, -23, 14, 8, .3, 3, .05);
            assertTrue(next.yaw() >= yaw && next.yaw() <= 42);
            assertTrue(next.pitch() <= pitch && next.pitch() >= -23);
            yaw = next.yaw(); pitch = next.pitch();
        }
        assertEquals(42, yaw, .051);
        assertEquals(-23, pitch, .051);
        var still = stepper.step(yaw, pitch, 42, -23, 14, 8, .3, 3, .05);
        assertEquals(yaw, still.yaw());
        assertEquals(pitch, still.pitch());
    }

    @Test void deadzoneCancelsExistingMomentumInsteadOfMicroCorrecting() {
        var stepper = new RotationStepper();
        var first = stepper.step(0, 0, 90, 30, 10, 10, .5, 3, .25);
        var held = stepper.step(first.yaw(), first.pitch(), first.yaw() + .1f,
                first.pitch() - .1f, 10, 10, .5, 3, .25);
        assertEquals(first, held);
    }

    @Test void resetRemovesAccumulatedVelocityAndPitchStaysWithinVanillaLimits() {
        var stepper = new RotationStepper();
        stepper.step(0, 0, 90, 80, 30, 20, .5, 3, 0);
        stepper.step(3, 3, 90, 80, 30, 20, .5, 3, 0);
        stepper.reset();
        var fresh = stepper.step(0, 89, 90, 300, 30, 20, 1, 3, 0);
        assertEquals(3, fresh.yaw(), 1e-5);
        assertEquals(90, fresh.pitch(), 1e-5);
    }

    @Test void suddenReversalNeverPullsAwayFromTheNewTarget() {
        var stepper = new RotationStepper();
        var moving = stepper.step(0, 0, 90, 45, 30, 30, 1, 5, 0);
        var reversed = stepper.step(moving.yaw(), moving.pitch(), -90, -45, 30, 30, 1, 1, 0);
        assertTrue(reversed.yaw() <= moving.yaw());
        assertTrue(reversed.pitch() <= moving.pitch());
    }

    @Test void zeroSpeedAndNonFiniteInputsDoNotProduceUnboundedAngles() {
        var stepper = new RotationStepper();
        var held = stepper.step(20, 10, 90, 80, 0, 0, .5, 4, 0);
        assertEquals(new CombatMath.Rotation(20, 10), held);
        var invalid = stepper.step(Float.NaN, 110, 20, 20, 30, 30, .5, 4, 0);
        assertEquals(new CombatMath.Rotation(0, 90), invalid);
    }
}
