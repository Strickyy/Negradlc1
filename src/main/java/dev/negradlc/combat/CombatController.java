package dev.negradlc.combat;

import dev.negradlc.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Optional;

/** Runs on the client tick thread. Rotation uses the real camera, with no spoofed packets. */
public final class CombatController {
    private static final Logger LOG = LoggerFactory.getLogger("Negradlc/Combat");
    private static final boolean DEBUG = Boolean.getBoolean("negradlc.debugCombat");
    private LivingEntity target;
    private String status = "Disabled";
    private int sprintStoppedAt = -1;
    private final FallTracker fallTracker = new FallTracker();
    private ClientPlayerEntity trackedPlayer;
    private Object trackedWorld;
    private LivingEntity auraTarget, aimTarget;
    private final RotationStepper auraStepper = new RotationStepper();
    private final RotationStepper aimStepper = new RotationStepper();
    private final RotationStepper returnStepper = new RotationStepper();
    private String activeMode;
    private final AuraViewSession viewSession = new AuraViewSession();
    private final TargetDwell triggerDwell = new TargetDwell();

    public String getStatus() { return status; }

    public LivingEntity getTarget() {
        return target;
    }

    public void reset() {
        target = null;
        status = "Disabled";
        sprintStoppedAt = -1;
        fallTracker.reset();
        trackedPlayer = null;
        trackedWorld = null;
        clearAuraMotion();
        aimTarget = null;
        aimStepper.reset();
        triggerDwell.reset();
    }

    public void tick(MinecraftClient client, ClientConfig config) {
        target = null;
        status = "No target";
        if (client.player == null || client.world == null || client.interactionManager == null
                || client.currentScreen != null || !client.isWindowFocused()
                || !client.player.isAlive() || client.player.isSpectator()
                || client.player.isUsingItem()) {
            status = "Paused";
            sprintStoppedAt = -1;
            fallTracker.reset();
            clearAuraMotion();
            aimTarget = null;
            aimStepper.reset();
            triggerDwell.reset();
            return;
        }

        if (trackedPlayer != client.player || trackedWorld != client.world) {
            fallTracker.reset();
            trackedPlayer = client.player;
            trackedWorld = client.world;
            clearAuraMotion();
            aimTarget = null;
            aimStepper.reset();
            triggerDwell.reset();
        }
        // In 1.21.4 Entity.move skips fall() for player-controlled entities.
        // Observe real descent instead of requiring the often-zero client fallDistance.
        float observedFall = fallTracker.sample(client.player.getY(), client.player.isOnGround());

        if (!java.util.Objects.equals(activeMode, config.auraRotation)) {
            auraStepper.reset();
            returnStepper.reset();
            activeMode = config.auraRotation;
        }
        viewSession.observe(client.player.getYaw(), client.player.getPitch());
        if (!config.auraReturnView) viewSession.reset();

        double attackRange = Math.max(0.0, config.attackRange);
        LivingEntity attackTarget = null;
        if (config.killAura) {
            LivingEntity candidate = closest(client, config, attackRange, config.auraFov, true, true);
            LivingEntity next = retainAuraTarget(client, config, candidate, attackRange);
            if (next != auraTarget) auraStepper.reset();
            target = auraTarget = next;
            attackTarget = next;
        } else {
            auraTarget = null;
            auraStepper.reset();
        }
        boolean auraActive = config.killAura && auraTarget != null;
        if (!auraActive && config.triggerBot && client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living
                && eligible(client, config, living, attackRange, true)) {
            attackTarget = target = living;
        }
        boolean dwellReady = triggerDwell.update(!auraActive && config.triggerBot && attackTarget != null
                ? attackTarget.getId() : -1, client.player.age, config.triggerDwellTicks);

        boolean aimRequested = config.aimAssist || config.triggerBot && config.smoothTriggerAim;
        boolean aimAllowed = aimRequested && (!config.requireAttackKey || client.options.attackKey.isPressed());
        if (auraActive) {
            aimTarget = null;
            aimStepper.reset();
            // Keep one stable acquisition session across hits and cooldowns. Returning
            // after each swing was the source of the visible aim/view oscillation.
            viewSession.acquire(client.player.getYaw(), client.player.getPitch(), config.auraReturnView);
            rotateAura(client, config, auraTarget);
        } else if (aimAllowed) {
            // KillAura enabled with no eligible melee target must not disable Aim Assist.
            viewSession.reset();
            returnStepper.reset();
            double fov = config.triggerBot && config.smoothTriggerAim ? Math.min(config.aimFov, config.triggerAimFov) : config.aimFov;
            LivingEntity next = config.aimLockTarget && aimTarget != null
                    && eligible(client, config, aimTarget, config.aimRange, config.aimVisibleOnly)
                    && CombatMath.insideFov(client.player.getYaw(), rotation(client, config.aimPoint, aimTarget).yaw(), fov)
                    ? aimTarget : closest(client, config, Math.max(0.0, config.aimRange), fov, config.aimVisibleOnly, false);
            if (next != aimTarget) aimStepper.reset();
            aimTarget = next;
            if (next != null) {
                rotateAim(client, config, next);
                status = "Aiming";
                target = next;
            } else {
                status = noTargetStatus(config);
            }
        } else {
            aimTarget = null;
            aimStepper.reset();
            if (aimRequested) status = "Aim: hold attack key";
            else if (config.killAura || config.triggerBot) status = noTargetStatus(config);
            else status = "Disabled";
            // TriggerBot retains camera priority when its crosshair already has a target.
            if (config.triggerBot && attackTarget != null) viewSession.reset();
            else {
                viewSession.release(config.auraReturnView);
                if (viewSession.returning()) {
                    returnView(client, config);
                    status = "Returning view";
                    return;
                }
            }
        }

        if (attackTarget == null || (!config.killAura && !config.triggerBot)) {
            sprintStoppedAt = -1;
            return;
        }
        if (!auraActive && config.triggerBot && !dwellReady) {
            status = "Trigger: stabilizing crosshair";
            return;
        }
        if (!looksAt(client, attackTarget, attackRange)) {
            status = "Aligning / blocked";
            return;
        }
        if (client.player.getAttackCooldownProgress(0.0f) < config.attackCooldown) {
            status = "Recharging";
            return;
        }
        if (config.critOnly) {
            if (config.airStag || client.player.getAbilities().flying || client.player.hasNoGravity()) {
                status = "Crit: disable hover / flight";
                return;
            }
            if (client.player.isClimbing() || client.player.isTouchingWater() || client.player.isInLava()
                    || client.player.hasStatusEffect(StatusEffects.BLINDNESS) || client.player.hasVehicle()) {
                status = "Crit unavailable here";
                return;
            }
            // A held sprint key can restart sprint before vanilla notices our end-tick
            // change. Transmit the same real transition explicitly, keeping client and
            // server in the stopped state before the attack packet is sent.
            if (client.player.isSprinting()) {
                if (sprintStoppedAt < 0) sprintStoppedAt = client.player.age;
                client.player.setSprinting(false);
                client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(
                        client.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
            if (sprintStoppedAt == client.player.age) {
                status = "Stopping sprint";
                return;
            }
            if (client.player.isOnGround() && config.autoCritJump) {
                client.player.jump();
                status = "Jumping for critical";
                return;
            }
            if (DEBUG) LOG.info("crit tick={} y={} vy={} ground={} vanillaFall={} observedFall={} sprint={}",
                    client.player.age, client.player.getY(), client.player.getVelocity().y,
                    client.player.isOnGround(), client.player.fallDistance, observedFall, client.player.isSprinting());
            if (!CombatMath.canCriticalHit(Math.max(client.player.fallDistance, observedFall),
                    client.player.isOnGround(), client.player.isClimbing(), client.player.isTouchingWater(),
                    client.player.hasStatusEffect(StatusEffects.BLINDNESS), client.player.hasVehicle(),
                    client.player.isSprinting()) || client.player.getVelocity().y >= 0) {
                status = "Waiting for falling critical";
                return;
            }
        } else {
            sprintStoppedAt = -1;
        }
        client.interactionManager.attackEntity(client.player, attackTarget);
        client.player.swingHand(Hand.MAIN_HAND);
        status = config.critOnly ? "Critical attack" : "Attack";
    }

    private LivingEntity closest(MinecraftClient client, ClientConfig config, double range, double fov, boolean requireVisibility, boolean aura) {
        Comparator<LivingEntity> distance = Comparator.comparingDouble(entity -> distanceSquared(client.player.getEyePos(), entity.getBoundingBox()));
        Comparator<LivingEntity> priority = !aura ? distance : switch (config.auraPriority) {
            case "HEALTH" -> Comparator.comparingDouble((LivingEntity entity) -> entity.getHealth() + entity.getAbsorptionAmount()).thenComparing(distance);
            case "CROSSHAIR" -> Comparator.comparingDouble((LivingEntity entity) -> Math.abs(CombatMath.wrapDegrees(
                    rotation(client, config.auraAimPoint, entity).yaw() - client.player.getYaw()))).thenComparing(distance);
            default -> distance;
        };
        return client.world.getEntitiesByClass(LivingEntity.class,
                        client.player.getBoundingBox().expand(range + 1.0),
                        entity -> eligible(client, config, entity, range, requireVisibility)
                                && CombatMath.insideFov(client.player.getYaw(),
                                rotation(client, aura ? config.auraAimPoint : config.aimPoint, entity).yaw(), fov))
                .stream().min(priority.thenComparingInt(Entity::getId)).orElse(null);
    }

    private boolean eligible(MinecraftClient client, ClientConfig config, LivingEntity entity, double range, boolean requireVisibility) {
        if (entity == client.player || !entity.isAlive() || entity.isRemoved()
                || !entity.isAttackable() || requireVisibility && !client.player.canSee(entity)
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

    private CombatMath.Rotation rotation(MinecraftClient client, String aimPoint, LivingEntity entity) {
        Box box = entity.getBoundingBox();
        Vec3d eye = client.player.getEyePos();
        Vec3d aim;
        if ("NEAREST".equals(aimPoint)) {
            double inset = Math.min(0.03, Math.min(box.getLengthX(), box.getLengthZ()) * 0.1);
            aim = new Vec3d(CombatMath.clamp(eye.x, box.minX + inset, box.maxX - inset),
                    CombatMath.clamp(eye.y, box.minY + inset, box.maxY - inset),
                    CombatMath.clamp(eye.z, box.minZ + inset, box.maxZ - inset));
        } else {
            double fraction = "HEAD".equals(aimPoint) ? 0.9 : 0.65;
            aim = new Vec3d((box.minX + box.maxX) / 2.0,
                    box.minY + (box.maxY - box.minY) * fraction, (box.minZ + box.maxZ) / 2.0);
        }
        Vec3d difference = aim.subtract(eye);
        return CombatMath.lookAt(difference.x, difference.y, difference.z);
    }

    private void rotateAim(MinecraftClient client, ClientConfig config, LivingEntity entity) {
        CombatMath.Rotation desired = rotation(client, config.aimPoint, entity);
        apply(client, aimStepper.step(client.player.getYaw(), client.player.getPitch(), desired.yaw(), desired.pitch(),
                config.aimSpeed, config.aimPitchSpeed, config.aimSmoothing, config.aimAcceleration, config.aimDeadzone));
    }

    private void rotateAura(MinecraftClient client, ClientConfig config, LivingEntity entity) {
        CombatMath.Rotation desired = rotation(client, config.auraAimPoint, entity);
        double yawSpeed = config.auraYawSpeed, pitchSpeed = config.auraPitchSpeed;
        double smoothing = config.auraSmoothing, acceleration = config.auraAcceleration;
        if ("SNAP".equals(config.auraRotation)) {
            // Fast acquisition, not an instantaneous 180-degree teleport. Even Snap
            // retains finite velocity/acceleration so the screen cannot flicker.
            yawSpeed = Math.min(60, Math.max(20, yawSpeed));
            pitchSpeed = Math.min(35, Math.max(12, pitchSpeed));
            smoothing = Math.max(0.65, smoothing);
            acceleration = Math.min(20, Math.max(8, acceleration));
        } else if ("SNAP_LEGIT".equals(config.auraRotation)) {
            yawSpeed = Math.min(35, yawSpeed);
            pitchSpeed = Math.min(22, pitchSpeed);
            acceleration = Math.min(8, acceleration);
        } else {
            yawSpeed = Math.min(18, yawSpeed);
            pitchSpeed = Math.min(12, pitchSpeed);
            smoothing = Math.min(0.35, smoothing);
            acceleration = Math.min(4, acceleration);
        }
        CombatMath.Rotation next = auraStepper.step(client.player.getYaw(), client.player.getPitch(),
                desired.yaw(), desired.pitch(), yawSpeed, pitchSpeed, smoothing, acceleration, config.auraDeadzone);
        apply(client, next);
        viewSession.applied(next.yaw(), next.pitch());
    }

    private void returnView(MinecraftClient client, ClientConfig config) {
        // Always ease the one return after loss/disable; never snap the view backwards.
        CombatMath.Rotation next = returnStepper.step(client.player.getYaw(), client.player.getPitch(),
                viewSession.yaw(), viewSession.pitch(), Math.min(12, config.auraYawSpeed),
                Math.min(8, config.auraPitchSpeed), Math.min(0.25, config.auraSmoothing),
                Math.min(3, config.auraAcceleration), config.auraDeadzone);
        apply(client, next);
        viewSession.applied(next.yaw(), next.pitch());
        if (Math.abs(CombatMath.wrapDegrees(viewSession.yaw() - next.yaw())) <= config.auraDeadzone + .01
                && Math.abs(viewSession.pitch() - next.pitch()) <= config.auraDeadzone + .01) {
            viewSession.reset();
            auraStepper.reset(); returnStepper.reset();
        }
    }

    private LivingEntity retainAuraTarget(MinecraftClient client, ClientConfig config,
                                          LivingEntity candidate, double range) {
        if (auraTarget == null || !eligible(client, config, auraTarget, range, true)
                || !CombatMath.insideFov(client.player.getYaw(), rotation(client, config.auraAimPoint, auraTarget).yaw(), config.auraFov)) return candidate;
        if (config.auraLockTarget || candidate == null || candidate == auraTarget) return auraTarget;
        // A small nearest/health/FOV fluctuation must not swing the view between entities.
        if ("HEALTH".equals(config.auraPriority)) {
            return candidate.getHealth() + candidate.getAbsorptionAmount() + 2
                    < auraTarget.getHealth() + auraTarget.getAbsorptionAmount() ? candidate : auraTarget;
        }
        if ("CROSSHAIR".equals(config.auraPriority)) {
            double currentAngle = Math.abs(CombatMath.wrapDegrees(rotation(client, config.auraAimPoint, auraTarget).yaw() - client.player.getYaw()));
            double nextAngle = Math.abs(CombatMath.wrapDegrees(rotation(client, config.auraAimPoint, candidate).yaw() - client.player.getYaw()));
            return nextAngle + 10 < currentAngle ? candidate : auraTarget;
        }
        double currentDistance = Math.sqrt(distanceSquared(client.player.getEyePos(), auraTarget.getBoundingBox()));
        double nextDistance = Math.sqrt(distanceSquared(client.player.getEyePos(), candidate.getBoundingBox()));
        return nextDistance + 0.35 < currentDistance ? candidate : auraTarget;
    }

    private String noTargetStatus(ClientConfig config) {
        if (!config.targetPlayers && !config.targetHostile && !config.targetPassive) return "Enable target filters";
        if (!config.targetHostile && !config.targetPassive) return "No player in range/FOV (mobs off)";
        return "No eligible target in range/FOV";
    }

    private void clearAuraMotion() {
        auraTarget = null;
        viewSession.reset();
        auraStepper.reset(); returnStepper.reset();
    }

    private void apply(MinecraftClient client, CombatMath.Rotation rotation) {
        client.player.setYaw(rotation.yaw());
        client.player.setPitch(rotation.pitch());
        client.player.setHeadYaw(rotation.yaw());
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
