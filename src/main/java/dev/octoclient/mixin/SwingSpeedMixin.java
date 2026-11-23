package dev.octoclient.mixin;

import dev.octoclient.module.render.SwingSpeed;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SwingSpeedMixin {
    @Inject(method = "getCurrentSwingDuration()I", at = @At("RETURN"), cancellable = true)
    private void octoclient$adjustSwingDuration(CallbackInfoReturnable<Integer> cir) {
        float speed = SwingSpeed.speedFor((LivingEntity) (Object) this);
        if (speed != 1f) {
            cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValue() / speed)));
        }
    }
}
