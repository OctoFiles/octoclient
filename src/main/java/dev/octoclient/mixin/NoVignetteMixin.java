package dev.octoclient.mixin;

import dev.octoclient.module.render.PackTweaks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
abstract class NoVignetteMixin {
    @Inject(method = "extractVignette(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$disableVignette(GuiGraphicsExtractor graphics, Entity entity, CallbackInfo ci) {
        if (PackTweaks.hidesVignette()) {
            ci.cancel();
        }
    }
}
