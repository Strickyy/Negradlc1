package dev.negradlc.movement;

/** Vanilla-coordinate steering without diagonal speed multiplication. */
public final class MovementMath {
    private MovementMath() {}

    public static double[] steer(double velocityX, double velocityZ, float yaw,
                                 double forward, double sideways, double control) {
        double length = Math.hypot(forward, sideways);
        if (length < 1.0e-6) return new double[] {velocityX, velocityZ};
        forward /= length;
        sideways /= length;
        double radians = Math.toRadians(yaw);
        double acceleration = Math.max(0.0, Math.min(0.2, control));
        double x = velocityX + (sideways * Math.cos(radians) - forward * Math.sin(radians)) * acceleration;
        double z = velocityZ + (forward * Math.cos(radians) + sideways * Math.sin(radians)) * acceleration;
        // Steering may retain existing momentum but never increases a high existing speed.
        double cap = Math.max(0.35, Math.hypot(velocityX, velocityZ));
        double speed = Math.hypot(x, z);
        if (speed > cap) {
            x *= cap / speed;
            z *= cap / speed;
        }
        return new double[] {x, z};
    }
}
