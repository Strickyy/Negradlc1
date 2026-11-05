package dev.negradlc.movement;

import dev.negradlc.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Client movement controls. The server remains authoritative over movement. */
public final class MovementController {
    private ClientPlayerEntity hoverPlayer;
    private boolean originalNoGravity;
    private ClientPlayerEntity sprintPlayer;
    private boolean originalSprint;

    public void tick(MinecraftClient client, ClientConfig config) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.currentScreen != null
                || !client.isWindowFocused() || !player.isAlive() || player.isSpectator()) {
            reset();
            return;
        }

        // Never transfer temporary state to a respawned player or a new world.
        if (hoverPlayer != null && hoverPlayer != player) restoreHover();
        if (sprintPlayer != null && sprintPlayer != player) restoreSprint();

        boolean forward = client.options.forwardKey.isPressed();
        boolean backward = client.options.backKey.isPressed();
        boolean left = client.options.leftKey.isPressed();
        boolean right = client.options.rightKey.isPressed();
        boolean sneaking = client.options.sneakKey.isPressed();
        boolean canSprint = config.sprint && !(config.critOnly && (config.killAura || config.triggerBot))
                && forward && !backward && !sneaking
                && !player.horizontalCollision && !player.isUsingItem()
                && (player.getHungerManager().getFoodLevel() > 6 || player.getAbilities().allowFlying);
        if (canSprint) {
            if (sprintPlayer == null) {
                sprintPlayer = player;
                originalSprint = player.isSprinting();
            }
            player.setSprinting(true);
        } else {
            // Preserve sprint if the player is currently explicitly holding its key.
            if (client.options.sprintKey.isPressed()) sprintPlayer = null;
            else restoreSprint();
        }

        boolean canHover = config.airStag && !player.getAbilities().flying
                && !player.hasVehicle() && !player.isTouchingWater() && !player.isInLava();
        if (canHover) {
            if (hoverPlayer == null) {
                hoverPlayer = player;
                originalNoGravity = player.hasNoGravity();
            }
            player.setNoGravity(true);
            double vertical = (client.options.jumpKey.isPressed() ? 0.18 : 0.0)
                    - (sneaking ? 0.18 : 0.0);
            Vec3d velocity = player.getVelocity();
            player.setVelocity(velocity.x, vertical, velocity.z);
            player.fallDistance = 0.0f;
        } else {
            restoreHover();
        }

        if (config.airStrafe && !player.isOnGround() && !player.getAbilities().flying
                && !player.hasVehicle() && !player.isTouchingWater() && !player.isInLava()) {
            double forwardInput = (forward ? 1.0 : 0.0) - (backward ? 1.0 : 0.0);
            double sidewaysInput = (left ? 1.0 : 0.0) - (right ? 1.0 : 0.0);
            Vec3d current = player.getVelocity();
            double[] horizontal = MovementMath.steer(current.x, current.z, player.getYaw(),
                    forwardInput, sidewaysInput, config.airControl);
            player.setVelocity(horizontal[0], current.y, horizontal[1]);
        }
    }

    /** Releases only state owned by this controller. Safe during disconnect/respawn. */
    public void reset() {
        restoreHover();
        restoreSprint();
    }

    private void restoreHover() {
        if (hoverPlayer != null) {
            hoverPlayer.setNoGravity(originalNoGravity);
            hoverPlayer = null;
        }
    }

    private void restoreSprint() {
        if (sprintPlayer != null) {
            // Do not restart a sprint vanilla has since ended.
            if (!originalSprint) sprintPlayer.setSprinting(false);
            sprintPlayer = null;
        }
    }
}
