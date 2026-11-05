package dev.negradlc.visuals;

import dev.negradlc.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Projects interpolated entity bounds from the world camera, then draws clean screen-space
 * boxes and tracers. Rendering through walls needs no depth-buffer or entity-state changes.
 */
public final class EspRenderer {
    private static List<Overlay> frame = List.of();
    private static Object frameWorld;
    private static long frameNanos;

    private EspRenderer() {}

    private record Overlay(double left, double top, double right, double bottom,
                           double feetX, double feetY, String name, float health,
                           float maximumHealth, double distance, boolean box, boolean tracer) {}

    private record LabelRect(int left, int top, int right, int bottom) {
        boolean overlaps(LabelRect other) {
            return left < other.right + 3 && right + 3 > other.left
                    && top < other.bottom + 3 && bottom + 3 > other.top;
        }
    }

    /** Register at WorldRenderEvents.LAST; world matrices are valid only during this callback. */
    public static void render(WorldRenderContext context, ClientConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        frame = List.of();
        frameWorld = client.world;
        frameNanos = System.nanoTime();
        if ((!config.esp && !config.tracers) || client.world == null || client.player == null) return;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        if (width <= 0 || height <= 0) return;
        Matrix4f transform = new Matrix4f(context.projectionMatrix()).mul(context.positionMatrix());
        Vec3d camera = context.camera().getPos();
        float delta = context.tickCounter().getTickDelta(false);
        List<Overlay> overlays = new ArrayList<>();
        double maxRange = Math.max(config.esp ? config.espRange : 0, config.tracers ? config.tracerRange : 0);
        for (LivingEntity entity : client.world.getEntitiesByClass(LivingEntity.class,
                client.player.getBoundingBox().expand(maxRange), candidate -> candidate != client.player
                        && candidate.isAlive() && !candidate.isRemoved())) {
            if (entity instanceof PlayerEntity player && player.isSpectator()) continue;
            boolean player = entity instanceof PlayerEntity;
            boolean mob = entity instanceof MobEntity;
            boolean hostile = entity.getType().getSpawnGroup() == SpawnGroup.MONSTER;
            boolean boxType = player ? config.espPlayers : mob && (hostile ? config.espHostile : config.espPassive);
            boolean tracerType = player ? config.tracerPlayers : mob && config.tracerMobs;
            double distance = client.player.distanceTo(entity);
            boolean visible = client.player.canSee(entity);
            boolean boxEnabled = config.esp && boxType && distance <= config.espRange
                    && (config.espThroughWalls || visible);
            boolean tracerEnabled = config.tracers && tracerType && distance <= config.tracerRange
                    && (config.tracerThroughWalls || visible);
            if (!boxEnabled && !tracerEnabled) continue;
            Vec3d position = entity.getLerpedPos(delta);
            Box bounds = entity.getBoundingBox().offset(position.subtract(entity.getPos())).expand(0.03);
            double left = Double.POSITIVE_INFINITY, top = Double.POSITIVE_INFINITY;
            double right = Double.NEGATIVE_INFINITY, bottom = Double.NEGATIVE_INFINITY;
            int corners = 0;
            for (int ix = 0; ix < 2; ix++) for (int iy = 0; iy < 2; iy++) for (int iz = 0; iz < 2; iz++) {
                ScreenProjection.Point point = project(transform,
                        (ix == 0 ? bounds.minX : bounds.maxX) - camera.x,
                        (iy == 0 ? bounds.minY : bounds.maxY) - camera.y,
                        (iz == 0 ? bounds.minZ : bounds.maxZ) - camera.z, width, height);
                if (point == null) continue;
                corners++;
                left = Math.min(left, point.x()); top = Math.min(top, point.y());
                right = Math.max(right, point.x()); bottom = Math.max(bottom, point.y());
            }
            // A box crossing the camera's near plane can become screen-sized; omit it rather than flicker.
            if (corners != 8 || right < 0 || left > width || bottom < 0 || top > height) continue;
            left = Math.max(-4, left); right = Math.min(width + 4, right);
            top = Math.max(-4, top); bottom = Math.min(height + 4, bottom);
            ScreenProjection.Point feet = project(transform, position.x - camera.x,
                    position.y - camera.y, position.z - camera.z, width, height);
            overlays.add(new Overlay(left, top, right, bottom,
                    feet == null ? (left + right) * 0.5 : feet.x(), feet == null ? bottom : feet.y(),
                    entity.getName().getString(), entity.getHealth(), entity.getMaxHealth(), distance,
                    boxEnabled, tracerEnabled));
        }
        // Nearby targets get first choice of label space in crowded groups of entities.
        overlays.sort(Comparator.comparingDouble(Overlay::distance));
        frame = overlays;
    }

    private static ScreenProjection.Point project(Matrix4f transform, double x, double y, double z,
                                                   int width, int height) {
        Vector4f clip = transform.transform(new Vector4f((float) x, (float) y, (float) z, 1));
        return ScreenProjection.projectClip(clip.x, clip.y, clip.z, clip.w, width, height);
    }

    public static void renderHud(DrawContext draw, MinecraftClient client, ClientConfig config) {
        if ((!config.esp && !config.tracers) || client.world == null || client.player == null
                || client.options.hudHidden || frameWorld != client.world
                || System.nanoTime() - frameNanos > 500_000_000L) return;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        draw.enableScissor(0, 0, width, height);
        for (Overlay overlay : frame) {
            if (config.tracers && overlay.tracer) {
                ScreenProjection.Point endpoint = ScreenProjection.clipTracer(overlay.feetX, overlay.feetY, width, height);
                line(draw, width * 0.5, height * 0.5, endpoint.x(), endpoint.y(),
                        config.tracerWidth + 1.5, 0x90000000);
                line(draw, width * 0.5, height * 0.5, endpoint.x(), endpoint.y(),
                        config.tracerWidth, 0xDD000000 | (config.tracerColor & 0xFFFFFF));
            }
            if (!config.esp || !overlay.box) continue;
            double x = overlay.left, y = overlay.top, w = overlay.right - x, h = overlay.bottom - y;
            int color = 0xEE000000 | (config.espColor & 0xFFFFFF);
            outline(draw, x, y, w, h, config.espLineWidth + 2, 0xA0000000);
            outline(draw, x, y, w, h, config.espLineWidth, color);
            if (config.espHealth) {
                double fraction = ScreenProjection.healthFraction(overlay.health, overlay.maximumHealth);
                draw.fill((int) x - 6, (int) y - 1, (int) x - 2, (int) (y + h) + 1, 0xC0000000);
                int healthColor = fraction > 0.5 ? 0xFF69D7A0 : fraction > 0.25 ? 0xFFF0CB6C : 0xFFEC7777;
                draw.fill((int) x - 5, (int) (y + h * (1 - fraction)), (int) x - 3, (int) (y + h), healthColor);
            }
        }
        // Labels are drawn after all geometry, so distant boxes cannot obscure nearer names.
        // Suppress a farther label if its background would overlap an already visible label.
        List<LabelRect> occupied = new ArrayList<>();
        if (config.esp && config.espNames) for (Overlay overlay : frame) {
            if (!overlay.box) continue;
            String name = client.textRenderer.trimToWidth(overlay.name, 132);
            String label = client.textRenderer.trimToWidth(name
                    + String.format(java.util.Locale.ROOT, "  %.0fm", overlay.distance), Math.max(1, width - 12));
            int textWidth = client.textRenderer.getWidth(label);
            int textX = (int) Math.max(5, Math.min(width - textWidth - 5,
                    (overlay.left + overlay.right) * 0.5 - textWidth * 0.5));
            int textY = (int) Math.max(3, Math.min(height - 11, overlay.top - 14));
            LabelRect area = new LabelRect(textX - 3, textY - 2, textX + textWidth + 3, textY + 10);
            if (occupied.stream().anyMatch(area::overlaps)) continue;
            occupied.add(area);
            draw.fill(area.left, area.top, area.right, area.bottom, 0xD80A1413);
            draw.drawTextWithShadow(client.textRenderer, label, textX, textY, 0xFFE3F5EF);
        }
        draw.disableScissor();
    }

    private static void outline(DrawContext draw, double x, double y, double width, double height,
                                double thickness, int color) {
        line(draw, x, y, x + width, y, thickness, color);
        line(draw, x + width, y, x + width, y + height, thickness, color);
        line(draw, x + width, y + height, x, y + height, thickness, color);
        line(draw, x, y + height, x, y, thickness, color);
    }

    private static void line(DrawContext draw, double x1, double y1, double x2, double y2,
                             double thickness, int color) {
        double dx = x2 - x1, dy = y2 - y1;
        int length = (int) Math.ceil(Math.hypot(dx, dy));
        if (length == 0) return;
        draw.getMatrices().push();
        draw.getMatrices().translate(x1, y1, 0);
        draw.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(dy, dx)));
        draw.getMatrices().scale(1, (float) thickness, 1);
        draw.fill(0, 0, length, 1, color);
        draw.getMatrices().pop();
    }
}
