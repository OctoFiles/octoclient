package dev.octoclient.mixin;

import dev.octoclient.module.render.PackTweaks;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
abstract class PackTweaksPumpkinOverlayMixin {
    @Inject(method = "getViewBlockingState(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN"), cancellable = true)
    private static void octoclient$hidePumpkinOverlay(Player player, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (PackTweaks.hidesPumpkinOverlay() && state != null && state.is(Blocks.CARVED_PUMPKIN)) {
            cir.setReturnValue(null);
        }
    }
}
