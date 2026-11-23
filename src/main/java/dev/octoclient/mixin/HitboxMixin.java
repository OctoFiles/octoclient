package dev.octoclient.mixin;

import dev.octoclient.module.render.Hitbox;
import dev.octoclient.module.render.ArrowTrail;
import dev.octoclient.module.hud.LightLevelHud;
import dev.octoclient.module.secret.StorageESP;
import dev.octoclient.module.secret.Tracers;
import dev.octoclient.module.secret.HoleESP;
import dev.octoclient.module.secret.PlayerESP;
import dev.octoclient.module.secret.AuraViz;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.gizmos.Gizmos;
import org.joml.Matrix4fc;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class HitboxMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
            at = @At("HEAD"))
    private void octoclient$collectHitboxes(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker,
                                            boolean renderBlockOutline, CameraRenderState camera,
                                            Matrix4fc projection, GpuBufferSlice fog,
                                            Vector4f clearColor, boolean renderSky, CallbackInfo ci) {
        try (Gizmos.TemporaryCollection ignored =
                     ((LevelRenderer) (Object) this).collectPerFrameRenderThreadGizmos()) {
            Minecraft client = Minecraft.getInstance();
            Hitbox.submitWorldHitboxes(client);
            if (client.level != null) {
                ArrowTrail.submitTrails(client.level);
            }
            LightLevelHud.submitWorldOverlay(client);
            Tracers.submitTracers(client);
            StorageESP.submitStorage(client);
            HoleESP.submitHoles(client);
            PlayerESP.submitPlayers(client);
            AuraViz.submit(client);
        }
    }
}
