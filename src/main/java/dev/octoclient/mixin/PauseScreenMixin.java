package dev.octoclient.mixin;

import dev.octoclient.ui.CosmeticsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void octoclient$addCosmeticsButton(CallbackInfo ci) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        Button cosmetics = Button.builder(Component.translatable("menu.octoclient.cosmetics"), button ->
                        Minecraft.getInstance().setScreenAndShow(new CosmeticsScreen()))
                .bounds(screenWidth - 132, screenHeight - 28, 122, 20)
                .build();
        ((ScreenAccessor) (Object) this).octoclient$addRenderableWidget(cosmetics);
    }
}
