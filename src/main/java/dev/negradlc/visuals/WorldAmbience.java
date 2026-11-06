package dev.negradlc.visuals;

import dev.negradlc.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;

/** Client-only particle emitter. Uses no packets and keeps no unbounded entity/particle queue. */
public final class WorldAmbience {
    private static ClientWorld world;
    private static ClientPlayerEntity player;
    private static double budget;
    private WorldAmbience() {}

    public static void tick(MinecraftClient client, ClientConfig config) {
        if (client.world != world || client.player != player) {
            world = client.world;
            player = client.player;
            budget = 0;
        }
        if (!config.worldParticles || world == null || player == null || client.isPaused()
                || client.currentScreen != null || !client.isWindowFocused()) {
            budget = 0;
            return;
        }
        budget += AmbienceMath.particlesPerTick(config.worldParticleDensity);
        int count = Math.min(3, (int) budget);
        budget -= count;
        double radius = Math.max(2, Math.min(24, config.worldParticleRange));
        SimpleParticleType type = switch (config.worldParticleStyle) {
            case "ENCHANT" -> ParticleTypes.ENCHANT;
            case "CHERRY" -> ParticleTypes.CHERRY_LEAVES;
            default -> ParticleTypes.END_ROD;
        };
        for (int i = 0; i < count; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double distance = radius * Math.sqrt(world.random.nextDouble());
            double x = player.getX() + Math.cos(angle) * distance;
            double y = player.getY() + .3 + world.random.nextDouble() * Math.min(6, radius);
            double z = player.getZ() + Math.sin(angle) * distance;
            // Vanilla particles retain their normal bounded lifetimes; speed is intentionally gentle.
            world.addParticle(type, x, y, z,
                    (world.random.nextDouble() - .5) * .015,
                    type == ParticleTypes.CHERRY_LEAVES ? -.015 : .008,
                    (world.random.nextDouble() - .5) * .015);
        }
    }
}
