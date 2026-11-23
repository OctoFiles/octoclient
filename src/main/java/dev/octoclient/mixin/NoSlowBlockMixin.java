package dev.octoclient.mixin;

import dev.octoclient.module.secret.NoSlow;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Removes honey/soul sand/slime block slowdowns for the local player. */
@Mixin(Entity.class)
public class NoSlowBlockMixin {
    @Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
    private void octoclient$blockSlowdown(CallbackInfoReturnable<Float> cir) {
        if ((Object) this == Minecraft.getInstance().player && NoSlow.shouldRemoveBlockSlowdown()) {
            cir.setReturnValue(1.0f);
        }
    }
}
