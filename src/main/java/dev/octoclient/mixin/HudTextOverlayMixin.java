package dev.octoclient.mixin;

import dev.octoclient.module.render.ActionBar;
import dev.octoclient.module.render.Titles;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudTextOverlayMixin {
    @Shadow
    private Component title;
    @Shadow
    private Component subtitle;
    @Shadow
    private int titleTime;
    @Shadow
    private int titleFadeInTime;
    @Shadow
    private int titleStayTime;
    @Shadow
    private int titleFadeOutTime;
    @Shadow
    private Component overlayMessageString;
    @Shadow
    private int overlayMessageTime;

    @Inject(method = "extractTitle(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$customTitle(GuiGraphicsExtractor gfx, DeltaTracker deltaTracker, CallbackInfo ci) {
        Titles module = Titles.active();
        if (module == null || title == null || titleTime <= 0) {
            return;
        }
        float remaining = titleTime - deltaTracker.getGameTimeDeltaPartialTick(false);
        int alpha = titleAlpha(module.animation(), remaining);
        if (alpha <= 0) {
            ci.cancel();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        float scale = module.scale();
        float centerX = gfx.guiWidth() / 2f;
        float y = gfx.guiHeight() / 2f - 20 + module.yOffset();
        if (module.animation().equals("Slide")) {
            y += 12f * (1f - alpha / 255f);
        }

        gfx.nextStratum();
        gfx.pose().pushMatrix();
        gfx.pose().translate(centerX, y);
        gfx.pose().scale(scale, scale);
        Component renderedTitle = module.customTitleEnabled()
                ? Component.literal(module.displayedTitle(title.getString())) : title;
        String renderedSubtitleText = module.displayedSubtitle(subtitle == null ? "" : subtitle.getString());
        Component renderedSubtitle = module.customSubtitleEnabled()
                ? (renderedSubtitleText.isEmpty() ? null : Component.literal(renderedSubtitleText))
                : subtitle;
        gfx.text(font, renderedTitle, -font.width(renderedTitle) / 2, 0, 0x00FFFFFF | (alpha << 24), true);
        if (renderedSubtitle != null) {
            gfx.text(font, renderedSubtitle, -font.width(renderedSubtitle) / 2, 10,
                    0x00FFFFFF | (alpha << 24), true);
        }
        gfx.pose().popMatrix();
        ci.cancel();
    }

    @Inject(method = "extractOverlayMessage(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$customActionBar(GuiGraphicsExtractor gfx, DeltaTracker deltaTracker, CallbackInfo ci) {
        ActionBar module = ActionBar.active();
        if (module == null || overlayMessageString == null || overlayMessageTime <= 0) {
            return;
        }
        int alpha = Mth.clamp(Math.round((overlayMessageTime
                - deltaTracker.getGameTimeDeltaPartialTick(false)) * 255f / 20f), 0, 255);
        if (alpha <= 0) {
            ci.cancel();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int width = font.width(overlayMessageString);
        float scaleX = module.scaleX(width);
        float scaleY = module.scaleY(font.lineHeight);
        float centerX = gfx.guiWidth() / 2f + module.xOffset();
        float y = gfx.guiHeight() - 68f + module.yOffset();
        int color = module.textColor();
        int textAlpha = ((color >>> 24) * alpha / 255) << 24;
        color = textAlpha | (color & 0xFFFFFF);

        gfx.nextStratum();
        gfx.pose().pushMatrix();
        gfx.pose().translate(centerX, y);
        gfx.pose().scale(scaleX, scaleY);
        gfx.text(font, overlayMessageString, -width / 2, 0, color, true);
        gfx.pose().popMatrix();
        ci.cancel();
    }

    private int titleAlpha(String animation, float remaining) {
        if (animation.equals("None")) {
            return 255;
        }
        int alpha = 255;
        if (titleTime > titleFadeOutTime + titleStayTime && titleFadeInTime > 0) {
            float elapsed = titleFadeInTime + titleStayTime + titleFadeOutTime - remaining;
            alpha = Math.round(elapsed * 255f / titleFadeInTime);
        } else if (titleTime <= titleFadeOutTime && titleFadeOutTime > 0) {
            alpha = Math.round(remaining * 255f / titleFadeOutTime);
        }
        return Mth.clamp(alpha, 0, 255);
    }
}
