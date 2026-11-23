package dev.octoclient.mixin;

import dev.octoclient.module.render.NoFog;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class NoFogMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void octoclient$adjustFog(Camera camera, int renderDistance, DeltaTracker deltaTracker,
                                      float partialTick, ClientLevel level,
                                      CallbackInfoReturnable<FogData> cir) {
        NoFog.adjust(cir.getReturnValue(), camera.getFluidInCamera());
    }
}
