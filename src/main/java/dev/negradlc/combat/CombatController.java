package dev.negradlc.combat;

import dev.negradlc.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Comparator;
import java.util.Optional;

/** Runs on the client tick thread. Rotation uses the real camera, with no spoofed packets. */
public final class CombatController {
    private LivingEntity target;

    public LivingEntity getTarget() {
        return target;
    }

    public void reset() {
        target = null;
    }

    public void tick(MinecraftClient client, ClientConfig config) {
        reset();
        if (client.player == null || client.world == null || client.interactionManager == null
                || client.currentScreen != null || !client.isWindowFocused()
                || !client.player.isAlive() || client.player.isSpectator()
                || client.player.isUsingItem()) {
            return;
        }
        if (!config.killAura && !config.triggerBot && !config.aimAssist) return;

        double attackRange = Math.max(0.0, config.attackRange);
        if (config.killAura) {
            target = closest(client, config, attackRange, false);
        } else if (config.triggerBot && client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living
                && eligible(client, config, living, attackRange)) {
            target = living;
        }

        LivingEntity attackTarget = target;
        if (config.killAura && target != null) {
            // AimAssist provides the turn-speed setting; otherwise Aura turns directly.
            rotate(client, target, config.aimAssist ? config.aimSpeed : 180.0);
        } else if (config.aimAssist && (!config.requireAttackKey || client.options.attackKey.isPressed())) {
            LivingEntity aimTarget = closest(client, config, Math.max(0.0, config.aimRange), true);
            if (aimTarget != null) {
                rotate(client, aimTarget, config.aimSpeed);
                target = aimTarget;
            }
        }

        if (attackTarget == null || (!config.killAura && !config.triggerBot)) return;
        if (client.player.getAttackCooldownProgress(0.0f) < Math.max(0.0, Math.min(1.0, config.attackCooldown))) return;
        if (config.critOnly && !CombatMath.canCriticalHit(client.player.fallDistance,
                client.player.isOnGround(), client.player.isClimbing(), client.player.isTouchingWater(),
                client.player.hasStatusEffect(StatusEffects.BLINDNESS), client.player.hasVehicle(),
                client.player.isSprinting())) return;
        if (!looksAt(client, attackTarget, attackRange)) return;

        client.interactionManager.attackEntity(client.player, attackTarget);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private LivingEntity closest(MinecraftClient client, ClientConfig config, double range, boolean useFov) {
        return client.world.getEntitiesByClass(LivingEntity.class,
                        client.player.getBoundingBox().expand(range + 1.0),
                        entity -> eligible(client, config, entity, range)
                                && (!useFov || CombatMath.insideFov(client.player.getYaw(),
                                rotation(client, entity).yaw(), config.aimFov)))
                .stream().min(Comparator.comparingDouble(entity -> distanceSquared(client.player.getEyePos(),
                        entity.getBoundingBox()))).orElse(null);
    }

    private boolean eligible(MinecraftClient client, ClientConfig config, LivingEntity entity, double range) {
        if (entity == client.player || !entity.isAlive() || entity.isRemoved()
                || !entity.isAttackable() || !client.player.canSee(entity)
                || distanceSquared(client.player.getEyePos(), entity.getBoundingBox()) > range * range) return false;
        if (entity instanceof PlayerEntity player) {
            if (!config.targetPlayers || player.isSpectator() || player.isCreative()) return false;
            return !config.ignoreFriends || config.friends == null || config.friends.stream()
                    .noneMatch(name -> player.getGameProfile().getName().equalsIgnoreCase(name));
        }
        if (!(entity instanceof MobEntity)) return false;
        return entity.getType().getSpawnGroup() == SpawnGroup.MONSTER
                ? config.targetHostile : config.targetPassive;
    }

    private CombatMath.Rotation rotation(MinecraftClient client, LivingEntity entity) {
        Box box = entity.getBoundingBox();
        Vec3d aim = new Vec3d((box.minX + box.maxX) / 2.0,
                box.minY + (box.maxY - box.minY) * 0.65, (box.minZ + box.maxZ) / 2.0);
        Vec3d difference = aim.subtract(client.player.getEyePos());
        return CombatMath.lookAt(difference.x, difference.y, difference.z);
    }

    private void rotate(MinecraftClient client, LivingEntity entity, double speed) {
        CombatMath.Rotation rotation = rotation(client, entity);
        client.player.setYaw(CombatMath.turnToward(client.player.getYaw(), rotation.yaw(), speed));
        float pitchDifference = rotation.pitch() - client.player.getPitch();
        float step = (float) Math.max(0.0, Math.min(180.0, speed));
        client.player.setPitch(CombatMath.clampPitch(client.player.getPitch()
                + Math.max(-step, Math.min(step, pitchDifference))));
        client.player.setHeadYaw(client.player.getYaw());
    }

    /** Recheck the post-rotation ray and blocks; stale crosshair data must not cause an attack. */
    private boolean looksAt(MinecraftClient client, LivingEntity entity, double range) {
        Vec3d start = client.player.getEyePos();
        Vec3d end = start.add(client.player.getRotationVec(1.0f).multiply(range));
        Box box = entity.getBoundingBox();
        Optional<Vec3d> hit = box.raycast(start, end);
        if (hit.isEmpty() && !box.contains(start)) return false;
        double targetDistance = hit.map(start::squaredDistanceTo).orElse(0.0);
        HitResult block = client.world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
        if (block.getType() != HitResult.Type.MISS
                && start.squaredDistanceTo(block.getPos()) + 0.0001 < targetDistance) return false;

        // Do not attack a target through another entity standing in front of it.
        for (Entity other : client.world.getOtherEntities(client.player,
                client.player.getBoundingBox().stretch(end.subtract(start)).expand(1.0),
                candidate -> candidate != entity && candidate.canHit())) {
            Optional<Vec3d> obstruction = other.getBoundingBox().raycast(start, end);
            if (other.getBoundingBox().contains(start)
                    || obstruction.isPresent() && start.squaredDistanceTo(obstruction.get()) < targetDistance) return false;
        }
        return true;
    }

    private static double distanceSquared(Vec3d point, Box box) {
        double x = Math.max(box.minX, Math.min(box.maxX, point.x));
        double y = Math.max(box.minY, Math.min(box.maxY, point.y));
        double z = Math.max(box.minZ, Math.min(box.maxZ, point.z));
        return point.squaredDistanceTo(x, y, z);
    }
}
