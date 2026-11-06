package dev.negradlc.mixin;

import dev.negradlc.NegradlcClient;
import dev.negradlc.config.ClientConfig;
import dev.negradlc.visuals.AmbienceMath;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class SkyTintMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void negradlc$sky(Vec3d camera, float delta, CallbackInfoReturnable<Integer> cir) {
        ClientConfig config = NegradlcClient.config();
        if (config.worldCustomizer && config.skyTintEnabled)
            cir.setReturnValue(AmbienceMath.blendRgb(cir.getReturnValue(), config.skyTintColor, config.skyTintStrength));
    }
}
