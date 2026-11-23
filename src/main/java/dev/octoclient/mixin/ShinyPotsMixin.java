package dev.octoclient.mixin;

import dev.octoclient.module.render.ShinyPots;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ShinyPotsMixin {
    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void octoclient$addPotionGlint(CallbackInfoReturnable<Boolean> cir) {
        if (ShinyPots.shouldGlow((ItemStack) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
