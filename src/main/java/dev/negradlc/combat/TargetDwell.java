package dev.negradlc.combat;

/** Counts contiguous ticks on the same crosshair entity; loss or switching resets it. */
public final class TargetDwell {
    private int target = -1, since;

    public boolean update(int targetId, int tick, int requiredTicks) {
        if (targetId < 0) { reset(); return false; }
        if (targetId != target || tick < since) { target = targetId; since = tick; }
        return tick - since >= Math.max(0, requiredTicks);
    }

    public void reset() { target = -1; since = 0; }
}
