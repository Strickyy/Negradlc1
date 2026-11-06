package dev.negradlc.visuals;

import dev.negradlc.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/** Animated world-space target markers and expanding jump rings, projected without changing GL state. */
public final class TargetEffects {
    private static final int MAX_LINES = 128, MAX_DOTS = 48;
    private static final double TARGET_FADE_SECONDS = 0.28;
    private static final EffectGeometry.JumpDetector detector = new EffectGeometry.JumpDetector();
    private static final List<JumpRing> rings = new ArrayList<>();
    private static Object world, player, frameWorld;
    private static LivingEntity target;
    private static Vec3d targetPosition;
    private static double targetWidth, targetHeight, targetSeen;
    private static List<Line> frameLines = List.of();
    private static List<Dot> frameDots = List.of();
    private static long frameNanos;

    private TargetEffects() {}
    private record JumpRing(Vec3d center, double born) {}
    private record Line(double x1, double y1, double x2, double y2, double width, int color) {}
    private record Dot(double x, double y, double radius, int color) {}

    public static void tick(MinecraftClient client, ClientConfig config, LivingEntity combatTarget) {
        if (world != client.world || player != client.player) {
            world = client.world; player = client.player;
            rings.clear(); detector.reset(); target = null; targetPosition = null;
            frameLines = List.of(); frameDots = List.of();
        }
        if (client.player == null || client.world == null) return;
        double now = seconds();
        EffectGeometry.Point takeoff = detector.sample(client.player.getX(), client.player.getY(), client.player.getZ(),
                client.player.isOnGround(), client.player.getVelocity().y);
        if (!config.jumpCircle) rings.clear();
        else if (takeoff != null) {
            rings.add(new JumpRing(new Vec3d(takeoff.x(), takeoff.y() + .025, takeoff.z()), now));
            if (rings.size() > 12) rings.removeFirst();
        }
        rings.removeIf(ring -> now - ring.born > config.jumpCircleDuration);
        LivingEntity candidate = combatTarget;
        if (candidate == null && config.targetEffectCrosshair
                && client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) candidate = living;
        if (candidate == client.player || candidate != null && (!candidate.isAlive() || candidate.isRemoved()
                || candidate.getWorld() != client.world || !config.targetEffectsThroughWalls && !client.player.canSee(candidate))) candidate = null;
        if (!config.targetEffects) { target = null; targetPosition = null; return; }
        if (candidate != null) {
            target = candidate;
            targetPosition = candidate.getPos();
            Box box = candidate.getBoundingBox();
            targetWidth = Math.max(box.getLengthX(), box.getLengthZ());
            targetHeight = box.getLengthY();
            targetSeen = now;
        } else if (now - targetSeen > TARGET_FADE_SECONDS) { target = null; targetPosition = null; }
        else target = null; // A brief fade uses the last position, not an entity that has disappeared.
    }

    /** Capture projection only while the world's matrices are valid (WorldRenderEvents.LAST). */
    public static void capture(WorldRenderContext context, ClientConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        frameWorld = client.world; frameNanos = System.nanoTime();
        List<Line> lines = new ArrayList<>(); List<Dot> dots = new ArrayList<>();
        frameLines = lines; frameDots = dots;
        if (client.world == null || client.player == null || client.world != world) return;
        Matrix4f matrix = new Matrix4f(context.projectionMatrix()).mul(context.positionMatrix());
        Projection projection = new Projection(matrix, context.camera().getPos(), client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), lines, dots);
        double now = seconds();
        float delta = context.tickCounter().getTickDelta(false);
        if (config.targetEffects && targetPosition != null) {
            boolean stillVisible = target == null || config.targetEffectsThroughWalls || client.player.canSee(target);
            double fade = target == null ? EffectGeometry.fade(now - targetSeen, TARGET_FADE_SECONDS) : 1;
            if (stillVisible && fade > .005) {
                Vec3d center = target == null ? targetPosition : target.getLerpedPos(delta);
                int color = alpha(config.targetEffectColor, fade * .95);
                double size = config.targetEffectSize;
                switch (config.targetEffectStyle) {
                    case "CUBE" -> {
                        for (EffectGeometry.Edge edge : EffectGeometry.cube(targetWidth * .5 * size + .06, targetHeight * size + .06))
                            projection.edge(center.add(0, -.02, 0), edge.from(), edge.to(), 1.6, color);
                    }
                    case "SQUARE" -> {
                        // Construct a billboard in world coordinates using the camera's quaternion.
                        org.joml.Vector3f right = new org.joml.Vector3f(1, 0, 0).rotate(context.camera().getRotation());
                        org.joml.Vector3f up = new org.joml.Vector3f(0, 1, 0).rotate(context.camera().getRotation());
                        Vec3d mid = center.add(0, targetHeight * .5, 0);
                        double half = Math.max(targetWidth, targetHeight * .65) * .65 * size;
                        Vec3d[] corners = new Vec3d[4];
                        int[] signsX = {-1, 1, 1, -1}, signsY = {-1, -1, 1, 1};
                        for (int i = 0; i < 4; i++) corners[i] = mid.add(right.x * half * signsX[i] + up.x * half * signsY[i],
                                right.y * half * signsX[i] + up.y * half * signsY[i], right.z * half * signsX[i] + up.z * half * signsY[i]);
                        for (int i = 0; i < 4; i++) projection.line(corners[i], corners[(i + 1) % 4], 1.8, color);
                    }
                    case "POINTS" -> {
                        double phase = now * config.targetEffectSpeed * 1.4;
                        for (int i = 0; i < 32; i++) {
                            double angle = Math.PI * 2 * i / 32 + phase;
                            double height = targetHeight * (.5 + .45 * Math.sin(angle * 2 + phase));
                            double radius = (targetWidth * .65 + .2) * size;
                            projection.dot(center.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius),
                                    1.3 + .6 * Math.sin(angle + phase), alpha(config.targetEffectColor, fade * (.45 + .5 * (i + 1) / 32.0)));
                        }
                    }
                    default -> {
                        double height = .06 + (Math.sin(now * config.targetEffectSpeed * 2) + 1) * .08;
                        projection.ring(center, targetWidth * .7 * size + .15, height, 40,
                                now * config.targetEffectSpeed, 1.8, color);
                        projection.ring(center, targetWidth * .7 * size + .21, height, 40,
                                -now * config.targetEffectSpeed, .8, alpha(config.targetEffectColor, fade * .35));
                    }
                }
            }
        }
        if (config.jumpCircle) for (int i = rings.size() - 1; i >= 0 && lines.size() < MAX_LINES; i--) {
            JumpRing ring = rings.get(i);
            double age = now - ring.born;
            double fade = EffectGeometry.fade(age, config.jumpCircleDuration);
            if (fade <= .005) continue;
            double radius = EffectGeometry.jumpRadius(config.jumpCircleRadius, age, config.jumpCircleDuration);
            projection.ring(ring.center, radius, 0, 24, now * .25, 1.8, alpha(config.jumpCircleColor, fade));
            // Sparse glints make a clean ring readable over both light and dark terrain.
            for (EffectGeometry.Point p : EffectGeometry.ring(radius, .015, 8, now * .4))
                projection.dot(ring.center.add(p.x(), p.y(), p.z()), 1.2, alpha(config.jumpCircleColor, fade));
        }
    }

    public static void renderHud(DrawContext draw, MinecraftClient client, ClientConfig config) {
        if ((!config.jumpCircle && !config.targetEffects) || client.world == null || client.player == null
                || client.options.hudHidden || frameWorld != client.world || System.nanoTime() - frameNanos > 500_000_000L) return;
        draw.enableScissor(0, 0, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        for (Line line : frameLines) {
            screenLine(draw, line, line.width + 2, (line.color >>> 24) / 3 << 24);
            screenLine(draw, line, line.width, line.color);
        }
        for (Dot dot : frameDots) {
            int x = (int) dot.x, y = (int) dot.y, r = Math.max(1, (int) Math.round(dot.radius));
            int glow = ((dot.color >>> 24) / 5 << 24) | (dot.color & 0xFFFFFF);
            draw.fill(x - r - 2, y - r - 2, x + r + 3, y + r + 3, glow);
            draw.fill(x - r, y - r, x + r + 1, y + r + 1, dot.color);
        }
        draw.disableScissor();
    }

    private static int alpha(int rgb, double opacity) {
        return (int) (Math.max(0, Math.min(1, opacity)) * 255) << 24 | rgb & 0xFFFFFF;
    }
    private static double seconds() { return System.nanoTime() / 1_000_000_000.0; }
    private static void screenLine(DrawContext draw, Line line, double thickness, int color) {
        double dx = line.x2 - line.x1, dy = line.y2 - line.y1;
        int length = (int) Math.ceil(Math.hypot(dx, dy));
        if (length <= 0) return;
        draw.getMatrices().push(); draw.getMatrices().translate(line.x1, line.y1, 0);
        draw.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(dy, dx)));
        draw.getMatrices().scale(1, (float) thickness, 1); draw.fill(0, 0, length, 1, color); draw.getMatrices().pop();
    }

    private record Projection(Matrix4f matrix, Vec3d camera, int width, int height, List<Line> lines, List<Dot> dots) {
        ScreenProjection.Point point(Vec3d position) {
            Vec3d relative = position.subtract(camera);
            Vector4f clip = matrix.transform(new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1));
            return ScreenProjection.projectClip(clip.x, clip.y, clip.z, clip.w, width, height);
        }
        void line(Vec3d a, Vec3d b, double thickness, int color) {
            if (lines.size() >= MAX_LINES) return;
            ScreenProjection.Point p = point(a), q = point(b);
            if (p == null || q == null || Math.abs(p.x()) > width * 4 || Math.abs(p.y()) > height * 4
                    || Math.abs(q.x()) > width * 4 || Math.abs(q.y()) > height * 4) return;
            lines.add(new Line(p.x(), p.y(), q.x(), q.y(), thickness, color));
        }
        void edge(Vec3d origin, EffectGeometry.Point a, EffectGeometry.Point b, double thickness, int color) {
            line(origin.add(a.x(), a.y(), a.z()), origin.add(b.x(), b.y(), b.z()), thickness, color);
        }
        void ring(Vec3d origin, double radius, double height, int count, double phase, double thickness, int color) {
            List<EffectGeometry.Point> points = EffectGeometry.ring(radius, height, count, phase);
            for (int i = 0; i < points.size(); i++) edge(origin, points.get(i), points.get((i + 1) % points.size()), thickness, color);
        }
        void dot(Vec3d position, double radius, int color) {
            if (dots.size() >= MAX_DOTS) return;
            ScreenProjection.Point p = point(position);
            if (p != null && p.x() > -8 && p.y() > -8 && p.x() < width + 8 && p.y() < height + 8)
                dots.add(new Dot(p.x(), p.y(), radius, color));
        }
    }
}
