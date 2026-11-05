package dev.negradlc.ui;

import dev.negradlc.config.ClientConfig;
import dev.negradlc.visuals.EspRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** HUD widgets are drawn in logical pixels so their placement and scale are independent. */
public final class HudRenderer {
    static final int BACKGROUND = 0xD20C1513;
    static final int BORDER = 0xFF26463D;
    static final int ACCENT = 0xFF57D9C0;
    static final int WHITE = 0xFFE6F4EE;
    static final int MUTED = 0xFF91AAA0;
    private static String combatStatus = "No target";

    private HudRenderer() {}

    public static void render(DrawContext draw, MinecraftClient client, ClientConfig config, LivingEntity target) {
        render(draw, client, config, target, target == null ? "No target" : "Ready");
    }

    public static void render(DrawContext draw, MinecraftClient client, ClientConfig config,
                              LivingEntity target, String status) {
        EspRenderer.renderHud(draw, client, config);
        combatStatus = status == null ? "" : status;
        if (!config.hud || client.player == null || client.options.hudHidden) return;
        for (int widget = 0; widget < 3; widget++) {
            if (!enabled(config, widget) || (widget == 2 && target == null
                    && !config.killAura && !config.triggerBot && !config.aimAssist)) continue;
            double scale = MathHelper.clamp(scale(config, widget), 0.6, 2.0);
            int maxX = Math.max(0, client.getWindow().getScaledWidth() - (int) (width(widget) * scale));
            int maxY = Math.max(0, client.getWindow().getScaledHeight() - (int) (height(client, config, widget) * scale));
            renderWidget(draw, client, config, target, widget,
                    MathHelper.clamp(x(config, widget), 0, maxX),
                    MathHelper.clamp(y(config, widget), 0, maxY), scale, false);
        }
    }

    static void renderWidget(DrawContext draw, MinecraftClient client, ClientConfig config,
                             LivingEntity target, int widget, double x, double y, double scale, boolean preview) {
        draw.getMatrices().push();
        draw.getMatrices().translate(x, y, 0);
        draw.getMatrices().scale((float) scale, (float) scale, 1);
        int width = width(widget);
        int height = height(client, config, widget);
        panel(draw, width, height);
        switch (widget) {
            case 0 -> {
                draw.fill(10, 12, 13, 30, ACCENT);
                draw.drawTextWithShadow(client.textRenderer, "NEGRADLC", 20, 10, WHITE);
                draw.drawText(client.textRenderer, "FABRIC  /  " + client.getCurrentFps() + " FPS", 20, 25, MUTED, false);
                draw.fill(width - 13, 12, width - 9, 16, 0xFF8CE4B1);
            }
            case 1 -> {
                List<String> modules = modules(client, config);
                draw.drawTextWithShadow(client.textRenderer, "ACTIVE MODULES", 10, 10, ACCENT);
                String count = Integer.toString(modules.size());
                draw.drawText(client.textRenderer, count, width - 10 - client.textRenderer.getWidth(count), 10, MUTED, false);
                draw.fill(10, 25, width - 10, 26, BORDER);
                if (modules.isEmpty()) {
                    draw.drawText(client.textRenderer, "No modules enabled", 10, 34, MUTED, false);
                } else {
                    for (int i = 0; i < modules.size(); i++) {
                        int rowY = 34 + i * 16;
                        draw.fill(10, rowY + 2, 12, rowY + 6, ACCENT);
                        draw.drawTextWithShadow(client.textRenderer, modules.get(i), 19, rowY, WHITE);
                    }
                }
            }
            case 2 -> {
                String name = target == null ? preview ? "Target preview" : "Combat status" : target.getName().getString();
                float health = target == null ? preview ? 17 : 0 : Math.max(0, target.getHealth());
                float maxHealth = target == null ? 20 : Math.max(1, target.getMaxHealth());
                draw.fill(10, 12, 39, 41, 0xFF183D32);
                String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
                draw.drawCenteredTextWithShadow(client.textRenderer, initial, 25, 22, ACCENT);
                draw.drawTextWithShadow(client.textRenderer, clipped(client, name, width - 60), 49, 11, WHITE);
                String hp = String.format(java.util.Locale.ROOT, "%.1f / %.1f HP", health, maxHealth);
                draw.drawText(client.textRenderer, hp, 49, 25, MUTED, false);
                draw.fill(49, 40, width - 10, 44, 0xFF263C33);
                draw.fill(49, 40, 49 + (int) ((width - 59) * MathHelper.clamp(health / maxHealth, 0, 1)), 44, ACCENT);
                String detail = preview ? "Drag to place  /  corner to resize" : target == null ? "" :
                        client.player == null ? "" : String.format(java.util.Locale.ROOT, "Distance  %.1fm", client.player.distanceTo(target));
                draw.drawText(client.textRenderer, detail, 10, 52, MUTED, false);
                if (!preview) draw.drawText(client.textRenderer,
                        clipped(client, combatStatus, width - 20), 10, 65, ACCENT, false);
            }
            default -> { }
        }
        draw.getMatrices().pop();
    }

    private static String clipped(MinecraftClient client, String text, int width) {
        return client.textRenderer.getWidth(text) > width ? client.textRenderer.trimToWidth(text, width - 6) + "…" : text;
    }

    private static void panel(DrawContext draw, int width, int height) {
        draw.fill(3, 3, width + 3, height + 3, 0x35000000);
        draw.fill(1, 0, width - 1, height, BACKGROUND);
        draw.fill(0, 1, width, height - 1, BACKGROUND);
        draw.fill(1, 0, width - 1, 1, BORDER);
        draw.fill(1, height - 1, width - 1, height, BORDER);
        draw.fill(0, 1, 1, height - 1, BORDER);
        draw.fill(width - 1, 1, width, height - 1, BORDER);
    }

    static List<String> modules(MinecraftClient client, ClientConfig config) {
        List<String> modules = new ArrayList<>();
        if (config.killAura) modules.add("KillAura");
        if (config.triggerBot) modules.add("Trigger Bot");
        if (config.aimAssist) modules.add("Aim Assist");
        if (config.sprint) modules.add("Sprint");
        if (config.airStrafe) modules.add("Air Strafe");
        if (config.airStag) modules.add("AirStag");
        if (config.fullbright) modules.add("Fullbright");
        if (config.esp) modules.add("ESP");
        if (config.tracers) modules.add("Tracers");
        modules.sort(Comparator.comparingInt((String name) -> client.textRenderer.getWidth(name)).reversed());
        return modules;
    }

    static int width(int widget) { return widget == 2 ? 204 : 170; }
    static int height(MinecraftClient client, ClientConfig config, int widget) {
        return switch (widget) {
            case 0 -> 43;
            case 1 -> 38 + Math.max(1, modules(client, config).size()) * 16;
            default -> 82;
        };
    }
    static String name(int widget) {
        return switch (widget) { case 0 -> "Watermark"; case 1 -> "Modules"; default -> "Target HUD"; };
    }
    static boolean enabled(ClientConfig config, int widget) {
        return switch (widget) { case 0 -> config.watermark; case 1 -> config.moduleList; default -> config.targetHud; };
    }
    static double x(ClientConfig config, int widget) {
        return switch (widget) { case 0 -> config.watermarkLayout.x; case 1 -> config.modulesLayout.x; default -> config.targetLayout.x; };
    }
    static double y(ClientConfig config, int widget) {
        return switch (widget) { case 0 -> config.watermarkLayout.y; case 1 -> config.modulesLayout.y; default -> config.targetLayout.y; };
    }
    static double scale(ClientConfig config, int widget) {
        return switch (widget) { case 0 -> config.watermarkLayout.scale; case 1 -> config.modulesLayout.scale; default -> config.targetLayout.scale; };
    }
    static void layout(ClientConfig config, int widget, double x, double y, double scale) {
        switch (widget) {
            case 0 -> { config.watermarkLayout.x = x; config.watermarkLayout.y = y; config.watermarkLayout.scale = scale; }
            case 1 -> { config.modulesLayout.x = x; config.modulesLayout.y = y; config.modulesLayout.scale = scale; }
            default -> { config.targetLayout.x = x; config.targetLayout.y = y; config.targetLayout.scale = scale; }
        }
    }
}
