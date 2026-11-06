package dev.negradlc.combat;

/** One view snapshot per acquisition session, never one per attack. */
public final class AuraViewSession {
    private boolean saved, returning;
    private float yaw, pitch, appliedYaw, appliedPitch;

    public void acquire(float currentYaw, float currentPitch, boolean returnEnabled) {
        returning = false;
        if (saved || !returnEnabled) return;
        saved = true;
        yaw = appliedYaw = currentYaw;
        pitch = appliedPitch = currentPitch;
    }

    public void observe(float currentYaw, float currentPitch) {
        if (!saved) return;
        float yawInput = CombatMath.wrapDegrees(currentYaw - appliedYaw);
        float pitchInput = currentPitch - appliedPitch;
        if (returning && (Math.abs(yawInput) > 0.25 || Math.abs(pitchInput) > 0.25)) {
            reset(); // Manual mouse movement takes priority over automatic return.
            return;
        }
        yaw += yawInput;
        pitch = CombatMath.clampPitch(pitch + pitchInput);
        appliedYaw = currentYaw;
        appliedPitch = currentPitch;
    }

    public void applied(float currentYaw, float currentPitch) {
        appliedYaw = currentYaw;
        appliedPitch = currentPitch;
    }

    public void release(boolean returnEnabled) {
        if (!returnEnabled) reset();
        else returning = saved;
    }

    public boolean returning() { return returning; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public void reset() { saved = false; returning = false; }
}
