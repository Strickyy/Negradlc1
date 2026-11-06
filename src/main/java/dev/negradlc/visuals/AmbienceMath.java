package dev.negradlc.visuals;

/** Game-independent bounds for ambience settings. */
public final class AmbienceMath {
    private AmbienceMath() {}
    public static int blendRgb(int original, int tint, double strength) {
        double blend = Double.isFinite(strength) ? Math.max(0, Math.min(1, strength)) : 0;
        int result = original & 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int a = (original >>> shift) & 255, b = (tint >>> shift) & 255;
            result |= ((int) Math.round(a + (b - a) * blend)) << shift;
        }
        return result;
    }
    public static long timeOfDay(int configured) { return Math.floorMod(configured, 24000); }
    public static float rain(String weather) { return "RAIN".equals(weather) || "THUNDER".equals(weather) ? 1f : 0f; }
    public static float thunder(String weather) { return "THUNDER".equals(weather) ? 1f : 0f; }
    public static double particlesPerTick(double density) {
        return Double.isFinite(density) ? Math.max(0, Math.min(60, density)) / 20.0 : 0;
    }
}
