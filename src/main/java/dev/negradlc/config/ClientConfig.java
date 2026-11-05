package dev.negradlc.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import dev.negradlc.input.BindingCodes;

/** All gameplay modules are opt-in. Saved per game directory. */
public final class ClientConfig {
    public boolean killAura, triggerBot, aimAssist;
    public boolean critOnly, autoCritJump = true, targetPlayers = true, targetHostile, targetPassive;
    public boolean ignoreFriends = true, requireAttackKey = true;
    public Set<String> friends = new LinkedHashSet<>();
    public double attackRange = 3.0, aimRange = 4.5, aimFov = 90, aimSpeed = 6;
    public double attackCooldown = 0.92;
    public double aimPitchSpeed = 4, aimSmoothing = 0.35, auraFov = 180;
    public String aimPoint = "CHEST";
    public boolean aimVisibleOnly = true, aimLockTarget = true;
    public String auraRotation = "SNAP_LEGIT", auraAimPoint = "CHEST", auraPriority = "DISTANCE";
    public double auraYawSpeed = 35, auraPitchSpeed = 20, auraSmoothing = 0.45;
    public double auraAcceleration = 8, auraDeadzone = 0.15;
    public boolean auraReturnView = true, auraLockTarget = true;
    public double aimAcceleration = 2, aimDeadzone = 0.15;
    public boolean smoothTriggerAim;
    public double triggerAimFov = 30;
    public boolean esp, tracers;
    public boolean espPlayers = true, espHostile, espPassive, espThroughWalls = true;
    public boolean espHealth = true, espNames = true;
    public double espRange = 64, espLineWidth = 1.5;
    public int espColor = 0x57D9C0;
    public boolean tracerPlayers = true, tracerMobs, tracerThroughWalls = true;
    public double tracerRange = 64, tracerWidth = 1.5;
    public int tracerColor = 0x57D9C0;
    public boolean toggleSounds = true;
    public double toggleVolume = 0.35;
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
        "killAura", "triggerBot", "aimAssist", "sprint", "airStag", "airStrafe", "esp", "tracers", "fullbright", "hud", "toggleSounds"
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
            case "esp" -> esp;
            case "tracers" -> tracers;
            case "toggleSounds" -> toggleSounds;
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
            case "esp" -> esp = !esp;
            case "tracers" -> tracers = !tracers;
            case "toggleSounds" -> toggleSounds = !toggleSounds;
            default -> throw new IllegalArgumentException("Unknown module: " + module);
        }
    }

    public void sanitize() {
        attackRange = clamp(attackRange, 1, 3, 3);
        aimRange = clamp(aimRange, 1, 8, 4.5);
        aimFov = clamp(aimFov, 5, 180, 90);
        aimSpeed = clamp(aimSpeed, 0.1, 30, 6);
        attackCooldown = clamp(attackCooldown, 0.1, 1, 0.92);
        aimPitchSpeed = clamp(aimPitchSpeed, 0.1, 30, 4);
        aimSmoothing = clamp(aimSmoothing, 0.05, 1, 0.35);
        auraFov = clamp(auraFov, 10, 360, 180);
        if (!Set.of("CHEST", "HEAD", "NEAREST").contains(aimPoint == null ? "" : aimPoint)) aimPoint = "CHEST";
        auraRotation = choice(auraRotation, "SNAP_LEGIT", "SNAP", "SMOOTH");
        auraAimPoint = choice(auraAimPoint, "CHEST", "HEAD", "NEAREST");
        auraPriority = choice(auraPriority, "DISTANCE", "CROSSHAIR", "HEALTH");
        auraYawSpeed = clamp(auraYawSpeed, 1, 180, 35);
        auraPitchSpeed = clamp(auraPitchSpeed, 1, 90, 20);
        auraSmoothing = clamp(auraSmoothing, 0.05, 1, 0.45);
        auraAcceleration = clamp(auraAcceleration, 0.25, 40, 8);
        auraDeadzone = clamp(auraDeadzone, 0, 2, 0.15);
        aimAcceleration = clamp(aimAcceleration, 0.1, 20, 2);
        aimDeadzone = clamp(aimDeadzone, 0, 2, 0.15);
        triggerAimFov = clamp(triggerAimFov, 5, 90, 30);
        espRange = clamp(espRange, 8, 192, 64);
        tracerRange = clamp(tracerRange, 8, 192, 64);
        espLineWidth = clamp(espLineWidth, 1, 4, 1.5);
        tracerWidth = clamp(tracerWidth, 1, 4, 1.5);
        espColor &= 0xFFFFFF;
        tracerColor &= 0xFFFFFF;
        toggleVolume = clamp(toggleVolume, 0, 1, 0.35);
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
            if (key == null || !BindingCodes.isValid(key) || key == 344) binds.put(module, -1);
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

    private static String choice(String value, String fallback, String... alternatives) {
        if (fallback.equals(value)) return value;
        for (String alternative : alternatives) if (alternative.equals(value)) return value;
        return fallback;
    }
}
