package dev.negradlc.combat;

/** Observes genuine downward movement; never changes the player or outgoing packets. */
public final class FallTracker {
    private double previousY = Double.NaN;
    private float distance;

    public void reset() {
        previousY = Double.NaN;
        distance = 0;
    }

    public float sample(double y, boolean onGround) {
        double delta = y - previousY;
        if (!Double.isFinite(y)) { reset(); return 0; }
        if (!Double.isFinite(previousY) || onGround || Math.abs(delta) > 3 || delta > 0.00001) {
            distance = 0;
        } else if (delta < -0.00001) {
            distance += (float) -delta;
        }
        previousY = y;
        return distance;
    }
}
