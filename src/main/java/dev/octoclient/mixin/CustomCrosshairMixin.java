package dev.octoclient.mixin;

import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.render.CustomCrosshair;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Hud.class)
public class CustomCrosshairMixin {
    @Redirect(method = "extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void octoclient$replaceCrosshairSprite(GuiGraphicsExtractor gfx, RenderPipeline pipeline,
                                                   Identifier sprite, int x, int y, int width, int height) {
        CustomCrosshair module = ModuleManager.get()
                .byName("Custom Crosshair", CustomCrosshair.class).orElse(null);
        if (module != null && module.shouldReplaceVanillaCrosshair()
                && sprite.getPath().equals("hud/crosshair")) {
            module.render(gfx, Minecraft.getInstance());
            return;
        }
        gfx.blitSprite(pipeline, sprite, x, y, width, height);
    }
}
