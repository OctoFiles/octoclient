package dev.octoclient.mixin;

import dev.octoclient.module.render.BadOptimization;
import dev.octoclient.module.render.PackTweaks;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class PackTweaksMixin {
    @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
    private void octoclient$hideSky(CallbackInfo ci) {
        if (PackTweaks.hidesSky()) {
            ci.cancel();
        }
    }

    @Inject(method = "addCloudsPass", at = @At("HEAD"), cancellable = true)
    private void octoclient$hideClouds(CallbackInfo ci) {
        if (PackTweaks.hidesClouds() || BadOptimization.hidesClouds()) {
            ci.cancel();
        }
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true)
    private void octoclient$hideWeather(CallbackInfo ci) {
        if (BadOptimization.hidesWeather()) {
            ci.cancel();
        }
    }
}
