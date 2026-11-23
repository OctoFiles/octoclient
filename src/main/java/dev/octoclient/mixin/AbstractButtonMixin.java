package dev.octoclient.mixin;

import dev.octoclient.module.settings.ClientSettings;
import dev.octoclient.util.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {
    @Unique
    private float octoclient$hover;
    @Unique
    private long octoclient$lastFrameNanos;

    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void octoclient$renderGlassButton(GuiGraphicsExtractor gfx, CallbackInfo ci) {
        AbstractWidget widget = (AbstractWidget) (Object) this;
        long now = System.nanoTime();
        float delta = octoclient$lastFrameNanos == 0L
                ? 1f / 60f : Math.min(0.05f, (now - octoclient$lastFrameNanos) / 1_000_000_000f);
        octoclient$lastFrameNanos = now;
        float target = widget.isActive() && widget.isHoveredOrFocused() ? 1f : 0f;
        octoclient$hover += (target - octoclient$hover) * (1f - (float) Math.exp(-18f * delta));

        // Every button uses the same glass look as the options rows, including
        // the title menu entries and its small icon buttons.
        float opacity = widget.isActive() ? 0.94f : 0.55f;
        Draw.liquidGlass(gfx, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(),
                Math.min(ClientSettings.buttonRadius(), widget.getHeight() / 2),
                0x929EAD, opacity, octoclient$hover,
                Math.round(34f + 48f * octoclient$hover));
        ci.cancel();
    }
}
