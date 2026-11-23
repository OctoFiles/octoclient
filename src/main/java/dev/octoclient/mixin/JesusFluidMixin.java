package dev.octoclient.mixin;

import dev.octoclient.module.secret.Jesus;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Meteor's CanWalkOnFluidEvent equivalent for Jesus. */
@Mixin(LivingEntity.class)
public class JesusFluidMixin {
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void octoclient$jesus(FluidState state, CallbackInfoReturnable<Boolean> cir) {
        if (Jesus.canWalkOnFluid(state, Minecraft.getInstance())) {
            cir.setReturnValue(true);
        }
    }
}
