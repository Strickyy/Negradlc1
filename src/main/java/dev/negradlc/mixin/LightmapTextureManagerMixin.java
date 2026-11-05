package dev.negradlc.mixin;

import dev.negradlc.NegradlcClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Overrides the lightmap's gamma read without changing the player's saved options. */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Shadow @Final private MinecraftClient client;

    @Redirect(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object negradlc$lightmapGamma(SimpleOption<?> option) {
        if (option == client.options.getGamma() && NegradlcClient.config() != null
                && NegradlcClient.config().fullbright) {
            return 16.0;
        }
        return option.getValue();
    }
}
