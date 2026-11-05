package dev.negradlc.combat;

/** Game-independent rotation and critical-hit rules shared by the combat modules. */
public final class CombatMath {
    private CombatMath() {}

    public record Rotation(float yaw, float pitch) {}

    public static Rotation lookAt(double dx, double dy, double dz) {
        double horizontal = Math.hypot(dx, dz);
        return new Rotation((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f,
                clampPitch((float) -Math.toDegrees(Math.atan2(dy, horizontal))));
    }

    public static float wrapDegrees(float angle) {
        float wrapped = angle % 360.0f;
        if (wrapped >= 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }

    public static float turnToward(float current, float target, double maximumStep) {
        float difference = wrapDegrees(target - current);
        float step = (float) Math.max(0.0, Math.min(180.0, maximumStep));
        return current + Math.max(-step, Math.min(step, difference));
    }

    public static float clampPitch(float pitch) {
        return Math.max(-90.0f, Math.min(90.0f, pitch));
    }

    /** FOV is the full horizontal cone, centered on the current camera direction. */
    public static boolean insideFov(float cameraYaw, float targetYaw, double fullFov) {
        return Math.abs(wrapDegrees(targetYaw - cameraYaw)) <= Math.max(0.0, Math.min(360.0, fullFov)) / 2.0;
    }

    /** Matches ordinary falling-melee critical conditions; this never creates a critical. */
    public static boolean canCriticalHit(float fallDistance, boolean onGround, boolean climbing,
                                         boolean touchingWater, boolean blind, boolean riding,
                                         boolean sprinting) {
        return fallDistance > 0.0f && !onGround && !climbing && !touchingWater
                && !blind && !riding && !sprinting;
    }
}
