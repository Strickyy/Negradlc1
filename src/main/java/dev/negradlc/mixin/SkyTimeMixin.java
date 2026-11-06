package dev.negradlc.mixin;

import dev.negradlc.NegradlcClient;
import dev.negradlc.config.ClientConfig;
import dev.negradlc.visuals.AmbienceMath;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.LunarWorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Changes only the visual sky angle; tickTime still reads the untouched real server clock. */
@Mixin(LunarWorldView.class)
public interface SkyTimeMixin {
    @Inject(method = "getSkyAngle", at = @At("HEAD"), cancellable = true)
    private void negradlc$skyTime(float delta, CallbackInfoReturnable<Float> cir) {
        ClientConfig config = NegradlcClient.config();
        if ((Object) this instanceof ClientWorld world && config.worldCustomizer && config.customTimeEnabled)
            cir.setReturnValue(world.getDimension().getSkyAngle(AmbienceMath.timeOfDay(config.worldTime)));
    }
}
