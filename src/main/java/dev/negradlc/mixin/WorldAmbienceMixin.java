package dev.negradlc.mixin;

import dev.negradlc.NegradlcClient;
import dev.negradlc.config.ClientConfig;
import dev.negradlc.visuals.AmbienceMath;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Getter overlays leave all server packets and real weather fields intact. */
@Mixin(World.class)
public abstract class WorldAmbienceMixin {
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void negradlc$rain(float delta, CallbackInfoReturnable<Float> cir) {
        ClientConfig config = NegradlcClient.config();
        if ((Object) this instanceof ClientWorld && config.worldCustomizer && config.customWeatherEnabled)
            cir.setReturnValue(AmbienceMath.rain(config.worldWeather));
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void negradlc$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        ClientConfig config = NegradlcClient.config();
        if ((Object) this instanceof ClientWorld && config.worldCustomizer && config.customWeatherEnabled)
            cir.setReturnValue(AmbienceMath.thunder(config.worldWeather));
    }
}
