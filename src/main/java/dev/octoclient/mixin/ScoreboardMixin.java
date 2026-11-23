package dev.octoclient.mixin;

import dev.octoclient.module.render.Scoreboard;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class ScoreboardMixin {
    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$configureScoreboard(GuiGraphicsExtractor gfx,
                                                net.minecraft.world.scores.Objective objective,
                                                CallbackInfo ci) {
        if (Scoreboard.isHidden()) {
            ci.cancel();
            return;
        }
        Scoreboard.begin(gfx);
    }

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("RETURN"))
    private void octoclient$restoreScoreboard(GuiGraphicsExtractor gfx,
                                              net.minecraft.world.scores.Objective objective,
                                              CallbackInfo ci) {
        Scoreboard.end(gfx);
    }

    @Redirect(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private void octoclient$styleScoreboardPanel(GuiGraphicsExtractor gfx, int x1, int y1,
                                                  int x2, int y2, int color) {
        Scoreboard.drawPanel(gfx, x1, y1, x2, y2, color);
    }

    @Redirect(method = "lambda$displayScoreboardSidebar$1(Lnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/network/chat/numbers/NumberFormat;Lnet/minecraft/world/scores/PlayerScoreEntry;)Lnet/minecraft/client/gui/Hud$1DisplayEntry;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/scores/PlayerScoreEntry;formatValue(Lnet/minecraft/network/chat/numbers/NumberFormat;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent octoclient$hideScoreNumbers(PlayerScoreEntry entry, NumberFormat format) {
        return Scoreboard.formatScore(entry, format);
    }

    @ModifyArg(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"),
            index = 5)
    private boolean octoclient$setScoreboardTextShadow(boolean original) {
        return Scoreboard.fontShadowEnabled();
    }
}
