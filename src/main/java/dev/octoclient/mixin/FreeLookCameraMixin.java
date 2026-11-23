package dev.octoclient.mixin;

import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.utility.FreeLook;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FreeLookCameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "alignWithEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
    private void octoclient$applyFreeLookRotation(float partialTick, CallbackInfo ci) {
        FreeLook freeLook = ModuleManager.get().byName("Free Look", FreeLook.class).orElse(null);
        if (freeLook == null || !freeLook.isFreeLooking()) {
            return;
        }
        setRotation(freeLook.cameraYaw(), freeLook.cameraPitch());
    }

    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), index = 0)
    private float octoclient$applyFreeLookDistance(float vanillaDistance) {
        FreeLook freeLook = ModuleManager.get().byName("Free Look", FreeLook.class).orElse(null);
        return freeLook != null && freeLook.shouldApplyDistance()
                ? freeLook.cameraDistance() : vanillaDistance;
    }
}
