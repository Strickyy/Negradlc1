package dev.negradlc.combat;

/**
 * Tick-based camera easing with independent yaw/pitch velocity and braking.
 * Reset when changing targets, modes, or resuming after a pause.
 */
public final class RotationStepper {
    private double yawVelocity;
    private double pitchVelocity;

    public void reset() {
        yawVelocity = 0;
        pitchVelocity = 0;
    }

    public CombatMath.Rotation step(float yaw, float pitch, float targetYaw, float targetPitch,
                                    double yawLimit, double pitchLimit, double smoothing,
                                    double acceleration, double deadzone) {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)
                || !Float.isFinite(targetYaw) || !Float.isFinite(targetPitch)) {
            reset();
            return new CombatMath.Rotation(Float.isFinite(yaw) ? yaw : 0,
                    Float.isFinite(pitch) ? CombatMath.clampPitch(pitch) : 0);
        }
        pitch = CombatMath.clampPitch(pitch);
        double yawDifference = CombatMath.wrapDegrees(targetYaw - yaw);
        double pitchDifference = CombatMath.clampPitch(targetPitch) - pitch;
        double ease = finiteClamp(smoothing, 0.01, 1);
        double accel = finiteClamp(acceleration, 0, 180);
        double zone = finiteClamp(deadzone, 0, 5);
        yawVelocity = advance(yawDifference, yawVelocity, finiteClamp(yawLimit, 0, 180), ease, accel, zone);
        pitchVelocity = advance(pitchDifference, pitchVelocity, finiteClamp(pitchLimit, 0, 180), ease, accel, zone);
        return new CombatMath.Rotation((float) (yaw + yawVelocity),
                CombatMath.clampPitch((float) (pitch + pitchVelocity)));
    }

    private static double advance(double difference, double velocity, double limit,
                                  double ease, double acceleration, double deadzone) {
        double remaining = Math.abs(difference);
        if (remaining <= deadzone || limit == 0 || acceleration == 0) return 0;
        double desired = Math.copySign(Math.min(limit,
                Math.min(remaining * ease, Math.sqrt(2 * acceleration * remaining))), difference);
        double next = velocity + CombatMath.clamp(desired - velocity, -acceleration, acceleration);
        // Arrival and sudden target reversal are hard safety boundaries: stale momentum
        // must never continue past the target or pull the camera away from it.
        if (Math.signum(next) != Math.signum(difference)) next = 0;
        return Math.copySign(Math.min(remaining, Math.min(Math.abs(next), limit)), difference);
    }

    private static double finiteClamp(double value, double min, double max) {
        return Double.isFinite(value) ? CombatMath.clamp(value, min, max) : min;
    }
}
