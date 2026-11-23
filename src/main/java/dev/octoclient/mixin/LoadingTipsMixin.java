package dev.octoclient.mixin;

import dev.octoclient.module.render.Tips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public class LoadingTipsMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void octoclient$drawLoadingTip(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                           float partialTick, CallbackInfo ci) {
        String tip = Tips.currentTip();
        if (tip != null) {
            graphics.centeredText(Minecraft.getInstance().font, Component.literal("Tip: " + tip),
                    graphics.guiWidth() / 2, graphics.guiHeight() - 28, 0xFFFFFFFF);
        }
    }
}
