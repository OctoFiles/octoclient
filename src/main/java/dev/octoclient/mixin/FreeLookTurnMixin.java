package dev.octoclient.mixin;

import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.utility.FreeLook;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class FreeLookTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void octoclient$captureMouseLook(double yaw, double pitch, CallbackInfo ci) {
        FreeLook freeLook = ModuleManager.get().byName("Free Look", FreeLook.class).orElse(null);
        if (freeLook != null && freeLook.capturesTurn((Entity) (Object) this)) {
            freeLook.addLookDelta(yaw, pitch);
            ci.cancel();
        }
    }
}
