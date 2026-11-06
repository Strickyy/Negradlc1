package dev.negradlc.visuals;

import java.util.ArrayList;
import java.util.List;

/** Small, bounded geometry helpers independent of Minecraft rendering. */
public final class EffectGeometry {
    private EffectGeometry() {}
    public record Point(double x, double y, double z) {}
    public record Edge(Point from, Point to) {}

    public static List<Point> ring(double radius, double height, int segments, double phase) {
        int count = Math.max(8, Math.min(64, segments));
        double r = Double.isFinite(radius) ? Math.max(0, Math.min(16, radius)) : 0;
        double y = Double.isFinite(height) ? height : 0;
        double rotation = Double.isFinite(phase) ? phase : 0;
        List<Point> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = rotation + Math.PI * 2 * i / count;
            points.add(new Point(Math.cos(angle) * r, y, Math.sin(angle) * r));
        }
        return List.copyOf(points);
    }

    public static List<Edge> cube(double halfWidth, double height) {
        double r = Double.isFinite(halfWidth) ? Math.max(0.05, Math.min(16, halfWidth)) : .5;
        double h = Double.isFinite(height) ? Math.max(0.1, Math.min(32, height)) : 1;
        List<Edge> edges = new ArrayList<>(12);
        List<Point> bottom = List.of(new Point(-r, 0, -r), new Point(r, 0, -r),
                new Point(r, 0, r), new Point(-r, 0, r));
        for (int i = 0; i < 4; i++) {
            Point a = bottom.get(i), b = bottom.get((i + 1) % 4);
            Point topA = new Point(a.x, h, a.z), topB = new Point(b.x, h, b.z);
            edges.add(new Edge(a, b)); edges.add(new Edge(topA, topB)); edges.add(new Edge(a, topA));
        }
        return List.copyOf(edges);
    }

    public static double fade(double age, double duration) {
        if (!Double.isFinite(age) || !Double.isFinite(duration) || duration <= 0 || age < 0 || age >= duration) return 0;
        double remaining = 1 - age / duration;
        return remaining * remaining;
    }

    public static double jumpRadius(double maximum, double age, double duration) {
        if (!Double.isFinite(maximum) || !Double.isFinite(age) || !Double.isFinite(duration) || duration <= 0) return 0;
        double progress = Math.max(0, Math.min(1, age / duration));
        return Math.max(0, maximum) * (0.18 + 0.82 * (1 - Math.pow(1 - progress, 3)));
    }

    /** Detect an actual rising takeoff, not merely walking off a ledge or teleporting. */
    public static final class JumpDetector {
        private Point previous;
        private boolean grounded;
        public void reset() { previous = null; grounded = false; }
        public Point sample(double x, double y, double z, boolean onGround, double verticalVelocity) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) { reset(); return null; }
            Point takeoff = null;
            if (previous != null) {
                double dx = x - previous.x, dy = y - previous.y, dz = z - previous.z;
                if (grounded && !onGround && dy > 0.02 && dy < 1.5 && verticalVelocity > 0.02
                        && dx * dx + dz * dz < 4) takeoff = previous;
            }
            previous = new Point(x, y, z);
            grounded = onGround;
            return takeoff;
        }
    }
}
