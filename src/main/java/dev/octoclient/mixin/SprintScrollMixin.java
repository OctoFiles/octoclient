package dev.octoclient.mixin;

import dev.octoclient.module.utility.Sprint;
import dev.octoclient.module.render.Zoom;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class SprintScrollMixin {
    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void octoclient$adjustFlyBoost(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (Sprint.adjustFlyBoostFromScroll(vertical) || Zoom.adjustFromScroll(vertical)) {
            ci.cancel();
        }
    }
}
