package dev.octoclient.mixin;

import dev.octoclient.module.hud.KeystrokesHud;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class KeystrokesMouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void octoclient$recordKeystrokesClick(long window, MouseButtonInfo button, int action,
                                                   CallbackInfo ci) {
        KeystrokesHud.recordMousePress(button.button(), action);
    }
}
