package dev.octoclient.mixin;

import dev.octoclient.module.render.OldAnimations;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Hud.class)
public class OldHeartAnimationMixin {
    @ModifyArg(method = "extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Hud$HeartType;getSprite(ZZZ)Lnet/minecraft/resources/Identifier;"),
            index = 1)
    private boolean octoclient$toggleOldHeartFlash(boolean flashing) {
        return flashing && OldAnimations.oldHeartAnimationEnabled();
    }
}
