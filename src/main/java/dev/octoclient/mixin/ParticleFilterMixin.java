package dev.octoclient.mixin;

import dev.octoclient.module.render.Particles;
import dev.octoclient.module.render.BadOptimization;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ParticleFilterMixin {
    @Inject(method = "doAddParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$filterParticles(ParticleOptions particle, boolean force, boolean important,
                                            double x, double y, double z, double velocityX, double velocityY,
                                            double velocityZ, CallbackInfo ci) {
        if (Particles.shouldSuppress(particle) || BadOptimization.suppressParticle(force, important)) {
            ci.cancel();
        }
    }
}
