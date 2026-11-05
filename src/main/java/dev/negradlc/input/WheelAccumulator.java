package dev.negradlc.input;

/** High-resolution trackpads need a full notch; changing direction discards a partial notch. */
public final class WheelAccumulator {
    private double remainder;
    public int accept(double delta) {
        if (!Double.isFinite(delta) || delta == 0) return 0;
        if (Math.signum(delta) != Math.signum(remainder)) remainder = 0;
        remainder += delta;
        if (Math.abs(remainder) < 1 - 1e-6) return 0;
        int direction = remainder > 0 ? 1 : -1;
        remainder = 0;
        return direction;
    }
    public void reset() { remainder = 0; }
}
