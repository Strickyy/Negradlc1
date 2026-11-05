package dev.negradlc.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** All gameplay modules are opt-in. Saved per game directory. */
public final class ClientConfig {
    public boolean killAura, triggerBot, aimAssist;
    public boolean critOnly = true, targetPlayers = true, targetHostile, targetPassive;
    public boolean ignoreFriends = true, requireAttackKey = true;
    public Set<String> friends = new LinkedHashSet<>();
    public double attackRange = 3.0, aimRange = 4.5, aimFov = 90, aimSpeed = 6;
    public double attackCooldown = 0.92;
    public boolean sprint, airStrafe, airStag, fullbright;
    public double airControl = 0.08;
    public boolean hud = true, watermark = true, moduleList = true, targetHud = true;
    public double guiX = -1, guiY = -1, guiWidth = 540, guiHeight = 350, guiScale = 1;
    public HudLayout watermarkLayout = new HudLayout(12, 12, 1);
    public HudLayout modulesLayout = new HudLayout(12, 60, 1);
    public HudLayout targetLayout = new HudLayout(12, 180, 1);
    public Map<String, Integer> binds = new LinkedHashMap<>();

    public ClientConfig() {
        for (String module : MODULES) binds.put(module, -1);
    }

    public static final String[] MODULES = {
        "killAura", "triggerBot", "aimAssist", "sprint", "airStag", "airStrafe", "fullbright", "hud"
    };

    public static final class HudLayout {
        public double x, y, scale;
        public HudLayout(double x, double y, double scale) { this.x = x; this.y = y; this.scale = scale; }
    }

    public boolean enabled(String module) {
        return switch (module) {
            case "killAura" -> killAura;
            case "triggerBot" -> triggerBot;
            case "aimAssist" -> aimAssist;
            case "sprint" -> sprint;
            case "airStag" -> airStag;
            case "airStrafe" -> airStrafe;
            case "fullbright" -> fullbright;
            case "hud" -> hud;
            default -> false;
        };
    }

    public void toggle(String module) {
        switch (module) {
            case "killAura" -> killAura = !killAura;
            case "triggerBot" -> triggerBot = !triggerBot;
            case "aimAssist" -> aimAssist = !aimAssist;
            case "sprint" -> sprint = !sprint;
            case "airStag" -> airStag = !airStag;
            case "airStrafe" -> airStrafe = !airStrafe;
            case "fullbright" -> fullbright = !fullbright;
            case "hud" -> hud = !hud;
            default -> throw new IllegalArgumentException("Unknown module: " + module);
        }
    }

    public void sanitize() {
        attackRange = clamp(attackRange, 1, 3, 3);
        aimRange = clamp(aimRange, 1, 8, 4.5);
        aimFov = clamp(aimFov, 5, 180, 90);
        aimSpeed = clamp(aimSpeed, 0.1, 30, 6);
        attackCooldown = clamp(attackCooldown, 0.1, 1, 0.92);
        airControl = clamp(airControl, 0.01, 0.3, 0.08);
        guiScale = clamp(guiScale, 0.7, 1.5, 1);
        guiWidth = clamp(guiWidth, 220, 1200, 540);
        guiHeight = clamp(guiHeight, 180, 900, 350);
        guiX = clamp(guiX, -1, 16384, -1);
        guiY = clamp(guiY, -1, 16384, -1);
        watermarkLayout = layout(watermarkLayout, 12, 12);
        modulesLayout = layout(modulesLayout, 12, 60);
        targetLayout = layout(targetLayout, 12, 180);
        if (friends == null) friends = new LinkedHashSet<>();
        friends.removeIf(name -> name == null || name.isBlank());
        if (binds == null) binds = new LinkedHashMap<>();
        for (String module : MODULES) {
            Integer key = binds.get(module);
            if (key == null || key < -1 || key > 348) binds.put(module, -1);
        }
    }

    private static HudLayout layout(HudLayout layout, double x, double y) {
        if (layout == null) return new HudLayout(x, y, 1);
        layout.x = clamp(layout.x, 0, 16384, x);
        layout.y = clamp(layout.y, 0, 16384, y);
        layout.scale = clamp(layout.scale, 0.5, 2, 1);
        return layout;
    }

    private static double clamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
