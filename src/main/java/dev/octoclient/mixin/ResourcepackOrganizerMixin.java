package dev.octoclient.mixin;

import dev.octoclient.module.utility.ResourcepackOrganizer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
abstract class ResourcepackTransparentBackgroundMixin {
    @Inject(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$transparentPackScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                  float delta, CallbackInfo ci) {
        if ((Object) this instanceof PackSelectionScreen
                && ResourcepackOrganizer.transparentBackgroundEnabled()) {
            ci.cancel();
        }
    }
}

@Mixin(PackSelectionModel.EntryBase.class)
abstract class ResourcepackPreviewMixin {
    @Inject(method = "getIconTexture", at = @At("HEAD"), cancellable = true)
    private void octoclient$hidePackIcon(CallbackInfoReturnable<Identifier> cir) {
        if (!ResourcepackOrganizer.packPreviewsEnabled()) {
            cir.setReturnValue(Identifier.fromNamespaceAndPath("minecraft", "textures/misc/unknown_pack.png"));
        }
    }

    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
    private void octoclient$hidePackDescription(CallbackInfoReturnable<Component> cir) {
        if (!ResourcepackOrganizer.packPreviewsEnabled()) {
            cir.setReturnValue(Component.empty());
        }
    }
}
