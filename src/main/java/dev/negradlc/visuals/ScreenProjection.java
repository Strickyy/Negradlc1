package dev.negradlc.visuals;

/** Homogeneous projection rules, independent of the renderer and game state. */
public final class ScreenProjection {
    private ScreenProjection() {}

    public record Point(double x, double y) {}

    /** Points behind the camera or in front of the near plane cannot be projected safely. */
    public static Point projectClip(double x, double y, double z, double w, int width, int height) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Double.isFinite(w) || w <= 0.001 || z < -w || z > w
                || width <= 0 || height <= 0) return null;
        return new Point((x / w + 1) * width * 0.5, (1 - y / w) * height * 0.5);
    }

    public static double healthFraction(double health, double maxHealth) {
        if (!Double.isFinite(health) || !Double.isFinite(maxHealth) || maxHealth <= 0) return 0;
        return Math.max(0, Math.min(1, health / maxHealth));
    }

    /** Clip a center-origin tracer to the viewport while retaining its direction. */
    public static Point clipTracer(double x, double y, int width, int height) {
        double centerX = width * 0.5, centerY = height * 0.5;
        if (!Double.isFinite(x) || !Double.isFinite(y) || width <= 0 || height <= 0)
            return new Point(centerX, centerY);
        double dx = x - centerX, dy = y - centerY;
        double factor = 1;
        if (dx != 0) factor = Math.min(factor, Math.max(0, centerX - 1) / Math.abs(dx));
        if (dy != 0) factor = Math.min(factor, Math.max(0, centerY - 1) / Math.abs(dy));
        return new Point(centerX + dx * factor, centerY + dy * factor);
    }
}
