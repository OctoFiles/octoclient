package dev.octoclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.octoclient.module.render.BlockOutlines;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class BlockOutlinesMixin {
    @Inject(method = "submitHitOutline",
            at = @At("HEAD"))
    private void octoclient$submitOverlay(PoseStack pose, SubmitNodeCollector collector, RenderType renderType,
                                          BlockOutlineRenderState state, int color, float lineWidth, boolean fade,
                                          CallbackInfo ci) {
        BlockOutlines.submitOverlay(state);
    }

    @Redirect(method = "submitHitOutline",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitShapeOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/client/renderer/rendertype/RenderType;IFZ)V"))
    private void octoclient$customOutline(SubmitNodeCollector collector, PoseStack pose, VoxelShape shape,
                                         RenderType renderType, int color, float lineWidth, boolean fade) {
        if (BlockOutlines.active() && BlockOutlines.outlineEnabled()) {
            collector.submitShapeOutline(pose, shape, renderType, BlockOutlines.color(),
                    BlockOutlines.lineWidth(), fade);
        } else if (BlockOutlines.active()) {
            return;
        } else {
            collector.submitShapeOutline(pose, shape, renderType, color, lineWidth, fade);
        }
    }
}
